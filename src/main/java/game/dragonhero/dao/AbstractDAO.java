package game.dragonhero.dao;

import game.config.CfgServer;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.GUtil;
import ozudo.base.helper.QueryCallBackDAO;
import ozudo.base.helper.RootLogger;
import ozudo.base.helper.UpdateCallBackDAO;
import ozudo.base.log.Logs;

import javax.persistence.EntityManager;

public class AbstractDAO implements RootLogger {
    protected EntityManager getEntityManager() {
        return DBJPA.getEntityManager();
    }

    protected void closeSession(EntityManager session) {
        DBJPA.closeSession(session);
    }

    protected void exceptionCatch(Exception ex) {
        ex.printStackTrace();
        getLogger().error(GUtil.exToString(ex));
    }

    public Boolean save(Object object) {
        return DBJPA.save(object);
    }

    public Boolean update(Object object) {
        return DBJPA.update(object);
    }

    protected Boolean doUpdate(UpdateCallBackDAO callback) {
        long curTime = System.currentTimeMillis();
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            Object object = callback.onUpdate(em);
            em.getTransaction().commit();
            if (object instanceof Boolean) return (Boolean) object;
            return true;
        } catch (Exception ex) {
            exceptionCatch(ex);
        } finally {
            closeSession(em);
            slowLog(curTime, String.format("SQL doUpdate %s", getClass().getSimpleName()));
        }
        return false;
    }

    protected <T> T doQuery(QueryCallBackDAO callback) {
        long curTime = System.currentTimeMillis();
        EntityManager em = getEntityManager();
        try {
            return (T) callback.onQuery(em);
        } catch (Exception ex) {
            exceptionCatch(ex);
        } finally {
            closeSession(em);
            slowLog(curTime, String.format("SQL doQuery %s", getClass().getSimpleName()));
        }
        return null;
    }

    private void slowLog(long curTime, String msg) {
        long timePass = System.currentTimeMillis() - curTime;
        if (timePass >= CfgServer.getSlowSQLTime()) {
            Logs.slow(String.format("%s -> %s", msg, timePass));
        }
    }
}
