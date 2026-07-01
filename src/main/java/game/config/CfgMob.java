package game.config;

import game.treasure.mapping.UserMobEntity;
import game.treasure.mapping.main.ResMobEntity;
import game.treasure.service.resource.ResMob;
import game.treasure.service.user.Bonus;

import java.util.ArrayList;
import java.util.List;

public class CfgMob {
  /** Tier 1..4 → 1.0 / 1.2 / 1.5 / 1.8 */
  static final float[] TIER_MULT = {1f, 1.2f, 1.5f, 1.8f};

  public static float getTierMult(int tier) {
    int t = tier > 0 ? Math.min(tier, 4) : 1;
    return TIER_MULT[t - 1];
  }

  public static int scaleStat(int base, int tier) {
    if (base <= 0)
      return base;
    return Math.max(1, Math.round(base * getTierMult(tier)));
  }

  public static long scaleBonusAmount(long amount, int tier) {
    if (amount <= 0)
      return amount;
    return Math.max(1, Math.round(amount * getTierMult(tier)));
  }

  public static List<Long> getPriceSellMob(UserMobEntity mob) {
    if (mob == null)
      return new ArrayList<>();
    ResMobEntity res = ResMob.getMob(mob.getMobId());
    if (res == null || res.getPrice() <= 0)
      return new ArrayList<>();
    int price = Math.max(1, Math.round(res.getPrice() * getTierMult(mob.getTier())));
    return Bonus.viewGem(price);
  }
}
