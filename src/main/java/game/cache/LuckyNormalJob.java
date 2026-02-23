package game.cache;

import game.config.CfgLottery;
import game.config.CfgServer;
import game.config.aEnum.ItemKey;
import game.config.aEnum.ItemType;
import game.config.aEnum.StatusType;
import game.dragonhero.mapping.UserItemEntity;
import game.dragonhero.server.App;
import game.dragonhero.server.AppInit;
import game.dragonhero.service.user.Bonus;
import game.monitor.Telegram;
import ozudo.base.database.DBJPA2;
import ozudo.base.database.DBResource;
import ozudo.base.helper.*;
import ozudo.base.log.Logs;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LuckyNormalJob {
    public static final String PATH = "/root/DB/LUCKY_NORMAL/";

    public static void main(String args[]) throws Exception {
        new LuckyNormalJob().process();
        System.exit(0);
    }

    private void process() {
        try {
            AppInit.initAll();
            App.initConfig();
            processConvertDB();
        } catch (Exception ex) {
            String exception = GUtil.exToString(ex);
            Logs.error(exception);
        }
    }

    private void processConvertDB() {
        int luckyNum = NumberUtil.getRandom(100000, 999999);
        int event = DateTime.getNumberDay() + 1;
        List<UserItemEntity> uLot = DBResource.getInstance().getList(CfgServer.DB_DSON + "user_item", Arrays.asList("item_id", ItemKey.TICKER_NORMAL.id), "", UserItemEntity.class);
        if (uLot == null || uLot.size() <= 0) return;
        List<Integer> luckyId = new ArrayList<>();
        String sql = "";
        for (int i = 0; i < uLot.size(); i++) {
            List<Integer> ticker = GsonUtil.strToListInt(uLot.get(i).getData());
            if (ticker == null || ticker.isEmpty()) continue;
            int eventBuy = ticker.get(0);
            if (eventBuy != event - 1) continue;
            ticker = ticker.subList(1, ticker.size());
            int status = StatusType.LOCK.value;
            List<Integer> result = new ArrayList<>();
            int gem = 0;
            for (int j = 0; j < ticker.size(); j++) {
                int check = checkOne(luckyNum, ticker.get(j));
                result.add(check);
                if (check == 1) luckyId.add(uLot.get(i).getUserId());
                if (check > 0) { // trúng giải
                    status = StatusType.RECEIVE.value;
                    gem += CfgLottery.config.bonusNormal.get(check - 1);
                }
            }
            List<Long> bonus = gem > 0 ? Bonus.viewGem(gem) : new ArrayList<>();
            sql += "(" + uLot.get(i).getUserId() + "," + event + "," + uLot.get(i).getType().value + ",'" + StringHelper.toDBString(ticker) + "','" + DateTime.getFullDate() + "'," +
                    luckyNum + "," + status + ",'" + StringHelper.toDBString(bonus) + "','" + StringHelper.toDBString(result) + "'),";
        }
        System.out.println("Quay vé số thường xong, số may mắn của kì quay \" + event + \" là: \" + luckyNum + \". Người trúng giải đặc biệt: \" + luckyId");
        if (sql.isEmpty()) return;
        String query = String.format("INSERT INTO user_lottery_history(user_id,event_id,type,number,time,lucky,status,bonus,result) VALUES %s", sql.substring(0, sql.length() - 1));
        DBResource.getInstance().rawSQL(query);
    }

    private int checkOne(int luckyNum, int myNum) {
        int num = 0;
        String myL = myNum + "";
        myL = myL.length() < 6 ? NumberUtil.genListStringInt(6 - myL.length(), 0) + myL : myL;
        String lucky = luckyNum + "";
        lucky = lucky.length() < 6 ? NumberUtil.genListStringInt(6 - lucky.length(), 0) + lucky : lucky;
        for (int i = 6; i > 0; i--) {
            if (lucky.charAt(i - 1) == myL.charAt(i - 1)) {
                num = i;
            } else break;
        }
        return num;
    }

    protected EntityManager getEntityManager() {
        return DBJPA2.getEntityManager();
    }

    protected void closeSession(EntityManager session) {
        DBJPA2.closeSession(session);
    }

}
