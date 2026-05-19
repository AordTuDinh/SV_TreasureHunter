package game.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import game.treasure.mapping.main.ResMaterialEntity;
import game.treasure.service.resource.ResItem;
import game.treasure.service.user.Bonus;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CfgMaterial {
    public static final int RANK_COMMON = 1;
    public static final int RANK_RARE = 2;
    public static final int RANK_EPIC = 3;
    public static final int RANK_LEGEND = 4;

    public static final int MAX_LEVEL = 10;
    public static final double COST_LEVEL_MULT = 1.5;

    /** upgradeBaseCost[tier-1][rank-1] — gold tier 1–2, gem tier 3–4 */
    private static long[][] upgradeBaseCost = defaultUpgradeBaseCost();

    public static void loadConfig(String strJson) {
        if (strJson == null || strJson.isBlank()) {
            return;
        }
        DataConfig cfg = new Gson().fromJson(strJson, DataConfig.class);
        if (cfg == null) {
            return;
        }
        if (cfg.upgradeBaseCost != null && !cfg.upgradeBaseCost.isEmpty()) {
            upgradeBaseCost = new long[4][4];
            for (UpgradeBaseCostRow row : cfg.upgradeBaseCost) {
                if (row.tier < 1 || row.tier > 4 || row.rank < 1 || row.rank > 4) {
                    continue;
                }
                upgradeBaseCost[row.tier - 1][row.rank - 1] = row.cost;
            }
        }
    }

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

    public static class DataConfig {
        public List<UpgradeBaseCostRow> upgradeBaseCost;
    }

    public static class UpgradeBaseCostRow {
        public int tier;
        public int rank;
        public long cost;
    }
}
