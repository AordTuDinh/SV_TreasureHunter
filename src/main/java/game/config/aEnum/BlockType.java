package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum BlockType {
    NULL(0),
    BLOCK_LOGIN(1), // khóa tài khoản

    ;

    public final int value;

    BlockType(int value) {
        this.value = value;
    }

    // lookup
    static Map<Integer, BlockType> lookup = new HashMap<>();

    static {
        for (BlockType itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public static BlockType get(int rangeAttack) {
        int type = 0;
        if (rangeAttack > 0 && rangeAttack <= 1) type = 1;
        else if (rangeAttack > 1) type = 2;
        return lookup.get(type);
    }
}
