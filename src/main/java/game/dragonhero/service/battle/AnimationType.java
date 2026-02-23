package game.dragonhero.service.battle;

import java.util.HashMap;
import java.util.Map;

public enum AnimationType {
    ATTACK(1),

    ;
    public final long id;

    AnimationType(long id) {
        this.id = id;
    }

    static Map<Long, AnimationType> lookup = new HashMap<>();

    static {
        for (AnimationType target : AnimationType.values()) lookup.put(target.id, target);
    }

    public static AnimationType get(long value) {
        return lookup.get(value);
    }

}
