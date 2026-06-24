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
    public static final int TIER_COUNT = 4;
    public static final int AUTO_SELL_TYPE_ITEM = 1;
    public static final int AUTO_SELL_TYPE_MATERIAL = 2;

    public static final int MAX_LEVEL = 10;
    public static final double COST_LEVEL_MULT = 1.5;
    public static final float SOCKET_RATE_MIN = 0.30f;
    public static final float SOCKET_RATE_MAX = 0.36f;
    public static final double SOCKET_LEVEL_MULT = 1.1;

    private static long[][] upgradeBaseCost = defaultUpgradeBaseCost();
    private static MergeDataConfig mergeCfg = defaultMergeConfig();
    private static final List<Integer> SELL_PRICE_BASE_T1 = List.of(5, 10, 16, 25, 38, 57, 83, 120, 172, 246);

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
        } else if (root.minMaterials > 0 || (root.legendRecipes != null && !root.legendRecipes.isEmpty())) {
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

    public static int getAutoSellMaterialSize() {
        return ResItem.getMaterialCount() * TIER_COUNT;
    }

    public static boolean isValidAutoSellMaterialIndex(int index) {
        return index >= 0 && index < getAutoSellMaterialSize();
    }

    public static int toAutoSellMaterialIndex(int materialId, int tier) {
        List<Integer> ids = ResItem.getSortedMaterialIds();
        int materialIndex = ids.indexOf(materialId);
        if (materialIndex < 0 || tier < RANK_COMMON || tier > RANK_LEGEND) {
            return -1;
        }
        return materialIndex * TIER_COUNT + (tier - 1);
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

    /** Giá bán material — res_material.tier × base[level]; tier 1–2 vàng, tier 3–4 kim cương. */
    public static long getSellPrice(UserMaterialEntity gem) {
        if (gem == null)
            return 0;
        ResMaterialEntity res = gem.getRes();
        if (res == null)
            return 0;
        int gemTier = res.getTier();
        if (gemTier < 1)
            gemTier = 1;
        int level = gem.getLevel();
        if (level < 1)
            level = 1;
        int idx = Math.min(level, SELL_PRICE_BASE_T1.size()) - 1;
        return (long) gemTier * SELL_PRICE_BASE_T1.get(idx);
    }

    public static List<Long> getPriceSellMaterial(UserMaterialEntity gem) {
        return getPriceSellByTierAndLevel(getSellTier(gem), getSellLevel(gem));
    }

    static int getSellTier(UserMaterialEntity gem) {
        if (gem == null)
            return 0;
        ResMaterialEntity res = gem.getRes();
        if (res == null)
            return 0;
        int gemTier = res.getTier();
        return gemTier < 1 ? 1 : gemTier;
    }

    static int getSellLevel(UserMaterialEntity gem) {
        if (gem == null)
            return 1;
        int level = gem.getLevel();
        return level < 1 ? 1 : level;
    }

    /** Giá bán theo material tier × base[level] — tier 1–2 vàng, tier 3–4 gem. */
    public static List<Long> getPriceSellByTierAndLevel(int tier, int level) {
        if (tier < 1 || level < 1)
            return new ArrayList<>();
        int idx = Math.min(level, SELL_PRICE_BASE_T1.size()) - 1;
        long price = (long) tier * SELL_PRICE_BASE_T1.get(idx);
        if (price <= 0)
            return new ArrayList<>();
        if (tier <= 2)
            return Bonus.viewGold(price);
        return Bonus.viewGem((int) price);
    }

    public static boolean usesGemCurrency(ResMaterialEntity res) {
        return res != null && res.getTier() >= 3;
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

    // --- merge (point-based: rankPt = 7^(tier-1), levelPt = (level-1)*rankPt*levelPointMult) ---

    private static final double RANK_POINT_BASE = 7.0;

    public static int getMergeMinMaterials() {
        return mergeCfg.minMaterials;
    }

    public static int getMergeMaxMaterials() {
        return mergeCfg.maxMaterials;
    }

    public static long getMergeSellPrice(int tier, int level) {
        if (tier < 1 || tier > 4) {
            return 0;
        }
        long base = mergeCfg.sellBase[tier - 1][tier - 1];
        return base * Math.max(1, level);
    }

    public static long sumMergeSellPrice(List<UserMaterialEntity> gems) {
        long sum = 0;
        for (UserMaterialEntity g : gems) {
            if (g.getRes() != null) {
                sum += getMergeSellPrice(g.getTier(), g.getLevel());
            }
        }
        return sum;
    }

    /** Điểm rank: 7^(tier-1). Level: (level-1) × rankPt × levelPointMult. */
    public static double rankPointValue(int tier) {
        if (tier < 1 || tier > TIER_COUNT) {
            return 0;
        }
        return Math.pow(RANK_POINT_BASE, tier - 1);
    }

    public static double gemMergePoints(UserMaterialEntity gem) {
        if (gem == null) {
            return 0;
        }
        int tier = gem.getTier();
        if (tier < 1 || tier > TIER_COUNT) {
            return 0;
        }
        int level = Math.max(1, gem.getLevel());
        double rankPt = rankPointValue(tier);
        double levelPt = (level - 1) * rankPt * mergeCfg.levelPointMult;
        return rankPt + levelPt;
    }

    public static double sumMergePoints(List<UserMaterialEntity> gems) {
        double total = 0;
        for (UserMaterialEntity g : gems) {
            double pt = gemMergePoints(g);
            if (pt <= 0) {
                return 0;
            }
            total += pt;
        }
        return total;
    }

    /** Ngưỡng 100% lên rank minRank+1. */
    public static double upgradeThreshold(int minRank) {
        if (minRank < 1 || minRank >= TIER_COUNT) {
            return 0;
        }
        return Math.pow(RANK_POINT_BASE, minRank);
    }

    /** Ngưỡng 100% giữ cùng rank R (need = R+1 viên cùng rank). */
    public static double sameRankThreshold(int rank) {
        if (rank < 1 || rank > TIER_COUNT) {
            return 0;
        }
        return (rank + 1) * rankPointValue(rank);
    }

    static int pointsToRatePercent(double points, double threshold) {
        if (threshold <= 0 || points <= 0) {
            return 0;
        }
        return (int) Math.min(100, Math.floor(points / threshold * 100.0));
    }

    public static MergePlan buildMergePlan(List<UserMaterialEntity> gems) {
        if (gems == null || gems.size() < mergeCfg.minMaterials || gems.size() > mergeCfg.maxMaterials) {
            return null;
        }
        int count = gems.size();
        Map<Integer, Integer> rankCount = new HashMap<>();
        Map<Integer, Integer> materialCount = new HashMap<>();
        int minRank = TIER_COUNT;
        int maxRank = 0;
        for (UserMaterialEntity g : gems) {
            int r = g.getTier();
            if (r < RANK_COMMON || r > RANK_LEGEND) {
                return null;
            }
            rankCount.merge(r, 1, Integer::sum);
            materialCount.merge(g.getMaterialId(), 1, Integer::sum);
            minRank = Math.min(minRank, r);
            maxRank = Math.max(maxRank, r);
        }

        LegendRecipeRow legend = matchLegendRecipe(rankCount, count);
        if (legend != null) {
            int rate = Math.min(100, legend.rate + calcLegendLevelBonusPercent(gems));
            return new MergePlan(legend.outputRank, rate, true, materialCount, gems);
        }

        if (maxRank - minRank > 1) {
            return null;
        }
        if (minRank >= RANK_LEGEND) {
            return null;
        }

        double totalPoints = sumMergePoints(gems);
        if (totalPoints <= 0) {
            return null;
        }

        boolean upgradePath = shouldUseUpgradePath(minRank, maxRank, count, totalPoints);
        int outputRank;
        int rate;
        if (upgradePath) {
            outputRank = minRank + 1;
            rate = pointsToRatePercent(totalPoints, upgradeThreshold(minRank));
        } else {
            outputRank = maxRank;
            rate = pointsToRatePercent(totalPoints, sameRankThreshold(maxRank));
        }
        if (rate <= 0) {
            return null;
        }
        return new MergePlan(outputRank, rate, false, materialCount, gems);
    }

    /** Mixed rank → lên bậc; cùng rank → lên bậc khi đủ điểm 7^minRank hoặc ≥ upgradeMinCount viên. */
    static boolean shouldUseUpgradePath(int minRank, int maxRank, int count, double totalPoints) {
        if (minRank != maxRank) {
            return true;
        }
        if (totalPoints >= upgradeThreshold(minRank)) {
            return true;
        }
        return count >= mergeCfg.upgradeMinCount;
    }

    /** Level đã góp vào T; legend cộng thêm % từ phần level qua ngưỡng rank 2. */
    static int calcLegendLevelBonusPercent(List<UserMaterialEntity> gems) {
        double levelPoints = 0;
        for (UserMaterialEntity g : gems) {
            int tier = g.getTier();
            int level = Math.max(1, g.getLevel());
            if (tier < 1 || tier > TIER_COUNT) {
                continue;
            }
            levelPoints += (level - 1) * rankPointValue(tier) * mergeCfg.levelPointMult;
        }
        double threshold = upgradeThreshold(RANK_RARE);
        if (threshold <= 0) {
            return 0;
        }
        return (int) Math.floor(levelPoints / threshold * 100.0);
    }

    public static int calcMergeSuccessPercent(MergePlan plan) {
        return Math.min(100, Math.max(0, plan.baseRate));
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
            maxRank = Math.max(maxRank, g.getTier());
        }
        UserMaterialEntity best = null;
        for (UserMaterialEntity g : gems) {
            if (g.getTier() == maxRank) {
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
        if (loaded.levelPointMult > 0) {
            mergeCfg.levelPointMult = loaded.levelPointMult;
        }
        if (loaded.upgradeMinCount > 0) {
            mergeCfg.upgradeMinCount = loaded.upgradeMinCount;
        }
        if (loaded.sellBaseRows != null && !loaded.sellBaseRows.isEmpty()) {
            mergeCfg.sellBase = toGrid(loaded.sellBaseRows);
        } else if (loaded.sellBase != null && loaded.sellBase.length > 0) {
            mergeCfg.sellBase = loaded.sellBase;
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
        d.levelPointMult = 0.49;
        d.upgradeMinCount = 6;
        d.sellBaseRows = List.of(
                List.of(5L, 10L, 15L, 20L),
                List.of(10L, 20L, 30L, 40L),
                List.of(1L, 2L, 3L, 4L),
                List.of(2L, 3L, 5L, 6L)
        );
        d.sellBase = toGrid(d.sellBaseRows);
        d.legendRecipes = List.of(
                legendRow(7, 1, 3, 4),
                legendRow(6, 2, 10, 4),
                legendRow(5, 3, 20, 4),
                legendRow(4, 4, 40, 4)
        );
        return d;
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
            m.levelPointMult = levelPointMult;
            m.upgradeMinCount = upgradeMinCount;
            m.sellBaseRows = sellBaseRows;
            m.sellBase = sellBase;
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
        double levelPointMult = 0.49;
        int upgradeMinCount = 6;
        long[][] sellBase = new long[4][4];
        List<List<Long>> sellBaseRows = new ArrayList<>();
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
