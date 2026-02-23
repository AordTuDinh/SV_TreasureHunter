package game.dragonhero.service.battle;

import java.util.HashMap;
import java.util.Map;

public enum EffectType {
    EXPLODE("explode", 1, true, true, 1200, 1f),
    POISON("poison", 2, false, false, 4000, 0f), // độc
    PARALYZE("paralyze", 3, false, true, 2000, 0f), // đòn đánh đầu có cơ hội x làm tê liệt đối thủ mục tiêu trong y giây
    DEC_SPEED("decSpeed", 4, false, false, 0, 0f), // giảm chỉ số move speed
    DMG_HP("dmgHp", 5, true, true, 1500, 0f), // dđòn đánh tăng sát thương theo hp tối đa của đối thủ, tối đa 1000%
    INF("inf", 6, false, false, 0, 1.5f), // bãi độc tồn tại 2s, nhiễm độc trong 4s
    RE_HP("reHp", 7, false, false, 800, 0f), // hút máu theo dame gây ra, tối đa 75% max HP
    SHIELD_FIRE("shiedFire", 8, false, false, 0, 0f), // khiên lửa -> giảm sát thương nhận vào
    RANDOM("random", 9, true, false, 0, 0f), // tao phi tieu ngau nhien 4 loai
    SMOKE("smoke", 10, false, false, 0, 1.5f), // Tạo 1 bãi khói đứng trong bất tử
    SANDSTORM("sandStorm", 11, false, false, 0, 2f),//Tạo 1 bãi lốc cát gây sát thuơng
    DOT_FIRE("dotFire", 12, false, false, 0, 0f),// Triệu hồi ngọn lửa xanh giảm khả năng hồi phục của đối thủ
    DEC_28("dec28", 14, false, false, 0, 0f),// Giảm defense và magic resist
    RE_HP_INIT("reHpInit", 15, true, false, 800, 0f),   // Hồi máu trong khoảng thời gian
    BOMB("bomb", 16, true, false, 2000, 7f),// Siêu boom nổ tác dụng trong 1 vùng
    TOXIC("toxic", 18, false, false, 0, 0f), // gay doc va lay ra nhung muc tieu khac voi 50% co hoi, toi da 2000% magic atk power
    RUNA("runa", 19, true, true, 0, 0f),   // buff toc do danh
    BURNED("burned", 20, false, false, 0, 0f),   // gay dame = attack + max hp va reduces def
    BERSERK("berserk", 21, true, true, 0, 0f),     // tang atk speed, zen atk,zen magic atk, critdamage
    BLIZZARD("blizzard", 22, false, false, 0, 3f),   // multi hit water, slow, reduce deff,magic resist
    DECO("deco", 22, false, false, 0, 0f),   // giảm cooldown các phi tiêu khác đang được trang bị
    DAME_MAGIC("dameMagic", 23, true, false, 1500, 0f),   // cộng vào dame shuriken
    DEC_DEF("decDef", 24, false, false, 0, 0f),   // giảm % defense

