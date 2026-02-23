package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum QuestType {
    NULL(0),
    QUEST_D(1),
    QUEST_C(2),
    QUEST_B(3),
    QUEST_MONTH(4),
    ;
    public final int value;

    QuestType(int value) {
        this.value = value;
    }

    static Map<Integer, QuestType> lookup = new HashMap<>();

    static {
        for (QuestType item : values()) {
            lookup.put(item.value, item);
        }
    }

    public static QuestType get(int id) {
        return lookup.get(id);
    }
}
