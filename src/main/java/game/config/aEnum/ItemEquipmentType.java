package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum ItemEquipmentType {
    AXE(1, "rìu"),
    HAT(2, "mũ"),
    ARMOR(3, "áo"),
    CAPE(4, "áo choàng"),
    BOOTS(5, "giày"),
    ;

    public final int value;
    public final String name;

    ItemEquipmentType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    static final Map<Integer, ItemEquipmentType> lookup = new HashMap<>();

    static {
        for (ItemEquipmentType itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public static ItemEquipmentType get(int type) {
        return lookup.get(type);
    }
}
