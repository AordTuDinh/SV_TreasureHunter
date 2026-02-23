package game.battle.type;

import java.util.HashMap;
import java.util.Map;

public enum AutoMode {
    NORMAL(0), // Đứng im k làm j cả
    STAND_STILL(1),  // đứng yên 1 chỗ, trong tầm đánh thì đánh
    MOVE_ATTACK(2),  // trong tầm thì đánh, k đủ tầm thì di chuyển đến thằng gấn nhất để đánh
    // HIT_RUN(2), // đánh xong move xong đánh... ==!
    ;

    public int value;

    AutoMode(int value) {
        this.value = value;
    }

    // lookup
    static Map<Integer, AutoMode> lookup = new HashMap<>();

    static {
        for (AutoMode itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public static AutoMode get(int type) {
        return lookup.get(type);
    }
}
