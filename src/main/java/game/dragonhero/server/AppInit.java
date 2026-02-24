package game.dragonhero.server;

import game.cache.JCache;
import game.config.CfgServer;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import ozudo.base.database.DBJPA;
import ozudo.base.database.DBJPA2;
import ozudo.base.database.DBResource;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.StringHelper;
import ozudo.base.log.Config;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class AppInit {


    public static void initAll() throws Exception {
        initLogs();
        initConfig();
        initDb();
    }

    private static void initLogs() {
        try {
            InputStream inputStream = new FileInputStream("log4j2.xml");
            ConfigurationSource source = new ConfigurationSource(inputStream);
            Configurator.initialize(null, source);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void initConfig() throws Exception {
        Config.load("config.xml");
        CfgServer.serverId = Config.getInt("config.server.id");
        CfgServer.runningPort = Config.getInt("config.server.port");
        CfgServer.serverType = Config.getString("config.server.type");
        AppConfig.load("config.json");
        // add sv (Redis) - tạm comment khi chạy không Redis
        // String ids = JCache.getInstance().getValue(CfgServer.SVID);
        // if (ids == null) {
        //     JCache.getInstance().setValue(CfgServer.SVID, StringHelper.toDBString(Arrays.asList(CfgServer.serverId)), JCache.EXPIRE_1D * 30);
        // } else {
        //     List<Integer> svs = GsonUtil.strToListInt(ids);
        //     if (!svs.contains(CfgServer.serverId)) {
        //         svs.add(CfgServer.serverId);
        //         JCache.getInstance().setValue(CfgServer.SVID, StringHelper.toDBString(svs));
        //     }
        // }
    }

    private static void initDb() {
        // tạm comment khi chạy không DB
        // DBJPA.init(AppConfig.cfg.db.entity1);
        // DBJPA2.init(AppConfig.cfg.db.entity2);
        // DBResource.getInstance().init(AppConfig.cfg.db.entityResource);
        // AppConfig.setDbConfig();
    }
}
