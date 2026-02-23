package game.cache;

import game.dragonhero.server.AppConfig;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class JCache extends IRedis {
    static JCache instance, jobInstance;

    public static JCache getInstance() {
        if (instance == null) {
            instance = new JCache();
        }
        return instance;
    }

    public static JCache getJobInstance() {
        if (jobInstance == null) {
            jobInstance = new JCache();
        }
        return jobInstance;
    }

    public JCache() {
        this.host = AppConfig.cfg.redis.gameHost;
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(100);
        config.setMaxIdle(5);
        pool = new JedisPool(config, host, 6379);
    }
}
