package game.dragonhero.table;

import com.google.protobuf.AbstractMessage;
import game.battle.model.Character;
import game.battle.model.Pet;
import game.battle.model.Player;
import game.battle.object.*;
import game.battle.type.RoomState;
import game.config.CfgServer;
import game.battle.type.StateType;
import game.dragonhero.controller.AHandler;
import game.dragonhero.mapping.main.BaseMap;
import game.dragonhero.server.Constans;
import game.dragonhero.server.IAction;
import game.dragonhero.service.battle.ClientSendType;
import game.object.MyUser;
import game.protocol.ProtoState;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ozudo.base.helper.ChUtil;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.Util;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@NoArgsConstructor
public abstract class BaseRoom extends MonoRoom {
    @Getter
    List<Character> aPlayer;
    @Getter
    List<Character> aPet;
    float server_time;

    @Getter
    List<Character> aEnemy = new ArrayList<>();
    @Getter
    @Setter
    BaseMap mapInfo;
    int idNext = 0;
    @Getter
    boolean allowReviveEnemy;
    @Getter
    boolean isBattleRoom;

    public BaseRoom(BaseMap mapInfo, List<Character> aPlayer, String keyRoom, boolean allowReviveEnemy) {
        super(keyRoom);
        this.idNext = 0;
        this.mapInfo = mapInfo;
        this.aPlayer = aPlayer;
        this.aPet = new ArrayList<>();
        for (int i = 0; i < aPlayer.size(); i++) {
            if(!aPlayer.get(i).isPlayer()) continue;
            Pet pet = aPlayer.get(i).getPlayer().getPet();
            if (pet != null) {
                pet.setId(getIdNext());
                aPet.add(pet);
            }
        }
        startInit();
        this.allowReviveEnemy = allowReviveEnemy;
        this.isBattleRoom = getSubId() > 0;
    }

    protected void startInit() {

    }


    protected void sendTableState() {
        byte[] data = genTableState();
        if (data == null) return;
        //System.out.println("send table state -------------------");
        for (int i = 0; i < aPlayer.size(); i++) {
            if (aPlayer.get(i) != null && aPlayer.get(i).isPlayer() &&  aPlayer.get(i).getPlayer().getMUser().getChannel() != null) {
                Util.sendGameData(aPlayer.get(i).getPlayer().getMUser().getChannel(), data, Constans.MAGIC_IN_PUT);
            }
        }
    }

    public List<Channel> getListChannel(){
        List<Channel> lst = new ArrayList<>();
        for (int i = 0; i < aPlayer.size(); i++) {
            if(!aPlayer.get(i).isPlayer()) continue;
            Player p = aPlayer.get(i).getPlayer();
            if(p!=null && p.getMUser().getChannel()!=null && p.getMUser().getChannel().isActive()){
                lst.add(p.getMUser().getChannel());
            }
        }
        return lst;
    }

    public synchronized int getIdNext() { // dùng để gen id cho character
        idNext++;
        if (idNext > 201190) idNext = 0;
        return idNext;
    }

    public boolean isMaxPlayer() {
        return aPlayer.size() >= roomType.maxPlayer;
    }

