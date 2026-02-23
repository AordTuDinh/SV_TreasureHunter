package game.dragonhero.task;

import game.config.CfgServer;
import game.dragonhero.mapping.cms.*;
import game.dragonhero.server.App;
import game.dragonhero.server.AppInit;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import ozudo.base.database.DBJPA;
import ozudo.base.database.DBResource;
import ozudo.base.helper.DateTime;
import ozudo.base.log.Logs;

import java.lang.reflect.Field;
import java.util.*;

@DisallowConcurrentExecution
public class EndDayProcess extends JobCounter implements Job {

    public static void main(String[] args) throws Exception {
        AppInit.initAll();
        App.initConfig();
        EndDayProcess e = new EndDayProcess();
        e.executeJob();
    }

    @Override
    protected void executeJob() {
        String likeDate = DateTime.getDateyyyyMMddCross(Calendar.getInstance().getTime()) + "%";
        List<String> queries = new ArrayList<>();
        try {
            // Cache max ccu in day
            List<CcuMaxEntity> listCCU = DBResource.getInstance().getSelectQuery("SELECT server_id,MAX(ONLINE) as number,date_created,hours FROM cms.ccu c WHERE c.date_created LIKE '" + likeDate + "' AND (c.server_id, c.online) IN (SELECT server_id, MAX(online) and server_id > 0 FROM cms.ccu WHERE date_created LIKE '" + likeDate + "' GROUP BY server_id)"
                    , CcuMaxEntity.class);
            if (listCCU != null) {
                for (int i = 0; i < listCCU.size(); i++) {
                    queries.add(listCCU.get(i).sqlSave());
                }
            }
        } catch (Exception ex) {
            System.out.println("CCU MAX save data fail");
            Logs.error(ex);
        }
        try {
            // Cache money
            List<MoneyEntity> lstUPack = DBResource.getInstance().getSelectQuery("SELECT server_id,SUM(price) as number,date_created FROM cms.log_buy_iap WHERE date_created like '" + likeDate + "' and status=1 and server_id > 0 GROUP BY server_id", MoneyEntity.class);
            if (lstUPack != null) {
                for (int i = 0; i < lstUPack.size(); i++) {
                    queries.add(lstUPack.get(i).sqlSave());
                }
            }
        } catch (Exception ex) {
            System.out.println("CCU MAX save data fail");
            Logs.error(ex);
        }
        try {
            // DAU - UDID : user hoạt động theo ngày
            List<DauEntity> lstDau = DBResource.getInstance().getSelectQuery("SELECT u.server as server_id,m.os, COUNT(*) AS number, m.last_login as date_created  FROM dson.user u INNER JOIN " + CfgServer.DB_MAIN + "main_user m ON m.id = u.main_id WHERE m.last_login LIKE '" + likeDate + "' and u.server > 0  GROUP BY m.os,u.server", DauEntity.class);
            if (lstDau != null) {
                for (int i = 0; i < lstDau.size(); i++) {
                    queries.add(lstDau.get(i).sqlSave());
                }
            }
        } catch (Exception ex) {
            System.out.println("DAU - UDID save data fail");
            Logs.error(ex);
        }
        try {
            // NRU - UDID : người dùng đăng kí mới
            List<NruEntity> listNru = DBResource.getInstance().getSelectQuery("SELECT u.server as server_id,m.os,COUNT(*) AS number,u.date_created FROM " + "dson.user u INNER JOIN " + CfgServer.DB_MAIN + "main_user m ON u.main_id=m.id  WHERE u.date_created LIKE '" + likeDate + "' and u.server > 0 GROUP BY os,u.server", NruEntity.class);
            if (listNru != null) {
                for (int i = 0; i < listNru.size(); i++) {
                    queries.add(listNru.get(i).sqlSave());
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Logs.error(ex);
        }
        //todo

        try {
            // QUIT 7 : người dùng quit 7 ngày
            List<Quit7Entity> lstQ7 = DBResource.getInstance().getSelectQuery("SELECT u.server as server_id,m.os,COUNT(*) number FROM dson.user u INNER JOIN " + CfgServer.DB_MAIN + "main_user m ON m.id = u.main_id WHERE DATEDIFF(u.date_created,u.last_login)=7 and u.server > 0  GROUP BY SERVER,os", Quit7Entity.class);
            if (lstQ7 != null) {
                for (int i = 0; i < lstQ7.size(); i++) {
                    queries.add(lstQ7.get(i).sqlSave());
                }
            }
        } catch (Exception ex) {
            System.out.println("Quit 7 save data fail");
            Logs.error(ex);
        }

        try {
            // QUIT 30
            List<Quit30Entity> lstQ30 = DBResource.getInstance().getSelectQuery("SELECT u.server as server_id,m.os,COUNT(*) number FROM dson.user u INNER JOIN " + CfgServer.DB_MAIN + "main_user m ON m.id = u.main_id WHERE DATEDIFF(u.date_created,u.last_login)=30 and u.server > 0 GROUP BY SERVER,os", Quit30Entity.class);
            if (lstQ30 != null) {
                for (int i = 0; i < lstQ30.size(); i++) {
                    queries.add(lstQ30.get(i).sqlSave());
                }

            }
        } catch (Exception ex) {
            System.out.println("Quit 30 save data fail");
            Logs.error(ex);
        }
        // RR : retention rate
        try {
            List<NruEntity> listNru = DBResource.getInstance().getSelectQuery("SELECT u.server as server_id,m.os,COUNT(*) AS number,u.date_created FROM " + "dson.user u INNER JOIN " + CfgServer.DB_MAIN + "main_user m ON u.main_id=m.id  WHERE u.date_created LIKE '" + likeDate + "' and u.server > 0 GROUP BY os,u.server", NruEntity.class);
            Map<Integer, RetentionRateEntity> mRR = new HashMap<>();
            if (listNru != null) {
                for (int i = 0; i < listNru.size(); i++) {
                    mRR.put(listNru.get(i).getServerId(), new RetentionRateEntity(listNru.get(i).getServerId()));
                }
            }
            List<Object[]> listRR = DBJPA.getList("SELECT server,COUNT(*) AS number,rr FROM dson.user WHERE date_created LIKE '" + likeDate + "' and rr>0 and server > 0 GROUP BY server,rr");
            if (listRR != null) {
                for (int i = 0; i < listRR.size(); i++) {
                    int server = (int) listRR.get(i)[0];
                    int number = (int) listRR.get(i)[1];
                    int rr = (int) listRR.get(i)[2];
                    RetentionRateEntity retentionRate = new RetentionRateEntity(server);
                    if (mRR.containsKey(server)) {
                        retentionRate = mRR.get(server);
                    }
                    Field field = retentionRate.getClass().getDeclaredField("rr_" + rr);
                    field.setAccessible(true);//to access private fields
                    int per = retentionRate.getNru() == 0 ? 0 : number * 100 / retentionRate.getNru();
                    field.set(rr, per);
                    field.set(rr, number);
                    if (!mRR.containsKey(server)) mRR.put(server, retentionRate);
                }
            }
            if (!mRR.isEmpty()) {
                for (Map.Entry<Integer, RetentionRateEntity> pair : mRR.entrySet()) {
                    queries.add(pair.getValue().sqlSave());
                }
            }
        } catch (Exception ex) {
            System.out.println("Retention rate save data fail");
            Logs.error(ex);
        }
        // Retention rate udid
        try {
            int nru = Math.toIntExact(DBResource.getInstance().getNumber("SELECT CAST(COUNT(*) AS UNSIGNED) AS number FROM " + CfgServer.DB_MAIN + "main_user WHERE created_on LIKE '" + likeDate + "' "));
            RetentionRateUdidEntity rUDID = new RetentionRateUdidEntity(nru);
            List<Object[]> listRR = DBJPA.getList("SELECT rr,COUNT(*) AS number FROM " + CfgServer.DB_MAIN + "main_user WHERE created_on LIKE '" + likeDate + "' AND rr>0  GROUP BY rr");
            if (listRR != null) {
                for (int i = 0; i < listRR.size(); i++) {
                    int rr = (int) listRR.get(i)[0];
                    int number = (int) listRR.get(i)[1];
                    Field field = rUDID.getClass().getDeclaredField("rr_" + rr);
                    field.setAccessible(true);//to access private fields
                    int per = nru == 0 ? 0 : number * 100 / nru;
                    field.set(rr, per);
                }
            }
            queries.add(rUDID.sqlSave());
            queries.add("INSERT into cms.nru_udid(date_created,NUMBER) VALUES(CURDATE()," + nru + ")");
        } catch (Exception ex) {
            System.out.println("Retention rate udid save data fail");
            Logs.error(ex);
        }
      if (DBResource.getInstance().rawSQL(queries)) System.out.println("Save data stats successful");
    }
}