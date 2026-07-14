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
    /** Số hàng res_material.tier (loại nguyên liệu: 1–4). */
    public static final int RES_TIER_COUNT = 4;
    public static final int AUTO_SELL_TYPE_ITEM = 1;
    public static final int AUTO_SELL_TYPE_MATERIAL = 2;

    public static final int MAX_LEVEL = 10;
    public static final double COST_LEVEL_MULT = 1.5;
    /** Rank > 4: base cost / stat mul = rank trước × 1.25 (khi không khai báo trong config). */
    public static final double RANK_STEP_MULT = 1.25;
    public static final int DEFAULT_MAX_MATERIAL_RANK = 5;
    public static final float SOCKET_RATE_MIN = 0.30f;
    public static final float SOCKET_RATE_MAX = 0.36f;
    public static final double SOCKET_LEVEL_MULT = 1.1;
    /** Thu hồi khi bán material = % tổng chi phí nâng đã bỏ ra (config: sellRecoveryRate). */
    public static final double DEFAULT_SELL_RECOVERY_RATE = 0.5;

    private static long[][] upgradeBaseCost = defaultUpgradeBaseCost();
    private static MergeDataConfig mergeCfg = defaultMergeConfig();
    private static double sellRecoveryRate = DEFAULT_SELL_RECOVERY_RATE;
    private static int maxMaterialRank = DEFAULT_MAX_MATERIAL_RANK;

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
            for (UpgradeBaseCostRow row : root.upgradeBaseCost) {
                if (row.tier < 1 || row.tier > RES_TIER_COUNT || row.rank < 1 || row.cost <= 0) {
                    continue;
                }
                setUpgradeBaseCost(row.tier, row.rank, row.cost);
                maxMaterialRank = Math.max(maxMaterialRank, row.rank);
            }
        }
        if (root.maxMaterialRank > 0) {
            maxMaterialRank = root.maxMaterialRank;
        }
        if (root.sellRecoveryRate > 0) {
            sellRecoveryRate = root.sellRecoveryRate;
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

    public static int getMaxMaterialRank() {
        return maxMaterialRank;
    }

    public static int getAutoSellMaterialSize() {
        return ResItem.getMaterialCount() * getMaxMaterialRank();
    }

    public static boolean isValidAutoSellMaterialIndex(int index) {
        return index >= 0 && index < getAutoSellMaterialSize();
    }

    /** @param materialRank user_material.tier (rank Common..N) */
    public static int toAutoSellMaterialIndex(int materialId, int materialRank) {
        List<Integer> ids = ResItem.getSortedMaterialIds();
        int materialIndex = ids.indexOf(materialId);
        if (materialIndex < 0 || materialRank < RANK_COMMON || materialRank > getMaxMaterialRank()) {
            return -1;
        }
        return materialIndex * getMaxMaterialRank() + (materialRank - 1);
    }

    public static boolean canUpgrade(int level) {
        return level > 0 && level < MAX_LEVEL;
    }

    /** Làm tròn xuống bội số 4 — khớp bảng phí nâng material (vd. 18→16, 27→24). */
    static long floorTo4(double value) {
        if (value <= 0) {
            return 0;
        }
        return ((long) value / 4) * 4;
    }

    public static long getUpgradeCost(int tier, int rank, int currentLevel) {
        long base = getBaseCost(tier, rank);
        if (base <= 0 || currentLevel < 1) {
            return 0;
        }
        return floorTo4(base * Math.pow(COST_LEVEL_MULT, currentLevel - 1));
    }

    /** Tổng phí nâng từ lv1 đến trước level hiện tại (đã đầu tư). */
    public static long sumUpgradeCost(int tier, int rank, int level) {
        if (level <= 1) {
            return 0;
        }
        long sum = 0;
        for (int l = 1; l < level; l++) {
            sum += getUpgradeCost(tier, rank, l);
        }
        return sum;
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

    /**
     * Giá bán material — max(sàn, floorTo4(tổng phí nâng × sellRecoveryRate)).
     * Sàn: rank (tier 3–4 gem) hoặc rank×2 (tier 1–2 vàng).
     */
    public static long getSellPrice(UserMaterialEntity gem) {
        if (gem == null)
            return 0;
        ResMaterialEntity res = gem.getRes();
        if (res == null)
            return 0;
        int resTier = res.getTier();
        if (resTier < 1)
            resTier = 1;
        int rank = gem.getTier();
        if (rank < 1)
            rank = 1;
        int level = gem.getLevel();
        if (level < 1)
            level = 1;
        long floorSell = resTier <= 2 ? (long) rank * 2 : rank;
        long invested = sumUpgradeCost(resTier, rank, level);
        long recovery = floorTo4(invested * sellRecoveryRate);
        return Math.max(floorSell, recovery);
    }

    public static List<Long> getPriceSellMaterial(UserMaterialEntity gem) {
        if (gem == null)
            return new ArrayList<>();
        long price = getSellPrice(gem);
        if (price <= 0)
            return new ArrayList<>();
        ResMaterialEntity res = gem.getRes();
        int resTier = res != null && res.getTier() >= 1 ? res.getTier() : 1;
        if (resTier <= 2)
            return Bonus.viewGold(price);
        return Bonus.viewGem((int) price);
    }

    /** Giá bán preview — tier 1–2 vàng, tier 3–4 gem. */
    public static List<Long> getPriceSellByTierRankAndLevel(int tier, int rank, int level) {
        if (tier < 1 || rank < 1 || level < 1)
            return new ArrayList<>();
        long floorSell = tier <= 2 ? (long) rank * 2 : rank;
        long recovery = floorTo4(sumUpgradeCost(tier, rank, level) * sellRecoveryRate);
        long price = Math.max(floorSell, recovery);
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
        if (res == null || rank < RANK_COMMON) {
            return 1d;
        }
        if (rank == RANK_COMMON) {
            return 1d;
        }
        if (rank == RANK_RARE) {
            return positiveOr1(res.getRare());
        }
        if (rank == RANK_EPIC) {
            return positiveOr1(res.getEpic());
        }
        if (rank == RANK_LEGEND) {
            return positiveOr1(res.getLegend());
        }
        double mul = positiveOr1(res.getLegend());
        for (int r = RANK_LEGEND + 1; r <= rank; r++) {
            mul *= RANK_STEP_MULT;
        }
        return mul;
    }

    static double positiveOr1(double v) {
        return v > 0 ? v : 1d;
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

    static long getBaseCost(int resTier, int rank) {
        if (resTier < 1 || resTier > RES_TIER_COUNT || rank < 1) {
            return 0;
        }
        long cost = getConfiguredBaseCost(resTier, 1);
        if (cost <= 0) {
            return 0;
        }
        for (int r = 2; r <= rank; r++) {
            long explicit = getConfiguredBaseCost(resTier, r);
            if (explicit > 0) {
                cost = explicit;
            } else {
                cost = scaleRankCostStep(cost);
            }
        }
        return cost;
    }

    static long scaleRankCostStep(long prev) {
        return Math.round(prev * RANK_STEP_MULT);
    }

    static long getConfiguredBaseCost(int resTier, int rank) {
        if (resTier < 1 || resTier > upgradeBaseCost.length) {
            return 0;
        }
        long[] row = upgradeBaseCost[resTier - 1];
        if (rank < 1 || rank > row.length) {
            return 0;
        }
        return row[rank - 1];
    }

    static void setUpgradeBaseCost(int resTier, int rank, long cost) {
        ensureUpgradeMatrix(resTier, rank);
        upgradeBaseCost[resTier - 1][rank - 1] = cost;
    }

    static void ensureUpgradeMatrix(int resTier, int rank) {
        while (upgradeBaseCost.length < resTier) {
            long[][] next = new long[upgradeBaseCost.length + 1][];
            System.arraycopy(upgradeBaseCost, 0, next, 0, upgradeBaseCost.length);
            next[upgradeBaseCost.length] = new long[Math.max(rank, 4)];
            upgradeBaseCost = next;
        }
        long[] row = upgradeBaseCost[resTier - 1];
        if (row.length < rank) {
            long[] expanded = new long[rank];
            System.arraycopy(row, 0, expanded, 0, row.length);
            upgradeBaseCost[resTier - 1] = expanded;
        }
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

    public static long getMergeSellPrice(int resTier, int level) {
        if (resTier < 1 || resTier > RES_TIER_COUNT) {
            return 0;
        }
        long base = mergeCfg.sellBase[resTier - 1][resTier - 1];
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
    public static double rankPointValue(int rank) {
        if (rank < 1) {
            return 0;
        }
        return Math.pow(RANK_POINT_BASE, rank - 1);
    }

    public static double gemMergePoints(UserMaterialEntity gem) {
        if (gem == null) {
            return 0;
        }
        int rank = gem.getTier();
        if (rank < RANK_COMMON) {
            return 0;
        }
        int level = Math.max(1, gem.getLevel());
        double rankPt = rankPointValue(rank);
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

    /** Điểm rate — chỉ tính rank, bỏ qua level. */
    public static double sumMergeRankPoints(List<UserMaterialEntity> gems) {
        double total = 0;
        for (UserMaterialEntity g : gems) {
            if (g == null) {
                continue;
            }
            double pt = rankPointValue(g.getTier());
            if (pt <= 0) {
                return 0;
            }
            total += pt;
        }
        return total;
    }

    static int pointsToRatePercent(double points, double threshold) {
        if (threshold <= 0 || points <= 0) {
            return 0;
        }
        return (int) Math.min(100, Math.floor(points / threshold * 100.0));
    }

    /** Ngưỡng 100% rate: upgradeMinCount × 7^(outputRank-2) — 6 viên cùng rank Lv1 = 100%. */
    static double mergeRateThreshold(int outputRank) {
        if (outputRank < RANK_RARE) {
            return 0;
        }
        return mergeCfg.upgradeMinCount * Math.pow(RANK_POINT_BASE, outputRank - 2);
    }

    public static MergePlan buildMergePlan(List<UserMaterialEntity> gems) {
        if (gems == null || gems.size() < mergeCfg.minMaterials || gems.size() > mergeCfg.maxMaterials) {
            return null;
        }
        int count = gems.size();
        Map<Integer, Integer> materialCount = new HashMap<>();
        int minRank = getMaxMaterialRank();
        int maxRank = 0;
        for (UserMaterialEntity g : gems) {
            int r = g.getTier();
            if (r < RANK_COMMON) {
                return null;
            }
            materialCount.merge(g.getMaterialId(), 1, Integer::sum);
            minRank = Math.min(minRank, r);
            maxRank = Math.max(maxRank, r);
        }

        double totalPoints = sumMergePoints(gems);
        if (totalPoints <= 0) {
            return null;
        }

        int outputRank = resolveMergeOutputRank(totalPoints, minRank, maxRank, count);
        double ratePoints = sumMergeRankPoints(gems);
        int rate = pointsToRatePercent(ratePoints, mergeRateThreshold(outputRank));
        if (rate <= 0) {
            return null;
        }
        return new MergePlan(outputRank, rate, false, materialCount, gems);
    }

    /**
     * Rank đích từ tổng điểm: rank cao nhất R với T ≥ 7^(R-1).
     * Cùng rank và ≥ upgradeMinCount viên → ít nhất minRank+1.
     */
    static int resolveMergeOutputRank(double totalPoints, int minRank, int maxRank, int count) {
        int cap = getMaxMaterialRank();
        int fromPoints = RANK_RARE;
        for (int r = cap; r >= RANK_RARE; r--) {
            if (totalPoints >= rankPointValue(r)) {
                fromPoints = r;
                break;
            }
        }
        if (minRank == maxRank && count >= mergeCfg.upgradeMinCount && minRank < cap) {
            fromPoints = Math.max(fromPoints, minRank + 1);
        }
        return Math.min(cap, Math.max(RANK_RARE, fromPoints));
    }

    public static int calcMergeSuccessPercent(MergePlan plan) {
        return Math.min(100, Math.max(0, plan.baseRate));
    }

    /** Roll pass → outputRank; fail → viên mới rank thấp hơn 1 bậc (tối thiểu Common). */
    public static int resolveMergeResultRank(MergePlan plan, boolean success) {
        if (success) {
            return plan.outputRank;
        }
        return Math.max(RANK_COMMON, plan.outputRank - 1);
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
        /** Thu hồi khi bán = tổng phí nâng × rate (mặc định 0.5). */
        public double sellRecoveryRate;
        /** Rank material tối đa (auto-sell, merge cap). Mặc định 5. */
        public int maxMaterialRank;

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
