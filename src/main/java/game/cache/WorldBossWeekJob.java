package game.cache;

import game.config.CfgArena;
import game.config.CfgServer;
import game.config.CfgWorldBoss;
import game.config.lang.Lang;
import game.dragonhero.mapping.UserArenaEntity;
import game.dragonhero.mapping.UserWeekEntity;
import game.dragonhero.server.App;
import game.dragonhero.server.AppInit;
import game.dragonhero.service.Services;
import game.monitor.Telegram;
import org.apache.commons.lang.StringEscapeUtils;
import ozudo.base.database.DBResource;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.GUtil;
import ozudo.base.log.Logs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldBossWeekJob {
    public static void main(String args[]) throws Exception {
        new WorldBossWeekJob().process();
        System.exit(0);
    }

    private void process() {
        try {
            AppInit.initAll();
            App.initConfig();
            processData();
        } catch (Exception ex) {
            String exception = GUtil.exToString(ex);
            Logs.error(exception);
//            Telegram.sendNotify("Send bonus arena week -> err=" + exception);
        }
    }

    public void processData() {
        List<Integer> servers = ArenaBonusDayJob.getListServerId();
        int weekId=  DateTime.getNumberWeek();
        for (int i = 0; i < servers.size(); i++) {
            List<UserWeekEntity> uWeek = DBResource.getInstance().getList(CfgServer.DB_DSON + "user_week", Arrays.asList("server", servers.get(i),"week_id",weekId), " and kill_boss > 0 order BY kill_boss desc LIMIT 10000", UserWeekEntity.class);
            String sql = "";
            for (int j = 0; j < uWeek.size(); j++) {
                UserWeekEntity aWeek = uWeek.get(j);
                sql += "(" + aWeek.getUserId() + ",'" + StringEscapeUtils.escapeSql(Lang.getTitle(CfgServer.config.mainLanguage, Lang.mail_world_boss_week)) + "','" + StringEscapeUtils.escapeSql(String.format(Lang.getTitle(CfgServer.config.mainLanguage,Lang.mail_world_boss_top), (j + 1))) + "','" + CfgWorldBoss.getBonusWeekByRank(j + 1) + "','" + DateTime.getDateyyyyMMdd() + "'),";
            }
            if (sql.isEmpty()) continue;
            String query = String.format("insert into user_mail(user_id, title, message, bonus, mail_idx) VALUES %s", sql.substring(0, sql.length() - 1));
            DBResource.getInstance().rawSQL(query);
        }
        System.out.println("------------------DONE------------------");
    }
}
