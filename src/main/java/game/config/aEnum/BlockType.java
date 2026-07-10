package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum BlockType {
    NULL(0),
    BLOCK_LOGIN(1), // khóa tài khoản
    BLOCK_ACTION(2), // Vào trại giam

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

    public static BlockType get(int type) {
        return lookup.get(type);
    }
}
