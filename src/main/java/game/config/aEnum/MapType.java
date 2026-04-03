package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum MapType {
    LOGIN(-1, false, 0),
    HOME(0, true, 100),
    ;

    public final int value;
    public final boolean allowChangeChanel;// cho phép đổi kênh không
    public final int maxPlayer;

    MapType(int value, boolean allowChangeChanel, int maxPlayer) {
        this.value = value;
        this.allowChangeChanel = allowChangeChanel;
        this.maxPlayer = maxPlayer;
    }

    // lookup
    static Map<Integer, MapType> lookup = new HashMap<>();

    static {
        for (MapType itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public static MapType get(int type) {
        return lookup.get(type);
    }
}
