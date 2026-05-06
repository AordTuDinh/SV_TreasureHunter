package game.treasure.mapping;

import game.config.CfgCheckin;
import game.config.aEnum.*;
import game.treasure.controller.AHandler;
import game.treasure.controller.UserHandler;
import game.treasure.mapping.main.*;
import game.treasure.server.IAction;
import game.treasure.service.resource.*;
import game.object.MyUser;
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
    int levelTraining, friendNotify, tutorial, questTutorial, questTutorialNumber;
    String numSlot, dataInt, checkIn;
    String buff; // [time25,time50,timex2 ...] x9
    String dameSkin, chatFrame, listTrial;
    int dameSkinEquip, chatFrameEquip, trialEquip, effInit;

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
        this.effInit = 0;
        this.numSlot = "[10,10,10]"; // item - item Equipment - artifact
        this.dameSkin = "[0]";
        this.listTrial = "[0]";
        this.chatFrame = "[0]";
        this.checkIn = "[]";
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

    public int getNumSlot() {
        return 10;
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


    public UserInt getUInt() {
        if (uInt == null) {
            uInt = new UserInt(dataInt, userId);
        }
        return uInt;
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

    public Pbmethod.PbUserData toProto(MyUser mUser) {
        Pbmethod.PbUserData.Builder pb = Pbmethod.PbUserData.newBuilder();
        pb.setLvTraining(levelTraining);
        pb.setMaxlvTraining(maxLvTraining);
        pb.setTutorial(tutorial);
        pb.setDameSkinEquip(dameSkinEquip);
        pb.addAllDameSkins(getListDameSkin());
        pb.setChatFrameEquip(chatFrameEquip);
        pb.addAllChatFrames(getListChatFrame());
        pb.setTrialEquip(trialEquip);
        pb.addAllTrials(getListIntTrial());
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

    //endreion
}
