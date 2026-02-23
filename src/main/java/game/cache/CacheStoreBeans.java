package game.cache;

import game.monitor.ClanManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CacheStoreBeans {

    public static final CacheStore<Integer> syncCache = new CacheStore<>(30, TimeUnit.SECONDS);
    public static final CacheStore<Integer> cache1Min = new CacheStore<>(1, TimeUnit.MINUTES);
    public static final CacheStore<Integer> cache10Min = new CacheStore<>(10, TimeUnit.MINUTES);
    public static final CacheStore<ClanManager> cacheClanManager = new CacheStore<>(365, TimeUnit.DAYS);

    public static final Map<Integer, CacheStore<String>> cacheStoreSecond = new HashMap<Integer, CacheStore<String>>() {{
        put(60, new CacheStore<>(60, TimeUnit.SECONDS));
        put(300, new CacheStore<>(300, TimeUnit.SECONDS));
    }};

    public static CacheStore<String> getCacheStoreSecond(int second) {
        return cacheStoreSecond.get(second);
    }

    public static final Map<String, Object> mCache = new HashMap<>();


}
