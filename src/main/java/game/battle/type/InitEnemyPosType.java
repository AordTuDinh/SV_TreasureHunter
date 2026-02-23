package game.battle.type;

import java.util.HashMap;
import java.util.Map;

public enum InitEnemyPosType {
    RANDOM(0),
    CORNERS_4(1), // 4 góc
    CORNERS_6(2), // 6 góc
    CORNERS_8(3), // 8 góc
    CORNERS_10(4), // 10 góc
    CORNERS_12(5), // 12 góc
    TOP_BOT_CENTER(6),// trên giữa + dưới giữa
    LEFT_RIGHT_CENTER(7),
    PER_1_3_CENTER(8),
    PER_1_3_RIGHT(9),
    PER_1_3_LEFT(10),
    PER_2_3_CENTER(11),
    PER_2_3_RIGHT(12),
    PER_2_3_LEFT(11),
    TOP_CENTER(12),
    TOP_RIGHT(13),
    TOP_LEFT(14),
    BOT_CENTER(15),
    BOT_RIGHT(16),
    BOT_LEFT(17),
    MID_CENTER(18),
    MID_RIGHT(19),
    MID_LEFT(20),
    LEFT(21),
    RIGHT(22),
    ;

    public int value;

    InitEnemyPosType(int value) {
        this.value = value;
    }

    //lookup
    static Map<Integer, InitEnemyPosType> lookup = new HashMap<>();

    static {
        for (InitEnemyPosType itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public static InitEnemyPosType get(int type) {
        return lookup.get(type);
    }
}
