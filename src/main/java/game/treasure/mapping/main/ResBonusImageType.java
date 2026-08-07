package game.treasure.mapping.main;

public enum ResBonusImageType {
    /** data = exp per roll (vd: "1" hoặc "[1]") */
    CRAFT_EXP(1),

    /** data = JSON array materialId (vd: "[6,7,9]") */
    MATERIAL(2),

    /** data = JSON array skinId (vd: "[101,102]") */
    SKIN(3),

    /** data = JSON array petId (vd: "[1,2,3]"); tier = res_bonus_image.tier */
    PET(4),

    /** data = JSON array mountId (vd: "[1,2,3]"); tier = res_bonus_image.tier */
    MOUNT(5),

    /** data = flat bonus wire (vd: "[2,200,13,15,500]"); tier không dùng; không chứa type 17 */
    BONUS_DATA(6),

    /** data = JSON array skinId; roll 1 skin/lần × times; tier = res_bonus_image.tier; cho phép trùng skinId */
    SKIN_LIST(7);

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

