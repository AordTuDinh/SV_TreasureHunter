package game.dragonhero.mapping;

import game.battle.calculate.IMath;
import game.battle.object.Point;
import game.battle.type.StateType;
import game.config.CfgBag;
import game.config.CfgCheckin;
import game.config.CfgUser;
import game.config.aEnum.*;
import game.dragonhero.controller.AHandler;
import game.dragonhero.controller.UserHandler;
import game.dragonhero.mapping.main.*;
import game.dragonhero.server.IAction;
import game.dragonhero.service.resource.*;
import game.dragonhero.service.user.Bonus;
import game.object.MyUser;
import game.object.StatEntity;
import game.protocol.CommonProto;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.StringHelper;
import ozudo.base.helper.Util;
import protocol.Pbmethod;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.*;

@Data
@Entity
@Table(name = "user_data")
@NoArgsConstructor
public class UserDataEntity implements Serializable {
    @Id
    int userId;
    int levelGachaWeapon, levelGachaPet, levelTraining, numPointLevel, numStone, numStoneVip, friendNotify, tutorial, questTutorial, questTutorialNumber;
    String goldStat, levelStat, numSlot, dataInt, checkIn;
    String bossGod; // level 5 con boss mình đánh đc
    String campaign;//[id - number]
    String campaignReward;//[0-1] theo id
    String buff; // [time25,time50,timex2 ...] x9
    String farmTree, farmDeco, farmPoint, dameSkin, chatFrame, listTrial;
    int dameSkinEquip, chatFrameEquip, trialEquip,effInit;

    @Transient
    Map<Integer, StatEntity> aGoldStat = new HashMap<>();
    @Transient
    Map<Integer, StatEntity> aLevelStat = new HashMap<>();
    @Transient
    int maxLvTraining = 60;
    @Transient
    UserInt uInt;
    @Transient
    int countNormal = 0, numberUpdate = 100;
    @Transient
    List<Integer> dataCampaign; //[id - number]
    @Transient
    long lastClanChat;

    public UserDataEntity(int userId) {
        this.userId = userId;
        this.levelGachaPet = 1;
        this.numStone = 0;
        this.numStoneVip = 0;
        this.effInit  = 0;
        this.levelGachaWeapon = 1;
        this.numSlot = CfgBag.genBaseSlot(); // item - item Equipment - artifact
        this.goldStat = "[]";
        this.levelStat = "[]";
        this.campaign = "[1,0]";
        this.campaignReward = "[]";
        this.dameSkin = "[0]";
        this.listTrial = "[0]";
        this.chatFrame = "[0]";
        this.bossGod = NumberUtil.genListInt(5, 0).toString();
        this.checkIn = "[]";
        this.farmPoint = NumberUtil.genListStringInt(3, 0);// time - per - exp
        this.farmTree = NumberUtil.genListStringInt(ResFarm.farmTree.size(), 0);
        this.farmDeco = NumberUtil.genListStringInt(ResFarm.getMaxSizeDeco(), 0);
        this.tutorial = 0;
        this.questTutorialNumber = 0;
        this.questTutorial = 1;
        this.chatFrameEquip = 0;
        this.dameSkinEquip = 0;
        this.trialEquip = 0;
        this.buff = NumberUtil.genListStringInt(9, 0);
    }

    public String getGoldStat() {
        if (goldStat.isEmpty()) {
            goldStat = "[]";
        }
        return goldStat;
    }

    public void checkQuestTutorial(MyUser mUser, QuestTutType type, int idInfo, int number) {
        ResTutorialQuestEntity res = ResQuest.mTutQuest.get(questTutorial);
        if (res == null) return;
        // check đúng loại  type value k
        if (res.getType() == type && res.getIdInfo() == idInfo && updateTutQuestNumber(questTutorialNumber + number))
            Util.sendProtoData(mUser.getChannel(), CommonProto.getCommonVector(questTutorial, questTutorialNumber), IAction.TUTORIAL_QUEST_UPDATE);
    }

