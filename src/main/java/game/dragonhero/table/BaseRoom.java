package game.dragonhero.table;

import com.google.protobuf.AbstractMessage;
import game.battle.model.*;
import game.battle.model.Character;
import game.battle.object.*;
import game.battle.type.RoomState;
import game.config.CfgServer;
import game.config.aEnum.MapType;
import game.dragonhero.controller.AHandler;
import game.dragonhero.mapping.main.ResMapEntity;
import game.dragonhero.server.Constans;
import game.dragonhero.server.IAction;
import game.object.MyUser;
import game.protocol.ProtoState;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ozudo.base.helper.ChUtil;
import ozudo.base.helper.Util;
import ozudo.base.log.Logs;

import java.util.*;

@NoArgsConstructor
public abstract class BaseRoom extends MonoRoom {
    // static data
    Map<Integer, ChunkObject> mChunk = new HashMap<>();
    Map<Integer, List<Integer>> visibleByChunkId = new HashMap<>();  // chunk id -> list visible chunk by id chunk
    // runtime
    Map<Long, Character> mCharacter = new HashMap<>();
    List<Long> aPlayerIds = new ArrayList<>();
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
        byte[] data = genTableState();
        if (data == null) return;
        //System.out.println("send table state -------------------");
        for (int i = 0; i < aPlayerIds.size(); i++) {
            Character player = mCharacter.get(aPlayerIds.get(i));
            if (player != null && player.isPlayer() && player.getPlayer().getMUser().getChannel() != null) {
                Util.sendGameData(player.getPlayer().getMUser().getChannel(), data, Constans.MAGIC_IN_PUT);
            }
        }
    }

    public List<Channel> getListChannel() {
        List<Channel> lst = new ArrayList<>();
        for (int i = 0; i < aPlayerIds.size(); i++) {
            Player p = mCharacter.get(aPlayerIds.get(i)).getPlayer();
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


    protected byte[] genTableState() {
        int action = IAction.TABLE_STATE;// K dùng nhưng viết ở đây để referent
        protocol.Pbmethod.PbState.Builder builder = protocol.Pbmethod.PbState.newBuilder();
        builder.setServerTime(serverTime);

        String debug = "";
        boolean send = false;

        for (Character character : mCharacter.values()) {
            if ( character != null && character.isAlive()) {
                builder.addUnitPos(character.toProtoPos());
                send = true;
            }
        }

        int size = aProtoAdd.size();
        for (int i = 0; i < size; i++) {
            builder.addUnitAdd(aProtoAdd.get(0));
            aProtoAdd.remove(0);
            send = true;
        }
        if (!aProtoUnitState.isEmpty()) {
            builder.addAUnitUpdate(ProtoState.protoUnitUpdate(Constans.TYPE_UPDATE_CHARACTER, ProtoState.protoListCharacterState(aProtoUnitState)));
            send = true;
            aProtoUnitState.clear();
        }
//        if (!debug.isEmpty()) System.out.println("debug = " + debug);
        if (send) return ProtoState.convertProtoBuffToState(builder.build());
        else return null;
    }

    protected void debug(String msg) {
        if (CfgServer.isRealServer()) {
            System.out.println(msg);
        }
    }

    public Character getPlayerId(long id) {
        return mCharacter.getOrDefault(id, null);
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


    public void joinMap(AHandler handler, Player player) {
        if (roomState != RoomState.ACTIVE) return;
        addCharacter(player);

        // trả về 9 chunk xung quanh player
        int curChunk = player.getChunkId();
        List<Integer> chunkVisible = visibleByChunkId.get(curChunk);

        pbInit.clearUnits();
        for (int i = 0; i < chunkVisible.size() ; i++) {
            Set<Long> aCharInChunk = chunkCharacter.get(chunkVisible.get(i));
            for (Long charId : aCharInChunk) {
                Character unit = mCharacter.get(charId);
                pbInit.addUnits(unit.toProtoAdd());
            }
        }
        handler.addResponse(IAction.JOIN_MAP, pbInit.build());
    }

    public void sendDataAllUser(int service, AbstractMessage data) {
        for (int i = 0; i < aPlayerIds.size(); i++) {
            Player p = mCharacter.get(aPlayerIds.get(i)).getPlayer();
            if (p != null) {
                Util.sendProtoData(p.getMUser().getChannel(), data, service);
            }
        }
    }


    public void characterDie(Character character) {
    }

    public boolean hasPlayer(long userId) {
        return mCharacter.containsKey(userId);
    }


//    public void changePet(Pet oldPet, Pet newPet) {
//        aPet.remove(oldPet);
//        newPet.setId(oldPet.getId());
//        aPet.add(newPet);
//    }
//
//    public void removePet(Pet pet) { // remove and send
//        aProtoAdd.add(pet.toProtoRemove());
//        aPet.remove(pet);
//    }


    public int worldPosToChunkId(Pos pos) {
        return mapInfo.worldPosToChunkId(pos);
    }


    // trả cho các thằng player khác để add vào
    public void addCharacter(Character character) {
        if (roomState != RoomState.ACTIVE && roomState != RoomState.PAUSE) return;
        long idMap = getIdNext();
        character.setIdInMap(idMap);
        // tính chunk hiện tại nó đang ở
        int chunkId = worldPosToChunkId(character.getPos());
        character.setChunkId(chunkId);

        mCharacter.put(idMap,character);
        chunkCharacter.get(chunkId).add(idMap);

        aProtoAdd.add(character.toProtoAdd());

        if (character.isPlayer()) {
            Pet pet = character.getPlayer().getPetUse();
            if (pet != null) addCharacter(pet);
        }
    }

    public void removeCharacter(Character character) {
        debug("Remove character ---------------------- " + character.getName() + " for room : " + getBattleId());
        // bao nhung thang khac cua room xoa no di
        protocol.Pbmethod.CommonVector.Builder pbLeave = protocol.Pbmethod.CommonVector.newBuilder();
        pbLeave.addALong(battleId);
        pbLeave.addALong(character.getIdInMap());
        pbLeave.addALong(getMapInfo().getId());

        if (character.isPlayer()) {
            character.setReady(false);
            aPlayerIds.remove(character.getIdInMap());
        }


        aProtoAdd.add(character.toProtoRemove());
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
        return Integer.parseInt(Constans.getKeyRoomById(battleId) [1]);
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
