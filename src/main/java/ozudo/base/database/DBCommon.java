package ozudo.base.database;

import com.google.gson.Gson;
import game.config.CfgServer;
import org.hibernate.Session;
import org.slf4j.Logger;
import ozudo.base.helper.GUtil;
import ozudo.base.log.Logs;
import ozudo.base.log.slib_Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

public class DBCommon {
    public EntityManagerFactory emFactory;

    public void init(String managerName) {
        System.out.println("Init entity manager = " + managerName);
        emFactory = Persistence.createEntityManagerFactory(managerName);
    }

    /**
     * insert method
     *
     * @param table table name
     */
    public int insert(String table, List<Object> keys, List<Object> values) {
        long curTime = System.currentTimeMillis();
        String sql = "";
        EntityManager session = null;
        try {
            session = emFactory.createEntityManager();
            session.getTransaction().begin();
            String fieldNames = "";
            String fieldValues = "";
            for (int i = 0; i < keys.size(); i++) {
                fieldNames += "," + keys.get(i);
                fieldValues += ",:" + keys.get(i);
            }
            fieldNames = fieldNames.substring(1);
            fieldValues = fieldValues.substring(1);
            sql = String.format("INSERT INTO %s (%s) VALUES (%s)", table, fieldNames, fieldValues);
            Query query = session.createNativeQuery(sql);
            for (int i = 0; i < keys.size(); i++) {
                query.setParameter(keys.get(i).toString(), values.get(i).toString());
            }
            int result = query.executeUpdate();
            session.getTransaction().commit();
            return result;
        } catch (Exception ex) {
            ex.printStackTrace();
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
            slowLog(curTime, String.format("SQL A %s", sql));
        }
        return -1;
    }

