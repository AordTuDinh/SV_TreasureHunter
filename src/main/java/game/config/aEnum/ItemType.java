package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

/** Loại lưu trữ trên bảng user_item (cột type). */
public enum ItemType {
    CONSUMABLE(1),
    EQUIPMENT(2),
    CURRENCY(3),
    EVENT(4),
    ;

    public final int value;

    ItemType(int value) {
        this.value = value;
    }

    static final Map<Integer, ItemType> lookup = new HashMap<>();

    static {
        for (ItemType t : values()) lookup.put(t.value, t);
    }

    public static ItemType get(int value) {
        return lookup.get(value);
    }
}
