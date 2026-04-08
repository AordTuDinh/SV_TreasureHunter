package game.treasure.server;


import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class Constans implements Serializable {
    public static LocalDateTime ldt = LocalDateTime.of(2025, 11, 7, 11, 1, 0);
    public static Date timeOpenServer = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    public static List<Integer> UNSYNC_SERVICE = Arrays.asList();
    public static final List<String> MAGICS = Arrays.asList("T1", "T2");
    public static final String MAGIC_OUT_GAME = "T1";
    public static final String MAGIC_IN_PUT = "T2";
    public static final int PROTOCOL_TCP = 1;

    public static final String KEY_PROTOCOL = "protocol";


    // ******** RUNTIME MAP ***************

    public static final Map<Long, String> mIdToBattleId = new HashMap<>();
    public static long counterId;

    //region state
    public static synchronized long getCounterId() {
        return ++counterId;
    }

    public static String[] getKeyRoomById(long battleId) {
        return (mIdToBattleId.getOrDefault(battleId, "0_0")).split("_");
    }

}
