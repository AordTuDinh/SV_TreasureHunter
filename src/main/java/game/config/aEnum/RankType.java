package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum RankType {
    NORMAL(1, "thường"),
    ADVANCED(2, "hiếm"),
    RARE(3, "đặc biệt"),
    LEGENDARY(4, "huyền thoại");

    public final int value;
    public final String name;


    RankType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    // lookup
    static Map<Integer, RankType> lookup = new HashMap<>();

    static {
        for (RankType itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public static RankType get(int type) {
        return lookup.get(type);
    }
}
