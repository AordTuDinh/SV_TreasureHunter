package game.config;


import com.google.gson.Gson;

import game.battle.calculate.IMath;
import game.battle.object.Point;
import game.config.aEnum.ItemType;

import game.config.lang.Lang;

import game.treasure.mapping.UserEquipmentEntity;
import game.treasure.mapping.UserItemEntity;

import game.object.MyUser;

import game.treasure.service.user.Bonus;

import lombok.Data;

import lombok.Getter;
import protocol.Pbmethod;


import java.util.ArrayList;

import java.util.HashMap;

import java.util.List;

import java.util.Map;


public class CfgItem {
    public static final int MAX_UPGRADE_LEVEL = 10;
    public static final List<Integer> UPGRADE_FEE_BASE_T1 = List.of(10, 13, 19, 27, 38, 53, 75, 105, 147);
    public static final List<Integer> SELL_PRICE_BASE_T1 = List.of(5, 10, 16, 25, 38, 57, 83, 120, 172, 246);

    static final List<Integer> ITEM_MEDICINE_IDS = List.of(
            Pbmethod.ItemKey.BINH_MAU_1.getNumber(),
            Pbmethod.ItemKey.BINH_MAU_2.getNumber(),
            Pbmethod.ItemKey.BINH_MAU_3.getNumber(),
            Pbmethod.ItemKey.BINH_MAU_4.getNumber()
    );

    /**
     * Hệ số tăng chỉ số theo cấp: Stat_L = Stat_1 × STAT_LEVEL_MULT^(L-1).
     * Mỗi cấp tăng 10% so với cấp trước.
     */
    public static final double STAT_LEVEL_MULT = 1.1;

    @Getter
    private static EquipStatRollConfig equipStatRoll = defaultEquipStatRoll();

    /**
     * Stat_L = Stat_1 × 1.1^(L-1).
     */
    public static float getStatLevelMultiplier(int level) {
        if (level <= 1)
            return 1f;
        return (float) Math.pow(STAT_LEVEL_MULT, level - 1);
    }

    /**
     * Tính chỉ số tại level từ giá trị gốc level 1 (user_item.data).
     */
    public static float getStatAtLevel(float statLevel1, int level) {
        if (statLevel1 <= 0f || level < 1)
            return 0f;
        return statLevel1 * getStatLevelMultiplier(level);
    }

    /**
     * Làm tròn stat theo loại point (MOVE_SPEED giữ 1 chữ số thập phân).
     */
    public static float formatPointStat(int pointId, float raw) {
        if (pointId == Point.MOVE_SPEED)
            return IMath.round1(raw);
        return Math.round(raw);
    }


    public static boolean canUpLevel(UserItemEntity item) {
        if (item == null) return false;
        int type = item.getType();
        if (type != ItemType.CONSUMABLE.value && type != ItemType.EQUIPMENT.value) return false;
        int level = item.getLevel();
        return level >= 1 && level < MAX_UPGRADE_LEVEL;
    }

    public static boolean canUpLevel(UserEquipmentEntity item) {
        if (item == null) return false;
        int level = item.getLevel();
        return level >= 1 && level < MAX_UPGRADE_LEVEL;
    }


    public static int getTierMult(UserItemEntity item) {
        int tier = item.getTier();
        return tier > 0 ? tier : 1;
    }

    public static int getTierMult(UserEquipmentEntity item) {
        int tier = item.getTier();
        return tier > 0 ? tier : 1;
    }


    /**
     * Phí nâng từ level hiện tại lên level+1 (vàng, trước khi trừ).
     */

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

    public static long getUpgradeFeeGold(UserEquipmentEntity item) {
        if (!canUpLevel(item)) return 0;
        int level = item.getLevel();
        int idx = level - 1;
        if (idx < 0 || idx >= UPGRADE_FEE_BASE_T1.size()) return 0;
        return (long) getTierMult(item) * UPGRADE_FEE_BASE_T1.get(idx);
    }


