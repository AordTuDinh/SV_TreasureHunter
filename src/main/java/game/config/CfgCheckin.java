package game.config;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class CfgCheckin {
    public static DataConfig config;
    public static int MONTH = 0;
    public static int DAY_CHECKIN = 1;
    public static int NUM_CHECKIN = 2;
    public static int STATUS = 3;//1 đã nhận,0 chưa nhận

    public static void loadConfig(String strJson) {
        config = new Gson().fromJson(strJson, DataConfig.class);
    }

    public static String getBonusCheckin() {
        return config.bonusCheckin.toString();
    }

    public class DataConfig {
        public List<List<Long>> bonusCheckin;
    }
}
