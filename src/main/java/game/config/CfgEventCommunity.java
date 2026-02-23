package game.config;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class CfgEventCommunity {
    public static int SIZE_DAME_SKIN_FREE;
    public static int SIZE_100_SCROLL;
    public static DataConfig config;
    public static final int TYPE_LEVEL = 1;
    public static final int TYPE_DAY = 2;


    public static void loadConfig(String json) {
        config = new Gson().fromJson(json, DataConfig.class);
        SIZE_100_SCROLL = config.free100Scroll.size();
        SIZE_DAME_SKIN_FREE = config.freeDameSkin.size();
    }

    public static List<Long> getBonusCommunity(int eventId) {
        return config.events.get(eventId - 1).getBonus();
    }

    public class DataConfig {
        public List<EventCommunity> events;
        public List<EventCommunity> free100Scroll;
        public List<EventCommunity> freeDameSkin;
    }

    public class EventCommunity {
        public int id;
        public String name;
        List<Long> bonus;
        public String link;
        public List<Integer> type;

        public List<Long> getBonus() {
            return new ArrayList<>(bonus);
        }
    }
}
