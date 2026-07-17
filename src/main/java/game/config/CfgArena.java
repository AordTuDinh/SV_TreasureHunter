package game.config;

import com.google.gson.Gson;
import game.config.aEnum.ItemPointKey;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

/**
 * DB key {@code config_arena} → {@link #loadConfig(String)}.
 * Giờ theo GMT+7 (VN).
 */
public class CfgArena {
    public static DataConfig config;

    /** TODO test xong set false — đấu trường luôn mở, ghép ngay không chờ 5p đầu khung. */
    public static final boolean FORCE_OPEN_FOR_TEST = true;

    private static final long TEST_REMAIN_MS = 90L * 60 * 1000;

    public static void loadConfig(String strJson) {
        config = new Gson().fromJson(strJson, DataConfig.class);
        if (config == null)
            config = defaults();
        fillDefaults(config);
    }

    public static DataConfig cfg() {
        if (config == null) {
            config = defaults();
            fillDefaults(config);
        }
        return config;
    }

    public static int minCup() {
        return Math.max(0, cfg().minCup);
    }

    public static int registerGem() {
        return Math.max(0, cfg().registerGem);
    }

    public static int cupWin() {
        return cfg().cupWin;
    }

    public static int cupLose() {
        return cfg().cupLose;
    }

    public static int coinWin() {
        return Math.max(0, cfg().coinWin);
    }

    public static int coinLose() {
        return Math.max(0, cfg().coinLose);
    }

    public static int coinDraw() {
        return Math.max(0, cfg().coinDraw);
    }

    public static int matchDurationSec() {
        return Math.max(10, cfg().matchDurationSec);
    }

    public static int registerDelaySec() {
        return Math.max(0, cfg().registerDelaySec);
    }

    public static int cupMatchRange() {
        return Math.max(0, cfg().cupMatchRange);
    }

    public static int moveNormalSec() {
        return Math.max(0, cfg().moveNormalSec);
    }

    public static int moveSlowSec() {
        return Math.max(0, cfg().moveSlowSec);
    }

    public static int moveSlowPercentPerSec() {
        return Math.max(1, cfg().moveSlowPercentPerSec);
    }

    public static int arenaCoinPointId() {
        return cfg().arenaCoinPointId > 0 ? cfg().arenaCoinPointId : ItemPointKey.ARENA_COIN.id;
    }

    public static float[] posA() {
        return xy(cfg().posA, 6.6f, -34f);
    }

    public static float[] posB() {
        return xy(cfg().posB, 9.3f, -34f);
    }

    public static float[] dirA() {
        return xy(cfg().dirA, 1f, 0f);
    }

    public static float[] dirB() {
        return xy(cfg().dirB, -1f, 0f);
    }

    public static float[] posEndA() {
        if (cfg().posEndA != null && cfg().posEndA.length >= 2)
            return cfg().posEndA;
        // fallback cũ posEnd
        return xy(cfg().posEnd, 3.8f, -34f);
    }

    public static float[] posEndB() {
        if (cfg().posEndB != null && cfg().posEndB.length >= 2)
            return cfg().posEndB;
        return xy(cfg().posEnd, 12f, -34f);
    }

    static float[] xy(float[] arr, float dx, float dy) {
        return arr != null && arr.length >= 2 ? arr : new float[]{dx, dy};
    }

    public static List<WeekReward> weekRewards() {
        return cfg().weekRewards != null ? cfg().weekRewards : Collections.emptyList();
    }

    /** Bonus tuần theo hạng 1-based; null nếu ngoài config. */
    public static List<Long> weekBonusForRank(int rank) {
        for (WeekReward r : weekRewards()) {
            if (r == null || r.bonus == null)
                continue;
            if (rank >= r.from && rank <= r.to)
                return new ArrayList<>(r.bonus);
        }
        return null;
    }

    /** Cửa sổ đang mở (đăng ký được). */
    public static boolean isWindowOpen(long nowMs) {
        if (FORCE_OPEN_FOR_TEST)
            return true;
        return currentWindow(nowMs) != null;
    }

    /** Đã qua thời gian chờ 5p đầu khung → bắt đầu ghép. */
    public static boolean canMatch(long nowMs) {
        if (FORCE_OPEN_FOR_TEST)
            return true;
        Window w = currentWindow(nowMs);
        if (w == null)
            return false;
        return nowMs >= w.startMs + registerDelaySec() * 1000L;
    }

