package game.battle.type;

import game.battle.object.Point;

import java.util.HashMap;
import java.util.Map;

public enum StateType {
    ADD_BONUS(1, -1), // custom size
    DIE(2, 1),// faction
    REVIVE(3, 2),
    PLAY_ANIM(4,1),
    BE_DAMAGE(5, 4), // attackerId - crit  - atkDame - magDame





    EFFECT_BODY(11, 2), // effect Id, time

    RE_HP(12, 1), // recovery hp
    UPDATE_CHAT_FRAME(13,1),
    UPDATE_TRIAL(14,1),

    UPDATE_ITEM_SLOT(15, 4), // item buf in player
    UPDATE_TEXT_DAME(16,1),


    UPDATE_MULTI_POINT(17, -1), //  [point id - cur] lưu ý chỉ dùng số ít point chứ k phải toàn bộ point

    USE_ITEM_SLOT(18, 1), //  slot
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
