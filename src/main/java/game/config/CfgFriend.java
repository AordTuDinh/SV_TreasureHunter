package game.config;

import com.google.gson.Gson;
import game.dragonhero.mapping.UserEntity;
import net.sf.json.JSONObject;
import ozudo.base.database.DBJPA;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CfgFriend {
    public static JSONObject json;
    public static DataConfig config;
    //
    public static final boolean FRIEND_GLOBAL = false;
    private static List<UserEntity> aSuggestUser = new ArrayList<>();
    private static List<UserEntity> aSuggestUserTest = new ArrayList<>();
    private static long lastCache = 0;

    public static void loadConfig(String strJson) {
        config = new Gson().fromJson(strJson, DataConfig.class);
    }

    public static List<UserEntity> getASuggestUser(int server) {
        if (System.currentTimeMillis() - lastCache > 10000 || aSuggestUser.isEmpty()) {
            lastCache = System.currentTimeMillis();
            cacheSuggestUser(server);
        }
        List<UserEntity> randomUser = new ArrayList<>(server < 0 ? aSuggestUserTest : aSuggestUser);
        Collections.shuffle(randomUser);
        return randomUser.subList(0, randomUser.size() > 20 ? 20 : randomUser.size());
    }

    private static void cacheSuggestUser(int serverId) {
        List<UserEntity> aUser = new ArrayList<>();
        if (serverId > 0) {
            aUser = DBJPA.getSelectQuery("select * from user where level >1 and server = "+serverId+" and number_friend<50 order by last_action desc limit 100", UserEntity.class);
        } else {
            aUser = DBJPA.getSelectQuery("select * from user where server<0 and number_friend<50 and level >1 order by last_action desc limit 100", UserEntity.class);
        }
        if (aUser != null && !aUser.isEmpty()) {
            if (serverId >0) aSuggestUser = aUser;
            else aSuggestUserTest = aUser;
        }
    }

    public class DataConfig {
        public int limitedFriend;
        public List<Long> rewardSend;
    }
}