    public void checkStatusTut(MyUser mUser, QuestTutType type, int idInfo, AHandler handler) {
        ResTutorialQuestEntity res = ResQuest.mTutQuest.get(questTutorial);
        if (res == null) return;
        if (res.getType() == type && res.getIdInfo() == idInfo) UserHandler.tutorialQuestStatus(mUser, handler);
    }

    public void checkQuestTutDefault(MyUser mUser, QuestTutType type, int number) {
        checkQuestTutorial(mUser, type, 0, number);
    }


    public List<Long> getBuff() {
        List<Long> aBuff = GsonUtil.strToListLong(buff);
        while (aBuff.size() < BuffItemType.values().length) aBuff.add(0L);
        return aBuff;
    }

    public List<Integer> getBossGod() {
        return GsonUtil.strToListInt(bossGod);
    }

    public List<Integer> getFarmPoint() {
        List<Integer> ret = GsonUtil.strToListInt(farmPoint);
        while (ret.size() < 3) ret.add(0);
        return ret;
    }

    public List<Integer> getListDameSkin() {
        List<Integer> dameSkins = GsonUtil.strToListInt(dameSkin);
        if (!dameSkins.contains(0)) dameSkins.add(0);
        return dameSkins;
    }

    public List<Integer> getListChatFrame() {
        List<Integer> chatFrame = GsonUtil.strToListInt(this.chatFrame);
        if (!chatFrame.contains(0)) chatFrame.add(0);
        return chatFrame;
    }

    public List<Integer> getListIntTrial() {
        List<Integer> trials = GsonUtil.strToListInt(this.listTrial);
        if (!trials.contains(0)) trials.add(0);
        return trials;
    }

    public boolean addDameSkin(int skinId) {
        ResDameSkinEntity skin = ResAvatar.mDameSkin.get(skinId);
        if (skin == null) return false;
        List<Integer> lst = getListDameSkin();
        if (!lst.contains(skinId)) {
            lst.add(skinId);
            dameSkin = StringHelper.toDBString(lst);
            return true;
        }
        return false;
    }

    public boolean addChatFrame(int frameId) {
        ResChatFrameEntity frame = ResAvatar.mChatFrame.get(frameId);
        if (frame == null) return false;
        List<Integer> lst = getListChatFrame();
        if (!lst.contains(frameId)) {
            lst.add(frameId);
            chatFrame = StringHelper.toDBString(lst);
            return true;
        }
        return false;
    }

    public boolean addEffectTrial(int trialId) {
        ResEffectTrialEntity frame = ResAvatar.mTrial.get(trialId);
        if (frame == null) return false;
        List<Integer> lst = getListIntTrial();
        if (!lst.contains(trialId)) {
            lst.add(trialId);
            listTrial = StringHelper.toDBString(lst);
            return true;
        }
        return false;
    }

    public List<Integer> addFarmPoint(List<Float> farmPoint) {
        List<Integer> ret = getFarmPoint();
        for (int i = 0; i < ret.size(); i++) {
            ret.set(i, ret.get(i) + (int) (farmPoint.get(i) * 100));
        }
        return ret;
    }

    public List<Long> getDataTree() {
        List<Long> data = GsonUtil.strToListLong(farmTree);
        while (data.size() < ResFarm.farmTree.size()) {
            data.add(0L);
        }
        return data;
    }

    public List<Integer> getDataDeco() {
        List<Integer> data = GsonUtil.strToListInt(farmDeco);
        while (data.size() < ResFarm.getMaxSizeDeco()) {
            data.add(0);
        }
        return data;
    }

    public UserInt getUInt() {
        if (uInt == null) {
            uInt = new UserInt(dataInt, userId);
        }
        return uInt;
    }

    public String getLevelStat() {
        if (levelStat.isEmpty()) {
            levelStat = "[]";
        }
        return levelStat;
    }

    public List<Integer> getCampaign() {
        if (dataCampaign == null) dataCampaign = GsonUtil.strToListInt(campaign);
        return dataCampaign == null ? List.of(1, 0) : dataCampaign;
    }

    public List<Integer> getCampaignReward() {
        List<Integer> data = GsonUtil.strToListInt(campaignReward);
        boolean update = false;
        while (data.size() < ResMap.maxMapCampaign) {
            data.add(0);
            update = true;
        }
        if (update) {
            if (update(List.of("campaign_reward", StringHelper.toDBString(data)))) {
                campaignReward = data.toString();
            }
        }

        return data;
    }

