package game.treasure.table;

import com.google.protobuf.AbstractMessage;
import game.battle.model.*;
import game.battle.model.Unit;
import game.battle.object.*;
import game.battle.type.AnimationType;
import game.battle.type.RoomState;
import game.config.CfgItem;
import game.config.CfgMaterial;
import game.config.CfgServer;
import game.config.aEnum.BlockType;
import game.config.aEnum.DetailActionType;
import protocol.Pbmethod;
import game.config.aEnum.MapType;
import game.treasure.BattleConfig;
import game.treasure.mapping.UserEquipmentEntity;
import game.treasure.mapping.UserItemEntity;
import game.treasure.mapping.UserMaterialEntity;
import game.treasure.service.resource.ResItem;
import game.object.TaskMonitor;
import game.treasure.controller.AHandler;
import game.treasure.mapping.main.ResMapEntity;
import game.treasure.server.Constans;
import game.treasure.server.IAction;
import game.treasure.service.user.Bonus;
import game.object.MyUser;
import game.protocol.ProtoState;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ozudo.base.helper.ChUtil;
import ozudo.base.helper.Util;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import java.util.*;

@NoArgsConstructor
public abstract class BaseRoom extends MonoRoom {
    // static data
    Map<Integer, ChunkObject> mChunk = new HashMap<>();
    // chunk id -> list visible chunk by id chunk
    Map<Integer, List<Integer>> visibleByChunkId = new HashMap<>();
    // id in map --> Unit
    Map<Long, Unit> mUnit = new HashMap<>();
    // list player id in map
    List<Long> aPlayerIds = new ArrayList<>();

    // id chunk -> list unit id ở trong chunk = thay đổi giữa các chunk chỉ cần set  lại ở đây
    Map<Integer, Set<Long>> chunkCharacter = new HashMap<>();
    // danh cách các cell object đang xử lí ( hồi sinh, xóa)
    Map<Integer, Set<Integer>> cellObjectProcess = new HashMap<>();
    //
    Set<CellObject> cellObjectDie = new HashSet<>();


    float serverTime;
    @Getter
    @Setter
    ResMapEntity mapInfo;
    int idNext;
    public List<Integer> chunkNoAttack = new ArrayList<>();

    public BaseRoom(ResMapEntity mapInfo, Map<Integer, ChunkObject> mChunk, String keyRoom) {
        super(keyRoom);
        this.mapInfo = mapInfo;
        this.mChunk = mChunk;
        chunkNoAttack = mapInfo.getChunkNoAttack();
        // gen chunk default
        for (Integer key : mChunk.keySet()) {
            chunkCharacter.put(key, new HashSet<>());
            cellObjectProcess.put(key, new HashSet<>());
        }
        for (int y = mapInfo.getMinChunkY(); y <= mapInfo.getMaxChunkY(); y++) {
            for (int x = mapInfo.getMinChunkX(); x <= mapInfo.getMaxChunkX(); x++) {
                int centerId = MapService.chunkPosToId(mapInfo, x, y);
                visibleByChunkId.put(centerId, GameCore.getVisibleChunkIds(mapInfo, x, y));
            }
        }
        startInit();
    }

    protected void startInit() {

    }


    protected void sendTableState() {
        Map<Integer, byte[]> data = buildChunkViewData();
        if (data == null) return;
        for (int i = 0; i < aPlayerIds.size(); i++) {
            Unit player = mUnit.get(aPlayerIds.get(i));
            if (player != null && player.isPlayer() && player.getPlayer().getMUser().getChannel() != null) {
                Util.sendGameData(player.getPlayer().getMUser().getChannel(), data.get(player.getChunkId()), Constans.MAGIC_IN_PUT);
            }
        }
    }

    public Pbmethod.PbInitMap.Builder newPbInitMap() {
        Pbmethod.PbInitMap.Builder pbInitMap = Pbmethod.PbInitMap.newBuilder();
        pbInitMap.setMapId(mapType.value);
        pbInitMap.setBattleId(battleId);
        return pbInitMap;
    }

