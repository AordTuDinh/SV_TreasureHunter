package game.config;

import com.google.gson.Gson;
import game.dragonhero.service.user.Bonus;
import ozudo.base.helper.NumberUtil;

import java.util.ArrayList;
import java.util.List;

public class CfgLuckySpine {
    public static DataConfig config;
    public static List<Integer> rateRotate;
    public static final int indexSelectOne = 6;

    public static void loadConfig(String value) {
        config = new Gson().fromJson(value, DataConfig.class);
        rateRotate = new ArrayList<>();
        int rate = 0;
        for (int i = 0; i < config.casinoRate.size(); i++) {
            rate += config.casinoRate.get(i);
            rateRotate.add(rate);
        }
    }

    public static int getRandomIndex() {
        int rand = NumberUtil.getRandom(1000);
        for (int i = 0; i < rateRotate.size(); i++) {
            if (rand < rateRotate.get(i)) return i;
        }
        return 0;
    }

    public static List<List<Long>> getSpineBonusShow(int level) {
        List<List<Long>> arrBonus = new ArrayList<>();
        int currLevel = level;
        SpineBonusObject bonusObject = null;
        if (currLevel > config.bonusNormal.get(config.bonusNormal.size() - 1).levelMax) {
            currLevel = config.bonusNormal.get(config.bonusNormal.size() - 1).levelMax;
        }

        for (int i = 0; i < config.bonusNormal.size(); i++) {
            if (currLevel <= config.bonusNormal.get(i).levelMax) {
                bonusObject = config.bonusNormal.get(i);
                break;
            }
        }

        if (bonusObject != null) {
            int indexBonus = NumberUtil.getRandom(3);
            //set bonus
            arrBonus.addAll(Bonus.parse(bonusObject.bonus.get(indexBonus)));
        }
        return arrBonus;
    }

    public class DataConfig {
        public int timeFreeRefresh;
        public List<Integer> casinoRate;
        public int feeRefresh;
        public int[] feeRotate;
        public List<SpineBonusObject> bonusNormal;
        public int priceChip;
    }

    public static class SpineBonusObject {
        public int levelMax;
        public List<Integer> chance;
        public List<List<Long>> bonus;
    }
}
