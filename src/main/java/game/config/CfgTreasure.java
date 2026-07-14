package game.config;

import com.google.gson.Gson;
import lombok.Data;

/**
 * DB key {@code config_treasure} → {@link #loadConfig(String)}.
 * Rate phần nghìn (1000). TTL milli giây.
 */
public class CfgTreasure {
    public static DataConfig config;

    public static void loadConfig(String strJson) {
        config = new Gson().fromJson(strJson, DataConfig.class);
        if (config == null) config = defaults();
        fillDefaults(config);
    }

    public static DataConfig cfg() {
        if (config == null) {
            config = defaults();
        }
        return config;
    }

    public static int rateDropChest() {
        return Math.max(0, cfg().rateDropChest);
    }

    public static int rateDropKey() {
        return Math.max(0, cfg().rateDropKey);
    }

    public static long keyTtlMs() {
        return Math.max(1000L, cfg().keyTtlSec) * 1000L;
    }

    public static long chestTtlMs() {
        return Math.max(1000L, cfg().chestTtlSec) * 1000L;
    }

    public static long openChannelMs() {
        // Clamp 10..60s — mở rương phải đủ thời gian liên tục
        int sec = cfg().openChannelSec;
        if (sec < 10) sec = 10;
        if (sec > 60) sec = 10;
        return sec * 1000L;
    }

    public static int keySellGem() {
        return Math.max(0, cfg().keySellGem);
    }

    public static int keyItemId() {
        return cfg().keyItemId > 0 ? cfg().keyItemId : 9;
    }

    public static int openGemMin() {
        return Math.max(1, cfg().openGemMin);
    }

    public static int openGemMax() {
        return Math.max(openGemMin(), cfg().openGemMax);
    }

    public static int openMaterialIdMin() {
        return Math.max(1, cfg().openMaterialIdMin);
    }

    public static int openMaterialIdMax() {
        return Math.max(openMaterialIdMin(), cfg().openMaterialIdMax);
    }

    public static int openMaterialTier() {
        return Math.max(1, cfg().openMaterialTier);
    }

    static DataConfig defaults() {
        DataConfig d = new DataConfig();
        fillDefaults(d);
        return d;
    }

    static void fillDefaults(DataConfig d) {
        if (d.rateDropChest <= 0) d.rateDropChest = 10;
        if (d.rateDropKey <= 0) d.rateDropKey = 5;
        if (d.keyTtlSec <= 0) d.keyTtlSec = 300;
        if (d.chestTtlSec <= 0) d.chestTtlSec = 1200;
        if (d.openChannelSec <= 0) d.openChannelSec = 10;
        if (d.keySellGem <= 0) d.keySellGem = 5;
        if (d.keyItemId <= 0) d.keyItemId = 9;
        if (d.openGemMin <= 0) d.openGemMin = 20;
        if (d.openGemMax <= 0) d.openGemMax = 80;
        if (d.openMaterialIdMin <= 0) d.openMaterialIdMin = 1;
        if (d.openMaterialIdMax <= 0) d.openMaterialIdMax = 23;
        if (d.openMaterialTier <= 0) d.openMaterialTier = 4;
    }

    @Data
    public static class DataConfig {
        /** Phần nghìn — mặc định 10. */
        int rateDropChest;
        /** Phần nghìn — mặc định 5. */
        int rateDropKey;
        /** Chìa tồn tại (giây) — mặc định 300 = 5 phút. */
        int keyTtlSec;
        /** Rương tồn tại (giây) — mặc định 1200 = 20 phút. */
        int chestTtlSec;
        /** Đứng im mở rương (giây) — mặc định 10. */
        int openChannelSec;
        /** Bán chìa nhận gem — mặc định 5. */
        int keySellGem;
        int keyItemId;
        int openGemMin;
        int openGemMax;
        int openMaterialIdMin;
        int openMaterialIdMax;
        int openMaterialTier;
    }
}
