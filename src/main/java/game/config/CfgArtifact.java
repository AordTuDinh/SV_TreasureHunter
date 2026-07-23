package game.config;

import game.object.MyUser;
import game.treasure.mapping.UserArtifactEntity;
import game.treasure.service.user.Bonus;

import java.util.ArrayList;
import java.util.List;

/** Artifact upgrade/sell — phí bằng item point id=1 (Cổ vật). */
public class CfgArtifact {
    public static final int ARTIFACT_POINT_ID = 1;
    public static final int MAX_LEVEL = CfgMaterial.MAX_LEVEL;
    public static final int BASE_COST_PER_TIER = 100;
    public static final int CRAFT_COST_PER_TIER = 1000;
    public static final double COST_LEVEL_MULT = 1.4;
    public static final double SELL_LEVEL_MULT = 1.5;

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
        return getUpgradeFee(artifact, currentLevel, null);
    }

    public static List<Long> getUpgradeFee(UserArtifactEntity artifact, int currentLevel, MyUser mUser) {
        if (artifact == null)
            return List.of();
        int tier = artifact.getTier() > 0 ? artifact.getTier() : 1;
        long cost = CfgItem.applyUpgradeFeeVip(getUpgradeCost(tier, currentLevel), mUser);
        if (cost <= 0)
            return List.of();
        return Bonus.viewItemPoint(ARTIFACT_POINT_ID, -cost);
    }

    public static long getSellPrice(int tier, int level) {
        if (tier < 1 || tier > 4 || level < 1)
            return 0;
        long base = (long) BASE_COST_PER_TIER * tier;
        return Math.round(base * Math.pow(SELL_LEVEL_MULT, level - 1));
    }

    /** GAME_CONFIG index 7 — client đọc hệ số tính phí nâng/bán/chế tạo. */
    public static List<Integer> getGameConfigCoeffs() {
        return List.of(
                BASE_COST_PER_TIER,
                (int) Math.round(COST_LEVEL_MULT * 1000),
                (int) Math.round(SELL_LEVEL_MULT * 1000),
                CRAFT_COST_PER_TIER);
    }

    /** Giá bán artifact — nhận item point Cổ vật (id=1). */
    /** Phí chế tạo smithy — Cổ vật theo tier artifact (1000 × tier). */
    public static long getCraftCost(int tier) {
        if (tier < 1 || tier > 4)
            return 0;
        return (long) CRAFT_COST_PER_TIER * tier;
    }

    public static List<Long> getCraftFee(int tier) {
        return getCraftFee(tier, null);
    }

    public static List<Long> getCraftFee(int tier, MyUser mUser) {
        long cost = CfgCraft.applyCraftFeeVip(getCraftCost(tier), mUser);
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

    public static UserArtifactEntity getEquippedArtifact(MyUser mUser) {
        int rowId = Bonus.getEquippedArtifactRowId(mUser);
        if (rowId <= 0)
            return null;
        return mUser.getResources().getArtifact(rowId);
    }

    /** CD artifact đang mặc (giây) — từ user_artifact.data[idx_cd]. */
    public static long getEquippedArtifactCooldownSec(MyUser mUser) {
        UserArtifactEntity artifact = getEquippedArtifact(mUser);
        if (artifact == null)
            return 0;
        return Math.round(artifact.getEffectiveSlot(ArtifactDataSlot.IDX_CD));
    }

    public static boolean isArtifactOnCooldown(MyUser mUser) {
        long activeMs = mUser.getUData().getTimeActiveArtifact();
        if (activeMs <= 0)
            return false;
        long cdSec = getEquippedArtifactCooldownSec(mUser);
        if (cdSec <= 0)
            return false;
        return System.currentTimeMillis() < activeMs + cdSec * 1000L;
    }
}
