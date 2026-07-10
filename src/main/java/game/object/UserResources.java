package game.object;

import game.config.CfgAchievement;
import game.config.aEnum.*;
import game.treasure.mapping.*;
import game.protocol.CommonProto;
import game.treasure.service.user.Bonus;
import game.treasure.service.user.ItemSlotHelper;
import protocol.Pbmethod;
import lombok.Getter;
import lombok.Setter;
import ozudo.base.log.Logs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class UserResources implements Serializable {
    MyUser mUser;
    @Setter
    List<UserItemEntity> items;
    @Setter
    List<UserEquipmentEntity> equipments;
    @Setter
    List<UserPetEntity> pets;
    @Setter
    List<UserArtifactEntity> artifacts;
    @Setter
    List<UserMountEntity> mounts;
    @Setter
    List<UserMobEntity> mobs;
    @Setter
    List<UserPackEntity> packs;
    @Setter
    List<UserMaterialEntity> materials;
    @Setter
    List<UserSkinEntity> skins;
    @Setter
    List<UserItemPointEntity> itemPoints;

    @Getter
    Map<Long, UserItemEntity> mItem = new HashMap<>();
    @Getter
    Map<Long, UserEquipmentEntity> mEquipment = new HashMap<>();
    @Getter
    Map<Long, UserPetEntity> mPet = new HashMap<>();
    @Getter
    Map<Long, UserArtifactEntity> mArtifact = new HashMap<>();
    @Getter
    Map<Long, UserMountEntity> mMount = new HashMap<>();
    @Getter
    Map<Long, UserMobEntity> mMob = new HashMap<>();
    @Getter
    Map<Integer, UserPackEntity> mPacks = new HashMap<>();
    @Getter
    Map<Integer, Integer> mWeaponByRank = new HashMap<>();
    @Getter
    Map<Long, UserMaterialEntity> mMaterial = new HashMap<>();
    @Getter
    Map<Long, UserSkinEntity> mSkin = new HashMap<>();
    @Getter
    Map<Integer, UserItemPointEntity> mItemPoint = new HashMap<>();

    public UserResources(MyUser mUser) {
        this.mUser = mUser;
    }

    void syncEquipFlagsFromUser() {
        Set<Integer> equippedIds = new HashSet<>(mUser.getUser().getListIdEquipmentEquip());
        for (UserEquipmentEntity item : mEquipment.values()) {
            boolean equipped = equippedIds.contains((int) item.getId());
            item.setEquip(equipped);
            if (equipped)
                item.setBagSlot(-1);
        }
        for (UserPetEntity pet : mPet.values()) {
            pet.syncEquipFlag(mUser);
        }
        for (UserMountEntity mount : mMount.values()) {
            mount.syncEquipFlag(mUser);
        }
    }

    void applyItemSlotsFromUserData() {
        List<Long> slots = mUser.getUData().getItemSlotList();
        int bagCount = mUser.getUData().getSlotBagUI();
        for (UserItemEntity item : mItem.values()) {
            item.setBagSlot(-1);
            if (!Bonus.usesItemSlotForUserItem(Pbmethod.ItemType.valueOf(item.getType())))
                continue;
            Integer bagSlot = ItemSlotHelper.findSlotOf(slots, 0, bagCount, Bonus.BONUS_ITEM, item.getId());
            if (bagSlot != null)
                item.setBagSlot(bagSlot);
        }
        for (UserEquipmentEntity equip : mEquipment.values()) {
            if (equip.isEquip()) {
                equip.setBagSlot(-1);
                continue;
            }
            Integer bagSlot = ItemSlotHelper.findSlotOf(slots, 0, bagCount, Bonus.BONUS_EQUIPMENT, equip.getId());
            equip.setBagSlot(bagSlot != null ? bagSlot : -1);
        }
        for (UserArtifactEntity artifact : mArtifact.values()) {
            artifact.setBagSlot(-1);
            Integer bagSlot = ItemSlotHelper.findSlotOf(slots, 0, bagCount, Bonus.BONUS_ARTIFACT, artifact.getId());
            if (bagSlot != null)
                artifact.setBagSlot(bagSlot);
        }
    }

    public boolean isOk() {
        try {
            if (items != null) {
                items.stream().filter(i -> !i.isEquipment()).forEach(item -> mItem.put(item.getId(), item));
            }
            if (equipments != null) {
                equipments.forEach(eq -> mEquipment.put(eq.getId(), eq));
            }
            if (packs != null) {
                packs.forEach(pack -> {
                    if (pack.hasHSD() && !mPacks.containsKey(pack.getPackId())) {
                        mPacks.put(pack.getPackId(), pack);
                    }
                });
            }
            if (pets != null) {
                pets.forEach(pet -> mPet.put(pet.getId(), pet));
            }
            if (artifacts != null) {
                artifacts.forEach(item -> mArtifact.put(item.getId(), item));
            }
            if (mounts != null) {
                mounts.forEach(item -> mMount.put(item.getId(), item));
            }
            if (mobs != null) {
                mobs.forEach(mob -> mMob.put(mob.getId(), mob));
            }
            if (materials != null) {
                materials.forEach(mat -> mMaterial.put(mat.getId(), mat));
            }
            if (skins != null) {
                skins.forEach(skin -> mSkin.put(skin.getId(), skin));
            }
            if (itemPoints != null) {
                itemPoints.forEach(row -> mItemPoint.put(row.getPointId(), row));
            }
            syncEquipFlagsFromUser();
            Bonus.verifyItemSlots(mUser, true);
            applyItemSlotsFromUserData();
            mUser.getUData().flushItemSlotIfDirty();
            return true;
        } catch (Exception ex) {
            Logs.error(ex);
        }
        return false;
    }

    public int getNumWeaponByRank(int rank) {
        if (!mWeaponByRank.containsKey(rank)) return 0;
        return mWeaponByRank.get(rank);
    }

    public UserItemEntity getItem(long id) {
        return mItem.get(id);
    }

    public UserEquipmentEntity getEquipment(long id) {
        return mEquipment.get(id);
    }

    /** @deprecated dùng {@link #getEquipment(long)} */
    @Deprecated
    public UserEquipmentEntity getItemEquipment(long id) {
        return getEquipment(id);
    }

    public UserItemEntity getItemByItemKey(int itemId) {
        for (UserItemEntity item : mItem.values()) {
            if (item.getItemId() == itemId) return item;
        }
        return null;
    }

    public UserItemEntity getItem(Pbmethod.ItemKey key) {
        return getItemByItemKey(key.getNumber());
    }

    public int countByItemKey(int itemId) {
        if (game.config.aEnum.ItemPointKey.isPointKey(itemId))
            return getItemPointNumber(itemId);
        int total = 0;
        for (UserItemEntity item : mItem.values()) {
            if (item.getItemId() == itemId) total += 1;
        }
        return total;
    }

    public List<UserItemEntity> listByItemKey(int itemId) {
        return mItem.values().stream()
                .filter(item -> item.getItemId() == itemId)
                .collect(Collectors.toList());
    }

    public List<UserEquipmentEntity> listEquipment() {
        return new ArrayList<>(mEquipment.values());
    }

    public UserPackEntity getPack(PackType type) {
        return mPacks.get(type.value);
    }

    public UserPackEntity getPack(int id) {
        return mPacks.get(id);
    }

    public List<UserPackEntity> getListPack() {
        return new ArrayList<>(mPacks.values());
    }

    public int getNumItemBag() {
        int bagCount = mUser.getUData().getSlotBagUI();
        List<Long> slots = mUser.getUData().getItemSlotList();
        return ItemSlotHelper.countOccupied(slots, 0, bagCount);
    }

    public int getNumMaterial() {
        int n = 0;
        for (UserMaterialEntity m : mMaterial.values()) {
            if (Bonus.isBlockedFromBagSlot(m.getIsTrading(), m.getInMarket()))
                continue;
            n++;
        }
        return n;
    }

    public int getNumEquipment() {
        return mEquipment.size();
    }

    public boolean canAddBagItem(int count) {
        if (count <= 0) return true;
        Bonus.reconcileItemSlots(mUser);
        return getNumItemBag() + count <= mUser.getUData().getSlotBagUI();
    }

    public boolean canAddEventItem(int count) {
        if (count <= 0) return true;
        return getNumItemEvent() + count <= mUser.getUData().getSlotEvent();
    }

    /** Item tab túi lớn — user_item_point có type EVENT/USE/SPEAKER và number > 0. */
    public int getNumItemEvent() {
        int n = 0;
        for (UserItemPointEntity row : mItemPoint.values()) {
            if (row.getNumber() > 0 && Bonus.usesEventBagPoint(row.getPointId()))
                n++;
        }
        return n;
    }

    public boolean canAddMaterial(int count) {
        if (count <= 0) return true;
        return getNumMaterial() + count <= mUser.getUData().getSlotMaterial();
    }

    public UserPetEntity getPet(long id) {
        return mPet.get(id);
    }

    public UserPetEntity getPetByConfigId(int petId) {
        for (UserPetEntity pet : mPet.values()) {
            if (pet.getPetId() == petId) return pet;
        }
        return null;
    }

    public UserArtifactEntity getArtifact(long id) {
        return mArtifact.get(id);
    }

    public UserArtifactEntity getArtifactByConfigId(int artifactId) {
        if (artifacts == null)
            return null;
        for (UserArtifactEntity artifact : artifacts) {
            if (artifact.getArtifactId() == artifactId)
                return artifact;
        }
        return null;
    }

    public void addArtifact(UserArtifactEntity uArtifact) {
        if (artifacts == null) artifacts = new ArrayList<>();
        artifacts.add(uArtifact);
        mArtifact.put(uArtifact.getId(), uArtifact);
    }

    public void removeArtifact(long id) {
        UserArtifactEntity rm = mArtifact.remove(id);
        if (rm != null && artifacts != null)
            artifacts.remove(rm);
    }

    public UserMountEntity getMount(long id) {
        return mMount.get(id);
    }

    public boolean hasItem(int itemId) {
        if (game.config.aEnum.ItemPointKey.isPointKey(itemId))
            return getItemPointNumber(itemId) > 0;
        return countByItemKey(itemId) > 0;
    }

    public void addItem(UserItemEntity uItem) {
        if (items == null) items = new ArrayList<>();
        items.add(uItem);
        mItem.put(uItem.getId(), uItem);
        applyItemSlotsFromUserData();
    }

    public void addEquipment(UserEquipmentEntity uEquip) {
        if (equipments == null) equipments = new ArrayList<>();
        equipments.add(uEquip);
        mEquipment.put(uEquip.getId(), uEquip);
        CfgAchievement.addAchievement(mUser, 2, uEquip.getItemId() + 30, 1);
        mUser.getUData().checkQuestTutorial(mUser, QuestTutType.HAS_ITEM_EQUIP_ID, uEquip.getItemId(), 1);
        applyItemSlotsFromUserData();
    }

    public void removeItem(long id) {
        UserItemEntity rm = mItem.remove(id);
        if (rm != null && items != null) items.remove(rm);
    }

    public void removeEquipment(long id) {
        UserEquipmentEntity rm = mEquipment.remove(id);
        if (rm != null && equipments != null) equipments.remove(rm);
    }

    public void removeItemEquip(List<UserEquipmentEntity> equipItems) {
        for (UserEquipmentEntity item : equipItems) {
            if (equipments != null) equipments.remove(item);
            mEquipment.remove(item.getId());
        }
    }

    public boolean removeItemsByItemKey(int itemId, int count) {
        if (count <= 0) return true;
        if (game.config.aEnum.ItemPointKey.isPointKey(itemId)) {
            List<Long> bonus = Bonus.viewItemPoint(itemId, -count);
            return !Bonus.receiveListItem(mUser, "remove_item_point", bonus).isEmpty();
        }
        List<UserItemEntity> rows = listByItemKey(itemId);
        if (rows.isEmpty()) return false;
        int removed = 0;
        for (UserItemEntity row : rows) {
            if (removed >= count) break;
            Bonus.clearItemFromSlot(mUser, Bonus.BONUS_ITEM, row.getId());
            if (row.deleteFromDb()) {
                removeItem(row.getId());
                removed++;
            }
        }
        return removed == count;
    }

    public void addPack(UserPackEntity pack) {
        if (!mPacks.containsKey(pack.getPackId())) mPacks.put(pack.getPackId(), pack);
    }

    public UserMaterialEntity getMaterial(long id) {
        return mMaterial.get(id);
    }

    public void addMaterial(UserMaterialEntity uMaterial) {
        if (materials == null) materials = new ArrayList<>();
        materials.add(uMaterial);
        mMaterial.put(uMaterial.getId(), uMaterial);
    }

    public void removeMaterial(long id) {
        UserMaterialEntity rm = mMaterial.remove(id);
        if (rm != null && materials != null) materials.remove(rm);
    }

    public void addPet(UserPetEntity uPet) {
        if (pets == null) pets = new ArrayList<>();
        pets.add(uPet);
        mPet.put(uPet.getId(), uPet);
    }

    public void addMount(UserMountEntity uMount) {
        if (mounts == null) mounts = new ArrayList<>();
        mounts.add(uMount);
        mMount.put(uMount.getId(), uMount);
    }

    public void removePet(long id) {
        UserPetEntity rm = mPet.remove(id);
        if (rm != null && pets != null) {
            pets.remove(rm);
        }
    }

    public void removeMount(long id) {
        UserMountEntity rm = mMount.remove(id);
        if (rm != null && mounts != null) {
            mounts.remove(rm);
        }
    }

    public UserMobEntity getMob(long id) {
        return mMob.get(id);
    }

    public void addMob(UserMobEntity uMob) {
        if (mobs == null) mobs = new ArrayList<>();
        mobs.add(uMob);
        mMob.put(uMob.getId(), uMob);
    }

    public void removeMob(long id) {
        UserMobEntity rm = mMob.remove(id);
        if (rm != null && mobs != null)
            mobs.remove(rm);
    }

    public UserSkinEntity getSkin(long id) {
        return mSkin.get(id);
    }

    public UserSkinEntity getSkinByConfigId(int skinId) {
        for (UserSkinEntity skin : mSkin.values()) {
            if (skin.getSkinId() == skinId) return skin;
        }
        return null;
    }

    public void addSkin(UserSkinEntity uSkin) {
        if (skins == null) skins = new ArrayList<>();
        skins.add(uSkin);
        mSkin.put(uSkin.getId(), uSkin);
    }

    public void removeSkin(long id) {
        UserSkinEntity rm = mSkin.remove(id);
        if (rm != null && skins != null)
            skins.remove(rm);
    }

    /** Lưu item_slot in-memory; flush response sẽ gửi UPDATE_BAG. */
    public boolean saveItemSlot(List<Long> slots) {
        if (!mUser.getUData().saveItemSlot(slots))
            return false;
        mUser.queueUpdateBag();
        return true;
    }

    /** Full snapshot ô túi nhỏ home: flat [bonusType, rowId, ...] × slotBagUI. */
    public Pbmethod.CommonVector buildUpdateBagPayload() {
        List<Long> slots = mUser.getUData().getItemSlotList();
        int bagCount = mUser.getUData().getSlotBagUI();
        List<Long> payload = new ArrayList<>(bagCount * 2);
        for (int i = 0; i < bagCount * 2; i++)
            payload.add(i < slots.size() ? slots.get(i) : 0L);
        return CommonProto.getCommonVectorProto(payload);
    }

    /** Gán ô túi UI khi nhận consum / equip / pet / mount. */
    public boolean prepareNewItemSlot(int bonusType, long rowId) {
        return Bonus.prepareNewItemSlot(mUser, bonusType, rowId);
    }

    public void clearItemFromSlot(int bonusType, long rowId) {
        Bonus.clearItemFromSlot(mUser, bonusType, rowId);
    }

    /** Mở thêm ô túi — client cần USER_DATA_INFO (slotBagUI) kèm UPDATE_BAG. */
    public void notifyBagSlotCountChanged() {
        mUser.queueUserDataInfo();
        mUser.queueUpdateBag();
    }

    public UserItemPointEntity getItemPoint(int pointId) {
        return mItemPoint.get(pointId);
    }

    public int getItemPointNumber(int pointId) {
        UserItemPointEntity row = mItemPoint.get(pointId);
        return row != null ? row.getNumber() : 0;
    }

    public void addItemPoint(UserItemPointEntity row) {
        if (itemPoints == null)
            itemPoints = new ArrayList<>();
        if (!itemPoints.contains(row))
            itemPoints.add(row);
        mItemPoint.put(row.getPointId(), row);
    }
}
