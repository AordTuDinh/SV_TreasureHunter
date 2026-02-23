package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum LevelType {
    VERRY_EASY(1),
    EASY(2),
    NORMAL(3),
    HARD(4),
    NIGHTMARE(5),
    LEVEL_6(6),
    LEVEL_7(7),
    LEVEL_8(8),
    LEVEL_9(9),
    LEVEL_10(10);

    public final int value;

    LevelType(int value) {
        this.value = value;
    }

    // lookup
    static Map<Integer, LevelType> lookup = new HashMap<>();

    static {
        for (LevelType itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public static LevelType get(int type) {
        return lookup.get(type);
    }
}
