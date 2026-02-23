package game.config;

import com.google.gson.Gson;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class CfgWorldBoss {
    public static DataConfig config;
    private static final LocalTime RANGE1_START = LocalTime.of(12, 30);
    private static final LocalTime RANGE1_END   = LocalTime.of(13, 30);

    private static final LocalTime RANGE2_START = LocalTime.of(19, 30);
    private static final LocalTime RANGE2_END   = LocalTime.of(20, 30);
    private static final Map<Integer, BonusWeek> mBonusWeek = new HashMap<>();

    static List<Integer> killNumber = new ArrayList<>();

    public static void loadConfig(String strJson) {
        config = new Gson().fromJson(strJson, DataConfig.class);
        for (int i = 0; i < config.reward.size(); i++) {
            killNumber.add(config.reward.get(i).kill);
        }

        for (int i = 0; i < config.rewardWeek.size(); i++) {
            int from = config.rewardWeek.get(i).from;
            int to = config.rewardWeek.get(i).to;
            if (from == to) mBonusWeek.put(config.rewardWeek.get(i).to, config.rewardWeek.get(i));
            else {
                for (int j = from; j <= to; j++) {
                    mBonusWeek.put(j, config.rewardWeek.get(i));
                }
            }
        }
    }

    public static List<Long> getBonusWeekByRank(int rank) {
        for (var data : mBonusWeek.entrySet()) {
            if (rank <= data.getKey()) return new ArrayList<>(data.getValue().bonus);
        }
        return null;
    }

    public boolean isFreeParty (){
        return getBossTime()>0;
    }

    public static long getBossTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalTime t = now.toLocalTime();

        // Boss đang mở → trả về thời gian còn lại (dương)
        if (!t.isBefore(RANGE1_START) && t.isBefore(RANGE1_END)) {
            return Duration.between(t, RANGE1_END).toSeconds(); // > 0
        }
        if (!t.isBefore(RANGE2_START) && t.isBefore(RANGE2_END)) {
            return Duration.between(t, RANGE2_END).toSeconds(); // > 0
        }

        // Boss chưa mở → trả về thời gian đến lúc mở (âm)
        LocalDateTime nextOpen;

        if (t.isBefore(RANGE1_START)) {
            nextOpen = now.with(RANGE1_START);
        } else if (t.isBefore(RANGE2_START)) {
            nextOpen = now.with(RANGE2_START);
        } else {
            nextOpen = now.plusDays(1).with(RANGE1_START);
        }
        return -Duration.between(now, nextOpen).toSeconds(); // < 0
    }

    public static int getRank(int maxKill) {
        int index =0;
        for (int i = 0; i < killNumber.size(); i++) {
            if(maxKill < killNumber.get(i)) return i;
        }
        return index;
    }

    public static List<Long> getBonusByRank(int rank) {
        rank =Math.min(rank,config.reward.size()-1);
        return new ArrayList<>(config.reward.get(rank).bonus);
    }

    public class DataConfig {
        public List<BonusData> reward;
        public List<BonusWeek> rewardWeek;
    }


    public class BonusWeek {
        public int from, to;
        List<Long> bonus;
    }

    public class BonusData {
        int id;
        List<Long> bonus;
        int kill;
    }
}
