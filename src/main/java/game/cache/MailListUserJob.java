package game.cache;

import game.config.CfgServer;
import game.config.lang.Lang;
import game.treasure.server.App;
import game.treasure.server.AppInit;
import ozudo.base.database.DBJPA2;
import ozudo.base.database.DBResource;
import ozudo.base.helper.*;
import ozudo.base.log.Logs;

import javax.persistence.EntityManager;
import java.util.List;

public class MailListUserJob {

    public static void main(String args[]) throws Exception {
        new MailListUserJob().process();
        System.exit(0);
    }

    private void process() {
        try {
            AppInit.initAll();
            App.initConfig();
            processMail();
        } catch (Exception ex) {
            String exception = GUtil.exToString(ex);
            Logs.error(exception);
//            Telegram.sendNotify("LuckyNormalJob -> err=" + exception);
        }
    }

    void processMail(){
        List<Integer> lstSend = List.of(
                245010,245043,245033,245005,245011,245003,245004,245016,
                245058,245020,245007,245059,245009,245006,245001,245000
        );
        List<Integer> bonus = List.of( 6,78,1,6,115,2,6,1,50);
        for (int i = 0; i < lstSend.size(); i++) {
            String sql = DBHelper.sqlMail(lstSend.get(i), Lang.getTitle(CfgServer.config.mainLanguage, Lang.mail_reset_hero_bonus), StringHelper.toDBString(bonus));
            DBResource.getInstance().rawSQL(sql);
        }
        System.out.println("-------------- DONE --------------");
    }

    protected EntityManager getEntityManager() {
        return DBJPA2.getEntityManager();
    }

    protected void closeSession(EntityManager session) {
        DBJPA2.closeSession(session);
    }

}
