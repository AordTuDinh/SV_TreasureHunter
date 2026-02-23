package game.dragonhero.controller;

import game.battle.model.*;
import game.battle.model.Character;
import game.battle.object.Pos;
import game.config.CfgBattle;
import game.config.CfgClan;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.dragonhero.mapping.ClanEntity;
import game.dragonhero.mapping.UserClanEntity;
import game.dragonhero.mapping.main.BaseMap;
import game.dragonhero.mapping.main.ResBossEntity;
import game.dragonhero.mapping.main.ResClanSkillEntity;
import game.dragonhero.server.IAction;
import game.dragonhero.service.Services;
import game.dragonhero.service.resource.ResClan;
import game.dragonhero.service.resource.ResEnemy;
import game.dragonhero.service.resource.ResMap;
import game.dragonhero.service.user.Actions;
import game.dragonhero.service.user.Bonus;
import game.dragonhero.table.*;
import game.monitor.ClanManager;
import game.monitor.Online;
import game.object.TaskMonitor;
import game.protocol.CommonProto;
import io.netty.channel.Channel;
import ozudo.base.helper.ChUtil;
import ozudo.base.helper.Util;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ClanSkillHandler extends AHandler {
    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(CLAN_SKILL_STATUS, CLAN_SKILL_UPGRADE, CLAN_CHAT_LIST, CLAN_SKILL_RESET, CLAN_BOSS_STATUS, CLAN_BOSS_ACTIVE);
        actions.forEach(action -> mHandler.put(action, this));
    }

    static ClanSkillHandler instance;
    UserClanEntity userClan;
    ClanEntity clan;

    public static ClanSkillHandler getInstance() {
        if (instance == null) {
            instance = new ClanSkillHandler();
        }
        return instance;
    }

    @Override
    public AHandler newInstance() {
        return new ClanSkillHandler();
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        checkTimeMonitor("s");
        if (user.getClan() <= 0) return;
        clan = ClanManager.getInstance(user.getClan()).getClan();
        if (clan == null) {
            addErrResponse(getLang(Lang.need_join_clan_to_use));
            return;
        }
        userClan = Services.userDAO.getUserClan(mUser);
        if (userClan == null) {
            addErrResponse();
            return;
        }
        try {
            switch (actionId) {
                case IAction.CLAN_SKILL_STATUS -> status();
                case IAction.CLAN_SKILL_UPGRADE -> upgrade();
                case IAction.CLAN_SKILL_RESET -> reset();
                case IAction.CLAN_BOSS_STATUS -> bossStatus();
                case IAction.CLAN_BOSS_ACTIVE -> bossActive();
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    void status() {
        protocol.Pbmethod.ListCommonVector.Builder lst = protocol.Pbmethod.ListCommonVector.newBuilder();
        lst.addAVector(getCommonIntVector(userClan.getSkills()));
        lst.addAVector(getCommonVector(userClan.getFirstReset(), CfgClan.config.masterSkill));
        addResponse(lst.build());

    }

    void upgrade() {
        List<Long> aLong = CommonProto.parseCommonVector(requestData).getALongList();
        int skillId = aLong.get(0).intValue();
        int numberUp = aLong.get(1).intValue();
        if (numberUp <= 0 || numberUp > 20) {
            addErrParam();
            return;
        }
        ResClanSkillEntity resClanSkill = ResClan.getClanSkill(skillId);
        int indexClass = ResClan.getIndexClass(skillId);
        List<Integer> skillData = userClan.getSkills();
        int curLevel = Math.toIntExact(skillData.get(skillId - 1));
        if (curLevel == -1) {
            addErrResponse(getLang(Lang.locked_skill_in_clan));
            return;
        }

        int number = resClanSkill.getMaxLevel() - curLevel < numberUp ? resClanSkill.getMaxLevel() - curLevel : numberUp;
        if (number <= 0) {
            addErrResponse(getLang(Lang.max_level));
            return;
        }
        boolean hasUpdate = CfgClan.hasUpdateSkill(resClanSkill.getGroup(), userClan.getSkillsCount(), number);
        if (!hasUpdate) {
            addErrResponse(getLang(Lang.clan_skill_upgrade_error));
            return;
        }
        // check real num

        List<Long> feeUpgrade = resClanSkill.getFee(curLevel, number);
        String checkMoney = Bonus.checkMoney(mUser, feeUpgrade);
        if (checkMoney != null) {
            addErrResponse(checkMoney);
            return;
        }
        List<Long> aBonus = Bonus.receiveListItem(mUser, DetailActionType.NANG_KI_NANG_BANG.getKey(), feeUpgrade);
        if (aBonus.isEmpty()) {
            addErrResponse();
            return;
        }
        skillData.set(skillId - 1, curLevel + number);

        if (userClan.upgradeSkill(skillData, indexClass)) {
            protocol.Pbmethod.ListCommonVector.Builder lstCmm = protocol.Pbmethod.ListCommonVector.newBuilder();
            lstCmm.addAVector(getCommonVector(aBonus));
            lstCmm.addAVector(getCommonIntVector(userClan.getSkills()));
            lstCmm.addAVector(user.reCalculatePoint(mUser).toCommonVector());
            addResponse(lstCmm.build());
            Actions.save(user, "clan_skill", "upgrade", "level", skillData.toString(), "number", number);
        } else addErrResponse();
    }

    void reset() {
        List<Long> feeReset = new ArrayList<>();
        if (!userClan.isFirstReset()) {
            feeReset = Bonus.viewGem(-CfgClan.config.feeReset);
            String err = Bonus.checkMoney(mUser, feeReset);
            if (err != null) {
                return;
            }
        }
        List<Integer> skillData = userClan.getSkills();
        Long gold = 0L, coin = 0L;
        for (int i = 0; i < skillData.size(); i++) {
            int curLevel = Math.toIntExact(skillData.get(i)) - 1;
            if (curLevel < 0) continue;
            ResClanSkillEntity resClanSkill = ResClan.getClanSkill(i + 1);
            gold += resClanSkill.getGoldReset(curLevel);
            coin += resClanSkill.getCoinReset(curLevel);
        }
        if (gold == 0 || coin == 0) {
            addErrResponse(getLang(Lang.err_reset_skill));
            return;
        }
        feeReset.addAll(Bonus.viewGold((long) (gold * CfgClan.perGoldReset)));
        feeReset.addAll(Bonus.viewItem(ItemKey.HUY_HIEU_BANG, (long) (coin * CfgClan.perCointReset)));
        List<Long> bonus = Bonus.receiveListItem(mUser, DetailActionType.RESET_KI_NANG_BANG.getKey(), feeReset);
        if (bonus.isEmpty()) {
            addErrResponse();
            return;
        }
        if (userClan.resetSkill()) {
            protocol.Pbmethod.ListCommonVector.Builder lstCmm = protocol.Pbmethod.ListCommonVector.newBuilder();
            lstCmm.addAVector(getCommonVector(bonus));
            lstCmm.addAVector(getCommonIntVector(userClan.getSkills()));
            addResponse(lstCmm.build());
            mUser.reCalculatePoint();
        } else addErrResponse();
    }

    void bossStatus() {
        long timeRemain = CfgClan.getTimeRemainBoss(clan.getTimeOpenBoss());
        addResponse(getCommonVector(timeRemain));
    }

    void bossActive() {
        if (clan.getHonor() < CfgClan.config.feeOpenBoss) {
            addErrResponse(getLang(Lang.err_not_enough_honor));
            return;
        }

        List<Integer> info =clan.getInfoAttackBoss();

        if(info.get(1) >= CfgClan.NUM_ATTACK_BOSS){
            addErrResponse(getLang(Lang.err_boss_daily_limit));
            return;
        }

        if (clan.getHonor() < CfgClan.config.feeOpenBoss) {
            addErrResponse(getLang(Lang.err_not_enough_honor));
            return;
        }
        long timeRemain = CfgClan.getTimeRemainBoss(clan.getTimeOpenBoss());
        if (timeRemain > 0) {
            addErrResponse(getLang(Lang.err_clan_boss_open));
            return;
        }

        if (clan.openBoss()) {
            addResponse(getCommonVector(clan.getHonor(),CfgClan.getTimeRemainBoss(clan.getTimeOpenBoss())));
            BaseRoom curRoom = (BaseRoom) ChUtil.get(channel, ChUtil.KEY_ROOM);
            List<Channel> lstChannel = curRoom.getListChannel();
            clan.addClanLog(Lang.clan_message_16, user.getName(),CfgClan.config.feeOpenBoss+"");
            Util.sendProtoDataToListChanel(lstChannel, Pbmethod.CommonVector.newBuilder().addALong(5L).addALong(13).addAString(getLang(Lang.countdown_teleport_boss)).build(), IAction.COUNTDOWN_MSG);
        }else  addErrResponse(getLang(Lang.err_clan_boss_open));
    }
}
