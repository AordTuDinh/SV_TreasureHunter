package game.treasure.service.user;

import game.battle.calculate.IMath;
import game.battle.object.Coroutine;
import game.battle.object.Point;
import game.monitor.Online;
import game.object.MyUser;
import game.protocol.CommonProto;
import game.treasure.controller.UserHandler;
import game.treasure.server.IAction;
import org.quartz.SchedulerException;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.StringHelper;
import ozudo.base.helper.Util;
import ozudo.base.log.Logs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * user_data.buff = [pointId, value×1000, endTimeMs, …] — nhiều entry có thể trùng pointId (stack, không đè).
 */
public final class UserBuff {
    private static final int TRIPLET = 3;
    private static final Map<String, Coroutine> expireJobs = new ConcurrentHashMap<>();

    private UserBuff() {
    }

    public static List<Long> parseTriplets(String buffJson) {
        if (buffJson == null || buffJson.isBlank())
            return new ArrayList<>();
        List<Long> raw = GsonUtil.strToListLong(buffJson);
        if (raw.isEmpty())
            return raw;
        if (raw.size() % TRIPLET != 0)
            return new ArrayList<>();
        return raw;
    }

    public static void purgeExpired(MyUser mUser) {
        if (mUser == null || mUser.getUData() == null)
            return;
        long now = System.currentTimeMillis();
        List<Long> triplets = parseTriplets(mUser.getUData().getBuff());
        List<Long> active = new ArrayList<>();
        for (int i = 0; i + 2 < triplets.size(); i += TRIPLET) {
            if (triplets.get(i + 2) > now) {
                active.add(triplets.get(i));
                active.add(triplets.get(i + 1));
                active.add(triplets.get(i + 2));
            }
        }
        if (active.size() != triplets.size())
            save(mUser, active);
    }

    public static void applyActiveToPoint(MyUser mUser, Point point) {
        if (mUser == null || point == null)
            return;
        long now = System.currentTimeMillis();
        List<Long> triplets = parseTriplets(mUser.getUData().getBuff());
        for (int i = 0; i + 2 < triplets.size(); i += TRIPLET) {
            long end = triplets.get(i + 2);
            if (end <= now)
                continue;
            int pointId = triplets.get(i).intValue();
            float value = triplets.get(i + 1) / 1000f;
            IMath.addPointData(point, pointId, value);
        }
    }

    /** Thêm buff mới (không gỡ buff cũ cùng pointId). */
    public static long addBuff(MyUser mUser, int pointId, long valueScaled, long durationSec) {
        if (mUser == null || durationSec <= 0 || valueScaled == 0)
            return 0;
        long now = System.currentTimeMillis();
        long end = now + durationSec * 1000L;
        List<Long> triplets = new ArrayList<>(parseTriplets(mUser.getUData().getBuff()));
        triplets.add((long) pointId);
        triplets.add(valueScaled);
        triplets.add(end);
        save(mUser, triplets);
        scheduleExpire(mUser, end);
        return end;
    }

    /**
     * Cấp buff + tính lại stat + push ADD_BUFF cho client.
     *
     * @return endTimeMs của entry vừa thêm
     */
    public static long grantBuff(MyUser mUser, int pointId, long valueScaled, long durationSec) {
        long end = addBuff(mUser, pointId, valueScaled, durationSec);
        if (end <= 0)
            return 0;
        mUser.reCalculatePoint();
        long timeRemain = (end - System.currentTimeMillis()) / 1000;
        if (timeRemain > 0)
            pushAddBuff(mUser, pointId, valueScaled, timeRemain);
        return end;
    }

    public static void pushAddBuff(MyUser mUser, int pointId, long valueScaled, long timeRemainSec) {
        if (mUser == null || mUser.getChannel() == null || timeRemainSec <= 0)
            return;
        Util.sendProtoData(mUser.getChannel(),
                CommonProto.getCommonVector((long) pointId, valueScaled, timeRemainSec),
                IAction.ADD_BUFF);
    }

    static void save(MyUser mUser, List<Long> triplets) {
        String db = StringHelper.toDBString(triplets);
        mUser.getUData().update(List.of("buff", db));
        mUser.getUData().setBuff(db);
    }

    public static List<Long> buildWire(MyUser mUser) {
        purgeExpired(mUser);
        long now = System.currentTimeMillis();
        List<Long> ret = new ArrayList<>();
        List<Long> triplets = parseTriplets(mUser.getUData().getBuff());
        for (int i = 0; i + 2 < triplets.size(); i += TRIPLET) {
            long end = triplets.get(i + 2);
            if (end <= now)
                continue;
            long timeRemain = (end - now) / 1000;
            if (timeRemain <= 0)
                continue;
            ret.add(triplets.get(i));
            ret.add(triplets.get(i + 1));
            ret.add(timeRemain);
        }
        return ret;
    }

    public static void scheduleExpire(MyUser mUser, long endTimeMs) {
        if (mUser == null)
            return;
        int userId = mUser.getUser().getId();
        String key = jobKey(userId, endTimeMs);
        if (expireJobs.containsKey(key))
            return;
        long delay = endTimeMs - System.currentTimeMillis();
        if (delay <= 0) {
            onExpire(userId, endTimeMs);
            return;
        }
        try {
            Coroutine job = new Coroutine(() -> onExpire(userId, endTimeMs), userId, jobName(endTimeMs), delay);
            expireJobs.put(key, job);
        } catch (SchedulerException e) {
            Logs.warn("UserBuff schedule expire failed user=" + userId + " end=" + endTimeMs + ": " + e.getMessage());
        }
    }

    static void cancelExpire(int userId, long endTimeMs) {
        Coroutine old = expireJobs.remove(jobKey(userId, endTimeMs));
        if (old != null)
            old.StopCoroutineJob();
    }

    static void onExpire(int userId, long endTimeMs) {
        expireJobs.remove(jobKey(userId, endTimeMs));
        MyUser mUser = Online.getMUser(userId);
        if (mUser == null)
            return;
        List<Long> triplets = new ArrayList<>(parseTriplets(mUser.getUData().getBuff()));
        boolean changed = false;
        for (int i = 0; i + 2 < triplets.size(); ) {
            if (triplets.get(i + 2) == endTimeMs) {
                triplets.remove(i);
                triplets.remove(i);
                triplets.remove(i);
                changed = true;
            } else {
                i += TRIPLET;
            }
        }
        if (!changed)
            purgeExpired(mUser);
        else
            save(mUser, triplets);
        mUser.reCalculatePoint();
        UserHandler.buffInfo(mUser);
    }

    public static void onLogin(MyUser mUser) {
        if (mUser == null)
            return;
        purgeExpired(mUser);
        long now = System.currentTimeMillis();
        Set<Long> ends = new HashSet<>();
        List<Long> triplets = parseTriplets(mUser.getUData().getBuff());
        for (int i = 0; i + 2 < triplets.size(); i += TRIPLET) {
            long end = triplets.get(i + 2);
            if (end > now)
                ends.add(end);
        }
        for (long end : ends)
            scheduleExpire(mUser, end);
    }

    static String jobName(long endTimeMs) {
        return "artifact_buff_" + endTimeMs;
    }

    static String jobKey(int userId, long endTimeMs) {
        return userId + "_" + jobName(endTimeMs);
    }
}
