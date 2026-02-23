package game.dragonhero.server;


import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Constans implements Serializable {
    public static LocalDateTime ldt = LocalDateTime.of(2025, 11, 7, 11, 1,0);
    public static Date timeOpenServer =  Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    public static List<Integer> UNSYNC_SERVICE = Arrays.asList();
    public static final List<String> MAGICS = Arrays.asList("T1", "T2");
    public static final String MAGIC_OUT_GAME = "T1";
    public static final String MAGIC_IN_PUT = "T2";
    public static final int PROTOCOL_TCP = 1;

    public static final String KEY_PROTOCOL = "protocol";


    /*
     * ******** CUSTOM PROTO ***************
     */
    public static final int TYPE_ADD_OR_REMOVE = 1;
    public static final int TYPE_POS = 2;
    public static final int TYPE_UPDATE_CHARACTER = 3;

    // ******** TYPE_ADD_OR_REMOVE ***************
    public static final int TYPE_PLAYER = 1;
    public static final int TYPE_MONSTER = 2;
    public static final int TYPE_BULLET = 3;
    public static final int TYPE_PET = 4;
}
