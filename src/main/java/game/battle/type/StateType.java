package game.battle.type;

import game.battle.object.Point;

import java.util.HashMap;
import java.util.Map;

public enum StateType {
    ADD_BONUS(1, -1), // custom size
    DIE(2, 1),// faction
    REVIVE(3, 2),
    BE_DAMAGE(4, 4), // attackerId - crit  - atkDame - magDame
    PLAY_ANIMATION(5, 1),// // animation id
    USE_SKILL(6, 6), // skill - time - numActive - driection x - y - activeSkill

    RANGE_DAMAGE(7, 7), //attackerId - crit  - atkDame - magDame - faction - posx - posy

    EFFECT_DAME(8, 3),// attackerId - atkDame - magDame

    EFFECT(9, 4),// effectId -  pos - timeActive ;

    UPDATE_COOL_DOWN(10, 5),// time cd by slot (ms)

    EFFECT_BODY(11, 2), // effect Id, time

    RE_HP(12, 1), // recovery hp

    SET_ALL_POINT(13, Point.size), //dame

    SWITCH_TARGET(14, 1),// target id

    UPDATE_ITEM_SLOT(15, 4), // item buf in player

    UPDATE_AVATAR(16, 5), // type-  avatarId - heroId - frame - skin

    UPDATE_MULTI_POINT(17, -1), //  [point id - cur] lưu ý chỉ dùng số ít point chứ k phải toàn bộ point

    USE_ITEM_SLOT(18, 1), //  slot

    UPDATE_ARENA_POINT(19, 2),// point p1, point p2 ,
    CHANGE_HERO_ARENA_DIRECTION(20, 2),// dirX - dirY
    UPDATE_PER_HP_ARENA(21, 2),// team - per(100)
    PET_USE_SKILL(22, 1), // team
    CLIENT_SKILL(23, -1), // dữ liệu cho client sử dụng để play skill
    CHANGE_TARGET_EFFECT(24, 3), //  [effect Type - Effect Id - targetId new]  -> target = -1 -> null :
    CHANGE_PET(25, 1), // pet id = 0 null
    UPDATE_TEXT_DAME(26, 1), // idText Hit
    BONUS_ADD_FORCE(27, -1),//custom size
    UPDATE_NUM_POINT_LEVEL_STAT(29, 1),// cur point
    UPDATE_CHAT_FRAME(30, 1),// id
    UPDATE_TRIAL(31, 1),// id
    UPDATE_NUMBER_KILL_TOWER(32, 2), // cur
    UPDATE_ITEM_EQUIP(33,8), // list item ids db
    BONUS_SHARE_PARTY(34,-1), // share bonus party
    ;

    public int id, length;

    StateType(int id, int length) {
        this.id = id;
        this.length = length;
    }

    // lookup
    static Map<Integer, StateType> lookup = new HashMap<>();

    static {
        for (StateType itemType : values()) {
            lookup.put(itemType.id, itemType);
        }
    }

    public static StateType get(int type) {
        return lookup.get(type);
    }
}
