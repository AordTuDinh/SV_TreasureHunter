package game.battle.type;


import java.util.HashMap;
import java.util.Map;

public enum AttackType {
    COLLIDE(0),
    MELEE(1),
    LONG_RANGE(2);

    public final int value;

    AttackType(int value) {
        this.value = value;
    }

    // lookup
    static Map<Integer, AttackType> lookup = new HashMap<>();

    static {
        for (AttackType itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public static AttackType get(float rangeAttack) {
        int type = COLLIDE.value;
        if (rangeAttack > 0 && rangeAttack <= 2) type = MELEE.value;
        else if (rangeAttack > 2) type = LONG_RANGE.value;
        return lookup.get(type);
    }
}
