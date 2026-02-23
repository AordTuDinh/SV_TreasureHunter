package game.dragonhero.controller;

import game.config.*;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.dragonhero.mapping.*;
import game.dragonhero.mapping.main.*;
import game.dragonhero.server.IAction;
import game.dragonhero.service.Services;
import game.dragonhero.service.resource.ResEvent;
import game.dragonhero.service.resource.ResFarm;
import game.dragonhero.service.user.Actions;
import game.dragonhero.service.user.Bonus;
import game.dragonhero.task.dbcache.MailCreatorCache;
import game.object.DataQuest;
import game.protocol.CommonProto;
import io.netty.channel.Channel;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.StringHelper;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.*;
import java.util.stream.Collectors;

import static game.config.CfgFarm.ID_TREE;

public class FarmHandler extends AHandler {
    final String KEY_DATA = "user_farm_quest";
    private static FarmHandler instance;
    private static final int TYPE_TREE = 1;
    private static final int TYPE_LAND = 2;
    private static final int TYPE_DECO = 2;
    //
    public static final int QUEST_UNLOCK = 0;
    public static final int QUEST_LOCK = 1;
    private Map<Integer, UserFarmQuestEntity> mFarmQuest = new HashMap<>();
    private List<UserFarmQuestEntity> aUserFarmQuest;

    public static FarmHandler getInstance() {
        if (instance == null) instance = new FarmHandler();
        return instance;
    }