    public static Window currentWindow(long nowMs) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+07:00"));
        cal.setTimeInMillis(nowMs);
        int dayMinute = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        for (int[] slot : cfg().windows) {
            if (slot == null || slot.length < 4)
                continue;
            int start = slot[0] * 60 + slot[1];
            int end = slot[2] * 60 + slot[3];
            if (dayMinute >= start && dayMinute < end) {
                Calendar startCal = (Calendar) cal.clone();
                startCal.set(Calendar.HOUR_OF_DAY, slot[0]);
                startCal.set(Calendar.MINUTE, slot[1]);
                startCal.set(Calendar.SECOND, 0);
                startCal.set(Calendar.MILLISECOND, 0);
                Calendar endCal = (Calendar) cal.clone();
                endCal.set(Calendar.HOUR_OF_DAY, slot[2]);
                endCal.set(Calendar.MINUTE, slot[3]);
                endCal.set(Calendar.SECOND, 0);
                endCal.set(Calendar.MILLISECOND, 0);
                return new Window(startCal.getTimeInMillis(), endCal.getTimeInMillis());
            }
        }
        return null;
    }

    /** ms đến lần mở kế tiếp nếu đang đóng; nếu đang mở trả 0. */
    public static long msUntilOpen(long nowMs) {
        if (FORCE_OPEN_FOR_TEST || isWindowOpen(nowMs))
            return 0;
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+07:00"));
        cal.setTimeInMillis(nowMs);
        int dayMinute = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        int best = Integer.MAX_VALUE;
        for (int[] slot : cfg().windows) {
            if (slot == null || slot.length < 4)
                continue;
            int start = slot[0] * 60 + slot[1];
            int diff = start - dayMinute;
            if (diff <= 0)
                diff += 24 * 60;
            best = Math.min(best, diff);
        }
        if (best == Integer.MAX_VALUE)
            return 24 * 60 * 60 * 1000L;
        return best * 60L * 1000L;
    }

    public static long msUntilClose(long nowMs) {
        if (FORCE_OPEN_FOR_TEST)
            return TEST_REMAIN_MS;
        Window w = currentWindow(nowMs);
        if (w == null)
            return 0;
        return Math.max(0, w.endMs - nowMs);
    }

    static DataConfig defaults() {
        DataConfig d = new DataConfig();
        fillDefaults(d);
        return d;
    }

    static void fillDefaults(DataConfig d) {
        if (d.minCup <= 0)
            d.minCup = 50;
        if (d.registerGem <= 0)
            d.registerGem = 10;
        if (d.cupWin == 0)
            d.cupWin = 5;
        if (d.cupLose == 0)
            d.cupLose = -5;
        if (d.coinWin <= 0)
            d.coinWin = 2;
        if (d.coinLose <= 0)
            d.coinLose = 1;
        if (d.coinDraw <= 0)
            d.coinDraw = 1;
        if (d.matchDurationSec <= 0)
            d.matchDurationSec = 90;
        if (d.registerDelaySec <= 0)
            d.registerDelaySec = 300;
        if (d.cupMatchRange <= 0)
            d.cupMatchRange = 10;
        if (d.moveNormalSec <= 0)
            d.moveNormalSec = 10;
        if (d.moveSlowSec <= 0)
            d.moveSlowSec = 10;
        if (d.moveSlowPercentPerSec <= 0)
            d.moveSlowPercentPerSec = 10;
        if (d.arenaCoinPointId <= 0)
            d.arenaCoinPointId = ItemPointKey.ARENA_COIN.id;
        if (d.posA == null || d.posA.length < 2)
            d.posA = new float[]{6.6f, -34f};
        if (d.posB == null || d.posB.length < 2)
            d.posB = new float[]{9.3f, -34f};
        if (d.dirA == null || d.dirA.length < 2)
            d.dirA = new float[]{1f, 0f};
        if (d.dirB == null || d.dirB.length < 2)
            d.dirB = new float[]{-1f, 0f};
        if (d.posEndA == null || d.posEndA.length < 2)
            d.posEndA = new float[]{3.8f, -34f};
        if (d.posEndB == null || d.posEndB.length < 2)
            d.posEndB = new float[]{12f, -34f};
        if (d.posEnd == null || d.posEnd.length < 2)
            d.posEnd = d.posEndA;
        if (d.windows == null || d.windows.isEmpty()) {
            d.windows = new ArrayList<>();
            d.windows.add(new int[]{11, 30, 12, 30});
            d.windows.add(new int[]{20, 30, 21, 30});
        }
        if (d.weekRewards == null || d.weekRewards.isEmpty()) {
            d.weekRewards = defaultWeekRewards();
        }
    }

    static List<WeekReward> defaultWeekRewards() {
        List<WeekReward> list = new ArrayList<>();
        list.add(week(1, 1, 1, 10));
        list.add(week(2, 2, 1, 10));
        list.add(week(3, 3, 1, 10));
        list.add(week(4, 10, 1, 10));
        list.add(week(11, 20, 1, 10));
        list.add(week(21, 50, 1, 10));
        list.add(week(51, 100, 1, 10)); // 50-100 trong design → 51-100 tránh overlap
        return list;
    }

    static WeekReward week(int from, int to, long... bonus) {
        WeekReward r = new WeekReward();
        r.from = from;
        r.to = to;
        r.bonus = new ArrayList<>();
        for (long v : bonus)
            r.bonus.add(v);
        return r;
    }

    public static class DataConfig {
        public int minCup = 50;
        public int registerGem = 10;
        public int cupWin = 5;
        public int cupLose = -5;
        public int coinWin = 2;
        public int coinLose = 1;
        public int coinDraw = 1;
        public int matchDurationSec = 90;
        public int registerDelaySec = 300;
        public int cupMatchRange = 10;
        public int moveNormalSec = 10;
        public int moveSlowSec = 10;
        public int moveSlowPercentPerSec = 10;
        /** Item point id xu đấu trường (mặc định 14). */
        public int arenaCoinPointId = 14;
        public float[] posA;
        public float[] posB;
        public float[] dirA;
        public float[] dirB;
        public float[] posEndA;
        public float[] posEndB;
        /** @deprecated dùng posEndA/posEndB */
        public float[] posEnd;
        /** [hStart, mStart, hEnd, mEnd] GMT+7 */
        public List<int[]> windows;
        /** Phần thưởng top tuần — sửa bonus tại đây. */
        public List<WeekReward> weekRewards;
    }

    public static class WeekReward {
        public int from;
        public int to;
        public List<Long> bonus;
    }

    public static class Window {
        public final long startMs;
        public final long endMs;

        public Window(long startMs, long endMs) {
            this.startMs = startMs;
            this.endMs = endMs;
        }
    }
}
