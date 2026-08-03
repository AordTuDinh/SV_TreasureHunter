package game.treasure.controller;

import game.cache.JCache;
import game.config.*;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.treasure.dao.ClanDAO;
import game.treasure.mapping.*;
import game.treasure.server.IAction;
import game.treasure.service.Services;
import game.treasure.service.user.Actions;
import game.treasure.service.user.Bonus;
import game.treasure.task.dbcache.MailCreatorCache;
import game.monitor.ClanManager;
import game.monitor.Online;
import game.object.*;
import game.protocol.CommonProto;
import io.netty.channel.Channel;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.*;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import java.util.*;
import java.util.stream.Collectors;

public class ClanHandler extends AHandler {
    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(CLAN_CREATE, CLAN_APPLICATION_LIST, CLAN_CHECKIN, CLAN_REQ, CLAN_ANSWER_REQ,
                CLAN_MEMBER_LIST, CLAN_INFO, CLAN_KICK_MEMBER, CLAN_LEAVE, CLAN_DYNAMIC_DETAIL, CLAN_DYNAMIC_STATUS, CLAN_DYNAMIC_REWARD,
                CLAN_DYNAMIC_REWARD_BOX, CLAN_FINDING, CLAN_SET_JOIN_RULE, CLAN_SET_POSITION, CLAN_USER_UPDATE_STATE, CLAN_MAIL_TO_MEMBER,
                CLAN_CHANGE_NAME, CLAN_CHAT, CLAN_CHANGE_AVATAR_INTRO, CLAN_CHAT_LIST, CLAN_START_QUEST, CLAN_LIST_QUEST, CLAN_UPGRADE_QUEST,
                CLAN_RECEIVE_QUEST, CLAN_CONTRIBUTE_INFO, CLAN_CONTRIBUTE, CLAN_CONTRIBUTE_TOP, CLAN_UP_LEVEL, CLAN_HONOR_STATUS, CLAN_HONOR,
                CLAN_HONOR_GET_BONUS, CLAN_SYSTEM_JOIN, CLAN_SYSTEM_LEAVE, CLAN_MY_REQ_LIST, CLAN_CANCEL_REQ);
        actions.forEach(action -> mHandler.put(action, this));
    }

    public static final int INDEX_STAR = 0;
    public static final int INDEX_TIME_DONE = 1;
    public static final int INDEX_TIME = 2;
    public static final int INDEX_STATUS = 3;
    public static final int INDEX_BONUS = 4;

    static ClanHandler instance;
    public static String KEY_CLAN_LEAVE = "clanleave:";

    public static ClanHandler getInstance() {
        if (instance == null) {
            instance = new ClanHandler();
        }
        return instance;
    }

    ClanDAO dao = Services.clanDAO;
    ClanManager clanManager = null;


    @Override
    public AHandler newInstance() {
        return new ClanHandler();
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        checkTimeMonitor("s");
        try {
            switch (actionId) {
                case IAction.CLAN_CREATE -> create();
                case IAction.CLAN_INFO -> clanInfo();
                case IAction.CLAN_MEMBER_LIST -> memberList();
                case IAction.CLAN_REQ -> sendClanReq();
                case IAction.CLAN_FINDING -> findClan();
                case IAction.CLAN_MY_REQ_LIST -> myReqList();
                case IAction.CLAN_CANCEL_REQ -> cancelReq();
                case IAction.CLAN_SYSTEM_JOIN -> joinSystemClan();
                case IAction.CLAN_SYSTEM_LEAVE -> leaveSystemClan();
                default -> {
                    if (user.getClan() <= 0) {
//                        addErrResponse(getLang(Lang.clan_no_clan));
                        return;
                    }
                    clanManager = ClanManager.getInstance(user.getClan());
                    switch (actionId) {
                        case IAction.CLAN_SET_JOIN_RULE -> joinRule();
                        case IAction.CLAN_CHANGE_NAME -> changeName();
                        case IAction.CLAN_CHECKIN -> checkin();
                        case IAction.CLAN_CHANGE_AVATAR_INTRO -> changeInfo();
                        case IAction.CLAN_MAIL_TO_MEMBER -> sendMail();
                        case IAction.CLAN_APPLICATION_LIST -> applicationList();
                        case IAction.CLAN_ANSWER_REQ -> answerReq();
                        case IAction.CLAN_KICK_MEMBER -> kickMember();
                        case IAction.CLAN_LEAVE -> leaveClan();
                        case IAction.CLAN_SET_POSITION -> setPosition();
                        case IAction.CLAN_USER_UPDATE_STATE -> updateOwnState();
                        // chat
                        case IAction.CLAN_CHAT_LIST -> chatList();
                        case IAction.CLAN_CHAT -> clanChat();
                        // clan quest
                        case IAction.CLAN_LIST_QUEST -> questList();
                        case IAction.CLAN_RECEIVE_QUEST -> questReceive();
                        case IAction.CLAN_UPGRADE_QUEST -> questUpgrade();
                        case IAction.CLAN_START_QUEST -> questStart();
                        // upgrade level
                        case IAction.CLAN_UP_LEVEL -> upLevel();
                        case IAction.CLAN_HONOR_STATUS -> honorStatus();
                        case IAction.CLAN_HONOR_GET_BONUS -> honorGetBonus();
//                        case IAction.CLAN_HONOR -> honor();
                    }
                }

            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }


    void upLevel() {
        int inputHonor = getInputInt();
        if (inputHonor <= 0) {
            addErrParam();
            return;
        }
        ClanEntity clan = clanManager.getClan();
        if (inputHonor > clan.getHonor()) {
            addErrResponse(getLang(Lang.err_not_enough_clan_honor));
            return;
        }

        if (clan.addExp(inputHonor)) {
            addResponse(getCommonVector(clan.getLevel(), clan.getExp(), CfgClan.getMaxExp(clan.getLevel()),clan.getHonor()));
           clan.addClanLog(Lang.clan_message_13,user.getName(),inputHonor+"");
        } else addErrSystem();
    }

    void honorStatus() {
        UserClanEntity uClan = Services.userDAO.getUserClan(mUser);
        if (uClan == null) {
            addErrParam();
            return;
        }
        UserDailyEntity uDaily = mUser.getUDaily();
        int isGetBonus = uDaily.getUDaily().getValue(DataDaily.BONUS_TOP_HONOR);
        List<Long> bonus = CfgClan.getBonusDailyHonor(uClan.getHonor());
        bonus.add(0, (long) uClan.getHonor());
        bonus.add(1, (long) isGetBonus);
        bonus.add(2, isGetBonus == 0 ? 0 : DateTime.getSecondsToNextDay());
        bonus.add(3, (long) uDaily.getUDaily().getValue(DataDaily.NUM_HONOR));
        bonus.add(4, (long) CfgClan.getIndexBonusHonor(uClan.getHonor()));
        addResponse(IAction.CLAN_HONOR_STATUS, getCommonVector(bonus));
    }

    void honorGetBonus() {
        UserClanEntity uClan = Services.userDAO.getUserClan(mUser);
        if (uClan == null) {
            addErrParam();
            return;
        }
        UserDailyEntity uDaily = mUser.getUDaily();
        int isGetBonus = uDaily.getUDaily().getValue(DataDaily.BONUS_TOP_HONOR);
        if (isGetBonus == 1) {
            addErrResponse(getLang(Lang.err_no_bonus));
            return;
        }
        List<Long> bonus = CfgClan.getBonusDailyHonor(uClan.getHonor());
        if (uDaily.getUDaily().setValueAndUpdate(DataDaily.BONUS_TOP_HONOR, 1)) {
            addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.RECEIVE_BONUS_HONOR.getKey(Calendar.getInstance().get(Calendar.DAY_OF_YEAR)), bonus)));
            honorStatus();
        }
    }

