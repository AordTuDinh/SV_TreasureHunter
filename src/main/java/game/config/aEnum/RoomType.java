package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum RoomType {
    NULL(-1, false, 0),
    HOME(0, true, 100),
    ;

    public final int value;
    public final boolean allowChangeChanel;// cho phép đổi kênh không
    public final int maxPlayer;

    RoomType(int value, boolean allowChangeChanel, int maxPlayer) {
        this.value = value;
        this.allowChangeChanel = allowChangeChanel;
        this.maxPlayer = maxPlayer;
    }

    // lookup
    static Map<Integer, RoomType> lookup = new HashMap<>();

    static {
        for (RoomType itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public static RoomType get(int type) {
        return lookup.get(type);
    }
}
