package game.config.aEnum;

import ozudo.base.helper.DateTime;

import java.util.HashMap;
import java.util.Map;

public enum TopType {
    USER_POWER(1, 0, "POWER",
            "SELECT *,power number FROM dson.user WHERE server=%s order by power desc limit 50",
            "SELECT count(*) number FROM dson.user WHERE server=%s and power > (SELECT power FROM dson.user WHERE id=%s)",
            "SELECT *, power number FROM dson.user WHERE id=%s"),
    CLAN_POWER(2, 1, "CLAN_POWER",
            "SELECT *,power number FROM dson.clan WHERE server=%s order by power desc limit 50",
            "SELECT count(*) number FROM dson.clan WHERE server=%s and power > (SELECT power FROM dson.clan WHERE id=%s)",
            "SELECT *,power number FROM dson.clan WHERE id=%s"),
    CLAN_STAR(3, 1, "CLAN_STAR",
            "SELECT *,star number FROM dson.clan WHERE server=%s order by star desc limit 50",
            "SELECT count(*) number FROM dson.clan WHERE server=%s and star > (SELECT star FROM dson.clan WHERE id=%s)",
            "SELECT *,star number FROM dson.clan WHERE id=%s"),
    CLAN_CONTRIBUTE(4, 2, "CLAN_CONTRIBUTE",
            "SELECT u.*,c.contribute number FROM dson.user u INNER JOIN dson.user_clan c ON u.id = c.user_id WHERE u.SERVER=%s AND u.clan=%s order BY c.contribute desc limit 50",
            "SELECT count(*) number FROM dson.user_clan WHERE server=%s and contribute > (SELECT contribute FROM dson.user_clan WHERE user_id=%s)",
            "SELECT u.*,c.contribute number FROM dson.user u INNER JOIN dson.user_clan a ON u.id = c.user_id WHERE u.id=%s"),
    /** Top thắng arena tuần — point = user_week.arena_win. get(): server, weekId */
    USER_ARENA(5, 0, "ARENA",
            "SELECT u.*, COALESCE(w.arena_win,0) number FROM dson.user u LEFT JOIN dson.user_week w ON u.id = w.user_id AND w.week_id=%2$s WHERE u.server=%1$s ORDER BY number DESC LIMIT 50",
            "SELECT count(*) number FROM dson.user_week WHERE server=%1$s AND week_id=(SELECT week_id FROM dson.user_week WHERE user_id=%2$s LIMIT 1) AND arena_win > COALESCE((SELECT arena_win FROM dson.user_week WHERE user_id=%2$s LIMIT 1),-1)",
            "SELECT u.*, COALESCE(w.arena_win,0) number FROM dson.user u LEFT JOIN dson.user_week w ON u.id = w.user_id AND w.week_id=(SELECT week_id FROM dson.user_week WHERE user_id=%1$s ORDER BY week_id DESC LIMIT 1) WHERE u.id=%1$s"),
//    PURCHASE(8, 0, "PURCHASE",
//            "SELECT u.*,c.total_purchases number FROM dson.user u INNER JOIN dson.user_top_purchase c ON u.id = c.user_id WHERE u.SERVER=%s  order BY c.total_purchases desc limit 50",
//            "SELECT count(*) number FROM dson.user_top_purchase WHERE server_id=%s and total_purchases > (SELECT total_purchases FROM dson.user_top_purchase WHERE user_id=%s)",
//            "SELECT u.*,c.total_purchases number FROM dson.user u INNER JOIN dson.user_top_purchase c ON u.id = c.user_id WHERE u.id=%s"),
//    PET_POINT(9, 0, "PET_POINT",
//            "SELECT u.*,c.point number FROM dson.user u INNER JOIN dson.user_event_top c ON u.id = c.user_id WHERE u.SERVER=%s  order BY c.point desc limit 50",
//            "SELECT count(*) number FROM dson.user_event_top WHERE server=%s and point > (SELECT point FROM dson.user_event_top WHERE user_id=%s)",
//            "SELECT u.*,c.point number FROM dson.user u INNER JOIN dson.user_event_top c ON u.id = c.user_id WHERE u.id=%s"),
    ;

    public final int value, type;
    public final String name, sql, sqlMyRank, sqlMyInfo;
    public static final int NORMAL = 0;
    public static final int CLAN_TYPE = 1;
    public static final int CLAN_MEMBER_TYPE = 2;

    TopType(int value, int type, String name, String sql, String sqlMyRank, String sqlMyInfo) {
        this.value = value;
        this.type = type;
        this.name = name;
        this.sql = sql;
        this.sqlMyRank = sqlMyRank;
        this.sqlMyInfo = sqlMyInfo;
    }

    // lookup
    static Map<Integer, TopType> lookup = new HashMap<>();

    static {
        for (TopType itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public static TopType get(int type) {
        return lookup.get(type);
    }
}
