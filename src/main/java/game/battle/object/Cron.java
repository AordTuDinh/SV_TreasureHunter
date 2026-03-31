package game.battle.object;

import org.quartz.Job;
import org.quartz.JobExecutionContext;

public class Cron implements Job {
    @Override
    public void execute(JobExecutionContext context) {
        Coroutine cron = (Coroutine) context.getJobDetail().getJobDataMap().get("Coroutine");
        if (cron == null) return;
        if (cron.cronCount == 1) {
            cron.action.Call();
        }
        cron.cronCount++;
    }
}
