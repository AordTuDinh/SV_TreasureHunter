package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum AvatarIndex {
    AVATAR_TYPE(0),
    AVATAR_ID(1),
    HERO(2),
    FRAME(3),
    SKIN(4),
    ;

    public final int value;

    AvatarIndex(int value) {
        this.value = value;
    }

    // lookup
    static Map<Integer, AvatarIndex> lookup = new HashMap<>();

    static {
        for (AvatarIndex itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public static AvatarIndex get(int type) {
        return lookup.get(type);
    }
}
