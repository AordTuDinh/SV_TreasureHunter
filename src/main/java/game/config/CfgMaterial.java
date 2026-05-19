package game.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import game.treasure.mapping.UserMaterialEntity;
import game.treasure.mapping.main.ResMaterialEntity;
import game.treasure.service.resource.ResItem;
import game.treasure.service.user.Bonus;
import lombok.Data;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Material upgrade + merge.
 * DB keys: {@code config_material} (upgrade + optional {@code merge} block),
 * {@code config_material_merge} (merge only, via {@link #loadMergeConfig}).
 */
public class CfgMaterial {
    public static final int RANK_COMMON = 1;
    public static final int RANK_RARE = 2;
    public static final int RANK_EPIC = 3;
    public static final int RANK_LEGEND = 4;

    public static final int MAX_LEVEL = 10;
    public static final double COST_LEVEL_MULT = 1.5;
    public static final float SOCKET_RATE_MIN = 0.30f;
    public static final float SOCKET_RATE_MAX = 0.36f;
    public static final double SOCKET_LEVEL_MULT = 1.1;

    private static long[][] upgradeBaseCost = defaultUpgradeBaseCost();
    private static MergeDataConfig mergeCfg = defaultMergeConfig();

    // --- load config ---

    public static void loadConfig(String strJson) {
        if (strJson == null || strJson.isBlank()) {
            return;
        }
        MaterialRootConfig root = new Gson().fromJson(strJson, MaterialRootConfig.class);
        if (root == null) {
            return;
        }
        if (root.upgradeBaseCost != null && !root.upgradeBaseCost.isEmpty()) {
            upgradeBaseCost = new long[4][4];
            for (UpgradeBaseCostRow row : root.upgradeBaseCost) {
                if (row.tier < 1 || row.tier > 4 || row.rank < 1 || row.rank > 4) {
                    continue;
                }
                upgradeBaseCost[row.tier - 1][row.rank - 1] = row.cost;
            }
        }
        if (root.merge != null) {
            applyMergeConfig(root.merge);
        } else if (root.minMaterials > 0 || (root.standardRates != null && !root.standardRates.isEmpty())) {
            applyMergeConfig(root.toMergeConfig());
        }
    }

    /** DB key {@code config_material_merge} — chỉ phần merge. */
    public static void loadMergeConfig(String strJson) {
        if (strJson == null || strJson.isBlank()) {
            return;
        }
        MergeDataConfig loaded = new Gson().fromJson(strJson, MergeDataConfig.class);
        if (loaded != null) {
            applyMergeConfig(loaded);
        }
    }

    // --- upgrade ---

    public static ResMaterialEntity get(int materialId) {
        return ResItem.getMaterial(materialId);
    }

    public static boolean canUpgrade(int level) {
        return level > 0 && level < MAX_LEVEL;
    }

    public static long getUpgradeCost(int tier, int rank, int currentLevel) {
        long base = getBaseCost(tier, rank);
        if (base <= 0 || currentLevel < 1) {
            return 0;
        }
        return Math.round(base * Math.pow(COST_LEVEL_MULT, currentLevel - 1));
    }

    public static List<Long> getUpgradeFee(int tier, int rank, int currentLevel) {
        long cost = getUpgradeCost(tier, rank, currentLevel);
        if (cost <= 0) {
            return new ArrayList<>();
        }
        if (tier <= 2) {
            return Bonus.viewGold(-cost);
        }
        return Bonus.viewGem((int) -cost);
    }

    public static float rollSocketRate() {
        return NumberUtil.getRandom(SOCKET_RATE_MIN, SOCKET_RATE_MAX);
    }

    public static float getSocketSuccessPercent(float storedRate, int level) {
        if (storedRate <= 0 || level < 1) {
            return 0f;
        }
        return (float) (storedRate * 100f * Math.pow(SOCKET_LEVEL_MULT, level - 1));
    }

    public static float nextSocketRate(float currentRate) {
        return (float) (currentRate * SOCKET_LEVEL_MULT);
    }

    public static float rollValue(ResMaterialEntity res, int rank) {
        if (res == null) {
            return 0f;
        }
        double[] range = parseBasePoint(res);
        double base = NumberUtil.getRandom((float) range[0], (float) range[1]);
        return (float) (base * getMultiplier(res, rank));
    }

    public static double getMultiplier(ResMaterialEntity res, int rank) {
        return switch (rank) {
            case RANK_RARE -> res.getRare();
            case RANK_EPIC -> res.getEpic();
            case RANK_LEGEND -> res.getLegend();
            default -> 1d;
        };
    }

    public static double[] parseBasePoint(ResMaterialEntity res) {
        String basePoint = res.getBasePoint();
        JsonArray arr = GsonUtil.parseJsonArray(basePoint == null || basePoint.isEmpty() ? "[0,0]" : basePoint);
        double min = arr.size() > 0 ? arr.get(0).getAsDouble() : 0;
        double max = arr.size() > 1 ? arr.get(1).getAsDouble() : min;
        if (min > max) {
            double t = min;
            min = max;
            max = t;
        }
        return new double[]{min, max};
    }

    static long getBaseCost(int tier, int rank) {
        if (tier < 1 || tier > 4 || rank < 1 || rank > 4) {
            return 0;
        }
        return upgradeBaseCost[tier - 1][rank - 1];
    }

    static long[][] defaultUpgradeBaseCost() {
        return new long[][]{
                {10, 20, 30, 40},
                {20, 40, 60, 80},
                {2, 4, 6, 8},
                {3, 6, 9, 12},
        };
    }

    // --- merge ---

    public static int getMergeMinMaterials() {
        return mergeCfg.minMaterials;
    }

    public static int getMergeMaxMaterials() {
        return mergeCfg.maxMaterials;
    }

    public static long getMergeSellPrice(int tier, int rank, int level) {
        if (tier < 1 || tier > 4 || rank < 1 || rank > 4) {
            return 0;
        }
        long base = mergeCfg.sellBase[tier - 1][rank - 1];
        return base * Math.max(1, level);
    }

    public static long sumMergeSellPrice(List<UserMaterialEntity> gems) {
        long sum = 0;
        for (UserMaterialEntity g : gems) {
            if (g.getRes() != null) {
                sum += getMergeSellPrice(g.getTier(), g.getRank(), g.getLevel());
            }
        }
        return sum;
    }

    public static MergePlan buildMergePlan(List<UserMaterialEntity> gems) {
        if (gems == null || gems.size() < mergeCfg.minMaterials || gems.size() > mergeCfg.maxMaterials) {
            return null;
        }
        int count = gems.size();
        Map<Integer, Integer> rankCount = new HashMap<>();
        Map<Integer, Integer> materialCount = new HashMap<>();
        int minRank = 4;
        int maxRank = 0;
        for (UserMaterialEntity g : gems) {
            int r = g.getRank();
            rankCount.merge(r, 1, Integer::sum);
            materialCount.merge(g.getMaterialId(), 1, Integer::sum);
            minRank = Math.min(minRank, r);
            maxRank = Math.max(maxRank, r);
        }

        LegendRecipeRow legend = matchLegendRecipe(rankCount, count);
        if (legend != null) {
            return new MergePlan(legend.outputRank, legend.rate, true, materialCount, gems);
        }

        if (minRank != maxRank || minRank >= 4) {
            return null;
        }
        int rate = getStandardMergeRate(minRank, count);
        if (rate <= 0) {
            return null;
        }
        return new MergePlan(minRank + 1, rate, false, materialCount, gems);
    }

    public static int calcMergeSuccessPercent(MergePlan plan) {
        int rate = plan.baseRate;
        for (UserMaterialEntity g : plan.gems) {
            int lv = g.getLevel();
            if (lv > 1) {
                rate += (lv - 1) * mergeCfg.levelBonusPerLevelTier1;
            }
            if (lv > 2) {
                rate += (lv - 2) * mergeCfg.levelBonusPerLevelTier2;
            }
        }
        return Math.min(100, rate);
    }

    public static int pickMergeOutputMaterialId(Map<Integer, Integer> materialCount) {
        if (materialCount.size() == 1) {
            return materialCount.keySet().iterator().next();
        }
        List<Integer> ids = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : materialCount.entrySet()) {
            for (int i = 0; i < e.getValue(); i++) {
                ids.add(e.getKey());
            }
        }
        return ids.get(NumberUtil.getRandom(ids.size()));
    }

    public static UserMaterialEntity pickMergeFailReturnGem(List<UserMaterialEntity> gems) {
        int maxRank = 0;
        for (UserMaterialEntity g : gems) {
            maxRank = Math.max(maxRank, g.getRank());
        }
        UserMaterialEntity best = null;
        for (UserMaterialEntity g : gems) {
            if (g.getRank() == maxRank) {
                if (best == null || g.getLevel() > best.getLevel()) {
                    best = g;
                }
            }
        }
        return best;
    }

    private static LegendRecipeRow matchLegendRecipe(Map<Integer, Integer> rankCount, int total) {
        int rare = rankCount.getOrDefault(2, 0);
        int epic = rankCount.getOrDefault(3, 0);
        if (mergeCfg.legendRecipes == null) {
            return null;
        }
        for (LegendRecipeRow r : mergeCfg.legendRecipes) {
            if (rare == r.rareCount && epic == r.epicCount && rare + epic == total) {
                return r;
            }
        }
        return null;
    }

    private static int getStandardMergeRate(int inputRank, int count) {
        Map<String, Integer> row = mergeCfg.standardRates.get(String.valueOf(inputRank));
        if (row == null) {
            return 0;
        }
        Integer exact = row.get(String.valueOf(count));
        if (exact != null) {
            return exact;
        }
        for (int c = count; c >= mergeCfg.minMaterials; c--) {
            Integer v = row.get(String.valueOf(c));
            if (v != null) {
                return v;
            }
        }
        return 0;
    }

    private static void applyMergeConfig(MergeDataConfig loaded) {
        if (loaded.minMaterials > 0) {
            mergeCfg.minMaterials = loaded.minMaterials;
        }
        if (loaded.maxMaterials > 0) {
            mergeCfg.maxMaterials = loaded.maxMaterials;
        }
        if (loaded.levelBonusPerLevelTier1 > 0) {
            mergeCfg.levelBonusPerLevelTier1 = loaded.levelBonusPerLevelTier1;
        }
        if (loaded.levelBonusPerLevelTier2 >= 0) {
            mergeCfg.levelBonusPerLevelTier2 = loaded.levelBonusPerLevelTier2;
        }
        if (loaded.sellBaseRows != null && !loaded.sellBaseRows.isEmpty()) {
            mergeCfg.sellBase = toGrid(loaded.sellBaseRows);
        } else if (loaded.sellBase != null && loaded.sellBase.length > 0) {
            mergeCfg.sellBase = loaded.sellBase;
        }
        if (loaded.standardRates != null && !loaded.standardRates.isEmpty()) {
            mergeCfg.standardRates = loaded.standardRates;
        }
        if (loaded.legendRecipes != null && !loaded.legendRecipes.isEmpty()) {
            mergeCfg.legendRecipes = loaded.legendRecipes;
        }
    }

    private static long[][] toGrid(List<List<Long>> rows) {
        long[][] g = new long[4][4];
        for (int i = 0; i < Math.min(4, rows.size()); i++) {
            List<Long> row = rows.get(i);
            if (row == null) {
                continue;
            }
            for (int j = 0; j < Math.min(4, row.size()); j++) {
                g[i][j] = row.get(j);
            }
        }
        return g;
    }

    private static MergeDataConfig defaultMergeConfig() {
        MergeDataConfig d = new MergeDataConfig();
        d.minMaterials = 2;
        d.maxMaterials = 8;
        d.levelBonusPerLevelTier1 = 5;
        d.levelBonusPerLevelTier2 = 1;
        d.sellBaseRows = List.of(
                List.of(5L, 10L, 15L, 20L),
                List.of(10L, 20L, 30L, 40L),
                List.of(1L, 2L, 3L, 4L),
                List.of(2L, 3L, 5L, 6L)
        );
        d.sellBase = toGrid(d.sellBaseRows);
        d.standardRates = new HashMap<>();
        d.standardRates.put("1", mapRates("3", 10, "4", 25, "5", 50, "6", 100, "7", 100, "8", 100));
        d.standardRates.put("2", mapRates("3", 5, "4", 15, "5", 35, "6", 100, "7", 100, "8", 100));
        d.standardRates.put("3", mapRates("3", 2, "4", 8, "5", 20, "6", 100, "7", 100, "8", 100));
        d.legendRecipes = List.of(
                legendRow(7, 1, 3, 4),
                legendRow(6, 2, 10, 4),
                legendRow(5, 3, 20, 4),
                legendRow(4, 4, 40, 4)
        );
        return d;
    }

    private static Map<String, Integer> mapRates(Object... kv) {
        Map<String, Integer> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(String.valueOf(kv[i]), ((Number) kv[i + 1]).intValue());
        }
        return m;
    }

    private static LegendRecipeRow legendRow(int rare, int epic, int rate, int outputRank) {
        LegendRecipeRow r = new LegendRecipeRow();
        r.rareCount = rare;
        r.epicCount = epic;
        r.rate = rate;
        r.outputRank = outputRank;
        return r;
    }

    // --- config DTOs ---

    public static class MaterialRootConfig extends MergeDataConfig {
        public List<UpgradeBaseCostRow> upgradeBaseCost;
        public MergeDataConfig merge;

        MergeDataConfig toMergeConfig() {
            MergeDataConfig m = new MergeDataConfig();
            m.minMaterials = minMaterials;
            m.maxMaterials = maxMaterials;
            m.levelBonusPerLevelTier1 = levelBonusPerLevelTier1;
            m.levelBonusPerLevelTier2 = levelBonusPerLevelTier2;
            m.sellBaseRows = sellBaseRows;
            m.sellBase = sellBase;
            m.standardRates = standardRates;
            m.legendRecipes = legendRecipes;
            return m;
        }
    }

    public static class UpgradeBaseCostRow {
        public int tier;
        public int rank;
        public long cost;
    }

    @Data
    public static class MergeDataConfig {
        int minMaterials = 2;
        int maxMaterials = 8;
        int levelBonusPerLevelTier1 = 5;
        int levelBonusPerLevelTier2 = 1;
        long[][] sellBase = new long[4][4];
        List<List<Long>> sellBaseRows = new ArrayList<>();
        Map<String, Map<String, Integer>> standardRates = new HashMap<>();
        List<LegendRecipeRow> legendRecipes = new ArrayList<>();
    }

    @Data
    public static class LegendRecipeRow {
        int rareCount;
        int epicCount;
        int rate;
        int outputRank;
    }

    public static class MergePlan {
        public final int outputRank;
        public final int baseRate;
        public final boolean legendJump;
        public final Map<Integer, Integer> materialCount;
        public final List<UserMaterialEntity> gems;

        public MergePlan(int outputRank, int baseRate, boolean legendJump,
                         Map<Integer, Integer> materialCount, List<UserMaterialEntity> gems) {
            this.outputRank = outputRank;
            this.baseRate = baseRate;
            this.legendJump = legendJump;
            this.materialCount = materialCount;
            this.gems = gems;
        }
    }
}
