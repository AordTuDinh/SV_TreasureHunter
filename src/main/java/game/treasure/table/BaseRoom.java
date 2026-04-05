package game.treasure.table;

import com.google.protobuf.AbstractMessage;
import game.battle.model.*;
import game.battle.model.Unit;
import game.battle.object.*;
import game.battle.type.RoomState;
import game.config.CfgServer;
import game.config.aEnum.MapType;
import game.treasure.controller.AHandler;
import game.treasure.mapping.main.ResMapEntity;
import game.treasure.server.Constans;
import game.treasure.server.IAction;
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

    float serverTime;
    @Getter
    @Setter
    ResMapEntity mapInfo;
    int idNext;

    public BaseRoom(ResMapEntity mapInfo, Map<Integer, ChunkObject> mChunk, String keyRoom) {
        super(keyRoom);
        this.mapInfo = mapInfo;
        this.mChunk = mChunk;
        // gen chunk default
        for (Integer key : mChunk.keySet()) {
            chunkCharacter.put(key, new HashSet<>());
        }
        for (int y = mapInfo.getMinChunkY(); y <= mapInfo.getMaxChunkY(); y++) {
            for (int x = mapInfo.getMinChunkX(); x <= mapInfo.getMaxChunkX(); x++) {
                int centerId = mapInfo.chunkPosToId(x, y);
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
        pbInitMap.setMapId(Constans.mIdToBattleId.get(battleId));
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
        Map<Integer, byte[]> chunkViewData = new HashMap<>();

        // 1. cache unit theo chunk (convert 1 lần)
        Map<Integer, List<Pbmethod.PbUnitPos>> chunkUnitPosMap = new HashMap<>();
        for (Map.Entry<Integer, Set<Long>> entry : chunkCharacter.entrySet()) {
            List<Pbmethod.PbUnitPos> list = new ArrayList<>();
            for (Long unitId : entry.getValue()) {
                Unit u = mUnit.get(unitId);
                if (u != null && u.isAlive() && u.isMove()) {
                    list.add(u.toProtoPos());
                }
            }
            chunkUnitPosMap.put(entry.getKey(), list);
        }

        // copy event
        List<Pbmethod.PbUnit> protoChange = new ArrayList<>(aProtoChange);
        aProtoChange.clear();

        List<Pbmethod.PbUnitState> protoUnitStateCopy = new ArrayList<>(aProtoUnitState);
        aProtoUnitState.clear();

        // 2. build data cho từng chunk
        for (Integer chunkId : chunkCharacter.keySet()) {
            protocol.Pbmethod.PbState.Builder builder = protocol.Pbmethod.PbState.newBuilder();
            builder.setServerTime(serverTime);

            List<Integer> visibleChunks = visibleByChunkId.get(chunkId);
            Set<Long> added = new HashSet<>();

            for (Integer vChunk : visibleChunks) {
                List<Pbmethod.PbUnitPos> list = chunkUnitPosMap.get(vChunk);
                if (list != null) {
                    for (Pbmethod.PbUnitPos u : list) {
                        if (added.add((long) u.getId())) {
                            builder.addUnitPos(u);
                        }
                    }
                }
            }

            for (Pbmethod.PbUnit u : protoChange) {
                if (visibleChunks.contains(u.getChunkId())) {
                    builder.addUnitAdd(u);
                }
            }

            if (!protoUnitStateCopy.isEmpty()) {
                builder.addAUnitUpdate(
                        ProtoState.protoUnitUpdate(
                                Constans.TYPE_UPDATE_CHARACTER,
                                ProtoState.protoListCharacterState(protoUnitStateCopy)
                        )
                );
            }

            chunkViewData.put(chunkId,
                    ProtoState.convertProtoBuffToState(builder.build())
            );
        }

        return chunkViewData;
    }

//    protected byte[] genTableState() {
//        int action = IAction.TABLE_STATE;// K dùng nhưng viết ở đây để referent
//        protocol.Pbmethod.PbState.Builder builder = protocol.Pbmethod.PbState.newBuilder();
//        builder.setServerTime(serverTime);
//
//        String debug = "";
//        boolean send = false;
//
//        for (Unit unit : mUnit.values()) {
//            if (unit != null && unit.isAlive() && unit.isMove()) {
//                builder.addUnitPos(unit.toProtoPos());
//                send = true;
//            }
//        }
//        int size = aProtoAdd.size();
//        for (int i = 0; i < size; i++) {
//            // trả về cho những thằng trong chunk view thôi
//            builder.addUnitAdd(aProtoAdd.get(0));
//            aProtoAdd.remove(0);
//            send = true;
//        }
//        if (!aProtoUnitState.isEmpty()) {
//            builder.addAUnitUpdate(ProtoState.protoUnitUpdate(Constans.TYPE_UPDATE_CHARACTER, ProtoState.protoListCharacterState(aProtoUnitState)));
//            send = true;
//            aProtoUnitState.clear();
//        }
////        if (!debug.isEmpty()) System.out.println("debug = " + debug);
//        if (send) return ProtoState.convertProtoBuffToState(builder.build());
//        else return null;
//    }

    protected void debug(String msg) {
        if (CfgServer.isRealServer()) {
            System.out.println(msg);
        }
    }

    public Unit getPlayerId(long id) {
        return mUnit.getOrDefault(id, null);
    }


    @Override
    public void Update() {
        long _dt = System.currentTimeMillis() - _dte;
        _dte = System.currentTimeMillis();
        local_time += _dt / 1000.0;
        serverTime = local_time;
        // send data
//        try {
//            if (aPlayer.size() > 0) {
//                sendTableState();
//            } else cancelTask();
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//        }
//
//        for (int i = 0; i < getAPlayer().size(); i++) {
//            getAPlayer().get(i).Update();
//        }
//        for (int i = 0; i < getAEnemy().size(); i++) {
//            getAEnemy().get(i).Update();
//        }
    }


    public synchronized void doSyncAction(Channel channel, int actionId, byte[] srcRequest) {
        try {
            MyUser mUser = (MyUser) ChUtil.get(channel, ChUtil.KEY_M_USER);
            switch (actionId) {
                case IAction.CLIENT_INPUT:
                    if (roomState != RoomState.ACTIVE) return;
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
            //System.out.println("player.getTimeLastProcessInput() = " + player.getTimeLastProcessInput());
            float timeTravel = System.currentTimeMillis() - player.getTimeLastProcessInput();
            player.setTimeLastProcessInput(System.currentTimeMillis());
            player.setIndexLastInputSeq(lastInputSeq);
            //System.out.println("timeTravel = " + timeTravel / 1000);
            //System.out.println("BattleConfig.CHECK = " + BattleConfig.CHECK);
            //System.out.println("timeTravel ============= " + timeTravel);
            // không thể nhận với tốc độ lớn hơn tốc độ update của unity được (dôi ra 5ms lường trước mạng)
            //if (timeTravel < BattleConfig.CHECK) {
            //player.getMUser().addResponse(IAction.MSG_TOAST, CommonProto.getCommonVector("HACK SPEED"));
            // hack
            //  player.forceDie();
            // Util.sendProtoData(player.getChannel(), CommonProto.getCommonVectorProto(null, Arrays.asList(player.getUserName() + " sử dụng hack speed")), IAction.MSG_TOAST, System.currentTimeMillis());
            //Logs.warn("player " + player.getId() + " hack speed");
            //System.out.println("Hack speed");
            //} else {
            // check hack
//            double distance = input.playerPos.distance(player.getPos());
//            if (distance > 0.3f) {
//                //Util.sendProtoData(player.getMUser().getChannel(), CommonProto.getCommonVectorProto(List.of(0L), List.of(" Nghi vấn hack")), IAction.MSG_TOAST);
//                //Telegram.sendNotify(player.getId() + "  Nghi vấn hack distance == " + distance);
//            }
            if (player.isAlive()) player.setPosAndDirection(input.playerPos, input.playerDirection);
//            player.getInputs().add(input);


        } else if (input.typeId == NInput.PET_MOVE && player.getPetUse() != null) {
            player.getPetUse().setPosAndDirection(input.petPos, input.petDirection);
        } else if (input.typeId == NInput.INPUT_SLOT) {
            if (player.beBlock()) return;
            int[] slots = input.slotActive;
            for (int i = 0; i < slots.length; i++) {
                if (!player.isAlive()) return;
                if (slots[i] > -1 && player.hasUseItem(i)) {
                    player.setTimeUseItem(i);
                    player.useItem(i);
                }
            }
        } else if (input.typeId == NInput.PING_GAME) {
            Util.sendProtoData(player.getMUser().getChannel(), null, IAction.PING_GAME);
        } else if (input.typeId == NInput.CLIENT_STATE) {

        }
    }


    public void joinChunk(Player player, int newChunk, int oldChunk) {
        Set<Long> oldSet = chunkCharacter.get(oldChunk);
        if (oldSet != null) oldSet.remove(player.getIdInMap());

        chunkCharacter.computeIfAbsent(newChunk, k -> new HashSet<>())
                .add(player.getIdInMap());

        aProtoChange.add(player.toProtoRemove(oldChunk));
        aProtoChange.add(player.toProtoAdd(newChunk));
    }

    public void joinMap(AHandler handler, Player player) {
        if (roomState != RoomState.ACTIVE) return;
        addCharacter(player);

        // trả về 9 chunk xung quanh player
        int curChunk = player.getChunkId();
        List<Integer> chunkVisible = visibleByChunkId.get(curChunk);
        protocol.Pbmethod.PbInitMap.Builder pbInit = newPbInitMap();

        for (int i = 0; i < chunkVisible.size(); i++) {
            // add data chunks
            ChunkObject chunkData = mChunk.get(chunkVisible.get(i));
            pbInit.addChunks(chunkData.toProto());
            Set<Long> aCharInChunk = chunkCharacter.get(chunkVisible.get(i));
            for (Long charId : aCharInChunk) {
                Unit unit = mUnit.get(charId);
                pbInit.addUnits(unit.toProtoAdd());
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

    public int worldPosToChunkId(Pos pos) {
        return mapInfo.worldPosToChunkId(pos);
    }


    //  join chunk -> tất cả object đểu phải qua join chunk
    protected void addUnitToChunk(Pos newPos) {

    }

    // add mới từ khi instance ra 1 unit thì chạy qua đây
    public void addCharacter(Unit unit) {
        if (roomState != RoomState.ACTIVE && roomState != RoomState.PAUSE) return;
        long idInMap = getIdNext();
        unit.setIdInMap(idInMap);
        // tính chunk hiện tại nó đang ở
        int chunkId = worldPosToChunkId(unit.getPos());
        unit.setChunkId(chunkId);
        mUnit.put(idInMap, unit);
        chunkCharacter.get(chunkId).add(idInMap);
        aProtoChange.add(unit.toProtoAdd());

        if (unit.isPlayer()) {
            Pet pet = unit.getPlayer().getPetUse();
            if (pet != null) addCharacter(pet);
        }
    }

    public void removeCharacter(Unit unit) {
        debug("Remove character ---------------------- " + unit.getName() + " for room : " + getBattleId());
        // bao nhung thang khac cua room xoa no di
        protocol.Pbmethod.CommonVector.Builder pbLeave = protocol.Pbmethod.CommonVector.newBuilder();
        pbLeave.addALong(battleId);
        pbLeave.addALong(unit.getIdInMap());
        pbLeave.addALong(getMapInfo().getId());

        if (unit.isPlayer()) {
            unit.setReady(false);
            aPlayerIds.remove(unit.getIdInMap());
        }


        aProtoChange.add(unit.toProtoRemove(unit.getChunkId()));
    }

    public void removePlayer(long idInMap) {
        // debug("Remove character ---------------------- " + userId + " for room : " + keyRoom);
        // bao nhung thang khac cua room xoa no di
        protocol.Pbmethod.CommonVector.Builder pbLeave = protocol.Pbmethod.CommonVector.newBuilder();
        pbLeave.addALong(getRoomTypeId());
        pbLeave.addALong(idInMap);
        pbLeave.addALong(mapInfo != null ? mapInfo.getId() : 0);

//        Character playerRemove = aPlayer.stream().filter(player -> player.getId() == userId).findAny().orElse(null);
//        if (playerRemove != null) {
//            aPlayer.remove(playerRemove);
//            aPet.remove(playerRemove.getPet());
//            aProtoAdd.add(playerRemove.toProtoRemove());
//            if (playerRemove.getPet() != null) aProtoAdd.add(playerRemove.getPet().toProtoRemove());
//        }
//        if (aPlayer.isEmpty()) cancelTask();
    }


    protected void cancelTask() {
    }

    public int getRoomTypeId() { // = id map
        return Integer.parseInt(Constans.getKeyRoomById(battleId)[1]);
    }

    public MapType getRoomType() { // = id map
        return MapType.get(Integer.parseInt(Constans.getKeyRoomById(battleId)[1]));
    }

    public int getChannelId() {
        return Integer.parseInt(Constans.getKeyRoomById(battleId)[2]);
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
