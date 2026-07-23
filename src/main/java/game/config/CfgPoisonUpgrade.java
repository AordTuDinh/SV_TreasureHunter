package game.config;

import com.google.gson.Gson;
import game.treasure.mapping.UserItemEntity;
import game.treasure.service.user.Bonus;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Phí nâng cấp consumable poison/medicine đã hóa hình (icon 1000–1005).
 * DB key {@code config_poisonUpgrade} → {@link #loadConfig(String)}.
 */
public class CfgPoisonUpgrade {
    public static final int DEFAULT_ICON_MIN = 1000;
    public static final int DEFAULT_ICON_MAX = 1005;
    public static final int DEFAULT_GEM_FROM_HH = 4;
    public static final int DEFAULT_MAX_LEVEL = 10;
    public static final int DEFAULT_MAX_RES_TIER = 6;
    private static final int[] DEFAULT_BASE_ROW_LOW = {1, 4, 6};

    private static int iconMin = DEFAULT_ICON_MIN;
    private static int iconMax = DEFAULT_ICON_MAX;
    private static int gemFromHhTier = DEFAULT_GEM_FROM_HH;
    private static int maxLevel = DEFAULT_MAX_LEVEL;
    private static int maxResTier = DEFAULT_MAX_RES_TIER;
    private static int[] baseRowLow = DEFAULT_BASE_ROW_LOW.clone();

    public static void loadConfig(String strJson) {
        if (strJson == null || strJson.isBlank())
            return;
        RootConfig root = new Gson().fromJson(strJson, RootConfig.class);
        if (root == null)
            return;
        if (root.iconMin > 0)
            iconMin = root.iconMin;
        if (root.iconMax > 0)
            iconMax = root.iconMax;
        if (root.gemFromHhTier > 0)
            gemFromHhTier = root.gemFromHhTier;
        if (root.maxLevel > 0)
            maxLevel = root.maxLevel;
        if (root.maxResTier > 0)
            maxResTier = root.maxResTier;
        if (root.baseRowLow != null && root.baseRowLow.size() >= 3) {
            baseRowLow = new int[3];
            for (int i = 0; i < 3; i++)
                baseRowLow[i] = root.baseRowLow.get(i);
        }
    }

    public static boolean isTransformedPoison(int icon) {
        return icon >= iconMin && icon <= iconMax;
    }

    public static boolean isTransformedPoison(UserItemEntity item) {
        return item != null && isTransformedPoison(item.getEffectiveIcon());
    }

    /** Poison đủ điều kiện add túi trading / đăng bán (icon 1003–1005). */
    public static boolean isListingPoisonIcon(int icon) {
        return icon >= 1003 && icon <= 1005;
    }

    /** HH tier 1..6 từ icon 1000..1005. */
    public static int getHhTier(int icon) {
        return icon - iconMin + 1;
    }

    public static boolean usesGemCurrency(int icon) {
        return getHhTier(icon) >= gemFromHhTier;
    }

    /**
     * Phí nâng từ level hiện tại lên level+1.
     *
     * @param icon    icon hiệu lực (1000–1005)
     * @param resTier res_item.tier (row 1–6)
     * @param level   level hiện tại (1–9)
     */
    public static long getUpgradeCost(int icon, int resTier, int level) {
        if (!isTransformedPoison(icon) || level < 1 || level >= maxLevel)
            return 0;
        if (resTier < 1 || resTier > maxResTier)
            return 0;

        int hh = getHhTier(icon);
        int row = resTier;
        long base;
        long step;
        if (row <= 3) {
            base = baseRowLow[row - 1] + hh;
            step = row;
        } else {
            base = row + hh - 1L;
            step = Math.max(1, hh);
        }
        return base + step * (level - 1L);
    }

    /**
     * Trả phí nâng poison đã HH; {@code null} nếu item không thuộc loại này.
     */
    public static List<Long> tryGetUpgradeFee(UserItemEntity item) {
        return tryGetUpgradeFee(item, null);
    }

    public static List<Long> tryGetUpgradeFee(UserItemEntity item, game.object.MyUser mUser) {
        if (item == null || !isTransformedPoison(item))
            return null;
        if (!CfgItem.canUpLevel(item))
            return new ArrayList<>();

        int icon = item.getEffectiveIcon();
        long cost = CfgItem.applyUpgradeFeeVip(getUpgradeCost(icon, item.getTier(), item.getLevel()), mUser);
        if (cost <= 0)
            return new ArrayList<>();
        if (usesGemCurrency(icon))
            return Bonus.viewGem((int) -cost);
        return Bonus.viewGold(-cost);
    }

    @Data
    static class RootConfig {
        int iconMin;
        int iconMax;
        int gemFromHhTier;
        int maxResTier;
        int maxLevel;
        List<Integer> baseRowLow;
    }
}
