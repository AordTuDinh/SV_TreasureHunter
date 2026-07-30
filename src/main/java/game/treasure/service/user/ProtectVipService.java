package game.treasure.service.user;

import game.config.aEnum.VipType;
import game.object.DataDaily;
import game.object.MyUser;
import game.treasure.BattleConfig;
import game.treasure.mapping.UserVip;

/**
 * Khiên VIP daily: pool giây trong DataDaily, bật → time_protected, tắt/logout → hoàn giây còn lại.
 */
public final class ProtectVipService {
    private ProtectVipService() {
    }

    public static final int SECONDS_PER_HOUR = 3600;

    public static int hoursToSeconds(int hours) {
        return hours > 0 ? hours * SECONDS_PER_HOUR : 0;
    }

    public static int getPoolSeconds(MyUser mUser) {
        if (mUser == null || mUser.getUserDaily() == null) return 0;
        return Math.max(0, mUser.getUserDaily().getUDaily().getValue(DataDaily.PROTECTION_SHIELD_HOUR));
    }

    /** Cấp giây khiên vào daily từ tổng giờ vip_data (đã ×3600). */
    public static void grantDailyFromVipData(MyUser mUser) {
        if (mUser == null || mUser.getUSetting() == null) return;
        int hours = mUser.getEffectiveVipValue(VipType.PROTECTION_SHIELD_HOUR);
        int seconds = hoursToSeconds(hours);
        if (seconds <= 0) return;
        DataDaily daily = mUser.getUserDaily().getUDaily();
        if (daily.getValue(DataDaily.GET_PROTECTION_SHIELD_DAILY) != 0) return;
        daily.setValue(DataDaily.PROTECTION_SHIELD_HOUR, seconds);
        daily.setValueAndUpdate(DataDaily.GET_PROTECTION_SHIELD_DAILY, 1);
    }

    /** Cộng giây (từ giờ VIP) vào pool; nếu chưa cấp hôm nay thì ghi đủ tổng. */
    public static void addHoursToDaily(MyUser mUser, int hoursAdded, UserVip uVip) {
        if (mUser == null || hoursAdded <= 0) return;
        int addSec = hoursToSeconds(hoursAdded);
        DataDaily daily = mUser.getUserDaily().getUDaily();
        if (daily.getValue(DataDaily.GET_PROTECTION_SHIELD_DAILY) == 0) {
            int totalSec = hoursToSeconds(uVip.getValue(VipType.PROTECTION_SHIELD_HOUR));
            daily.setValue(DataDaily.PROTECTION_SHIELD_HOUR, totalSec);
            daily.setValue(DataDaily.GET_PROTECTION_SHIELD_DAILY, 1);
        } else {
            daily.addValue(DataDaily.PROTECTION_SHIELD_HOUR, addSec);
        }
        daily.update();
    }

    /**
     * Bật khiên VIP: chuyển toàn bộ pool → time_protected.
     * @return wire ms còn lại, hoặc -1 nếu lỗi
     */
    public static long activate(MyUser mUser) {
        if (mUser == null || mUser.getUData() == null) return -1;
        settleIfExpired(mUser);
        long now = System.currentTimeMillis();
        if (mUser.getUData().getTimeProtected() > now) return -1; // đang bảo vệ
        DataDaily daily = mUser.getUserDaily().getUDaily();
        int pool = Math.max(0, daily.getValue(DataDaily.PROTECTION_SHIELD_HOUR));
        if (pool <= 0) return -1;
        long until = now + pool * 1000L;
        daily.setValue(DataDaily.PROTECTION_SHIELD_HOUR, 0);
        daily.setValue(DataDaily.PROTECTION_FROM_VIP, 1);
        if (!daily.update()) return -1;
        if (!mUser.getUData().update(java.util.List.of("time_protected", until))) {
            daily.setValue(DataDaily.PROTECTION_SHIELD_HOUR, pool);
            daily.setValue(DataDaily.PROTECTION_FROM_VIP, 0);
            daily.update();
            return -1;
        }
        mUser.getUData().setTimeProtected(until);
        if (mUser.getPlayer() != null) {
            mUser.getPlayer().setTimeProtectedEnd(until);
        }
        return BattleConfig.toWireProtectedMs(until);
    }

    /**
     * Tắt khiên / logout: nếu đang VIP protect thì hoàn giây chưa dùng về pool.
     * @return wire ms sau settle (0)
     */
    public static long settleActive(MyUser mUser) {
        if (mUser == null || mUser.getUData() == null) return 0;
        long now = System.currentTimeMillis();
        long end = mUser.getUData().getTimeProtected();
        DataDaily daily = mUser.getUserDaily().getUDaily();
        boolean fromVip = daily.getValue(DataDaily.PROTECTION_FROM_VIP) == 1;
        if (end > now && fromVip) {
            int remainSec = (int) ((end - now) / 1000L);
            if (remainSec > 0) {
                daily.addValue(DataDaily.PROTECTION_SHIELD_HOUR, remainSec);
            }
        }
        daily.setValue(DataDaily.PROTECTION_FROM_VIP, 0);
        daily.update();
        if (end > 0) {
            mUser.getUData().update(java.util.List.of("time_protected", 0L));
            mUser.getUData().setTimeProtected(0);
            if (mUser.getPlayer() != null) {
                mUser.getPlayer().setTimeProtectedEnd(0);
            }
        }
        return 0;
    }

    /** Hết hạn tự nhiên: xóa flag, không hoàn (đã trừ hết). */
    public static void settleIfExpired(MyUser mUser) {
        if (mUser == null || mUser.getUData() == null) return;
        long end = mUser.getUData().getTimeProtected();
        if (end <= 0) return;
        if (end > System.currentTimeMillis()) return;
        DataDaily daily = mUser.getUserDaily().getUDaily();
        daily.setValue(DataDaily.PROTECTION_FROM_VIP, 0);
        daily.update();
        mUser.getUData().update(java.util.List.of("time_protected", 0L));
        mUser.getUData().setTimeProtected(0);
        if (mUser.getPlayer() != null) {
            mUser.getPlayer().setTimeProtectedEnd(0);
        }
    }
}
