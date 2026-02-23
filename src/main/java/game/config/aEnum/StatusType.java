package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum StatusType {
    LOCK(0),
    PROCESSING(1),
    RECEIVE(2),
    DONE(3);

    public final int value;

    StatusType(int value) {
        this.value = value;
    }

    // lookup
    static Map<Integer, StatusType> lookup = new HashMap<>();

    static {
        for (StatusType itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public static StatusType get(int type) {
        return lookup.get(type);
    }
}
