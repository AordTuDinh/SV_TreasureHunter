package game.battle.type;

import java.util.HashMap;
import java.util.Map;

public enum RoomState {
    INIT(0),
    ACTIVE(1),
    END(2),
    PAUSE(3),
    ;

    public int value;

    RoomState(int value) {
        this.value = value;
    }

    // lookup
    static Map<Integer, RoomState> lookup = new HashMap<>();

    static {
        for (RoomState itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public static RoomState get(int type) {
        return lookup.get(type);
    }
}
