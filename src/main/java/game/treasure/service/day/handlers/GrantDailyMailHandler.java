package game.treasure.service.day.handlers;

import game.treasure.service.Services;
import game.treasure.service.day.ServerDayContext;
import game.treasure.service.day.ServerDayHandler;

/** Quà ngày qua mail (VIP, gói, thẻ…) — idempotent nhờ cờ {@code user_daily}. */
public final class GrantDailyMailHandler implements ServerDayHandler {
    public static final GrantDailyMailHandler INSTANCE = new GrantDailyMailHandler();

    private GrantDailyMailHandler() {
    }

    @Override
    public void onNewDay(ServerDayContext ctx) {
        if (ctx.getMUser() != null)
            Services.userService.grantDailyMailRewards(ctx.getMUser());
    }
}
