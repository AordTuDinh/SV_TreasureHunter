package game.battle.type;

import java.util.HashMap;
import java.util.Map;

public enum EnemyType {
    MONSTER(0),

    BOSS_GOD(1),


    ;

    public int value;

    EnemyType(int value) {
        this.value = value;
    }

    // lookup
    static Map<Integer, EnemyType> lookup = new HashMap<>();

    static {
        for (EnemyType itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public static EnemyType get(int type) {
        return lookup.get(type);
    }
}
