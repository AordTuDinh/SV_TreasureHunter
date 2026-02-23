package game.config;

import com.google.gson.Gson;
import game.dragonhero.mapping.UserAfkEntity;
import game.dragonhero.service.Services;
import game.object.MyUser;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.NumberUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CfgAfk {
    public static DataConfig config;
    public static List<Integer> rateAfk;
    public static final float perExpAFK = 0.05f / 14400f;

    public static void loadConfig(String strJson) {
        config = new Gson().fromJson(strJson, DataConfig.class);
        rateAfk = new ArrayList<>();
        rateAfk.add(config.bonus5s.get(0).rate);
        for (int i = 1; i < config.bonus5s.size(); i++) {
            rateAfk.add(rateAfk.get(i - 1) + config.bonus5s.get(i).rate);
        }
    }

    public static boolean isFullAfkBonus(MyUser mUser) {
        UserAfkEntity uAfk = Services.userDAO.getUserAfk(mUser);
        if (uAfk == null) return false;
        long secondsMax = uAfk.getTimeFullBonus() * DateTime.HOUR_MILLI_SECOND;
        return Calendar.getInstance().getTimeInMillis() - uAfk.getTimeGetBonus() >= secondsMax;
    }

    public static List<Long> getBonusAfk() {
        int rand = NumberUtil.getRandom(1000);
        for (int i = 0; i < rateAfk.size(); i++) {
            if (rand < rateAfk.get(i)) {
                return new ArrayList<>(config.bonus5s.get(i).bonus);
            }
        }
        return new ArrayList<>();
    }

    public class DataConfig {
        public int timeMax;
        public float ratePiece;
        public List<BonusData> bonus5s;
        public int secondUpdate;
    }

    public class BonusData {
        int rate;
        List<Long> bonus;
    }
}
