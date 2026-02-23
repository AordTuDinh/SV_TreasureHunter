package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum PieceType {
    WEAPON(1),
    MONSTER(2),
    PET(3),
    ;

    public final int value;

    PieceType(int value) {
        this.value = value;
    }

    // lookup
    static Map<Integer, PieceType> lookup = new HashMap<>();

    static {
        for (PieceType type : values()) {
            lookup.put(type.value, type);
        }
    }

    public static PieceType get(int type) {
        return lookup.get(type);
    }
}