    UP_LEVEL("upLevel", 25, false, false, 1000, 3f), //up level
    KIM_THAN_SKILL_1("kim1", 26, false, true, 1500, 0.5f), // tạo 1 vùng rồi sét đánh đến vùng đó, dính đòn bị đóng băng 2s
    KIM_THAN_SKILL_2("kim2", 27, false, false, 4000, 6f), // tạo 1 vùng siêu bất ổn quanh boss sau 3s kẻ định ở trong tầm ảnh hưởng sẽ bị dính sát thương
    KIM_THAN_SKILL_3("kim3", 28, false, false, 5000, 0.2f), // sau 3s tìm kiếm player sẽ tạo 1 cột sét đánh xuống điểm mà player đang đứng gây dame và làm chậm player
    THUY_THAN_1("thuy1", 29, false, true, 500, 1.5f),// Vụ nổ trên room
    THUY_THAN_2("thuy2", 30, false, false, 3000, 3f),// Tạo 1 cơn bão cát làm chậm player khi đi vào đồng thời giảm giáp và kháng phép
    THUY_THAN_3("thuy3", 31, false, true, 0, 3f),// Tạo 1 con sóng di chuyển về phía player với sát thương lớn
    HOA_THAN_NORMAL("hoa1", 32, false, true, 0, 2f),// Khi phát nổ tạo ra 1 vùng lửa đốt tồn tại trong 2s
    HOA_THAN_2("hoa2", 33, false, true, 5000, 2f),// Boss vận công tạo ra vùng lửa ngẫu nhiên quanh bản đồ tồn tại trong 3s
    HOA_THAN_3("hoa3", 34, false, true, 0, 2f),// ném 3 quả cầu đến các vị trí ngẫu nhiên, player đi gần quả cầu sẽ bay theo và gây sát thương lên player
    THO_THAN_1("tho1", 35, false, false, 3000, 2f),// ném ra cục đá,khi bị tiêu diệt sẽ phát sinh thành tảng đá ngăn  chặn đạn của player
    THO_THAN_2("tho2", 36, true, true, 3000, 1.5f),// hất player và làm choáng
    PET_RE_HP("petReHp", 37, false, true, 0, 0),//Pet buff hp
    MOC_THAN_1("moc1", 38, false, true, 0, 0), // ném ra hạt giống
    MOC_THAN_2("moc2", 39, false, true, 0, 0), //
    MOC_THAN_3("moc3", 40, true, true, 0, 4f), // gan bom
    MOC_THAN_4("moc4", 41, false, false, 0, 0), // hất player và làm choáng
    MOC_THAN_5("moc5", 42, false, false, 0, 0), // hất player và làm choáng
    MOC_THAN_6("moc6", 43, false, false, 0, 0), // tạo ra một vùng nổ gây sát thương lên player ở trong phạm vi
    MOC_THAN_7("moc7", 44, false, false, 0, 0), // tạo ra một vùng nổ gây sát thương lên player ở trong phạm vi
    MOC_THAN_8("moc8", 45, false, false, 3000, 3), // tạo ra một vùng nổ gây sát thương lên player ở trong phạm vi
    MOC_THAN_9("moc9", 63, false, false, 3000, 3), // gây sát thương lên player và làm choáng
    PET_BUFF_SHELL("petBuffShell", 46, false, true, 0, 0),//Pet buff shell
    PET_BUFF_ATK("petBuffAtk", 47, false, true, 0, 0),
    PET_BUFF_MATK("petBuffMAtk", 48, false, true, 0, 0),
    PET_BUFF_DEF("petBuffDef", 49, false, true, 0, 0),
    PET_BUFF_MAGIC_RESIST("petBuffMS", 50, false, true, 0, 0),
    PET_BUFF_CRIT("petBuffCrit", 51, false, true, 0, 0),
    PET_BUFF_CRIT_DAME("petBuffCritDame", 52, false, true, 0, 0),
    PET_BUFF_DEC_DAME("petBuffDecDame", 53, false, true, 0, 0),
    PET_BUFF_TRUE_DAME("petBuffTrueDame", 54, false, true, 0, 0),
    PET_BUFF_DODGE("petBuffDodge", 55, false, true, 0, 0),
    PET_BUFF_SPD("petBuffSpd", 56, false, true, 0, 0),
    PET_BUFF_DAMAGE("petBuffDame", 57, false, true, 0, 0),
    PET_DEF_REST("petBuffDefRest", 58, false, true, 0, 0),
    DAME_MAGIC2("dameMagic2", 59, true, false, 1000, 0f),
    DEC_DEF2("decDef2", 60, false, false, 1500, 0f),   // giảm % defense
    RE_HP12("reHp12", 61, true, true, 1500, 0f), // hút máu
    DAME_MAGIC3("dameMagic3", 62, true, false, 1500, 0f),   // cộng vào dame shuriken
    ;

    public String name;
    public int id;
    public boolean isIncreBody;// cộng dồn trên character hay k
    public boolean isIncreRoom;// cộng dồn trên room hay k
    public long timeEffect; // thời gian hoạt động của effect 1 lần, tùy theo client, nếu lặp thì play theo server config
    public float radius;

    EffectType(String name, int id, boolean isIncreBody, boolean isIncreRoom, long timeEffect, float radius) {
        this.id = id;
        this.name = name;
        this.isIncreBody = isIncreBody;
        this.isIncreRoom = isIncreRoom;
        this.timeEffect = timeEffect;
        this.radius = radius;
    }


    static Map<String, EffectType> lookup = new HashMap<>();
    static Map<Integer, EffectType> lookupId = new HashMap<>();

    static {
        for (EffectType target : EffectType.values()) {
            lookup.put(target.name, target);
            lookupId.put(target.id, target);
        }
    }

    public static EffectType get(String value) {
        return lookup.get(value);
    }

    public static EffectType get(int value) {
        return lookupId.get(value);
    }
}
