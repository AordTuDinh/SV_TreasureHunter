package game.treasure.service.day.handlers;

import game.treasure.service.day.ServerDayContext;
import game.treasure.service.day.ServerDayHandler;

/** Cập nhật badge / notify sau khi qua ngày. */
public final class PushClientNotifyHandler implements ServerDayHandler {
    public static final PushClientNotifyHandler INSTANCE = new PushClientNotifyHandler();

    private PushClientNotifyHandler() {
    }

    @Override
    public void onNewDay(ServerDayContext ctx) {
        if (ctx.getMUser() != null && ctx.getMUser().getChannel() != null)
            ctx.getMUser().sendNotify();
    }
}
