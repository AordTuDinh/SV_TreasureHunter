package game.config;

import game.object.MyUser;
import game.treasure.mapping.*;
import game.treasure.service.user.Bonus;

/**
 * Quy tắc chợ tự do: phí đăng, giá, tab, điều kiện add/list.
 */
public class CfgTrading {
    public static final int TAB_ITEM = 1;
    public static final int TAB_MATERIAL = 2;
    public static final int FREE_SLOT_TAB1 = 20;
    public static final int FREE_SLOT_TAB2 = 0;
    public static final int PRICE_MIN = 2;
    public static final int PRICE_MAX = 9999;
    public static final int PAGE_SIZE = 20;
    public static final long WAIT_MIN_MS = 30_000L;
    public static final long WAIT_MAX_MS = 120_000L;

    /** Phí đăng tin — trả ngay khi xác nhận. Giá >= 400: chỉ 5%. */
    public static int calcListingFee(int price) {
        if (price >= 400)
            return (int) Math.ceil(price * 0.05);
        if (price >= 200)
            return 3;
        if (price >= 100)
            return 2;
        return 1;
    }

    public static int getUnlockCost(int tab, int unlockedCount) {
        int freeBase = tab == TAB_ITEM ? FREE_SLOT_TAB1 : FREE_SLOT_TAB2;
        return Math.max(1, unlockedCount - freeBase + 1);
    }

    public static boolean isValidPrice(int price) {
        return price >= PRICE_MIN && price <= PRICE_MAX;
    }

    public static long randomVerifyUntil() {
        long span = WAIT_MAX_MS - WAIT_MIN_MS;
        return System.currentTimeMillis() + WAIT_MIN_MS + (span > 0 ? (long) (Math.random() * span) : 0);
    }

    public static int resolveTab(int bonusType) {
        return bonusType == Bonus.BONUS_MATERIAL ? TAB_MATERIAL : TAB_ITEM;
    }

    /** Điều kiện lỏng — add vào túi trading. */
    public static String validateWalletAdd(int bonusType, Object entity, MyUser mUser) {
        if (entity == null)
            return "err_params";
        if (bonusType == Bonus.BONUS_PET) {
            UserPetEntity pet = (UserPetEntity) entity;
            if (UserPetEntity.isEquipped(mUser, pet.getId()))
                return "err_item_equip";
            return null;
        }
        if (bonusType == Bonus.BONUS_MOUNT) {
            UserMountEntity mount = (UserMountEntity) entity;
            if (UserMountEntity.isEquipped(mUser, mount.getId()))
                return "err_item_equip";
            return null;
        }
        if (bonusType == Bonus.BONUS_SKIN || bonusType == Bonus.BONUS_ARTIFACT || bonusType == Bonus.BONUS_MOB)
            return null;
        if (bonusType == Bonus.BONUS_ITEM) {
            UserItemEntity item = (UserItemEntity) entity;
            if (item.getLockDestroy() == 1)
                return "err_item_lock_in_bag";
            if (!CfgPoisonUpgrade.isListingPoisonIcon(item.getEffectiveIcon()))
                return "err_params";
            return null;
        }
        if (bonusType == Bonus.BONUS_EQUIPMENT) {
            UserEquipmentEntity equip = (UserEquipmentEntity) entity;
            if (equip.isEquip() || equip.getLockDestroy() == 1)
                return "err_item_equip";
            return null;
        }
        if (bonusType == Bonus.BONUS_MATERIAL) {
            UserMaterialEntity mat = (UserMaterialEntity) entity;
            if (mat.getTier() < 4)
                return "err_params";
            return null;
        }
        return "err_params";
    }

    /** Điều kiện chặt — đăng bán. */
    public static String validateListForSale(int bonusType, Object entity, MyUser mUser) {
        String base = validateWalletAdd(bonusType, entity, mUser);
        if (base != null)
            return base;
        if (bonusType == Bonus.BONUS_PET) {
            if (((UserPetEntity) entity).getIsCraft() != 1)
                return "err_trading_need_craft";
            return null;
        }
        if (bonusType == Bonus.BONUS_MOUNT) {
            if (((UserMountEntity) entity).getIsCraft() != 1)
                return "err_trading_need_craft";
            return null;
        }
        if (bonusType == Bonus.BONUS_SKIN) {
            if (((UserSkinEntity) entity).getIsCraft() != 1)
                return "err_trading_need_craft";
            return null;
        }
        if (bonusType == Bonus.BONUS_ITEM) {
            if (!CfgPoisonUpgrade.isListingPoisonIcon(((UserItemEntity) entity).getEffectiveIcon()))
                return "err_trading_poison_not_ready";
            return null;
        }
        if (bonusType == Bonus.BONUS_MATERIAL) {
            UserMaterialEntity mat = (UserMaterialEntity) entity;
            if (mat.getTier() < 4 || mat.getLevel() < 10)
                return "err_trading_material_not_ready";
            return null;
        }
        return null;
    }
}
