package game.config;

import com.google.gson.Gson;
import net.sf.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CfgAccount {
    public static JSONObject json;
    public static DataConfig config;

    public static boolean isAdmin(int userId) {
        return config.accountAdmin.contains(userId);
    }

    public static void loadConfig(String strJson) {
        json = JSONObject.fromObject(strJson);
        config = new Gson().fromJson(strJson, DataConfig.class);
    }

    public class DataConfig {
        public List<Integer> accountAdmin;
    }
}