    public void addCampaignNormal(int mapId, int numAdd) {
        List<Integer> data = getCampaign();
        if (mapId < data.get(0)) return;
        countNormal += numAdd;
        if (countNormal >= numberUpdate) {
            countNormal -= numberUpdate;
            data.set(0, mapId);
            data.set(1, data.get(1) + numAdd);
            updateCampaignNormal(StringHelper.toDBString(data));
        } else {
            data.set(0, mapId);
            data.set(1, data.get(1) + numAdd);
        }
    }

    public void initData(UserEntity user) {
        // check data or end level
        getGoldStatus(user);
        getLevelStatus(user);
    }

    public int getNumSlotItem() {
        return getSlot().get(0);
    }

    public List<Integer> getSlot() {
        List<Integer> slot = GsonUtil.strToListInt(numSlot);
        if (slot.size() < 1) {
            slot.add(CfgBag.config.numSlotItem);
        }
        if (slot.size() < 2) {
            slot.add(CfgBag.config.numSlotEquipment);
        }
        if (slot.size() < 3) {
            slot.add(CfgBag.config.numSlotPiece);
        }
        return slot;
    }

    public List<Integer> getNumCheckin() {
        List<Integer> lstCheckin = GsonUtil.strToListInt(checkIn); //checkIn : month - day - num - status
        while (lstCheckin.size() < 4) {
            lstCheckin.add(0);
        }
        Calendar ca = Calendar.getInstance();
        int month = ca.get(Calendar.MONTH);
        int day = ca.get(Calendar.DAY_OF_MONTH);
        if (lstCheckin.get(CfgCheckin.MONTH) != month) {
            lstCheckin.set(CfgCheckin.MONTH, month);
            lstCheckin.set(CfgCheckin.DAY_CHECKIN, day);
            lstCheckin.set(CfgCheckin.NUM_CHECKIN, 0);
            lstCheckin.set(CfgCheckin.STATUS, 0);
            update(Arrays.asList("check_in", lstCheckin.toString()));
        } else if (lstCheckin.get(CfgCheckin.DAY_CHECKIN) != day) {
            lstCheckin.set(CfgCheckin.DAY_CHECKIN, day);
            lstCheckin.set(CfgCheckin.STATUS, 0);
            update(Arrays.asList("check_in", lstCheckin.toString()));
        }
        return lstCheckin;
    }

    public int getStatusCheckIn() { // 1 : da check in, 0 chua checkin
        return getNumCheckin().get(CfgCheckin.STATUS);
    }

    public int getNumSlotItemEquip() {
        return getSlot().get(1);
    }

    public int getNumSlotPiece() {
        return getSlot().get(2);
    }

    public List<StatEntity> getGoldStatus(UserEntity user) {
        List<StatEntity> stats = new ArrayList<>();
        // id - level
        boolean hasUpdate = false;
        List<Integer> dataGoldStat = GsonUtil.strToListInt(getGoldStat());
        if (dataGoldStat.size() != ResStat.countGoldStat()) {
            if (dataGoldStat.size() == 0) { // tao moi
                // gen new data
                stats = genNewGoldStat();
                hasUpdate = true;
            } else { // check lai data - status
                stats = parseGoldStat(getGoldStat(), user);

                // check so luong co thay doi hay khong
                for (int i = 0; i < ResStat.countGoldStat(); i++) {
                    ResGoldStatEntity resGoldStat = ResStat.aGoldStat.get(i);
                    if (aGoldStat.get(resGoldStat.getId()) == null) { // chua co trong data
                        // tao moi va add vao
                        StatusType type = StatusType.LOCK;
                        if (resGoldStat.hasCondition()) {
                            if (resGoldStat.getConditionKey() == ConditionType.MAX_POINT.value) {
                                StatEntity stat = aGoldStat.get(resGoldStat.getConditionLevel());
                                if (stat != null) {
                                    // check thoa man dieu kien level cua chi so nao do lon hon dieu kien
                                    int levelNow = stat.level;
                                    if (levelNow < resGoldStat.getConditionLevel()) {
                                        type = StatusType.PROCESSING;
                                    }
                                }
                            } else {
                                type = StatusType.PROCESSING;
                            }
                        } else type = StatusType.PROCESSING;

                        StatEntity stt = new StatEntity(resGoldStat.getId(), type);
                        stats.add(stt);
                        dataGoldStat.add(resGoldStat.getId());
                        dataGoldStat.add(1);
                        aGoldStat.put(stt.id, stt);
                        hasUpdate = true;
                    }
                }
            }
        }
        if (hasUpdate) {
            if (!DBJPA.update("user_data", Arrays.asList("gold_stat", StringHelper.toDBString(dataGoldStat)), Arrays.asList("user_id", userId))) {
            }
        }
        return stats;
    }

