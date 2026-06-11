package game.config;

import game.config.aEnum.ItemType;
import game.config.lang.Lang;
import game.treasure.mapping.UserItemEntity;
import game.object.MyUser;
import game.treasure.service.user.Bonus;
import protocol.Pbmethod;

import java.util.ArrayList;
import java.util.List;

public class CfgItem {

    public static final int MAX_UPGRADE_LEVEL = 10;

    /** Phí nâng L→L+1 ở tier 1 (vàng). */
    public static final List<Integer> UPGRADE_FEE_BASE_T1 = List.of(10, 13, 19, 27, 38, 53, 75, 105, 147);

    /** Giá bán tại level L ở tier 1 (vàng). */
    public static final List<Integer> SELL_PRICE_BASE_T1 = List.of(5, 10, 16, 25, 38, 57, 83, 120, 172, 246);

    static final List<Integer> ITEM_MEDICINE_IDS = List.of(
            Pbmethod.ItemKey.BINH_MAU_1.getNumber(),
            Pbmethod.ItemKey.BINH_MAU_2.getNumber(),
            Pbmethod.ItemKey.BINH_MAU_3.getNumber(),
            Pbmethod.ItemKey.BINH_MAU_4.getNumber()
    );

    public static boolean canUpLevel(UserItemEntity item) {
        if (item == null) return false;
        int type = item.getType();
        if (type != ItemType.CONSUMABLE.value && type != ItemType.EQUIPMENT.value) return false;
        int level = item.getLevel();
        return level >= 1 && level < MAX_UPGRADE_LEVEL;
    }

    public static int getTierMult(UserItemEntity item) {
        int tier = item.getTier();
        return tier > 0 ? tier : 1;
    }

    /** Phí nâng từ level hiện tại lên level+1 (vàng, trước khi trừ). */
    public static long getUpgradeFeeGold(UserItemEntity item) {
        if (!canUpLevel(item)) return 0;
        int level = item.getLevel();
        int idx = level - 1;
        if (idx < 0 || idx >= UPGRADE_FEE_BASE_T1.size()) return 0;
        long fee = (long) getTierMult(item) * UPGRADE_FEE_BASE_T1.get(idx);
        if (item.getType() == ItemType.CONSUMABLE.value) {
            fee = fee / 2;
        }
        return fee;
    }

    public static List<Long> getUpgradeFee(UserItemEntity item) {
        long fee = getUpgradeFeeGold(item);
        if (fee <= 0) return new ArrayList<>();
        return Bonus.viewGold(-fee);
    }

    public static int getSellPriceGold(UserItemEntity item) {
        int level = item.getLevel();
        if (level < 1) level = 1;
        int idx = Math.min(level, SELL_PRICE_BASE_T1.size()) - 1;
        return getTierMult(item) * SELL_PRICE_BASE_T1.get(idx);
    }

    public static List<Long> getPriceSellItem(UserItemEntity uItem) {
        return Bonus.viewGold(getSellPriceGold(uItem));
    }

    public static boolean isItemMedicine(int itemId) {
        return ITEM_MEDICINE_IDS.contains(itemId);
    }

    public static String canBuyItem(MyUser mUser, List<Long> items, int number) {
        List<List<Long>> bms = Bonus.parse(items);
        for (int i = 0; i < bms.size(); i++) {
            List<Long> bonus = bms.get(i);
            if (bonus.get(0) == Bonus.BONUS_PET) {
                if (number > 1) return Lang.instance(mUser).get(Lang.err_can_buy_one);
            }
        }
        return null;
    }

    public static void loadConfig(String strJson) {
    }
}
