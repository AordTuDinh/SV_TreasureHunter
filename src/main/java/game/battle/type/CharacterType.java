package game.battle.type;

import java.util.HashMap;
import java.util.Map;

public enum CharacterType {
    PLAYER(0),
    MONSTER(1),
    BOSS_GOD(2),
    TOTEM(3),
    ZOMBIE(4),
    BOT_PLAYER(5),
    HERO(6),
    PET(7),
    STONE(8),
    CAGE(9),
    MOC1(10),
    MOC2(11),
    MOC3(12),
    MOC4(13),
    ;

    public int value;

    CharacterType(int value) {
        this.value = value;
    }

    // lookup
    static Map<Integer, CharacterType> lookup = new HashMap<>();

    static {
        for (CharacterType itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public static CharacterType get(int type) {
        return lookup.get(type);
    }
}
