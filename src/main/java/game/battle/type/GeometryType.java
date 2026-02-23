package game.battle.type;

import java.util.HashMap;
import java.util.Map;

public enum GeometryType {
    Null(0),
    Circle(1),
    Triangle(2),
    ;

    public int value;

    GeometryType(int value) {
        this.value = value;
    }

    // lookup
    static Map<Integer, GeometryType> lookup = new HashMap<>();

    static {
        for (GeometryType itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public static GeometryType get(int type) {
        return lookup.get(type);
    }
}
