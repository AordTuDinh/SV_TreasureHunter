package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum EventPanel {
    PACK_MONTH(1),
    TAB_CELL(2),
    ;

    public final int id;

    EventPanel(int id) {
        this.id = id;
    }

    // lookup
    static Map<Integer, EventPanel> lookup = new HashMap<>();

    static {
        for (EventPanel itemType : values()) {
            lookup.put(itemType.id, itemType);
        }
    }

    public static EventPanel get(int type) {
        return lookup.get(type);
    }

}
