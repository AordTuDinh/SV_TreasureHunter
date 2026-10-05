package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * Loại quest tuần tự. {@code quest_type} trong DB là {@code [type, idInfo]}.
 * Tiến độ do chỗ chơi gọi {@code checkQuestTutorial} / {@code checkQuestTutDefault}; luồng nhận thưởng chỉ so số đã lưu với {@code num}.
 */
public enum QuestTutType {
    NULL(0, ""),
    HARVEST_BOX(1, "Khai thác hộp"),
    DIG_SOIL(2, "Đào đất"),
    KILL_ENEMY(3, "Đánh bại quái"),
    EQUIP(4, "Mặc trang bị"),
    UPGRADE_EQUIP(5, "Nâng cấp trang bị"),
    DIG_STONE(6, "Đào đá"),
    UPGRADE_STONE(7, "Nâng đá"),
    MEET_NPC(8, "Gặp NPC"),
    MERGE_STONE(9, "Hợp nhất đá"),
    CRAFT_EQUIP(10, "Chế tạo trang bị"),
    CRAFT_POTION(11, "Chế tạo thuốc"),
    EQUIP_LEGENDARY(12, "Mặc đồ huyền thoại"),
    COMBINE_STONE(13, "Ghép đá"),
    AWAKEN_PET(14, "Thức tỉnh thú cưng"),
    CRAFT_WING(15, "Chế tạo cánh"),
    KILL_PLAYER(16, "Đánh bại người chơi"),
    FORGE_LEVEL(17, "Đạt cấp lò rèn"),
    HAS_MATERIAL_RANK(18, "Sở hữu nguyên liệu"),
    HAS_POWER(19, "Đạt lực chiến"),
    ARENA(20, "Tham gia đấu trường"),
    JOIN_CLAN(21, "Tham gia bang hội"),
    OPEN_CHEST(22, "Mở rương");

    public final int value;
    public final String label;

    QuestTutType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    static Map<Integer, QuestTutType> lookup = new HashMap<>();

    static {
        for (QuestTutType item : values()) {
            lookup.put(item.value, item);
        }
    }

    public static QuestTutType get(int id) {
        return lookup.get(id);
    }
}
