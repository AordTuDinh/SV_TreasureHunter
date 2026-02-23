package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum ConditionType {
    NULL(0),
    MAX_POINT(1),
    TUTORIAL_LEVEL(2),
    HAS_LEVEL(3),
    HAS_VIP_LEVEL(4),
    HARVEST(5), // Thu hoạch cây trồng
    ;

    public final int value;

    ConditionType(int value) {
        this.value = value;
    }

    // lookup
    static Map<Integer, ConditionType> lookup = new HashMap<>();

    static {
        for (ConditionType itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public static ConditionType get(int type) {
        return lookup.get(type);
    }
}
