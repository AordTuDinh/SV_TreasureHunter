package game.dragonhero.task.dbcache;

import game.cache.JCache;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class CCUCache {
    static final CCUCache INSTANCE = new CCUCache();

    public static CCUCache getInstance() {
        return INSTANCE;
    }

    public static final String queueCCUKey = "queue_ccu", queueLastAction = "queue_last_action";

    public void addQueueLastAction(int userId, long lastAction) {
        JCache.getInstance().sadd(queueLastAction, String.format("%s_%s", userId, lastAction));
    }

    public void addQueueCCU(int serverId, String dateCreated, String hours, long count) {
        JCache.getInstance().sadd(queueCCUKey, String.format("%s_%s_%s_%s", serverId, dateCreated, hours, count));
    }
}