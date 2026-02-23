package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum MaterialType {
    CHIP(33),

    LUCKY_COIN(35);


    public final int id;

    MaterialType(int id) {
        this.id = id;
    }

    // lookup
    static Map<Integer, MaterialType> lookup = new HashMap<>();

    static {
        for (MaterialType itemType : values()) {
            lookup.put(itemType.id, itemType);
        }
    }

    public static MaterialType get(int id) {
        return lookup.get(id);
    }
}
