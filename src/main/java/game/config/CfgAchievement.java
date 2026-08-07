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
        if (strJson == null || strJson.isBlank()) {
            config = new DataConfig();
        } else {
            config = new Gson().fromJson(strJson, DataConfig.class);
            if (config == null) {
                config = new DataConfig();
            }
        }
        ensureConfig();
        initStaticLists();
    }

    /** Luôn có config hợp lệ — dùng khi login/notify chạy trước lúc load DB xong. */
    public static DataConfig getConfig() {
        ensureConfig();
        return config;
    }

    public static int getMaxPoint() {
        ensureConfig();
        return config.maxPoint;
    }

    public static int getMaxAllPoint() {
        ensureConfig();
        return config.maxAllPoint;
    }

    static void initStaticLists() {
        if (checkinAchi != null) return;
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
        ensureConfig();
        List<Long> bonus = new ArrayList<>();
        if (config.bonus == null || config.bonus.isEmpty()) return bonus;
        List<BonusConfig> bms = config.bonus;
        for (int i = 0; i < num; i++) {
            bonus.addAll(BonusConfig.getRandomOneBonus(bms));
        }
        return bonus;
    }

    public static boolean checkMilestoneIndex(int index) {
        ensureConfig();
        return config.bonusMilestone != null
                && index >= 0
                && index < config.bonusMilestone.size();
    }

    public static int getMilestonePoint(int index) {
        ensureConfig();
        return config.pointMilestone.get(index);
    }

    public static List<Long> getMilestoneBonus(int index) {
        ensureConfig();
        return config.bonusMilestone.get(index);
    }

    public static int getMilestoneCount() {
        ensureConfig();
        if (config.bonusMilestone != null && !config.bonusMilestone.isEmpty())
            return config.bonusMilestone.size();
        if (config.pointMilestone != null && !config.pointMilestone.isEmpty())
            return config.pointMilestone.size();
        return 0;
    }

    /** Bổ sung giá trị mặc định khi config DB chưa load hoặc còn bản cũ. */
    static void ensureConfig() {
        if (config == null) {
            config = new DataConfig();
        }
        if (config.maxPoint <= 0) {
            config.maxPoint = 100;
        }
        if (config.pointMilestone == null || config.pointMilestone.isEmpty()) {
            config.pointMilestone = Arrays.asList(20, 40, 60, 80, 100);
        }
        if (config.bonusMilestone == null || config.bonusMilestone.isEmpty()) {
            config.bonusMilestone = Arrays.asList(
                    Arrays.asList(17L, 19L, 1L),
                    Arrays.asList(17L, 20L, 1L),
                    Arrays.asList(17L, 21L, 1L),
                    Arrays.asList(17L, 22L, 1L),
                    Arrays.asList(10L, 1L, 4L)
            );
        }
        int maxMilestone = config.pointMilestone.get(config.pointMilestone.size() - 1);
        if (config.maxAllPoint < maxMilestone) {
            config.maxAllPoint = maxMilestone;
        }
        initStaticLists();
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
