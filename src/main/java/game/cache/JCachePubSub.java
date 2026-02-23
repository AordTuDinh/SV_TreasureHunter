package game.cache;

import game.config.CfgServer;
import game.dragonhero.server.AppConfig;
import game.pubsub.Subscriber;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class JCachePubSub extends IRedis {
    public static String gameChannel = "server"; // của từng server
    public static String allGameChannel = "GameChannel"; //của tất cả sv

    static JCachePubSub instance;

    public static JCachePubSub getInstance() {
        if (instance == null) {
            instance = new JCachePubSub();
            instance.PREFIX = "";
        }
        return instance;
    }

    public JCachePubSub() {
        this.host = AppConfig.cfg.redis.pubSubHost;
        init();
    }

    public void init() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(5);
        config.setMaxIdle(1);
        pool = new JedisPool(config, host, 6379);
        gameChannel = CfgServer.isRealServer() ? String.format("server%s", CfgServer.serverId) : String.format("test%s", CfgServer.serverId);
    }

    public void subscriberGameServer() {
        subscriberToChannel(allGameChannel, new Subscriber());
        subscriberToChannel(gameChannel, new Subscriber());
    }

    public void subscriberTelegram() { // Only Game Task
        subscriberToChannel(AppConfig.cfg.telegram.redisChannel, new Subscriber());
    }
}
