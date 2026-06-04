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
        return "[" + config.freeSlotBag + "," + config.freeSlotMaterial + "," + config.freeSlotEvent + "]";
    }

    public static class DataConfig {
        public int freeSlotBag;
        public int maxSlotBag = 16;
        public int freeSlotMaterial;
        public int freeSlotEvent;
    }
}
