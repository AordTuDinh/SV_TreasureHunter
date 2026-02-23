package game.dragonhero.service.user;

import com.google.gson.JsonObject;
import game.dragonhero.mapping.UserEntity;
import org.slf4j.Logger;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.Filer;
import ozudo.base.log.slib_Logger;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Actions {
    public static String GRECEIVE = "receive";
    public static String GUSER = "user";
    public static String GBUY = "buyproduct";
    public static String GBUYFAIL = "buyfail";
    public static String GCLAN = "clan";
    public static String DCHAT = "chat";
    public static String DAREQ = "areq";
    public static String DCREATE = "create";
    public static String DKICK = "kick";
    public static String DLEAVE = "leave";
    public static String DDESTROY = "destroy";

    public static void save(UserEntity user, String gAction, String dAction, Object... desc) {
        save(user.getServer(), user.getId(), gAction, dAction, desc);
    }

    public static void save(int server, int userId, String gAction, String dAction, Object... desc) {
        save(server, userId, gAction, dAction, toLogString(desc));
    }

    public static void save(int server, int userId, String gAction, String dAction, String desc) {
        String value = server + "u" + userId + " " + gAction + " " + dAction + " " + desc;
//        getLogger().info(value);
        //
        String path = String.format("logs/activity/server%s/", server);
        String curDate = DateTime.getDateyyyyMMdd(Calendar.getInstance().getTime());
        File file = new File(path + curDate);
        if (!file.exists()) file.mkdirs();
        //
        Filer.append(path + curDate + "/" + userId + ".log", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()) + "\t" + value);
    }

    public static String toLogString(Object... values) {
        JsonObject obj = new JsonObject();
        for (int i = 0; i < values.length / 2; i++) {
            obj.addProperty(values[i * 2].toString(), values[i * 2 + 1].toString());
        }
        return obj.toString();
    }

    public static void logGem(UserEntity user, String detailAction, long addValue) {
        save(user, Actions.GRECEIVE, detailAction, "type", "gem",
                "value", user.getGem(), "addValue", addValue);
    }

    public static void logGold(UserEntity user, String detailAction, long addValue) {
        save(user, Actions.GRECEIVE, detailAction, "type", "gold",
                "value", user.getGold(), "addValue", addValue);
    }

    public static Logger getLogger() {
        return slib_Logger.act();
    }
}