    @Override
    public AHandler newInstance() {
        return new FarmHandler();
    }

    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(FARM_STATUS, FARM_CARE, FARM_BUY_LAND, FARM_SELL_ITEM, FARM_SELL_SINGLE, FARM_CREATE_FOOD, FARM_QUICK_CARE, FARM_BUY_ITEM, FARM_PING, FARM_HARVEST_TREE, FARM_QUEST_STATUS, FARM_QUEST_USE_ITEM, FARM_QUEST_REFRESH, FARM_QUEST_SPEED_UP, FARM_QUEST_LOCK_UNLOCK, FARM_QUEST_START, FARM_QUEST_RECEIVE, FARM_QUEST_CANCEL);
        actions.forEach(action -> mHandler.put(action, this));
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        try {
            switch (actionId) {
                case FARM_STATUS -> status();
                case FARM_BUY_LAND -> buyLand();
                case FARM_CARE -> care();
                case FARM_SELL_ITEM -> sellFarm();
                case FARM_SELL_SINGLE -> sellSingle();
                case FARM_CREATE_FOOD -> createFood();
                case FARM_QUICK_CARE -> quickCare(getInputALong(), false);
                case FARM_BUY_ITEM -> buyItem();
                case FARM_HARVEST_TREE -> harvestTree();
                case FARM_PING -> addResponseSuccess();
                default -> {
                    aUserFarmQuest = getUserFarmQuest();
                    if (aUserFarmQuest != null) {
                        for (int i = 0; i < aUserFarmQuest.size(); i++) {
                            mFarmQuest.put(aUserFarmQuest.get(i).getId(), aUserFarmQuest.get(i));
                        }
                        switch (actionId) {
                            case FARM_QUEST_STATUS -> questStatus();
                            case FARM_QUEST_USE_ITEM -> questUseItem();
                            case FARM_QUEST_REFRESH -> questRefresh();
                            case FARM_QUEST_SPEED_UP -> questSpeedUp();
                            case FARM_QUEST_LOCK_UNLOCK -> questLock();
                            case FARM_QUEST_START -> questStart();
                            case FARM_QUEST_RECEIVE -> questReceive();
                            case FARM_QUEST_CANCEL -> questCancel();
                        }
                    } else addErrSystem();
                }
                // farm quest

            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    private List<UserFarmQuestEntity> getUserFarmQuest() {
        List<UserFarmQuestEntity> obQuest = (List<UserFarmQuestEntity>) mUser.getCache().get(KEY_DATA);
        if (obQuest == null) {
            obQuest = DBJPA.getList("user_farm_quest", List.of("user_id", user.getId()), "", UserFarmQuestEntity.class);
            if (obQuest == null) {
                addErrResponse();
                return null;
            }
            mUser.getCache().set(KEY_DATA, obQuest);
        }

        if (obQuest != null && checkRefresh()) {
            removeUnlockQuest(obQuest);
            int numQuestLock = (int) obQuest.stream().filter(tavern -> tavern.getQuestLock() == QUEST_LOCK && tavern.getStatus() == StatusType.LOCK.value).count();
            int numQuestNew = ResEvent.getResVip(user.getVip()).getFarmQuest(user.getLevel()) - numQuestLock;
            numQuestNew = Math.min(CfgFarmQuest.MAX_QUEST - obQuest.size(), numQuestNew);
            boolean isFirstQuest = mUser.getUData().getUInt().getValue(UserInt.FIRST_FARM_QUEST) == 0;

            for (int i = 0; i < numQuestNew; i++) { // add new quest
                int idFarmQuest = isFirstQuest ? 0 : CfgFarmQuest.getRandomIdFarmQuest(mUser);
                UserFarmQuestEntity tavernEntity = addQuest("refresh", idFarmQuest, isFirstQuest);
                if (isFirstQuest) {
                    isFirstQuest = false;
                    mUser.getUData().getUInt().setValueAndUpdate(UserInt.FIRST_FARM_QUEST, 1);
                }
                if (tavernEntity != null) {
                    obQuest.add(tavernEntity);
                }
            }
        }
        return obQuest;
    }

    private void questStatus() {
        Pbmethod.ListCommonVector.Builder builder = Pbmethod.ListCommonVector.newBuilder();
        // check tavern complete
        checkCompleteFarmQuest();
        receiveOverCompleteTavern();
        List<UserFarmQuestEntity> aFarmQuest = mFarmQuest.values().stream().sorted(Comparator.comparing(UserFarmQuestEntity::getStatus).thenComparing(UserFarmQuestEntity::getStar).reversed().thenComparing(UserFarmQuestEntity::getId).reversed()).collect(Collectors.toList());
        for (int i = 0; i < aFarmQuest.size(); i++) {
            UserFarmQuestEntity farmQuestEntity = aFarmQuest.get(i);
            builder.addAVector(Pbmethod.CommonVector.newBuilder().addALong(farmQuestEntity.getId()).addALong(farmQuestEntity.getStar()).addALong(farmQuestEntity.getStatus()).addALong(farmQuestEntity.getQuestLock()).addALong(CfgFarmQuest.config.timeComplete[farmQuestEntity.getStar() - 1]).addALong(farmQuestEntity.getTavernCoundown()).addALong(CfgFarmQuest.config.feeSpeedup[farmQuestEntity.getStar() - 1]).addAllALong(farmQuestEntity.getTavernBonus()).addAString(getLang(Lang.farm_quest_order, farmQuestEntity.getId())));
            //require
            builder.addAVector(Pbmethod.CommonVector.newBuilder().addAllALong(Bonus.reverseBonus(farmQuestEntity.getFee())));
        }

        addResponse(FARM_QUEST_STATUS, builder.build());
    }

    public boolean checkRefresh() {
        boolean hasRefresh = false;
        UserInt uInt = mUser.getUData().getUInt();
        int lastTimeRefresh = uInt.getValue(UserInt.REFRESH_TAVERN);
        int timeRefresh = Calendar.getInstance().get(Calendar.YEAR) + Calendar.getInstance().get(Calendar.MONTH) + Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        if (lastTimeRefresh == 0 || (lastTimeRefresh < timeRefresh || lastTimeRefresh > timeRefresh)) {
            uInt.setValue(UserInt.REFRESH_TAVERN, timeRefresh);
            if (!uInt.update()) {
                uInt.setValue(UserInt.REFRESH_TAVERN, lastTimeRefresh);
                addErrResponse();
                return false;
            }
            hasRefresh = true;
        }
        return hasRefresh;
    }

    private void questUseItem() {
        if (aUserFarmQuest.size() >= CfgFarmQuest.MAX_QUEST) {
            addErrResponse(String.format(getLang(Lang.err_max_quest_farm), CfgFarmQuest.MAX_QUEST));
            return;
        }
        int type = getInputInt();
        if (type != 0 && type != 1) {
            addErrParam();
            return;
        }
        ItemKey itemKey = type == 0 ? ItemKey.FARM_QUEST_SCROLL : ItemKey.FARM_QUEST_SCROLL_VIP;
        List<Long> fee = Bonus.viewItem(itemKey, -1);
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        List<Long> aBonus = Bonus.receiveListItem(mUser, DetailActionType.USE_SCROLL_FARM_QUEST.getKey(itemKey.id), fee);
        if (aBonus.isEmpty()) {
            addErrResponse();
            return;
        }
        int idTavernConfig = type == 0 ? CfgFarmQuest.getRandomIdTavernNormal(mUser) : CfgFarmQuest.getRandomIdTavernSenior(mUser);
        UserFarmQuestEntity objTavern = addQuest(type == 0 ? "normal" : "vip", idTavernConfig, false);
        if (objTavern != null) {
            addFarmQuest(objTavern);
            addResponse(CommonProto.getCommonVectorProto(aBonus));
            questStatus();
        } else {
            addErrSystem();
            Bonus.receiveListItem(mUser, DetailActionType.USE_SCROLL_FARM_QUEST.getKey(itemKey.id), Bonus.reverseBonus(fee));
        }
    }

    private void questRefresh() {
        int totalGemRefresh = countQuestUnlock() * CfgFarmQuest.config.feeRefresh;
        if (user.getGem() < totalGemRefresh) {
            addErrResponse(getLang(Lang.err_not_enough_gem));
        } else if (totalGemRefresh == 0) {
            addErrResponse(getLang(Lang.no_need_refresh));
        } else {
            // refresh
            List<Long> aBonus = Bonus.receiveListItem(mUser, DetailActionType.Farm_QUEST_RESET.getKey(), Bonus.viewGem(-totalGemRefresh));
            if (aBonus.isEmpty()) {
                addErrResponse();
                return;
            }
            int numbInstance = (int) aUserFarmQuest.stream().filter(tavern -> tavern.getQuestLock() == QUEST_UNLOCK).count();
            if (removeUnlockQuest(aUserFarmQuest)) {
                for (int i = 0; i < numbInstance; i++) { // add new quest
                    int idTavernConfig = CfgFarmQuest.getRandomIdFarmQuest(mUser);
                    UserFarmQuestEntity objTavern = addQuest("refresh", idTavernConfig, false);
                    if (objTavern != null) {
                        addFarmQuest(objTavern);
                    }
                }
                addResponse(CommonProto.getCommonVectorProto(aBonus));
                questStatus();
            } else {
                Bonus.receiveListItem(mUser, DetailActionType.Farm_QUEST_RESET.getKey(), Bonus.viewGem(totalGemRefresh));
                addErrResponse();
            }
        }
    }

    private boolean removeUnlockQuest(List<UserFarmQuestEntity> obTavern) {
        String ids = obTavern.stream().filter(tavern -> tavern.getQuestLock() == QUEST_UNLOCK).map(tavern -> String.valueOf(tavern.getId())).collect(Collectors.joining(","));
        if (StringHelper.isEmpty(ids)) return true; // no unlock tavern
        if (DBJPA.rawSQL("delete from user_farm_quest where id in (" + ids + ")")) {
            for (int i = obTavern.size() - 1; i >= 0; i--) {
                if (obTavern.get(i).getQuestLock() == QUEST_UNLOCK) {
                    mFarmQuest.remove(obTavern.get(i).getId());
                    obTavern.remove(i);
                }
            }
            return true;
        }
        return false;
    }

    public int countQuestUnlock() {
        return (int) mFarmQuest.entrySet().stream().filter(quest -> quest.getValue().getQuestLock() == 0).count();
    }

    private void questSpeedUp() {
        int idQuest = getInputInt();
        if (!mFarmQuest.containsKey(idQuest)) {
            addErrResponse(getLang(Lang.farm_quest_not_exist_quest));
            return;
        }
        UserFarmQuestEntity quest = mFarmQuest.get(idQuest);
        int feeSpeedup = CfgFarmQuest.config.feeSpeedup[quest.getStar() - 1];
        List<Long> fee = Bonus.viewGem(-feeSpeedup);
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        List<Long> bonus = Bonus.receiveListItem(mUser, DetailActionType.FARM_QUEST_SPEED_UP.getKey(idQuest), fee);
        if (bonus.isEmpty()) {
            addErrResponse();
            return;
        }
        if (quest.update(List.of("status", StatusType.DONE.value))) {
            quest.setStatus(StatusType.DONE.value);
            Pbmethod.CommonVector.Builder pb = Pbmethod.CommonVector.newBuilder();
            pb.addALong(quest.getId()).addALong(quest.getStatus()).addAllALong(bonus);
            addResponse(FARM_QUEST_SPEED_UP, pb.build());
        } else {
            addErrSystem();
            Bonus.receiveListItem(mUser, DetailActionType.FARM_QUEST_SPEED_UP.getKey(idQuest), Bonus.reverseBonus(fee));
        }
    }

    private void questLock() {
        int idQuest = (int) CommonProto.parseCommonVector(requestData).getALong(0);
        if (!mFarmQuest.containsKey(idQuest)) {
            addErrResponse(getLang(Lang.farm_quest_not_exist_quest));
            return;
        }
        UserFarmQuestEntity uFarmQuest = mFarmQuest.get(idQuest);
        if (uFarmQuest.getStatus() == StatusType.PROCESSING.value) {
            addErrResponse(getLang(Lang.err_farm_quest_not_lock_quest));
            return;
        }

        if (uFarmQuest.updateTavernLock()) {
            addResponse(getCommonVector(idQuest, uFarmQuest.getQuestLock()));
        } else addErrSystem();
    }

    private void questStart() {
        int questId = getInputInt();
        if (!mFarmQuest.containsKey(questId)) {
            addErrResponse(getLang(Lang.farm_quest_not_exist_quest));
            return;
        }
        UserFarmQuestEntity tavern = mFarmQuest.get(questId);
        List<Long> fee = tavern.getFee();
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        List<Long> bonus = Bonus.receiveListItem(mUser, DetailActionType.START_FARM_QUEST.getKey(questId), fee);
        if (bonus.isEmpty()) {
            addErrSystem();
            return;
        }
        if (tavern.update(List.of("fee", StringHelper.toDBString(fee), "status", StatusType.PROCESSING.value, "quest_lock", QUEST_LOCK, "time_start", System.currentTimeMillis()))) {
            tavern.setFee(fee.toString());
            tavern.setStatus(StatusType.PROCESSING.value);
            tavern.setQuestLock(QUEST_LOCK);
            tavern.setTimeStart(System.currentTimeMillis());
            bonus.add(0, (long) questId);
            addResponse(getCommonVector(bonus));
            mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.SHIP, 1);
            // check event 7 day
            UserEventSevenDayEntity uEvent = Services.userDAO.getUserSevenDay(mUser);
            if (uEvent.hasEvent() && uEvent.hasActive(5) && uEvent.update(List.of("ship", uEvent.getShip() + 1))) {
                uEvent.setShip(uEvent.getShip() + 1);
            }
            // check quest B
            CfgQuest.addNumQuestB(mUser, CfgQuest.INDEX_SHIP, 1);
        } else {
            Bonus.receiveListItem(mUser, DetailActionType.START_FARM_QUEST.getKey(questId), Bonus.reverseBonus(fee));
            addErrSystem();
        }
    }

    private void questReceive() {
        int idQuest = getInputInt();
        if (!mFarmQuest.containsKey(idQuest)) {
            addErrResponse(getLang(Lang.farm_quest_not_exist_quest));
            return;
        }

        UserFarmQuestEntity tavern = mFarmQuest.get(idQuest);
        if (tavern.getStatus() != StatusType.DONE.value) {
            addErrResponse(getLang(Lang.quest_unfinished));
            return;
        }

        if (!removeQuest(idQuest)) {
            addErrResponse();
            return;
        }
        List<Long> bonus = tavern.getTavernBonus();
        List<Long> lstBonus = Bonus.receiveListItem(mUser, DetailActionType.FARM_QUEST_RECEIVE_QUEST.getKey(idQuest), bonus);
        if (lstBonus.isEmpty()) {
            addErrResponse();
            return;
        }
        lstBonus.add(0, (long) idQuest);
        addResponse(getCommonVector(lstBonus));
    }

    boolean removeQuest(int idQuest) {
        if (mFarmQuest.containsKey(idQuest)) {
            UserFarmQuestEntity uTavern = mFarmQuest.get(idQuest);
            if (uTavern.deleteTavern()) {
                mFarmQuest.remove(idQuest);
                aUserFarmQuest.remove(uTavern);
                Actions.save(user, "farmQuest", "remove", "id", uTavern.getId(), "star", uTavern.getStar());
                return true;
            }
        }
        return false;
    }

    private void questCancel() {
        int idQuest = getInputInt();
        if (!mFarmQuest.containsKey(idQuest)) {
            addErrResponse(getLang(Lang.farm_quest_not_exist_quest));
            return;
        }
        if (removeQuest(idQuest)) {
            questStatus();
        } else {
            addErrResponse();
        }
    }


    private void status() {
        List<UserLandEntity> uLands = new ArrayList<>(mUser.getResources().getMLand().values());
        // status
        Pbmethod.PbListLand.Builder status = Pbmethod.PbListLand.newBuilder();
        for (int i = 0; i < uLands.size(); i++) {
            status.addALand(uLands.get(i).toProto());
        }
        //tree
        status.addAllTreeStatus(mUser.getUData().getDataTree());
        //deco
        status.addAllDeco(mUser.getUData().getDataDeco());
        // todo bonus farm
        List<Integer> farmPoint = mUser.getUData().getFarmPoint();
        status.setBonusTime(farmPoint.get(DecoPointType.DEC_TIME.value));
        status.setBonusItem(farmPoint.get(DecoPointType.INC_QUANTITY.value));
        status.setBonusExp(farmPoint.get(DecoPointType.INC_EXP.value));
        addResponse(status.build());
        //toto npc work
        if (CfgFarm.hasNPC(mUser)) {
            quickCare(Arrays.asList((long) FarmCareType.HARVEST.value), true);
            quickHarvestTree();
        }

    }

    private void buyLand() {
        // check max land
        List<UserLandEntity> uLand = new ArrayList<>(mUser.getResources().getMLand().values());
        UserLandEntity curLand = uLand.stream().max(Comparator.comparing(UserLandEntity::getId)).orElse(null);
        int openId = 1;
        if (curLand != null) {
            openId = curLand.getId() + 1;
            if (curLand.getId() == CfgFarm.config.maxLand) {
                addErrResponse(getLang(Lang.err_max_land));
                return;
            }
        }
        // check require
        int indexLand = curLand == null ? 0 : openId - 1;
        int requireLevel = CfgFarm.hasLevelBuyLand(user.getLevel(), indexLand);
        if (requireLevel > 0) {
            addErrResponse(String.format(getLang(Lang.err_has_level_for_buy), requireLevel));
            return;
        }

        List<Long> fee = Bonus.reverseBonus(CfgFarm.getFeeBuyLand(openId));
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        UserLandEntity newLand = new UserLandEntity(user.getId(), openId);
        List<Long> bonus = Bonus.receiveListItem(mUser, DetailActionType.BUY_LAND.getKey(openId), fee);
        if (bonus != null && DBJPA.save(newLand)) {
            addResponse(newLand.toProto().build());
            addBonusToast(bonus);
            mUser.getResources().addLand(newLand);
            if (CfgServer.isRealServer())
                Actions.save(mUser.getUser(), Actions.GRECEIVE, "open_land", "land_id", openId);
            mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.BUY_LAND, 1);
            CfgEvent.processTriggerEventTimer(mUser, openId, TriggerEventTimer.OPEN_LAND);
            // check event 7 day
            UserEventSevenDayEntity uEvent = Services.userDAO.getUserSevenDay(mUser);
            if (uEvent.hasEvent() && uEvent.hasActive(5) && uEvent.update(List.of("buy_land", uEvent.getBuyLand() + 1))) {
                uEvent.setBuyLand(uEvent.getBuyLand() + 1);
            }
            mUser.getUData().checkStatusTut(mUser, QuestTutType.HAS_LAND, 0, this);
        } else addErrResponse(getLang(Lang.err_system_down));
    }


