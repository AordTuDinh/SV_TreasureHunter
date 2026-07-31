package game.cache;

import game.config.CfgLottery;
import game.config.CfgServer;
import game.config.aEnum.ItemPointKey;
import game.config.aEnum.StatusType;
import game.treasure.mapping.UserItemPointEntity;
import game.treasure.server.App;
import game.treasure.server.AppInit;
import game.treasure.service.user.Bonus;
import ozudo.base.database.DBJPA2;
import ozudo.base.database.DBResource;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.GUtil;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.StringHelper;
import ozudo.base.log.Logs;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LuckySpecialJob {
    public static final String PATH = "/root/DB/LUCKY_SPECIAL/";

    public static void main(String args[]) throws Exception {
        new LuckySpecialJob().process();
        System.exit(0);
    }

    private void process() throws Exception {
        try {
            AppInit.initAll();
            App.initConfig();
            processConverDB();
        } catch (Exception ex) {
            String exception = GUtil.exToString(ex);
            Logs.error(exception);
        }
    }


    private void processConverDB() {
        int event = DateTime.getNumberDay();
        List<UserItemPointEntity> uLot = DBResource.getInstance().getList(
                CfgServer.DB_DSON + "user_item_point",
                Arrays.asList("point_id", ItemPointKey.TICKER_SPECIAL.id),
                "",
                UserItemPointEntity.class);
        if (uLot == null || uLot.size() <= 0) return;
        long count = 0;
        long sumNumber = 0;
        for (int i = 0; i < uLot.size(); i++) {
            List<Integer> special = GsonUtil.strToListInt(uLot.get(i).getData());
            if (special.size() <= 1) continue;
            count += special.size() - 1;
            for (int j = 1; j < special.size(); j++) {
                sumNumber += special.get(j);
            }
        }
        if (count <= 0) return;
        int luckyNum = (int) ((sumNumber / count) * 0.8);
        long cacheGem = DBResource.getInstance().getNumber("select gem from " + CfgServer.DB_MAIN + "lottery_special");
        long gem = (int) (count * CfgLottery.config.feeSpecial) + cacheGem;
        int winnerTicker = 0;
        int[] countWinByRow = new int[uLot.size()];
        String sql = "";
        for (int i = 0; i < uLot.size(); i++) {
            int countTick = 0;
            List<Integer> ticker = GsonUtil.strToListInt(uLot.get(i).getData());
            if (ticker.isEmpty()) continue;
            int eventTicker = ticker.get(0);
            if (eventTicker != DateTime.getNumberDay()) continue;
            for (int j = 1; j < ticker.size(); j++) {
                if (ticker.get(j) == luckyNum) {
                    countTick++;
                    winnerTicker++;
                }
            }
            countWinByRow[i] = countTick;
        }
        long gemInTickerWin = 0;
        if (winnerTicker > 0) {
            gemInTickerWin = gem / winnerTicker;
        } else {
            DBResource.getInstance().rawSQL("update " + CfgServer.DB_MAIN + "lottery_special set gem= " + gem);
        }
        int eventType = ItemPointKey.TICKER_SPECIAL.id;
        for (int i = 0; i < uLot.size(); i++) {
            long curGemWin = (long) countWinByRow[i] * gemInTickerWin;
            List<Long> bonus = new ArrayList<>();
            int status = StatusType.LOCK.value;
            if (curGemWin > 0) {
                status = StatusType.RECEIVE.value;
                bonus = Bonus.viewGem((int) curGemWin);
            }
            sql += "(" + uLot.get(i).getUserId() + "," + event + "," + eventType + "," + StringHelper.toDBString(uLot.get(i).getData()) + ",'"
                    + DateTime.getFullDate() + "'," + luckyNum + "," + status + ",'" + StringHelper.toDBString(bonus) + "'),";
        }
        sql = String.format("INSERT INTO user_lottery_history(user_id,event_id,type,number,time,lucky,status,bonus) VALUES %s", sql.substring(0, sql.length() - 1));
        DBResource.getInstance().rawSQL(sql);
        System.out.println("Quay vé số đặc biệt xong, số may mắn của kì quay " + event + " là: " + luckyNum);
    }

    protected EntityManager getEntityManager() {
        return DBJPA2.getEntityManager();
    }

    protected void closeSession(EntityManager session) {
        DBJPA2.closeSession(session);
    }
}
