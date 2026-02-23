package game.dragonhero.service.battle;

import java.util.HashMap;
import java.util.Map;

public enum ClientSendType {
    EFFECT(1),

    ;


    ClientSendType(int id) {
        this.id = id;
    }

    public int id;
    static Map<Integer, ClientSendType> lookup = new HashMap<>();

    static {
        for (ClientSendType c : ClientSendType.values()) lookup.put(c.id, c);
    }


    public static ClientSendType get(int value) {
        return lookup.get(value);
    }

}
