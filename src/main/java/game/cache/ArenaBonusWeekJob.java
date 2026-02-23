package game.cache;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import game.battle.object.ServerObject;
import game.config.CfgArena;
import game.config.CfgServer;
import game.config.lang.Lang;
import game.dragonhero.mapping.UserArenaEntity;
import game.dragonhero.mapping.main.ConfigEntity;
import game.dragonhero.server.App;
import game.dragonhero.server.AppInit;
import game.monitor.Telegram;
import org.apache.commons.lang.StringEscapeUtils;
import ozudo.base.database.DBResource;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.GUtil;
import ozudo.base.log.Logs;

import java.util.Arrays;
import java.util.List;

public class ArenaBonusWeekJob {
    public static void main(String args[]) throws Exception {
        new ArenaBonusWeekJob().process();
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
        }
    }

    public void processData() {
        List<Integer> servers = ArenaBonusDayJob.getListServerId();
        for (int i = 0; i < servers.size(); i++) {
            List<UserArenaEntity> uArena = DBResource.getInstance().getList(CfgServer.DB_DSON + "user_arena", Arrays.asList("server", servers.get(i), "active_arena", 1), " order BY arena_point desc LIMIT 10000", UserArenaEntity.class);
            String sql = "";
            for (int j = 0; j < uArena.size(); j++) {
                UserArenaEntity arena = uArena.get(j);
                sql += "(" + arena.getUserId() + ",'" + StringEscapeUtils.escapeSql(Lang.getTitle(CfgServer.config.mainLanguage,Lang.mail_arena_week)) + "','" + StringEscapeUtils.escapeSql(String.format(Lang.getTitle(CfgServer.config.mainLanguage, Lang.mail_arena_top), (j + 1))) + "','" + CfgArena.getBonusWeekByRank(j + 1) + "','" + DateTime.getDateyyyyMMdd() + "'),";
            }
            if (sql.isEmpty()) continue;
            String query = String.format("insert into user_mail(user_id, title, message, bonus, mail_idx) VALUES %s", sql.substring(0, sql.length() - 1));
            DBResource.getInstance().rawSQL(query);

        }
        System.out.println("------------------DONE------------------");
    }
}