    public List<StatEntity> getLevelStatus(UserEntity user) {
        List<StatEntity> status = new ArrayList<>();
        // id - level
        boolean hasUpdate = false;
        List<Integer> dataLevelStat = GsonUtil.strToListInt(getLevelStat());
        if (dataLevelStat.size() != ResStat.countLevelStat()) {
            if (dataLevelStat.size() == 0) { // tao moi
                // gen new data
                status = genNewLevelStat();
                hasUpdate = true;
            } else { // check lai data
                status = parseLevelStat(getLevelStat(), user);
                // check so luong co thay doi hay khong
                for (int i = 0; i < ResStat.countLevelStat(); i++) {
                    ResLevelStatEntity retst = ResStat.aLevelStat.get(i);
                    if (aLevelStat.get(retst.getId()) == null) { // chua co trong data
                        // tao moi va add vao
                        StatusType type = StatusType.LOCK;
                        if (retst.hasCondition()) {
                            if (aLevelStat.get(retst.getConditionKey()) != null) {
                                // check thoa man dieu kien level cua chi so nao do lon hon dieu kien
                                int levelNow = aLevelStat.get(retst.getConditionKey()).level;
                                if (levelNow > retst.getConditionLevel()) {
                                    type = StatusType.PROCESSING;
                                }
                            }
                        }
                        StatEntity stt = new StatEntity(retst.getId(), type);
                        status.add(stt);
                        dataLevelStat.add(retst.getId());
                        dataLevelStat.add(1);
                        aLevelStat.put(stt.id, stt);
                        hasUpdate = true;
                    }
                }
            }
        }
        if (hasUpdate) {
            DBJPA.update("user_data", Arrays.asList("level_stat", StringHelper.toDBString(dataLevelStat)), Arrays.asList("user_id", userId));
        }
        return status;
    }

    public int getlvGoldStat(int id) {
        return aGoldStat.get(id).level;
    }

    public int getlvLevelStat(int id) {
        return aLevelStat.get(id).level;
    }

    public void upGoldStat(int id, int level) {
        aGoldStat.get(id).level += level;
    }

    public void upLevelStat(int id, int level) {
        aLevelStat.get(id).level += level;
    }


    List<StatEntity> parseGoldStat(String value, UserEntity user) {
        aGoldStat.clear();
        List<StatEntity> stats = new ArrayList<>();
        List<Integer> goldStatStatus = GsonUtil.strToListInt(value);
        for (int i = 0; i < goldStatStatus.size(); i += 2) {
            StatusType type = StatusType.PROCESSING;
            int id = goldStatStatus.get(i);
            int levelNow = goldStatStatus.get(i + 1);
            if (levelNow >= ResStat.getGoldItem(id).getLevelMax()) {
                type = StatusType.DONE;
            }
            StatEntity stat = new StatEntity(id, type, goldStatStatus.get(i + 1));
            aGoldStat.put(stat.id, stat);
            stats.add(stat);
        }
        // check lai status cua item
        for (int i = 0; i < stats.size(); i++) {
            ResGoldStatEntity res = ResStat.getGoldItem(stats.get(i).id);
            if (res.hasCondition()) {
                // check condition theo tung condition type
                int key = res.getConditionKey();
                if (key == ConditionType.MAX_POINT.value) {
                    int pointId = res.getConditionLevel();
                    ResGoldStatEntity resGold = ResStat.getGoldItem(pointId);
                    int levelNow = aGoldStat.get(pointId).level;
                    if (levelNow < resGold.getLevelMax()) {
                        stats.get(i).status = StatusType.LOCK;
                        aGoldStat.put(stats.get(i).id, stats.get(i));
                    }
                } else if (key == ConditionType.TUTORIAL_LEVEL.value) {
                    if (levelTraining < res.getConditionLevel()) {
                        stats.get(i).status = StatusType.LOCK;
                        aGoldStat.put(stats.get(i).id, stats.get(i));
                    }
                } else if (key == ConditionType.HAS_LEVEL.value) {
                    if (user.getLevel() < res.getConditionLevel()) {
                        stats.get(i).status = StatusType.LOCK;
                        aGoldStat.put(stats.get(i).id, stats.get(i));
                    }
                }
            }
        }
        return stats;
    }

