package game.config.aEnum;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum RankingType { // format : [id-type + name]
    HOME(0, List.of(1, 2, 3)),
    CLAN(1, List.of(3, 4)),
    TOWER(2, List.of(5)),
    ARENA(3, List.of(6)),
    CLAN_CONTRIBUTE(4, List.of(7)),
    WORLD_BOSS_SOLO(5, List.of(11))
    ;

    public final int value;
    public final List<Integer> ids;

    RankingType(int value, List<Integer> ids) {
        this.value = value;
        this.ids = ids;
    }

    // lookup
    static Map<Integer, RankingType> lookup = new HashMap<>();

    static {
        for (RankingType itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public static RankingType get(int type) {
        return lookup.get(type);
    }
}
