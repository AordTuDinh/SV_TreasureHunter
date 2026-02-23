package game.dragonhero.dao;

import ozudo.base.database.DBJPA2;
import ozudo.base.helper.GUtil;
import ozudo.base.log.Logs;

import javax.persistence.EntityManager;
import java.util.List;

public class UserMailDAO {
    public boolean hasMail(int userId) {
        EntityManager session = null;
        try {
            session = getEntityManager();
            String sql = "select 1 from user_mail where user_id=%s and receive=0 limit 1";
            List list = session.createNativeQuery(String.format(sql, userId)).getResultList();
            return !list.isEmpty();
        } catch (Exception ex) {
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return false;
    }

    protected EntityManager getEntityManager() {
        return DBJPA2.getEntityManager();
    }

    protected void closeSession(EntityManager session) {
        DBJPA2.closeSession(session);
    }
}