    protected byte[] genTableState() {
        int action = IAction.TABLE_STATE;// K dùng nhưng viết ở đây để referent
        protocol.Pbmethod.PbState.Builder builder = protocol.Pbmethod.PbState.newBuilder();
        builder.setServerTime(server_time);

        String debug = "";
        boolean send = false;
        for (int i = 0; i < aPlayer.size(); i++) {
            if (aPlayer.get(i) != null && aPlayer.get(i).isAlive()) {
                builder.addUnitPos(aPlayer.get(i).toProtoPos());
                send = true;
            }
        }
        for (int i = 0; i < aPet.size(); i++) {
            if (aPet.get(i) != null) {
//                System.out.println("join room pet id aPet.get(i).getId() = " + aPet.get(i).getId());
                builder.addUnitPos(aPet.get(i).toProtoPos());
                send = true;
            }
        }

        for (int i = 0; i < aEnemy.size(); i++) {
            if (aEnemy.get(i) != null && aEnemy.get(i).isAlive() && aEnemy.get(i).isMove()) {
                send = true;
                builder.addUnitPos(aEnemy.get(i).toProtoPos());
//                System.out.println("aEnemy.get(i).toProtoPos() = " + aEnemy.get(i).getPos().toString());
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

    public Character getPlayerId(int id) {
        for (int i = 0; i < aPlayer.size(); i++) {
            if (aPlayer.get(i).getId() == id) return aPlayer.get(i);
        }
        return null;
    }


    @Override
    public void Update() {
        long _dt = System.currentTimeMillis() - _dte;
        _dte = System.currentTimeMillis();
        local_time += _dt / 1000.0;
        server_time = local_time;
        // send data
        try {
            if (aPlayer.size() > 0) {
                sendTableState();
            } else cancelTask();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
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
        return roomType.allowChangeChanel;
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
            if(player.isAlive())            player.setPosAndDirection(input.playerPos, input.playerDirection);
//            player.getInputs().add(input);


        } else if (input.typeId == NInput.PET_MOVE && player.getPet() != null) {
            player.getPet().setPosAndDirection(input.petPos, input.petDirection);
        } else if (input.typeId == NInput.INPUT_SLOT) {
            if (player.beBlock()) return;
            if (player.hasSkillIndex(input.skillIndex) && player.hasActiveSkill()) {
                Pos direction = input.targetDirection.normalized();
                if (!player.isLikeFace(direction) || player.getDirection().equals(Pos.zero())) {
                    player.setDirection(direction.clone());
                }
                addBullet(player, input.skillIndex, player.activeSkillByDirection(input.skillIndex, input.targetDirection.normalized()));
                player.activeSkill(input.skillIndex);
            }
            int[] slots = input.slotActive;
            for (int i = 0; i < slots.length; i++) {
                if (!player.isAlive()) return;
                if (slots[i] > -1 && player.hasUseItemIndex(i) && player.hasUseItem(i)) {
                    player.setTimeUseItem(i);
                    player.useItem(i);
                }
            }
        } else if (input.typeId == NInput.PING_GAME) {
            Util.sendProtoData(player.getMUser().getChannel(), null, IAction.PING_GAME);
        } else if (input.typeId == NInput.CLIENT_STATE) {
            clientSendState(input.clientState);
        }
    }

    public synchronized void addBullet(Character attacker, int skillId, List<Bullet> lstB) {
    }

    private void clientSendState(Pbmethod.PbUnitState.Builder pb) {
        ClientSendType clientType = ClientSendType.get(pb.getId());
        if (clientType == null) return;
        switch (clientType) {
            case EFFECT -> processClientEffect(pb.getStatusList(), pb.getPointList());

        }
    }

    protected void processClientEffect(List<Integer> types, List<Long> data) {

    }


    public void joinMap(AHandler handler) {
        if (roomState != RoomState.ACTIVE) return;
        pbInit.clearAMonster();
        pbInit.clearAPlayer();
        pbInit.clearAPet();
        for (int i = 0; i < aPlayer.size(); i++) {
            pbInit.addAPlayer(aPlayer.get(i).toProtoAdd());
        }
        for (int i = 0; i < aPet.size(); i++) {
            pbInit.addAPet(aPet.get(i).toProtoAdd());
        }
        for (int i = 0; i < aEnemy.size(); i++) {
            pbInit.addAMonster(aEnemy.get(i).toProtoAdd());
        }
        handler.addResponse(IAction.JOIN_MAP, pbInit.build());
    }

    public void sendDataAllUser(int service, AbstractMessage data) {
        for (int i = 0; i < aPlayer.size(); i++) {
            if(!aPlayer.get(i).isPlayer()) continue;
            Player p = aPlayer.get(i).getPlayer();
            if (p != null) {
                Util.sendProtoData(p.getMUser().getChannel(), data, service);
            }
        }
    }


    public void characterDie(Character character) {
    }

    public boolean hasPlayer(int userId) {
        for (int i = 0; i < aPlayer.size(); i++) {
            if (aPlayer.get(i).getId() == userId) return true;
        }
        return false;
    }

    public void addPet(Pet pet) { // add and send
        aProtoAdd.add(pet.toProtoAdd());
        aPet.add(pet);
    }

    public void changePet(Pet oldPet, Pet newPet) {
        aPet.remove(oldPet);
        newPet.setId(oldPet.getId());
        aPet.add(newPet);
    }

    public void removePet(Pet pet) { // remove and send
        aProtoAdd.add(pet.toProtoRemove());
        aPet.remove(pet);
    }


    public void addPlayer(Player player) {
        //debug("---------------------- >add player " + player.getName() + " to room " + getKeyRoom() + "  --- pos: " + player.getPos());
        if (roomState != RoomState.ACTIVE && roomState != RoomState.PAUSE) return;
        aPlayer.add(player);
        player.setRoom(this);
        PanelMap panel = new PanelMap(mapInfo.getMapData());
        player.setPanelMap(panel);
        aProtoAdd.add(player.toProtoAdd());
        Pet pet = player.getPet();
        if (pet != null) {
            pet.setId(getIdNext());
            pet.setRoom(this);
            aPet.add(pet);
            aProtoAdd.add(pet.toProtoAdd());
        }
    }

//    public void removeCharacter(Character character) {
//        debug("Remove character ---------------------- " + character.getName() + " for room : " + cacheBattle.getId());
//        // bao nhung thang khac cua room xoa no di
//        protocol.Pbmethod.CommonVector.Builder pbLeave = protocol.Pbmethod.CommonVector.newBuilder();
//        pbLeave.addALong(cacheBattle.getId());
//        pbLeave.addALong(character.getId());
//        pbLeave.addALong(cacheBattle.getMapInfo().getId());
//
//        if (character.isPlayer()) {
//            character.setReady(false);
//            aPlayer.remove(character);
//        } else aMonster.remove(character);
//        aProtoAdd.add(character.protoRemove());
//    }

    public void removePlayer(int userId) {
       // debug("Remove character ---------------------- " + userId + " for room : " + keyRoom);
        // bao nhung thang khac cua room xoa no di
        protocol.Pbmethod.CommonVector.Builder pbLeave = protocol.Pbmethod.CommonVector.newBuilder();
        pbLeave.addALong(getRoomType());
        pbLeave.addALong(userId);
        pbLeave.addALong(mapInfo != null ? mapInfo.getId() : 0);
        Character playerRemove = aPlayer.stream().filter(player -> player.getId() == userId).findAny().orElse(null);
        if (playerRemove != null) {
            aPlayer.remove(playerRemove);
            aPet.remove(playerRemove.getPet());
            aProtoAdd.add(playerRemove.toProtoRemove());
            if (playerRemove.getPet() != null) aProtoAdd.add(playerRemove.getPet().toProtoRemove());
        }
        if (aPlayer.isEmpty()) cancelTask();
    }


    protected void cancelTask() {
    }

    public int getRoomType() { // = id map
        return Integer.parseInt(keys[1]);
    }

    public int getSubId() {
        return Integer.parseInt(keys[2]);
    }

    public int getChannelId() {
        return Integer.parseInt(keys[3]);
    }

    public void protoRoomState(StateType status, int size, List<Long> aInfo) {
        for (int i = 0; i < aPlayer.size(); i++) {
            aProtoUnitState.add(protoState(aPlayer.get(i).getId(), List.of(status), List.of(size), aInfo));
        }
    }

    public void protoRoomState(StateType status, List<Long> aInfo) {
        for (int i = 0; i < aPlayer.size(); i++) {
            aProtoUnitState.add(protoState(aPlayer.get(i).getId(), List.of(status), List.of(aInfo.size()), aInfo));
        }
    }

    public void protoRoomState(StateType status, Integer... data) {
        for (int i = 0; i < aPlayer.size(); i++) {
            aProtoUnitState.add(protoState(aPlayer.get(i).getId(), List.of(status), GsonUtil.toListLong(Arrays.asList(data))));
        }
    }

//    public void protoOneRoomState(StateType status, Long... info) {
//        for (int i = 0; i < aPlayer.size(); i++) {
//            aProtoUnitState.add(protoState(aPlayer.get(i).getId(), List.of(status), List.of(status.length), Arrays.asList(info)));
//        }
//    }

    public void protoRoomState(List<StateType> aStatus, List<List<Long>> aInfo) {
        List<Integer> aSize =  new  ArrayList<>();
        List<Long> data = new   ArrayList<>();
        for (int j = 0; j < aInfo.size(); j++) {
            aSize.add( aInfo.get(j).size());
            data.addAll(aInfo.get(j));
        }
        for (int i = 0; i < aPlayer.size(); i++) {
            aProtoUnitState.add(protoState(aPlayer.get(i).getId(), aStatus, aSize,  data));
        }
    }
}
