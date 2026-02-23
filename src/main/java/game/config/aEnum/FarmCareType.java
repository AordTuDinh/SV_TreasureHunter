package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum FarmCareType {
    PLANT(0), // trồng cây
    WATER(1), // tưới cây
    FERTILIZE(2), // thuốc kích thích tăng năng suất
    PLUCK(3), // Nhổ cây + dọn cây héo
    HARVEST(4), // Thu hoạch
    FER_TIME(5),//bón phân giảm thời gian trưởng thành
    VIP_PLUCK(6),//nhổ cây tự động
    ;

    public final int value;

    FarmCareType(int value) {
        this.value = value;
    }

    static Map<Integer, FarmCareType> lookup = new HashMap<>();

    static {
        for (FarmCareType type : values()) {
            lookup.put(type.value, type);
        }
    }

    public static FarmCareType get(int type) {
        return lookup.get(type);
    }


}
