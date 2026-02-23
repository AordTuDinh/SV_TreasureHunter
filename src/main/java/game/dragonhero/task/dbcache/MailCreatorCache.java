package game.dragonhero.task.dbcache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import game.cache.CacheStoreBeans;
import game.cache.JCache;
import game.config.CfgServer;
import game.config.lang.Lang;
import game.dragonhero.mapping.UserEntity;
import game.dragonhero.mapping.UserMailEntity;
import game.dragonhero.mapping.main.SystemMailEntity;
import ozudo.base.database.DBJPA;
import ozudo.base.log.Logs;

import java.util.Arrays;
import java.util.List;

public class MailCreatorCache {
    public static final String queueKey = "queue_mail_creator", cacheKey = "system_mail";

    public void addMail(UserMailEntity uMail) {
        JCache.getInstance().sadd(queueKey, new Gson().toJson(uMail));
    }

    private static String getKey(int userId, int mailId) {
        return String.format("%s:%s", cacheKey, userId + "_" + mailId);
    }

    public static void sendMail(List<UserMailEntity> aMail) {
        for (UserMailEntity mail : aMail) {
            if (!sendMail(mail)) {
                Logs.getMailLogger().error(new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create().toJson(mail));
            }
        }
    }

    public static boolean sendMail(UserMailEntity uMail) {
        String data = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create().toJson(uMail);
        if (JCache.getInstance().sadd(queueKey, data) >= 0) {
            Logs.getMailLogger().info(data);
            return true;
        }
        return false;
    }

    public static void sendSystemMail(UserEntity user, SystemMailEntity mail) {
        if (!isSendMail(user.getId(), mail)) {
            if (DBJPA.insert("user_system_mail", Arrays.asList("user_id", "system_mail_id"), Arrays.asList(user.getId(), mail.getId())) != -1) {
                CacheStoreBeans.cache10Min.add(getKey(user.getId(), mail.getId()), 1);
                UserMailEntity uMail = UserMailEntity.builder().userId(user.getId()).senderId(0).senderName( Lang.getTitle(CfgServer.config.mainLanguage,Lang.mail_sender_system))
                        .title(mail.getTitle()).message(mail.getMessage()).bonus(mail.getBonus())
                        .build().initDefault();
                String data = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create().toJson(uMail);
                if (JCache.getInstance().sadd(queueKey, data) >= 0) {
                    Logs.getMailLogger().info(data);
                } else Logs.getMailLogger().error(data);
            }
        }
    }

    public static boolean isSendMail(int userId, SystemMailEntity mail) {
        Integer number = CacheStoreBeans.cache10Min.get(getKey(userId, mail.getId()));
        if (number == null) {
            number = DBJPA.count("user_system_mail", "user_id", userId, "system_mail_id", mail.getId());
            CacheStoreBeans.cache10Min.add(getKey(userId, mail.getId()), number);
        }
        return number != 0;
    }

}
