package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum FeatureBlockType {
    BLOCK_NULL(0),
    BLOCK_BY_LEVEL(1),
    BLOCK_BY_TUT_QUEST(2),
    ;

    public final int value;

    FeatureBlockType(int value) {
        this.value = value;
    }

    //region Lookup
    static Map<Integer, FeatureBlockType> lookUp = new HashMap<>();

    static {
        for (FeatureBlockType f : values()) {
            lookUp.put(f.value, f);
        }
    }

    public static FeatureBlockType get(int value) {
        return lookUp.get(value);
    }
}
