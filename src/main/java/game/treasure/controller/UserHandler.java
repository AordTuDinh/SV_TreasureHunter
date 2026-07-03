package game.treasure.controller;

import game.battle.object.Point;
import game.config.*;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.treasure.mapping.*;
import game.treasure.mapping.main.*;
import game.treasure.server.IAction;
import game.treasure.service.resource.*;
import game.treasure.service.user.Actions;
import game.treasure.service.user.Bonus;
import game.treasure.mapping.UserSkinEntity;
import game.treasure.task.dbcache.MailCreatorCache;
import game.monitor.Online;
import game.monitor.TopMonitor;
import game.object.MyUser;
import game.protocol.CommonProto;
import io.netty.channel.Channel;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.*;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static game.config.lang.Lang.*;

public class UserHandler extends AHandler {
    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(CREATE_NAME, USER_INFO, DAME_SKIN_EQUIP, CHANGE_LANG,
                CHAT_FRAME_EQUIP, USE_GIFT_CODE, TRIAL_EQUIP, BUFF_INFO, RANKING_STATUS, TUTORIAL_STATUS,
                TUTORIAL_QUEST_RECEIVE, TUTORIAL_GO_TO, TUTORIAL_QUEST_STATUS, RANKING_INFO, SEND_MAIL,
                HELP_VALUE, CHANGE_NAME, SKIN_EQUIP, USER_DATA_INFO, UPDATE_NEXT_DAY, SET_AUTO, SET_AUTO_RANGE, CANCEL_PROTECT);
        actions.forEach(action -> mHandler.put(action, this));
    }

    static UserHandler instance;

    public static UserHandler getInstance() {
        if (instance == null) {
            instance = new UserHandler();
        }
        return instance;
    }

    @Override
    public AHandler newInstance() {
        return new UserHandler();
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        try {
            switch (actionId) {
                case POINT_DATA -> pointData();
                case CREATE_NAME -> createName();
                case USER_DATA_INFO -> userDataInfo();
                case CHANGE_NAME -> changeName();
                case SKIN_EQUIP -> skinEquip();
                case HELP_VALUE -> helpValue();
                case RANKING_INFO -> rankInfo();
                case RANKING_STATUS -> rank();
                case SEND_MAIL -> sendMail();
                case USER_INFO -> userInfo();
                case UPDATE_NEXT_DAY -> updateNextDay();
                case TUTORIAL_STATUS -> tutorial();
                case TUTORIAL_QUEST_STATUS -> tutorialQuestStatus(mUser, this);
                case TUTORIAL_QUEST_RECEIVE -> tutorialQuestReceive();
                case DAME_SKIN_EQUIP -> dameSkinEquip();
                case CHAT_FRAME_EQUIP -> chatFrameEquip();
                case TRIAL_EQUIP -> trialEquip();
                case USE_GIFT_CODE -> useGiftCode();
                case BUFF_INFO -> buffInfo(mUser);
                case CHANGE_LANG -> changeLang(getInputString());
                case SET_AUTO -> setAuto();
                case SET_AUTO_RANGE -> setAutoRange();
                case CANCEL_PROTECT -> cancelProtect();
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    private void cancelProtect() {
        long protectedEnd = mUser.getUData().getTimeProtected();
        if (protectedEnd <= System.currentTimeMillis()) {
            addResponse(getCommonVector(0L));
            return;
        }
        if (!mUser.getUData().update(List.of("time_protected", 0L))) {
            addErrSystem();
            return;
        }
        mUser.getUData().setTimeProtected(0);
        if (mUser.getPlayer() != null) {
            mUser.getPlayer().setTimeProtectedEnd(0);
        }
        addResponse(getCommonVector(0L));
    }

    private void setAuto() {
        List<Long> input = getInputALong();
        if (input.size() < 3) {
            addErrParam();
            return;
        }
        int type = input.get(0).intValue();
        int autoId = input.get(1).intValue();
        int value = input.get(2).intValue();
        if (value != 0 && value != 1) {
            addErrParam();
            return;
        }

        UserSettingsEntity uSetting = mUser.getUSetting();
        if (type == CfgMaterial.AUTO_SELL_TYPE_ITEM) {
            if (Pbmethod.AutoSell.valueOf(autoId) == null) {
                addErrParam();
                return;
            }
            List<Integer> list = uSetting.getAutoSellItemList();
            if (list.get(autoId) != value) {
                list.set(autoId, value);
                if (!uSetting.updateAutoSellItem(StringHelper.toDBString(list))) {
                    addErrSystem();
                    return;
                }
            }
        } else if (type == CfgMaterial.AUTO_SELL_TYPE_MATERIAL) {
            if (!CfgMaterial.isValidAutoSellMaterialIndex(autoId)) {
                addErrParam();
                return;
            }
            List<Integer> list = uSetting.getAutoSellMaterialList();
            if (list.get(autoId) != value) {
                list.set(autoId, value);
                if (!uSetting.updateAutoSellMaterial(StringHelper.toDBString(list))) {
                    addErrSystem();
                    return;
                }
            }
        } else {
            addErrParam();
            return;
        }
        addResponse(getCommonVector(type, autoId, value));
    }

    private void setAutoRange() {
        List<Long> input = getInputALong();
        if (input.size() < 2) {
            addErrParam();
            return;
        }

        int attackRange = input.get(0).intValue();
        int hpRange = input.get(1).intValue();
        if (attackRange < 2 || attackRange > 10 || hpRange < 10 || hpRange > 100) {
            addErrParam();
            return;
        }

        UserSettingsEntity uSetting = mUser.getUSetting();
        List<Integer> list = uSetting.getAutoRangeList();
        if (list.get(0) != attackRange || list.get(1) != hpRange) {
            list.set(0, attackRange);
            list.set(1, hpRange);
            if (!uSetting.updateAutoRange(StringHelper.toDBString(list))) {
                addErrSystem();
                return;
            }
        }

        addResponse(getCommonVector(attackRange, hpRange));
    }

    private void changeLang(String inputString) {
        String lang = inputString.toLowerCase();
        if (lang.equals(user.getLang())) {
            addResponseError();
            return;
        }
        if (user.update(List.of("lang", Lang.getValidLang(lang)))) {
            addResponse(getCommonVector(lang));
        } else addErrParam();
    }


    // tao nhan vat
    void createName() {
        protocol.Pbmethod.CommonVector cmm = CommonProto.parseCommonVector(requestData);
        String userName = cmm.getAString(0);
        if (!StringHelper.isEmpty(user.getName())) {
            addErrParam();
            return;
        }
        if (!CfgChat.validName(userName)) {
            addErrResponse(getLang(name_not_found));
            return;
        }

        if (userName.length() < 6) {
            addErrResponse(getLang(name_err_min_length));
            return;
        }
        if (userName.length() > CfgUser.maxLengthName) {
            addErrResponse(getLang(name_err_length));
            return;
        }
        if (DBJPA.count(CfgServer.DB_DSON + "user", "name", userName) > 0) {
            addErrResponse(Lang.getTitle(mUser, Lang.user_name_exist));
            return;
        }
        if (userName.contains("<") || userName.contains(">") || userName.contains("[") || userName.contains("]")) {
            addErrResponse(getLang(Lang.err_string_prefix));
            return;
        }


        if (mUser.getUser().updateCreateUser(userName)) {
            Pbmethod.PbLoginGame.Builder builder = Pbmethod.PbLoginGame.newBuilder();
            builder.setUser(user.toProto(mUser));
            //  user point
            addResponse(builder.build());
        } else addErrResponse();
    }


    void userDataInfo() {
        addResponse(IAction.USER_DATA_INFO, mUser.getUData().toProto(mUser));
    }

    void pointData() {
        mUser.reCalculatePoint();
    }

    void useGiftCode() {
        String gift = getInputString().toUpperCase().trim();
        ResGiftCodeEntity resGift = ResGift.getGiftCode(gift);
        if (resGift == null) {
            addErrResponse(getLang(Lang.err_not_gift_code));
            return;
        }

        if (resGift.expire()) {
            addErrResponse(getLang(Lang.err_not_gift_code_expire));
            return;
        }

        boolean exists = DBJPA.exists("user_gift_code", "user_id", user.getId(), "gift", gift);
        if (exists) {
            addErrResponse(getLang(Lang.err_gift_code_use));
            return;
        }
        if (resGift.getType() == GiftCodeType.ONE && resGift.getDataInt() != 0) {
            addErrResponse(getLang(Lang.err_gift_code_use));
            return;
        }
        if (resGift.getType() == GiftCodeType.GROUP_USER && !resGift.getListDataInt().contains(user.getId())) {
            addErrResponse(getLang(Lang.err_gift_code_use_not_allow));
            return;
        }
        // check đã ăn gift code loại này chưa, ăn rồi thì thôi
        int eventId = resGift.getEventGift();
        // =0 thì cho ăn nhiều lần
        if (eventId != 0 && DBJPA.exists("user_gift_code", "user_id", user.getId(), "event_gift", eventId)) {
            addErrResponse(getLang(Lang.err_gift_code_use_type));
            return;
        }


        // set lại vào data
        if (resGift.getType() == GiftCodeType.ONE && !resGift.updateData("1")) {
            addErrSystem();
            return;
        }

        UserGiftCodeEntity uGift = new UserGiftCodeEntity(user.getId(), gift, resGift.getEventGift());
        if (!DBJPA.save(uGift)) {
            addErrSystem();
            return;
        }
        addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.BONUS_GIFT_CODE.getKey(gift), resGift.getBonus())));
    }

    void helpValue() {
        String k = CommonProto.parseCommonVector(requestData).getAString(0);
        ConfigHelpEntity help = ResHelp.mHelp.get(k);
        if (help == null) {
            addResponse(CommonProto.getCommonVectorProto(null, Arrays.asList(k, "")));
            return;
        }
        if (Lang.instance(mUser).getLocale().equalsIgnoreCase(LOCALE_EN)) {
            addResponse(CommonProto.getCommonVectorProto(null, Arrays.asList(help.getK(), help.getEn())));
        } else if (Lang.instance(mUser).getLocale().equalsIgnoreCase(LOCALE_VI)) {
            addResponse(CommonProto.getCommonVectorProto(null, Arrays.asList(help.getK(), help.getVi())));
        } else if (Lang.instance(mUser).getLocale().equalsIgnoreCase(LOCALE_RU)) {
            addResponse(CommonProto.getCommonVectorProto(null, Arrays.asList(help.getK(), help.getRu())));
        } else if (Lang.instance(mUser).getLocale().equalsIgnoreCase(LOCALE_KM)) {
            addResponse(CommonProto.getCommonVectorProto(null, Arrays.asList(help.getK(), help.getKm())));
        } else if (Lang.instance(mUser).getLocale().equalsIgnoreCase(LOCALE_ZH)) {
            addResponse(CommonProto.getCommonVectorProto(null, Arrays.asList(help.getK(), help.getZh())));
        } else if (Lang.instance(mUser).getLocale().equalsIgnoreCase(LOCALE_JP)) {
            addResponse(CommonProto.getCommonVectorProto(null, Arrays.asList(help.getK(), help.getJp())));
        } else {
            addResponse(CommonProto.getCommonVectorProto(null, Arrays.asList(k, "")));
        }
    }


    void rankInfo() {
        int type = getInputInt();
        RankingType rankType = RankingType.get(type);
        if (rankType == null) {
            addServiceErrResponse();
            return;
        }
        addResponse(getCommonIntVector(rankType.ids));
    }

    private void rank() {
        int type = (int) CommonProto.parseCommonVector(requestData).getALong(0);
        TopType topType = TopType.get(type);
        if (topType == null) {
            addErrResponse(getLang(err_params));
            return;
        }
        if (topType.type == TopType.CLAN_TYPE) { // for clan
            Pbmethod.PbListClan pbClanList = (Pbmethod.PbListClan) TopMonitor.getInstance().get(topType, String.valueOf(user.getServer()));
            List<Pbmethod.PbClan> pbClans = pbClanList.getClanList();
            Pbmethod.PbClan myClan = pbClans.stream().filter(pb -> pb.getId() == user.getClan()).findFirst().orElse(null);
            if (myClan != null) {
                int index = pbClans.indexOf(myClan);
                myClan.toBuilder().setRank(index + 1);
                addResponse(pbClanList.toBuilder().setMyClan(myClan).build());
                return;
            } else if (!StringHelper.isEmpty(topType.sqlMyRank) && !StringHelper.isEmpty(topType.sqlMyInfo)) {
                Integer myRank = dbGetRank(String.format(topType.sqlMyRank, user.getServer(), user.getClan()));
                ClanEntity clan = dbGetClanInfo(String.format(topType.sqlMyInfo, user.getClan()));
                if (myRank != null && clan != null) {
                    if (myRank == 0) myRank = 9999;
                    addResponse(pbClanList.toBuilder().setMyClan(clan != null ? clan.toProto(myRank, topType.value) : null).build());
                    return;
                }
            }
            addResponse(pbClanList);
        } else {
            Pbmethod.PbListUser pbListUser = null;
            if (topType.type == TopType.CLAN_MEMBER_TYPE) { // for clan  member
                pbListUser = (Pbmethod.PbListUser) TopMonitor.getInstance().get(topType, String.valueOf(user.getServer()), String.valueOf(user.getClan()));
            } else {
                pbListUser = (Pbmethod.PbListUser) TopMonitor.getInstance().get(topType, String.valueOf(user.getServer()));
            }
            List<Pbmethod.PbUser> pbUsers = pbListUser.getAUserList();
            Pbmethod.PbUser myProto = pbUsers.stream().filter(pbUser -> pbUser.getId() == user.getId()).findFirst().orElse(null);
            if (myProto != null) {
                int index = pbUsers.indexOf(myProto) + 1;
                myProto.toBuilder().setRank(index);
                addResponse(pbListUser.toBuilder().setMyInfo(myProto).build());
                if (topType == TopType.USER_POWER) user.checkRankPower(index);
            } else if (!StringHelper.isEmpty(topType.sqlMyRank) && !StringHelper.isEmpty(topType.sqlMyInfo)) {
                Integer myRank = dbGetRank(String.format(topType.sqlMyRank, user.getServer(), user.getId()));
                TopUserEntity topUser = dbGetInfo(String.format(topType.sqlMyInfo, user.getId()));
                if (topType == TopType.USER_POWER && myRank != null && topUser != null) {
                    user.checkRankPower(myRank);
                }
                addResponse(pbListUser.toBuilder().build());
                if (topUser == null) {
                    addResponse(pbListUser.toBuilder().setMyInfo(toProtoNull()).build());

                } else {
                    addResponse(pbListUser.toBuilder().setMyInfo(topUser.toProto(myRank, topType)).build());
                }
            } else addDefault();
        }
    }


    public protocol.Pbmethod.PbUser toProtoNull() {
        protocol.Pbmethod.PbUser.Builder pb = user.toProto().toBuilder();
        pb.setPointRank(0);
        pb.clearVip();
        pb.addAllVip(List.of(user.getVip()));
        pb.setRank(9999);
        return pb.build();
    }


    void sendMail() {
        Pbmethod.CommonVector comm = CommonProto.parseCommonVector(requestData);
        int friendId = (int) comm.getALong(0);
        String content = comm.getAString(0);
        if (content.length() > CfgUser.maxLengthMail) {
            addErrResponse(getLang(content_err_length));
            return;
        }
        content = CfgChat.replaceInvalidWord(content);
        if (dbAddMailToFriend(friendId, content)) {
            addErrResponse(getLang(Lang.send_mail_successful));
            addResponse(null);
        } else addErrResponse();
    }

    void userInfo() {
        int userId = getInputInt();
        MyUser online = Online.getMUser(userId);
        if (online != null && online.getUser() != null && online.getPlayer() != null) {
            online.getUser().reCalculatePoint(online);
            addResponse(online.getUser().toProto(online));
            return;
        }
        UserEntity user = Online.getDbUser(userId);
        if (user == null) {
            addErrResponse(getLang(user_not_found));
            return;
        }
        addResponse(user.toProto());
    }

    void updateNextDay() {
//        Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
//        pb.addAVector(getCommonVector(1));
//        List<UserItemEntity> uItem = mUser.getResources().getMItem().values().stream().toList();
//        List<Long> bonusExpire = new ArrayList<>();
//        for (int i = 0; i < uItem.size(); i++) {
//            bonusExpire.addAll(Bonus.viewItem(uItem.get(i).getItemId(), -uItem.get(i).getQuantity()));
//        }
//        pb.addAVector(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.UPDATE_BONUS_NEXT_DAY.getKey(), bonusExpire)));
//        addResponse(pb.build());
    }

    private void tutorial() {
        int curTut = getInputInt();
        if (curTut == mUser.getUData().getTutorial() || mUser.getUData().update(List.of("tutorial", curTut))) {
            mUser.getUData().setTutorial(curTut);
            addResponse(getCommonVector(curTut));
        } else addErrSystem();
    }

    public static void tutorialQuestStatus(MyUser mUser, AHandler handler) {
        ResTutorialQuestEntity resQuest = ResQuest.mTutQuest.get(mUser.getUData().getQuestTutorial());
        if (resQuest == null) {
            handler.addErrResponse(handler.getLang(err_null_quest));
            return;
        }
        int status = CfgQuest.getQuestTutStatus(mUser, resQuest);
        Pbmethod.CommonVector.Builder builder = Pbmethod.CommonVector.newBuilder();
        builder.addALong(status).addALong(mUser.getUData().getQuestTutorial()).addALong(mUser.getUData().getQuestTutorialNumber()).addALong(resQuest.getNum()).addALong(resQuest.getGotoId()).addALong(resQuest.getType().value);
        builder.addAString(resQuest.getTitle(mUser));
        builder.addAString(resQuest.getBonus());
        handler.addResponse(TUTORIAL_QUEST_STATUS, builder.build());
    }

    private void tutorialQuestReceive() {
        ResTutorialQuestEntity resQuest = ResQuest.mTutQuest.get(mUser.getUData().getQuestTutorial());
        if (resQuest == null) {
            addErrResponse(getLang(err_null_quest));
            return;
        }
        int status = CfgQuest.getQuestTutStatus(mUser, resQuest);
        if (status == StatusType.RECEIVE.value) {
            if (mUser.getUData().updateTutorialQuest()) {
                addResponse(CommonProto.getCommonVectorProto(Bonus.receiveListItem(mUser, DetailActionType.RECEIVE_TUTORIAL_QUEST.getKey(mUser.getUData().getQuestTutorial() - 1), resQuest.getABonus())));
                CfgEvent.processTriggerEventTimer(mUser, mUser.getUData().getQuestTutorial(), TriggerEventTimer.QUEST_TUTORIAL_LEVEL);
            } else {
                addErrResponse();
            }
        } else {
            tutorialQuestStatus(mUser, this);
        }

    }

