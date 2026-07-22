package game.config;

import com.google.gson.Gson;
import ozudo.base.helper.NumberUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Rate roll tier khi mở rương item point (OPEN_BOX).
 * DB key: {@code config_openBox} → {@link #loadConfig(String)}.
 * Default: 45 / 35 / 15 / 5 cho tier 1..4.
 */
public class CfgOpenBox {
    public static final int MAX_OPEN_PER_TURN = 100;

    private static List<Integer> tierRates = defaultRates();

    public static void loadConfig(String strJson) {
        if (strJson == null || strJson.isBlank()) {
            return;
        }
        DataConfig root = new Gson().fromJson(strJson, DataConfig.class);
        if (root == null) {
            return;
        }
        if (root.tierRates != null && !root.tierRates.isEmpty()) {
            List<Integer> next = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                int rate = i < root.tierRates.size() ? Math.max(0, root.tierRates.get(i)) : 0;
                next.add(rate);
            }
            tierRates = next;
        }
    }

    public static List<Integer> getTierRates() {
        return tierRates;
    }

    /** Roll material tier 1..4 theo {@link #tierRates}. */
    public static int rollTier() {
        List<Integer> rates = tierRates;
        if (rates == null || rates.isEmpty()) {
            return 1;
        }
        int total = 0;
        for (int r : rates) {
            total += Math.max(0, r);
        }
        if (total <= 0) {
            return 1;
        }
        int roll = NumberUtil.getRandom(total);
        int acc = 0;
        for (int i = 0; i < rates.size() && i < 4; i++) {
            acc += Math.max(0, rates.get(i));
            if (roll < acc) {
                return i + 1;
            }
        }
        return Math.min(4, rates.size());
    }

    static List<Integer> defaultRates() {
        return new ArrayList<>(Arrays.asList(45, 35, 15, 5));
    }

    public static class DataConfig {
        /** Rate tier 1..4 — vd. [45,35,15,5]. */
        public List<Integer> tierRates;
    }
}
