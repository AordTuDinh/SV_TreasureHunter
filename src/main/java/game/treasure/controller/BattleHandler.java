package game.treasure.controller;

import game.battle.model.Player;
import game.battle.object.Pos;
import game.config.CfgBattle;
import game.config.CfgServer;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.treasure.BattleConfig;
import game.treasure.mapping.main.*;
import game.treasure.service.resource.ResMap;
import game.treasure.server.IAction;
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
        List<Integer> actions = Arrays.asList(SERVER_INFO, INIT_MAP, JOIN_MAP, REVIVE_PLAYER);
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
        PopupType pType = PopupType.get(popupType);
        if (initMapType == InitMapType.ROOMTYPE) {
            initMapByTypeId(MapType.get(type), Pos.zero(), pType);
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
        BaseRoom curRoom = (BaseRoom) ChUtil.get(channel, ChUtil.KEY_ROOM);
        if (curRoom != null && !curRoom.allowChangeChanel()) {
            addErrResponse(getLang(Lang.err_unauthorized));
            return;
        }
        com.google.protobuf.AbstractMessage initMap = prepareInitMap(channel, mUser, mapType, posInit, popupType);
        if (initMap == null) {
            addErrParam();
            return;
        }
        addResponse(INIT_MAP, initMap);
    }

    /** Chuyển player sang map mới; trả payload INIT_MAP nếu thành công. */
    private static com.google.protobuf.AbstractMessage prepareInitMap(
            Channel channel, MyUser mUser, MapType mapType, Pos posInit, PopupType popupType) {
        ResMapEntity map = ResMap.getMap(mapType);
        if (map == null) {
            return null;
        }
        BaseRoom curRoom = (BaseRoom) ChUtil.get(channel, ChUtil.KEY_ROOM);
        String curKeyRoom = curRoom != null ? TaskMonitor.mBattleIdToKey.get(curRoom.getBattleId()) : "";
        String keyRoom = CfgBattle.getKeyRoom(mUser, mapType.value);
        Player player = mUser.getPlayer();
        if (curRoom != null && curRoom.hasPlayer(player.getId())) {
            curRoom.removeUnit(player.getId());
        }
        BaseRoom room = (BaseRoom) TaskMonitor.getInstance().getRoom(keyRoom);
        while (room != null && room.isMaxPlayer()) {
            keyRoom = CfgBattle.getKeyRoom(mUser, mapType.value);
            if (curKeyRoom.equals(keyRoom)) continue;
            room = (BaseRoom) TaskMonitor.getInstance().getRoom(keyRoom);
        }
        boolean restoreHome = mapType == MapType.HOME && posInit.equals(Pos.zero());
        boolean wasDead = restoreHome && mUser.isLastHomeDead();
        Pos spawn = posInit;
        if (restoreHome) {
            if (mUser.getUser().getBlockType() == BlockType.BLOCK_ACTION) {
                Pos jailSpawn = map.getJailSpawnPos();
                spawn = jailSpawn != null ? jailSpawn : Pos.zero();
            } else {
                spawn = mUser.getLastHomePos();
            }
        }
        if (restoreHome) {
            player.clearDataForHomeRejoin(spawn, wasDead);
            if (!wasDead) {
                player.resetDataKeepHp();
            }
        } else {
            player.clearDataForChangeRoom(posInit);
            player.resetData();
        }
        if (room == null) {
            switch (mapType) {
                case HOME:
                    room = new HomeRoom(map, map.getDataMap(), keyRoom);
                    break;
            }
            TaskMonitor.getInstance().addRoom(room);
        }
        ChUtil.set(channel, ChUtil.KEY_ROOM, room);
        if (mapType == MapType.HOME) {
            mUser.sendNotify();
        }
        return CfgBattle.genInitMap(mapType.value, popupType);
    }

    private static void reviveToHome(Channel channel, MyUser mUser, boolean pushToClient, BattleHandler responseHandler) {
        mUser.clearLastHomeState();
        Player player = mUser.getPlayer();
        player.revive();
        com.google.protobuf.AbstractMessage initMap = prepareInitMap(channel, mUser, MapType.HOME, Pos.zero(), PopupType.NULL);
        player.getPoint().resetHpPercent(BattleConfig.P_reviveHpPercent);
        if (initMap == null) {
            return;
        }
        if (pushToClient) {
            Util.sendProtoData(channel, initMap, IAction.INIT_MAP);
        } else if (responseHandler != null) {
            responseHandler.addResponse(INIT_MAP, initMap);
        }
    }


    void joinMap() {
        BaseRoom curRoom = (BaseRoom) ChUtil.get(channel, ChUtil.KEY_ROOM);
        if (curRoom == null) {
            addResponse(LOGIN_REQUIRE, null);
            return;
        }
        mUser.getPlayer().setJoinMap(curRoom);
        Player player = mUser.getPlayer();
        curRoom.joinRoom(this, player);
        if (!player.isAlive()) {
            player.protoStatus(protocol.Pbmethod.SubStateType.DIE);
        }
    }

    void revivePlayer() {
        reviveToHome(channel, mUser, false, this);
    }

    public static void teleportHomeOnUnblock(MyUser mUser, Channel channel) {
        if (mUser == null || mUser.getUser() == null || channel == null || !channel.isActive()) return;
        if (mUser.getUser().getBlockType() != BlockType.BLOCK_ACTION) return;
        mUser.getUser().setBlockType(0);
        reviveToHome(channel, mUser, true, null);
    }
}
