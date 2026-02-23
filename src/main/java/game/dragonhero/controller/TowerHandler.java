package game.dragonhero.controller;

import game.battle.model.Character;
import game.battle.model.Player;
import game.config.*;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.dragonhero.mapping.UserTowerEntity;
import game.dragonhero.mapping.main.BaseMap;
import game.dragonhero.mapping.main.ResTowerEntity;
import game.dragonhero.server.IAction;
import game.dragonhero.service.Services;
import game.dragonhero.service.resource.ResTower;
import game.dragonhero.service.user.Bonus;
import game.dragonhero.table.BaseRoom;
import game.dragonhero.table.TowerRoom;
import game.object.DataQuest;
import game.object.TaskMonitor;
import io.netty.channel.Channel;
import ozudo.base.helper.ChUtil;
import ozudo.base.helper.DateTime;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TowerHandler extends AHandler {
    @Override
    public AHandler newInstance() {
        return new TowerHandler();
    }

    UserTowerEntity uTower;
    static AHandler instance;

    public static AHandler getInstance() {
        if (instance == null) {
            instance = new TowerHandler();
        }
        return instance;
    }

    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(TOWER_STATUS, TOWER_BUY_TURN, TOWER_ATTACK, TOWER_SMART);
        actions.forEach(action -> mHandler.put(action, this));
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        checkTimeMonitor("s");
        uTower = Services.userDAO.getUserTower(mUser);
        if (!CfgFeature.isOpenFeature(FeatureType.TOWER, mUser, this)) {
            return;
        }
        if (uTower == null) {
            addErrSystem();
            return;
        }
        uTower.checkKey();
        try {
            switch (actionId) {
                case TOWER_STATUS -> status();
                case TOWER_BUY_TURN -> buyTurn();
                case TOWER_ATTACK -> attack();
                case TOWER_SMART -> smart();
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    private void status() {
        Pbmethod.CommonVector.Builder cmm = Pbmethod.CommonVector.newBuilder();
        cmm.addALong(CfgTower.config.open ? 1L : 0L);
        cmm.addALong(uTower.getLevel());
        cmm.addALong(uTower.getNumberKey());
        long timeCD = CfgTower.getCountdown(uTower);
        cmm.addALong(timeCD);
        long time = 0;
        if (uTower.getNumberKey() < CfgTower.config.maxKey) {
            time = (CfgTower.config.maxKey - 1 - uTower.getNumberKey()) * DateTime.HOUR_SECOND * CfgTower.config.timeReceiveKey + timeCD;
        }
        cmm.addALong(time);
        cmm.addALong(CfgTower.getMaxBuy(uTower));
        addResponse(IAction.TOWER_STATUS, cmm.build());
    }

    private void buyTurn() {
        int number = getInputInt();
        if (number > CfgTower.getMaxBuy(uTower)) {
            addErrResponse(getLang(Lang.err_max_buy));
            return;
        }
        List<Long> fee = Bonus.xBonus(Bonus.reverseBonus(CfgTower.config.feeBuyKey), number);
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        List<Long> bonus = Bonus.receiveListItem(mUser, DetailActionType.TOWER_BUY_KEY_.getKey(number), fee);
        if (bonus.isEmpty()) {
            addErrSystem();
            return;
        }
        if (uTower.getNumberKey() + number > CfgTower.config.maxKey) {
            if (uTower.update(Arrays.asList("number_key", uTower.getNumberKey() + number, "number_buy", uTower.getNumberBuy() + number, "countdown", 0))) {
                uTower.setNumberBuy(uTower.getNumberBuy() + number);
                uTower.setNumberKey(uTower.getNumberKey() + number);
                uTower.setCountdown(0L);
                addResponse(getCommonVector(bonus));
                status();
            } else {
                Bonus.receiveListItem(mUser, DetailActionType.UPDATE_FAIL.getKey(), Bonus.reverseBonus(fee));
                addErrResponse(err);
            }
        } else {
            if (uTower.update(Arrays.asList("number_key", uTower.getNumberKey() + number, "number_buy", uTower.getNumberBuy() + number))) {
                uTower.setNumberBuy(uTower.getNumberBuy() + number);
                uTower.setNumberKey(uTower.getNumberKey() + number);
                addResponse(getCommonVector(bonus));
                status();
            } else {
                Bonus.receiveListItem(mUser, DetailActionType.UPDATE_FAIL.getKey(), Bonus.reverseBonus(fee));
                addErrResponse(err);
            }
        }

    }

    private void attack() {
        int curLevel = uTower.getLevel();
        int nextLevel = curLevel == 1 ? 1 : curLevel++;
        if (uTower.getNumberKey() < 1) {
            addErrResponse(getLang(Lang.err_number_item_key));
            return;
        }
        if (curLevel > ResTower.maxLevelTower) {
            addErrResponse(getLang(Lang.err_max_level_tower));
            return;
        }
        RoomType roomType = RoomType.TOWER;
        String keyRoom = CfgBattle.getKeyRoom(mUser, roomType.value, nextLevel, user.getId());
        BaseRoom curRoom = (BaseRoom) ChUtil.get(channel, ChUtil.KEY_ROOM);
        if (curRoom != null && curRoom.getKeyRoom().equals(keyRoom)) {
            return;
        }
        if (uTower.minusKey(1)) {
            // xóa khỏi room cũ
            Player player = mUser.getPlayer();
            if (curRoom != null && curRoom.hasPlayer(player.getId())) {
                curRoom.removePlayer(player.getId());
            }
            BaseMap baseMap = ResTower.mTower.get(curLevel);
            List<Character> players = new ArrayList<>();
            player.clearDataForChangeRoom(CfgTower.getPosPlayer());
            players.add(player);
            BaseRoom room = new TowerRoom(baseMap, players, keyRoom, nextLevel);
            TaskMonitor.getInstance().addRoom(room);
            ChUtil.set(channel, ChUtil.KEY_ROOM, room);
            addResponse(CfgBattle.genInitMap(roomType.value, nextLevel, 0, baseMap.getMapData().getPlayerCollider(), true, PopupType.NULL));
            status();
        } else addErrSystem();
    }

    private void smart() {
        int numberSmash = getInputInt();
        if (numberSmash <= 0) {
            addErrParam();
            return;
        }
        if (uTower.getNumberKey() < numberSmash) {
            addErrResponse(getLang(Lang.err_number_item_key));
            return;
        }
        int curLevel = uTower.getLevel();
        if (curLevel <= 0) {
            addErrResponse(getLang(Lang.need_to_pass_first));
            return;
        }
        ResTowerEntity resTower = ResTower.mTower.get(curLevel);
        List<Long> bonus = resTower.getABonusSmart();
        bonus.addAll(CfgEventDrop.bonusDrop(CfgEventDrop.config.getRateDropTower(), numberSmash ));
        if (uTower.minusKey(numberSmash)) {
            addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.TOWER_SMART.getKey(numberSmash), Bonus.xBonus(bonus, numberSmash))));
            status();
            CfgQuest.addNumQuest(mUser, DataQuest.SMASH_TOWER, numberSmash);
            mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.SMART_TOWER, numberSmash);
        } else addErrSystem();
    }
}
