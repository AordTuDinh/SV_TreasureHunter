package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum TriggerEventTimer {
    LEVEL(1),
    OPEN_LAND(2),
    QUEST_TUTORIAL_LEVEL(3),
    TIME(4), // time - level open
    ;

    public final int value;

    TriggerEventTimer(int value) {
        this.value = value;
    }

    //region Lookup
    static Map<Integer, TriggerEventTimer> lookUp = new HashMap<>();

    static {
        for (TriggerEventTimer chatType : values())
            lookUp.put(chatType.value, chatType);
    }

    public static TriggerEventTimer get(int value) {
        return lookUp.get(value);
    }
    //endregion
}
