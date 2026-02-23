package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum ToolFarmType {
    NORMAL(0),
    FER_TIME(1), //  phân bón giảm thời gian
    FERTILIZE(2), //thuốc kích thích rễ
    ;
    public final int value;

    ToolFarmType(int value) {
        this.value = value;
    }

    // lookup
    static Map<Integer, ToolFarmType> lookup = new HashMap<>();

    static {
        for (ToolFarmType itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public static ToolFarmType get(int type) {
        return lookup.get(type);
    }
}
