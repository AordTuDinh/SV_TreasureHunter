package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum InitMapType {
    ROOMTYPE (0),
    TELEPORT (1),

    ;

    public final int value;

    InitMapType(int value) {
        this.value = value;
    }

    //region Lookup
    static Map<Integer, InitMapType> lookUp = new HashMap<>();

    static {
        for (InitMapType chatType : values())
            lookUp.put(chatType.value, chatType);
    }

    public static InitMapType get(int value) {
        return lookUp.get(value);
    }
    //endregion
}