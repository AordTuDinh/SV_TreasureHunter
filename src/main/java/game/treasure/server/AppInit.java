package game.treasure.server;

import game.config.CfgServer;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import ozudo.base.database.DBJPA;
import ozudo.base.database.DBJPA2;
import ozudo.base.database.DBResource;
import ozudo.base.log.Config;

import java.io.FileInputStream;
import java.io.InputStream;

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
        AppConfig.load("config.json");
        CfgServer.serverId = AppConfig.cfg.serverId;
        CfgServer.runningPort = AppConfig.cfg.serverPort;
        CfgServer.serverType = AppConfig.cfg.serverType;
        Config.loadEmpty();
    }

    private static void initDb() {
         DBJPA.init(AppConfig.cfg.db.entity1);
         DBJPA2.init(AppConfig.cfg.db.entity2);
         DBResource.getInstance().init(AppConfig.cfg.db.entityResource);
         AppConfig.setDbConfig();
    }
}
