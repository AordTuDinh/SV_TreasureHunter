package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum ChatType {
    MSG(0),
    ;

    public final int value;

    ChatType(int value) {
        this.value = value;
    }

    //region Lookup
    static Map<Integer, ChatType> lookUp = new HashMap<>();

    static {
        for (ChatType chatType : values())
            lookUp.put(chatType.value, chatType);
    }

    public static ChatType get(int value) {
        return lookUp.get(value);
    }
    //endregion
}