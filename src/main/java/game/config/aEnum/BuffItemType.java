package game.config.aEnum;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum BuffItemType {
    X25_EXP(0, 13, 2, 25),
    X25_GOLD(1, 14, 1, 25),
    X25_DROP(2, 12, 0, 25),
    X50_EXP(3, 112, 2, 50),
    X50_GOLD(4, 113, 1, 50),
    X50_DROP(5, 114, 0, 50),
    X100_EXP(6, 115, 2, 100),
    X100_GOLD(7, 116, 1, 100),
    X10_DROP(8, 117, 0, 100),
    ;

    public final int id;
    public final int index;
    public final int pointIndex;
    public final long valueBuff;

    BuffItemType(int index, int id, int pointIndex, long valueBuff) {
        this.index = index;
        this.id = id;
        this.pointIndex = pointIndex;
        this.valueBuff = valueBuff;
    }


    // lookup
    static Map<Integer, BuffItemType> lookup = new HashMap<>();
    static Map<Integer, BuffItemType> lookupIndex = new HashMap<>();
    public static List<Integer> buffIds = List.of(X25_EXP.id, X25_GOLD.id, X25_DROP.id, X50_EXP.id, X50_GOLD.id, X50_DROP.id, X100_EXP.id, X100_GOLD.id, X10_DROP.id);

    static {
        for (BuffItemType itemType : values()) {
            lookup.put(itemType.id, itemType);
            lookupIndex.put(itemType.index, itemType);
        }
    }

    public static BuffItemType get(int id) {
        return lookup.get(id);
    }

    public static BuffItemType getByIndex(int index) {
        return lookupIndex.get(index);
    }
}
