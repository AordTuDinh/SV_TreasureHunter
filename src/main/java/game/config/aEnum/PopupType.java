package game.config.aEnum;

import game.config.lang.Lang;
import game.dragonhero.controller.AHandler;
import game.object.MyUser;
import protocol.Pbmethod;

import java.util.HashMap;
import java.util.Map;

public enum PopupType {
    NULL(0, ""),
    FORCE_LOGOUT(1, Lang.err_login_orther),
    POPUP_DEAD(2, Lang.err_login_orther),
    POPUP_END_GAME(3, Lang.err_login_orther),
    POPUP_END_TOWER(4, Lang.err_login_orther),
    POPUP_END_ARENA(5, Lang.err_login_orther),
    POPUP_WORLD_ARENA(6, ""),
    POPUP_END_BOSS_PARTY(7, Lang.err_login_orther),
    POPUP_END_BOSS_CLAN(8, Lang.err_login_orther),
    ;

    public final int value;
    public final String keyLang;

    PopupType(int value, String keyLang) {
        this.value = value;
        this.keyLang = keyLang;
    }

    // lookup
    static Map<Integer, PopupType> lookup = new HashMap<>();

    static {
        for (PopupType itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public static PopupType get(int type) {
        return lookup.get(type);
    }

    public Pbmethod.CommonVector toProto(MyUser mUser) {
        return Pbmethod.CommonVector.newBuilder().addALong(value).addAString(AHandler.getLang(mUser, keyLang)).build();
    }
}