    private void care() {
        List<Long> inputs = getInputALong();
        FarmCareType type = FarmCareType.get(inputs.get(0).intValue());
        int landId = inputs.get(1).intValue();
        UserLandEntity uLand = mUser.getResources().getMLand().get(landId);
        if (type == null || uLand == null) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        switch (type) {
            case PLANT -> plant(uLand, inputs.get(2).intValue());
            case WATER -> water(uLand);
            case FERTILIZE -> fertilize(uLand, inputs.get(2).intValue());
            case PLUCK -> pluck(uLand);
            case FER_TIME -> ferTime(uLand, inputs.get(2).intValue());
            case HARVEST -> harvest(uLand);
        }
    }


    void plant(UserLandEntity uLand, int seedId) {
        List<Integer> info = uLand.getInfo();
        if (info.get(ID_TREE) != 0) {
            addErrResponse(getLang(Lang.err_farm_has_tree));
            return;
        }
        UserItemFarmEntity seed = mUser.getResources().getItemFarm(ItemFarmType.SEED, seedId);
        List<Long> bonus = checkItemFarm(seed, DetailActionType.FARM_PLANT.getKey(seedId));
        if (bonus == null) return;
        info = plantInfo(mUser.getUData(), seedId);
        if (uLand.updateInfo(info)) {
            addBonusLand(TYPE_LAND, uLand.getId(), bonus);
            addResponse(uLand.toProto().build());
            CfgQuest.addNumQuest(mUser, DataQuest.PLANT_FARM, 1);
            mUser.getUData().checkQuestTutorial(mUser, QuestTutType.PLAN_FARM, info.get(0), 1);
        } else {
            addErrResponse(getLang(Lang.err_system_down));
            Bonus.receiveListItem(mUser, DetailActionType.FARM_PLANT.getKey(seedId), Bonus.viewItemFarm(seed, 1));
        }
    }

    private List<Integer> plantInfo(UserDataEntity uData, int seedId) {
        List<Integer> info = NumberUtil.genListInt(CfgFarm.MaxSizeInfoFarm, 0);
        info.set(ID_TREE, seedId);
        int curSeconds = (int) (Calendar.getInstance().getTimeInMillis() / 1000);
        info.set(CfgFarm.TIME_PLANT, curSeconds);
        ResSeedEntity resSeed = ResFarm.getSeed(seedId);
        info.set(CfgFarm.TIME_HARVEST, curSeconds + resSeed.getSeed().getTimeHarvest(uData));
        if (CfgFarm.hasNPC(mUser)) {
            info.set(CfgFarm.WATER, 1);
        }
        return info;
    }

    void water(UserLandEntity uLand) {
        List<Integer> info = uLand.getInfo();
        if (!CfgFarm.treeAlive(info)) {
            addErrResponse(getLang(Lang.err_farm_not_tree));
            return;
        }
        if (!CfgFarm.beforeHarvest(info)) {
            addErrResponse(getLang(Lang.err_farm_not_use));
            return;
        }
        if (info.get(CfgFarm.WATER) == 0) {
            info.set(CfgFarm.WATER, 1);
            uLand.updateInfo(info);
        }
        addResponse(uLand.toProto().build());
    }