    public boolean listQuery(List<String> aSql) {
        EntityManager session = null;
        try {
            session = emFactory.createEntityManager();
            session.getTransaction().begin();
            for (String sql : aSql) session.createNativeQuery(sql).executeUpdate();
            session.getTransaction().commit();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return false;
    }

    public String getInsertQuery(String table, List<String> keys, List<String> values) {
        String fieldNames = "";
        String fieldValues = "";
        for (int i = 0; i < keys.size(); i++) {
            fieldNames += "," + keys.get(i);
            fieldValues += ",'" + values.get(i) + "'";
        }
        fieldNames = fieldNames.substring(1);
        fieldValues = fieldValues.substring(1);
        return String.format("INSERT INTO %s (%s) VALUES (%s)", table, fieldNames, fieldValues);
    }

    public String getUpdateQuery(String table, List<Object> data, List<Object> whereValues) {
        String fieldDetails = "";
        for (int i = 0; i < data.size(); i += 2) {
            fieldDetails += String.format(",%s = '%s'", data.get(i), data.get(i + 1));
        }
        fieldDetails = fieldDetails.substring(1);
        //
        String whereDetails = "";
        for (int i = 0; i < whereValues.size(); i += 2) {
            if (i == 0) {
                whereDetails += String.format("where %s = '%s'", whereValues.get(i), whereValues.get(i + 1));
            } else {
                whereDetails += String.format(" and %s = '%s'", whereValues.get(i), whereValues.get(i + 1));
            }
        }
        //
        return String.format("update %s set %s %s", table, fieldDetails, whereDetails);
    }

    public String getDeleteQuery(String table, List<String> whereValues) {
        String whereDetails = "";
        for (int i = 0; i < whereValues.size(); i += 2) {
            if (i == 0) {
                whereDetails += String.format("where %s = '%s'", whereValues.get(i), whereValues.get(i + 1));
            } else {
                whereDetails += String.format(" and %s = '%s'", whereValues.get(i), whereValues.get(i + 1));
            }
        }
        //
        return String.format("delete from %s %s", table, whereDetails);
    }

    public String getSelectListQuery(List<String> sql) {
        return "";
    }

    public boolean deleteIn(String table, String columnIn, List<Object> inValues, Object... whereValues) {
        long curTime = System.currentTimeMillis();
        String sql = "";
        EntityManager session = null;
        try {
            session = emFactory.createEntityManager();
            session.getTransaction().begin();
            String strIn = inValues.stream().map(Object::toString).collect(Collectors.joining(","));
            String whereDetails = "";
            for (int i = 0; i < whereValues.length; i += 2) {
                whereDetails += String.format(" and %s = :%s", whereValues[i], whereValues[i]);
            }
            //
            sql = String.format("delete from %s where %s in (%s) %s", table, columnIn, strIn, whereDetails);
            Query query = session.createNativeQuery(sql);
            for (int i = 0; i < whereValues.length; i += 2) {
                query.setParameter(whereValues[i].toString(), whereValues[i + 1]);
            }
            query.executeUpdate();
            session.getTransaction().commit();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
            slowLog(curTime, String.format("SQL B %s", sql));
        }
        return false;
    }

    public  boolean exists(String table, Object... whereValues) {
        long curTime = System.currentTimeMillis();
        String sql = "";
        EntityManager session = null;
        try {
            session = emFactory.createEntityManager();

            // Build where
            StringBuilder whereDetails = new StringBuilder();
            for (int i = 0; i < whereValues.length; i += 2) {
                if (i == 0) {
                    whereDetails.append(String.format("WHERE %s = :%s", whereValues[i], whereValues[i]));
                } else {
                    whereDetails.append(String.format(" AND %s = :%s", whereValues[i], whereValues[i]));
                }
            }

            // SELECT 1 LIMIT 1
            sql = String.format("SELECT 1 FROM %s %s LIMIT 1", table, whereDetails.toString());

            Query query = session.createNativeQuery(sql);

            // Bind parameters
            for (int i = 0; i < whereValues.length; i += 2) {
                query.setParameter(whereValues[i].toString(), whereValues[i + 1]);
            }

            // getSingleResult() sẽ throw nếu không có row → dùng getResultList()
            return !query.getResultList().isEmpty();

        } catch (Exception he) {
            he.printStackTrace();
            Logs.error(GUtil.exToString(he));
        } finally {
            closeSession(session);
            slowLog(curTime, String.format("SQL EXISTS %s", sql));
        }
        return false;
    }

    public boolean delete(String table, Object... whereValues) {
        long curTime = System.currentTimeMillis();
        String sql = "";
        EntityManager session = null;
        try {
            session = emFactory.createEntityManager();
            session.getTransaction().begin();
            String whereDetails = "";
            for (int i = 0; i < whereValues.length; i += 2) {
                if (i == 0) {
                    whereDetails += String.format("where %s = :%s", whereValues[i], whereValues[i]);
                } else {
                    whereDetails += String.format(" and %s = :%s", whereValues[i], whereValues[i]);
                }
            }
            //
            sql = String.format("delete from %s %s", table, whereDetails);
            Query query = session.createNativeQuery(sql);
            for (int i = 0; i < whereValues.length; i += 2) {
                query.setParameter(whereValues[i].toString(), whereValues[i + 1]);
            }
            query.executeUpdate();
            session.getTransaction().commit();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
            slowLog(curTime, String.format("SQL C %s", sql));
        }
        return false;
    }

    public boolean update(String table, List<Object> data, List<Object> whereValues) {
        return update(null, table, data, whereValues);
    }

    public boolean update(EntityManager session, String table, List<Object> data, List<Object> whereValues) {
        long curTime = System.currentTimeMillis();
        String sql = "";
        boolean closeSession = session == null;
        try {
            if (session == null) session = emFactory.createEntityManager();
            session.getTransaction().begin();
            String fieldDetails = "";
            for (int i = 0; i < data.size(); i += 2) {
                fieldDetails += String.format(",%s = :%s", data.get(i), data.get(i));
            }
            fieldDetails = fieldDetails.substring(1);
            //
            String whereDetails = "";
            for (int i = 0; i < whereValues.size(); i += 2) {
                if (i == 0) {
                    whereDetails += String.format("where %s = :%s", whereValues.get(i), whereValues.get(i));
                } else {
                    whereDetails += String.format(" and %s = :%s", whereValues.get(i), whereValues.get(i));
                }
            }
            //
            sql = String.format("update %s set %s %s", table, fieldDetails, whereDetails);
            Query query = session.createNativeQuery(sql);
            for (int i = 0; i < data.size(); i += 2) {
                query.setParameter(data.get(i).toString(), data.get(i + 1));
            }
            for (int i = 0; i < whereValues.size(); i += 2) {
                query.setParameter(whereValues.get(i).toString(), whereValues.get(i + 1));
            }
            query.executeUpdate();
            session.getTransaction().commit();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            Logs.error(GUtil.exToString(ex));
        } finally {
            if (closeSession) closeSession(session);
            slowLog(curTime, String.format("SQL D %s", sql));
        }
        return false;
    }

    public boolean updateNumber(String table, List<Object> data, List<Object> whereValues) {
        long curTime = System.currentTimeMillis();
        String sql = "";
        EntityManager session = null;
        try {
            session = emFactory.createEntityManager();
            session.getTransaction().begin();
            String fieldDetails = "";
            for (int i = 0; i < data.size(); i += 2) {
                fieldDetails += String.format(",%s = %s + (:%s)", data.get(i), data.get(i), data.get(i));
            }
            fieldDetails = fieldDetails.substring(1);
            //
            String whereDetails = "";
            for (int i = 0; i < whereValues.size(); i += 2) {
                if (i == 0) {
                    whereDetails += String.format("where %s = :%s", whereValues.get(i), whereValues.get(i));
                } else {
                    whereDetails += String.format(" and %s = :%s", whereValues.get(i), whereValues.get(i));
                }
            }
            //
            sql = String.format("update %s set %s %s", table, fieldDetails, whereDetails);
            Query query = session.createNativeQuery(sql);
            for (int i = 0; i < data.size(); i += 2) {
                query.setParameter(data.get(i).toString(), data.get(i + 1));
            }
            for (int i = 0; i < whereValues.size(); i += 2) {
                query.setParameter(whereValues.get(i).toString(), whereValues.get(i + 1));
            }
            query.executeUpdate();
            session.getTransaction().commit();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
            slowLog(curTime, String.format("SQL E %s", sql));
        }
        return false;
    }

    public List getSelectQuery(String sql, Class aClass) {
        long curTime = System.currentTimeMillis();
        EntityManager session = null;
        try {
            session = emFactory.createEntityManager();
            session.getTransaction().begin();
            Query query = session.createNativeQuery(sql, aClass);
            return query.getResultList();
        } catch (Exception ex) {
            ex.printStackTrace();
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
            slowLog(curTime, String.format("SQL F %s", sql));
        }
        return null;
    }

    public List getList(String sql) {
        long curTime = System.currentTimeMillis();
        EntityManager session = null;
        try {
            session = emFactory.createEntityManager();
            return session.createNativeQuery(sql).getResultList();
        } catch (Exception ex) {
            ex.printStackTrace();
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
            slowLog(curTime, String.format("SQL G %s", sql));
        }
        return null;
    }

    public List getList(String table, List<Object> whereValues, String moreDetails, Class aClass) {
        long curTime = System.currentTimeMillis();
        String sql = "";
        EntityManager session = null;
        try {
            session = emFactory.createEntityManager();
            String whereDetails = "";
            for (int i = 0; i < whereValues.size(); i += 2) {
                if (i == 0) {
                    whereDetails += String.format("where %s = :%s", whereValues.get(i), whereValues.get(i));
                } else {
                    whereDetails += String.format(" and %s = :%s", whereValues.get(i), whereValues.get(i));
                }
            }
            sql = String.format("select * from %s %s %s", table, whereDetails, moreDetails);
            Query query = session.createNativeQuery(sql, aClass);
            for (int i = 0; i < whereValues.size(); i += 2) {
                query.setParameter(whereValues.get(i).toString(), whereValues.get(i + 1));
            }
            return query.getResultList();
        } catch (Exception ex) {
            ex.printStackTrace();
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
            slowLog(curTime, String.format("SQL H %s", sql));
        }
        return null;
    }

    public List getList(String sql, Class aClass, Object... values) {
        long curTime = System.currentTimeMillis();
        EntityManager session = null;
        try {
            session = emFactory.createEntityManager();
            Query query = session.createNativeQuery(sql, aClass);
            for (int i = 0; i < values.length; i += 2) {
                query.setParameter((String) values[i], values[i + 1]);
            }
            return query.getResultList();
        } catch (Exception ex) {
            ex.printStackTrace();
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
            slowLog(curTime, String.format("SQL I %s", sql));
        }
        return null;
    }

    public List getList(String table, Class aClass) {
        long curTime = System.currentTimeMillis();
        String sql = "";
        EntityManager session = null;
        try {
            session = emFactory.createEntityManager();
            sql = "select * from " + table;
            return session.createNativeQuery(sql, aClass).getResultList();
        } catch (Exception ex) {
            ex.printStackTrace();
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
            slowLog(curTime, String.format("SQL J %s", sql));
        }
        return null;
    }

    public Long getNumber(String sql) {
        long curTime = System.currentTimeMillis();
        EntityManager session = null;
        try {
            session = emFactory.createEntityManager();
            Query query = session.createNativeQuery(sql);

            Object obj = query.getSingleResult();

            if (obj == null) return 0L;

            if (obj instanceof Long)
                return (Long) obj;

            if (obj instanceof Integer)
                return ((Integer) obj).longValue();

            if (obj instanceof BigInteger)
                return ((BigInteger) obj).longValue();

            if (obj instanceof BigDecimal)
                return ((BigDecimal) obj).longValue();

            if (obj instanceof Number)
                return ((Number) obj).longValue();

            // fallback
            return Long.parseLong(obj.toString());

        } catch (Exception ex) {
            ex.printStackTrace();
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
            slowLog(curTime, String.format("SQL K %s", sql));
        }
        return 0L;
    }

    public Object getUnique(EntityManager session, String sql, Class aClass) {
        List aObj = session.createNativeQuery(sql, aClass).getResultList();
        return aObj.isEmpty() ? null : aObj.get(0);
    }

    public Object getUnique(String table, Class aClass, Object... whereValues) {
        return getUnique(null, table, aClass, whereValues);
    }

    public Object getUnique(EntityManager session, String table, Class aClass, Object... whereValues) {
        long curTime = System.currentTimeMillis();
        String sql = "";
        boolean closeSession = session == null;
        try {
            if (session == null) session = emFactory.createEntityManager();
            String whereDetails = "";
            for (int i = 0; i < whereValues.length; i += 2) {
                if (i == 0) {
                    whereDetails += String.format("where %s = :%s", whereValues[i], whereValues[i]);
                } else {
                    whereDetails += String.format(" and %s = :%s", whereValues[i], whereValues[i]);
                }
            }
            sql = String.format("select * from %s %s ", table, whereDetails);
            Query query = session.createNativeQuery(sql, aClass);
            for (int i = 0; i < whereValues.length; i += 2) {
                query.setParameter(whereValues[i].toString(), whereValues[i + 1]);
            }
            List listResult = query.getResultList();
            return listResult.isEmpty() ? null : listResult.get(0);
        } catch (Exception ex) {
            ex.printStackTrace();
            Logs.error(GUtil.exToString(ex));
        } finally {
            if (closeSession) closeSession(session);
            slowLog(curTime, String.format("SQL L %s", sql));
        }
        return null;
    }

    public String getUniqueColumn(String table, List<String> whereValues, String col) {
        long curTime = System.currentTimeMillis();
        String sql = "";
        EntityManager session = null;
        try {
            session = emFactory.createEntityManager();
            String whereDetails = "";
            for (int i = 0; i < whereValues.size(); i += 2) {
                if (i == 0) {
                    whereDetails += String.format("where %s = :%s", whereValues.get(i), whereValues.get(i));
                } else {
                    whereDetails += String.format(" and %s = :%s", whereValues.get(i), whereValues.get(i));
                }
            }
            sql = String.format("select %s from %s %s ", col, table, whereDetails);
            Query query = session.createNativeQuery(sql);
            for (int i = 0; i < whereValues.size(); i += 2) {
                query.setParameter(whereValues.get(i), whereValues.get(i + 1));
            }
            List result = query.getResultList();
            if (result.isEmpty()) return null;
            return result.get(0).toString();
        } catch (Exception he) {
            he.printStackTrace();
            Logs.error(col + " -> " + whereValues + " -> " + GUtil.exToString(he));
        } finally {
            closeSession(session);
            slowLog(curTime, String.format("SQL M %s", sql));
        }
        return null;
    }

    public int addNumber(String table, List<String> addColumn, List<Integer> addValue, List<String> whereValues, String moreDetails) {
        EntityManager session = null;
        try {
            session = emFactory.createEntityManager();
            session.getTransaction().begin();
            String whereDetails = "";
            for (int i = 0; i < whereValues.size(); i += 2) {
                if (i == 0) {
                    whereDetails += String.format("%s = :%s", whereValues.get(i), whereValues.get(i));
                } else {
                    whereDetails += String.format(" and %s = :%s", whereValues.get(i), whereValues.get(i));
                }
            }
            String addDetails = "";
            for (int i = 0; i < addColumn.size(); i++) {
                if (i == 0) {
                    addDetails += String.format("%s=%s+:%s", addColumn.get(i), addColumn.get(i), addColumn.get(i));
                } else {
                    addDetails += String.format(", %s=%s+:%s", addColumn.get(i), addColumn.get(i), addColumn.get(i));
                }
            }
            Query query = session.createNativeQuery(String.format("update %s set %s where %s %s", table, addDetails, whereDetails, moreDetails));
            for (int i = 0; i < addColumn.size(); i++) {
                query.setParameter(addColumn.get(i), addValue.get(i));
            }
            for (int i = 0; i < whereValues.size(); i += 2) {
                query.setParameter(whereValues.get(i), whereValues.get(i + 1));
            }
            int retValue = query.executeUpdate();
            session.getTransaction().commit();
            return retValue;
        } catch (Exception he) {
            he.printStackTrace();
            Logs.error(GUtil.exToString(he));
        } finally {
            closeSession(session);
        }
        return -1;
    }

    public int count(String table, Object... whereValues) {
        long curTime = System.currentTimeMillis();
        String sql = "";
        EntityManager session = null;
        try {
            session = emFactory.createEntityManager();
            String whereDetails = "";
            for (int i = 0; i < whereValues.length; i += 2) {
                if (i == 0) {
                    whereDetails += String.format("where %s = :%s", whereValues[i], whereValues[i]);
                } else {
                    whereDetails += String.format(" and %s = :%s", whereValues[i], whereValues[i]);
                }
            }
            sql = String.format("select count(*) from %s %s ", table, whereDetails);
            Query query = session.createNativeQuery(sql);
            for (int i = 0; i < whereValues.length; i += 2) {
                query.setParameter(whereValues[i].toString(), whereValues[i + 1].toString());
            }
            return Integer.parseInt(query.getSingleResult().toString());
        } catch (Exception he) {
            he.printStackTrace();
            Logs.error(GUtil.exToString(he));
        } finally {
            closeSession(session);
            slowLog(curTime, String.format("SQL N %s", sql));
        }
        return -1;
    }

    public boolean update(String sql) {
        long curTime = System.currentTimeMillis();
        EntityManager session = null;
        try {
            session = emFactory.createEntityManager();
            session.getTransaction().begin();
            session.createNativeQuery(sql).executeUpdate();
            session.getTransaction().commit();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
            slowLog(curTime, String.format("SQL O %s", sql));
        }
        return false;
    }

    public boolean update(Object obj) {
        long curTime = System.currentTimeMillis();
        EntityManager session = null;
        try {
            session = emFactory.createEntityManager();
            session.getTransaction().begin();
            session.unwrap(Session.class).update(obj);
            session.getTransaction().commit();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
            slowLog(curTime, String.format("SQL P %s", obj.getClass().getSimpleName()));
        }
        return false;
    }

    public boolean save(Object obj) {
        long curTime = System.currentTimeMillis();
        EntityManager session = null;
        try {
            session = emFactory.createEntityManager();
            session.getTransaction().begin();
            session.persist(obj);
            session.getTransaction().commit();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
            slowLog(curTime, String.format("SQL Q %s", obj.getClass().getSimpleName()));
        }
        return false;
    }

    public boolean saveOrUpdate(Object obj) {
        long curTime = System.currentTimeMillis();
        EntityManager session = null;
        try {
            session = emFactory.createEntityManager();
            session.getTransaction().begin();
            session.unwrap(Session.class).saveOrUpdate(obj);
            session.getTransaction().commit();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
            slowLog(curTime, String.format("SQL R %s", obj.getClass().getSimpleName()));
        }
        return false;
    }

    public boolean rawSQL(List<String> sqls) {
        long curTime = System.currentTimeMillis();
        EntityManager session = null;
        try {
            session = emFactory.createEntityManager();
            session.getTransaction().begin();
            for (String sql : sqls)
                session.createNativeQuery(sql).executeUpdate();
            session.getTransaction().commit();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
            slowLog(curTime, String.format("SQL S %s", sqls.toString()));
        }
        return false;
    }

    public boolean rawSQL(String... sqls) {
        long curTime = System.currentTimeMillis();
        EntityManager session = null;
        try {
            session = emFactory.createEntityManager();
            session.getTransaction().begin();
            for (String sql : sqls)
                session.createNativeQuery(sql).executeUpdate();
            session.getTransaction().commit();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
            slowLog(curTime, String.format("SQL T %s", new Gson().toJson(sqls)));
        }
        return false;
    }

    public boolean rawSQL(String sql) {
        long curTime = System.currentTimeMillis();
        EntityManager session = null;
        try {
            session = emFactory.createEntityManager();
            session.getTransaction().begin();
            session.createNativeQuery(sql).executeUpdate();
            session.getTransaction().commit();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
            slowLog(curTime, String.format("SQL U %s", sql));
        }
        return false;
    }

    public void closeSession(EntityManager session) {
        try {
            if (session != null) {
                try {
                    if (session.getTransaction().isActive()) session.getTransaction().rollback();
                } catch (Exception ex) {
                    Logs.error(ex);
                }
                session.close();
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    Logger getLogger() {
        return slib_Logger.root();
    }

    void slowLog(long curTime, String msg) {
        long timePass = System.currentTimeMillis() - curTime;
        if (timePass >= CfgServer.getSlowSQLTime())
            Logs.slow(String.format("%s -> %s", msg, timePass));
    }
}
