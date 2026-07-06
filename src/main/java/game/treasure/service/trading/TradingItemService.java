package game.treasure.service.trading;

import com.google.gson.Gson;
import game.config.CfgTrading;
import game.object.MyUser;
import game.object.UserResources;
import game.treasure.mapping.*;
import game.treasure.service.user.Bonus;
import ozudo.base.database.DBJPA;

import java.util.Arrays;
import java.util.List;

public final class TradingItemService {
    private TradingItemService() {
    }

    public static Object getOwned(MyUser mUser, int bonusType, long rowId) {
        UserResources res = mUser.getResources();
        return switch (bonusType) {
            case Bonus.BONUS_ITEM -> res.getItem(rowId);
            case Bonus.BONUS_EQUIPMENT -> res.getEquipment(rowId);
            case Bonus.BONUS_MATERIAL -> res.getMaterial(rowId);
            case Bonus.BONUS_PET -> res.getPet(rowId);
            case Bonus.BONUS_MOUNT -> res.getMount(rowId);
            case Bonus.BONUS_MOB -> res.getMob(rowId);
            case Bonus.BONUS_ARTIFACT -> res.getArtifact(rowId);
            case Bonus.BONUS_SKIN -> res.getSkin(rowId);
            default -> null;
        };
    }

    public static int getIsTrading(Object entity) {
        if (entity instanceof UserPetEntity e) return e.getIsTrading();
        if (entity instanceof UserMountEntity e) return e.getIsTrading();
        if (entity instanceof UserItemEntity e) return e.getIsTrading();
        if (entity instanceof UserEquipmentEntity e) return e.getIsTrading();
        if (entity instanceof UserMaterialEntity e) return e.getIsTrading();
        if (entity instanceof UserMobEntity e) return e.getIsTrading();
        if (entity instanceof UserArtifactEntity e) return e.getIsTrading();
        if (entity instanceof UserSkinEntity e) return e.getIsTrading();
        return 0;
    }

    public static int getInMarket(Object entity) {
        if (entity instanceof UserPetEntity e) return e.getInMarket();
        if (entity instanceof UserMountEntity e) return e.getInMarket();
        if (entity instanceof UserItemEntity e) return e.getInMarket();
        if (entity instanceof UserEquipmentEntity e) return e.getInMarket();
        if (entity instanceof UserMaterialEntity e) return e.getInMarket();
        if (entity instanceof UserMobEntity e) return e.getInMarket();
        if (entity instanceof UserArtifactEntity e) return e.getInMarket();
        if (entity instanceof UserSkinEntity e) return e.getInMarket();
        return 0;
    }

    public static boolean setTradingFlags(MyUser mUser, int bonusType, long rowId, int isTrading, int inMarket) {
        Object entity = getOwned(mUser, bonusType, rowId);
        if (entity == null)
            return false;
        String table = tableName(bonusType);
        if (table == null)
            return false;
        if (!DBJPA.update(table, Arrays.asList("is_trading", isTrading, "in_market", inMarket),
                Arrays.asList("id", rowId, "user_id", mUser.getUser().getId())))
            return false;
        applyFlags(entity, isTrading, inMarket);
        return true;
    }

    public static boolean transferToUser(int bonusType, long rowId, int fromUserId, int toUserId,
                                         int isTrading, int inMarket) {
        String table = tableName(bonusType);
        if (table == null)
            return false;
        return DBJPA.update(table,
                Arrays.asList("user_id", toUserId, "is_trading", isTrading, "in_market", inMarket),
                Arrays.asList("id", rowId, "user_id", fromUserId));
    }

    static void applyFlags(Object entity, int isTrading, int inMarket) {
        if (entity instanceof UserPetEntity e) {
            e.setIsTrading(isTrading);
            e.setInMarket(inMarket);
        } else if (entity instanceof UserMountEntity e) {
            e.setIsTrading(isTrading);
            e.setInMarket(inMarket);
        } else if (entity instanceof UserItemEntity e) {
            e.setIsTrading(isTrading);
            e.setInMarket(inMarket);
        } else if (entity instanceof UserEquipmentEntity e) {
            e.setIsTrading(isTrading);
            e.setInMarket(inMarket);
        } else if (entity instanceof UserMaterialEntity e) {
            e.setIsTrading(isTrading);
            e.setInMarket(inMarket);
        } else if (entity instanceof UserMobEntity e) {
            e.setIsTrading(isTrading);
            e.setInMarket(inMarket);
        } else if (entity instanceof UserArtifactEntity e) {
            e.setIsTrading(isTrading);
            e.setInMarket(inMarket);
        } else if (entity instanceof UserSkinEntity e) {
            e.setIsTrading(isTrading);
            e.setInMarket(inMarket);
        }
    }

