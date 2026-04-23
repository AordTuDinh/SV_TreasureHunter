package game.battle.type;

import java.util.HashMap;
import java.util.Map;

public enum AnimationType {
    ATTACK(0),

    ;

    public int value;

    AnimationType(int value) {
        this.value = value;
    }

    // lookup
    static Map<Integer, AnimationType> lookup = new HashMap<>();

    static {
        for (AnimationType itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public static AnimationType get(int type) {
        return lookup.get(type);
    }
}
