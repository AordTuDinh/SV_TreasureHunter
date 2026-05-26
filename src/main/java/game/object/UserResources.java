package game.object;

import game.config.CfgAchievement;
import game.config.aEnum.*;
import game.treasure.mapping.*;
import protocol.Pbmethod;
import game.treasure.service.resource.ResEventTop;
import lombok.Getter;
import lombok.Setter;
import ozudo.base.database.DBJPA;
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
    List<UserPackEntity> packs;
    @Setter
    List<UserMaterialEntity> materials;

    // GET VÀ SET TÚI SẼ LẤY TỪ DIC CHO DỄ QUẢN LÝ...
    @Getter
    Map<Integer, UserItemEntity> mItem = new HashMap<>();
    @Getter
    Map<Integer, UserPetEntity> mPetAnimal = new HashMap<>();
    @Getter
    Map<Long, UserItemEquipmentEntity> mItemEquipment = new HashMap<>();
    @Getter
    Map<Integer, UserArtifactEntity> mArtifact = new HashMap<>();
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

            pets.forEach(pet -> {
                mPetAnimal.put(pet.getPetId(), pet);
            });
            itemEquipments.forEach(item -> {
               mItemEquipment.put(item.getId(), item);
            });
            if (artifacts != null) {
                artifacts.forEach(item -> mArtifact.put(item.getArtifactId(), item));
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


    public UserPetEntity getPet( int petId) {
        return  mPetAnimal.get(petId);
    }

    public UserItemEquipmentEntity getItemEquipment(long itemId) {
        return mItemEquipment.get(itemId);
    }

    public UserArtifactEntity getArtifact(int artifactId) {
        return mArtifact.get(artifactId);
    }

    public void addArtifact(UserArtifactEntity uArtifact) {
        if (artifacts == null) artifacts = new ArrayList<>();
        if (!mArtifact.containsKey(uArtifact.getArtifactId())) {
            artifacts.add(uArtifact);
        }
        mArtifact.put(uArtifact.getArtifactId(), uArtifact);
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
        if (!mPetAnimal.containsKey(uPet.getPetId())) {
            mPetAnimal.put(uPet.getPetId(), uPet);
            int achiId = 106 + uPet.getPetId();
            if (achiId > 106 && achiId < 132) CfgAchievement.addAchievement(mUser, 2, achiId, 1);
        }
        mUser.reCalculatePoint();
//        ResEventTop.checkEvent(mUser, uPet, TopType.PET_POINT);
    }
}