    List<StatEntity> parseLevelStat(String value, UserEntity user) {
        aLevelStat.clear();
        List<StatEntity> stats = new ArrayList<>();
        List<Integer> levelStatStatus = GsonUtil.strToListInt(value);
        for (int i = 0; i < levelStatStatus.size(); i += 2) {
            StatusType type = StatusType.PROCESSING;
            int id = levelStatStatus.get(i);
            int levelNow = levelStatStatus.get(i + 1);
            ResLevelStatEntity resLevel = ResStat.getLevelItem(id);
            if (levelNow >= resLevel.getLevelMax()) {
                type = StatusType.DONE;
            }

            StatEntity stat = new StatEntity(id, type, levelStatStatus.get(i + 1));
            aLevelStat.put(stat.id, stat);
            stats.add(stat);
        }
        // check lai status cua item
        for (int i = 0; i < stats.size(); i++) {
            ResLevelStatEntity res = ResStat.getLevelItem(stats.get(i).id);
            if (res.hasCondition()) {
                // check condition theo tung condition type
                int key = res.getConditionKey();
                if (key == ConditionType.MAX_POINT.value) {
                    int pointId = res.getConditionLevel();
                    ResLevelStatEntity resLevel = ResStat.getLevelItem(pointId);
                    int levelNow = aLevelStat.get(pointId).level;
                    if (levelNow < resLevel.getLevelMax()) {
                        stats.get(i).status = StatusType.LOCK;
                        aLevelStat.put(stats.get(i).id, stats.get(i));
                    }
                } else if (key == ConditionType.TUTORIAL_LEVEL.value) {
                    if (levelTraining < res.getConditionLevel()) {
                        stats.get(i).status = StatusType.LOCK;
                        aLevelStat.put(stats.get(i).id, stats.get(i));
                    }
                } else if (key == ConditionType.HAS_LEVEL.value) {
                    if (user.getLevel() < res.getConditionLevel()) {
                        stats.get(i).status = StatusType.LOCK;
                        aLevelStat.put(stats.get(i).id, stats.get(i));
                    }
                }
            }
        }
        return stats;
    }

    // 0 = gold   ,   1  =level
    public List<Integer> converListStat(int type) {
        List<Integer> lst = new ArrayList<>();
        if (type == 0) {
            aGoldStat.forEach((k, v) -> {
                lst.add(v.id);
                lst.add(v.level);
            });
        } else {
            aLevelStat.forEach((k, v) -> {
                lst.add(v.id);
                lst.add(v.level);
            });
        }
        return lst;
    }

    List<StatEntity> genNewGoldStat() {
        aGoldStat.clear();
        List<StatEntity> lst = new ArrayList<>();
        for (int i = 0; i < ResStat.countGoldStat(); i++) {
            ResGoldStatEntity res = ResStat.aGoldStat.get(i);
            StatEntity stt = new StatEntity(res.getId(), res.hasCondition() ? StatusType.LOCK : StatusType.PROCESSING);
            lst.add(stt);
            aGoldStat.put(stt.id, stt);
            List<Integer> dataGoldStat = GsonUtil.strToListInt(goldStat);
            dataGoldStat.add(res.getId());
            dataGoldStat.add(0);
            goldStat = dataGoldStat.toString();
        }
        return lst;
    }

