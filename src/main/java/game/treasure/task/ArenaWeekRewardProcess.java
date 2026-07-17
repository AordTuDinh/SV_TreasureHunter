package game.treasure.task;

import game.treasure.server.App;
import game.treasure.server.AppInit;
import game.treasure.service.arena.ArenaWeekRewardService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import ozudo.base.log.Logs;

/**
 * Chủ nhật 23:55 GMT+7 — trả thưởng top 100 arena tuần + lưu top_arena.
 */
@DisallowConcurrentExecution
public class ArenaWeekRewardProcess extends JobCounter implements Job {

    public static void main(String[] args) throws Exception {
        AppInit.initAll();
        App.initConfig();
        new ArenaWeekRewardProcess().executeJob();
    }

    @Override
    protected void executeJob() {
        try {
            App.initConfig();
        } catch (Exception ex) {
            Logs.error(ex);
        }
        try {
            ArenaWeekRewardService.run();
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }
}
