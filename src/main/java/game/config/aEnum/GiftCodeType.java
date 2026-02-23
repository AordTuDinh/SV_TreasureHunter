package game.config.aEnum;


import java.util.HashMap;
import java.util.Map;

public enum GiftCodeType {
    ALL_TIME(0), // toan bo user deu nhan dc
    ONE(1), // duy nhat 1 lan nhan dau tien
    GROUP_USER(2), //nhóm người có user id trong data

    ;

    public int value;

    GiftCodeType(int value) {
        this.value = value;
    }

    // lookup
    static Map<Integer, GiftCodeType> lookup = new HashMap<>();

    static {
        for (GiftCodeType type : values()) {
            lookup.put(type.value, type);
        }
    }

    public static GiftCodeType get(int type) {
        return lookup.get(type);
    }
}
