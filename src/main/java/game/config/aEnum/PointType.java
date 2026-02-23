package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum PointType {
    ATTACK(1),
    P_ATTACK(2),
    ADVANCED_ATTACK(3),
    P_ADVANCED_ATTACK(4),
    DIVINE_ATTACK(5),
    P_DIVINE_ATTACK(6),
    UTIMATE_ATTACK(7),
    P_UTIMATE_ATTACK(8),
    MAGIC_ATTACK(9),
    P_MAGIC_ATTACK(10),
    ADVANCED_MAGIC_ATTACK(11),
    P_ADVANCED_MAGIC_ATTACK(12),
    DIVINE_MAGIC_ATTACK(13),
    P_DIVINE_MAGIC_ATTACK(14),
    UTIMATE_MAGIC_ATTACK(15),
    P_UTIMATE_MAGIC_ATTACK(16),
    HP(17),
    P_HP(18),
    ADVANCED_HP(19),
    P_ADVANCED_HP(20),
    DIVINE_HP(21),
    P_DIVINE_HP(22),
    UTIMATE_HP(23),
    P_UTIMATE_HP(24),
    HP_REGEN(25), // hồi máu mỗi 5s
    P_HP_REGEN(26), // hồi máu mỗi 5s
    MP(27),
    P_MP(28),
    MP_REGEN(29), // hồi mana mỗi 5s
    P_MP_REGEN(30), // hồi mana mỗi 5s
    P_CRIT(31),
    ADVANCED_P_CRIT(32),
    P_CRIT_DAMAGE(33),
    ADVANCED_P_CRIT_DAMAGE(34),
    P_WEAPON_COOLDOWN(35),
    DEFENSE(36),
    P_DEFENSE(37),
    MAGIC_RESIST(38),
    P_MAGIC_RESIST(39),
    P_IMMUNITY(40),
    P_AGILITY(41);

    public final int value;

    PointType(int value) {
        this.value = value;
    }

    // lookup
    static Map<Integer, PointType> lookup = new HashMap<>();

    static {
        for (PointType itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public static PointType get(int type) {
        return lookup.get(type);
    }
}
