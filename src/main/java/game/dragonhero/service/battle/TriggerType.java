package game.dragonhero.service.battle;

import java.util.HashMap;
import java.util.Map;

public enum TriggerType {
    NULL("null", -1), // bonus cộng thẳng vào room?
    INIT("init", 0), // giai đoạn khởi tạo
    INIT3("init3", 1), // giai đoạn khởi tạo - check trigger 3th cho shuriken đầu tiên
    INIT4("init4", 2), // giai đoạn khởi tạo - check trigger 4th cho shuriken đầu tiên
    INIT5("init5", 3), // giai đoạn khởi tạo - check trigger 5th cho shuriken đầu tiên
    HIT("hit", 4), // dính đòn
    FIRST_HIT("firstHit", 5),
    TH3("3th", 6),
    TH4("4th", 7),
    TH5("5th", 8),
    DESTROY("des", 9),
    LAST_DAME("last", 10), // tính toán ra dame rồi, dùng cho các effect tính toán theo dame( dame này là đã trừ thủ đối phương rồi)

    ;


    public String value;
    public int id;

    TriggerType(String value, int id) {
        this.value = value;
        this.id = id;
    }

    static Map<String, TriggerType> lookup = new HashMap<>();
    static Map<Integer, TriggerType> lookupId = new HashMap<>();

    static {
        for (TriggerType target : TriggerType.values()) lookup.put(target.value, target);
    }

    static {
        for (TriggerType target : TriggerType.values()) lookupId.put(target.id, target);
    }

    public static TriggerType get(String value) {
        return lookup.get(value);
    }

    public static TriggerType get(int id) {
        return lookupId.get(id);
    }
}
