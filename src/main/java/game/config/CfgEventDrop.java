package game.config;

import com.google.gson.Gson;
import game.treasure.service.user.Bonus;
import lombok.Data;
import ozudo.base.helper.NumberUtil;

import java.util.ArrayList;
import java.util.List;

public class CfgEventDrop {

    public static DataConfig config;

    public static boolean inEvent() {
        return config.active;
    }

    public static List<Long> bonusDrop(int per, int num) {
        if (!inEvent()) return new ArrayList<>();
        List<Long> bonus = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            int perx = NumberUtil.getRandom(1000);
            if (perx < per) bonus.addAll(Bonus.viewItem(config.itemId, 1));
        }
        return bonus;
    }


    public static void loadConfig(String strJson) {
        config = new Gson().fromJson(strJson, DataConfig.class);
    }

    @Data
    public class DataConfig {
        boolean active;
        int eventId;
        int itemId; // const item id
        int rateDropCampaign;
        int rateDropTree;
        int rateDropBossGod;
        int rateDropTower;
    }
}
