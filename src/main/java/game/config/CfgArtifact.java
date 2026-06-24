package game.config;

import game.treasure.mapping.UserArtifactEntity;
import game.treasure.mapping.main.ResArtifactEntity;
import game.treasure.service.user.Bonus;

import java.util.ArrayList;
import java.util.List;

/** Artifact upgrade — phí gem giống material res tier 4. */
public class CfgArtifact {
    public static final int RES_GEM_TIER = 4;
    public static final int MAX_LEVEL = CfgMaterial.MAX_LEVEL;

    public static boolean canUpgrade(int level) {
        return CfgMaterial.canUpgrade(level);
    }

    public static long getUpgradeCost(int rank, int currentLevel) {
        return CfgMaterial.getUpgradeCost(RES_GEM_TIER, rank, currentLevel);
    }

    public static List<Long> getUpgradeFee(ResArtifactEntity res, int currentLevel) {
        if (res == null)
            return List.of();
        int rank = res.getRank() > 0 ? res.getRank() : 1;
        return CfgMaterial.getUpgradeFee(RES_GEM_TIER, rank, currentLevel);
    }

    /** Giá bán artifact — material res tier 4 theo level. */
    public static List<Long> getPriceSellArtifact(UserArtifactEntity artifact) {
        if (artifact == null)
            return new ArrayList<>();
        int level = artifact.getLevel() > 0 ? artifact.getLevel() : 1;
        return CfgMaterial.getPriceSellByTierAndLevel(RES_GEM_TIER, level);
    }
}
