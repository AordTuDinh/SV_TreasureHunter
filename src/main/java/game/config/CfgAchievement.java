package game.config;

import com.google.gson.Gson;
import game.treasure.mapping.UserAchievementEntity;
import game.treasure.service.Services;
import game.object.BonusConfig;
import game.object.MyUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CfgAchievement {
    public static DataConfig config;
    public static final float TIME_UPDATE = 60;
    static List<Integer> types = Arrays.asList(1, 2, 3, 4, 5);
    public static List<Integer> checkinAchi;
    public static List<Integer> addGold;
    public static List<Integer> addGem;
    public static List<Integer> addRuby;

    public static void loadConfig(String strJson) {
        config = new Gson().fromJson(strJson, DataConfig.class);
        checkinAchi = new ArrayList<>();
        for (int i = 1; i <= 19; i++) {
            checkinAchi.add(i);
        }
        addGold = new ArrayList<>();
        for (int i = 1; i <= 13; i++) {
            addGold.add(i);
        }
        addGem = new ArrayList<>();
        for (int i = 14; i <= 30; i++) {
            addGem.add(i);
        }
        addRuby = new ArrayList<>();
        for (int i = 31; i <= 47; i++) {
            addRuby.add(i);
        }
    }

    /** Tab slider treasure (type 1–5): random bonus theo config.bonus. */
    public static List<Long> getBonusByType(int type, int num) {
        List<Long> bonus = new ArrayList<>();
        List<BonusConfig> bms = CfgAchievement.config.bonus;
        for (int i = 0; i < num; i++) {
            bonus.addAll(BonusConfig.getRandomOneBonus(bms));
        }
        return bonus;
    }

    public static boolean checkMilestoneIndex(int index) {
        return config != null
                && config.bonusMilestone != null
                && index >= 0
                && index < config.bonusMilestone.size();
    }

    public static int getMilestonePoint(int index) {
        return config.pointMilestone.get(index);
    }

    public static List<Long> getMilestoneBonus(int index) {
        return config.bonusMilestone.get(index);
    }

    public static int getMilestoneCount() {
        if (config == null || config.bonusMilestone == null) return 0;
        return config.bonusMilestone.size();
    }

    public static boolean checkType(int type) {
        return types.contains(type);
    }

    public static void addAchievement(MyUser mUser, int type, int id, int num) {
        UserAchievementEntity uAchi = Services.userDAO.getUserAchievement(mUser);
        if (uAchi == null) return;
        uAchi.addAchievement(type, id, num);
    }

    public static void addListAchievement(MyUser mUser, int type, List<Integer> ids, int num) {
        UserAchievementEntity uAchi = Services.userDAO.getUserAchievement(mUser);
        if (uAchi == null) return;
        uAchi.addListAchievement(type, ids, num);
    }

    public static class DataConfig {
        public int maxPoint;
        public int maxAllPoint;
        public List<BonusConfig> bonus;
        /** @deprecated Thanh tổng dùng bonusMilestone cố định; giữ field để tương thích JSON cũ. */
        public List<BonusConfig> bonusAll;
        public List<Integer> pointMilestone;
        public List<List<Long>> bonusMilestone;
    }
}
