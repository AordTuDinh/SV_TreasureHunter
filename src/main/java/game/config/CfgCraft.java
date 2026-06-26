package game.config;

import com.google.gson.Gson;
import game.config.aEnum.CraftTargetType;
import game.treasure.service.user.Bonus;
import lombok.Data;
import ozudo.base.helper.NumberUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Load from DB key {@code config_craft} → {@link #loadConfig(String)}.
 */
public class CfgCraft {

    private static DataConfig cfg = defaultConfig();
    private static Map<Integer, TargetConfig> targetByType = buildTargetMap(cfg);

    public static void loadConfig(String strJson) {
        if (strJson == null || strJson.isBlank()) {
            return;
        }
        DataConfig loaded = new Gson().fromJson(strJson, DataConfig.class);
        if (loaded == null) {
            return;
        }
        mergeConfig(loaded);
        targetByType = buildTargetMap(cfg);
    }

    public static int getMaxCraftLevel() {
        return cfg.maxCraftLevel;
    }

    public static int getExpToNext(int craftLevel) {
        if (craftLevel < 1 || craftLevel >= cfg.maxCraftLevel) {
            return 0;
        }
        return cfg.expToNext.get(craftLevel - 1);
    }

    public static int getLockedSocket(int craftLevel) {
        if (craftLevel < 1) {
            return cfg.lockedSocket.get(0);
        }
        if (craftLevel > cfg.maxCraftLevel) {
            return 0;
        }
        return cfg.lockedSocket.get(craftLevel - 1);
    }

    public static int getMaxSocket(CraftTargetType type) {
        if (type == null) {
            return 0;
        }
        TargetConfig t = targetByType.get(type.id);
        return t == null ? 0 : t.maxSocket;
    }

    public static int getCurSlot(CraftTargetType type, int craftLevel) {
        if (type == null) {
            return 0;
        }
        return Math.max(0, getMaxSocket(type) - getLockedSocket(craftLevel));
    }

    public static int getCraftLevelBonusPercent(int craftLevel) {
        if (craftLevel <= cfg.craftLevelBonusFromLevel) {
            return 0;
        }
        return (craftLevel - cfg.craftLevelBonusFromLevel) * cfg.craftLevelBonusPerLevel;
    }

    public static int getCraftExpByRank(int rank) {
        if (rank < 1 || rank >= cfg.expByRank.size()) {
            return 0;
        }
        return cfg.expByRank.get(rank);
    }

    public static boolean grantsCraftExp(int craftLevel, int materialRank) {
        if (craftLevel < cfg.minCraftLevelForRankExpGate) {
            return true;
        }
        return materialRank >= cfg.minMaterialRankForExp;
    }

    public static int getCraftSuccessPercent(CraftTargetType type, int maxGemRank, int itemLevel, int craftLevel) {
        int base = craftSuccessBase(maxGemRank);
        int levelBonus = Math.max(0, effectiveItemLevel(itemLevel) - 1);
        int smithBonus = getCraftLevelBonusPercent(craftLevel);
        return Math.min(100, base + levelBonus + smithBonus);
    }

    public static int craftSuccessBase(int rank) {
        if (rank < 1 || rank >= cfg.craftSuccessBaseByRank.size()) {
            return 0;
        }
        return cfg.craftSuccessBaseByRank.get(rank);
    }

    public static int effectiveItemLevel(int storedLevel) {
        return Math.max(1, storedLevel + cfg.itemLevelOffset);
    }

    public static boolean losesTargetOnCraftFail(CraftTargetType type) {
        TargetConfig t = type == null ? null : targetByType.get(type.id);
        return t != null && t.loseTargetOnCraftFail;
    }

    public static long getFeeAmount(CraftTargetType type, int rank) {
        TargetConfig t = type == null ? null : targetByType.get(type.id);
        if (t == null || rank < 1 || rank >= t.feeByRank.size()) {
            return 0;
        }
        return t.feeByRank.get(rank);
    }

    public static List<Long> getCraftFee(CraftTargetType type, int rank) {
        long amount = getFeeAmount(type, rank);
        if (amount <= 0) {
            return new ArrayList<>();
        }
        TargetConfig t = targetByType.get(type.id);
        if (t != null && "gold".equalsIgnoreCase(t.feeCurrency)) {
            return Bonus.viewGold(-amount);
        }
        return Bonus.viewGem((int) -amount);
    }

    public static List<Long> sumCraftFees(CraftTargetType type, List<Integer> ranks) {
        List<Long> total = new ArrayList<>();
        for (int rank : ranks) {
            List<Long> part = getCraftFee(type, rank);
            if (part.isEmpty()) {
                return new ArrayList<>();
            }
            if (total.isEmpty()) {
                total.addAll(part);
            } else {
                total.set(1, total.get(1) + part.get(1));
            }
        }
        return total;
    }

    /**
     * @return true if leveled up
     */
    public static boolean addCraftExp(game.treasure.mapping.UserDataEntity uData, int expGain) {
        if (expGain <= 0) {
            return false;
        }
        int oldLevel = uData.getCraftLevel();
        uData.setCraftExp(uData.getCraftExp() + expGain);
        boolean leveled = false;
        while (uData.getCraftLevel() < cfg.maxCraftLevel) {
            int need = getExpToNext(uData.getCraftLevel());
            if (need <= 0 || uData.getCraftExp() < need) {
                break;
            }
            uData.setCraftExp(uData.getCraftExp() - need);
            uData.setCraftLevel(uData.getCraftLevel() + 1);
            leveled = true;
        }
        return leveled || uData.getCraftLevel() != oldLevel;
    }

    /** @return 0 = no transform, 1..3 = hh tier */
    public static int rollTransformTier() {
        if (cfg.transformRate <= 0 || NumberUtil.getRandom(100) >= cfg.transformRate)
            return 0;
        if (cfg.transformTiers == null || cfg.transformTiers.isEmpty())
            return 0;
        int roll = NumberUtil.getRandom(100);
        int acc = 0;
        for (TransformTierConfig tier : cfg.transformTiers) {
            acc += tier.rate;
            if (roll < acc)
                return tier.tier;
        }
        return cfg.transformTiers.get(cfg.transformTiers.size() - 1).tier;
    }

    public static float getTransformStatMul(int tier) {
        if (cfg.transformTiers == null || tier <= 0)
            return 1f;
        for (TransformTierConfig t : cfg.transformTiers) {
            if (t.tier == tier)
                return t.statMul > 0 ? t.statMul : 1f;
        }
        return 1f;
    }

    private static void mergeConfig(DataConfig loaded) {
        if (loaded.maxCraftLevel > 0) {
            cfg.maxCraftLevel = loaded.maxCraftLevel;
        }
        if (loaded.minCraftLevelForRankExpGate > 0) {
            cfg.minCraftLevelForRankExpGate = loaded.minCraftLevelForRankExpGate;
        }
        if (loaded.minMaterialRankForExp > 0) {
            cfg.minMaterialRankForExp = loaded.minMaterialRankForExp;
        }
        if (loaded.craftLevelBonusFromLevel > 0) {
            cfg.craftLevelBonusFromLevel = loaded.craftLevelBonusFromLevel;
        }
        if (loaded.craftLevelBonusPerLevel > 0) {
            cfg.craftLevelBonusPerLevel = loaded.craftLevelBonusPerLevel;
        }
        if (loaded.itemLevelOffset != 0) {
            cfg.itemLevelOffset = loaded.itemLevelOffset;
        }
        if (loaded.expToNext != null && !loaded.expToNext.isEmpty()) {
            cfg.expToNext = loaded.expToNext;
        }
        if (loaded.lockedSocket != null && !loaded.lockedSocket.isEmpty()) {
            cfg.lockedSocket = loaded.lockedSocket;
        }
        if (loaded.craftSuccessBaseByRank != null && !loaded.craftSuccessBaseByRank.isEmpty()) {
            cfg.craftSuccessBaseByRank = loaded.craftSuccessBaseByRank;
        }
        if (loaded.expByRank != null && !loaded.expByRank.isEmpty()) {
            cfg.expByRank = loaded.expByRank;
        }
        if (loaded.targets != null && !loaded.targets.isEmpty()) {
            cfg.targets = loaded.targets;
        }
        if (loaded.transformRate > 0) {
            cfg.transformRate = loaded.transformRate;
        }
        if (loaded.transformTiers != null && !loaded.transformTiers.isEmpty()) {
            cfg.transformTiers = loaded.transformTiers;
        }
    }

    private static Map<Integer, TargetConfig> buildTargetMap(DataConfig data) {
        Map<Integer, TargetConfig> map = new HashMap<>();
        if (data.targets != null) {
            for (TargetConfig t : data.targets) {
                map.put(t.targetType, t);
            }
        }
        return map;
    }

    private static DataConfig defaultConfig() {
        DataConfig d = new DataConfig();
        d.maxCraftLevel = 10;
        d.minCraftLevelForRankExpGate = 5;
        d.minMaterialRankForExp = 3;
        d.craftLevelBonusFromLevel = 5;
        d.craftLevelBonusPerLevel = 5;
        d.itemLevelOffset = 1;
        d.expToNext = List.of(5, 15, 30, 45, 50, 75, 100, 125, 150, 175);
        d.lockedSocket = List.of(4, 3, 2, 1, 0, 0, 0, 0, 0, 0);
        d.craftSuccessBaseByRank = List.of(0, 75, 70, 65, 60);
        d.expByRank = List.of(0, 1, 2, 3, 4);
        d.transformRate = 20;
        d.transformTiers = List.of(
                transformTier(1, 50, 1.2f),
                transformTier(2, 35, 1.35f),
                transformTier(3, 15, 1.5f)
        );
        d.targets = List.of(
                target(1, 8, "gold", true, 0, 50, 100, 150, 200),
                target(2, 12, "gem", false, 0, 100, 200, 300, 400),
                target(3, 12, "gem", false, 0, 50, 100, 150, 200),
                target(4, 12, "gem", false, 0, 500, 1000, 1500, 2000),
                target(5, 12, "item_point", false, 0, 1000, 2000, 3000, 4000)
        );
        return d;
    }

    private static TransformTierConfig transformTier(int tier, int rate, float statMul) {
        TransformTierConfig t = new TransformTierConfig();
        t.tier = tier;
        t.rate = rate;
        t.statMul = statMul;
        return t;
    }

    private static TargetConfig target(int type, int maxSocket, String currency, boolean loseOnFail, int... fees) {
        TargetConfig t = new TargetConfig();
        t.targetType = type;
        t.maxSocket = maxSocket;
        t.feeCurrency = currency;
        t.loseTargetOnCraftFail = loseOnFail;
        t.feeByRank = new ArrayList<>();
        for (int fee : fees) {
            t.feeByRank.add(fee);
        }
        return t;
    }

    @Data
    public static class DataConfig {
        int maxCraftLevel = 10;
        int minCraftLevelForRankExpGate = 5;
        int minMaterialRankForExp = 3;
        int craftLevelBonusFromLevel = 5;
        int craftLevelBonusPerLevel = 5;
        /** effectiveLevel = storedLevel + itemLevelOffset */
        int itemLevelOffset = 1;
        List<Integer> expToNext = new ArrayList<>();
        List<Integer> lockedSocket = new ArrayList<>();
        /** index = material rank (1..4) */
        List<Integer> craftSuccessBaseByRank = new ArrayList<>();
        List<Integer> expByRank = new ArrayList<>();
        List<TargetConfig> targets = new ArrayList<>();
        int transformRate = 20;
        List<TransformTierConfig> transformTiers = new ArrayList<>();
    }

    @Data
    public static class TransformTierConfig {
        /** 1 = hh1, 2 = hh2, 3 = hh3 */
        int tier = 1;
        /** weight trong 100 khi đã trúng hóa hình */
        int rate;
        float statMul = 1f;
    }

    @Data
    public static class TargetConfig {
        int targetType;
        int maxSocket;
        String feeCurrency;
        boolean loseTargetOnCraftFail;
        /** index = rank (0 unused, 1=Common .. 4=Legend) */
        List<Integer> feeByRank = new ArrayList<>();
    }
}
