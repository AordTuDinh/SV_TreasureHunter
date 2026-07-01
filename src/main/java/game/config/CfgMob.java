package game.config;

import com.google.gson.Gson;
import game.treasure.mapping.UserMobEntity;
import game.treasure.mapping.main.ResMobEntity;
import game.treasure.service.resource.ResMob;
import game.treasure.service.user.Bonus;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Load from DB key {@code config_mob} → {@link #loadConfig(String)}. */
public class CfgMob {
  private static final float[] DEFAULT_TIER_MULT = {1f, 1.2f, 1.5f, 1.8f};
  private static float[] tierMult = Arrays.copyOf(DEFAULT_TIER_MULT, DEFAULT_TIER_MULT.length);

  public static void loadConfig(String strJson) {
    if (strJson == null || strJson.isBlank()) {
      return;
    }
    DataConfig loaded = new Gson().fromJson(strJson, DataConfig.class);
    if (loaded == null || loaded.tierMult == null || loaded.tierMult.isEmpty()) {
      return;
    }
    tierMult = new float[loaded.tierMult.size()];
    for (int i = 0; i < loaded.tierMult.size(); i++) {
      tierMult[i] = loaded.tierMult.get(i);
    }
  }

  public static float getTierMult(int tier) {
    if (tierMult == null || tierMult.length == 0) {
      return 1f;
    }
    int t = tier > 0 ? Math.min(tier, tierMult.length) : 1;
    return tierMult[t - 1];
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

  @Data
  public static class DataConfig {
    public List<Float> tierMult;
  }
}
