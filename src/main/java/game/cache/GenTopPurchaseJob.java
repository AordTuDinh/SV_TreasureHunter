package game.cache;

import game.dragonhero.server.App;
import game.dragonhero.server.AppInit;
import ozudo.base.database.DBJPA2;
import ozudo.base.database.DBResource;
import ozudo.base.helper.GUtil;
import ozudo.base.log.Logs;

import javax.persistence.EntityManager;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class GenTopPurchaseJob {

    public static void main(String args[]) throws Exception {
        new GenTopPurchaseJob().process();
        System.exit(0);
    }

    private void process() {
        try {
            AppInit.initAll();
            App.initConfig();
            processGiftCode();
        } catch (Exception ex) {
            String exception = GUtil.exToString(ex);
            Logs.error(exception);
        }
    }

    void processGiftCode() {
        String sql = "INSERT INTO dson.user_top_purchase (user_id, total_purchases, server_id) SELECT user_id, SUM(price) AS total_purchases, 1 AS server_id FROM cms.log_buy_iap WHERE server_id = 1 GROUP BY user_id ON DUPLICATE KEY UPDATE total_purchases = VALUES(total_purchases), date_update = CURRENT_TIMESTAMP";
        DBResource.getInstance().rawSQL(sql);
        System.out.println("-------------- DONE --------------");
    }

    protected EntityManager getEntityManager() {
        return DBJPA2.getEntityManager();
    }

    protected void closeSession(EntityManager session) {
        DBJPA2.closeSession(session);
    }

}
