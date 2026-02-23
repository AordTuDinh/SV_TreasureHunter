package ozudo.base.log;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.Filer;
import ozudo.base.helper.GUtil;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;

public class Logs {
    static HashSet<String> mError = new HashSet<>();
    static String curDate = "";
    static boolean realServer = true;
    private static String[][] ignoreErr = {
            {".CCUCounter", "Duplicate entry"}
    };

    public static void init(boolean realServer) {
        Logs.realServer = realServer;
    }

    public static void warn(String msg) {
        slib_Logger.root().warn(msg);
    }

    public static void error(String debug, Exception ex) {
//        error(GUtil.exToString(ex));
        slib_Logger.root().error(debug, ex);
    }

    public static void error(Exception ex) {
        slib_Logger.root().error("", ex);
    }

    public static void event(String msg) {
        slib_Logger.event().info(msg);
    }

    public static void error(String msg) {
        error(msg, true);
    }

    public static void apiErr(String msg) {
        slib_Logger.api().error(msg);
    }

    public static void slow(String msg) {
        LoggerFactory.getLogger("SLOW").info(msg);
    }

    public static void error(String msg, boolean mail) {
        slib_Logger.root().error(msg);
        try {
            if (!mError.contains(msg)) {
                String[] ignore = Arrays.stream(ignoreErr).filter(values -> {
                    for (String value : values) {
                        if (!msg.contains(value)) return false;
                    }
                    return true;
                }).findFirst().orElse(null);
                if (ignore == null) {
                    mError.add(msg);
                    Filer.append("logs/errorUnique.log", String.format("%s -> %s", DateTime.getFullDate(Calendar.getInstance().getTime()), msg));
                    //
//                    MailNotify.teleNotify(String.format("%s -> %s", DateTime.getFullDate(Calendar.getInstance().getTime()), msg));
                }
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    public static void debug(String msg) {
        slib_Logger.root().debug(msg);
    }

    public static void info(String msg) {
        slib_Logger.root().info(msg);
    }

    public static void save(Object obj) {
        Session session = null;
        try {
            session = DBJPA.getEntityManager().unwrap(Session.class);
            session.beginTransaction();
            session.save(obj);
            session.getTransaction().commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            slib_Logger.root().error(GUtil.exToString(ex));
        } finally {
            try {
                if (session != null) {
                    session.close();
                }
            } catch (Exception ex) {
            }
        }
    }

    public static Logger getMailLogger() {
        return LoggerFactory.getLogger("MAIL");
    }
}
