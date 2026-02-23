package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum EquipSlotType {
    NULL(0, ""),
    HAT(1, "mũ"),
    GLOVES(2, "găng tay"),
    ARMOR(3, "áo giáp"),
    SHOES(4, "giày"),
    WEAPON(5, "vũ khí"),
    TREASURE(6, "bảo vật"),
    RING(7, "nhẫn"),
    NECKLACE(8, "vòng cổ"),
    ;

    public final int value;
    public final String name;

    EquipSlotType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    // lookup
    static Map<Integer, EquipSlotType> lookup = new HashMap<>();

    static {
        for (EquipSlotType itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public static EquipSlotType get(int slot) {
        return lookup.get(slot);
    }
}
