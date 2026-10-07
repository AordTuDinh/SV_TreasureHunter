package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum MapType {
    LOGIN(-1, false, 0),
    NEWBIE(0, false, 200),
    HOME(1, true, 200),
    ;

    public final int value;
    public final boolean allowChangeChanel;// cho phép đổi kênh không
    public final int maxPlayer;

    MapType(int value, boolean allowChangeChanel, int maxPlayer) {
        this.value = value;
        this.allowChangeChanel = allowChangeChanel;
        this.maxPlayer = maxPlayer;
    }

    /** Map thế giới có lưu vị trí: làng tân thủ và map chính. */
    public boolean isOpenWorld() {
        return this == NEWBIE || this == HOME;
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