    List<StatEntity> genNewLevelStat() {
        aLevelStat.clear();
        List<StatEntity> lst = new ArrayList<>();
        for (int i = 0; i < ResStat.countLevelStat(); i++) {
            ResLevelStatEntity res = ResStat.aLevelStat.get(i);
            StatEntity stt = new StatEntity(res.getId(), res.hasCondition() ? StatusType.LOCK : StatusType.PROCESSING);
            lst.add(stt);
            aLevelStat.put(stt.id, stt);
            List<Integer> dataLevelStat = GsonUtil.strToListInt(levelStat);
            dataLevelStat.add(res.getId());
            dataLevelStat.add(0);
            levelStat = dataLevelStat.toString();
        }
        return lst;
    }

    public Pbmethod.PbListStat.Builder pbGoldStat() {
        Pbmethod.PbListStat.Builder pb = Pbmethod.PbListStat.newBuilder();
        aGoldStat.forEach((k, v) -> {
            Pbmethod.PbStat.Builder stat = Pbmethod.PbStat.newBuilder();
            int idStat = v.id;
            stat.setId(v.id);
            stat.setStatus(v.status.value);
            stat.setLevel(v.level);
            ResGoldStatEntity res = ResStat.getGoldItem(idStat);
            stat.addAllCondition(res.getListConditions());
            stat.addAllFormula(res.getAFormular());
            stat.setMaxLevel(res.getLevelMax());
            stat.setPointPer((long) (res.getPointPerLevel() * 100));
            pb.addAStat(stat);
        });
        return pb;
    }

    public Pbmethod.PbListStat.Builder pbLevelStat() {
        Pbmethod.PbListStat.Builder pb = Pbmethod.PbListStat.newBuilder();
        aLevelStat.forEach((k, v) -> {
            Pbmethod.PbStat.Builder stat = Pbmethod.PbStat.newBuilder();
            int idStat = v.id;
            stat.setId(idStat);
            stat.setStatus(v.status.value);
            stat.setLevel(v.level);
            ResLevelStatEntity res = ResStat.getLevelItem(idStat);
            stat.addAllCondition(res.getListConditions());
            stat.setMaxLevel(res.getLevelMax());
            stat.setPointPer((long) (res.getPointPerLevel() * 100));
            pb.addAStat(stat);
        });
        return pb;
    }

