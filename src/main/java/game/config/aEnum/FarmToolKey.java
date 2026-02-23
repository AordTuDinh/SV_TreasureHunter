package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum FarmToolKey {
    XENG(1),//Xẻng

    ;

    public final int value;

    FarmToolKey(int value) {
        this.value = value;
    }


    // lookup
    static Map<Integer, FarmToolKey> lookup = new HashMap<>();

    static {
        for (FarmToolKey key : values()) {
            lookup.put(key.value, key);
        }
    }

    public static FarmToolKey get(int key) {
        return lookup.get(key);
    }
}
