package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum YesNo {
    no(0),
    yes(1),
    ;

    public final int value;

    YesNo(int value) {
        this.value = value;
    }

    // lookup
    static Map<Integer, YesNo> lookup = new HashMap<>();

    static {
        for (YesNo type : values()) {
            lookup.put(type.value, type);
        }
    }

    public static YesNo get(int type) {
        return lookup.get(type);
    }

}
