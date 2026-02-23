package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum ItemFarmType {
    SEED(1), // hạt giống
    AGRI(2), // nông sản
    TOOL(3), // phân bón, công cụ
    FOOD(4), // thức ăn cho pet
    ;
    public final int value;

    ItemFarmType(int value) {
        this.value = value;
    }

    // lookup
    static Map<Integer, ItemFarmType> lookup = new HashMap<>();

    static {
        for (ItemFarmType type : values()) {
            lookup.put(type.value, type);
        }
    }

    public static ItemFarmType get(int type) {
        return lookup.get(type);
    }
}
