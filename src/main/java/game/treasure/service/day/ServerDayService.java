package game.treasure.service.day;

import game.monitor.Online;
import game.object.MyUser;
import game.treasure.mapping.UserDailyEntity;
import io.netty.channel.Channel;
import ozudo.base.log.Logs;

/**
 * Điểm vào duy nhất khi server qua ngày — dựa {@code user_daily.eventId} vs {@link ozudo.base.helper.DateTime#getNumberDay()}.
 * Không tin giờ client. Mọi logic qua ngày chạy qua {@link ServerDayPipeline}.
 */
public final class ServerDayService {
    private ServerDayService() {
    }

    /**
     * Nếu đã sang ngày mới theo server → chạy pipeline qua ngày.
     *
     * @return true nếu vừa chạy pipeline
     */
    public static boolean ensureCurrentDay(MyUser mUser) {
        if (mUser == null)
            return false;
        try {
            UserDailyEntity daily = mUser.getUserDaily();
            if (!daily.consumeRolledOver())
                return false;
            ServerDayPipeline.run(mUser);
            return true;
        } catch (Exception ex) {
            Logs.error("ServerDayService.ensureCurrentDay user="
                    + (mUser.getUser() != null ? mUser.getUser().getId() : "?") + ": " + ex.getMessage());
            return false;
        }
    }

    /** Job 0h — pipeline cho mọi user đang online trên game server. */
    public static void ensureCurrentDayForOnlineUsers() {
        for (Channel ch : Online.getAllChanel()) {
            MyUser u = Online.getMUser(ch);
            if (u != null)
                ensureCurrentDay(u);
        }
    }
}
