package game.config;

import com.google.gson.Gson;

import java.util.List;

public class CfgUser {
    public static DataConfig config;
    public static final int maxLengthName = 28;
    public static final int maxLengthMail = 450;

    public static void loadConfig(String strJson) {
        config = new Gson().fromJson(strJson, DataConfig.class);
    }

    public static String getSlotBagInit(){
        return "[" + config.freeSlotBag + "," + config.freeSlotMaterial + "," + config.freeSlotEvent
                + "," + getDefaultSlotTrading1() + "," + getDefaultSlotTrading2() + "]";
    }

    public static int getDefaultSlotTrading1() {
        return config != null && config.freeSlotTrading1 > 0 ? config.freeSlotTrading1 : 20;
    }

    public static int getDefaultSlotTrading2() {
        return config != null ? config.freeSlotTrading2 : 0;
    }

    /** Số cup tối thiểu (sàn), dưới mức này không trừ thêm. */
    public static int getCupFloor() {
        return config != null ? config.cupFloor : 0;
    }

    /**
     * Tính cup chuyển khi killer hạ victim.
     * gain = clamp(round(cupBase + (victimCup - killerCup) / cupScale), cupMin, cupMax)
     */
    public static int calcPvpCupAmount(int victimCup, int killerCup) {
        int base = config != null ? config.cupBase : 2;
        int scale = config != null && config.cupScale > 0 ? config.cupScale : 300;
        int min = config != null ? config.cupMin : 1;
        int max = config != null ? config.cupMax : 3;
        double raw = base + (victimCup - killerCup) / (double) scale;
        return Math.max(min, Math.min(max, (int) Math.round(raw)));
    }

    public static class DataConfig {
        public int freeSlotBag;
        public int freeSlotMaterial;
        public int freeSlotEvent;
        public int freeSlotTrading1 = 20;
        public int freeSlotTrading2 = 0;
        /** Công thức PvP cup: clamp(round(cupBase + diff/cupScale), cupMin, cupMax) */
        public int cupBase = 2;
        public int cupScale = 300;
        public int cupMin = 1;
        public int cupMax = 3;
        public int cupFloor = 0;
    }
}
