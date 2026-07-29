package game.treasure.mapping.main;

public enum ResBonusImageType {
    /** data = exp per roll (vd: "1" hoặc "[1]") */
    CRAFT_EXP(1),

    /** data = JSON array materialId (vd: "[6,7,9]") */
    MATERIAL(2),

    /** data = JSON array skinId (vd: "[101,102]") */
    SKIN(3);

    public final int value;

    ResBonusImageType(int value) {
        this.value = value;
    }

    public static ResBonusImageType fromInt(int v) {
        for (ResBonusImageType t : values()) {
            if (t.value == v) return t;
        }
        return null;
    }
}

