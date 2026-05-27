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

public class UserResources implements Serializable {
    MyUser mUser;
    // lay luc dang nhap thoi, trong game k lay vi xu ly bang dic cho de, cap nhap thi vao dic
    @Setter
    List<UserItemEntity> items;
    @Setter
    List<UserPetEntity> pets;
    @Setter
    List<UserItemEquipmentEntity> itemEquipments;
    @Setter
    List<UserArtifactEntity> artifacts;
    @Setter
    List<UserMountEntity> mounts;
    @Setter
    List<UserPackEntity> packs;
    @Setter
    List<UserMaterialEntity> materials;

    // GET VÀ SET TÚI SẼ LẤY TỪ DIC CHO DỄ QUẢN LÝ...
    @Getter
    Map<Integer, UserItemEntity> mItem = new HashMap<>();
    @Getter
    Map<Long, UserPetEntity> mPet = new HashMap<>();
    @Getter
    Map<Long, UserItemEquipmentEntity> mItemEquipment = new HashMap<>();
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
            items.forEach(item -> mItem.put(item.getItemId(), item));
            packs.forEach(pack -> {
                if (pack.hasHSD()) {
                    if (!mPacks.containsKey(pack.getPackId())) {
                        mPacks.put(pack.getPackId(), pack);
                    }
                }
            });

            if (pets != null) {
                pets.forEach(pet -> mPet.put(pet.getId(), pet));
            }
            itemEquipments.forEach(item -> {
               mItemEquipment.put(item.getId(), item);
            });
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

    public UserItemEntity getItem(int itemId) {
        return mItem.get(itemId);
    }

    public UserItemEntity getItem(Pbmethod.ItemKey key) {
        return getItem(key.getNumber());
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
        return (int) mItem.values().stream().filter(item -> item.getNumber() > 0).count();
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

    public boolean hasPetConfig(int petId) {
        return getPetByConfigId(petId) != null;
    }

    public UserItemEquipmentEntity getItemEquipment(long itemId) {
        return mItemEquipment.get(itemId);
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

    public UserMountEntity getMountByConfigId(int mountId) {
        for (UserMountEntity mount : mMount.values()) {
            if (mount.getMountId() == mountId) return mount;
        }
        return null;
    }


    public boolean hasItem(int itemId) {
        return mItem.containsKey(itemId);
    }



    public void addItem(UserItemEntity uItem) {
        items.add(uItem);
        mItem.put(uItem.getItemId(), uItem);
    }


    public void addPack(UserPackEntity pack) {
        if (!mPacks.containsKey(pack.getPackId())) mPacks.put(pack.getPackId(), pack);
    }

    public void addItemEquip(UserItemEquipmentEntity uItem) {
        itemEquipments.add(uItem);
        mItemEquipment.put(uItem.getId(), uItem);
        // bắt đầu id = 30 nên sẽ cộng thêm 30
        CfgAchievement.addAchievement(mUser, 2, uItem.getItemId() + 30, 1);
        mUser.getUData().checkQuestTutorial(mUser, QuestTutType.HAS_ITEM_EQUIP_ID, uItem.getRes().getId(), 1);
    }

    public void removeItemEquip(List<UserItemEquipmentEntity> items) {
        for (int i = 0; i < items.size(); i++) {
            itemEquipments.remove(items.get(i));
            mItemEquipment.remove(items.get(i).getId());
        }
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
