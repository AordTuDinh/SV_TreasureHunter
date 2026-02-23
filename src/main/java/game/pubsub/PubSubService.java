package game.pubsub;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public enum PubSubService {
    MAIL_NOTIFY(1), // bắn sang game channel
    MESSAGE_SLIDER(2),
    TELEGRAM_NOTIFY(3),
    MESSAGE_TOAST(4),
    RELOAD_CONFIG_CHAT(5), // bắn sang game channel
    NAP_SMS(6), // bắn sang game channel
    BUY_QR(7), // bắn sang game channel
    RELOAD_CONFIG(8), // force reload config
    RELOAD_GIFT_CODE(9),
    DELAY_RESTART_SERVER(10), // hẹn giờ restart server
    ;

    public int id;
    public String name;

    PubSubService(int id, String... name) {
        this.id = id;
        if (name.length > 0) this.name = name[0];
    }

    // lookup
    static Map<Integer, game.pubsub.PubSubService> lookup = new HashMap<>();

    static {
        for (game.pubsub.PubSubService itemType : values()) {
            lookup.put(itemType.id, itemType);
        }
    }

    public static game.pubsub.PubSubService get(int type) {
        return lookup.get(type);
    }

    public static List<game.pubsub.PubSubService> filter(String name) {
        return Arrays.stream(values()).filter(value -> name.toLowerCase().startsWith(value.getKey().toLowerCase())).collect(Collectors.toList());
    }

    public String getKey() {
        String[] tmp = name().split("_");
        return tmp.length >= 2 ? tmp[0] + tmp[1] : tmp[0];
    }

    public static String getName(int id) {
        game.pubsub.PubSubService service = get(id);
        return service == null ? String.valueOf(id) : service.name();
    }

    public String getMessage(String message) {
        return String.format("%s@%s", id, message);
    }
}