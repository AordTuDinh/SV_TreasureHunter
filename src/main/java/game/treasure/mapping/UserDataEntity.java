package game.treasure.mapping;

import game.config.CfgCheckin;
import game.config.CfgUser;
import game.config.aEnum.*;
import game.object.MyUser;
import game.treasure.BattleConfig;
import game.treasure.controller.AHandler;
import game.treasure.controller.UserHandler;
import game.treasure.mapping.main.*;
import game.treasure.server.IAction;
import game.treasure.service.resource.ResAvatar;
import game.treasure.service.resource.ResQuest;
import game.treasure.service.user.EquipSlotBonus;
import game.treasure.service.user.ItemSlotHelper;
import game.treasure.service.user.Bonus;
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
    String itemSlot;
    String buff; // [pointId, value, time end]
    String dameSkin, chatFrame, listTrial;
    int dameSkinEquip, chatFrameEquip, trialEquip, effInit, craftLevel, craftExp;
    long timeProtected;
    long timeActiveArtifact;
    /**
     * Vị trí HOME lần logout gần nhất — JSON [x, y].
     */
    String lastPos;
    /**
     * 1 = logout lúc đang chết trên HOME.
     */
    int lastDead;

    @Transient
    int maxLvTraining = 60;
    @Transient
    UserInt uInt;
    @Transient
    long lastClanChat;
    /**
     * item_slot đổi in-memory; flush khi logout hoặc piggyback uData.update().
     */
    @Transient
    boolean itemSlotDirty;

    public UserDataEntity(int userId) {
        this.userId = userId;
        this.effInit = 0;
        this.numSlot = CfgUser.getSlotBagInit();    // [slotBagUI, material, event]
        this.dameSkin = "[0]";
        this.listTrial = "[0]";
        this.chatFrame = "[0]";
        this.checkIn = "[]";
        this.tutorial = 0;
        this.questTutorialNumber = 0;
        this.questTutorial = 1;
        this.chatFrameEquip = 0;
        this.dameSkinEquip = 0;
        this.craftLevel = 1;
        this.trialEquip = 0;
        this.craftExp = 0;
        this.buff = "[]";
        this.itemSlot = buildEmptyItemSlot(CfgUser.getSlotBagInit());
        this.lastPos = "[0,0]";
        this.lastDead = 0;
    }

    static String buildEmptyItemSlot(int bagCount) {
        List<Long> slots = new ArrayList<>();
        ItemSlotHelper.ensureBagCapacity(slots, bagCount);
        return ItemSlotHelper.serialize(slots);
    }

    static String buildEmptyItemSlot(String numSlotJson) {
        List<Integer> caps = GsonUtil.strToListInt(numSlotJson);
        while (caps.size() < 3) caps.add(8);
        return buildEmptyItemSlot(caps.get(0));
    }

    /**
     * Khởi tạo item_slot rỗng theo số ô bag/event hiện tại của user.
     */
    public void ensureItemSlotInitialized() {
        if (itemSlot != null && !itemSlot.isEmpty())
            return;
        itemSlot = buildEmptyItemSlot(getSlotBagUI());
    }

    public List<Long> getItemSlotList() {
        List<Long> slots = ItemSlotHelper.parse(itemSlot);
        ItemSlotHelper.ensureBagCapacity(slots, getSlotBagUI());
        return slots;
    }

    /**
     * Ghi item_slot in-memory; DB flush qua {@link #flushItemSlotIfDirty()} hoặc {@link #update(List)}.
     */
    public boolean saveItemSlot(List<Long> slots) {
        itemSlot = ItemSlotHelper.serialize(slots);
        itemSlotDirty = true;
        return true;
    }

    public boolean flushItemSlotIfDirty() {
        if (!itemSlotDirty)
            return true;
        if (DBJPA.update("user_data", Arrays.asList("item_slot", itemSlot), Arrays.asList("user_id", userId))) {
            itemSlotDirty = false;
            return true;
        }
        return false;
    }

    static boolean containsUpdateField(List<Object> data, String field) {
        for (int i = 0; i + 1 < data.size(); i += 2) {
            if (field.equals(String.valueOf(data.get(i))))
                return true;
        }
        return false;
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


    public List<Long> getBuffTriplets() {
        return game.treasure.service.user.UserBuff.parseTriplets(buff);
    }

    public List<Integer> getSlot() {
        List<Integer> slot = GsonUtil.strToListInt(numSlot);
        while (slot.size() < 5) {
            if (slot.size() == 3) slot.add(CfgUser.getDefaultSlotTrading1());
            else slot.add(CfgUser.getDefaultSlotTrading2());
        }
        return slot;
    }

    public int getSlotTrading1() {
        return getSlot().get(3);
    }

    public int getSlotTrading2() {
        return getSlot().get(4);
    }

    public boolean saveSlotTrading(int tab, int unlocked) {
        List<Integer> slot = getSlot();
        if (tab == game.config.CfgTrading.TAB_ITEM)
            slot.set(3, unlocked);
        else
            slot.set(4, unlocked);
        numSlot = StringHelper.toDBString(slot);
        return update(Arrays.asList("num_slot", numSlot));
    }

    /**
     * Số ô slotBagUI — gộp item type 1 (consumable) + type 2 (equipment chưa mặc).
     * {@code num_slot[0]} lưu tổng (base + bonus trang bị point 23).
     */
    public int getSlotBagUI() {
        return getSlot().get(0);
    }

    /**
     * Số ô material — {@code num_slot[1]} lưu tổng (base + bonus trang bị point 24).
     */
    public int getSlotMaterial() {
        return getSlot().get(1);
    }

    /**
     * Login: chuẩn hóa {@code num_slot} — tách base từ giá trị cũ (nếu đã cộng bonus) rồi cộng lại bonus hiện tại.
     */
    public boolean reconcileSlotsOnLogin(MyUser mUser) {
        return applySlotTotals(mUser, true);
    }

    /**
     * Sau equip / unequip / bán / nâng cấp trang bị đang mặc — cập nhật {@code num_slot} theo bonus mới.
     */
    public boolean syncSlotsAfterEquipmentChange(MyUser mUser, int oldBonusBag, int oldBonusMat) {
        return applySlotTotals(mUser, false, oldBonusBag, oldBonusMat);
    }

    boolean applySlotTotals(MyUser mUser, boolean loginReconcile) {
        return applySlotTotals(mUser, loginReconcile, EquipSlotBonus.bagBonus(mUser), EquipSlotBonus.materialBonus(mUser));
    }

    boolean applySlotTotals(MyUser mUser, boolean loginReconcile, int oldBonusBag, int oldBonusMat) {
        int newBonusBag = EquipSlotBonus.bagBonus(mUser);
        int newBonusMat = EquipSlotBonus.materialBonus(mUser);
        List<Integer> slot = getSlot();
        int minBag = CfgUser.getFreeSlotBag();
        int minMat = CfgUser.getFreeSlotMaterial();

        int baseBag;
        int baseMat;
        if (loginReconcile) {
            baseBag = slot.get(0);
            if (newBonusBag > 0 && baseBag - newBonusBag >= minBag)
                baseBag -= newBonusBag;
            baseMat = slot.get(1);
            if (newBonusMat > 0 && baseMat - newBonusMat >= minMat)
                baseMat -= newBonusMat;
        } else {
            baseBag = slot.get(0) - oldBonusBag;
            baseMat = slot.get(1) - oldBonusMat;
        }

        int newBag = Math.max(baseBag + newBonusBag, minBag);
        int newMat = Math.max(baseMat + newBonusMat, minMat);
        if (newBag == slot.get(0) && newMat == slot.get(1))
            return false;

        slot.set(0, newBag);
        slot.set(1, newMat);
        numSlot = StringHelper.toDBString(slot);
        resizeItemSlotForBagCount(newBag);
        List<Object> fields = new ArrayList<>(Arrays.asList("num_slot", numSlot));
        if (itemSlotDirty)
            fields.addAll(Arrays.asList("item_slot", itemSlot));
        if (!update(fields))
            return false;
        itemSlotDirty = false;
        if (mUser != null && mUser.getResources() != null)
            mUser.getResources().notifyBagSlotCountChanged();
        return true;
    }

    void resizeItemSlotForBagCount(int bagCount) {
        List<Long> slots = ItemSlotHelper.parse(itemSlot);
        ItemSlotHelper.ensureBagCapacity(slots, bagCount);
        itemSlot = ItemSlotHelper.serialize(slots);
        itemSlotDirty = true;
    }

    /**
     * Số ô item sự kiện — user_item type 4.
     */
    public int getSlotEvent() {
        return getSlot().get(2);
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


    public UserInt getUInt() {
        if (uInt == null) {
            uInt = new UserInt(dataInt, userId);
        }
        return uInt;
    }

    /**
     * checkIn: month - day - num - status
     */
    public List<Integer> getNumCheckin() {
        return getNumCheckin(null);
    }

    public List<Integer> getNumCheckin(MyUser mUser) {
        List<Integer> lstCheckin = GsonUtil.strToListInt(checkIn);
        while (lstCheckin.size() < 4) {
            lstCheckin.add(0);
        }
        // Bỏ field vip cũ nếu còn trong DB
        if (lstCheckin.size() > 4) {
            lstCheckin = new ArrayList<>(lstCheckin.subList(0, 4));
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
            clearCheckinVipPack(mUser);
        } else if (lstCheckin.get(CfgCheckin.DAY_CHECKIN) != day) {
            lstCheckin.set(CfgCheckin.DAY_CHECKIN, day);
            lstCheckin.set(CfgCheckin.STATUS, 0);
            update(Arrays.asList("check_in", lstCheckin.toString()));
        }
        return lstCheckin;
    }

    private void clearCheckinVipPack(MyUser mUser) {
        if (mUser == null || mUser.getResources() == null)
            return;
        UserPackEntity pack = mUser.getResources().getPack(CfgCheckin.PACK_CHECKIN_VIP);
        if (pack == null)
            return;
        DBJPA.delete("user_pack", "user_id", userId, "pack_id", CfgCheckin.PACK_CHECKIN_VIP);
        mUser.getResources().removePack(CfgCheckin.PACK_CHECKIN_VIP);
    }

    public int getStatusCheckIn() { // 1 : da check in, 0 chua checkin
        return getNumCheckin().get(CfgCheckin.STATUS);
    }

    public boolean hasCheckinVipPack(MyUser mUser) {
        if (mUser == null || mUser.getResources() == null)
            return false;
        UserPackEntity pack = mUser.getResources().getPack(CfgCheckin.PACK_CHECKIN_VIP);
        if (pack == null)
            return false;
        if (!pack.hasHSD()) {
            mUser.getResources().removePack(CfgCheckin.PACK_CHECKIN_VIP);
            return false;
        }
        return true;
    }

    public Pbmethod.PbUserData toProto(MyUser mUser) {
        List<Integer> slots = getSlot();
        Pbmethod.PbUserData.Builder pb = Pbmethod.PbUserData.newBuilder();
        pb.setSlogBagUI(slots.get(0));
        pb.setSlotMaterial(slots.get(1));
        pb.setSlotItemEvent(slots.get(2));
        pb.setLvTraining(levelTraining);
        pb.setMaxlvTraining(maxLvTraining);
        pb.setTutorial(tutorial);
        pb.setDameSkinEquip(dameSkinEquip);
        pb.addAllDameSkins(getListDameSkin());
        pb.setChatFrameEquip(chatFrameEquip);
        pb.addAllChatFrames(getListChatFrame());
        pb.setTrialEquip(trialEquip);
        pb.setCraftLevel(craftLevel);
        pb.setCraftExp(craftExp);
        pb.addAllTrials(getListIntTrial());

        Pbmethod.PbListItem.Builder lstItem = Pbmethod.PbListItem.newBuilder();
        for (Map.Entry<Long, UserItemEntity> item : mUser.getResources().getMItem().entrySet()) {
            UserItemEntity uItem = item.getValue();
            Pbmethod.PbItem itemPb = uItem.toProtoWire();
            if (itemPb != null) {
                lstItem.addItem(itemPb);
            }
        }
        pb.setItems(lstItem);

        Pbmethod.PbListEquipment.Builder lstEquip = Pbmethod.PbListEquipment.newBuilder();
        for (Map.Entry<Long, UserEquipmentEntity> eq : mUser.getResources().getMEquipment().entrySet()) {
            Pbmethod.PbEquipment eqPb = eq.getValue().toProtoWire();
            if (eqPb != null)
                lstEquip.addEquipment(eqPb);
        }
        pb.setEquipments(lstEquip);
        pb.addAllItemSlot(getItemSlotList());
        // material
        Pbmethod.PbListMaterial.Builder lstMat = Pbmethod.PbListMaterial.newBuilder();
        for (Map.Entry<Long, UserMaterialEntity> mat : mUser.getResources().getMMaterial().entrySet()) {
            Pbmethod.PbMaterial matPb = mat.getValue().toProtoWire();
            if (matPb != null) lstMat.addMaterials(matPb);
        }
        pb.setAMaterial(lstMat);
        // artifact
        Pbmethod.PbListArtifact.Builder lstArtifact = Pbmethod.PbListArtifact.newBuilder();
        for (Map.Entry<Long, UserArtifactEntity> artifact : mUser.getResources().getMArtifact().entrySet()) {
            Pbmethod.PbArtifact artifactPb = artifact.getValue().toProtoWire();
            if (artifactPb != null)
                lstArtifact.addArtifacts(artifactPb);
        }
        pb.setAArtifact(lstArtifact);
        // pet
        for (Map.Entry<Long, UserPetEntity> pets : mUser.getResources().getMPet().entrySet()) {
            pb.addAPet(pets.getValue().toProto());
        }

        // character skin
        for (Map.Entry<Long, UserSkinEntity> skin : mUser.getResources().getMSkin().entrySet()) {
            pb.addASkin(skin.getValue().toProto());
        }

        // mount
        for (Map.Entry<Long, UserMountEntity> mounts : mUser.getResources().getMMount().entrySet()) {
            pb.addAMount(mounts.getValue().toProto());
        }

        // mob
        for (Map.Entry<Long, UserMobEntity> mobs : mUser.getResources().getMMob().entrySet()) {
            pb.addAMob(mobs.getValue().toProto());
        }

        // item equipment in hero
        List<Integer> lstEquipId = mUser.getUser().getListIdEquipmentEquip();
        for (int i = 0; i < lstEquipId.size(); i++) {
            if (lstEquipId.get(i) > 0) {
                UserEquipmentEntity iEquip = mUser.getResources().getEquipment(lstEquipId.get(i));
                if (iEquip != null) {
                    pb.addItemEquipments(iEquip.getId());
                }
            }
        }
        pb.setTimeProtected(BattleConfig.toWireProtectedMs(timeProtected));
        pb.setTimeActiveArtifact(timeActiveArtifact);

        Pbmethod.PbListItemPoint.Builder lstItemPoint = Pbmethod.PbListItemPoint.newBuilder();
        for (Map.Entry<Integer, UserItemPointEntity> entry : mUser.getResources().getMItemPoint().entrySet()) {
            lstItemPoint.addItemPoints(entry.getValue().toProto());
        }
        if (lstItemPoint.getItemPointsCount() > 0)
            pb.setAItemPoint(lstItemPoint);

        try {
            byte[] bytes = pb.build().toByteArray();
            bytes = game.treasure.service.item.ProtoUserDataWire.appendSlotTrading(bytes,
                    getSlotTrading1(), getSlotTrading2());
            return Pbmethod.PbUserData.parseFrom(bytes);
        } catch (Exception ex) {
            return pb.build();
        }
    }

    public boolean updateCheckIn(String checkinData) {
        if (update(Arrays.asList("check_in", checkinData))) {
            checkIn = checkinData;
            return true;
        }
        return false;
    }

    public boolean update(List<Object> updateData) {
        List<Object> data = new ArrayList<>(updateData);
        boolean explicitItemSlot = containsUpdateField(updateData, "item_slot");
        boolean piggybackSlot = itemSlotDirty && !explicitItemSlot;
        if (piggybackSlot) {
            data.add("item_slot");
            data.add(itemSlot);
        }
        if (!DBJPA.update("user_data", data, Arrays.asList("user_id", userId)))
            return false;
        if (piggybackSlot || explicitItemSlot)
            itemSlotDirty = false;
        return true;
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
