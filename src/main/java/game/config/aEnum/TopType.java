package game.config.aEnum;

import ozudo.base.helper.DateTime;

import java.util.HashMap;
import java.util.Map;

public enum TopType {
    USER_LEVEL(1, 0, "LEVEL",
            "SELECT *,level number FROM dson.user WHERE server=%s and level>1 ORDER BY level DESC LIMIT 50",
            "SELECT count(*) number FROM dson.user WHERE server=%s and level > (SELECT level FROM dson.user WHERE id=%s)",
            "SELECT *, level number FROM dson.user WHERE id=%s"),
    USER_POWER(2, 0, "POWER",
            "SELECT *,power number FROM dson.user WHERE server=%s and level>1 order by power desc limit 50",
            "SELECT count(*) number FROM dson.user WHERE server=%s and power > (SELECT power FROM dson.user WHERE id=%s)",
            "SELECT *, power number FROM dson.user WHERE id=%s"),
    CLAN_POWER(3, 1, "CLAN_POWER",
            "SELECT *,power number FROM dson.clan WHERE server=%s order by power desc limit 50",
            "SELECT count(*) number FROM dson.clan WHERE server=%s and power > (SELECT power FROM dson.clan WHERE id=%s)",
            "SELECT *,power number FROM dson.clan WHERE id=%s"),
    CLAN_STAR(4, 1, "CLAN_STAR",
            "SELECT *,star number FROM dson.clan WHERE server=%s order by star desc limit 50",
            "SELECT count(*) number FROM dson.clan WHERE server=%s and star > (SELECT star FROM dson.clan WHERE id=%s)",
            "SELECT *,star number FROM dson.clan WHERE id=%s"),
    TOWER_LEVEL(5, 0, "TOWER_LEVEL",
            "SELECT u.id,u.name,u.username,u.intro,u.level,u.vip, u.clan,u.clan_rank,u.clan_position,u.clan_avatar,u.gold,u.gem,u.power,u.avatar,u.clan_name,u.weapon,u.item_equipment,t.level number" +
                    " FROM dson.user u INNER JOIN dson.user_tower t ON u.id = t.user_id WHERE u.server=%s and u.level>1 order by t.level DESC , t.last_attack asc limit 50",
            "SELECT count(*) number FROM dson.user_tower WHERE server=%s and level > (SELECT level FROM dson.user_tower WHERE user_id=%s)",
            "SELECT u.id,u.name,u.username,u.intro,u.level,u.vip, u.clan,u.clan_rank,u.clan_position,u.clan_avatar,u.gold,u.gem,u.power,u.avatar,u.clan_name,u.weapon,u.item_equipment,t.level number" +
                    " FROM dson.user u INNER JOIN dson.user_tower t ON u.id = t.user_id WHERE user_id=%s"),
    ARENA(6, 0, "ARENA",
            "SELECT u.*,a.arena_point number FROM dson.user u INNER JOIN dson.user_arena a ON u.id = a.user_id WHERE u.SERVER=%s AND a.active_arena = 1 order BY a.arena_point desc limit 50",
            "SELECT count(*) number FROM dson.user_arena WHERE server=%s and arena_point > (SELECT arena_point FROM dson.user_arena WHERE user_id=%s)",
            "SELECT u.*,a.arena_point number FROM dson.user u INNER JOIN dson.user_arena a ON u.id = a.user_id WHERE u.id=%s"),
    CLAN_CONTRIBUTE(7, 2, "CLAN_CONTRIBUTE",
            "SELECT u.*,c.contribute number FROM dson.user u INNER JOIN dson.user_clan c ON u.id = c.user_id WHERE u.SERVER=%s AND u.clan=%s order BY c.contribute desc limit 50",
            "SELECT count(*) number FROM dson.user_clan WHERE server=%s and contribute > (SELECT contribute FROM dson.user_clan WHERE user_id=%s)",
            "SELECT u.*,c.contribute number FROM dson.user u INNER JOIN dson.user_clan a ON u.id = c.user_id WHERE u.id=%s"),
    PURCHASE(8, 0, "PURCHASE",
            "SELECT u.*,c.total_purchases number FROM dson.user u INNER JOIN dson.user_top_purchase c ON u.id = c.user_id WHERE u.SERVER=%s  order BY c.total_purchases desc limit 50",
            "SELECT count(*) number FROM dson.user_top_purchase WHERE server_id=%s and total_purchases > (SELECT total_purchases FROM dson.user_top_purchase WHERE user_id=%s)",
            "SELECT u.*,c.total_purchases number FROM dson.user u INNER JOIN dson.user_top_purchase c ON u.id = c.user_id WHERE u.id=%s"),
    PET_POINT(9, 0, "PET_POINT",
            "SELECT u.*,c.point number FROM dson.user u INNER JOIN dson.user_event_top c ON u.id = c.user_id WHERE u.SERVER=%s  order BY c.point desc limit 50",
            "SELECT count(*) number FROM dson.user_event_top WHERE server=%s and point > (SELECT point FROM dson.user_event_top WHERE user_id=%s)",
            "SELECT u.*,c.point number FROM dson.user u INNER JOIN dson.user_event_top c ON u.id = c.user_id WHERE u.id=%s"),
    CLAN_HONOR(10, 2, "CLAN_HONOR",
            "SELECT u.*,c.honor number FROM dson.user u INNER JOIN dson.user_clan c ON u.id = c.user_id WHERE u.SERVER=%s AND u.clan=%s order BY c.honor desc limit 50",
            "SELECT count(*) number FROM dson.user_clan WHERE server=%s and honor > (SELECT honor FROM dson.user_clan WHERE user_id=%s)",
            "SELECT u.*,c.honor number FROM dson.user u INNER JOIN dson.user_clan a ON u.id = c.user_id WHERE u.id=%s"),
    WORLD_BOSS(11, 0, "WORLD_BOSS",
            "SELECT u.*,a.kill_boss number FROM dson.user u INNER JOIN dson.user_week a ON u.id = a.user_id WHERE u.SERVER=%s and week_id = "+ DateTime.getNumberWeek()+" order BY a.kill_boss desc limit 50",
            "SELECT count(*) number FROM dson.user_week WHERE server=%s and week_id = "+ DateTime.getNumberWeek()+" and kill_boss > (SELECT kill_boss FROM dson.user_week WHERE user_id=%s)",
            "SELECT u.*,a.kill_boss number FROM dson.user u INNER JOIN dson.user_week a ON u.id = a.user_id WHERE u.id=%s and week_id = "+ DateTime.getNumberWeek()),
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
