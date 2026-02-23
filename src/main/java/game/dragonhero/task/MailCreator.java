package game.dragonhero.task;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import game.cache.JCache;
import game.cache.JCachePubSub;
import game.dragonhero.mapping.UserMailEntity;
import game.dragonhero.task.dbcache.MailCreatorCache;
import game.pubsub.PubSubService;
import org.hibernate.Session;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import ozudo.base.database.DBJPA2;
import ozudo.base.log.Logs;

import javax.persistence.EntityManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Set;

@DisallowConcurrentExecution
public class MailCreator extends JobCounter implements Job {

    private static final int numberPop = 20;

    @Override
    public void executeJob() {
        int counter = 0, numberUpdate = 0;
        try {
            Set<String> values = getQueue();
            while (values != null && !values.isEmpty()) {
                counter++;
                if (counter % 1000 == 0) getLogger().warn("mailCreator continue=" + counter);
                numberUpdate += values.size();
                if (!dbUpdate(values)) {
                    for (String value : values) {
                        Logs.getMailLogger().error(value);
                    }
                } else {
                    for (String value : values) {
                        Logs.getMailLogger().info(value);
                    }
                }
                values = getQueue();
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
        if (counter > 0)
            getLogger().warn(String.format("mailCreator summary count=%s, update=%s", counter, numberUpdate));
    }

    private Set<String> getQueue() {
        return JCache.getInstance().spop(MailCreatorCache.queueKey, numberPop);
    }

    private boolean dbUpdate(Set<String> values) {
        EntityManager em = null;
        try {
            em = DBJPA2.getEntityManager();
            em.getTransaction().begin();
            Session session = em.unwrap(Session.class);
            session.doWork(connection -> {
                Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
                PreparedStatement statement = connection.prepareStatement("insert into user_mail(user_id, sender_id, title,sender_name, message, bonus, date_created,mail_idx) " + "values(?,?,?,?,?,?,?,?)");
                for (String value : values) {
                    UserMailEntity uMail = gson.fromJson(value, UserMailEntity.class);
                    statement.setInt(1, uMail.getUserId());
                    statement.setInt(2, uMail.getSenderId());
                    statement.setString(3, uMail.getTitle());
                    statement.setString(4, uMail.getSenderName());
                    statement.setString(5, uMail.getMessage());
                    statement.setString(6, uMail.getBonus());
                    statement.setTimestamp(7, new Timestamp(uMail.getDateCreated().getTime()));
                    statement.setInt(8, uMail.getMailIdx());
                    statement.addBatch();
                    JCachePubSub.getInstance().publish(JCachePubSub.allGameChannel, PubSubService.MAIL_NOTIFY.getMessage(String.valueOf(uMail.getUserId())));
                }
                statement.executeBatch();
            });
            em.getTransaction().commit();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            Logs.error(ex);
        } finally {
            DBJPA2.closeSession(em);
        }
        return false;
    }

}
