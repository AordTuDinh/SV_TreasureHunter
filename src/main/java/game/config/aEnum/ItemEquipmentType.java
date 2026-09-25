package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum ItemEquipmentType {
    WEAPON(1, "vũ khí"),
    HAT(2, "mũ"),
    ARMOR(3, "áo"),
    PANTS(4, "quần"),
    SHOES(5, "giày"),
    CLOAK(6, "áo choàng"),
    GLOVES(7, "bao tay"),
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
