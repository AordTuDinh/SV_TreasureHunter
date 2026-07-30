package game.treasure.service.day;

import game.object.MyUser;
import game.treasure.service.day.handlers.GrantDailyMailHandler;
import game.treasure.service.day.handlers.PushClientNotifyHandler;
import game.treasure.service.day.handlers.ResetPlotItemPointHandler;
import ozudo.base.log.Logs;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Pipeline xử lý khi server sang ngày mới.
 * Thêm handler mới: tạo class implement {@link ServerDayHandler} và append vào {@link #HANDLERS}.
 */
public final class ServerDayPipeline {
    private static final List<ServerDayHandler> HANDLERS = Collections.unmodifiableList(Arrays.asList(
            ResetPlotItemPointHandler.INSTANCE,
            GrantDailyMailHandler.INSTANCE,
            PushClientNotifyHandler.INSTANCE
    ));

    private ServerDayPipeline() {
    }

    public static void run(MyUser mUser) {
        if (mUser == null)
            return;
        ServerDayContext ctx = new ServerDayContext(mUser);
        for (ServerDayHandler handler : HANDLERS) {
            try {
                handler.onNewDay(ctx);
            } catch (Exception ex) {
                Logs.error("ServerDayPipeline " + handler.getClass().getSimpleName()
                        + " user=" + (mUser.getUser() != null ? mUser.getUser().getId() : "?")
                        + ": " + ex.getMessage());
            }
        }
        ctx.flushPrivateBonusToClient();
    }

    /** Cho test / mở rộng sau này. */
    public static List<ServerDayHandler> getHandlers() {
        return HANDLERS;
    }
}
