package game.dragonhero.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import game.config.CfgServer;
import game.dragonhero.mapping.main.ConfigEntity;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.Filer;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.StringHelper;
import ozudo.base.log.Logs;

public class AppConfig {
    public static DataConfig cfg;
    public static final int SERVER_BATTLE_FAKE = 2;

    public static boolean isServerRealTest() {
        return cfg.isRealTest;
    }

    public static int getAdminPort() {
        return cfg.backdoor;
    }

    public static void setDbConfig() {
        ConfigEntity config = (ConfigEntity) DBJPA.getUnique(CfgServer.DB_MAIN + "config", ConfigEntity.class, "k", cfg.keyConfig);
        JsonObject obj = GsonUtil.parseJsonObject(config.getV());
        cfg.mongodb = obj.get("mongodb").getAsString();
        cfg.prefixIp = obj.get("prefixIp").getAsString();
        cfg.redis = new Gson().fromJson(obj.get("redis").toString(), RedisConfig.class);
    }

    public static void load(String filename) {
        String data = Filer.readFile(filename);
        if (StringHelper.isEmpty(data)) {
            Logs.error("Couldn't load json config file");
        }
        cfg = new Gson().fromJson(data, DataConfig.class);
        if (StringHelper.isEmpty(cfg.keyConfig)) cfg.keyConfig = "server_config";
        else cfg.keyConfig = "server_config_" + cfg.keyConfig;
    }

    public class DataConfig {
        public String name, keyConfig, prefixIp;
        public boolean battleFake;
        public boolean isRealTest;
        public TelegramConfig telegram;
        public int backdoor, localIp;
        public DBConfig db;

        public String mongodb;
        public RedisConfig redis;
    }

    public class DBConfig {
        public String mysqlUsername;
        public String entity1, entity2, entityResource;
    }

    public class TelegramConfig {
        public String redisChannel, prefix;
        public String botId, chatId;
    }

    public class RedisConfig {
        public String pubSubHost, gameHost;
    }
}
