package ozudo.base.helper;

import org.apache.commons.lang.StringEscapeUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DBHelper {

    public static String sqlMail(int userId, String title, String bonus) {
        return String.format("insert into user_mail(user_id, title, bonus, mail_idx) values(%s,'%s','%s','%s')", userId, StringEscapeUtils.escapeSql(title), bonus, DateTime.getDateyyyyMMdd());
    }

    public static String sqlMail(int userId, String title, String bonus, Date availableTime) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return String.format("insert into user_mail(user_id, title, bonus, mail_idx, available_time) values(%s,'%s','%s','%s','%s')", userId, StringEscapeUtils.escapeSql(title), bonus, DateTime.getDateyyyyMMdd(),df.format(availableTime));
    }

    public static String sqlMail(int userId, String title, String message, String bonus) {
        return String.format("insert into user_mail(user_id, title, message, bonus, mail_idx) values(%s,'%s','%s','%s','%s')", userId, StringEscapeUtils.escapeSql(title), StringEscapeUtils.escapeSql(message), bonus, DateTime.getDateyyyyMMdd());
    }

    public static String sqlMail(int userId, String title, String message, String bonus, Date availableTime) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return String.format("insert into user_mail(user_id, title, message, bonus, mail_idx, available_time) values(%s,'%s','%s','%s','%s','%s')", userId, StringEscapeUtils.escapeSql(title), StringEscapeUtils.escapeSql(message), bonus, DateTime.getDateyyyyMMdd(),df.format(availableTime));
    }

    public static String sqlMailClan(int userId, String title, String message, String bonus) {
        return String.format("insert into user_mail(user_id, sender_id, title, message, bonus, mail_idx) values(%s,-1,'%s','%s','%s','%s')", userId, StringEscapeUtils.escapeSql(title), StringEscapeUtils.escapeSql(message), bonus, DateTime.getDateyyyyMMdd());
    }


    public static String sqlGem(int userId, int gem) {
        return String.format("update user set gem=gem+(%s) where id=%s", gem, userId);
    }
}
