package game.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import game.battle.object.ServerObject;
import game.cache.JCache;
import game.dragonhero.mapping.UserEntity;
import game.dragonhero.mapping.main.SystemMailEntity;
import game.dragonhero.task.dbcache.MailCreatorCache;
import ozudo.base.database.DBResource;
import ozudo.base.log.Logs;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CfgServer {
    public static final String REAL = "real";
    public static final String TEST = "test";
    public static final String SIMULATE = "simulate";
    public static final int TEST_SERVER_ID = 0;
    public static DataConfig config;
    public static String DB_MAIN = "dson_main.";
    public static String DB_DSON = "dson.";
    public static int runningPort = 0, serverId;
    public static String serverType;
    public static int maxChannelOpen = 20;
    public static int maxChannelHome = 20;
    static List<SystemMailEntity> cacheSystemMail;
    static String KEY_CACHE_SYSTEM_MAIL = "KEY_CACHE_SYSTEM_MAIL";
    public static final String SVID = "SVID";
    public static final boolean BAOTRI = false;
    public static boolean isRealServer() {
        return serverType.equals(REAL);
    }

    public static boolean isTestServer() {
        return serverType.equals(TEST);
    }

    public static boolean isSimulateServer() {
        return serverType.equals(SIMULATE);
    }

    public static List<ServerObject> serverObjects = new ArrayList<>();
    public static List<Integer> serverOpens = new ArrayList<>();
    public static int maxServerOpen;


    public static String getCfgTable() {
        if (isTestServer()) return DB_MAIN + "config_dev";
        return DB_MAIN + "config";
    }
    


    public static void setServerList(String json) {
        serverObjects = new Gson().fromJson(json, new TypeToken<List<ServerObject>>() {
        }.getType());
        for (int i = 0; i < serverObjects.size(); i++) {
            int id = serverObjects.get(i).id;
            if (!serverOpens.contains(id)) serverOpens.add(id);
        }
        maxServerOpen = serverOpens.size();
    }


    public static long getSlowSQLTime() {
        if (config == null || config.slowSQL < 100) return 1000;
        return config.slowSQL;
    }

    public static int getSeason() {
        int week = Calendar.getInstance().get(Calendar.WEEK_OF_MONTH);
        return week > 4 ? 4 : week;

    }

    private static  List<SystemMailEntity> getSystemMail(){
        if ( cacheSystemMail ==null || JCache.getInstance().getValue(KEY_CACHE_SYSTEM_MAIL) == null) {
            JCache.getInstance().setValue(KEY_CACHE_SYSTEM_MAIL, "1", JCache.EXPIRE_1M * 5);
            cacheSystemMail = DBResource.getInstance().getList( DB_MAIN+ "system_mail",SystemMailEntity.class);
        }
        return cacheSystemMail;
    }

    public static void checkSystemMail(UserEntity user) {
        try {

            List<SystemMailEntity> sysMail = getSystemMail();
            if (sysMail == null) return;
            for (SystemMailEntity mail : sysMail) {
                if (mail.getServerId() == 0 || mail.getServerId() == user.getServer()) {
                    if (mail.getIsEnabled() == 1) {
                        if (mail.getFromVip() <= user.getVip() && user.getVip() <= mail.getToVip()) {
                            switch (mail.getMailType()) {

                                case 1:
                                    MailCreatorCache.sendSystemMail(user, mail);
                                    break;
                                case 2: // Không gửi thư cho user mới
                                    Calendar ca = Calendar.getInstance();
                                    ca.add(Calendar.YEAR, -1);
                                    if (user.getDateCreated().before(mail.getDateCreated()) && user.getLastLogin().after(ca.getTime())) {
                                        MailCreatorCache.sendSystemMail(user, mail);
                                    }
                                    break;
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    public static void loadConfig(String strJson) {
        config = new Gson().fromJson(strJson, DataConfig.class);
        if (config.slowSQL < 100) config.slowSQL = 1000;
    }

    public static String getKeyCCU() {
        return "ccu_" + serverId;
    }

    public class DataConfig {
        public String publicKey;
        public String mainLanguage;
        public List<String> unlockCp;
        public long slowSQL;
    }
}