//    void honor() {
//        int num = getInputInt();
//        if (num < 10) {
//            addErrResponse(getLang(Lang.err_min_10_gem));
//            return;
//        }
//        UserClanEntity uClan = Services.userDAO.getUserClan(mUser);
//        if (uClan == null) {
//            addErrParam();
//            return;
//        }
//
//        UserDailyEntity uDaily = mUser.getUDaily();
//        if (uDaily == null) {
//            addErrSystem();
//            return;
//        }
//        int numHonorCur = uDaily.getUDaily().getValue(DataDaily.NUM_HONOR);
//        if (numHonorCur+num > CfgClan.config.maxNumHonor) {
//            addErrResponse(getLang(Lang.err_max_num_honor));
//            return;
//        }
//        num = Math.min(num, CfgClan.config.maxNumHonor - numHonorCur);
//        int numHH = num / 5;
//        List<Long> fee = Bonus.viewGem(-num);
//        String err = Bonus.checkMoney(mUser, fee);
//        if (err != null) {
//            addErrResponse(err);
//            return;
//        }
//        fee.addAll(Bonus.viewItem(ItemKey.HUY_HIEU_BANG, numHH));
//        ClanEntity clan = clanManager.getClan();
//        if (clan.addHonor(num) && uClan.addHonor(num) && uDaily.getUDaily().setValueAndUpdate(DataDaily.NUM_HONOR, numHonorCur+ num)) {
//            addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.CLAN_HONOR.getKey(), fee)));
//            honorStatus();
//            clan.addClanLog(Lang.clan_message_14,user.getName(),num+"");
//        } else addErrSystem();
//    }


    void create() {
        protocol.Pbmethod.CommonVector cmm = CommonProto.parseCommonVector(requestData);
        String name = cmm.getAString(0);
        String intro = cmm.getAStringCount() > 1 ? cmm.getAString(1) : "";
        int avatar = cmm.getALongCount() > 0 ? (int) cmm.getALong(0) : 0;
        int joinRule = cmm.getALongCount() > 1 ? (int) cmm.getALong(1) : 0;
        int type = cmm.getALongCount() > 2 ? (int) cmm.getALong(2) : 0;
        System.out.println("[CLAN_CREATE] userId=" + (user != null ? user.getId() : -1)
                + " name=" + name + " intro=" + intro
                + " avatar=" + avatar + " joinRule=" + joinRule + " type=" + type
                + " userClan=" + (user != null ? user.getClan() : -1)
                + " gem=" + (user != null ? user.getGem() : -1)
                + " level=" + (user != null ? user.getLevel() : -1)
                + " feeCreate=" + (CfgClan.config != null ? CfgClan.config.feeCreate : -1));

        if (name.contains("<") || name.contains(">") || name.contains("[") || name.contains("]")) {
            System.out.println("[CLAN_CREATE] FAIL name_err_1 invalid chars");
            addErrResponse(getLang(Lang.name_err_1));
            return;
        }

        if (CfgChat.validText(name) || CfgChat.validText(intro)) {
            System.out.println("[CLAN_CREATE] FAIL name_not_found invalidText name=" + CfgChat.validText(name) + " intro=" + CfgChat.validText(intro));
            addErrResponse(getLang(Lang.name_not_found));
            return;
        }
        if (avatar < 1001 || avatar >= 10000) {
            System.out.println("[CLAN_CREATE] FAIL err_params avatar=" + avatar);
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        if (CfgClan.config == null) {
            System.out.println("[CLAN_CREATE] FAIL CfgClan.config is null");
            addErrResponse(getLang(Lang.err_system_down));
            return;
        }
        if (name.length() > CfgClan.config.clanNameLength) {
            System.out.println("[CLAN_CREATE] FAIL name_err_length len=" + name.length() + " max=" + CfgClan.config.clanNameLength);
            addErrResponse(getLang(Lang.name_err_length));
            return;
        }
        if (intro.length() > CfgClan.config.introLength) {
            System.out.println("[CLAN_CREATE] FAIL err_intro_max_length len=" + intro.length() + " max=" + CfgClan.config.introLength);
            addErrResponse(getLang(Lang.err_intro_max_length));
            return;
        }
        if (name.length() < 4) {
            System.out.println("[CLAN_CREATE] FAIL clan_name_min_length len=" + name.length());
            addErrResponse(getLang(Lang.clan_name_min_length));
            return;
        }
        if (user.getClan() != 0) {
            System.out.println("[CLAN_CREATE] FAIL already in clan id=" + user.getClan());
            if (CfgClan.isSystemClan(user.getClan())) {
                addErrResponse(String.format(getLang(Lang.clan_leave_first), CfgClan.getSystemClanName(user.getClan())));
            } else {
                addErrResponse(String.format(getLang(Lang.clan_leave_first), ClanManager.getInstance(user.getClan()).getClan().getName()));
            }
            return;
        }
        if (hasClanLeaveCooldown(CfgClan.timeWaitLeave)) {
            System.out.println("[CLAN_CREATE] FAIL leave cooldown");
            return;
        }

        ClanEntity findClan = Services.clanDAO.getClan(name);
        if (findClan != null) {
            System.out.println("[CLAN_CREATE] FAIL name_exist id=" + findClan.getId());
            addErrResponse(getLang(Lang.clan_name_exist));
            return;
        }

        List<Long> fee = CfgClan.getFeeCreate(type);
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            System.out.println("[CLAN_CREATE] FAIL checkMoney err=" + err + " fee=" + fee + " gem=" + user.getGem() + " ruby=" + user.getRuby());
            addErrResponse(err);
            return;
        }
        // joinRule: 0 = chiêu mộ tắt (duyệt tay), 1 = bật (auto vào)
        if (joinRule != 0 && joinRule != 1) joinRule = 0;
        int gemFee = type == 0 ? CfgClan.config.feeCreate : 0;
        System.out.println("[CLAN_CREATE] creating gemFee=" + gemFee + " joinRule=" + joinRule);
        int ret = Services.clanDAO.createClan(user, name, gemFee, intro, avatar, joinRule, 1);
        if (ret > 0) {
            if (type == 0) user.addGem(-CfgClan.config.feeCreate);
            else user.addRuby(-CfgClan.config.feeCreateRuby);
            user.setClan(ret);
            user.setClanName(name);
            user.setClanPosition(ClanPosition.LEADER.value);
            user.setClanAvatar(avatar);
            clearAllPendingReq(user.getId());
            long moneyType = type == 0 ? Bonus.BONUS_GEM : Bonus.BONUS_RUBY;
            long moneyLeft = type == 0 ? user.getGem() : user.getRuby();
            long moneyCost = type == 0 ? -CfgClan.config.feeCreate : -CfgClan.config.feeCreateRuby;
            System.out.println("[CLAN_CREATE] OK clanId=" + ret + " moneyLeft=" + moneyLeft);
            addResponse(getCommonVector((long) ret, moneyType, moneyLeft, moneyCost));
            if (CfgServer.isRealServer()) Actions.save(user, Actions.GCLAN, Actions.DCREATE, "id", ret);
            mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.JOIN_CLAN, 1);
        } else {
            System.out.println("[CLAN_CREATE] FAIL dao.createClan ret=" + ret);
            addErrResponse(getLang(Lang.err_system_down));
        }
    }

    void applicationList() {
        protocol.Pbmethod.PbListUser.Builder builder = protocol.Pbmethod.PbListUser.newBuilder();
        List<ClanReqEntity> aReq = clanManager.getClan().getAReq();
        int size = Math.min(aReq.size(), 50);
        for (int i = 0; i < size; i++) {
            if (i < aReq.size()) {
                ClanReqEntity req = aReq.get(i);
                UserEntity user = Online.getDbUser(req.getUserId());
                if (user != null) {
                    builder.addAUser(user.protoTinyUser());
                }
            }
        }
        addResponse(builder.build());
    }

    void sendClanReq() {
        int clanId = CommonProto.parseCommonVector(requestData).getALongList().get(0).intValue();

        if (user.getClan() != 0) {
            if (CfgClan.isSystemClan(user.getClan())) {
                addErrResponse(String.format(getLang(Lang.clan_leave_first), CfgClan.getSystemClanName(user.getClan())));
            } else {
                ClanEntity clan = ClanManager.getInstance(user.getClan()).getClan();
                addErrResponse(String.format(getLang(Lang.clan_leave_first), clan.getName()));
            }
            return;
        }
        if (hasClanLeaveCooldown(CfgClan.timeWaitLeave)) return;

        ClanEntity clan = ClanManager.getInstance(clanId).getClan();
        if (clan == null) {
            addErrResponse(getLang(Lang.clan_not_found));
            return;
        }
        if (clan.getMember() >= CfgClan.getMaxMember(clan.getLevel())) {
            addErrResponse(String.format(getLang(Lang.clan_max_member), CfgClan.getMaxMember(clan.getLevel())));
            return;
        }

        if (clan.getJoinRule() == 1) {
            // Chiêu mộ ON — vào thẳng bang
            List<Integer> memberIds = clan.getMemberId();
            if (!memberIds.contains(user.getId())) memberIds.add(user.getId());
            if (dao.addNewMember(clan, user.getId(), memberIds)) {
                clan.joinUser(user);
                clearAllPendingReq(user.getId());
                if (CfgServer.isRealServer())
                    Actions.save(user, "clan", "answer_req1", "answer", 1, "userId", user.getId(), "clanId", clanId);
                Pbmethod.CommonVector.Builder cmm = Pbmethod.CommonVector.newBuilder();
                cmm.addALong(0);
                cmm.addALong(clan.getId());
                cmm.addALong(clan.getAvatar());
                cmm.addAString(String.format(getLang(Lang.clan_message_12), clan.getName()));
                cmm.addAString(clan.getName());
                mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.JOIN_CLAN, 1);
                addResponse(IAction.CLAN_ACCEPT_MEMBER, cmm.build());
            } else addErrResponse();
        } else {
            // Chiêu mộ OFF — gửi đơn chờ duyệt
            if (dao.countUserReq(user.getId()) >= CfgClan.MAX_PENDING_REQ) {
                addErrResponse(String.format(getLang(Lang.clan_max_pending_req), CfgClan.MAX_PENDING_REQ));
                return;
            }
            List<ClanReqEntity> aReq = clan.getAReq();
            String newKey = "clanReq_" + clanId + "_" + user.getId();
            if (JCache.getInstance().getValue(newKey) != null) {
                for (int i = 0; i < aReq.size(); i++) {
                    if (i < aReq.size() && aReq.get(i).getUserId() == user.getId()) {
                        addErrResponse(getLang(Lang.clan_application_processing));
                        return;
                    }
                }
                Long sendTime = JCache.getInstance().getLongValue(newKey);
                if (sendTime != null) {
                    addErrResponse(String.format(getLang(Lang.clan_application_rejected), DateTime.formatTime(DateTime.DAY_SECOND - (System.currentTimeMillis() - sendTime) / 1000)));
                } else addErrResponse(getLang(Lang.clan_application_rejected));
                return;
            }
            ClanReqEntity clanReq = new ClanReqEntity(clan.getId(), user.getId());
            JCache.getInstance().setValue(newKey, System.currentTimeMillis() + "", JCache.EXPIRE_1D);
            if (DBJPA.save(clanReq)) {
                aReq.add(0, clanReq);
                addResponse(getCommonVector(1L));
                Actions.save(user, "clan", "request", "clanId", clanId);
            } else addErrResponse(getLang(Lang.err_system_down));
        }
    }

    void answerReq() {
        List<Long> aLong = CommonProto.parseCommonVector(requestData).getALongList();
        int userId = aLong.get(0).intValue();
        int type = aLong.get(1).intValue(); // 1 accept, 2 reject
        type = type == 1 ? type : 2;
        ClanEntity clan = clanManager.getClan();
        if (!CfgClan.CLAN_RULE.contains(user.getClanPosition())) {
            addErrResponse(getLang(Lang.user_not_allow_function) + " 1");
            return;
        }
        Actions.save(user, "clan", "answer_req", "answer", type, "userId", userId, "clanId", user.getClan());
        if (type == 2) { // từ chối
            clan.deleteRequest(userId);
            addResponse(getCommonVector(0));
        } else { // cho vào
            if (clan.getMember() >= CfgClan.getMaxMember(clan.getLevel())) {
                addErrResponse(String.format(getLang(Lang.clan_max_member), CfgClan.getMaxMember(clan.getLevel())));
                addResponseError();
                return;
            }

            UserEntity memberUser = Online.getDbUser(userId);
            UserEntity dbUser = Services.userDAO.getUser(userId);
            if (memberUser == null || dbUser == null) {
                addErrResponse(getLang(Lang.user_not_found) + " 2");
                addResponse(getCommonVector(0));
                return;
            }

            if (memberUser.getClan() != 0 || dbUser.getClan() != 0) {
                addErrResponse(getLang(Lang.user_in_clan) + " 3");
                addResponse(getCommonVector(0));
                return;
            }


            String key = ClanHandler.KEY_CLAN_LEAVE + memberUser.getId();
            Long timeLeave = JCache.getInstance().getLongValue(key);
            if (timeLeave != null) {
                long timeRemain = CfgClan.timeWaitLeave + timeLeave - System.currentTimeMillis();
                if (timeRemain > 0) {
                    addErrResponse(String.format(getLang(Lang.clan_wait_leave1 + " 4"), DateTime.formatTime(timeRemain / 1000)));
                    addResponse(getCommonVector(0));
                    return;
                }
            }
            List<Integer> memberIds = clan.getMemberId();
            if (!memberIds.contains(memberUser.getId())) memberIds.add(memberUser.getId());
            if (dao.addNewMember(clan, userId, memberIds)) {
                clan.joinUser(memberUser);
                clearAllPendingReq(userId);
                addResponse(getCommonVector(1));
                addErrResponse(getLang(Lang.success));
                if (CfgServer.isRealServer())
                    Actions.save(user, "clan", "answer_req1", "answer", type, "userId", userId, "clanId", user.getClan());
                Channel channel = Online.getChannel(memberUser.getId());
                if (channel != null && channel.isOpen() && memberUser.getServer() == clan.getServer()) {
                    Pbmethod.CommonVector.Builder cmm = Pbmethod.CommonVector.newBuilder();
                    cmm.addALong(0);
                    cmm.addALong(clan.getId());
                    cmm.addALong(clan.getAvatar());
                    cmm.addAString(String.format(getLang(Lang.clan_message_12), clan.getName()));
                    cmm.addAString(clan.getName());
                    MyUser userMember = Online.getMUser(memberUser.getId());
                    if (userMember != null)
                        userMember.getUData().checkQuestTutDefault(mUser, QuestTutType.JOIN_CLAN, 1);
                    Util.sendProtoData(channel, cmm.build(), IAction.CLAN_ACCEPT_MEMBER);
                }
            } else addErrResponse();
        }
    }

    void memberList() {
        int clanId = CommonProto.parseCommonVector(requestData).getALongList().get(0).intValue();
        ClanManager clanManager = ClanManager.getInstance(clanId);
        if (clanManager == null) addErrResponse(getLang(Lang.clan_not_found));
        else {
            List<UserEntity> aUser = clanManager.getListMember();
            protocol.Pbmethod.PbClan.Builder builder = protocol.Pbmethod.PbClan.newBuilder();
            for (UserEntity user : aUser) {
                builder.addMember(user.protoClanMember());
            }
            addResponse(builder.build());
        }
    }

    void clanInfo() {
        int clanId = CommonProto.parseCommonVector(requestData).getALongList().get(0).intValue();
        ClanManager clanManager = ClanManager.getInstance(clanId);
        if (clanManager == null) {
            addErrParam();
            return;
        }
        ClanEntity clan = clanManager.getClan();
        if (clan == null) {
            addErrResponse(getLang(Lang.clan_not_found));
            return;
        }

        UserClanEntity userClan = Services.userDAO.getUserClan(mUser);
        if (userClan != null && !userClan.canCheckin()) {
            CfgQuest.addNumQuest(mUser, DataQuest.CHECK_IN_CLAN, 1);
        }
        addResponse(clan.protoClan(Lang.instance(mUser), 1).build());
    }

    void kickMember() {
        int kickUserId = CommonProto.parseCommonVector(requestData).getALongList().get(0).intValue();
        UserEntity memberUser = Services.userDAO.getUser(kickUserId);
        if (memberUser == null) {
            addErrResponse(getLang(Lang.user_not_found));
            return;
        }

        ClanEntity clan = clanManager.getClan();
        if (clan != null && !CfgClan.CLAN_RULE.contains(user.getClanPosition())) {
            addErrResponse(getLang(Lang.user_not_allow_function));
            return;
        }
        if (user.getId() == kickUserId) {
            addErrResponse(getLang(Lang.clan_kick_error));
            return;
        }
        if (memberUser.getClan() != clan.getId()) {
            addErrResponse(getLang(Lang.clan_kick_error));
            return;
        }

        Integer numberKick = JCache.getInstance().getIntValue(DateTime.getDateyyyyMMdd(Calendar.getInstance().getTime()) + "clan" + clan.getId());
        if (numberKick != null && numberKick >= 5) {
            addErrResponse(getLang(Lang.clan_kick_too_many));
            return;
        }
        clan.kick(this, user.getName(), memberUser);
    }

    void leaveClan() {
        ClanEntity clan = clanManager.getClan();
        // Chủ bang bắt buộc chuyển chức trước khi rời — không giải tán bang
        if (user.getClanPosition() == ClanPosition.LEADER.value) {
            addErrResponse(getLang(Lang.clan_leader_leave_error));
            return;
        }
        if (user.getClanJoin() != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR, -6);
            if (calendar.getTime().before(user.getClanJoin())) {
                addErrResponse(String.format(getLang(Lang.clan_wait_leave), DateTime.formatTime((user.getClanJoin().getTime() - calendar.getTimeInMillis()) / 1000)));
                return;
            }
        }
        clan.leaveClan(this, user);
    }

    void myReqList() {
        if (user.getClan() != 0) {
            addErrResponse(getLang(Lang.user_in_clan));
            return;
        }
        // Trả PbListClan — mỗi phần tử = 1 bang đang xin (tối đa 10).
        // joinTrophy luôn = 1 (đánh dấu pending). Không gửi activityLog / member list.
        List<ClanReqEntity> reqs = dao.getListUserReq(user.getId());
        protocol.Pbmethod.PbListClan.Builder builder = protocol.Pbmethod.PbListClan.newBuilder();
        if (reqs != null) {
            int count = 0;
            for (ClanReqEntity req : reqs) {
                if (count >= CfgClan.MAX_PENDING_REQ) break;
                ClanManager cm = ClanManager.getInstance(req.getClanId());
                if (cm == null || cm.getClan() == null) continue;
                ClanEntity clan = cm.getClan();
                protocol.Pbmethod.PbClan.Builder clanBuilder = protocol.Pbmethod.PbClan.newBuilder();
                clanBuilder.setId(clan.getId());
                clanBuilder.setName(clan.getName());
                clanBuilder.setAvatar(clan.getAvatar());
                clanBuilder.setLevel(clan.getLevel());
                clanBuilder.setNumberMember(clan.getMember());
                clanBuilder.setMaxMember(CfgClan.getMaxMember(clan.getLevel()));
                clanBuilder.setJoinRule(clan.getJoinRule());
                clanBuilder.setMasterName(clan.getMaster() != null ? clan.getMaster() : "");
                clanBuilder.setIntro(clan.getIntro() != null ? clan.getIntro() : "");
                clanBuilder.setJoinTrophy(1); // pending request flag
                clanBuilder.setStar((int) Math.min(Integer.MAX_VALUE, clan.getContribute()));
                clanBuilder.setPointRank(clan.getStar()); // cup bang
                // epoch ms lúc gửi đơn — client hiển thị thời gian nếu cần
                if (req.getDateCreated() != null) {
                    clanBuilder.setExp(req.getDateCreated().getTime());
                }
                builder.addClan(clanBuilder);
                count++;
            }
        }
        addResponse(builder.build());
    }

    void cancelReq() {
        if (user.getClan() != 0) {
            addErrResponse(getLang(Lang.user_in_clan));
            return;
        }
        int clanId = CommonProto.parseCommonVector(requestData).getALongList().get(0).intValue();
        ClanManager cm = ClanManager.getInstance(clanId);
        if (cm != null && cm.getClan() != null) {
            cm.getClan().deleteRequest(user.getId());
        } else {
            dao.deleteUserReq(clanId, user.getId());
        }
        JCache.getInstance().removeValue("clanReq_" + clanId + "_" + user.getId());
        addResponse(getCommonVector((long) clanId));
    }

    /** Xóa mọi đơn chờ của user + gỡ khỏi cache aReq của từng bang. */
    void clearAllPendingReq(int userId) {
        List<ClanReqEntity> reqs = dao.getListUserReq(userId);
        if (reqs != null) {
            for (ClanReqEntity req : reqs) {
                ClanManager cm = ClanManager.getInstance(req.getClanId());
                if (cm != null && cm.getClan() != null) {
                    cm.getClan().removeRequest(userId);
                }
                JCache.getInstance().removeValue("clanReq_" + req.getClanId() + "_" + userId);
            }
        }
        dao.deleteAllUserReq(userId);
    }

    void findClan() {
        List<ClanEntity> aClan = new ArrayList<>();
        String name = CommonProto.parseCommonVector(requestData).getAString(0);
        if (name.equals("")) {
            aClan = dao.suggestClan(user.getServer());
            if (aClan == null) aClan = new ArrayList<>();
        } else if (NumberUtil.isIntNumber(name.trim())) {
            ClanManager clanManager = ClanManager.getInstance(Integer.parseInt(name.trim()));
            if (clanManager != null) {
                if (clanManager.getClan() != null && clanManager.getClan().getServer() == user.getServer())
                    aClan.add(clanManager.getClan());
            }
        } else {
            if (name.length() < 4) {
                addErrResponse(getLang(Lang.clan_name_min_length));
                return;
            }
            aClan = dao.findClan(user.getServer(), name);
            if (aClan == null) aClan = new ArrayList<>();
        }

        var setClanId = dao.getListUserReq(user.getId()).stream().map(ClanReqEntity::getClanId).collect(Collectors.toSet());
        protocol.Pbmethod.PbListClan.Builder builder = protocol.Pbmethod.PbListClan.newBuilder();
        aClan.forEach(clan -> {
            protocol.Pbmethod.PbClan.Builder clanBuilder = clan.protoClan(Lang.instance(mUser));
            clanBuilder.setJoinTrophy(setClanId.contains(clan.getId()) ? 1 : 0);
            builder.addClan(clanBuilder);
        });
        addResponse(builder.build());
    }

    void joinRule() {
        int rule = getInputInt();
        if (rule != 0 && rule != 1) {
            addErrParam();
            return;
        }
        if (user.getClanPosition() == ClanPosition.LEADER.value || user.getClanPosition() == ClanPosition.CO_LEADER.value) {
            if (dao.updateClanJoinRule(user.getClan(), rule)) {
                ClanEntity clan = clanManager.getClan();
                clan.setJoinRule(rule);
                addResponse(getCommonVector(rule));
            } else addErrResponse();
        } else addErrSystem();
    }

    void setPosition() {
        protocol.Pbmethod.CommonVector cmm = CommonProto.parseCommonVector(requestData);
        int userId = (int) cmm.getALong(0);
        int newPosition = (int) cmm.getALong(1);
        if (newPosition < ClanPosition.MEMBER.value || newPosition > ClanPosition.LEADER.value) {
            addErrResponse(getLang(Lang.clan_new_position_error));
            return;
        }
        ClanEntity clan = clanManager.getClan();
        if (clan == null) {
            addErrResponse(getLang(Lang.clan_not_found));
            return;
        }
        String result = clan.setPosition(mUser, userId, newPosition);
        if (!StringHelper.isEmpty(result)) {
            addErrResponse(result);
            return;
        }
        addResponse(null);
        //Medal.transferMedalBangchu(mUser, userId);
    }

    private void updateOwnState() {
        ClanEntity clan = user.getClan() > 0 ? clanManager.getClan() : null;
        if (clan == null)
            addResponse(protocol.Pbmethod.CommonVector.newBuilder().addALong(0).addALong(0).addAString("").build());
        else
            addResponse(protocol.Pbmethod.CommonVector.newBuilder().addALong(user.getClan()).addALong(ClanPosition.MEMBER.value).addAString(clan.getName()).build());
    }

    void sendMail() {
        String content = CommonProto.parseCommonVector(requestData).getAString(0);
        if (content.length() >= 256) {
            addErrResponse(getLang(Lang.clan_mail_length));
            return;
        }
        String validContent = CfgChat.replaceInvalidWord(content);
        if (user.getClanPosition() == ClanPosition.CO_LEADER.value || user.getClanPosition() == ClanPosition.LEADER.value) {
            String msg = String.format(getLang( "title_mai_from"), clanManager.getClan().getName(), ClanPosition.getName(Lang.instance(mUser), user.getClanPosition()));
            List<UserEntity> aUser = clanManager.getListMember();
            List<UserMailEntity> aMail = new ArrayList<>();
            aUser.stream().filter(userEntity -> userEntity.getId() != user.getId()).forEach(userEntity -> aMail.add(UserMailEntity.builder().userId(userEntity.getId()).senderId(user.getId()).senderName(user.getName()).title(msg).message(validContent).build().initDefault()));
            MailCreatorCache.sendMail(aMail);
            addResponse(null);
        } else addErrResponse(getLang(Lang.clan_leader_coleader_required));
    }

    void changeName() {
        String name = CommonProto.parseCommonVector(requestData).getAString(0);
        String tmpName = CfgChat.replaceInvalidWord(name);
        if (tmpName.contains("***")) {
            addErrResponse(getLang(Lang.name_not_found));
            return;
        }
        if (user.getClanPosition() == ClanPosition.LEADER.value) {
            if (NumberUtil.isNumber(name)) addErrResponse(getLang(Lang.clan_name_character));
            else if (name == null || name.length() < 4) addErrResponse(getLang(Lang.clan_name_min_length));
            else if (name.length() > CfgChat.maxClanName) addErrResponse(getLang(Lang.clan_name_max_length));
            else if (dao.getClan(name) != null) addErrResponse(getLang(Lang.clan_name_exist));
            else {
                int gem = CfgClan.config.feeChangeName;
                List<Long> fee = CfgClan.getFeeChangeName();
                String err = Bonus.checkMoney(mUser, fee);
                if (err != null) {
                    addErrResponse(err);
                    return;
                }
                if (dao.updateClanName(user.getClan(), name)) {
                    ClanEntity clan = clanManager.getClan();
                    clan.setName(name);
                    addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.CHANG_NAME_CLAN.getKey(), fee)));
                    Actions.save(user, "clan", "change_name", "id", user.getClan(), "name", name);
                } else addErrResponse();
            }
        } else addErrSystem();
    }

    void checkin() {
        UserClanEntity userClan = Services.userDAO.getUserClan(mUser);
        if (userClan == null || !userClan.canCheckin()) {
            addErrParam();
            return;
        }
        int day = DateTime.getNumberDay();
        if (userClan.update(Arrays.asList("day_checkin", day))) {
            userClan.setDayCheckin(day);
            int clanHonor = CfgClan.config.checkInGuildExp, guildCoin = CfgClan.config.checkInGuildCoin;
            ClanEntity clan = clanManager.getClan();
            clan.addHonor(clanHonor);
            List<Long> aBonus = Bonus.receiveListItem(mUser, DetailActionType.DIEM_DANH_BANG_HOI.getKey(),
                    Bonus.viewItemPoint(ItemPointKey.CO_VAT.id, guildCoin));
            addResponse(protocol.Pbmethod.CommonVector.newBuilder().addALong(clan.getLevel()).
                    addALong(clan.getExp()).addALong(clanHonor).addALong(DateTime.secondsUntilEndDay()).addALong(clan.getContribute()).addAllALong(aBonus).build());
            CfgQuest.addNumQuest(mUser, DataQuest.CHECK_IN_CLAN, 1);
        } else addErrSystem();
    }

    void changeInfo() {
        int position = user.getClanPosition();
        if (position != ClanPosition.LEADER.value && position != ClanPosition.CO_LEADER.value) {
            addErrResponse(getLang(Lang.clan_not_enough_position));
            return;
        }
        protocol.Pbmethod.CommonVector cmm = CommonProto.parseCommonVector(requestData);
        int avatarId = (int) cmm.getALong(0);
        String status = cmm.getAString(0);
        if (status.length() >= 256) status = status.substring(0, 256);

        ClanEntity clan = clanManager.getClan();
        if (clan == null) {
            addErrResponse();
            return;
        }
        if (DBJPA.update("clan", Arrays.asList("intro", status, "avatar", avatarId), Arrays.asList("id", clan.getId()))) {
            clan.setIntro(status);
            clan.setAvatar(avatarId);
            clan.addClanLog(Lang.clan_edit_info, user.getName());
            addResponse(cmm);
            Actions.save(user, "clan", "change_info", "status", status, "avatar", avatarId);
        } else addErrResponse();
    }

    // region chat
    void chatList() {
        addResponse(CLAN_CHAT_LIST, protoListChat(getChatHistory(clanManager.getClan().getAChat())));
    }

    private List<ChatObject> getChatHistory(List<ChatObject> aChat) {
        List<ChatObject> tmp = new ArrayList<ChatObject>();
        int count = 0;
        for (int i = aChat.size() - 1; i >= 0; i--) {
            tmp.add(0, aChat.get(i));
            count++;
            if (count >= CfgChat.maxSaveChat) {
                break;
            }
        }
        return tmp;
    }

    void clanChat() {
        if (user.getClan() == 0) {
            addErrResponse(getLang(Lang.clan_no_clan));
            return;
        }
        ClanEntity clan = ClanManager.getInstance(user.getClan()).getClan();
        if (clan == null) {
            addErrSystem();
            return;
        }
        // Add chat;
        protocol.Pbmethod.CommonVector cmm = CommonProto.parseCommonVector(requestData);
        String chatMsg = cmm.getAString(0);
        chatMsg = chatMsg.length() >= 160 ? chatMsg.substring(0, 160) : chatMsg;
        if (!CfgChat.isValidChat(chatMsg, "")) {
            addPopupResponse(getLang(Lang.chat_msg_invalid));
            return;
        }
        ChatObject chat = new ChatObject(user, chatMsg);
        clan.addChat(user, chat);
        addResponse(chat.toProto());
        saveClanChat();
        List<Integer> memberIds = clan.getMemberId();
        for (int i = 0; i < memberIds.size(); i++) {
            if (memberIds.get(i) != user.getId() && Online.isOnline(memberIds.get(i))) {
                Channel channelChat = Online.getChannel(memberIds.get(i));
                Util.sendProtoData(channelChat, chat.toProto(), IAction.CLAN_CHAT);
                Util.sendProtoData(channelChat, CommonProto.getCommonVector(NotifyType.MESSAGE.value), IAction.ADD_NOTIFY);
            }
        }
    }

    private void saveClanChat() {
        mUser.getUData().setLastClanChat(System.currentTimeMillis());
    }
    //end region

    // region quest
    private void questList() {
        UserClanEntity userClan = Services.userDAO.getUserClan(mUser);
        if (userClan == null) {
            addErrParam();
            return;
        }
        Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
        List<List<Long>> quest = userClan.getQuest();
        List<Long> bonusDone = new ArrayList<>();
        ClanEntity clan = ClanManager.getInstance(user.getClan()).getClan();
        if (clan == null) {
            addErrParam();
            return;
        }
        boolean update = false;
        //todo check status
        int index = 0;
        int numDone = 0;
        int listQuestActive = 0;
        int size = quest.size();
        for (int i = 0; i < size; i++) {
            List<Long> data = quest.get(index);
                long timeRemain = data.get(INDEX_TIME_DONE) - System.currentTimeMillis() / 1000;
            timeRemain = timeRemain < 0 ? 0 : timeRemain;
            if (timeRemain <= 0) { // done quest thì trả về status
                update = true;
                numDone++;
                bonusDone.addAll(data.subList(INDEX_BONUS, data.size()));
                quest.remove(index);
            } else {
                listQuestActive++;
                List<Long> result = new ArrayList<>();
                long star = data.get(INDEX_STAR);
                result.add(star); // star
                result.add(timeRemain); // time remain
                result.add(data.get(INDEX_TIME)); // time
                StatusType status = StatusType.get(Math.toIntExact(data.get(INDEX_STATUS)));
                if (status != StatusType.DONE && timeRemain == 0) status = StatusType.RECEIVE;
                result.add((long) status.value); // status
                result.add(star == 5 ? 0L : (long) CfgClan.config.upgradeQuest.get((int) (star - 1)));
                result.addAll(data.subList(INDEX_BONUS, data.size())); // bonus
                pb.addAVector(getCommonVector(result));
                index++;
            }
        }

        if (update) if (userClan.updateQuest(quest)) {
            if (!bonusDone.isEmpty())
                addBonusToastPlus(Bonus.receiveListItem(mUser, DetailActionType.BONUS_CLAN_QUEST.getKey(numDone), bonusDone));
            addResponse(CLAN_LIST_QUEST, pb.build());
        } else addErrSystem();
        else addResponse(CLAN_LIST_QUEST, pb.build());

    }

    private void questAccept() {
        UserClanEntity userClan = Services.userDAO.getUserClan(mUser);
        if (userClan == null) {
            addErrParam();
            return;
        }
        List<List<Long>> quest = userClan.getQuest();
        // check đang có quest
        if (quest.size() > 0) {
            addErrResponse(getLang(Lang.clan_quest_err));
            return;
        }
        // tạo mới quest
        ClanEntity clan = ClanManager.getInstance(user.getClan()).getClan();
        if (clan == null) {
            addErrSystem();
            return;
        }
        if (userClan.updateQuest(quest)) {
            addResponseSuccess();
            questList();

        } else addErrSystem();
    }

    private void questReceive() {
        UserClanEntity userClan = Services.userDAO.getUserClan(mUser);
        if (userClan == null) {
            addErrParam();
            return;
        }
        List<List<Long>> quest = userClan.getQuest();
        int index = getInputInt();
        if (quest.isEmpty() || quest.get(index) == null || quest.get(index).get(INDEX_STATUS) == StatusType.DONE.value) {
            addErrParam();
            return;
        }
        if (!checkDoneQuest(quest.get(index))) {
            addErrResponse(getLang(Lang.err_quest_done));
            return;
        }
        quest.get(index).set(INDEX_STATUS, (long) StatusType.DONE.value);
        quest.remove(index);
        // xoá quest đã hoàn thành
        List<Long> bonus = quest.get(index).subList(INDEX_BONUS, quest.get(index).size());
        if (userClan.updateQuest(quest)) {
            addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.BONUS_CLAN_QUEST.getKey(index), bonus)));
            questList();
        } else addErrSystem();
    }

    private boolean checkDoneQuest(List<Long> quest) {
        if (quest == null || quest.size() < INDEX_BONUS) return false;
        return quest.get(1) > System.currentTimeMillis() / 1000;
    }

    private void questUpgrade() {
        UserClanEntity userClan = Services.userDAO.getUserClan(mUser);
        if (userClan == null) {
            addErrParam();
            return;
        }
        int index = getInputInt();
        List<List<Long>> quest = userClan.getQuest();
        List<Long> questData = quest.get(index);
        if (questData == null) {
            addErrParam();
            return;
        }
        int star = Math.toIntExact(quest.get(index).get(INDEX_STAR));
        if (star >= 5) {
            addErrResponse(getLang(Lang.err_max_level));
            return;
        }
        List<Long> fee = Bonus.viewGem(-CfgClan.config.upgradeQuest.get(star - 1));
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        // upgrade quest tăng 30% bonus
        List<Long> bonus = new ArrayList<>(questData.subList(INDEX_BONUS, questData.size()));
        questData.subList(INDEX_BONUS, questData.size()).clear();
        // check có đang trạng thái lock k
        if (Math.toIntExact(questData.get(INDEX_STATUS)) != StatusType.LOCK.value) {
            addErrParam();
            return;
        }
        questData.set(INDEX_STAR, (long) (star + 1)); // tăng sao
        questData.set(INDEX_TIME_DONE, questData.get(INDEX_TIME_DONE) + DateTime.HOUR_SECOND); // tăng thời gian
        questData.set(INDEX_TIME, questData.get(INDEX_TIME) + 1L); // tăng thời gian

        bonus.addAll(Bonus.xPerBonus(bonus, 30));
        questData.addAll(Bonus.merge(bonus)); // add lại quest đã nâng cấp bonus
        List<Long> aBonus = Bonus.receiveListItem(mUser, DetailActionType.BONUS_UPGRADE_CLAN_QUEST.getKey(index), fee);
        if (aBonus.isEmpty()) {
            addErrSystem();
            return;
        }
        if (userClan.updateQuest(quest)) {
            addBonusToast(aBonus);
            questList();
        } else {
            Bonus.receiveListItem(mUser, DetailActionType.BONUS_UPGRADE_CLAN_QUEST.getKey(index), Bonus.reverseBonus(fee));
            addErrSystem();
        }

    }

    private void questStart() {
        UserClanEntity userClan = Services.userDAO.getUserClan(mUser);
        if (userClan == null) {
            addErrParam();
            return;
        }
        int index = getInputInt();
        List<List<Long>> quest = userClan.getQuest();
        List<Long> questData = quest.get(index);
        if (questData == null) {
            addErrParam();
            return;
        }
        if (Math.toIntExact(questData.get(INDEX_STATUS)) != StatusType.LOCK.value) {
            addErrParam();
            return;
        }
        questData.set(INDEX_STATUS, (long) StatusType.PROCESSING.value);
        if (userClan.updateQuest(quest)) {
            addResponseSuccess();
            questList();
        } else addErrSystem();
    }

    private protocol.Pbmethod.PbListChat protoListChat(List<ChatObject> aChat) {
        protocol.Pbmethod.PbListChat.Builder builder = protocol.Pbmethod.PbListChat.newBuilder();
        aChat.forEach(chat -> builder.addAChat(chat.toProto()));
        return builder.build();
    }

    boolean hasClanLeaveCooldown(long waitMillis) {
        String key = ClanHandler.KEY_CLAN_LEAVE + user.getId();
        Long timeLeave = JCache.getInstance().getLongValue(key);
        if (timeLeave == null) return false;
        long timeRemain = waitMillis + timeLeave - System.currentTimeMillis();
        if (timeRemain > 0) {
            addErrResponse(String.format(getLang(Lang.clan_wait_leave1), DateTime.formatTime(timeRemain / 1000)));
            return true;
        }
        return false;
    }

    void joinSystemClan() {
        int clanId = getInputInt();
        if (!CfgClan.isSystemClan(clanId)) {
            addErrParam();
            return;
        }
        if (user.getClan() != 0) {
            addErrResponse(getLang(Lang.user_in_clan));
            return;
        }
        if (hasClanLeaveCooldown(CfgClan.timeWaitLeave)) return;
        String clanName = CfgClan.getSystemClanName(clanId);
        if (!dao.joinSystemClan(user, clanId, clanName)) {
            addErrResponse(getLang(Lang.err_system_down));
            return;
        }
        mUser.reCalculatePoint();
        Pbmethod.CommonVector.Builder cmm = Pbmethod.CommonVector.newBuilder();
        cmm.addALong(clanId);
        cmm.addAString(clanName);
        addResponse(IAction.CLAN_SYSTEM_JOIN, cmm.build());
        if (CfgServer.isRealServer()) Actions.save(user, "clan", "join_system", "clanId", clanId);
    }

    void leaveSystemClan() {
        int clanId = getInputInt();
        if (!CfgClan.isSystemClan(user.getClan()) || user.getClan() != clanId) {
            addErrResponse(getLang(Lang.clan_no_clan));
            return;
        }
        if (!dao.leaveSystemClan(user)) {
            addErrResponse(getLang(Lang.err_system_down));
            return;
        }
        mUser.reCalculatePoint();
        Pbmethod.CommonVector.Builder cmm = Pbmethod.CommonVector.newBuilder();
        cmm.addALong(0);
        cmm.addAString("");
        addResponse(IAction.CLAN_SYSTEM_LEAVE, cmm.build());
        if (CfgServer.isRealServer()) Actions.save(user, "clan", "leave_system", "clanId", clanId);
    }
}