    public static List<Long> getUpgradeFee(UserItemEntity item) {
        long fee = getUpgradeFeeGold(item);
        if (fee <= 0) return new ArrayList<>();
        return Bonus.viewGold(-fee);
    }

    public static List<Long> getUpgradeFee(UserEquipmentEntity item) {
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

    public static int getSellPriceGold(UserEquipmentEntity item) {
        int level = item.getLevel();
        if (level < 1) level = 1;
        int idx = Math.min(level, SELL_PRICE_BASE_T1.size()) - 1;
        return getTierMult(item) * SELL_PRICE_BASE_T1.get(idx);
    }


    public static List<Long> getPriceSellItem(UserItemEntity uItem) {
        return Bonus.viewGold(getSellPriceGold(uItem));
    }

    public static List<Long> getPriceSellItem(UserEquipmentEntity uItem) {
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
        if (strJson == null || strJson.isBlank())
            return;
        ItemRootConfig root = new Gson().fromJson(strJson, ItemRootConfig.class);
        if (root == null || root.equipStatRoll == null)
            return;
        equipStatRoll = mergeEquipStatRoll(defaultEquipStatRoll(), root.equipStatRoll);
        equipStatRoll.normalize();
    }

    /**
     * Giữ default cho field thiếu trong JSON DB (tránh mất secondaryRoll / byTier).
     */
    static EquipStatRollConfig mergeEquipStatRoll(EquipStatRollConfig base, EquipStatRollConfig loaded) {
        if (loaded.primaryAnchor != null && !loaded.primaryAnchor.isEmpty())
            base.primaryAnchor = loaded.primaryAnchor;
        if (loaded.slotPrimaryMultiplier != null && !loaded.slotPrimaryMultiplier.isEmpty())
            base.slotPrimaryMultiplier = loaded.slotPrimaryMultiplier;
        if (loaded.secondaryRoll != null) {
            if (base.secondaryRoll == null)
                base.secondaryRoll = new SecondaryRollConfig();
            SecondaryRollConfig src = loaded.secondaryRoll;
            if (src.byTier != null && !src.byTier.isEmpty())
                base.secondaryRoll.byTier = src.byTier;
            if (src.point2Ratio != null)
                base.secondaryRoll.point2Ratio = src.point2Ratio;
            if (src.point3Ratio != null)
                base.secondaryRoll.point3Ratio = src.point3Ratio;
        }
        return base;
    }


    static EquipStatRollConfig defaultEquipStatRoll() {
        EquipStatRollConfig cfg = new EquipStatRollConfig();
        cfg.primaryAnchor = new HashMap<>();
        cfg.primaryAnchor.put("1", tierMap(
                range(5, 8), range(20, 28), range(28, 38), range(40, 48)));
        cfg.primaryAnchor.put("2", tierMap(
                range(48, 55), range(60, 68), range(85, 95), range(115, 130)));
        cfg.primaryAnchor.put("3", tierMap(
                range(2.5f, 3.5f), range(9f, 11f), range(14f, 16f), range(18.5f, 20f)));
        cfg.slotPrimaryMultiplier = new HashMap<>();
        cfg.slotPrimaryMultiplier.put("1", 1.00f);
        cfg.slotPrimaryMultiplier.put("2", 0.45f);
        cfg.slotPrimaryMultiplier.put("3", 1.15f);
        cfg.slotPrimaryMultiplier.put("4", 0.35f);
        cfg.slotPrimaryMultiplier.put("5", 0.70f);
        cfg.secondaryRoll = new SecondaryRollConfig();
        cfg.secondaryRoll.byTier = new HashMap<>();
        cfg.secondaryRoll.byTier.put("1", rates(50, 30));
        cfg.secondaryRoll.byTier.put("2", rates(60, 40));
        cfg.secondaryRoll.byTier.put("3", rates(70, 60));
        cfg.secondaryRoll.byTier.put("4", rates(100, 90));
        cfg.secondaryRoll.point2Ratio = range(0.18f, 0.28f);
        cfg.secondaryRoll.point3Ratio = range(0.07f, 0.12f);
        cfg.normalize();
        return cfg;

    }


    private static Map<String, Range> tierMap(Range... tiers) {
        Map<String, Range> m = new HashMap<>();
        for (int i = 0; i < tiers.length; i++)
            m.put(String.valueOf(i + 1), tiers[i]);
        return m;
    }


    private static Range range(float min, float max) {
        Range r = new Range();
        r.min = min;
        r.max = max;
        return r;
    }


    private static SecondaryRollTierRates rates(int point2Rate, int point3Rate) {
        SecondaryRollTierRates r = new SecondaryRollTierRates();
        r.point2Rate = point2Rate;
        r.point3Rate = point3Rate;
        return r;
    }

    static class ItemRootConfig {
        EquipStatRollConfig equipStatRoll;
    }


    @Data

    public static class EquipStatRollConfig {
        public Map<String, Map<String, Range>> primaryAnchor = new HashMap<>();
        public Map<String, Float> slotPrimaryMultiplier = new HashMap<>();
        public SecondaryRollConfig secondaryRoll = new SecondaryRollConfig();
        public float point2RatioMin = 0.18f;
        public float point2RatioMax = 0.28f;
        public float point3RatioMin = 0.07f;
        public float point3RatioMax = 0.12f;

        void normalize() {
            if (secondaryRoll != null) {
                if (secondaryRoll.point2Ratio != null) {
                    point2RatioMin = secondaryRoll.point2Ratio.min;
                    point2RatioMax = secondaryRoll.point2Ratio.max;
                }
                if (secondaryRoll.point3Ratio != null) {
                    point3RatioMin = secondaryRoll.point3Ratio.min;
                    point3RatioMax = secondaryRoll.point3Ratio.max;
                }
            }
        }


        public Range getPrimaryAnchor(int set, int tier) {
            Map<String, Range> bySet = primaryAnchor.get(String.valueOf(set));
            if (bySet == null)
                return null;
            return bySet.get(String.valueOf(tier));
        }


        public float getSlotPrimaryMultiplier(int type) {
            Float mul = slotPrimaryMultiplier.get(String.valueOf(type));
            return mul != null ? mul : 1f;
        }


        public SecondaryRollTierRates getSecondaryRates(int tier) {
            if (secondaryRoll == null || secondaryRoll.byTier == null)
                return null;
            return secondaryRoll.byTier.get(String.valueOf(tier));
        }

        /** Stat phụ thứ 2 — budget × point2Ratio. */
        public Range getSecondaryRange(float budget, int pointId) {
            return buildRatioRange(budget, point2RatioMin, point2RatioMax, pointId);
        }

        /** Stat phụ thứ 3 — budget × point3Ratio. */
        public Range getTertiaryRange(float budget, int pointId) {
            return buildRatioRange(budget, point3RatioMin, point3RatioMax, pointId);
        }

        private Range buildRatioRange(float budget, float ratioMin, float ratioMax, int pointId) {
            if (budget <= 0f)
                return null;
            Range r = new Range();
            r.min = CfgItem.formatPointStat(pointId, budget * ratioMin);
            r.max = CfgItem.formatPointStat(pointId, budget * ratioMax);
            if (r.min > r.max) {
                float t = r.min;
                r.min = r.max;
                r.max = t;
            }
            return r;
        }

    }


    @Data

    public static class Range {
        public float min;
        public float max;
    }


    @Data

    public static class SecondaryRollConfig {
        public Map<String, SecondaryRollTierRates> byTier = new HashMap<>();
        public Range point2Ratio;
        public Range point3Ratio;
    }


    @Data

    public static class SecondaryRollTierRates {
        public int point2Rate;
        public int point3Rate;
    }

}


