package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum FactionType {
    NULL(0, 0, 0, 0),
    KIM(1, 40, 2, 4),
    MOC(2, 43, 5, 1),
    THUY(3, 41, 4, 5),
    HOA(4, 42, 1, 3),
    THO(5, 44, 3, 2),
    ;

    public final int value;
    public final int itemId;
    public final int winFaction;
    public final int lostFaction;

    FactionType(int value, int itemId, int winFaction, int lostFaction) {
        this.value = value;
        this.itemId = itemId;
        this.winFaction = winFaction;
        this.lostFaction = lostFaction;
    }

    // lookup
    static Map<Integer, FactionType> lookup = new HashMap<>();

    static {
        for (FactionType itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public boolean isWin(FactionType factionType) {
        return winFaction == factionType.value;
    }

    public boolean isLost(FactionType factionType) {
        return lostFaction == factionType.value;
    }

    public static FactionType get(int type) {
        return lookup.get(type);
    }
}
