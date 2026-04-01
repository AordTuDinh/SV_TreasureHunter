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
    String numSlot, dataInt, checkIn;
    String bossGod; // level 5 con boss mình đánh đc
    String campaign;//[id - number]
    String campaignReward;//[0-1] theo id
    String buff; // [time25,time50,timex2 ...] x9
    String farmPoint, dameSkin, chatFrame, listTrial;
    int dameSkinEquip, chatFrameEquip, trialEquip,effInit;

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
        this.campaign = "[1,0]";
        this.campaignReward = "[]";
        this.dameSkin = "[0]";
        this.listTrial = "[0]";
        this.chatFrame = "[0]";
        this.bossGod = NumberUtil.genListInt(5, 0).toString();
        this.checkIn = "[]";
        this.farmPoint = NumberUtil.genListStringInt(3, 0);// time - per - exp
        this.tutorial = 0;
        this.questTutorialNumber = 0;
        this.questTutorial = 1;
        this.chatFrameEquip = 0;
        this.dameSkinEquip = 0;
        this.trialEquip = 0;
        this.buff = NumberUtil.genListStringInt(9, 0);
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




    public UserInt getUInt() {
        if (uInt == null) {
            uInt = new UserInt(dataInt, userId);
        }
        return uInt;
    }

    public List<Integer> getCampaign() {
        if (dataCampaign == null) dataCampaign = GsonUtil.strToListInt(campaign);
        return dataCampaign == null ? List.of(1, 0) : dataCampaign;
    }

//    public List<Integer> getCampaignReward() {
//        List<Integer> data = GsonUtil.strToListInt(campaignReward);
//        boolean update = false;
//        while (data.size() < ResMap.maxMapCampaign) {
//            data.add(0);
//            update = true;
//        }
//        if (update) {
//            if (update(List.of("campaign_reward", StringHelper.toDBString(data)))) {
//                campaignReward = data.toString();
//            }
//        }
//
//        return data;
//    }

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
        // item equipment
        Pbmethod.PbListItemEquipment.Builder lstItemE = Pbmethod.PbListItemEquipment.newBuilder();
        for (Map.Entry<Long, UserItemEquipmentEntity> itemEq : mUser.getResources().getMItemEquipment().entrySet()) {
            Pbmethod.PbItemEquipment.Builder itemEquip = itemEq.getValue().toProto();
            if (itemEq.getValue().isForever() || itemEq.getValue().hasExpire()) {// hết hạn
                lstItemE.addItemEquip(itemEquip);
            }
        }
        pb.setItemEquipments(lstItemE);
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
