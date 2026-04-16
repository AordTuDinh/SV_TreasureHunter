package game.treasure.controller;

import game.battle.model.Player;
import game.battle.object.Pos;
import game.battle.type.AutoMode;
import game.config.CfgBattle;
import game.config.CfgServer;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.treasure.mapping.UserItemEntity;
import game.treasure.mapping.UserSettingsEntity;
import game.treasure.mapping.main.*;
import game.treasure.server.Constans;
import game.treasure.service.resource.ResMap;
import game.treasure.service.user.Bonus;
import game.treasure.table.*;
import game.object.MyUser;
import game.object.TaskMonitor;
import game.protocol.CommonProto;
import io.netty.channel.Channel;
import ozudo.base.helper.*;
import ozudo.base.log.Logs;
import protocol.Pbmethod.CommonVector;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BattleHandler extends AHandler implements Serializable {
    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(SERVER_INFO, INIT_MAP, CAMPAIGN_DATA, CAMPAIGN_REWARD,
                CAMPAIGN_SMART,  JOIN_MAP, BOSS_GOD_DATA, REVIVE_PLAYER, CHANGE_AUTO_MODE, CHANGE_ITEM_SLOT,
                CHANGE_CHANEL, SMART_BOSS, CHANGE_AUTO_SLOT);
        actions.forEach(action -> mHandler.put(action, this));
    }

    static BattleHandler instance;

    public static BattleHandler getInstance() {
        if (instance == null) {
            instance = new BattleHandler();
        }
        return instance;
    }

    @Override
    public AHandler newInstance() {
        return new BattleHandler();
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        try {
            switch (actionId) {
                case SERVER_INFO -> serverInfo();
                case INIT_MAP -> initMap();
                case JOIN_MAP -> joinMap();
                case REVIVE_PLAYER -> revivePlayer();
                case CHANGE_AUTO_MODE -> changeMode();
                case CHANGE_ITEM_SLOT -> changeItemSlot();
                case CHANGE_AUTO_SLOT -> changeAutoSlot();
//                case CHANGE_CHANEL -> changeChanel();
//                case SMART_BOSS -> smartBoss();
                case BOSS_GOD_DATA -> bossGodData();

            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    private void initMap() {
        CommonVector pbUB = CommonProto.parseCommonVector(requestData);
        int type = Math.toIntExact(pbUB.getALong(0));
        InitMapType initMapType = InitMapType.get(type);
        if (initMapType == null) {
            addErrParam();
            return;
        }
        int popupType = Math.toIntExact(pbUB.getALong(1));
        PopupType pType =   PopupType.get(popupType);
        if (initMapType == InitMapType.ROOMTYPE) {
            initMapByTypeId(MapType.get(type), Pos.zero(),pType);
        }
    }

    void serverInfo() {
        // todo : phần connect vào sv mới sẽ làm sau
        CommonVector pbUB = CommonProto.parseCommonVector(requestData);
        int mode = (int) pbUB.getALong(0);
        int mapId = (int) pbUB.getALong(0);
        int subMapId = (int) pbUB.getALong(1);
        CommonVector.Builder builder = CommonVector.newBuilder();
        List<String> svInfo = CfgBattle.getInfoServer(mode);
        builder.addALong(mode);
        builder.addALong(mapId);
        builder.addALong(subMapId);
        builder.addAString(svInfo.get(0));
        builder.addAString(svInfo.get(1));
        addResponse(builder.build());
    }


    public void initMapByTypeId(MapType mapType, Pos posInit, PopupType popupType) {
        ResMapEntity map = ResMap.getMap(mapType);
        if (map == null) {
            addErrParam();
            return;
        }
        int chanelId = mUser.getRoomChanelId();
        BaseRoom curRoom = (BaseRoom) ChUtil.get(channel, ChUtil.KEY_ROOM);
        String curKeyRoom = curRoom != null ? Constans.mIdToBattleId.get(curRoom.getBattleId()) : "";
        String keyRoom = CfgBattle.getKeyRoom(mUser, mapType.value, chanelId);

        if (curRoom != null && curKeyRoom.equals(keyRoom)) {
            addErrResponse(getLang(Lang.err_in_room_already));
            return;
        }
        if (curRoom != null && !curRoom.allowChangeChanel()) {
            addErrResponse(getLang(Lang.err_unauthorized));
            return;
        }
        // xóa khỏi room cũ
        Player player = mUser.getPlayer();
        if (curRoom != null && curRoom.hasPlayer(player.getId())) {
            curRoom.removeUnit(player.getId());
        }
        // tìm room thỏa mãn điều kiện max player room
        BaseRoom room = (BaseRoom) TaskMonitor.getInstance().getRoom(keyRoom);
        while (room != null && room.isMaxPlayer()) {
            chanelId = NumberUtil.getRandom(1, CfgServer.maxChannelOpen);
            keyRoom = CfgBattle.getKeyRoom(mUser, mapType.value, chanelId);
            if (curKeyRoom.equals(keyRoom)) continue;
            room = (BaseRoom) TaskMonitor.getInstance().getRoom(keyRoom);
        }
        // check có room hay chưa, có rồi thì join
        player.clearDataForChangeRoom(posInit);
        player.resetData();

        if (room == null) {
            switch (mapType) {
                case HOME:
                    room = new HomeRoom(map, map.getDataMap(), keyRoom);
                    break;
            }
            TaskMonitor.getInstance().addRoom(room);
        }
        mUser.setRoomChanelId(chanelId);
        ChUtil.set(channel, ChUtil.KEY_ROOM, room);
        // tra ve id teleport next
        addResponse(INIT_MAP, CfgBattle.genInitMap(mapType.value, mUser.getRoomChanelId(), popupType));
        if (mapType == MapType.HOME) {
            mUser.sendNotify();
        }
    }


    private void bossGodData() {
        addResponse(getCommonVector(mUser.getUData().getBossGod()));
    }

    void joinMap() {
        BaseRoom curRoom = (BaseRoom) ChUtil.get(channel, ChUtil.KEY_ROOM);
        if (curRoom == null) {
            addResponse(LOGIN_REQUIRE, null);
            return;
        }
        mUser.getPlayer().setJoinMap(curRoom);

        curRoom.joinRoom(this, mUser.getPlayer());
    }

    void revivePlayer() {
        int type = getInputInt();
        Player player = ((MyUser) ChUtil.get(channel, ChUtil.KEY_M_USER)).getPlayer();
        long checkTime = player.getTimeDie() + 4000;
        int per5 = (int) (mUser.getUser().getExp() * 0.1);
        if (type == 0 && System.currentTimeMillis() < checkTime) { // backHome
            player.revive();
            if (mUser.getUser().getExp() >= per5)
                addBonusPrivate(Bonus.receiveListItem(mUser, DetailActionType.REVIVE_FEE_10.getKey(), Bonus.viewExp(-per5)));
            initMapByTypeId(MapType.HOME,mUser.getCachePos(),PopupType.NULL);
        } else {
            List<Long> fee = Bonus.viewItem(ItemKey.VE_HOI_SINH, -1);
            String err = Bonus.checkMoney(mUser, fee);
            if (err != null) {
                player.revive();
                if (mUser.getUser().getExp() >= per5)
                    addBonusPrivate(Bonus.receiveListItem(mUser, DetailActionType.REVIVE_FEE_10.getKey(), Bonus.viewExp(-per5)));
                initMapByTypeId(MapType.HOME,mUser.getCachePos(),PopupType.NULL);
            } else {
                player.revive();
                addBonusToast(Bonus.receiveListItem(mUser, DetailActionType.REVIVE_PLAYER.getKey(), fee));
            }

        }
    }


    void changeMode() {
        CommonVector cmm = CommonProto.parseCommonVector(requestData);
        AutoMode mode = AutoMode.get((int) cmm.getALong(0));
        if (mode == null) {
            // addErrResponse();
            return;
        }
        UserSettingsEntity uSetting = mUser.getUSetting();
        if (uSetting.getAutoMode() == mode.value) {
            //addErrResponse();
            return;
        }
        if (uSetting.changeMode(mode)) {
            Player player = ((MyUser) ChUtil.get(channel, ChUtil.KEY_M_USER)).getPlayer();
            player.setAutoMode(mode);
            addResponse(getCommonVector(mode.value));
        } else addErrResponse();
    }

    void changeItemSlot() {
        CommonVector cmm = CommonProto.parseCommonVector(requestData);
        int slot = (int) cmm.getALong(0);
        int itemId = (int) cmm.getALong(1);
        if (slot != 0 && slot != 1) {
            addErrResponse(getLang(Lang.err_slot));
            return;
        }
        UserSettingsEntity uSetting = mUser.getUSetting();
        List<Integer> itemSlot = uSetting.getItemSlot(mUser);
        if (itemId == 0) {
            if (itemSlot.get(slot * 2 + 1) == itemId) {
                addErrResponse(getLang(Lang.err_params));
                return;
            }
            itemSlot.set(slot * 2 + 1, itemId);
        } else {
            UserItemEntity item = mUser.getResources().getItem(itemId);
            if (item == null) {
                addErrResponse(getLang(Lang.err_params));
                return;
            }
            if (itemSlot.get(slot * 2 + 1) == itemId) {
                addErrResponse(getLang(Lang.err_item_has_been_used));
                return;
            }
            if (item.getNumber() <= 0) {
                addErrResponse(getLang(Lang.err_item_number));
                return;
            }
            for (int i = 0; i < itemSlot.size(); i += 2) {
                if (itemSlot.get(i + 1) == itemId) {
                    itemSlot.set(i + 1, 0);
                }
            }
            itemSlot.set(slot * 2 + 1, itemId);
        }
        if (uSetting.saveSlot(itemSlot)) {
            Player player = ((MyUser) ChUtil.get(channel, ChUtil.KEY_M_USER)).getPlayer();
            player.setItemsBuf(uSetting.getItemSlot(mUser));
            addResponse(getCommonIntVector(itemSlot));
        } else addErrResponse();
    }


    void changeAutoSlot() {
        CommonVector cmm = CommonProto.parseCommonVector(requestData);
        int slot = (int) cmm.getALong(0);
        int trigger = (int) cmm.getALong(1);
        if (trigger < 0 || trigger > 100) {
            addErrParam();
            return;
        }
        if (slot != 0 && slot != 1) {
            addErrResponse(getLang(Lang.err_slot));
            return;
        }
        UserSettingsEntity uSetting = mUser.getUSetting();
        if (uSetting.saveSlot(mUser, slot, trigger)) {
            Player player = ((MyUser) ChUtil.get(channel, ChUtil.KEY_M_USER)).getPlayer();
            player.setItemsBuf(uSetting.getItemSlot(mUser));
            addResponse(cmm);
        } else addErrResponse();
    }

//    void changeChanel() {
//        int inputChanel = getInputInt();
//        if ((inputChanel < 0 || inputChanel > CfgServer.maxChannelOpen) && inputChanel != user.getId()) {
//            addErrResponse(String.format(getLang(Lang.err_open_chanel), CfgServer.maxChannelOpen));
//            return;
//        }
//        BaseRoom room = mUser.getPlayer().getRoom();
//        if (room == null) {
//            addErrResponse(getLang(Lang.err_params));
//            return;
//        }
//        int curChanel = mUser.getRoomChanelId();
//        if (inputChanel == curChanel) {
//            addErrResponse(getLang(Lang.err_use_chanel));
//            return;
//        }
//
//        MapType mapType = MapType.get(room.getRoomTypeId());
//        if (!mapType.allowChangeChanel) {
//            addErrResponse(getLang(Lang.err_change_chanel));
//            return;
//        }
//        String keyRoom = CfgBattle.getKeyRoom(mUser, room.getRoomTypeId(), inputChanel);
//        BaseRoom curRoom = (BaseRoom) ChUtil.get(mUser.getChannel(), ChUtil.KEY_ROOM);
//        if (curRoom != null && curRoom.getKeyRoom().equals(keyRoom)) {
//            return;
//        }
//        room = (BaseRoom) TaskMonitor.getInstance().getRoom(keyRoom); // room mới
//        if (room != null && room.isMaxPlayer()) {
//            addErrResponse(getLang(Lang.err_full_player));
//            return;
//        }
//
//        // xóa khỏi room cũ
//        Player player = mUser.getPlayer();
//        if (curRoom != null && curRoom.hasPlayer(player.getId())) {
//            curRoom.removePlayer(player.getId());
//        }
//        if (room == null) {  // tao room moi
//            switch (MapType.get(curRoom.getRoomTypeId())) {
//                default:
//                    room = new HomeRoom(curRoom.getMapInfo(), , keyRoom);
//                    break;
//            }
//            room.addPlayer(player);
//            TaskMonitor.getInstance().addRoom(room);
//        } else {
//            // check số lượng người trong room
//            if (room.isMaxPlayer()) {
//                addErrResponse(getLang(Lang.err_full_player));
//                return;
//            }
//            room.addPlayer(player);
//
//        }
//        ChUtil.set(mUser.getChannel(), ChUtil.KEY_ROOM, room);
//        addResponse(null);
//        mUser.setRoomChanelId(inputChanel);
//        if (mapType == MapType.HOME) mUser.sendNotify();
//        // tra ve id teleport next
//        addResponse(INIT_MAP, CfgBattle.genInitMap(room.getRoomTypeId(), inputChanel, PopupType.NULL));
//    }

}
