package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum QuestTutType {
    NULL(0,""),
    KILL_ENEMY(1,"TUT_KILL_ENEMY"),
    UP_SKILL_1(2,"TUT_UP_SKILL_1"),
    UP_SKILL_2(3,"TUT_UP_SKILL_2"),
    USE_ITEM_AUTO(4,"TUT_USE_ITEM_AUTO"),
    SUMMON_STONE(5,"TUT_SUMMON_STONE"),
    UPGRADE_WEAPON(6,"TUT_UPGRADE_WEAPON"),
    CREATE_WEAPON(7,"TUT_CREATE_WEAPON"),
    USE_WEAPON(8,"TUT_USE_WEAPON"),
    ATTACK_BOSS_GOD(9,"TUT_ATTACK_BOSS_GOD"),
    UPGRADE_ITEM_EQUIP(10,"TUT_UPGRADE_ITEM_EQUIP"),
    USE_ITEM_EQUIP(11,"TUT_USE_ITEM_EQUIP"),
    BUY_LAND(12,"TUT_BUY_LAND"),
    PLAN_FARM(13,"TUT_PLAN_FARM"),
    USE_FERTILIZER(14,"TUT_USE_FERTILIZER"),
    HARVEST(15,"TUT_HARVEST"),
    SHIP(16,"TUT_SHIP"),
    HAS_LEVEL_TOWER(17,"TUT_HAS_LEVEL_TOWER"), // check end attack
    USE_SPINE_ROTATE(18,"TUT_USE_SPINE_ROTATE"),
    ATTACK_ARENA(19,"TUT_ATTACK_ARENA"),
    SEND_REQUEST_FRIEND(20,"TUT_SEND_REQUEST_FRIEND"),
    SEND_FRIEND_GIFT(21,"TUT_SEND_FRIEND_GIFT"),
    SUMMON_PIECE(22,"TUT_SUMMON_PIECE"),
    HAS_LEVEL(23,"TUT_HAS_LEVEL"),
    BUY_SHOP(24,"TUT_BUY_SHOP"),
    HAS_LAND(25,"TUT_HAS_LAND"),
    GET_BONUS_ONLINE(26,"TUT_GET_BONUS_ONLINE"),
    BUY_GOLD(27,"TUT_BUY_GOLD"),
    GET_SUPPORT(28,"TUT_GET_SUPPORT"),
    JOIN_CLAN(29,"TUT_JOIN_CLAN"),
    HAS_COMBO_WEAPON(30,"TUT_HAS_COMBO_WEAPON"),
    HAS_WEAPON_ID(31,"TUT_HAS_WEAPON_ID"),
    HAS_TREE(32,"TUT_HAS_TREE"),
    HAS_MONSTER(33,"TUT_HAS_MONSTER"),
    HAS_PET(34,"TUT_HAS_PET"),
    CARE_PET_MONSTER(35,"TUT_CARE_PET_MONSTER"),
    SUMMON_PET(36,"TUT_SUMMON_PET"),
    HAS_POINT_D(37,"TUT_HAS_POINT_D"), // has handle
    HAS_ITEM_EQUIP_LEVEL(38,"TUT_HAS_ITEM_EQUIP_LEVEL"), // Cường hóa item name lên cấp %s
    HAS_ITEM_EQUIP_ID(39,"TUT_HAS_ITEM_EQUIP_ID"), // Sở hữu trang bị %s
    HAS_WEAPON_BY_RANK(40,"TUT_HAS_WEAPON_BY_RANK"), // Sở hữu 2 phi tiêu rank hiếm
    SMART_TOWER(41,"TUT_SMART_TOWER"), // Càn quét tháp
    USE_ITEM(42,"TUT_USE_ITEM"), // Sử dụng item id - num
    USE_ITEM_CAMPAIGN_SMART(43,"TUT_USE_ITEM_CAMPAIGN_SMART") // Sử dụng thẻ càn quét ải
    ;
    public final int value;
    public final String     keyLang;

    QuestTutType(int value,String keyLang) {
        this.value = value;
        this.keyLang = keyLang;
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