    public List<Channel> getListChannel() {
        List<Channel> lst = new ArrayList<>();
        for (int i = 0; i < aPlayerIds.size(); i++) {
            Player p = mUnit.get(aPlayerIds.get(i)).getPlayer();
            if (p != null && p.getMUser().getChannel() != null && p.getMUser().getChannel().isActive()) {
                lst.add(p.getMUser().getChannel());
            }
        }
        return lst;
    }

    public synchronized long getIdNext() {
        return idNext++;
    }

    public boolean isMaxPlayer() {
        return aPlayerIds.size() >= mapType.maxPlayer;
    }


    protected Map<Integer, byte[]> buildChunkViewData() {
        int action = IAction.TABLE_STATE;// K dùng nhưng viết ở đây để referent
        Map<Integer, byte[]> chunkViewData = new HashMap<>();
        // 1. cache unit theo chunk (convert 1 lần)
        Map<Integer, List<Pbmethod.PbUnitPos>> chunkUnitPosMap = new HashMap<>();

        for (Map.Entry<Integer, Set<Long>> entry : chunkCharacter.entrySet()) {
            List<Pbmethod.PbUnitPos> list = new ArrayList<>();
            for (Long unitId : entry.getValue()) {
                Unit u = mUnit.get(unitId);
                // Luôn sync vị trí unit còn sống (kể cả đứng yên). Lọc theo isMove() khiến sau P_timeNoMove
                // unit biến mất khỏi TYPE_POS → client khác không nhận pos / nội suy lệch.
                if (u != null && u.isAlive()) {
                    list.add(u.toProtoPos());
                }
            }
            chunkUnitPosMap.put(entry.getKey(), list);
        }

        // copy event
        List<Pbmethod.PbUnit> protoChange = new ArrayList<>(aProtoChange);
        aProtoChange.clear();

        Map<Long, List<Pbmethod.PbUnit>> protoChangeByUnitId = new LinkedHashMap<>();
        for (Pbmethod.PbUnit u : protoChange) {
            protoChangeByUnitId.computeIfAbsent(u.getId(), k -> new ArrayList<>()).add(u);
        }

        List<Pbmethod.PbUnitState> protoUnitStateCopy = snapshotAndClearProtoUnitState();

        // Snapshot cell pending theo chunk thật của cell — dùng cho mọi viewer có chunk đó trong view.
        // (Trước đây chỉ đọc cellObjectProcess.get(chunkId viewer) nên object ở chunk khác không gửi tới player lân cận.)
        Map<Integer, Set<Integer>> cellProcessSnapshot = new HashMap<>();
        for (Map.Entry<Integer, Set<Integer>> e : cellObjectProcess.entrySet()) {
            if (e.getValue() != null && !e.getValue().isEmpty()) {
                cellProcessSnapshot.put(e.getKey(), new LinkedHashSet<>(e.getValue()));
            }
        }

        // 2. build data cho từng chunk
        for (Integer chunkId : chunkCharacter.keySet()) {
            protocol.Pbmethod.PbState.Builder builder = protocol.Pbmethod.PbState.newBuilder();
            builder.setServerTime(serverTime);

            List<Integer> visibleChunks = visibleByChunkId.get(chunkId);
            Set<Long> added = new HashSet<>();

            if (visibleChunks != null) {
                for (Integer vChunk : visibleChunks) {
                    List<Pbmethod.PbUnitPos> list = chunkUnitPosMap.get(vChunk);
                    if (list != null) {
                        for (Pbmethod.PbUnitPos u : list) {
                            if (added.add(u.getId())) {
                                // System.out.println("u.getChunkId() = " + u.getChunkId());
                                builder.addUnitPos(u);
                            }
                        }
                    }
                }
            }

            appendProtoChangeForViewer(visibleChunks, builder, protoChangeByUnitId);

            if (!protoUnitStateCopy.isEmpty()) {
                builder.setUnitUpdate(ProtoState.protoUnitUpdate(Pbmethod.StateType.TYPE_UNIT_STATE_VALUE,
                        ProtoState.protoListCharacterState(protoUnitStateCopy)));
            }

            if (visibleChunks != null) {
                for (Integer vChunk : visibleChunks) {
                    Set<Integer> cellIds = cellProcessSnapshot.get(vChunk);
                    if (cellIds == null || cellIds.isEmpty()) continue;
                    ChunkObject chunkObject = mChunk.get(vChunk);
                    if (chunkObject == null) continue;
                    builder.addChunkState(chunkObject.toProtoUpdate(cellIds));
                }
            }

            chunkViewData.put(chunkId, ProtoState.convertProtoBuffToState(builder.build()));
        }

        for (Integer key : cellProcessSnapshot.keySet()) {
            Set<Integer> live = cellObjectProcess.get(key);
            if (live != null) live.clear();
        }

        return chunkViewData;
    }

