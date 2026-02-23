package game.cache;

import game.monitor.Online;

import java.util.HashMap;
import java.util.Map;

public class JedisService {
    public static final int NAP_NOTIFY = 1;
    public static final int MAIL_NOTIFY = 2;
    public static final int SALE_NOTIFY = 3;
    public static final int TELEGRAM_NOTIFY = 4;
    public static final int CHAT_GLOBAL_BROADCAST = 5;




    public static Map<Integer, String> serviceName = new HashMap<>() {{
        put(TELEGRAM_NOTIFY, "telegram");
    }};
}
