package game.cache;

import game.dragonhero.server.AppConfig;
import game.monitor.Telegram;
import ozudo.base.database.DBResource;
import ozudo.base.helper.GUtil;
import ozudo.base.log.Config;
import ozudo.base.log.Logs;

public class ClearOldItemJob {
    public static void main(String[] args) throws Exception {
        new ClearOldItemJob().process();
        System.exit(0);
    }

    private void process() throws Exception {
        try {
            AppConfig.load("config.json");
            Config.load("config.xml");
            DBResource.getInstance().init(AppConfig.cfg.db.entityResource);
            processClear();
        } catch (Exception ex) {
            String exception = GUtil.exToString(ex);
            Logs.error(exception);
        }
    }

    private void processClear() {
        long time = System.currentTimeMillis() / 1000;
        boolean deleteDone = DBResource.getInstance().rawSQL("DELETE FROM dson.user_item WHERE EXPIRE >0  AND  EXPIRE <" + time + ";");
        if (deleteDone) {
            // send notify telegram
        }
        deleteDone = DBResource.getInstance().rawSQL("DELETE FROM dson.user_item_equipment WHERE EXPIRE >0  AND  EXPIRE <" + time + ";");
        if (deleteDone) {
            // send notify telegram
        }
//        Telegram.sendNotify("--------- Clear old item done ---------");
    }
}