    private void fertilize(UserLandEntity uLand, int infoId) {
        List<Integer> info = uLand.getInfo();
        if (!CfgFarm.treeAlive(info)) {
            addErrResponse(getLang(Lang.err_farm_not_tree));
            return;
        }
        if (!CfgFarm.beforeHarvest(info)) {
            addErrResponse(getLang(Lang.err_farm_not_use));
            return;
        }
        if (info.get(CfgFarm.FERTILIZE) > 0) {
            addErrResponse(getLang(Lang.err_farm_has_fertilize));
            return;
        }

        UserItemFarmEntity uFarm = mUser.getResources().getItemFarm(ItemFarmType.TOOL, infoId);
        ResItemToolEntity resTool = (ResItemToolEntity) uFarm.getRes();
        if (resTool.getType() != ToolFarmType.FERTILIZE) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        List<Long> bonus = checkItemFarm(uFarm, DetailActionType.FARM_FERTILIZE.getKey(infoId));
        if (bonus == null) {
            return;
        }
        info.set(CfgFarm.FERTILIZE, infoId);
        if (uLand.updateInfo(info)) {
            addResponse(uLand.toProto().build());
            addBonusLand(TYPE_LAND, uLand.getId(), bonus);
            mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.USE_FERTILIZER, 1);
        } else {
            Bonus.receiveListItem(mUser, DetailActionType.FARM_FERTILIZE.getKey(infoId), Bonus.viewItemFarm(uFarm, 1));
            addErrResponse(getLang(Lang.err_system_down));
        }
    }

    private void pluck(UserLandEntity uLand) {
        List<Integer> info = uLand.getInfo();
        if (info.get(ID_TREE) == 0) {
            addErrResponse(getLang(Lang.err_farm_not_tree));
            return;
        }
        List<Long> bonus = null;
        boolean treeAlive = CfgFarm.treeAlive(info);
        UserItemFarmEntity uFarm = mUser.getResources().getItemFarm(ItemFarmType.TOOL, FarmToolKey.XENG);
        if (treeAlive) {
            bonus = checkItemFarm(uFarm, DetailActionType.FARM_PLUCK.getKey(uLand.getId()));
            if (bonus == null) {
                return;
            }
        }
        info = NumberUtil.genListInt(CfgFarm.MaxSizeInfoFarm, 0);
        if (uLand.updateInfo(info)) {
            addResponse(uLand.toProto().build());
            if (treeAlive) addBonusLand(TYPE_LAND, uLand.getId(), bonus);
        } else {
            if (treeAlive)
                Bonus.receiveListItem(mUser, DetailActionType.FARM_PLUCK.getKey(uLand.getId()), Bonus.viewItemFarm(uFarm, 1));
            addErrResponse(getLang(Lang.err_system_down));
        }
    }

    private void ferTime(UserLandEntity uLand, int intValue) {
        List<Integer> info = uLand.getInfo();
        if (!CfgFarm.treeAlive(info)) {
            addErrResponse(getLang(Lang.err_farm_not_tree));
            return;
        }
        if (!CfgFarm.beforeHarvest(info)) {
            addErrResponse(getLang(Lang.err_farm_not_use));
            return;
        }
        UserItemFarmEntity uFarm = mUser.getResources().getItemFarm(ItemFarmType.TOOL, intValue);
        ResItemToolEntity resTool = (ResItemToolEntity) uFarm.getRes();
        if (resTool.getType() != ToolFarmType.FER_TIME) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        List<Integer> dataTool = resTool.getData();
        if (info.get(CfgFarm.FER_TIME) > 0 && dataTool.get(0) == CfgFarm.ADD_PERCENT) {
            addErrResponse(getLang(Lang.err_farm_has_fer_time));
            return;
        }
        List<Long> bonus = checkItemFarm(uFarm, DetailActionType.FARM_FER_TIME.getKey(intValue));
        if (bonus == null) {
            return;
        }
        if (dataTool.get(0) == CfgFarm.ADD_NUMBER) { // trừ phút
            int newTime = info.get(CfgFarm.TIME_HARVEST) - DateTime.minuteToSeconds(dataTool.get(1));
            info.set(CfgFarm.TIME_HARVEST, newTime);
        } else if (dataTool.get(0) == CfgFarm.ADD_PERCENT) { // trừ % theo max -> cur Time
            int curTime = (int) (Calendar.getInstance().getTimeInMillis() / 1000);
            if (curTime < info.get(CfgFarm.TIME_HARVEST)) {
                int newTime = (int) (info.get(CfgFarm.TIME_HARVEST) - (float) dataTool.get(1) / 100 * (info.get(CfgFarm.TIME_HARVEST) - curTime));
                info.set(CfgFarm.TIME_HARVEST, newTime);
                info.set(CfgFarm.FER_TIME, 1);
            }
        }
        if (uLand.updateInfo(info)) {
            addResponse(uLand.toProto().build());
            addBonusLand(TYPE_LAND, uLand.getId(), bonus);
            mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.USE_FERTILIZER, 1);
        } else {
            Bonus.receiveListItem(mUser, DetailActionType.FARM_FER_TIME.getKey(intValue), Bonus.viewItemFarm(uFarm, 1));
            addErrResponse(getLang(Lang.err_system_down));
        }
    }

    private void harvest(UserLandEntity uLand) {
        List<Integer> info = uLand.getInfo();
        if (!CfgFarm.treeAlive(info)) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        int curSeconds = (int) (Calendar.getInstance().getTimeInMillis() / 1000);
        if (curSeconds < info.get(CfgFarm.TIME_HARVEST)) {
            addErrResponse(getLang(Lang.err_farm_harvest_time));
            return;
        }
        int treeId = info.get(0);
        List<Long> ret = CfgFarm.getBonusHarvest(mUser.getUData(), uLand, user.getLevel());
        ret.addAll(CfgEventDrop.bonusDrop(CfgEventDrop.config.getRateDropTree(), 1));
        info = NumberUtil.genListInt(CfgFarm.MaxSizeInfoFarm, 0);
        if (uLand.updateInfo(info)) {
            List<Long> bonus = Bonus.receiveListItem(mUser, DetailActionType.HARVEST_FARM.getKey(uLand.getId()), ret);
            if (bonus.isEmpty()) {
                addErrResponse(getLang(Lang.err_system_down));
                return;
            }
            addResponse(uLand.toProto().build());
            addBonusLand(TYPE_LAND, uLand.getId(), bonus);
            CfgQuest.addNumQuest(mUser, DataQuest.HARVEST_FARM, 1);
            mUser.getUData().checkQuestTutorial(mUser, QuestTutType.HARVEST, treeId, 1);
        } else addErrResponse(getLang(Lang.err_system_down));
    }


    List<Long> checkItemFarm(UserItemFarmEntity uFarm, String detail) {
        if (uFarm == null ) {
            addErrResponse(getLang(Lang.err_not_enough_items));
            return null;
        }
        if ( uFarm.getNumber() < 1) {
            addErrResponse(String.format(getLang(Lang.err_not_enough_item),uFarm.getRes().getName()));
            return null;
        }

        List<Long> bonus = Bonus.receiveListItem(mUser, detail, Bonus.viewItemFarm(uFarm, -1));
        if (bonus.isEmpty()) {
            addErrResponse(getLang(Lang.err_system_down));
            return null;
        }
        return bonus;
    }


    private void sellFarm() {
        try {
            List<Long> inputs = getInputALong();
            int gold = 0;
            int xu = 0;
            List<Long> bonus = new ArrayList<>();
            for (int i = 0; i < inputs.size(); i += 3) {
                ItemFarmType itemType = ItemFarmType.get(inputs.get(i).intValue());
                int itemId = inputs.get(i + 1).intValue();
                UserItemFarmEntity uItem = mUser.getResources().getItemFarm(itemType, itemId);
                int number = inputs.get(i + 2).intValue();
                if (uItem == null || number > uItem.getNumber()) {
                    addErrResponse(getLang(Lang.err_not_enough_item_farm));
                    return;
                }
                AbstractItemFarm res = uItem.getRes();
                if (itemType == ItemFarmType.AGRI) xu += res.getSell() * number;
                else gold += res.getSell() * number;
                bonus.addAll(Bonus.viewItemFarm(itemType, itemId, -number));
            }
            if (gold > 0) bonus.addAll(Bonus.viewGold(gold));
            if (xu > 0) bonus.addAll(Bonus.viewItem(ItemKey.XU_NONG_TRAI, xu));
            List<Long> ret = Bonus.receiveListItem(mUser, DetailActionType.SELL_AGRI.getKey(), bonus);
            if (ret.isEmpty()) {
                addErrResponse(getLang(Lang.err_system_down));
                return;
            }
            addResponse(null);
            addBonusToastPlus(ret);
        } catch (Exception e) {
            addErrResponse(getLang(Lang.err_system_down));
        }
    }

    private void sellSingle() {
        List<Long> inputs = getInputALong();
        ItemFarmType type = ItemFarmType.get(Math.toIntExact(inputs.get(0)));
        int id = Math.toIntExact(inputs.get(1));
        int number = Math.toIntExact(inputs.get(2));
        if (type == null || number <= 0) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        UserItemFarmEntity uFarm = mUser.getResources().getItemFarm(type, Math.toIntExact(id));
        if (uFarm == null || uFarm.getNumber() < number) {
            addErrResponse(getLang(Lang.err_item_number));
            return;
        }
        List<Long> bonus = new ArrayList<>();
        AbstractItemFarm resFarm = uFarm.getRes();
        bonus.addAll(Bonus.viewItemFarm(uFarm, -number));
        if (type == ItemFarmType.AGRI) bonus.addAll(Bonus.viewItem(ItemKey.XU_NONG_TRAI, resFarm.getSell() * number));
        else bonus.addAll(Bonus.viewGold(resFarm.getSell() * number));
        List<Long> ret = Bonus.receiveListItem(mUser, DetailActionType.SELL_FARM_SINGLE.getKey(id), bonus);
        if (ret.isEmpty()) {
            addErrResponse(getLang(Lang.err_system_down));
            return;
        }
        addResponse(null);
        addBonusToastPlus(ret);
    }

    private void createFood() {
        List<Long> inputs = getInputALong();
        int idFood = inputs.get(0).intValue();
        int number = inputs.get(1).intValue();
        ResItemFoodEntity food = ResFarm.getItemFood(idFood);
        if (food == null || number < 0) {
            addErrParam();
            return;
        }
        List<Long> fee = new ArrayList<>();
        List<List<Long>> aFee = food.getMaterial();
        for (int i = 0; i < aFee.size(); i++) {
            fee.addAll(Bonus.viewXNumber(aFee.get(i), -number));
        }
        fee.addAll(Bonus.viewGold(food.getFeeGold() * -number));
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        // create food
        fee.addAll(food.getFood(number));
        addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.CREATE_FOOD.getKey(idFood), fee)));
    }

    private void quickHarvestTree() {
        List<Long> dataTree = mUser.getUData().getDataTree();
        List<Long> aBonus = new ArrayList<>();
        for (int i = 0; i < dataTree.size(); i++) {
            long timeOld = dataTree.get(i);
            if (timeOld <= 0) continue;
            ResSeedEntity resTree = ResFarm.getTree(i);
            if (resTree == null) continue;
            int timeReceive = resTree.getTimeHarvest(mUser.getUData());
            int curSeconds = (int) (Calendar.getInstance().getTimeInMillis() / 1000);
            if (curSeconds < timeOld + timeReceive) {
                continue;
            }
            int numReceive = CfgFarm.checkHarvestInSeasonBuff(mUser.getUData(), false, resTree);
            List<Long> ret = Bonus.viewItemFarm(ItemFarmType.AGRI, resTree.getId(), numReceive);
            if (!ret.isEmpty()) {
                ret.addAll(CfgFarm.bonusExpHarvest(mUser.getUData(), resTree, user.getLevel()));
                dataTree.set(i, System.currentTimeMillis() / 1000);
                aBonus.addAll(ret);
            }
        }
        if (aBonus.isEmpty()) return;
        List<Long> bonus = Bonus.receiveListItem(mUser, DetailActionType.QUICK_HARVEST_TREE.getKey(), aBonus);
        if (bonus != null) {
            if (mUser.getUData().updateTreeData(dataTree)) {
                addBonusToastPlus(bonus);
            } else {
                Bonus.receiveListItem(mUser, DetailActionType.QUICK_HARVEST_TREE.getKey(), Bonus.reverseBonus(bonus));
            }
        }
        addResponse(IAction.FARM_TREE_STATUS, getCommonVector(dataTree));
    }

    private void quickCare(List<Long> inputs, boolean auto) {
        if (!CfgFarm.hasNPC(mUser)) {
            addErrResponse(getLang(Lang.err_npc_farm));
            return;
        }
        FarmCareType type = FarmCareType.get(inputs.get(0).intValue());
        int itemId = inputs.size() > 1 ? inputs.get(1).intValue() : 0;
        int number = inputs.size() > 2 ? inputs.get(2).intValue() : 0;
        List<UserLandEntity> uLands = new ArrayList<>(mUser.getResources().getMLand().values());
        if (uLands.isEmpty()) {
            addErrResponse(getLang(Lang.err_no_farm_slot));
            return;
        }
        switch (type) { // care basic
            case PLANT -> quickPlant(uLands, itemId, number);
            case PLUCK -> quickPluck(uLands); // dọn vườn
            case HARVEST -> quickHarvest(uLands, auto); // thu hoạch
            default -> {
                if (CfgFarm.hasBoNPC(mUser)) {
                    addErrResponse(getLang(Lang.err_npc_refuse_perform));
                    return;
                }
                switch (type) {
                    case VIP_PLUCK -> quickVipPluck(uLands); // nhổ cây
                    case FERTILIZE -> quickFertilize(uLands, itemId, number);
                    case FER_TIME -> quickFerTime(uLands, itemId, number);
                    default -> addErrParam();
                }
            }

        }
    }

    void buyItem() {
        List<Long> inputs = getInputALong();
        int type = inputs.get(0).intValue();
        int index = inputs.get(1).intValue();
        int number = inputs.get(2).intValue();
        if ((type != TYPE_TREE && type != TYPE_DECO) || number <= 0) {
            addErrParam();
            return;
        }
        if (type == TYPE_TREE) {
            ResSeedEntity resTree = ResFarm.getTree(index);
            if (resTree == null) {
                addErrParam();
                return;
            }
            if (user.getLevel() < resTree.getLevelRequire()) {
                addErrResponse(String.format(getLang(Lang.err_level_buy), resTree.getLevelRequire()));
                return;
            }

            // check data
            List<Long> dataTree = mUser.getUData().getDataTree();
            if (dataTree.get(index) != 0) //đã mua
            {
                addErrResponse(getLang(Lang.err_farm_has_buy_tree));
                return;
            }
            List<Long> price = Bonus.reverseBonus(resTree.getPrice());
            String err = Bonus.checkMoney(mUser, price);
            if (err != null) {
                addErrResponse(err);
                return;
            }
            List<Long> bonus = Bonus.receiveListItem(mUser, DetailActionType.BUY_TREE_FARM.getKey(resTree.getId()), price);
            if (bonus == null) {
                addErrSystem();
                return;
            }
            dataTree.set(index, System.currentTimeMillis() / 1000);
            if (mUser.getUData().updateTreeData(dataTree)) {
                addBonusToast(bonus);
                Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
                pb.addAVector(getCommonVector(inputs));
                pb.addAVector(getCommonVector(dataTree));
                addResponse(pb.build());
            } else {
                Bonus.receiveListItem(mUser, DetailActionType.BUY_TREE_FARM.getKey(resTree.getId()), Bonus.reverseBonus(price));
                addErrSystem();
            }
        } else { // buy deco
            int decoId = index + 1;
            ResFarmDecoEntity rDeco = ResFarm.getDeco(decoId);
            if (rDeco == null || number <= 0) {
                addErrParam();
                return;
            }
            List<Integer> dataDeco = mUser.getUData().getDataDeco();
            int curBuy = dataDeco.get(index);
            if (curBuy + number > rDeco.getNumber()) {
                addErrResponse(getLang(Lang.err_max_can_buy));
                return;
            }
            List<Long> price = rDeco.getPrice();
            List<Long> bonus = Bonus.receiveListItem(mUser, DetailActionType.BUY_DECO_FARM.getKey(decoId), Bonus.reverseBonus(price));
            if (bonus == null) {
                addErrSystem();
                return;
            }
            dataDeco.set(index, dataDeco.get(index) + number);
            List<Integer> farmPoint = mUser.getUData().addFarmPoint(rDeco.getData());
            if (mUser.getUData().updateFarmDeco(dataDeco, farmPoint)) {
                addBonusToast(bonus);
                Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
                pb.addAVector(getCommonVector(inputs));
                pb.addAVector(getCommonIntVector(dataDeco));
                pb.addAVector(getCommonIntVector(farmPoint));
                addResponse(pb.build());
            } else {
                Bonus.receiveListItem(mUser, DetailActionType.BUY_DECO_FARM.getKey(decoId), Bonus.reverseBonus(price));
                addErrSystem();
            }
        }

    }

    void harvestTree() {
        int index = getInputInt();
        ResSeedEntity resTree = ResFarm.getTree(index);
        if (resTree == null) {
            addErrParam();
            return;
        }
        List<Long> dataTree = mUser.getUData().getDataTree();
        long timeOld = dataTree.get(index);
        if (timeOld <= 0) {
            addErrResponse(getLang(Lang.err_farm_no_has_tree));
            return;
        }
        int tineReceive = resTree.getTimeHarvest(mUser.getUData());
        int curSeconds = (int) (Calendar.getInstance().getTimeInMillis() / 1000);
        if (curSeconds < timeOld + tineReceive) {
            addErrResponse(getLang(Lang.err_farm_harvest_time));
            return;
        }
        // check có đang trong mùa hay không
        int numReceive = CfgFarm.checkHarvestInSeasonBuff(mUser.getUData(), false, resTree);
        List<Long> ret = Bonus.viewItemFarm(ItemFarmType.AGRI, resTree.getId(), numReceive);
        ret.addAll(CfgFarm.bonusExpHarvest(mUser.getUData(), resTree, user.getLevel()));
        List<Long> bonus = Bonus.receiveListItem(mUser, DetailActionType.HARVEST_TREE.getKey(resTree.getId()), ret);
        bonus.addAll(CfgEventDrop.bonusDrop(CfgEventDrop.config.getRateDropTree(), 1));
        dataTree.set(index, System.currentTimeMillis() / 1000);
        if (mUser.getUData().updateTreeData(dataTree)) {
            addBonusLand(TYPE_TREE, index, bonus);
            addResponse(getCommonVector(dataTree));
        } else {
            Bonus.receiveListItem(mUser, DetailActionType.HARVEST_TREE.getKey(resTree.getId()), Bonus.viewItemFarm(ItemFarmType.AGRI, resTree.getId(), -numReceive));
            addErrSystem();
        }
    }


    private void quickFertilize(List<UserLandEntity> uLands, int itemId, int number) {
        if (number <= 0) {
            addErrParam();
            return;
        }
        UserItemFarmEntity tool = mUser.getResources().getItemFarm(ItemFarmType.TOOL, itemId);
        if (tool == null || tool.getNumber() <= 0) {
            addErrResponse(getLang(Lang.err_not_enough_item_tool));
        }
        ResItemToolEntity resTool = (ResItemToolEntity) tool.getRes();
        if (resTool.getType() != ToolFarmType.FERTILIZE) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        int numberItem = tool.getNumber() < number ? tool.getNumber() : number;
        List<Long> aBonus = new ArrayList<>();
        List<UserLandEntity> landUpdate = new ArrayList<>();
        Pbmethod.PbListLand.Builder pb = Pbmethod.PbListLand.newBuilder();
        for (int i = 0; i < uLands.size(); i++) {
            UserLandEntity land = uLands.get(i);
            List<Integer> info = land.getInfo();
            if (!CfgFarm.treeAlive(info)) {
                continue;
            }
            if (!CfgFarm.beforeHarvest(info)) {
                continue;
            }
            if (info.get(CfgFarm.FERTILIZE) > 0) {
                continue;
            }
            if (numberItem <= 0) {
                break;
            }
            numberItem--;
            List<Long> bonus = Bonus.viewItemFarm(tool, -1);
            info.set(CfgFarm.FERTILIZE, itemId);
            aBonus.addAll(bonus);
            land.setCacheInfo(info);
            land.setCacheBonus(bonus);
            landUpdate.add(land);

        }
        if (landUpdate.isEmpty()) return; // k có j thay đổi
        List<Long> retBonus = Bonus.receiveListItem(mUser, DetailActionType.QUICK_FERTILIZE.getKey(itemId), Bonus.viewItemFarm(tool, -landUpdate.size()));
        if (retBonus.isEmpty()) {
            addErrSystem();
            return;
        }
        pb.addAllABonus(retBonus);
        if (updateInfoFarm(landUpdate)) {
            for (int i = 0; i < landUpdate.size(); i++) {
                UserLandEntity uLand = mUser.getResources().getMLand().get(landUpdate.get(i).getId());
                uLand.setInfo(landUpdate.get(i).getCacheInfo().toString());
                // to proto
                Pbmethod.PbLand.Builder landProto = uLand.toProto();
                landProto.addAllBonus(uLand.getCacheBonus());
                pb.addALand(landProto);
            }
            addResponse(pb.build());
            mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.USE_FERTILIZER, landUpdate.size());
        } else {
            Bonus.receiveListItem(mUser, DetailActionType.QUICK_FERTILIZE.getKey(), Bonus.viewItemFarm(tool, +landUpdate.size()));
        }
    }

    private void quickFerTime(List<UserLandEntity> uLands, int itemId, int number) {
        if (number <= 0) {
            addErrParam();
            return;
        }
        UserItemFarmEntity tool = mUser.getResources().getItemFarm(ItemFarmType.TOOL, itemId);
        if (tool == null || tool.getNumber() <= 0) {
            addErrResponse(getLang(Lang.err_not_enough_item_tool));
        }
        ResItemToolEntity resTool = (ResItemToolEntity) tool.getRes();

        if (resTool.getType() != ToolFarmType.FER_TIME) {
            addErrParam();
            return;
        }
        List<Integer> dataTool = resTool.getData();
        int numberItem = tool.getNumber() < number ? tool.getNumber() : number;
        List<Long> aBonus = new ArrayList<>();
        List<UserLandEntity> landUpdate = new ArrayList<>();
        Pbmethod.PbListLand.Builder pb = Pbmethod.PbListLand.newBuilder();
        for (int i = 0; i < uLands.size(); i++) {
            UserLandEntity land = uLands.get(i);

            List<Integer> info = land.getInfo();
            if (!CfgFarm.treeAlive(info)) {
                continue;
            }
            if (!CfgFarm.beforeHarvest(info)) {
                continue;
            }
            if (info.get(CfgFarm.FER_TIME) > 0 && dataTool.get(0) == CfgFarm.ADD_PERCENT) {
                continue;
            }
            if (numberItem <= 0) {
                break;
            }
            numberItem--;
            // update info
            if (dataTool.get(0) == CfgFarm.ADD_NUMBER) { // trừ phút
                int newTime = info.get(CfgFarm.TIME_HARVEST) - DateTime.minuteToSeconds(dataTool.get(1));
                info.set(CfgFarm.TIME_HARVEST, newTime);
            } else if (dataTool.get(0) == CfgFarm.ADD_PERCENT) { // trừ % theo max -> cur Time
                int curTime = (int) (Calendar.getInstance().getTimeInMillis() / 1000);
                if (curTime < info.get(CfgFarm.TIME_HARVEST)) {
                    int newTime = info.get(CfgFarm.TIME_HARVEST) - (int) ((dataTool.get(1) / 100f) * (info.get(CfgFarm.TIME_HARVEST) - curTime));
                    info.set(CfgFarm.TIME_HARVEST, newTime);
                    info.set(CfgFarm.FER_TIME, 1);
                }
            }
            List<Long> bonus = Bonus.viewItemFarm(tool, -1);
            aBonus.addAll(bonus);
            land.setCacheBonus(bonus);
            land.setCacheInfo(info);
            landUpdate.add(land);
        }
        if (landUpdate.isEmpty()) {// k có j thay đổi
            addErrResponse(getLang(Lang.err_farm_not_fer_time));
            return;
        }
        List<Long> retBonus = Bonus.receiveListItem(mUser, DetailActionType.QUICK_FER_TIME.getKey(itemId), Bonus.viewItemFarm(tool, -landUpdate.size()));
        if (retBonus.isEmpty()) {
            addErrSystem();
            return;
        }
        pb.addAllABonus(retBonus);
        if (updateInfoFarm(landUpdate)) {
            for (int i = 0; i < landUpdate.size(); i++) {
                UserLandEntity uLand = mUser.getResources().getMLand().get(landUpdate.get(i).getId());
                uLand.setInfo(landUpdate.get(i).getCacheInfo().toString());
                uLand.setCacheInfo(landUpdate.get(i).getCacheInfo());
                // to proto
                Pbmethod.PbLand.Builder landProto = uLand.toProto();
                landProto.addAllBonus(landUpdate.get(i).getCacheBonus());
                pb.addALand(landProto);
            }
            addResponse(pb.build());
        } else {
            Bonus.receiveListItem(mUser, DetailActionType.QUICK_FER_TIME.getKey(), Bonus.viewItemFarm(tool, +landUpdate.size()));
            addErrSystem();
        }
    }


    private void quickPlant(List<UserLandEntity> uLands, int itemId, int number) {
        if (number <= 0) {
            addErrParam();
            return;
        }
        UserItemFarmEntity seed = mUser.getResources().getItemSeed(itemId);
        if (seed == null) {
            addErrResponse(getLang(Lang.err_not_enough_item_seed));
            return;
        }
        List<Integer> landUpdate = new ArrayList<>();
        Pbmethod.PbListLand.Builder pb = Pbmethod.PbListLand.newBuilder();
        int curNum = seed.getNumber() < number ? seed.getNumber() : number;
        String info = StringHelper.toDBString(plantInfo(mUser.getUData(), seed.getId()));
        for (int i = 0; i < uLands.size(); i++) {
            UserLandEntity land = uLands.get(i);
            if (land.getInfo().get(ID_TREE) == 0 && curNum > 0) {
                curNum--;
                land.setInfo(info);
                landUpdate.add(land.getId());
                Pbmethod.PbLand.Builder landProto = land.toProto();
                landProto.addAllBonus(Bonus.viewItemFarm(seed, -1));
                pb.addALand(landProto.build());
            }
        }
        // check dựa theo số lương hạt giống đã gieo
        if (landUpdate.isEmpty()) {
            addErrResponse(getLang(Lang.err_no_plant));
            return;
        }
        List<Long> bonus = Bonus.receiveListItem(mUser, DetailActionType.QUICK_PLANT.getKey(), Bonus.viewItemFarm(seed, -landUpdate.size()));
        if (bonus.isEmpty()) {
            addErrResponse();
            return;
        }
        pb.addAllABonus(bonus);

        //todo update DB
        String lst = NumberUtil.joiningListInt(landUpdate);
        if (DBJPA.rawSQL("update " + CfgServer.DB_DSON + "user_land set info='" + info + "' where user_id = " + user.getId() + " and id in(" + lst + ")")) {
            addResponse(pb.build());
            for (int i = 0; i < landUpdate.size(); i++) {
                UserLandEntity uLand = mUser.getResources().getMLand().get(landUpdate.get(i));
                uLand.setInfo(info);
            }
            CfgQuest.addNumQuest(mUser, DataQuest.PLANT_FARM, landUpdate.size());
            mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.PLAN_FARM, landUpdate.size());
        } else {
            Bonus.receiveListItem(mUser, DetailActionType.QUICK_PLANT.getKey(), Bonus.viewItemFarm(seed, +landUpdate.size()));
        }
    }

    private void quickPluck(List<UserLandEntity> uLands) {
        List<Integer> landUpdate = new ArrayList<>();
        Pbmethod.PbListLand.Builder pb = Pbmethod.PbListLand.newBuilder();
        String info = NumberUtil.genListStringInt(CfgFarm.MaxSizeInfoFarm, 0);
        for (int i = 0; i < uLands.size(); i++) {
            UserLandEntity land = uLands.get(i);
            List<Integer> infoLand = land.getInfo();
            if (infoLand.get(ID_TREE) == 0) continue;
            if (!CfgFarm.treeAlive(infoLand)) {
                land.setInfo(info);
                landUpdate.add(land.getId());
                pb.addALand(land.toProto().build());
            }
        }
        if (landUpdate.isEmpty()) {// k có j thay đổi
            addErrResponse(getLang(Lang.err_no_pluck));
            return;
        }
        String lst = NumberUtil.joiningListInt(landUpdate);
        if (DBJPA.rawSQL("update " + CfgServer.DB_DSON + "user_land set info='" + info + "' where user_id = " + user.getId() + " and id in(" + lst + ")")) {
            addResponse(pb.build());
            for (int i = 0; i < landUpdate.size(); i++) {
                UserLandEntity uLand = mUser.getResources().getMLand().get(landUpdate.get(i));
                uLand.setInfo(info);
            }
        }
    }

    private void quickHarvest(List<UserLandEntity> uLands, boolean auto) {
        List<Integer> landUpdate = new ArrayList<>();
        List<Long> bonus = new ArrayList<>();
        Pbmethod.PbListLand.Builder pb = Pbmethod.PbListLand.newBuilder();
        String infoDefault = NumberUtil.genListStringInt(CfgFarm.MaxSizeInfoFarm, 0);
        Map<Integer, Integer> lstTreeId = new HashMap<>();
        for (int i = 0; i < uLands.size(); i++) {
            UserLandEntity land = uLands.get(i);
            List<Integer> info = land.getInfo();
            if (info.get(CfgFarm.ID_TREE) == 0 || info.get(CfgFarm.TIME_HARVEST) + DateTime.DAY_SECOND * CfgFarm.config.dayAliveTree < DateTime.getSeconds()) {
                continue;
            }
            int curSeconds = (int) (Calendar.getInstance().getTimeInMillis() / 1000);
            if (curSeconds < info.get(CfgFarm.TIME_HARVEST)) {
                continue;
            }
            List<Long> agri = CfgFarm.getBonusHarvest(mUser.getUData(), land, user.getLevel());
            agri.addAll(CfgEventDrop.bonusDrop(CfgEventDrop.config.getRateDropTree(), 1));
            bonus.addAll(agri);
            landUpdate.add(land.getId());
            // pb
            land.setInfo(infoDefault);
            Pbmethod.PbLand.Builder landProto = land.toProto();
            landProto.addAllBonus(agri);
            pb.addALand(landProto);
            if (!lstTreeId.containsKey(info.get(CfgFarm.ID_TREE))) lstTreeId.put(info.get(CfgFarm.ID_TREE), 0);
            lstTreeId.put(info.get(CfgFarm.ID_TREE), lstTreeId.get(info.get(CfgFarm.ID_TREE)) + 1);
        }
        if (landUpdate.isEmpty()) // k có j thay đổi
        {
            if (!auto) addErrResponse(getLang(Lang.err_no_harvest));
            return;
        }
        String lst = NumberUtil.joiningListInt(landUpdate);
        List<Long> retBonus = Bonus.receiveListItem(mUser, DetailActionType.QUICK_HARVEST.getKey(), bonus);
        if (retBonus.isEmpty()) {
            addErrParam();
            return;
        }
        pb.addAllABonus(retBonus);
        if (DBJPA.rawSQL("update " + CfgServer.DB_DSON + "user_land set info='" + infoDefault + "' where user_id = " + user.getId() + " and id in(" + lst + ")")) {
            addResponse(FARM_QUICK_CARE, pb.build());
            for (int i = 0; i < landUpdate.size(); i++) {
                UserLandEntity uLand = mUser.getResources().getMLand().get(landUpdate.get(i));
                uLand.setInfo(infoDefault);
            }
            CfgQuest.addNumQuest(mUser, DataQuest.HARVEST_FARM, landUpdate.size());
            lstTreeId.forEach((key, value) -> {
                mUser.getUData().checkQuestTutorial(mUser, QuestTutType.HARVEST, key, value);
            });
        }
    }

    public void addBonusLand(int type, int id, List<Long> bonus) {
        bonus.add(0, (long) type);
        bonus.add(1, (long) id);
        addResponse(BONUS_LAND, CommonProto.getCommonVector(bonus));
    }

    private void quickVipPluck(List<UserLandEntity> uLands) {
        List<Integer> landUpdate = new ArrayList<>();
        List<Long> bonus = new ArrayList<>();
        Pbmethod.PbListLand.Builder pb = Pbmethod.PbListLand.newBuilder();
        String infoDefault = NumberUtil.genListStringInt(CfgFarm.MaxSizeInfoFarm, 0);
        UserItemFarmEntity xeng = mUser.getResources().getItemFarm(ItemFarmType.TOOL, FarmToolKey.XENG);
        if (xeng == null || xeng.getNumber() <= 0) {
            addErrResponse(getLang(Lang.err_not_enough_item_tool));
            return;
        }
        int numXeng = xeng.getNumber();
        for (int i = 0; i < uLands.size(); i++) {
            UserLandEntity land = uLands.get(i);
            List<Integer> info = land.getInfo();
            if (info.get(ID_TREE) == 0) {
                continue;
            }
            if (numXeng <= 0) break;
            // cây đang sống thì sẽ trừ xẻng, cây chết thì k trừ
            List<Long> tool = new ArrayList<>();
            if (CfgFarm.treeAlive(info)) {
                numXeng--;
                tool = Bonus.viewItemFarm(ItemFarmType.TOOL, FarmToolKey.XENG.value, -1);
                bonus.addAll(tool);
            }

            landUpdate.add(land.getId());
            // pb
            land.setInfo(infoDefault);
            Pbmethod.PbLand.Builder landProto = land.toProto();
            landProto.addAllBonus(tool);
            pb.addALand(landProto);
        }
        if (landUpdate.isEmpty()) // k có j thay đổi
        {
            addErrResponse(getLang(Lang.err_no_pluck));
            return;
        }
        String lst = NumberUtil.joiningListInt(landUpdate);

        List<Long> retBonus = Bonus.receiveListItem(mUser, DetailActionType.QUICK_VIP_PLUCK.getKey(), Bonus.viewItemFarm(ItemFarmType.TOOL, FarmToolKey.XENG.value, -landUpdate.size()));
        if (retBonus.isEmpty()) {
            addErrSystem();
            return;
        }
        pb.addAllABonus(retBonus);
        if (DBJPA.rawSQL("update " + CfgServer.DB_DSON + "user_land set info='" + infoDefault + "' where user_id = " + user.getId() + " and id in(" + lst + ")")) {
            addResponse(pb.build());
            for (int i = 0; i < landUpdate.size(); i++) {
                UserLandEntity uLand = mUser.getResources().getMLand().get(landUpdate.get(i));
                uLand.setInfo(infoDefault);
            }
        } else {
            addErrSystem();
            Bonus.receiveListItem(mUser, DetailActionType.QUICK_HARVEST.getKey(), Bonus.viewItemFarm(ItemFarmType.TOOL, FarmToolKey.XENG.value, +landUpdate.size()));
        }
    }

    private boolean updateInfoFarm(List<UserLandEntity> landUpdate) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            session.getTransaction().begin();

            for (UserLandEntity land : landUpdate) {
                Query q = session.createNativeQuery("update " + CfgServer.DB_DSON + "user_land set info = :info where user_id = :uid and id = :id");
                q.setParameter("info", StringHelper.toDBString(land.getCacheInfo()));
                q.setParameter("uid", user.getId());
                q.setParameter("id", land.getId());

                q.executeUpdate();
            }

            session.getTransaction().commit();
            return true;

        } catch (Exception ex) {
            Logs.error(ex);
            return false;

        } finally {
            closeSession(session);
        }
    }

    public void addFarmQuest(UserFarmQuestEntity uTavern) {
        aUserFarmQuest.add(uTavern);
        mFarmQuest.put(uTavern.getId(), uTavern);
    }

    public UserFarmQuestEntity addQuest(String k, int idQuest, boolean isFirst) {
        UserFarmQuestEntity uFarmQuest = new UserFarmQuestEntity(user.getId(), StringHelper.toDBString(CfgFarmQuest.getFarmQuestBonusShow(idQuest)), CfgFarmQuest.config.requireNumberItem[idQuest], idQuest + 1, isFirst);
        boolean isOk = DBJPA.save(uFarmQuest);
        if (!isOk) {
            return null;
        }
        Actions.save(user, "farmQuest", k, "id", uFarmQuest.getId(), "star", uFarmQuest.getStar());
        return uFarmQuest;
    }

    private boolean checkCompleteFarmQuest() {
        List<UserFarmQuestEntity> completeQuest = aUserFarmQuest.stream().filter(UserFarmQuestEntity::shouldComplete).collect(Collectors.toList());
        if (completeQuest.isEmpty()) return true;
        String strIds = completeQuest.stream().map(tavern -> String.valueOf(tavern.getId())).collect(Collectors.joining(","));
        if (DBJPA.rawSQL(String.format("update user_farm_quest set status=%s where id in (%s)", StatusType.DONE.value, strIds))) {
            completeQuest.forEach(quest -> quest.setStatus(StatusType.DONE.value));
            return true;
        }
        return false;
    }

    private void receiveOverCompleteTavern() {
        List<UserFarmQuestEntity> completeTaverns = aUserFarmQuest.stream().filter(UserFarmQuestEntity::isComplete).collect(Collectors.toList());
        int overSize = completeTaverns.size() - CfgFarmQuest.config.maxStore;
        if (overSize > 0) {
            completeTaverns.sort(Comparator.comparing(UserFarmQuestEntity::getStar));
            List<Integer> removeIds = new ArrayList<>();
            List<Long> aBonus = new ArrayList<>();
            for (int i = 0; i < overSize; i++) {
                UserFarmQuestEntity uTavern = completeTaverns.get(i);
                removeIds.add(uTavern.getId());
                aBonus.addAll(uTavern.getTavernBonus());
            }
            aBonus = Bonus.merge(aBonus);
            String ids = removeIds.stream().map(value -> String.valueOf(value)).collect(Collectors.joining(","));
            if (DBJPA.rawSQL("delete from user_tavern where tavern_id in (" + ids + ")")) {
                for (int i = 0; i < overSize; i++) {
                    UserFarmQuestEntity uTavern = completeTaverns.get(i);
                    aUserFarmQuest.remove(uTavern);
                    mFarmQuest.remove(uTavern.getId());
                    Actions.save(user, "farmQuest", "remove", "id", uTavern.getId(), "star", uTavern.getStar(), "bonus", StringHelper.toDBString(uTavern.getTavernBonus()));
                }
                MailCreatorCache.sendMail(UserMailEntity.builder().userId(user.getId()).title(getLang(Lang.farm_quest_body_auto_delete)).bonus(aBonus.toString().replace(" ", "")).build());
                addPopupResponse(String.format(getLang(Lang.farm_quest_title_auto_delete), overSize));
            }
        }
    }
}
