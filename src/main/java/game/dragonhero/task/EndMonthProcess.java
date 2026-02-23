package game.dragonhero.task;

import game.config.CfgServer;
import org.quartz.Job;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.DateTime;
import ozudo.base.log.Logs;

import java.util.Calendar;
import java.util.List;

public class EndMonthProcess extends JobCounter implements Job {
    @Override
    protected void executeJob() {
        try {
            // MAU
            String likeDate = DateTime.getDateyyyyMMCross(Calendar.getInstance().getTime()) + "%";
            List<Object[]> lstMau = DBJPA.getList("SELECT m.os, COUNT(*) AS NUMBER,u.server FROM dson.user u INNER JOIN  " + CfgServer.DB_MAIN + "main_user m ON m.id = u.main_id WHERE u.last_login LIKE '" +
                    likeDate + "' GROUP BY os,server");
            String valueUpdate = "";
            for (int i = 0; i < lstMau.size(); i++) {
                valueUpdate += "(NOW(),'" + lstMau.get(i)[0] + "'," + lstMau.get(i)[1] + "," + lstMau.get(i)[2] + "),";
            }
            if (valueUpdate.length() > 2) {
                valueUpdate = valueUpdate.substring(0, valueUpdate.length() - 1);
                DBJPA.rawSQL("INSERT INTO cms.mau (date_created, os,server_id,number) VALUES " + valueUpdate + " ON DUPLICATE KEY UPDATE number=Values(number)");
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    public static void main(String[] args) {
        String likeDate = DateTime.getDateyyyyMMCross(Calendar.getInstance().getTime());
        System.out.println("likeDate = " + likeDate);
    }
}