//    private void tutorialGoTo() {
//        List<Long> inputs = getInputALong();
//        MapType mapType = MapType.get(inputs.get(0).intValue());
//        ResMapEntity map = ResMap.getMap(mapType);
//        Pos posInit = new Pos(inputs.get(2) / 1000, inputs.get(3) / 1000);
//        if (map == null) {
//            addErrParam();
//            return;
//        }
//        if (mUser == null) {
//            addResponse(LOGIN_REQUIRE, null);
//            return;
//        }
//        int chanelId = mUser.getRoomChanelId();
//        String keyRoom = CfgBattle.getKeyRoom(mUser, mapType.value, chanelId);
//        BaseRoom curRoom = (BaseRoom) ChUtil.get(channel, ChUtil.KEY_ROOM);
//        if (curRoom != null && (curRoom.getKeyRoom().equals(keyRoom) || !curRoom.allowChangeChanel())) {
//            if (curRoom.getKeyRoom().equals(keyRoom)) {
//                addErrResponse(getLang(Lang.err_in_room_already));
//                return;
//            }
//            if (!curRoom.allowChangeChanel()) {
//                addErrResponse(getLang(Lang.err_unauthorized));
//                return;
//            }
//        }
//
//
//        // xóa khỏi room cũ
//        Player player = mUser.getPlayer();
//        if (curRoom != null && curRoom.hasPlayer(player.getId())) {
//            curRoom.removePlayer(player.getId());
//        }
//        // check có room hay chưa, có rồi thì join
//        BaseRoom room = (BaseRoom) TaskMonitor.getInstance().getRoom(keyRoom);
//        player.clearDataForChangeRoom(posInit);
//        if (room == null) {  // tao room moi
//            List<Character> players = new ArrayList<>();
//            players.add(player);
//            switch (mapType) {
//                default:
//                    room = new HomeRoom(map, players, keyRoom) {
//                    };
//                    break;
//            }
//            TaskMonitor.getInstance().addRoom(room);
//        } else { // join vào room có sẵn
//            if (room.getAPlayer().size() > mapType.maxPlayer) {
//                addErrResponse(Lang.instance(mUser).get(Lang.err_full_player));
//                return;
//            }
//            room.addPlayer(player);
//        }
//        ChUtil.set(channel, ChUtil.KEY_ROOM, room);
//        // tra ve id teleport next
//        addResponse(INIT_MAP, CfgBattle.genInitMap(mapType.value, mUser.getRoomChanelId(), PopupType.NULL));
//    }

    private void skinEquip() {
        protocol.Pbmethod.CommonVector cv = CommonProto.parseCommonVector(requestData);
        if (cv.getALongCount() < 2) {
            addErrParam();
            return;
        }
        int part = (int) cv.getALong(0);
        long userSkinId = cv.getALong(1);
        if (part < 0 || part >= UserSkinEntity.PART_COUNT) {
            addErrParam();
            return;
        }
        Pbmethod.SkinType skinType = Pbmethod.SkinType.valueOf(part);
        if (skinType == null) {
            addErrParam();
            return;
        }
        UserSkinEntity uSkin = mUser.getResources().getSkin(userSkinId);
        if (uSkin == null || uSkin.getUserId() != user.getId() || uSkin.getType() != part) {
            addErrResponse();
            return;
        }
        if (user.updateSkin(skinType, userSkinId, uSkin.getSkinId())) {
            addResponse(getCommonVector(part, userSkinId, uSkin.getSkinId()));
        } else addErrResponse();
    }

    private void dameSkinEquip() {
        int skinId = getInputInt();
        if (skinId != 0 && !mUser.getUData().getListDameSkin().contains(skinId)) {
            addErrResponse(getLang(Lang.err_no_has_dame_skin));
            return;
        }
        if (mUser.getUData().update(List.of("dame_skin_equip", skinId))) {
            mUser.getUData().setDameSkinEquip(skinId);
            mUser.getPlayer().protoStatus(Pbmethod.SubStateType.UPDATE_TEXT_DAME, (long) skinId);
            addResponse(getCommonVector(skinId));
        } else addErrSystem();
    }

    private void chatFrameEquip() {
        int frameId = getInputInt();
        if (frameId != 0 && !mUser.getUData().getListChatFrame().contains(frameId)) {
            addErrResponse(getLang(Lang.err_no_has_chat_frame));
            return;
        }
        if (mUser.getUData().update(List.of("chat_frame_equip", frameId))) {
            mUser.getUData().setChatFrameEquip(frameId);
            mUser.getPlayer().protoStatus(Pbmethod.SubStateType.UPDATE_CHAT_FRAME, (long) frameId);
            addResponse(getCommonVector(frameId));
        } else addErrSystem();
    }

    private void trialEquip() {
        int trialId = getInputInt();
        if (trialId != 0 && !mUser.getUData().getListIntTrial().contains(trialId)) {
            addErrResponse(getLang(Lang.err_no_has_trial));
            return;
        }
        if (mUser.getUData().update(List.of("trial_equip", trialId))) {
            mUser.getUData().setTrialEquip(trialId);
            mUser.getPlayer().protoStatus(Pbmethod.SubStateType.UPDATE_TRIAL, (long) trialId);
            addResponse(getCommonVector(trialId));
        } else addErrSystem();
    }

    public static void buffInfo(MyUser mUser) {
        if (mUser == null || mUser.getPlayer() == null || mUser.getChannel() == null)
            return;
        List<Long> wire = game.treasure.service.user.UserBuff.buildWire(mUser);
        Util.sendProtoData(mUser.getChannel(), CommonProto.getCommonVector(wire), IAction.BUFF_INFO);
    }

    private void addDefault() {
        addResponse(Pbmethod.PbListUser.newBuilder().setMyInfo(user.protoTinyUser(9999)).build());
    }

    void checkInStatus() {
        if (!CfgFeature.isOpenFeature(FeatureType.CHECK_IN, mUser, this)) {
            return;
        }
        Pbmethod.CommonVector.Builder pb = Pbmethod.CommonVector.newBuilder();
        int numCheckin = mUser.getUData().getNumCheckin().get(CfgCheckin.NUM_CHECKIN);
        pb.addALong(numCheckin);
        pb.addALong(mUser.getUData().getStatusCheckIn());
        pb.addALong(CfgCheckin.config.bonusCheckin.size());
        pb.addAString(CfgCheckin.getBonusCheckin());
        addResponse(pb.build());
    }

    void changeName() {
        String name = CommonProto.parseCommonVector(requestData).getAString(0);
        if (name.equals(user.getName())) {
            addErrResponse(getLang(Lang.err_name_sake));
            return;
        }
        if (!CfgChat.validName(name)) {
            addErrResponse(getLang(name_not_found));
            return;
        }


        if (name.contains("<") || name.contains(">") || name.contains("[") || name.contains("]")) {
            addErrResponse(getLang(Lang.name_err_1));
            return;
        }
        int feeGem = 200;
        if (user.getGem() < feeGem) {
            addErrResponse(getLang(Lang.err_not_enough_gem));
            return;
        }
        if (DBJPA.count(CfgServer.DB_DSON + "user", "name", name) > 0) {
            addErrResponse(getLang(Lang.user_name_exist));
//            Logs.debug("name_exist=" + user.getId() + " " + name);
            return;
        }
        if (updateName(user, name, feeGem)) {
            user.addGem(-feeGem);
            user.setName(name);
            addResponse(Pbmethod.CommonVector.newBuilder().addALong(user.getGem()).addAString(user.getName()).build());
            Actions.logGem(user, "change_name", -feeGem);
//            Actions.save(user, "user", "name", "date_created", DateTime.getFullDate(user.getDateCreated()), "last_login", DateTime.getFullDate(user.getLastLogin()));
        } else addErrResponse();
    }


    private boolean updateName(UserEntity user, String name, int feeGem) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            session.getTransaction().begin();
            Query query = session.createNativeQuery("update user set name=:name, gem=gem-" + feeGem + " where id=" + user.getId());
            query.setParameter("name", name);
            query.executeUpdate();
            session.getTransaction().commit();
            return true;
        } catch (Exception ex) {
            String strException = GUtil.exToString(ex);
            if (!strException.contains("Incorrect string value")) {
                Logs.error(strException);
            }
        } finally {
            closeSession(session);
        }
        return false;
    }

    private Integer dbGetRank(String sql) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            List listResult = session.createNativeQuery(sql).getResultList();
            return listResult.isEmpty() ? null : ((BigInteger) listResult.get(0)).intValue();
        } catch (Exception ex) {
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return null;
    }


    boolean dbAddMailToFriend(int userId, String content) {
        return MailCreatorCache.sendMail(UserMailEntity.builder().senderId(user.getId()).userId(userId).senderName(String.format(getLang("mail_from"), user.getName())).title(String.format(getLang("title_mail_friend"), user.getName())).message(content + "\n" + getLang("content_mail_friend")).build());
    }


    private TopUserEntity dbGetInfo(String sql) {
        TopUserEntity aUser = (TopUserEntity) DBJPA.getUnique(DBJPA.getEntityManager(), sql, TopUserEntity.class);
        return aUser == null ? null : aUser;
    }

    private ClanEntity dbGetClanInfo(String sql) {
        List<ClanEntity> aClans = (List<ClanEntity>) DBJPA.getUnique(DBJPA.getEntityManager(), sql, ClanEntity.class);
        return aClans == null ? null : aClans.isEmpty() ? null : aClans.get(0);
    }
}
