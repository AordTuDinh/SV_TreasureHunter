package game.config;

import com.google.gson.Gson;
import game.dragonhero.mapping.main.ResVipEntity;
import game.dragonhero.service.resource.ResEvent;

import java.util.List;

public class CfgUser {
    public static DataConfig config;
    public static final int maxLengthName = 28;
    public static final int maxLengthMail = 450;
    public static final int pointPerLevel = 3;

    public static void loadConfig(String strJson) {
        config = new Gson().fromJson(strJson, DataConfig.class);
    }


    public static long getExpByLevel(int curLevel) {
        if (curLevel > config.exp.size()) curLevel = config.exp.size();
        return config.exp.get(curLevel - 1);
    }


    public class DataConfig {
        public List<Long> exp;
        public List<Integer> heroStart;
    }
}
