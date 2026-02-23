package game.dragonhero.controller;

import game.battle.model.Character;
import game.battle.model.Player;
import game.battle.object.Point;
import game.battle.object.Pos;
import game.battle.type.StateType;
import game.config.*;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.dragonhero.mapping.*;
import game.dragonhero.mapping.main.*;
import game.dragonhero.server.IAction;
import game.dragonhero.service.Services;
import game.dragonhero.service.resource.*;
import game.dragonhero.service.user.Actions;
import game.dragonhero.service.user.Bonus;
import game.dragonhero.table.BaseRoom;
import game.dragonhero.table.CampaignRoom;
import game.dragonhero.table.DefaultRoom;
import game.dragonhero.task.dbcache.MailCreatorCache;
import game.monitor.Online;
import game.monitor.TopMonitor;
import game.object.DataQuest;
import game.object.MyUser;
import game.object.TaskMonitor;
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
        List<Integer> actions = Arrays.asList(CREATE_NAME, USER_INFO, DAME_SKIN_EQUIP, CHANGE_LANG, CHAT_FRAME_EQUIP, USE_GIFT_CODE, TRIAL_EQUIP, BUFF_INFO, RANKING_STATUS, TUTORIAL_STATUS, TUTORIAL_QUEST_RECEIVE, TUTORIAL_GO_TO, TUTORIAL_QUEST_STATUS, RANKING_INFO, SEND_MAIL, CHANGE_INTRO, HELP_VALUE, CHANGE_NAME, PIECE_GRAFT, AVATAR_LIST, AVATAR_CHOOSE, USER_DATA_INFO, BAG_STATUS, BAG_BUY_SLOT, UPDATE_NEXT_DAY, AFK_STATUS, AFK_GET_BONUS);
        actions.forEach(action -> mHandler.put(action, this));
    }

    static UserHandler instance;
    private static final int AVATAR_HERO = 0;
    private static final int AVATAR_SPECIAL = 1;

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
                case CHANGE_INTRO -> changeIntro();
                case USER_DATA_INFO -> userDataInfo();
                case BAG_STATUS -> bagStatus();
                case BAG_BUY_SLOT -> bagBuySlot();
                case AVATAR_LIST -> avatarList();
                case AVATAR_CHOOSE -> avatarChoose();
                case CHANGE_NAME -> changeName();
                case HELP_VALUE -> helpValue();
                case RANKING_INFO -> rankInfo();
                case RANKING_STATUS -> rank();
                case SEND_MAIL -> sendMail();
                case USER_INFO -> userInfo();
                case UPDATE_NEXT_DAY -> updateNextDay();
                case AFK_STATUS -> afkStatus();
                case AFK_GET_BONUS -> afkGetBonus();
                case PIECE_GRAFT -> pieceGraft();
                case TUTORIAL_STATUS -> tutorial();
                case TUTORIAL_QUEST_STATUS -> tutorialQuestStatus(mUser, this);
                case TUTORIAL_QUEST_RECEIVE -> tutorialQuestReceive();
                case TUTORIAL_GO_TO -> tutorialGoTo();
                case DAME_SKIN_EQUIP -> dameSkinEquip();
                case CHAT_FRAME_EQUIP -> chatFrameEquip();
                case TRIAL_EQUIP -> trialEquip();
                case USE_GIFT_CODE -> useGiftCode();
                case BUFF_INFO -> buffInfo(mUser);
                case CHANGE_LANG -> changeLang(getInputString());
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    private void changeLang(String inputString) {
        String lang =inputString.toLowerCase() ;
        System.out.println("lang ========== " + lang);
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
        int characterId = (int) cmm.getALong(0);
        if (!CfgUser.config.heroStart.contains(characterId)) {
            addErrParam();
            return;
        }
        //them vao nhan vat, free con dau
        UserHeroEntity uHero = new UserHeroEntity(user.getId(), characterId);
        if (!DBJPA.saveOrUpdate(uHero)) {
            addErrResponse();
            return;
        }
        mUser.getResources().addHero(uHero);
        // tang them cho 5 cai phi tieu
        UserWeaponEntity w1 = new UserWeaponEntity(user.getId(), 1);
        DBJPA.saveOrUpdate(w1);
        mUser.getResources().addWeapon(w1);
        w1 = new UserWeaponEntity(user.getId(), 2);
        DBJPA.saveOrUpdate(w1);
        mUser.getResources().addWeapon(w1);
        w1 = new UserWeaponEntity(user.getId(), 3);
        DBJPA.saveOrUpdate(w1);
        mUser.getResources().addWeapon(w1);
        w1 = new UserWeaponEntity(user.getId(), 4);
        DBJPA.saveOrUpdate(w1);
        mUser.getResources().addWeapon(w1);
        w1 = new UserWeaponEntity(user.getId(), 5);
        DBJPA.saveOrUpdate(w1);
        mUser.getResources().addWeapon(w1);

        if (mUser.getUser().updateCreateUser(userName, characterId)) {
            Pbmethod.PbLoginGame.Builder builder = Pbmethod.PbLoginGame.newBuilder();
            builder.setUser(user.toProto(mUser));
            //  user point
            addResponse(builder.build());
        } else addErrResponse();
    }

    void changeIntro() {
        protocol.Pbmethod.CommonVector cmm = CommonProto.parseCommonVector(requestData);
        String newIntro = cmm.getAString(0);
        if (newIntro.isEmpty()) {
            addErrResponse(getLang(Lang.err_intro));
            return;
        }
        if (newIntro.length() > 45) {
            addErrResponse(getLang(Lang.err_max_character));
            return;
        }
        if (user.update(Arrays.asList("intro", newIntro))) {
            user.setIntro(newIntro);
            addResponse(cmm);
        } else addErrResponse();
    }

    void userDataInfo() {
        addResponse(IAction.USER_DATA_INFO, mUser.getUData().toProto(mUser));
    }

    void pointData() {
        mUser.reCalculatePoint();
    }

    void bagStatus() {
        List<Long> status = new ArrayList<>();
        UserDataEntity uData = mUser.getUData();
        status.addAll(NumberUtil.converListIntToLong(uData.getSlot())); //3
        status.addAll(GsonUtil.toListLong(user.getAllInfoItemEquip()));
        addResponse(getCommonVector(status));
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
        }  else if (Lang.instance(mUser).getLocale().equalsIgnoreCase(LOCALE_RU)) {
            addResponse(CommonProto.getCommonVectorProto(null, Arrays.asList(help.getK(), help.getRu())));
        } else if (Lang.instance(mUser).getLocale().equalsIgnoreCase(LOCALE_KM)) {
            addResponse(CommonProto.getCommonVectorProto(null, Arrays.asList(help.getK(), help.getKm())));
        }else if (Lang.instance(mUser).getLocale().equalsIgnoreCase(LOCALE_ZH)) {
            addResponse(CommonProto.getCommonVectorProto(null, Arrays.asList(help.getK(), help.getZh())));
        }else if (Lang.instance(mUser).getLocale().equalsIgnoreCase(LOCALE_JP)) {
            addResponse(CommonProto.getCommonVectorProto(null, Arrays.asList(help.getK(), help.getJp())));
        } else {
            addResponse(CommonProto.getCommonVectorProto(null, Arrays.asList(k, "")));
        }
    }


    void bagBuySlot() {
        int type = (int) CommonProto.parseCommonVector(requestData).getALong(0);
        int num = (int) CommonProto.parseCommonVector(requestData).getALong(1);
        int curSlot = 0, maxSlot = 0;
        if (type == 1) {
            curSlot = mUser.getUData().getNumSlotItem();
            maxSlot = CfgBag.maxSlotItem();
        } else if (type == 2) {
            curSlot = mUser.getUData().getNumSlotItemEquip();
            maxSlot = CfgBag.maxSlotEquipment();
        } else if (type == 3) {
            curSlot = mUser.getUData().getNumSlotPiece();
            maxSlot = CfgBag.maxSlotPiece();
        }
        if (curSlot + num > maxSlot) {
            addErrResponse(getLang(Lang.err_max_number));
            return;
        }
        List<Long> price = new ArrayList<>();
        price.addAll(Bonus.viewGem(-CfgBag.getPriceSot(curSlot, num, type)));
        String error = Bonus.checkMoney(mUser, price);
        if (StringHelper.isEmpty(error)) {
            List<Long> retBonus = Bonus.receiveListItem(mUser, DetailActionType.BUY_SLOT_BAG.getKey(type), price);
            if (retBonus.isEmpty()) {
                addErrResponse();
                return;
            }
            if (!mUser.getUData().updateSlot(type, curSlot + num)) {
                retBonus = Bonus.receiveListItem(mUser, DetailActionType.BUY_SLOT_BAG_FAIL.getKey(), Bonus.viewGem(CfgBag.getPriceSot(curSlot, num, type)));
            }
            Pbmethod.CommonVector.Builder cmm = Pbmethod.CommonVector.newBuilder();
            cmm.addALong(type);
            cmm.addALong(curSlot + num);
            cmm.addAllALong(retBonus);
            addResponse(cmm.build());
        } else addErrResponse(error);
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
        UserEntity user = Online.getDbUser(userId);
        if (user == null) {
            addErrResponse(getLang(user_not_found));
            return;
        }
        addResponse(user.toProto());
    }

    void updateNextDay() {
        Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
        pb.addAVector(getCommonVector(CfgServer.getSeason()));
        List<UserItemEntity> uItem = mUser.getResources().getMItem().values().stream().filter(item -> item.expired()).collect(Collectors.toList());
        List<Long> bonusExpire = new ArrayList<>();
        for (int i = 0; i < uItem.size(); i++) {
            bonusExpire.addAll(Bonus.viewItem(uItem.get(i).getItemId(), -uItem.get(i).getNumber()));
        }
        pb.addAVector(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.UPDATE_BONUS_NEXT_DAY.getKey(), bonusExpire)));
        addResponse(pb.build());
    }

    void afkStatus() {
        try {
            UserAfkEntity uAfk = Services.userDAO.getUserAfk(mUser);
            Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
            Pbmethod.CommonVector.Builder cmm = Pbmethod.CommonVector.newBuilder();
            cmm.addALong(uAfk.getTimeGetBonus());
            cmm.addALong(uAfk.getTimeFullBonus());
            long timeCurCheck = Calendar.getInstance().getTimeInMillis();
            long secondsMax = uAfk.getTimeFullBonus() * DateTime.HOUR_MILLI_SECOND;
            long timeRemain = secondsMax - (timeCurCheck - uAfk.getTimeGetBonus()) / 1000;
            long timeOffset = timeCurCheck - uAfk.getTimeCheckBonus();
            timeOffset = timeOffset > timeRemain ? timeRemain / 1000 : timeOffset / 1000;
            List<Long> bonus = uAfk.getBonus();
            // bonus moi 5s
            int number = (int) (timeOffset / CfgAfk.config.secondUpdate);
            for (int i = 0; i < number; i++) {
                bonus.addAll(CfgAfk.getBonusAfk());
            }
            // ti re roi quai
            int numRatePice = number / 10;
            int numPieceReceive = 0;
            for (int i = 0; i < numRatePice; i++) {
                if (NumberUtil.rand100((int) (CfgAfk.config.ratePiece * 10))) numPieceReceive++;
            }
            // check id toi da nhan dc roi random trong khoang do
            int curMap = mUser.getUData().getCampaign().get(0);
            curMap = Math.min(curMap, ResMap.maxMapCampaign);
            int maxId = ResMap.getMapCampaign(curMap).getListEnemyIds().get(0);
            List<Long> bonusPiece = new ArrayList<>();
            for (int i = 0; i < numPieceReceive; i++) {
                bonusPiece.addAll(Bonus.viewPiece(PieceType.MONSTER, NumberUtil.getRandom(maxId) + 1, 1L));
            }
            if (!bonusPiece.isEmpty()) {
                bonus.addAll(bonusPiece);
            }
            long addExp = (long) (CfgUser.getExpByLevel(user.getLevel()) * CfgAfk.perExpAFK * number);
            if (addExp > 0) bonus.addAll(Bonus.viewExp(addExp));
            bonus = Bonus.merge(bonus);
            if (number > 0 && uAfk.update(Arrays.asList("time_check_bonus", timeCurCheck, "bonus", StringHelper.toDBString(bonus)))) { // có thay đổi, cần check set lại db
                uAfk.setTimeCheckBonus(timeCurCheck);
                uAfk.setBonus(bonus.toString());
            }
            cmm.addAllALong(bonus);
            pb.addAVector(cmm);
            pb.addAVector(getCommonVector(ResEvent.getResPack(PackType.AFK_ADD_TIME).getPrice()));
            addResponse(IAction.AFK_STATUS, cmm.build());
        } catch (Exception e) {
            addErrResponse();
        }
    }

    void afkGetBonus() {
        UserAfkEntity uAfk = Services.userDAO.getUserAfk(mUser);
        List<Long> bonus = Bonus.xPerBonus(uAfk.getBonus(), uAfk.getPerBonus());
        if (bonus.isEmpty()) {
            addErrResponse(getLang(err_no_bonus));
            uAfk.resetBonus();
            afkStatus();
            return;
        }
        if (uAfk.resetBonus()) {
            //bo bonus manh quai vat
            List<List<Long>> aBonus = Bonus.parse(bonus);
            List<Long> bonusNoReview = new ArrayList<>();
            Iterator<List<Long>> it = aBonus.iterator();
            while (it.hasNext()) {
                List<Long> item = it.next();
                if (item == null || item.isEmpty()) continue; // an toàn
                // nếu Bonus.BONUS_PIECE là primitive long:
                if (item.get(0) != null && item.get(0) == Bonus.BONUS_PIECE) {
                    bonusNoReview.addAll(item);
                    it.remove(); // an toàn khi lặp bằng iterator
                }
            }

            List<Long> flatList = aBonus.stream().flatMap(List::stream).toList();
            addBonusToast(Bonus.receiveListItem(mUser, DetailActionType.GET_BONUS_AFK.getKey(), flatList));
            addBonusPrivate(Bonus.receiveListItem(mUser, DetailActionType.GET_BONUS_AFK.getKey(), bonusNoReview));
            afkStatus();
            CfgQuest.addNumQuest(mUser, DataQuest.GET_BONUS_AFK, 1);
        } else addErrSystem();

        CfgEvent.processTriggerEventTimer(mUser, mUser.getUser().getLevel(), TriggerEventTimer.TIME);
    }

    private void pieceGraft() {
        List<Long> inputs = getInputALong();
        int type = inputs.get(0).intValue();
        int id = inputs.get(1).intValue();
        int number = 1; // dùng cho các loại mảnh khác
        if (type == PieceType.MONSTER.value) {
            int petType = type - 1;
            ResEnemyEntity enemy = ResEnemy.getEnemy(id);
            if (enemy == null || enemy.getDataPet().isEmpty()) {
                addErrParam();
                return;
            }

            if (mUser.getResources().hasPet(petType, id)) {
                addErrResponse(getLang(err_has_monster));
                return;
            }
            List<Long> aBonus = Bonus.viewPiece(type, id, -CfgPet.PIECE_TO_ITEM);
            String err = Bonus.checkMoney(mUser, aBonus);
            if (err != null) {
                addErrResponse(err);
                return;
            }
            aBonus.addAll(Bonus.viewPet(petType, id));

            addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.PIECE_GRAFT.getKey(type), aBonus)));
            // check event 7 day
            UserEventSevenDayEntity uEvent = Services.userDAO.getUserSevenDay(mUser);
            if (uEvent.hasEvent() && uEvent.hasActive(6) && uEvent.update(List.of("monster", uEvent.getMonster() + 1))) {
                uEvent.setMonster(uEvent.getMonster() + 1);
            }
            Actions.save(user, "piece", "graft", "type", type, "id", id, "number", number);
        } else addErrResponse();
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

    private void tutorialGoTo() {
        List<Long> inputs = getInputALong();
        RoomType roomType = RoomType.get(inputs.get(0).intValue());
        int mapId = inputs.get(1).intValue();
        BaseMap map = ResMap.getMap(roomType, mapId);
        Pos posInit = new Pos(inputs.get(2) / 1000, inputs.get(3) / 1000);
        if (map == null) {
            addErrParam();
            return;
        }
        if (mUser == null) {
            addResponse(LOGIN_REQUIRE, null);
            return;
        }
        int chanelId = roomType == RoomType.CAMPAIGN && mapId == 1 ? user.getId() : mUser.getRoomChanelId();
        String keyRoom = CfgBattle.getKeyRoom(mUser, roomType.value, mapId, chanelId);
        BaseRoom curRoom = (BaseRoom) ChUtil.get(channel, ChUtil.KEY_ROOM);
        if (curRoom != null && (curRoom.getKeyRoom().equals(keyRoom) || !curRoom.allowChangeChanel())) {
            if (curRoom.getKeyRoom().equals(keyRoom)) {
                addErrResponse(getLang(Lang.err_in_room_already));
                return;
            }
            if (!curRoom.allowChangeChanel()) {
                addErrResponse(getLang(Lang.err_unauthorized));
                return;
            }
        }
        // xóa khỏi room cũ
        Player player = mUser.getPlayer();
        if (curRoom != null && curRoom.hasPlayer(player.getId())) {
            curRoom.removePlayer(player.getId());
        }
        // check có room hay chưa, có rồi thì join
        BaseRoom room = (BaseRoom) TaskMonitor.getInstance().getRoom(keyRoom);
        player.clearDataForChangeRoom(posInit);
        if (room == null) {  // tao room moi
            List<Character> players = new ArrayList<>();
            players.add(player);
            switch (roomType) {
                case CAMPAIGN:
                    room = mapId > 0 ? new CampaignRoom(map, players, keyRoom) : new DefaultRoom(map, players, keyRoom);
                    break;
                case FARM:
                    // UI Room nên không create room
                    break;
                default:
                    room = new DefaultRoom(map, players, keyRoom);
                    break;
            }
            TaskMonitor.getInstance().addRoom(room);
        } else { // join vào room có sẵn
            if (room.getAPlayer().size() > roomType.maxPlayer) {
                addErrResponse(Lang.instance(mUser).get(Lang.err_full_player));
                return;
            }
            room.addPlayer(player);
        }
        ChUtil.set(channel, ChUtil.KEY_ROOM, room);
        // tra ve id teleport next
        addResponse(INIT_MAP, CfgBattle.genInitMap(roomType.value, mapId, mUser.getRoomChanelId(), map.getMapData().getPlayerCollider(), mapId > 0, PopupType.NULL));
    }

    private void dameSkinEquip() {
        int skinId = getInputInt();
        if (skinId != 0 && !mUser.getUData().getListDameSkin().contains(skinId)) {
            addErrResponse(getLang(Lang.err_no_has_dame_skin));
            return;
        }
        if (mUser.getUData().update(List.of("dame_skin_equip", skinId))) {
            mUser.getUData().setDameSkinEquip(skinId);
            mUser.getPlayer().protoStatus(StateType.UPDATE_TEXT_DAME, (long) skinId);
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
            mUser.getPlayer().protoStatus(StateType.UPDATE_CHAT_FRAME, (long) frameId);
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
            mUser.getPlayer().protoStatus(StateType.UPDATE_TRIAL, (long) trialId);
            addResponse(getCommonVector(trialId));
        } else addErrSystem();
    }

    public static void buffInfo(MyUser mUser) {
        List<Long> buffs = mUser.getUData().getBuff();
        List<Long> ret = new ArrayList<>();
        List<Long> sumBuff = NumberUtil.genListLong(4, 0L);
        // buff from item
        for (int i = 0; i < buffs.size(); i++) {
            if (buffs.get(i) <= 0) continue;
            // còn hạn
            long timeRemain = (buffs.get(i) - System.currentTimeMillis()) / 1000;
            if (timeRemain > 0) {
                BuffItemType buff = BuffItemType.getByIndex(i);
                ret.add((long) buff.id);
                ret.add(timeRemain);
                sumBuff.set(buff.pointIndex, sumBuff.get(buff.pointIndex) + buff.valueBuff);
                sumBuff.set(3, 1L); // active anim buff
            }
        }
        // buff từ phúc lợi bang và trang bị
        Point point = mUser.getPlayer().getPoint();
        sumBuff.set(0, sumBuff.get(0) + point.getBuffDrop());
        sumBuff.set(1, sumBuff.get(1) + point.getBuffGold());
        sumBuff.set(2, sumBuff.get(2) + point.getBuffExp());
        //
        sumBuff.addAll(ret);
        Util.sendProtoData(mUser.getChannel(), CommonProto.getCommonVector(sumBuff), IAction.BUFF_INFO);
        mUser.getPlayer().updateBuff();
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

    void avatarList() {
        List<ResAvatarEntity> avatars = ResAvatar.aAvatarHero;
        List<UserAvatarEntity> uAvatar = DBJPA.getList("user_avatar", Arrays.asList("user_id", user.getId()), "", UserAvatarEntity.class);
        List<UserAvatarEntity> heroAvatar = uAvatar.stream().filter(avatar -> avatar.getTypeId() == AVATAR_HERO).collect(Collectors.toList());
        // Lấy id những avatar đang sở hữu.
        List<Integer> uAvatarHero = new ArrayList<>();
        for (int i = 0; i < heroAvatar.size(); i++) {
            uAvatarHero.add(heroAvatar.get(i).getAvatarId());
        }
        List<Integer> dbAvatarIds = mUser.getResources().getHeroes().stream().map(hero -> hero.getHeroId()).distinct().collect(Collectors.toList());
        dbAvatarIds.removeAll(uAvatarHero);
        for (int i = 0; i < dbAvatarIds.size(); i++) {
            if (!avatars.contains(dbAvatarIds.get(i))) { // có trong db nhưng chưa có trong túi, và đã sở hữu tướng.
                UserAvatarEntity tmp = new UserAvatarEntity(user.getId(), dbAvatarIds.get(i), AVATAR_HERO);
                if (DBJPA.saveOrUpdate(tmp)) {
                    uAvatar.add(tmp);
                }
            }
        }
        Pbmethod.ListCommonVector.Builder builder = Pbmethod.ListCommonVector.newBuilder();
        { // hero
            Pbmethod.CommonVector.Builder cmm = Pbmethod.CommonVector.newBuilder();
            uAvatar.stream().filter(avatar -> avatar.getTypeId() == AVATAR_HERO).forEach(avatar -> cmm.addALong(avatar.getAvatarId()));
            builder.addAVector(cmm);
        }
        { // special
            Pbmethod.CommonVector.Builder cmm = Pbmethod.CommonVector.newBuilder();
            uAvatar.stream().filter(avatar -> avatar.getTypeId() == AVATAR_SPECIAL).forEach(avatar -> cmm.addALong(avatar.getAvatarId()));
            builder.addAVector(cmm);
        }
        addResponse(builder.build());
    }

    void avatarChoose() {
        int avatarType = (int) CommonProto.parseCommonVector(requestData).getALong(0);
        int avatarId = (int) CommonProto.parseCommonVector(requestData).getALong(1);
        List<UserAvatarEntity> uAvatar = DBJPA.getList("user_avatar", Arrays.asList("user_id", user.getId()), "", UserAvatarEntity.class);
        boolean hasAvatar = false;
        for (UserAvatarEntity avatar : uAvatar) {
            if (avatar.getAvatarId() == avatarId && avatar.getTypeId() == avatarType) {
                hasAvatar = true;
                break;
            }
        }
        if (!hasAvatar) addErrResponse(getLang(Lang.err_params));
        List<Integer> avatars = user.getAvatar();
        avatars.set(0, avatarType);
        avatars.set(1, avatarId);
        if (DBJPA.update("user", Arrays.asList("avatar", StringHelper.toDBString(avatars)), Arrays.asList("id", String.valueOf(user.getId())))) {
            user.setAvatar(avatars.toString());
            addResponse(getCommonVector(avatarType, avatarId));
        } else addErrResponse();
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
