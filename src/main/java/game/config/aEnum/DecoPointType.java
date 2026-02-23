package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum DecoPointType {
    DEC_TIME(0),
    INC_QUANTITY(1),
    INC_EXP(2),

    ;


    public final int value;

    DecoPointType(int value) {
        this.value = value;
    }

    static Map<Integer, DecoPointType> lookup = new HashMap<>();

    static {
        for (DecoPointType type : values()) {
            lookup.put(type.value, type);
        }
    }

    public static DecoPointType get(int type) {
        return lookup.get(type);
    }
}
