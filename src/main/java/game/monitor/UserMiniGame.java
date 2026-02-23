package game.monitor;

import game.object.KeoBuaBao;

import java.util.HashMap;
import java.util.Map;

public class UserMiniGame {
    public static Map<Integer, KeoBuaBao> keoBuaBao = new HashMap<>();

    public static void addKb(KeoBuaBao kb) {
        if (!keoBuaBao.containsKey(kb.userId)) {
            keoBuaBao.put(kb.userId, kb);
        } else {
            keoBuaBao.get(kb.userId).reset();
        }
    }
}
