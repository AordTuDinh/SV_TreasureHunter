package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum RoomType {
    NULL(-1, false, 0),
    HOME(0, true, 100),
    CAMPAIGN(1, true, 5),
    SELECT_GOD(3, false, 10),
    SHOP(4, false, 10),
    SMITHY(5, false, 10),
    FARM(6, false, 1),
    CLAN(7, true, 50),
    CLAN_BOSS(8, true, 50),
    KIM_THAN(9, false, 1),
    THUY_THAN(10, false, 1),
    HOA_THAN(11, false, 1),
    THO_THAN(12, false, 1),
    TOWER(13, false, 1),
    ARENA(15, false, 2),
    WORLD_BOSS(16, false, 3),
    ;

    public final int value;
    public final boolean allowChangeChanel;// cho phép đổi kênh không
    public final int maxPlayer;

    RoomType(int value, boolean allowChangeChanel, int maxPlayer) {
        this.value = value;
        this.allowChangeChanel = allowChangeChanel;
        this.maxPlayer = maxPlayer;
    }

    // lookup
    static Map<Integer, RoomType> lookup = new HashMap<>();

    static {
        for (RoomType itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public static RoomType get(int type) {
        return lookup.get(type);
    }
}
