package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum SkinType {
    NULL(0),
    CHARACTER(1),
    DAMAGE_SKIN(2),
    CHAT_FRAME(3),
    TRIAL(4),

    ;

    public final int value;

    SkinType(int value) {
        this.value = value;
    }

    // lookup
    static Map<Integer, SkinType> lookup = new HashMap<>();

    static {
        for (SkinType itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public static SkinType get(int type) {
        return lookup.get(type);
    }
}
