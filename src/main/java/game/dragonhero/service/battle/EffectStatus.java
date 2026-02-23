package game.dragonhero.service.battle;

import java.util.HashMap;
import java.util.Map;

public enum EffectStatus {
    HIT(1),
    OUT(2),
    END(3),
    ;


    EffectStatus(int id) {
        this.id = id;
    }

    public int id;
    static Map<Integer, EffectStatus> lookup = new HashMap<>();

    static {
        for (EffectStatus c : EffectStatus.values()) lookup.put(c.id, c);
    }


    public static EffectStatus get(int value) {
        return lookup.get(value);
    }

}
