package game.battle.type;

import java.util.HashMap;
import java.util.Map;

public enum EffectBodyType {
    HEALING(1),
    POISON(2),
    TOXIC(3),
    RUNA(4),
    BURNED(5),
    BERSERK(6),
    TARGET(7),
    SLOW(9),
    BROKEN_SHIELD(10),
    SHIELD_FIRE(11),
    HEALING_BASIC(12),
    DOT_FIRE(13),
    DECO(14),
    CRAZY(15),
    SHIELD_BOSS(16),
    ;


    public long value;

    EffectBodyType(long value) {
        this.value = value;
    }

    // lookup
    static Map<Long, EffectBodyType> lookup = new HashMap<>();

    static {
        for (EffectBodyType itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public static EffectBodyType get(long type) {
        return lookup.get(type);
    }
}
