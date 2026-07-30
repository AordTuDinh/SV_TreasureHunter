package game.treasure.task;

import game.treasure.service.day.ServerDayService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import ozudo.base.log.Logs;

/** 0h server — chạy {@link ServerDayService} cho user online (không phụ thuộc client). */
@DisallowConcurrentExecution
public class ServerDayRolloverJob implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            ServerDayService.ensureCurrentDayForOnlineUsers();
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }
}
