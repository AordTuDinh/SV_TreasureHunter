package game.cache;

import game.config.CfgServer;
import game.config.lang.Lang;
import game.dragonhero.server.App;
import game.dragonhero.server.AppInit;
import ozudo.base.database.DBJPA2;
import ozudo.base.database.DBResource;
import ozudo.base.helper.DBHelper;
import ozudo.base.helper.GUtil;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.StringHelper;
import ozudo.base.log.Logs;

import javax.persistence.EntityManager;
import java.util.*;

public class SendBonusTopJob {

    public static void main(String args[]) throws Exception {
        new SendBonusTopJob().process();
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
        List<Integer> lstUserId = List.of(245016, 245023, 245034, 245003, 245065, 245014, 245196, 245195, 245019, 245017, 245181, 245115, 245030, 245024, 245069, 245000, 245045, 245072, 245233, 245036, 245163, 245001, 245027, 245210, 245230, 245070, 245245, 245020, 245222, 245176, 245103, 245080, 245006, 245151, 245121, 245086, 245073, 245049, 245047, 245147, 245108, 245042);
        String bonusConfig ="[[1,1,2,9999,10,2,16,5,7,6,30,99,6,35,199],[2,2,2,8888,11,4,5,6,30,88,6,35,188],[3,3,2,7777,6,106,99,6,30,77,6,35,177],[4,4,2,6666,6,106,88,6,30,66,6,35,166],[5,5,2,5555,6,106,77,6,30,55,6,35,155],[6,10,2,4444,6,106,66,6,30,44,6,35,144],[11,20,2,1000,6,106,55,6,30,33,6,35,133],[21,50,2,999,6,106,44,6,30,22,6,35,122],[51,100,2,888,6,106,33,6,30,11,6,35,111],[101,200,2,777,6,106,22,6,30,11,6,35,99],[201,500,2,666,6,106,11,6,30,11,6,35,88],[501,1000,2,555,6,106,11,6,30,11,6,35,77],[1001,3000,2,444,6,106,11,6,30,11,6,35,66],[3001,10000,2,333,6,106,11,6,30,11,6,35,55]]";
        String title = Lang.getTitle(CfgServer.config.mainLanguage,Lang.mail_event_top_level);
        List<List<Integer>> bncf = GsonUtil.strTo2ListInt(bonusConfig);
        Map<Integer,List<Integer>> mapBonus = new HashMap<>();
        for (int i = 0; i < bncf.size(); i++) {
            int from = bncf.get(i).get(0);
            int to = bncf.get(i).get(1);
            if (from == to) mapBonus.put(to,  new ArrayList<> (bncf.get(i).subList(2,bncf.get(i).size())));
            else {
                for (int j = from; j <= to; j++) {
                    mapBonus.put(j, new ArrayList<> (bncf.get(i).subList(2,bncf.get(i).size())));
                }
            }
        }
        for (int i = 0; i < lstUserId.size(); i++) {
            int top = i+1;
            String sql = DBHelper.sqlMail(lstUserId.get(i),String.format(title,top), StringHelper.toDBString(mapBonus.get(top)));
            DBResource.getInstance().rawSQL(sql);
        }

    }

}
