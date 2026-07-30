package game.treasure.service.day.handlers;

import game.config.aEnum.ItemPointKey;
import game.treasure.mapping.UserItemPointEntity;
import game.treasure.service.day.ServerDayContext;
import game.treasure.service.day.ServerDayHandler;
import game.treasure.service.user.Bonus;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Ô đất (PLOT) — xóa số dư còn lại, chỉ dùng trong ngày. */
public final class ResetPlotItemPointHandler implements ServerDayHandler {
    public static final ResetPlotItemPointHandler INSTANCE = new ResetPlotItemPointHandler();

    private ResetPlotItemPointHandler() {
    }

    @Override
    public void onNewDay(ServerDayContext ctx) {
        if (ctx.getMUser() == null)
            return;
        List<Long> wire = resetPlot(ctx);
        ctx.queuePrivateBonus(wire);
    }

    static List<Long> resetPlot(ServerDayContext ctx) {
        var mUser = ctx.getMUser();
        int pointId = ItemPointKey.PLOT.id;
        int cur = mUser.getResources().getItemPointNumber(pointId);
        if (cur <= 0)
            return Collections.emptyList();

        UserItemPointEntity row = mUser.getResources().getItemPoint(pointId);
        if (row != null) {
            row.setNumber(0);
            row.updateNumber(0);
            row.setNumberDirty(false);
        }
        return Arrays.asList((long) Bonus.BONUS_ITEM_POINT, (long) pointId, (long) -cur, 0L);
    }
}