    public Pbmethod.PbUserData toProto(MyUser mUser) {
        Pbmethod.PbUserData.Builder pb = Pbmethod.PbUserData.newBuilder();
        pb.setLvGachaWeapon(levelGachaWeapon);
        pb.setLvGachaPet(levelGachaPet);
        pb.setLvTraining(levelTraining);
        pb.setStone(numStone);
        pb.setStoneVip(numStoneVip);
        pb.setMaxlvTraining(maxLvTraining);
        pb.setNumPointLevel(numPointLevel);
        pb.setTutorial(tutorial);
        pb.setDameSkinEquip(dameSkinEquip);
        pb.addAllDameSkins(getListDameSkin());
        pb.setChatFrameEquip(chatFrameEquip);
        pb.addAllChatFrames(getListChatFrame());
        pb.setTrialEquip(trialEquip);
        pb.addAllTrials(getListIntTrial());
        pb.addAllBossGod(getBossGod());
        // item
        Pbmethod.PbListItem.Builder lstItem = Pbmethod.PbListItem.newBuilder();
        for (Map.Entry<Integer, UserItemEntity> item : mUser.getResources().getMItem().entrySet()) {
            Pbmethod.PbItem.Builder itemPb = item.getValue().toProto();
            if (itemPb != null) {
                lstItem.addItem(itemPb);
            }
        }
        pb.setItems(lstItem);
        Point basePoint = IMath.calculatePoint(mUser, false);
        // list hero
        for (Map.Entry<Integer, UserHeroEntity> heroes : mUser.getResources().getMHero().entrySet()) {
            // cal point hero
            heroes.getValue().calPointHero(mUser, basePoint.cloneInstance());
            List<Integer> items = heroes.getValue().getListIdEquipmentEquip();
            for (int i = 0; i < items.size(); i++) {
                UserItemEquipmentEntity uItem = mUser.getResources().getItemEquipment(items.get(i));
                if (uItem != null) uItem.setHeroIdEquip(heroes.getValue().heroId);
            }

            pb.addAHero(heroes.getValue().toProto());
        }

        // item equipment
        Pbmethod.PbListItemEquipment.Builder lstItemE = Pbmethod.PbListItemEquipment.newBuilder();
        for (Map.Entry<Long, UserItemEquipmentEntity> itemEq : mUser.getResources().getMItemEquipment().entrySet()) {
            Pbmethod.PbItemEquipment.Builder itemEquip = itemEq.getValue().toProto();
            if (itemEq.getValue().isForever() || itemEq.getValue().hasExpire()) {// hết hạn
                lstItemE.addItemEquip(itemEquip);
            }
        }
        pb.setItemEquipments(lstItemE);

        // item seed
        for (Map.Entry<Integer, UserItemFarmEntity> itemFarm : mUser.getResources().getMItemFarmSeed().entrySet()) {
            if (itemFarm.getValue().getNumber() > 0) pb.addAItemFarm(itemFarm.getValue().toProto());
        }

        // item farm food
        for (Map.Entry<Integer, UserItemFarmEntity> itemFarm : mUser.getResources().getMItemFarmFood().entrySet()) {
            if (itemFarm.getValue().getNumber() > 0) pb.addAItemFarm(itemFarm.getValue().toProto());
        }

        // item agri
        for (Map.Entry<Integer, UserItemFarmEntity> itemFarm : mUser.getResources().getMItemFarmAgri().entrySet()) {
            if (itemFarm.getValue().getNumber() > 0) pb.addAItemFarm(itemFarm.getValue().toProto());
        }

        // item farm tool
        for (Map.Entry<Integer, UserItemFarmEntity> itemFarm : mUser.getResources().getMItemFarmTool().entrySet()) {
            if (itemFarm.getValue().getNumber() > 0) pb.addAItemFarm(itemFarm.getValue().toProto());
        }

        // item piece weapon
        for (Map.Entry<Integer, UserPieceEntity> piece : mUser.getResources().getMPieceWeapon().entrySet()) {
            if (piece.getValue().getNumber() > 0) pb.addAItemPiece(piece.getValue().toProto());
        }
        // item piece animal
        for (Map.Entry<Integer, UserPieceEntity> piece : mUser.getResources().getMPiecePet().entrySet()) {
            if (piece.getValue().getNumber() > 0) pb.addAItemPiece(piece.getValue().toProto());
        }
        // item piece monster
        for (Map.Entry<Integer, UserPieceEntity> piece : mUser.getResources().getMPieceMonster().entrySet()) {
            if (piece.getValue().getNumber() > 0) pb.addAItemPiece(piece.getValue().toProto());
        }

        // pet monster
        for (Map.Entry<Integer, UserPetEntity> pets : mUser.getResources().getMPetMonster().entrySet()) {
            pb.addAPet(pets.getValue().toProto());
        }

        // pet animal
        for (Map.Entry<Integer, UserPetEntity> pets : mUser.getResources().getMPetAnimal().entrySet()) {
            pb.addAPet(pets.getValue().toProto());
        }

        // item equipment
        List<Integer> lstEquip = mUser.getUser().getListIdEquipmentEquip();
        for (int i = 0; i < lstEquip.size(); i++) {
            if (lstEquip.get(i) > 0) {
                UserItemEquipmentEntity iEquip = mUser.getResources().getItemEquipment(lstEquip.get(i));
                if (iEquip != null && (iEquip.hasExpire() || iEquip.isForever())) {
                    pb.addAItemEquip(iEquip.toProto());
                }
            }
        }

        return pb.build();
    }


    //region db
    public boolean updateGoldStat(int id, int levelNext) {
        upGoldStat(id, levelNext);
        String data = StringHelper.toDBString(converListStat(0));
        if (DBJPA.update("user_data", Arrays.asList("gold_stat", data), Arrays.asList("user_id", userId))) {
            this.goldStat = data;
            return true;
        }
        return false;
    }