    protected void debug(String msg) {
        if (CfgServer.isRealServer()) {
            System.out.println(msg);
        }
    }

    protected void ProcessReviveCell() {
        if (cellObjectDie.isEmpty()) return;
        Iterator<CellObject> it = cellObjectDie.iterator();
        while (it.hasNext()) {
            CellObject cell = it.next();
            if (cell == null || cell.canAttack()) {
                it.remove();
                continue;
            }
            // nếu đủ điều kiện hồi sinh thì revive + mark update để client nhận state
            if (cell.canRevive()) {
                cell.revive();
                addCellProcess(cell);
                it.remove();
            }
        }
    }


    private static void appendProtoChangeForViewer(List<Integer> visibleChunks, protocol.Pbmethod.PbState.Builder builder,
                                                   Map<Long, List<Pbmethod.PbUnit>> protoChangeByUnitId) {
        if (visibleChunks == null) return;
        for (List<Pbmethod.PbUnit> events : protoChangeByUnitId.values()) {
            if (events.isEmpty()) continue;
            if (events.size() == 1) {
                Pbmethod.PbUnit u = events.get(0);
                if (visibleChunks.contains(u.getChunkId())) {
                    builder.addUnitAdd(u);
                }
                continue;
            }
            if (events.size() == 2
                    && !events.get(0).getIsAdd()
                    && events.get(1).getIsAdd()) {
                Pbmethod.PbUnit rem = events.get(0);
                Pbmethod.PbUnit add = events.get(1);
                boolean seeOld = visibleChunks.contains(rem.getChunkId());
                boolean seeNew = visibleChunks.contains(add.getChunkId());
                if (seeOld && seeNew) {
                    continue;
                }
                if (seeOld) {
                    builder.addUnitAdd(rem);
                }
                if (seeNew) {
                    builder.addUnitAdd(add);
                }
                continue;
            }
            for (Pbmethod.PbUnit u : events) {
                if (visibleChunks.contains(u.getChunkId())) {
                    builder.addUnitAdd(u);
                }
            }
        }
    }


    public Unit getPlayerId(long id) {
        return mUnit.getOrDefault(id, null);
    }

    @Override
    public void Update1s() {
        ProcessReviveCell();
    }

