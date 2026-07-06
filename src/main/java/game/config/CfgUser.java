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

    public static class DataConfig {
        public int freeSlotBag;
        public int freeSlotMaterial;
        public int freeSlotEvent;
        public int freeSlotTrading1 = 20;
        public int freeSlotTrading2 = 0;
    }
}