    public boolean updateLevelStat(int id, int levelNext) {
        upLevelStat(id, levelNext);
        String data = StringHelper.toDBString(converListStat(1));
        if (DBJPA.update("user_data", Arrays.asList("level_stat", data, "num_point_level", numPointLevel - levelNext), Arrays.asList("user_id", userId))) {
            this.levelStat = data;
            this.numPointLevel -= levelNext;
            return true;
        }
        return false;
    }

    public boolean updateSlot(int type, int number) {
        List<Integer> slot = getSlot();
        slot.set(type - 1, number);
        if (DBJPA.update("user_data", Arrays.asList("num_slot", StringHelper.toDBString(slot)), Arrays.asList("user_id", userId))) {
            numSlot = slot.toString();
            return true;
        }
        return false;
    }

    public boolean updateCheckIn(String checkinData) {
        if (update(Arrays.asList("check_in", checkinData))) {
            checkIn = checkinData;
            return true;
        }
        return false;
    }

    public boolean update(List<Object> updateData) {
        return DBJPA.update("user_data", updateData, Arrays.asList("user_id", userId));
    }

    public boolean updateTreeData(List<Long> dataTree) {
        if (update(List.of("farm_tree", StringHelper.toDBString(dataTree)))) {
            this.farmTree = dataTree.toString();
            return true;
        }
        return false;
    }

    public boolean updateFarmDeco(List<Integer> dataDeco, List<Integer> farmPoint) {
        if (update(List.of("farm_deco", StringHelper.toDBString(dataDeco), "farm_point", StringHelper.toDBString(farmPoint)))) {
            this.farmDeco = dataDeco.toString();
            this.farmPoint = farmPoint.toString();
            return true;
        }
        return false;
    }

    public boolean resetLevelStat(MyUser mUser) {
        int point = CfgUser.pointPerLevel * (mUser.getUser().getLevel() - 1);
        if (update(List.of("level_stat", "[]", "num_point_level", point))) {
            this.levelStat = "[]";
            this.numPointLevel = point;
            getLevelStatus(mUser.getUser());
            mUser.reCalculatePoint();
            mUser.getPlayer().protoStatus(StateType.UPDATE_NUM_POINT_LEVEL_STAT, (long) point);
            return true;
        }
        return false;
    }

    public List<Long> resetGoldStat(MyUser mUser) {
        if (update(List.of("gold_stat", "[]"))) {
            long energy = 0;
            // reset item
            for (var entry : aGoldStat.entrySet()) {
                energy += ResStat.countEnergyUpgrade(entry.getKey(), entry.getValue().level);
            }
            this.goldStat = "[]";
            getGoldStatus(mUser.getUser());
            mUser.reCalculatePoint();
            return Bonus.viewItem(ItemKey.NANG_LUONG, energy);
        }
        return null;
    }

    public boolean updateComboWeapon(String lvComboWeapon) {
        return update(Arrays.asList("lv_combo_weapon", lvComboWeapon));
    }

    public void increNumPointUpLV(MyUser mUser, int numUp) {
        if (update(Arrays.asList("num_point_level", numPointLevel += CfgUser.pointPerLevel * numUp))) {
            Util.sendProtoData(mUser.getPlayer().getMUser().getChannel(), CommonProto.getCommonVector(numPointLevel), IAction.UPDATE_NUM_POINT_LEVEL);
        }
    }

    public boolean updateTutorialQuest() {
        if (update(List.of("quest_tutorial", questTutorial + 1, "quest_tutorial_number", 0))) {
            this.questTutorial++;
            this.questTutorialNumber = 0;
            return true;
        }
        return false;
    }

    public boolean updateTutQuestNumber(int data) {
        if (update(List.of("quest_tutorial_number", data))) {
            this.questTutorialNumber = data;
            return true;
        }
        return false;
    }

    public boolean updateCampaignNormal(String data) {
        if (update(Arrays.asList("campaign", data))) {
            campaign = data;
            return true;
        }
        return false;
    }

    //endreion
}