    public static String tableName(int bonusType) {
        return switch (bonusType) {
            case Bonus.BONUS_ITEM -> "user_item";
            case Bonus.BONUS_EQUIPMENT -> "user_equipment";
            case Bonus.BONUS_MATERIAL -> "user_material";
            case Bonus.BONUS_PET -> "user_pet";
            case Bonus.BONUS_MOUNT -> "user_mount";
            case Bonus.BONUS_MOB -> "user_mob";
            case Bonus.BONUS_ARTIFACT -> "user_artifact";
            case Bonus.BONUS_SKIN -> "user_skin";
            default -> null;
        };
    }

    public static int countTradingSlotsUsed(MyUser mUser, int tab) {
        UserResources res = mUser.getResources();
        int count = 0;
        count += countTab(res.getMPet().values(), tab, Bonus.BONUS_PET);
        count += countTab(res.getMMount().values(), tab, Bonus.BONUS_MOUNT);
        count += countTab(res.getMItem().values(), tab, Bonus.BONUS_ITEM);
        count += countTab(res.getMEquipment().values(), tab, Bonus.BONUS_EQUIPMENT);
        count += countTab(res.getMMaterial().values(), tab, Bonus.BONUS_MATERIAL);
        count += countTab(res.getMMob().values(), tab, Bonus.BONUS_MOB);
        count += countTab(res.getMArtifact().values(), tab, Bonus.BONUS_ARTIFACT);
        count += countTab(res.getMSkin().values(), tab, Bonus.BONUS_SKIN);
        return count;
    }

    static int countTab(Iterable<?> items, int tab, int bonusType) {
        if (CfgTrading.resolveTab(bonusType) != tab)
            return 0;
        int n = 0;
        for (Object o : items) {
            if (getIsTrading(o) == 1 || getInMarket(o) == 1)
                n++;
        }
        return n;
    }

    public static boolean hasEmptyTradingSlot(MyUser mUser, int tab) {
        UserDataEntity uData = mUser.getUData();
        int unlocked = tab == CfgTrading.TAB_ITEM ? uData.getSlotTrading1() : uData.getSlotTrading2();
        return countTradingSlotsUsed(mUser, tab) < unlocked;
    }

    public static String serializeItemInfo(Object entity) {
        return new Gson().toJson(entity);
    }

    public static Object loadFromDb(int bonusType, long rowId, int userId) {
        String table = tableName(bonusType);
        if (table == null)
            return null;
        Class<?> clazz = entityClass(bonusType);
        if (clazz == null)
            return null;
        return DBJPA.getUnique(table, clazz, "id", rowId, "user_id", userId);
    }

    static Class<?> entityClass(int bonusType) {
        return switch (bonusType) {
            case Bonus.BONUS_ITEM -> UserItemEntity.class;
            case Bonus.BONUS_EQUIPMENT -> UserEquipmentEntity.class;
            case Bonus.BONUS_MATERIAL -> UserMaterialEntity.class;
            case Bonus.BONUS_PET -> UserPetEntity.class;
            case Bonus.BONUS_MOUNT -> UserMountEntity.class;
            case Bonus.BONUS_MOB -> UserMobEntity.class;
            case Bonus.BONUS_ARTIFACT -> UserArtifactEntity.class;
            case Bonus.BONUS_SKIN -> UserSkinEntity.class;
            default -> null;
        };
    }

    public static void attachToBuyerResources(MyUser buyer, int bonusType, long rowId, int sellerId) {
        Object row = loadFromDb(bonusType, rowId, buyer.getUser().getId());
        if (row == null)
            return;
        applyFlags(row, 1, 0);
        UserResources res = buyer.getResources();
        if (row instanceof UserPetEntity e) res.addPet(e);
        else if (row instanceof UserMountEntity e) res.addMount(e);
        else if (row instanceof UserItemEntity e) res.addItem(e);
        else if (row instanceof UserEquipmentEntity e) res.addEquipment(e);
        else if (row instanceof UserMaterialEntity e) res.addMaterial(e);
        else if (row instanceof UserMobEntity e) res.addMob(e);
        else if (row instanceof UserArtifactEntity e) res.addArtifact(e);
        else if (row instanceof UserSkinEntity e) res.addSkin(e);
    }

    public static void detachFromSellerResources(MyUser seller, int bonusType, long rowId) {
        UserResources res = seller.getResources();
        switch (bonusType) {
            case Bonus.BONUS_ITEM -> res.removeItem(rowId);
            case Bonus.BONUS_EQUIPMENT -> res.removeEquipment(rowId);
            case Bonus.BONUS_MATERIAL -> res.removeMaterial(rowId);
            case Bonus.BONUS_PET -> res.removePet(rowId);
            case Bonus.BONUS_MOUNT -> res.removeMount(rowId);
            case Bonus.BONUS_MOB -> res.removeMob(rowId);
            case Bonus.BONUS_ARTIFACT -> res.removeArtifact(rowId);
            case Bonus.BONUS_SKIN -> res.removeSkin(rowId);
            default -> {
            }
        }
    }
}
