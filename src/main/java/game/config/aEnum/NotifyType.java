package game.config.aEnum;


import java.util.HashMap;
import java.util.Map;

public enum NotifyType {
    MAIL(0),
    MESSAGE(1),
    CHECK_IN(2),
    GUILD_CHECKIN(3),
    FRIEND_REQUEST(4),
    SUMMON_FREE(5),
    CLAN_REQUEST(6),
    EVENT_1_HOUR(7),
    EVENT_BUY_GOLD(8),
    EVENT_LUNCH(9),
    EVENT_14_DAYS(10),
    EVENT_MONTH(11),
    QUEST_D(12),
    ACHIEVEMENT(13),
    AFK_BONUS(14),
    FRIEND_SEND_GIFT(15),
    PHUC_LOI(16),
    QUEST_C(17),
    QUEST_7_DAY(18),
    FREE_100_SCROLL(19),
    FREE_DAME_SKIN(20),
    ACHIEVEMENT_TAB_1(21),
    ACHIEVEMENT_TAB_2(22),
    ACHIEVEMENT_TAB_3(23),
    ACHIEVEMENT_TAB_4(24),
    ACHIEVEMENT_TAB_5(25),
    ;

    public int value;

    NotifyType(int value) {
        this.value = value;
    }

    // lookup
    static Map<Integer, NotifyType> lookup = new HashMap<>();

    static {
        for (NotifyType type : values()) {
            lookup.put(type.value, type);
        }
    }

    public static NotifyType get(int type) {
        return lookup.get(type);
    }
}
