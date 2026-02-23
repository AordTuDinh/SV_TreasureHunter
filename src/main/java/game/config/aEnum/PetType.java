package game.config.aEnum;


import java.util.HashMap;
import java.util.Map;

public enum PetType {
    MONSTER(1),
    ANIMAL(2);

    public int value;

    PetType(int value) {
        this.value = value;
    }

    // lookup
    static Map<Integer, PetType> lookup = new HashMap<>();

    static {
        for (PetType type : values()) {
            lookup.put(type.value, type);
        }
    }

    public static PetType get(int type) {
        return lookup.get(type);
    }
}
