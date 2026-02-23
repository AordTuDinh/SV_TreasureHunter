package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum FeatureType {
    CAMPAIGN(1),
    SUMMON_WEAPON(2),
    SKILL(3),
    SHOP(4),
    FRIENDS(5),
    CLAN(6),
    RANK(7),
    CHAT(8),
    WEAPON(9),
    CHECK_IN(10),
    MAIL(11),
    SPIN(12),
    LUCKY(13),
    TOWER(14),
    ARENA(15),

    // ------block type
    BLOCK_NULL(0),
    BLOCK_BY_LEVEL(1),
    BLOCK_BY_TUT_QUEST(2),
    ;

    public final int value;

    FeatureType(int value) {
        this.value = value;
    }

    //region Lookup
    static Map<Integer, FeatureType> lookUp = new HashMap<>();
    static Map<Integer, FeatureType> lookUpBlock = Map.ofEntries(
            Map.entry(BLOCK_NULL.value, BLOCK_NULL),
            Map.entry(BLOCK_BY_LEVEL.value, BLOCK_BY_LEVEL),
            Map.entry(BLOCK_BY_TUT_QUEST.value, BLOCK_BY_TUT_QUEST));

    static {
        for (FeatureType f : values()) {
            lookUp.put(f.value, f);
        }
    }

    public static FeatureType get(int value) {
        return lookUp.get(value);
    }
}
