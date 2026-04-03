package game.battle.type;

import java.util.HashMap;
import java.util.Map;

public enum UnitType {
    PLAYER(0),
    ENEMY(1),
    BOSS(2),
    BOT_PLAYER(3),
    PET(4),
    ;

    public int value;

    UnitType(int value) {
        this.value = value;
    }

    // lookup
    static Map<Integer, UnitType> lookup = new HashMap<>();

    static {
        for (UnitType itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public static UnitType get(int type) {
        return lookup.get(type);
    }
}
