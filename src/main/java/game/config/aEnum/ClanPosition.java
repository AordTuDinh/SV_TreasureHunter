package game.config.aEnum;

import game.config.lang.Lang;

public enum ClanPosition {
    NOT_MEMBER(-1, ""),
    MEMBER(0, "clan_member"),
    ELDER(1, "clan_elder"),
    CO_LEADER(2, "clan_co_leader"), // phó bang
    LEADER(3, "clan_leader"); // chủ bang

    public final int value;
    public final String keyLang;

    ClanPosition(int value, String keyLang) {
        this.value = value;
        this.keyLang = keyLang;
    }

    public static String getKey(int position) {
        for (ClanPosition clanPosition : values()) {
            if (clanPosition.value == position) {
                return clanPosition.keyLang;
            }
        }
        return "";
    }

    public static String getName(Lang lang, int position) {
        for (ClanPosition clanPosition : values()) {
            if (clanPosition.value == position) {
                return lang.get(clanPosition.keyLang);
            }
        }
        return "";
    }


    public static boolean isLeader(int position) {
        return LEADER.value == position;
    }
}
