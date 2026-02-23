package game.monitor;

import game.cache.JCachePubSub;
import game.dragonhero.server.AppConfig;
import game.pubsub.PubSubService;

public class Telegram {
    public static void sendNotify(String message) {
        JCachePubSub.getInstance().publish(AppConfig.cfg.telegram.redisChannel, PubSubService.TELEGRAM_NOTIFY.getMessage("(" + AppConfig.cfg.name + ") " + message));
    }
}
