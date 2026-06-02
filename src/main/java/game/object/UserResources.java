package game.object;

import game.config.CfgAchievement;
import game.config.aEnum.*;
import game.treasure.mapping.*;
import protocol.Pbmethod;
import lombok.Getter;
import lombok.Setter;
import ozudo.base.log.Logs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UserResources implements Serializable {
    MyUser mUser;
    @Setter
    List<UserItemEntity> items;
    @Setter
    List<UserPetEntity> pets;
    @Setter
    List<UserArtifactEntity> artifacts;
    @Setter
    List<UserMountEntity> mounts;
    @Setter
    List<UserPackEntity> packs;
    @Setter
    List<UserMaterialEntity> materials;

    @Getter
    Map<Long, UserItemEntity> mItem = new HashMap<>();
    @Getter
    Map<Long, UserPetEntity> mPet = new HashMap<>();
    @Getter
    Map<Long, UserArtifactEntity> mArtifact = new HashMap<>();
    @Getter
    Map<Long, UserMountEntity> mMount = new HashMap<>();
    @Getter
    Map<Integer, UserPackEntity> mPacks = new HashMap<>();
    @Getter
    Map<Integer, Integer> mWeaponByRank = new HashMap<>();
    @Getter
    Map<Long, UserMaterialEntity> mMaterial = new HashMap<>();

    public UserResources(MyUser mUser) {
        this.mUser = mUser;
    }

    public boolean isOk() {
        try {
            if (items != null) {
                items.forEach(item -> mItem.put(item.getId(), item));
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
            if (materials != null) {
                materials.forEach(mat -> mMaterial.put(mat.getId(), mat));
            }
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

    /** Trang bị (storage type = 2). */
    public UserItemEntity getItemEquipment(long id) {
        UserItemEntity item = mItem.get(id);
        if (item != null && item.isEquipment()) return item;
        return null;
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
        int total = 0;
        for (UserItemEntity item : mItem.values()) {
            if (item.getItemId() == itemId) total += 1;
        }
        return total;
    }

    public List<UserItemEntity> listByItemKey(int itemId) {
        return mItem.values().stream()
                .filter(item -> item.getItemId() == itemId )
                .collect(Collectors.toList());
    }

    public List<UserItemEntity> listEquipment() {
        return mItem.values().stream().filter(UserItemEntity::isEquipment).collect(Collectors.toList());
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
        return (int) mItem.values().stream()
                .filter(item -> !item.isEquipment())
                .count();
    }

    public int getNumEquipment() {
        return (int) mItem.values().stream().filter(UserItemEntity::isEquipment).count();
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

    public void addArtifact(UserArtifactEntity uArtifact) {
        if (artifacts == null) artifacts = new ArrayList<>();
        artifacts.add(uArtifact);
        mArtifact.put(uArtifact.getId(), uArtifact);
    }

    public UserMountEntity getMount(long id) {
        return mMount.get(id);
    }

    public boolean hasItem(int itemId) {
        return countByItemKey(itemId) > 0;
    }

    public void addItem(UserItemEntity uItem) {
        if (items == null) items = new ArrayList<>();
        items.add(uItem);
        mItem.put(uItem.getId(), uItem);
        if (uItem.isEquipment()) {
            CfgAchievement.addAchievement(mUser, 2, uItem.getItemId() + 30, 1);
            mUser.getUData().checkQuestTutorial(mUser, QuestTutType.HAS_ITEM_EQUIP_ID, uItem.getItemId(), 1);
        }
    }

    public void removeItem(long id) {
        UserItemEntity rm = mItem.remove(id);
        if (rm != null && items != null) items.remove(rm);
    }

    public void removeItemEquip(List<UserItemEntity> equipItems) {
        for (UserItemEntity item : equipItems) {
            if (items != null) items.remove(item);
            mItem.remove(item.getId());
        }
    }

    public boolean removeItemsByItemKey(int itemId, int count) {
        if (count <= 0) return true;
        List<UserItemEntity> rows = listByItemKey(itemId);
        if (rows.isEmpty()) return false;
        UserItemEntity first = rows.get(0);
        if (first.isAggregatedItem()) {
            List<Long> dataSticker = new ArrayList<>(ozudo.base.helper.GsonUtil.strToListLong(first.getData() == null ? "[]" : first.getData()));
            for (int i = 0; i < count && dataSticker.size() > 1; i++) {
                dataSticker.remove(1);
            }
            if (dataSticker.size() <= 1) first.clearAggregated();
            first.setData(ozudo.base.helper.StringHelper.toDBString(dataSticker));
            return first.update(List.of("data", first.getData()));
        }
        int removed = 0;
        for (UserItemEntity row : rows) {
            if (removed >= count) break;
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
}
