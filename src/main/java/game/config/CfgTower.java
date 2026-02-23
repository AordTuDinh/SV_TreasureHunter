package game.config;

import com.google.gson.Gson;
import game.battle.object.Pos;
import game.dragonhero.mapping.UserTowerEntity;
import ozudo.base.helper.DateTime;

import java.util.List;

public class CfgTower {
    public static DataConfig config;

    public static void loadConfig(String strJson) {
        config = new Gson().fromJson(strJson, DataConfig.class);
    }

    public static long getCountdown(UserTowerEntity uTower) {
        if (uTower.getNumberKey() >= CfgTower.config.maxKey || uTower.getCountdown() == -1) return 0;
        return (getTimeMinuteReceiveKey() - (System.currentTimeMillis() - uTower.getCountdown())) / 1000;
    }

    public static long getTimeMinuteReceiveKey() {
        return DateTime.MIN_MILLI_SECOND * CfgTower.config.timeReceiveKey;
    }


    public static long getMaxBuy(UserTowerEntity uTower) {
        long buy = config.maxBuyKey - uTower.getNumberBuy();
        return buy < 0 ? 0 : buy;
    }

    public static Pos getPosPlayer() {
        return new Pos(0, -2.5f);
    }

    public class DataConfig {
        public boolean open;
        public int timeReceiveKey;
        public int maxKey;
        public int maxBuyKey;
        public List<Long> feeBuyKey;
    }
}
