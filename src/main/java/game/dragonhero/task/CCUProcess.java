package game.dragonhero.task;

import game.cache.JCache;
import game.config.CfgServer;
import org.quartz.Job;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.GsonUtil;

import java.util.Calendar;
import java.util.List;

public class CCUProcess extends JobCounter implements Job {


    @Override
    protected void executeJob() {
        String svIds = JCache.getInstance().getValue(CfgServer.SVID);
        if (svIds != null) {
            List<Integer> svs = GsonUtil.strToListInt(svIds);
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            int min = Calendar.getInstance().get(Calendar.MINUTE);
            int saveMin = min / 5 * 5;
            String hourSave = hour + ":" + saveMin;
            String values = "";
            for (int i = 0; i < svs.size(); i++) {
                if(svs.get(i)<0) continue;
                String subSvNum = JCache.getInstance().getValue("ccu_" + svs.get(i));
                if (subSvNum != null) {
                    List<Integer> ccu = GsonUtil.strToListInt(subSvNum);
                    for (int j = 0; j < ccu.size(); j += 2) {
                        values += "(" + ccu.get(j) + ",'" + DateTime.getDateyyyyMMddCross(Calendar.getInstance().getTime()) +
                                "','" + hourSave + "'," + ccu.get(j + 1) + "," + Calendar.getInstance().getTimeInMillis() / 1000 + "),";
                    }
                }
                
                if (values.length() > 2) {
                    values = values.substring(0, values.length() - 1);
                    DBJPA.rawSQL("insert into cms.ccu(server_id, date_created,hours,online,time) VALUES" + values + "ON DUPLICATE KEY UPDATE online=values(online)");
                }
            }
        }
    }
}