    @Override
    public void Update() {
        long _dt = System.currentTimeMillis() - _dte;
        _dte = System.currentTimeMillis();
        localTime += _dt / 1000.0;
        serverTime = localTime;
        // send data
        mUnit.forEach((k, u) -> u.Update());
        try {
            sendTableState();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    public synchronized void doSyncAction(Channel channel, int actionId, byte[] srcRequest) {
        try {
            MyUser mUser = (MyUser) ChUtil.get(channel, ChUtil.KEY_M_USER);
            switch (actionId) {
                case IAction.CLIENT_INPUT:
                    handleClientInput(mUser.getPlayer(), NInput.parse(srcRequest));
                    break;
            }
        } catch (Exception ex) {
            Logs.error(Util.exToString(ex));
        }
    }


    public boolean allowChangeChanel() {
        return mapType.allowChangeChanel;
    }

    public void handleClientInput(Player player, NInput input) {
        if (input.typeId == NInput.ADD_ZONE_HEATH || input.typeId == NInput.REMOVE_ZONE_HEATH) {
            if (this instanceof HomeRoom && player != null && player.isAlive()
                    && player.getRoom() != null && player.getRoom().getRoomState() == RoomState.ACTIVE) {
                ((HomeRoom) this).handleZoneHeathInput(player, input);
            }
            return;
        }
        if (!player.isAlive() || !player.isReady() || player.getRoom() == null || player.getRoom().getRoomState() != RoomState.ACTIVE)
            return;
        // check Idle
        if (input.typeId != NInput.PING_GAME) {
//            System.out.println("remove idle -----------------------");
            ChUtil.remove(player.getMUser().getChannel(), ChUtil.KEY_IDLE);
        }
        if (input.typeId == NInput.INPUT_PLAYER_MOVE) {
            long lastInputSeq = input.seq;
            if (lastInputSeq <= player.getIndexLastInputSeq()) {
                return;
            }
            float timeTravel = System.currentTimeMillis() - player.getTimeLastProcessInput();
            player.setTimeLastProcessInput(System.currentTimeMillis());
            player.setIndexLastInputSeq(lastInputSeq);
            if (player.isAlive()) {
                Pos movePos = input.playerPos;
                if (player.getMUser().getUser().getBlockType() == BlockType.BLOCK_ACTION) {
                    movePos = mapInfo.clampToJailZone(movePos);
                }
                player.setPosAndDirection(movePos, input.playerDirection);
            }
        } else if (input.typeId == NInput.PING_GAME) {
            Util.sendProtoData(player.getMUser().getChannel(), null, IAction.PING_GAME);
        } else if (input.typeId == NInput.USE_ITEM) {
            game.treasure.service.resource.ResItem.useBuffItemInRoom(player, input.useItemId);
        } else if (input.typeId == NInput.ATTACK) {
            if (!player.canAttack()) return;
            if (input.targetAttack == Pbmethod.TargetAttack.OBJECT) {
                int globalCellId = (int) input.idAttack;
                int chunkId = MapService.globalCellIdToChunkId(mapInfo, globalCellId);
                CellObject cellObject = getCellObject(globalCellId, chunkId);
                if (cellObject != null && cellObject.canAttack() && player.hasAttack()) {
                    addCellProcess(cellObject);
                    boolean cellDie = cellObject.attack();
                    player.setTimeAttack();
                    player.protoStatus(Pbmethod.SubStateType.PLAY_ANIM, (long) AnimationType.ATTACK.value);
                    if (cellDie) {
                        addCellDie(cellObject);
                        applyCellKillBonus(player, cellObject.getBonusKillMe());
                    }
                }
            } else { // Đánh unit
                Unit unit = mUnit.get(input.idAttack);
                if (unit == null) return;
                if (!unit.isAlive()) return;
                if (player.getClanId() != 0 && player.getClanId() == unit.getClanId()) return;
                if (unit.isPlayer()) {
                    if (mapInfo.isInCampFireSafeZone(player.getPos())
                            || mapInfo.isInCampFireSafeZone(unit.getPos())) return;
                    if (mapInfo.isInBlockedPvpZone(player.getPos())
                            || mapInfo.isInBlockedPvpZone(unit.getPos())) return;
                }

                // giống nhánh OBJECT: chống spam theo tick bằng attackSpeed
                if (!player.hasAttack() || !player.targetInSizeAttack(unit)) return;
                player.faceToward(unit);
                player.setTimeAttack();
                player.protoStatus(Pbmethod.SubStateType.PLAY_ANIM, (long) AnimationType.ATTACK.value);
                // attacker là player, target là unit
                player.attackUnit(unit);
            }
        }
    }


    public void addCellProcess(CellObject cellObject) {
        cellObjectProcess.get(cellObject.getChunkId()).add(cellObject.getId());
    }

    public void addCellDie(CellObject cellObject) {
        cellObjectDie.add(cellObject);
    }

    public CellObject getCellObject(int globalCellId, int chunkId) {
        if (mChunk.containsKey(chunkId) && mChunk.get(chunkId).getMCells().containsKey(globalCellId))
            return mChunk.get(chunkId).getMCells().get(globalCellId);
        return null;
    }

    /** Phá cell: cộng bonus cho player; chỉ spawn quái khi chunk {@code [-1, mobId]}. */
    void applyCellKillBonus(Player player, List<Long> bonus) {
        if (bonus == null || bonus.isEmpty()) return;
        if (bonus.get(0) == -1L) {
            if (bonus.size() < 2) return;
            Pos posInit = Pos.randomPos(player.getPos(), 2f, 2f);
            Enemy enemy = new Enemy(Math.toIntExact(bonus.get(1)), player, posInit);
            addUnit(enemy);
            return;
        }
        List<Long> resolved = resolveCellKillBonus(player.getMUser(), bonus);
        if (resolved.isEmpty()) return;
        player.sendBonus(resolved, DetailActionType.KILL_CELL.getKey());
    }

    /**
     * Chỉ dùng khi phá cell: túi/event/material đầy thì quy item sang vàng theo giá bán preview.
     */
    List<Long> resolveCellKillBonus(MyUser mUser, List<Long> bonus) {
        List<Long> result = new ArrayList<>();
        int pendingBag = 0;
        int pendingEvent = 0;
        int pendingMaterial = 0;

        for (List<Long> chunk : Bonus.parse(bonus)) {
            int[] need = countCellKillSlotNeed(mUser, chunk);
            boolean overflow = (need[0] > 0 && !mUser.getResources().canAddBagItem(pendingBag + need[0]))
                    || (need[1] > 0 && !mUser.getResources().canAddEventItem(pendingEvent + need[1]))
                    || (need[2] > 0 && !mUser.getResources().canAddMaterial(pendingMaterial + need[2]));

            if (overflow) {
                long sellGold = getCellKillPreviewSellGold(mUser, chunk);
                if (sellGold > 0) result.addAll(Bonus.viewGold(sellGold));
                continue;
            }

            pendingBag += need[0];
            pendingEvent += need[1];
            pendingMaterial += need[2];
            result.addAll(chunk);
        }
        return Bonus.merge(result);
    }

    /** @return [bagSlots, eventSlots, materialSlots] */
    static int[] countCellKillSlotNeed(MyUser mUser, List<Long> chunk) {
        int[] need = new int[3];
        if (chunk.isEmpty()) return need;
        int bonusType = chunk.get(0).intValue();
        if (bonusType == Bonus.BONUS_ITEM) {
            int itemKey = chunk.get(1).intValue();
            if (itemKey < 0) return need;
            if (Bonus.resolveStorageType(itemKey) == Pbmethod.ItemType.POSITION)
                need[0] = 1;
        } else if (bonusType == Bonus.BONUS_ITEM_POINT) {
            int pointId = chunk.get(1).intValue();
            if (chunk.size() >= 3 && chunk.get(2) > 0 && Bonus.usesEventBagPoint(pointId)
                    && mUser.getResources().getItemPointNumber(pointId) <= 0)
                need[1] = 1;
        } else if (bonusType == Bonus.BONUS_EQUIPMENT) {
            need[0] = 1;
        } else if (bonusType == Bonus.BONUS_MATERIAL) {
            need[2] = 1;
        }
        return need;
    }

    static long getCellKillPreviewSellGold(MyUser mUser, List<Long> chunk) {
        if (chunk.isEmpty()) return 0;
        int bonusType = chunk.get(0).intValue();
        if (bonusType == Bonus.BONUS_ITEM) {
            int itemKey = chunk.get(1).intValue();
            if (itemKey < 0) return 0;
            Pbmethod.ItemType type = Bonus.resolveStorageType(itemKey);
            UserItemEntity preview = new UserItemEntity(mUser.getUser().getId(), itemKey, type);
            return CfgItem.getSellPriceGold(preview);
        }
        if (bonusType == Bonus.BONUS_EQUIPMENT ) {
            int itemKey = chunk.get(1).intValue();
            int tier = chunk.get(2).intValue();
            UserEquipmentEntity preview = new UserEquipmentEntity(mUser.getUser().getId(), itemKey);
            preview.setTier(ResItem.resolveTier(tier));
            return CfgItem.getSellPriceGold(preview);
        }
        if (bonusType == Bonus.BONUS_MATERIAL && chunk.size() >= 3) {
            int materialId = chunk.get(1).intValue();
            int rank = chunk.get(2).intValue();
            UserMaterialEntity preview = new UserMaterialEntity(mUser.getUser().getId(), materialId, rank);
            return CfgMaterial.getMergeSellPrice(preview.getTier(), preview.getLevel());
        }
        return 0;
    }


    // tất cả unit sẽ gọi qua hàm này
    public boolean joinChunk(Unit unit, int newChunk) {
        if (unit == null) return false;
        int oldChunk = unit.getChunkId();
        if (oldChunk == newChunk) return false;

        if (!isValidChunkId(newChunk)) {
            Logs.warn("joinChunk invalid newChunk=" + newChunk + ", unitId=" + unit.getId());
            return false;
        }

        boolean oldValid = isValidChunkId(oldChunk);
        if (!oldValid) {
            Logs.warn("joinChunk invalid oldChunk=" + oldChunk + ", unitId=" + unit.getId());
            // recover: không remove chunk cũ vì oldChunk không hợp lệ
        } else {
            Set<Long> oldSet = chunkCharacter.get(oldChunk);
            if (oldSet != null) oldSet.remove(unit.getId());
        }
        Set<Long> newSet = chunkCharacter.get(newChunk);
        if (newSet == null) return false;

        newSet.add(unit.getId());
        unit.setChunkId(newChunk);

        // Quái/pet: client nhận chunkId mới qua PbUnitPos — không remove/add để tránh despawn.
        if (unit.isPlayer()) {
            if (oldValid) aProtoChange.add(unit.toProtoRemove(oldChunk));
            aProtoChange.add(unit.toProtoAdd(newChunk));
        }

        return true;
    }

    public void joinRoom(AHandler handler, Player player) {
        if (roomState != RoomState.ACTIVE) return;
        addUnit(player);

        // trả về 9 chunk xung quanh player
        int curChunk = player.getChunkId();
        List<Integer> chunkVisible = visibleByChunkId.get(curChunk);
        Pbmethod.PbInitMap.Builder pbInit = newPbInitMap();
        long protectedEnd = player.getMUser().getUData().getTimeProtected();
        if (protectedEnd > System.currentTimeMillis()) {
            player.setTimeProtectedEnd(protectedEnd);
            pbInit.setTimeProtected(BattleConfig.toWireProtectedMs(protectedEnd));
        }

        for (int i = 0; i < chunkVisible.size(); i++) {
            // add data chunks
            ChunkObject chunkData = mChunk.get(chunkVisible.get(i));
            pbInit.addChunks(chunkData.toProtoAdd());
            Set<Long> aCharInChunk = chunkCharacter.get(chunkVisible.get(i));
            for (Long charId : aCharInChunk) {
                Unit unit = mUnit.get(charId);
                pbInit.addUnits(unit.toProtoAdd(unit.getChunkId()));
            }
        }
        handler.addResponse(IAction.JOIN_MAP, pbInit.build());
    }

    public void sendDataAllUser(int service, AbstractMessage data) {
        for (int i = 0; i < aPlayerIds.size(); i++) {
            Player p = mUnit.get(aPlayerIds.get(i)).getPlayer();
            if (p != null) {
                Util.sendProtoData(p.getMUser().getChannel(), data, service);
            }
        }
    }


    public void characterDie(Unit unit) {
    }

    public boolean hasPlayer(long userId) {
        return mUnit.containsKey(userId);
    }

    /** Player đang online trong room (dùng cho buff cổ vật area). */
    public List<Player> listPlayers() {
        List<Player> list = new ArrayList<>();
        for (int i = 0; i < aPlayerIds.size(); i++) {
            Unit unit = mUnit.get(aPlayerIds.get(i));
            if (unit != null && unit.isPlayer()) {
                Player p = unit.getPlayer();
                if (p != null && p.getMUser() != null)
                    list.add(p);
            }
        }
        return list;
    }

    public int worldPosToChunkId(Pos pos) {
        return MapService.worldPosToChunkId(mapInfo, pos);
    }

    public boolean isValidChunkId(int chunkId) {
        return chunkCharacter.containsKey(chunkId);
    }


    // add mới từ khi instance ra 1 unit thì chạy qua đây
    public void addUnit(Unit unit) {
        if (roomState != RoomState.ACTIVE && roomState != RoomState.PAUSE) return;
        long idInMap = getIdNext();
        unit.setId(idInMap);
        // tính chunk hiện tại nó đang ở
        int chunkId = worldPosToChunkId(unit.getPos());
        unit.setChunkId(chunkId);
        mUnit.put(idInMap, unit);
        chunkCharacter.get(chunkId).add(idInMap);
        aProtoChange.add(unit.toProtoAdd(chunkId));

        if (unit.isPlayer()) {
            aPlayerIds.add(idInMap);
            Pet pet = unit.getPlayer().getPetUse();
            if (pet != null) addUnit(pet);
        }
    }


    // tất cả các unit đều sẽ qua hàm này để xóa
    public void removeUnit(long idInMap) {
        debug("Remove unit ---------------------- " + idInMap + " for room : " + battleId);
        Unit unit = mUnit.get(idInMap);
        if (unit == null) return;

        Set<Long> set = chunkCharacter.get(unit.getChunkId());
        if (set != null) set.remove(idInMap);

        // remove pet nếu là player
        if (unit.isPlayer()) {
            aPlayerIds.remove(idInMap);
            Pet pet = unit.getPlayer().getPetUse();
            if (pet != null) {
                long petId = pet.getId();
                Unit petUnit = mUnit.get(petId);
                if (petUnit != null) {
                    Set<Long> petSet = chunkCharacter.get(petUnit.getChunkId());
                    if (petSet != null) petSet.remove(petId);
                    mUnit.remove(petId);
                    aProtoChange.add(petUnit.toProtoRemove(petUnit.getChunkId()));
                }
            }
        }

        mUnit.remove(idInMap);
        aProtoChange.add(unit.toProtoRemove(unit.getChunkId()));
        if (aPlayerIds.isEmpty()) cancelTask();
    }


    protected abstract void cancelTask();

    public int getRoomTypeId() { // = id map
        return Integer.parseInt(TaskMonitor.getKeyRoomById(battleId)[1]);
    }

    public MapType getRoomType() { // = id map
        return MapType.get(Integer.parseInt(TaskMonitor.getKeyRoomById(battleId)[1]));
    }

    public int getChannelId() {
        return Integer.parseInt(TaskMonitor.getKeyRoomById(battleId)[2]);
    }

    public void syncViewDeltaForPlayer(Player player, int oldChunk, int newChunk) {
        if (player == null || player.getMUser() == null || player.getMUser().getChannel() == null) return;
        List<Integer> oldVisible = visibleByChunkId.get(oldChunk);
        List<Integer> newVisible = visibleByChunkId.get(newChunk);
        if (oldVisible == null || newVisible == null) return;

        // entered = newVisible - oldVisible
        Set<Integer> entered = new HashSet<>(newVisible);
        oldVisible.forEach(entered::remove);

        // exited = oldVisible - newVisible
        Set<Integer> exited = new HashSet<>(oldVisible);
        newVisible.forEach(exited::remove);

        protocol.Pbmethod.PbState.Builder builder = protocol.Pbmethod.PbState.newBuilder();
        builder.setServerTime(serverTime);

        long selfId = player.getId();
        // Add toàn bộ unit trong các chunk mới lọt vào view
        Set<Long> added = new HashSet<>();
        for (Integer chunkId : entered) {
            Set<Long> unitIds = chunkCharacter.get(chunkId);
            if (unitIds == null) continue;
            for (Long uid : unitIds) {
                if (uid == selfId) continue; // skip self nếu muốn
                if (!added.add(uid)) continue;

                Unit u = mUnit.get(uid);
                if (u == null || !u.isAlive()) continue;
                // chunkId truyền vào proto là chunk historical theo view nhập mới
                builder.addUnitAdd(u.toProtoAdd(chunkId));
            }

            // add state cell in chunk
            ChunkObject chunk = mChunk.get(chunkId);
            builder.addChunkState(chunk.toProtoAdd());
        }

        // Remove toàn bộ unit trong các chunk rời khỏi view
        Set<Long> removed = new HashSet<>();
        for (Integer chunkId : exited) {
            Set<Long> unitIds = chunkCharacter.get(chunkId);
            if (unitIds == null) continue;

            for (Long uid : unitIds) {
                if (uid == selfId) continue;
                if (!removed.add(uid)) continue;

                Unit u = mUnit.get(uid);
                if (u == null) continue;
                // remove theo chunk historical (chunk đang exited), không dùng u.getChunkId() hiện tại
                builder.addUnitAdd(u.toProtoRemove(chunkId));

            }
            // add state cell in chunk
            ChunkObject chunk = mChunk.get(chunkId);
            builder.addChunkState(chunk.toProtoRemove());
        }

        byte[] state = ProtoState.convertProtoBuffToState(builder.build());
        Util.sendGameData(player.getMUser().getChannel(), state, Constans.MAGIC_IN_PUT);
    }


//    public void protoRoomState(StateType status, int size, List<Long> aInfo) {
//        for (int i = 0; i < aPlayer.size(); i++) {
//            aProtoUnitState.add(protoState(aPlayer.get(i).getId(), List.of(status), List.of(size), aInfo));
//        }
//    }
//
//    public void protoRoomState(StateType status, List<Long> aInfo) {
//        for (int i = 0; i < aPlayer.size(); i++) {
//            aProtoUnitState.add(protoState(aPlayer.get(i).getId(), List.of(status), List.of(aInfo.size()), aInfo));
//        }
//    }
//
//    public void protoRoomState(StateType status, Integer... data) {
//        for (int i = 0; i < aPlayer.size(); i++) {
//            aProtoUnitState.add(protoState(aPlayer.get(i).getId(), List.of(status), GsonUtil.toListLong(Arrays.asList(data))));
//        }
//    }


//    public void protoRoomState(List<StateType> aStatus, List<List<Long>> aInfo) {
//        List<Integer> aSize = new ArrayList<>();
//        List<Long> data = new ArrayList<>();
//        for (int j = 0; j < aInfo.size(); j++) {
//            aSize.add(aInfo.get(j).size());
//            data.addAll(aInfo.get(j));
//        }
//        for (int i = 0; i < aPlayer.size(); i++) {
//            aProtoUnitState.add(protoState(aPlayer.get(i).getId(), aStatus, aSize, data));
//        }
//    }
}
