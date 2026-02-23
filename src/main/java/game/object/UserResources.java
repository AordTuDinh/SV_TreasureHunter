package game.object;

import game.battle.calculate.IMath;
import game.config.CfgAchievement;
import game.config.aEnum.PetType;
import game.config.aEnum.*;
import game.dragonhero.mapping.*;
import game.dragonhero.service.resource.ResEventTop;
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
    //@Setter
    //List<UserF> friends;
    @Setter
    List<UserWeaponEntity> weapons;
    @Setter
    List<UserPetEntity> pets;
    @Setter
    List<UserItemFarmEntity> farms;
    @Setter
    List<UserPieceEntity> pieces;
    @Setter
    List<UserItemEquipmentEntity> itemEquipments;
    @Setter
    List<UserHeroEntity> heroes;
    @Setter
    List<UserPackEntity> packs;
    @Setter
//    @Getter
    List<UserLandEntity> lands;

    // GET VÀ SET TÚI SẼ LẤY TỪ DIC CHO DỄ QUẢN LÝ...
    @Getter
    Map<Integer, UserItemEntity> mItem = new HashMap<>();
    @Getter
    Map<Integer, UserWeaponEntity> mWeapon = new HashMap<>();
    @Getter
    Map<Integer, UserHeroEntity> mHero = new HashMap<>();
    @Getter
    Map<Integer, UserPetEntity> mPetAnimal = new HashMap<>();
    @Getter
    Map<Integer, UserPetEntity> mPetMonster = new HashMap<>();
    @Getter
    Map<Integer, UserItemFarmEntity> mItemFarmSeed = new HashMap<>();
    @Getter
    Map<Integer, UserItemFarmEntity> mItemFarmFood = new HashMap<>();
    @Getter
    Map<Integer, UserPieceEntity> mPieceWeapon = new HashMap<>();
    @Getter
    Map<Integer, UserPieceEntity> mPieceMonster = new HashMap<>();
    @Getter
    Map<Integer, UserPieceEntity> mPiecePet = new HashMap<>();
    @Getter
    Map<Integer, UserItemFarmEntity> mItemFarmAgri = new HashMap<>();
    @Getter
    Map<Integer, UserItemFarmEntity> mItemFarmTool = new HashMap<>();
    @Getter
    Map<Long, UserItemEquipmentEntity> mItemEquipment = new HashMap<>();
    @Getter
    Map<Integer, UserLandEntity> mLand = new HashMap<>();
    Map<Integer, UserPackEntity> mPacks = new HashMap<>();
    @Getter
    Map<Integer, Integer> mWeaponByRank = new HashMap<>();

    public UserResources(MyUser mUser) {
        this.mUser = mUser;
    }


    public boolean isOk() {
        try {
            items.forEach(item -> mItem.put(item.getItemId(), item));
            lands.forEach(land -> mLand.put(land.getId(), land));
            weapons.forEach(weapon -> {
                mWeapon.put(weapon.getWeaponId(), weapon);
                int rank = weapon.getRes().getRank();
                if (!mWeaponByRank.containsKey(rank)) mWeaponByRank.put(rank, 1);
                else mWeaponByRank.put(rank, mWeaponByRank.get(rank) + 1);
            });
            mUser.calComboWeapon();
            packs.forEach(pack -> {
                if (pack.hasHSD()) {
                    if (!mPacks.containsKey(pack.getPackId())) {
                        mPacks.put(pack.getPackId(), pack);
                    }
                }
            });
            heroes.forEach(hero -> mHero.put(hero.getHeroId(), hero));
            pets.forEach(pet -> {
                if (pet.getType() == PetType.MONSTER) {
                    mPetMonster.put(pet.getPetId(), pet);
                } else if (pet.getType() == PetType.ANIMAL) {
                    mPetAnimal.put(pet.getPetId(), pet);
                }
            });
            farms.forEach(farm -> {
                if (farm.getType() == ItemFarmType.SEED.value) {
                    mItemFarmSeed.put(farm.getId(), farm);
                } else if (farm.getType() == ItemFarmType.AGRI.value) {
                    mItemFarmAgri.put(farm.getId(), farm);
                } else if (farm.getType() == ItemFarmType.TOOL.value) {
                    mItemFarmTool.put(farm.getId(), farm);
                } else if (farm.getType() == ItemFarmType.FOOD.value) {
                    mItemFarmFood.put(farm.getId(), farm);
                }
            });
            pieces.forEach(piece -> {
                if (piece.getType() == PieceType.WEAPON.value) {
                    mPieceWeapon.put(piece.getId(), piece);
                } else if (piece.getType() == PieceType.MONSTER.value) {
                    mPieceMonster.put(piece.getId(), piece);
                } else if (piece.getType() == PieceType.PET.value) {
                    mPiecePet.put(piece.getId(), piece);
                }
            });
            List<Object> itemExpired = new ArrayList<>();
            itemEquipments.forEach(item -> {
                if (item.hasExpire()) mItemEquipment.put(item.getId(), item);
                else itemExpired.add(item.getId());
            });
            // xóa item hết hạn
            if (itemExpired.size() > 0) {
                DBJPA.deleteIn("user_item_equipment", "id", itemExpired);
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

    public UserItemEntity getItem(ItemKey key) {
        return getItem(key.id);
    }

    public UserItemFarmEntity getItemFarm(int itemType, int itemKey) {
        switch (ItemFarmType.get(itemType)) {
            case SEED -> {
                return mItemFarmSeed.get(itemKey);
            }
            case AGRI -> {
                return mItemFarmAgri.get(itemKey);
            }
            case TOOL -> {
                return mItemFarmTool.get(itemKey);
            }
            case FOOD -> {
                return mItemFarmFood.get(itemKey);
            }
        }
        return null;
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
        return (int) mItem.values().stream().filter(item -> item.getRes().getShowBag() == 1 && item.getNumber() > 0).count();
    }

    public UserItemFarmEntity getItemFarm(ItemFarmType itemType, int itemKey) {
        return getItemFarm(itemType.value, itemKey);
    }

    public UserItemFarmEntity getItemFarm(ItemFarmType itemType, FarmToolKey itemKey) {
        return getItemFarm(itemType.value, itemKey.value);
    }

    public UserPieceEntity getPiece(int pieceType, int itemKey) {
        if (pieceType == PieceType.WEAPON.value) return mPieceWeapon.get(itemKey);
        else if (pieceType == PieceType.MONSTER.value) return mPieceMonster.get(itemKey);
        else if (pieceType == PieceType.PET.value) return mPiecePet.get(itemKey);
        return null;
    }

    public UserPieceEntity getPiece(PieceType pieceType, int itemKey) {
        return getPiece(pieceType.value, itemKey);
    }

    public UserItemFarmEntity getItemSeed(int itemKey) {
        return mItemFarmSeed.get(itemKey);
    }

    public UserItemFarmEntity getItemAgri(int itemKey) {
        return mItemFarmAgri.get(itemKey);
    }

    public List<UserHeroEntity> getHeroes() {
        return mHero.values().stream().toList();
    }

    public UserWeaponEntity getWeapon(int weaponId) {
        return mWeapon.get(weaponId);
    }

    public UserPetEntity getPet(int type, int petId) {
        if (type == PetType.MONSTER.value) return mPetMonster.get(petId);
        else if (type == PetType.ANIMAL.value) return mPetAnimal.get(petId);
        return null;
    }

    public UserPetEntity getPet(PetType type, int petId) {
        return getPet(type.value, petId);
    }


    public UserHeroEntity getHero(int heroId) {
        return mHero.get(heroId);
    }

    public List<UserWeaponEntity> getWeaponsByRank(int rank) {
        List<UserWeaponEntity> lstWe = new ArrayList<>();
        mWeapon.forEach((k, v) -> {
            if (v.getRes().getRank() == rank) {
                lstWe.add(v);
            }
        });
        return lstWe;
    }

    public List<UserWeaponEntity> getWeaponEquip() {
        List<Integer> lstEquip = mUser.getUser().getWeaponEquipId();
        List<UserWeaponEntity> lstWe = new ArrayList<>();
        lstEquip.forEach(we -> {
            if (we != 0) lstWe.add(mWeapon.get(we));
        });
        return lstWe;
    }

    public UserWeaponEntity getWepon(int weponId) {
        return mWeapon.get(weponId);
    }

    public UserItemEquipmentEntity getItemEquipment(long itemId) {
        return mItemEquipment.get(itemId);
    }


    public boolean hasItem(int itemId) {
        return mItem.containsKey(itemId);
    }

    public boolean hasItemFarm(int itemType, int itemId) {
        switch (ItemFarmType.get(itemType)) {
            case SEED -> {
                return mItemFarmSeed.containsKey(itemId);
            }
            case AGRI -> {
                return mItemFarmAgri.containsKey(itemId);
            }
            case TOOL -> {
                return mItemFarmTool.containsKey(itemId);
            }
            case FOOD -> {
                return mItemFarmFood.containsKey(itemId);
            }
        }
        return false;
    }

    public boolean hasPiece(int itemType, int itemId) {
        if (itemType == PieceType.WEAPON.value) return mPieceWeapon.containsKey(itemId);
        else if (itemType == PieceType.MONSTER.value) return mPieceMonster.containsKey(itemId);
        else if (itemType == PieceType.PET.value) return mPiecePet.containsKey(itemId);
        else return false;
    }

    public boolean hasWeapon(int weaponId) {
        return mWeapon.containsKey(weaponId);
    }

    public boolean hasHero(int heroId) {
        return mHero.containsKey(heroId);
    }

    public boolean hasPet(int petType, int petId) {
        if (petType == PetType.MONSTER.value) {
            return mPetMonster.containsKey(petId);
        } else if (petType == PetType.ANIMAL.value) {
            return mPetAnimal.containsKey(petId);
        } else return false;
    }

    public void addItem(UserItemEntity uItem) {
        items.add(uItem);
        mItem.put(uItem.getItemId(), uItem);
    }

    public void addLand(UserLandEntity uLand) {
        lands.add(uLand);
        mLand.put(uLand.getId(), uLand);
    }

    public void addItemFarm(int itemType, UserItemFarmEntity uItemFarm) {
        farms.add(uItemFarm);
        switch (ItemFarmType.get(itemType)) {
            case SEED -> mItemFarmSeed.put(uItemFarm.getId(), uItemFarm);
            case AGRI -> mItemFarmAgri.put(uItemFarm.getId(), uItemFarm);
            case TOOL -> mItemFarmTool.put(uItemFarm.getId(), uItemFarm);
            case FOOD -> mItemFarmFood.put(uItemFarm.getId(), uItemFarm);
        }
    }

    public void addPiece(int itemType, UserPieceEntity uPiece) {
        pieces.add(uPiece);
        if (itemType == PieceType.WEAPON.value) mPieceWeapon.put(uPiece.getId(), uPiece);
        else if (itemType == PieceType.MONSTER.value) mPieceMonster.put(uPiece.getId(), uPiece);
        else if (itemType == PieceType.PET.value) mPiecePet.put(uPiece.getId(), uPiece);
    }

    public void addPack(UserPackEntity pack) {
        if (!mPacks.containsKey(pack.getPackId())) mPacks.put(pack.getPackId(), pack);
    }

    public void addItemEquip(UserItemEquipmentEntity uItem) {
        itemEquipments.add(uItem);
        if (uItem.hasExpire()) mItemEquipment.put(uItem.getId(), uItem);
        // bắt đầu id = 30 nên sẽ cộng thêm 30
        CfgAchievement.addAchievement(mUser, 2, uItem.getItemId() + 30, 1);
        mUser.getUData().checkQuestTutorial(mUser, QuestTutType.HAS_ITEM_EQUIP_ID, uItem.getRes().getId(), 1);
    }

    public void addWeapon(UserWeaponEntity uWeapon) {
        weapons.add(uWeapon);
        mWeapon.put(uWeapon.getWeaponId(), uWeapon);
        int rank = uWeapon.getRes().getRank();
        if (!mWeaponByRank.containsKey(rank)) mWeaponByRank.put(rank, 1);
        else mWeaponByRank.put(rank, mWeaponByRank.get(rank) + 1);
        mUser.reCalculatePoint();
        CfgAchievement.addAchievement(mUser, 2, uWeapon.getWeaponId(), 1);
    }

    public void addHero(UserHeroEntity uHero) {
        if (!mHero.containsKey(uHero.getHeroId())) {
            mHero.put(uHero.getHeroId(), uHero);
            uHero.calPointHero(mUser, IMath.calculatePoint(mUser, false));
        }
    }

    public void removeItemEquip(List<UserItemEquipmentEntity> items) {
        for (int i = 0; i < items.size(); i++) {
            itemEquipments.remove(items.get(i));
            mItemEquipment.remove(items.get(i).getId());
        }
    }

    public void addPet(UserPetEntity uPet) {
        if (uPet.getType() == PetType.MONSTER && !mPetMonster.containsKey(uPet.getPetId())) {
            mPetMonster.put(uPet.getPetId(), uPet);
            // pet từ 90 -> 106
            int achiId = 90 + uPet.getPetId();
            if (achiId > 90 && achiId < 107) CfgAchievement.addAchievement(mUser, 2, achiId, 1);
            mUser.getUData().checkQuestTutorial(mUser, QuestTutType.HAS_MONSTER, uPet.getPetId(), 1);
        } else if (uPet.getType() == PetType.ANIMAL && !mPetAnimal.containsKey(uPet.getPetId())) {
            mPetAnimal.put(uPet.getPetId(), uPet);
            int achiId = 106 + uPet.getPetId();
            if (achiId > 106 && achiId < 132) CfgAchievement.addAchievement(mUser, 2, achiId, 1);
        }
        mUser.reCalculatePoint();
        ResEventTop.checkEvent(mUser, uPet,TopType.PET_POINT);
    }
}
