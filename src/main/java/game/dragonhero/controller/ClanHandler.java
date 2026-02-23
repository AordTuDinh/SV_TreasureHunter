package game.dragonhero.controller;

import game.cache.JCache;
import game.config.*;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.dragonhero.dao.ClanDAO;
import game.dragonhero.mapping.*;
import game.dragonhero.mapping.main.ResContributeEntity;
import game.dragonhero.mapping.main.ResDynamicTypeEntity;
import game.dragonhero.server.IAction;
import game.dragonhero.service.Services;
import game.dragonhero.service.resource.ResClan;
import game.dragonhero.service.user.Actions;
import game.dragonhero.service.user.Bonus;
import game.dragonhero.task.dbcache.MailCreatorCache;
import game.monitor.ClanManager;
import game.monitor.Online;
import game.monitor.TopMonitor;
import game.object.*;
import game.protocol.CommonProto;
import io.netty.channel.Channel;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.*;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import java.util.*;
import java.util.stream.Collectors;

import static game.dragonhero.service.resource.ResClan.*;

public class ClanHandler extends AHandler {
    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(CLAN_CREATE, CLAN_APPLICATION_LIST, CLAN_CHECKIN, CLAN_REQ, CLAN_ANSWER_REQ,
                CLAN_MEMBER_LIST, CLAN_INFO, CLAN_KICK_MEMBER, CLAN_LEAVE, CLAN_DYNAMIC_DETAIL, CLAN_DYNAMIC_STATUS, CLAN_DYNAMIC_REWARD,
                CLAN_DYNAMIC_REWARD_BOX, CLAN_FINDING, CLAN_SET_JOIN_RULE, CLAN_SET_POSITION, CLAN_USER_UPDATE_STATE, CLAN_MAIL_TO_MEMBER,
                CLAN_CHANGE_NAME, CLAN_CHAT, CLAN_CHANGE_AVATAR_INTRO, CLAN_CHAT_LIST, CLAN_START_QUEST, CLAN_LIST_QUEST, CLAN_UPGRADE_QUEST,
                CLAN_RECEIVE_QUEST, CLAN_CONTRIBUTE_INFO, CLAN_CONTRIBUTE, CLAN_CONTRIBUTE_TOP, CLAN_UP_LEVEL, CLAN_HONOR_STATUS, CLAN_HONOR,
                CLAN_HONOR_GET_BONUS);
        actions.forEach(action -> mHandler.put(action, this));
    }

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
                default -> {
                    if (user.getClan() == 0) {
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
                        case IAction.CLAN_CONTRIBUTE_INFO -> contributeInfo();
                        case IAction.CLAN_CONTRIBUTE -> contribute();
                        case IAction.CLAN_CONTRIBUTE_TOP -> contributeTop();
                        // clan dynamic
                        case IAction.CLAN_DYNAMIC_STATUS -> dynamicStatus();
                        case IAction.CLAN_DYNAMIC_DETAIL -> dynamicDetail();
                        case IAction.CLAN_DYNAMIC_REWARD -> dynamicReward();
                        case IAction.CLAN_DYNAMIC_REWARD_BOX -> dynamicBox();
                        // upgrade level
                        case IAction.CLAN_UP_LEVEL -> upLevel();
                        case IAction.CLAN_HONOR_STATUS -> honorStatus();
                        case IAction.CLAN_HONOR_GET_BONUS -> honorGetBonus();
                        case IAction.CLAN_HONOR -> honor();
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

    void honor() {
        int num = getInputInt();
        if (num < 10) {
            addErrResponse(getLang(Lang.err_min_10_gem));
            return;
        }
        UserClanEntity uClan = Services.userDAO.getUserClan(mUser);
        if (uClan == null) {
            addErrParam();
            return;
        }

        UserDailyEntity uDaily = mUser.getUDaily();
        if (uDaily == null) {
            addErrSystem();
            return;
        }
        int numHonorCur = uDaily.getUDaily().getValue(DataDaily.NUM_HONOR);
        if (numHonorCur+num > CfgClan.config.maxNumHonor) {
            addErrResponse(getLang(Lang.err_max_num_honor));
            return;
        }
        num = Math.min(num, CfgClan.config.maxNumHonor - numHonorCur);
        int numHH = num / 5;
        List<Long> fee = Bonus.viewGem(-num);
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        fee.addAll(Bonus.viewItem(ItemKey.HUY_HIEU_BANG, numHH));
        ClanEntity clan = clanManager.getClan();
        if (clan.addHonor(num) && uClan.addHonor(num) && uDaily.getUDaily().setValueAndUpdate(DataDaily.NUM_HONOR, numHonorCur+ num)) {
            addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.CLAN_HONOR.getKey(), fee)));
            honorStatus();
            clan.addClanLog(Lang.clan_message_14,user.getName(),num+"");
        } else addErrSystem();
    }


    void create() {
        protocol.Pbmethod.CommonVector cmm = CommonProto.parseCommonVector(requestData);
        String name = cmm.getAString(0);
        if (name.contains("<") || name.contains(">") || name.contains("[") || name.contains("]")) {
            addErrResponse(getLang(Lang.name_err_1));
            return;
        }

        String intro = cmm.getAString(1);
        if (CfgChat.validText(name) || CfgChat.validText(intro)) {
            addErrResponse(getLang(Lang.name_not_found));
            return;
        }
        int avatar = (int) cmm.getALong(0);
        // type =0 : kim cuong -- type =1 ruby
        int type = cmm.getALongCount() > 2 ? (int) cmm.getALong(2) : 0;
        if (avatar < 1001 || avatar >= 10000) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        if (name.length() > CfgClan.config.clanNameLength) {
            addErrResponse(getLang(Lang.name_err_length));
            return;
        }
        if (intro.length() > CfgClan.config.introLength) {
            addErrResponse(getLang(Lang.err_intro_max_length));
            return;
        }
        if (user.getLevel() < CfgClan.config.levelCreateClan) {
            addErrResponse(String.format(getLang(Lang.user_function_level_required), CfgClan.config.levelCreateClan));
            return;
        }
        if (name == null || name.length() < 4) {
            addErrResponse(getLang(Lang.clan_name_min_length));
            return;
        }
        if (user.getClan() > 0) {
            addErrResponse(String.format(getLang(Lang.clan_leave_first), ClanManager.getInstance(user.getClan()).getClan().getName()));
            return;
        }
        ClanEntity findClan = Services.clanDAO.getClan(name);
        if (findClan != null) {
            addErrResponse(getLang(Lang.clan_name_exist));
            return;
        }

        List<Long> fee = CfgClan.getFeeCreate(type);
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        int ret = Services.clanDAO.createClan(user, name, CfgClan.config.feeCreate, intro, avatar, (int) cmm.getALong(1), type == 0 ? 1 : 3);
        if (ret > 0) {
            user.addGem(-CfgClan.config.feeCreate);
            user.setClan(ret);
            user.setClanName(name);
            user.setClanPosition(ClanPosition.LEADER.value);
            user.setClanAvatar(avatar);
            addResponse(getCommonVector((long) ret, (long) Bonus.BONUS_GEM, user.getGem(), (long) -CfgClan.config.feeCreate));
            if (CfgServer.isRealServer()) Actions.save(user, Actions.GCLAN, Actions.DCREATE, "id", ret);
            mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.JOIN_CLAN, 1);
        } else {
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

        if (user.getClan() > 0) {
            ClanEntity clan = ClanManager.getInstance(user.getClan()).getClan();
            addErrResponse(String.format(getLang(Lang.clan_leave_first), clan.getName()));
            return;
        }
        String key = ClanHandler.KEY_CLAN_LEAVE + user.getId();
        Long timeLeave = JCache.getInstance().getLongValue(key);
        if (timeLeave != null) {
            long timeRemain = CfgClan.timeWaitLeave + timeLeave - System.currentTimeMillis();
            if (timeRemain > 0) {
                addErrResponse(String.format(getLang(Lang.clan_wait_leave1), DateTime.formatTime(timeRemain / 1000)));
                return;
            }
        }
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
            // duyet tu dong
            List<Integer> memberIds = clan.getMemberId();
            if (!memberIds.contains(user.getId())) memberIds.add(user.getId());
            if (dao.addNewMember(clan, user.getId(), memberIds)) {
                clan.joinUser(user);
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

            if (memberUser.getClan() > 0 || dbUser.getClan() > 0) {
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
        if (user.getClanPosition() == ClanPosition.LEADER.value && clan.getMember() > 1) {
            addErrResponse(getLang(Lang.clan_leader_leave_error));
            return;
        }
        if (user.getClanJoin() != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR, -12);
            if (calendar.getTime().before(user.getClanJoin())) {
                addErrResponse(String.format(getLang(Lang.clan_wait_leave), DateTime.formatTime((user.getClanJoin().getTime() - calendar.getTimeInMillis()) / 1000)));
                return;
            }
        }
        clan.leaveClan(this, user);
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
            List<Long> aBonus = Bonus.receiveListItem(mUser, DetailActionType.DIEM_DANH_BANG_HOI.getKey(), Bonus.viewItem(CfgClan.HUY_HIEU_BANG, guildCoin));
            addResponse(protocol.Pbmethod.CommonVector.newBuilder().addALong(clan.getLevel()).
                    addALong(clan.getExp()).addALong(clanHonor).addALong(DateTime.secondsUntilEndDay()).addALong(clan.getHonor()).addAllALong(aBonus).build());
            CfgQuest.addNumQuest(mUser, DataQuest.CHECK_IN_CLAN, 1);
            clan.checkDynamic(mUser, CfgClan.CHECK_IN_CLAN, 1);
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
        // check số quest có nhỏ hơn số quest hiện tại không , nhỏ hơn thì thêm mới quest
        ResContributeEntity con = ResClan.getClanContribute(clan.getLevelQuest());
        List<Integer> dataNum = con.getData();
        int numQuest = dataNum.get(0);
        while (listQuestActive < numQuest) {
            update = true;
            List<Long> bonus = Bonus.xPerBonus(con.getBonus(), dataNum.get(1));
            List<Long> data = new ArrayList<>();
            long star = 1L;
            data.add(star);// star
            long timeRemain = System.currentTimeMillis() / 1000 + DateTime.HOUR_SECOND * CfgClan.config.timeQuest;
            data.add(timeRemain); // time done quest
            data.add((long) CfgClan.config.timeQuest); // time
            data.add((long) StatusType.LOCK.value); // status
            data.addAll(bonus); // all bonus
            quest.add(data);
            listQuestActive++;
            // result client
            List<Long> result = new ArrayList<>();
            result.add(star); // star
            result.add(timeRemain); // time remain
            result.add((long) CfgClan.config.timeQuest); // time
            result.add((long) StatusType.LOCK.value); // status
            result.add((long) CfgClan.config.upgradeQuest.get(0));
            result.addAll(bonus); // bonus
            pb.addAVector(getCommonVector(result));
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
        ResContributeEntity con = ResClan.getClanContribute(clan.getLevelQuest());
        List<Integer> dataNum = con.getData();
        for (int i = 0; i < dataNum.get(0); i++) {
            List<Long> bonus = Bonus.xPerBonus(con.getBonus(), dataNum.get(1));
            List<Long> data = new ArrayList<>();
            data.add(1L);// star
            data.add(System.currentTimeMillis() / 1000 + DateTime.HOUR_SECOND * CfgClan.config.timeQuest); // time done quest
            data.add((long) CfgClan.config.timeQuest); // time
            data.add((long) StatusType.LOCK.value); // status
            data.addAll(bonus); // all bonus
            quest.add(data);
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

    //end region

    //region contribute
    private void contributeInfo() {
        List<Long> data = new ArrayList<>();
        ClanEntity clan = ClanManager.getInstance(user.getClan()).getClan();
        if (clan == null) {
            addErrSystem();
            return;
        }
        int levelQuest = clan.getLevelQuest();
        ResContributeEntity curRes = ResClan.getClanContribute(levelQuest);
        data.add((long) levelQuest); //curLevel
        int nextLevel = levelQuest >= ResClan.maxLevelContribute ? 0 : levelQuest + 1;
        data.add((long) nextLevel); // nextLevel
        ResContributeEntity nextRes = ResClan.getClanContribute(nextLevel);
        data.add(clan.getContribute());// cur cống hiến
        data.add((long) curRes.getGold());// max cống hiến
        data.addAll(GsonUtil.toListLong(curRes.getData())); // cur turn - cur buff
        data.addAll(nextRes == null ? Arrays.asList(0L, 0L) : GsonUtil.toListLong(nextRes.getData())); // next turn - next buff
        data.add((long) CfgClan.config.contributeX1);
        data.add((long) CfgClan.config.contributeX10);
        addResponse(CLAN_CONTRIBUTE_INFO, getCommonVector(data));
    }

    private void contribute() {
        UserClanEntity userClan = Services.userDAO.getUserClan(mUser);
        if (userClan == null) {
            addErrParam();
            return;
        }
        ClanEntity clan = ClanManager.getInstance(user.getClan()).getClan();
        if (clan == null) {
            addErrSystem();
            return;
        }
        int input = getInputInt();
        if (input != 1 && input != 10) {
            addErrParam();
            return;
        }
        int numBuff = input == 1 ? CfgClan.config.contributeX1 : CfgClan.config.contributeX10;
        List<Long> fee = Bonus.viewGold(-numBuff);
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        clan.addContribute(numBuff);
        userClan.addContribute(numBuff);
        addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.BONUS_CLAN_CONTRIBUTE.getKey(input), fee)));
        contributeInfo();
        clan.addClanLog(Lang.clan_message_15, user.getName(),input+"");
    }

    private void contributeTop() {
        TopType topType = TopType.CLAN_CONTRIBUTE;
        Pbmethod.PbListUser pbListUser = (Pbmethod.PbListUser) TopMonitor.getInstance().get(topType, String.valueOf(user.getServer()), String.valueOf(user.getClan()));
        addResponse(pbListUser.toBuilder().build());
    }

    //end region


    //region dynamic

    private void dynamicStatus() {
        ClanEntity clan = ClanManager.getInstance(user.getClan()).getClan();
        if (clan == null) {
            addErrSystem();
            return;
        }
        UserClanEntity userClan = Services.userDAO.getUserClan(mUser);
        if (userClan == null) {
            addErrParam();
            return;
        }
        Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
        List<Long> info = new ArrayList<>();
        int box = clan.getPointDynamic() / CfgClan.config.maxPointDynamic - userClan.getBoxDynamic().get(1);
        int curValue = clan.getPointDynamic() % CfgClan.config.maxPointDynamic;
        info.add(box > 0 ? box : 0L);// box có thể nhận
        info.add((long) curValue); // cur value
        info.add((long) CfgClan.config.maxPointDynamic); // max value
        pb.addAVector(getCommonVector(info));
        // list cell
        List<Integer> dynamicReceive = userClan.getDynamicReceive();
        List<CellDynamic> cells = clan.getDynamics();
        for (int i = 0; i < cells.size(); i++) {
            CellDynamic cell = cells.get(i);
            if (!dynamicReceive.contains(cell.id)) { // chưa nhận
                Pbmethod.CommonVector.Builder cmm = Pbmethod.CommonVector.newBuilder();
                cmm.addALong(cell.id);//id
                cmm.addAllALong(CfgClan.getBonusDynamic()); // bonus
                ResDynamicTypeEntity res = mDynamicType.get(cell.type);
                cmm.addAString(cell.name); // name user
                cmm.addAString(res.getName()); // desc
                cmm.addAString(res.getTitle()); // title
                pb.addAVector(cmm);
            }
        }
        addResponse(CLAN_DYNAMIC_STATUS, pb.build());
    }

    private void dynamicDetail() {
        UserClanEntity userClan = Services.userDAO.getUserClan(mUser);
        if (userClan == null) {
            addErrParam();
            return;
        }
        List<Integer> status = userClan.getDynamicDetail();
        List<ResDynamicTypeEntity> res = aDynamicType;
        Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
        for (int i = 0; i < res.size(); i++) {
            ResDynamicTypeEntity db = res.get(i);
            Pbmethod.CommonVector.Builder cm = Pbmethod.CommonVector.newBuilder();
            int curValue = 0; // todo check current
            switch (db.getType()) {
                case 1 -> { // điểm danh bang hội 1 lần
                    DataQuest dayData = mUser.getUQuest().getDataQuest(QuestType.QUEST_D);
                    curValue = dayData.getValue(DataQuest.CHECK_IN_CLAN);
                }
                case 2 -> { // attack boss clan
                    DataDaily dataDaily = mUser.getUserDaily().getUDaily();
                    curValue = dataDaily.getValue(DataDaily.ATTACK_BOS_CLAN);
                }
                case 3 -> {
                    DataQuest dayData = mUser.getUQuest().getDataQuest(QuestType.QUEST_D);
                    curValue = dayData.getValue(DataQuest.CUR_POINT_D);
                }
            }
            cm.addALong(curValue); // cur value
            cm.addALong(db.getNumber()); // max value
            cm.addALong(status.get(i) != StatusType.DONE.value && curValue >= db.getNumber() ? StatusType.DONE.value : status.get(i)); // status
            cm.addALong(db.getGo()); // goto id
            cm.addAString(db.getName());// name cell
            pb.addAVector(cm);
        }
        addResponse(CLAN_DYNAMIC_DETAIL, pb.build());
    }


    private void dynamicReward() {
        ClanEntity clan = ClanManager.getInstance(user.getClan()).getClan();
        if (clan == null) {
            addErrSystem();
            return;
        }
        UserClanEntity userClan = Services.userDAO.getUserClan(mUser);
        if (userClan == null) {
            addErrParam();
            return;
        }
        // list cell
        List<Integer> dynamicReceive = userClan.getDynamicReceive();
        List<CellDynamic> cells = clan.getDynamics();
        int inputType = getInputInt();
        if (inputType == -1) {
            int numBonus = 0;
            for (int i = 0; i < cells.size(); i++) {
                if (!dynamicReceive.contains(cells.get(i).id)) {
                    dynamicReceive.add(cells.get(i).id);
                    numBonus++;
                }
            }
            if (numBonus == 0) {
                addErrResponse(getLang(Lang.err_no_bonus));
                return;
            }
            if (userClan.update(List.of("dynamic_receive", StringHelper.toDBString(dynamicReceive)))) {
                userClan.setDynamicReceive(dynamicReceive.toString());
                addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.BONUS_CLAN_DYNAMIC_ALL.getKey(), Bonus.xBonus(CfgClan.getBonusDynamic(), numBonus))));
                dynamicStatus();
            } else addErrSystem();
        } else {
            CellDynamic target = cells.stream().filter(c -> c.id == inputType).findFirst().orElse(null);
            if (target == null) {
                addErrResponse(getLang(Lang.err_no_bonus));
                return;
            }
            if (dynamicReceive.contains(target.id)) {
                addErrResponse(getLang(Lang.err_received_bonus));
                return;
            }
            dynamicReceive.add(target.id);
            if (userClan.update(List.of("dynamic_receive", StringHelper.toDBString(dynamicReceive)))) {
                userClan.setDynamicReceive(dynamicReceive.toString());
                addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.BONUS_CLAN_DYNAMIC.getKey(inputType), CfgClan.getBonusDynamic())));
                dynamicStatus();
            } else addErrSystem();
        }
    }

    private void dynamicBox() {
        ClanEntity clan = ClanManager.getInstance(user.getClan()).getClan();
        if (clan == null) {
            addErrSystem();
            return;
        }
        UserClanEntity userClan = Services.userDAO.getUserClan(mUser);
        if (userClan == null) {
            addErrParam();
            return;
        }
        List<Integer> boxs = userClan.getBoxDynamic();
        int box = clan.getPointDynamic() / CfgClan.config.maxPointDynamic - boxs.get(1);
        if (box <= 0) {
            addErrResponse(getLang(Lang.err_no_bonus));
            return;
        }
        boxs.set(1, boxs.get(1) + box);
        if (userClan.update(List.of("box_dynamic", StringHelper.toDBString(boxs)))) {
            userClan.setBoxDynamic(boxs.toString());
            addBonusToastPlus(Bonus.receiveListItem(mUser, DetailActionType.BONUS_CLAN_DYNAMIC_BOX.getKey(box), CfgClan.getBonusBoxDynamic(box)));
            addResponse(getCommonVector(0L));
        } else addErrSystem();
    }

    //end region

    private protocol.Pbmethod.PbListChat protoListChat(List<ChatObject> aChat) {
        protocol.Pbmethod.PbListChat.Builder builder = protocol.Pbmethod.PbListChat.newBuilder();
        aChat.forEach(chat -> builder.addAChat(chat.toProto()));
        return builder.build();
    }
}
