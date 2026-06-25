package game.config;

import game.treasure.mapping.UserArtifactEntity;
import game.treasure.service.user.Bonus;

import java.util.ArrayList;
import java.util.List;

/** Artifact upgrade/sell — phí bằng item point id=1 (Cổ vật). */
public class CfgArtifact {
    public static final int ARTIFACT_POINT_ID = 1;
    public static final int MAX_LEVEL = CfgMaterial.MAX_LEVEL;
    public static final int BASE_COST_PER_TIER = 100;
    public static final double COST_LEVEL_MULT = 1.4;
    private static final List<Integer> SELL_PRICE_BASE = List.of(5, 10, 16, 25, 38, 57, 83, 120, 172, 246);

    public static boolean canUpgrade(int level) {
        return CfgMaterial.canUpgrade(level);
    }

    public static long getUpgradeCost(int tier, int currentLevel) {
        if (tier < 1 || tier > 4 || currentLevel < 1)
            return 0;
        long base = (long) BASE_COST_PER_TIER * tier;
        return Math.round(base * Math.pow(COST_LEVEL_MULT, currentLevel - 1));
    }

    public static List<Long> getUpgradeFee(UserArtifactEntity artifact, int currentLevel) {
        if (artifact == null)
            return List.of();
        int tier = artifact.getTier() > 0 ? artifact.getTier() : 1;
        long cost = getUpgradeCost(tier, currentLevel);
        if (cost <= 0)
            return List.of();
        return Bonus.viewItemPoint(ARTIFACT_POINT_ID, -cost);
    }

    public static long getSellPrice(int tier, int level) {
        if (tier < 1 || level < 1)
            return 0;
        int idx = Math.min(level, SELL_PRICE_BASE.size()) - 1;
        return (long) tier * SELL_PRICE_BASE.get(idx);
    }

    /** Giá bán artifact — nhận item point Cổ vật (id=1). */
    /** Phí chế tạo smithy — Cổ vật theo tier artifact (1000 × tier). */
    public static long getCraftCost(int tier) {
        if (tier < 1 || tier > 4)
            return 0;
        return 1000L * tier;
    }

    public static List<Long> getCraftFee(int tier) {
        long cost = getCraftCost(tier);
        if (cost <= 0)
            return List.of();
        return Bonus.viewItemPoint(ARTIFACT_POINT_ID, -cost);
    }

    public static List<Long> getPriceSellArtifact(UserArtifactEntity artifact) {
        if (artifact == null)
            return new ArrayList<>();
        int tier = artifact.getTier() > 0 ? artifact.getTier() : 1;
        int level = artifact.getLevel() > 0 ? artifact.getLevel() : 1;
        long price = getSellPrice(tier, level);
        if (price <= 0)
            return new ArrayList<>();
        return Bonus.viewItemPoint(ARTIFACT_POINT_ID, price);
    }
}
