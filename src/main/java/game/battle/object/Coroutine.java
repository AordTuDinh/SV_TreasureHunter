package game.battle.object;


import game.config.CfgServer;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import ozudo.base.helper.DateTime;

import java.util.Calendar;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

// Lưu ý, trong class này có 2 loại coroutine có cơ chế hoạt động hoàn toàn khác nhau...
public class Coroutine {
    // check id để còn process
    public long timeAction;
    public ICoroutine action;
    // coroutine2
    int cronCount = 0;
    JobKey jobKey;
    Scheduler scheduler;


    // delayActive = seconds
    public Coroutine(float delayActive, ICoroutine action) {
        this.timeAction = System.currentTimeMillis() + (long) (delayActive * DateTime.SECOND2_MILLI_SECOND);
        this.action = action;
    }

    public Coroutine(ICoroutine action, int userId, String nameCron, long ms) throws SchedulerException {
        jobKey = new JobKey(userId + "_" + nameCron, "sv" + CfgServer.serverId);
        JobDetail job = JobBuilder.newJob(Cron.class).withIdentity(jobKey).build();
        this.action = action;
        cronCount = 0;
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity(userId + "_" + nameCron, "sv" + CfgServer.serverId).startAt(Calendar.getInstance().getTime()).withSchedule(simpleSchedule().withIntervalInMilliseconds(ms).withRepeatCount(1)).build();
        job.getJobDataMap().put("Coroutine", this);
        scheduler = new StdSchedulerFactory().getScheduler();
        scheduler.start();
        scheduler.scheduleJob(job, trigger);
    }

    public void StopCoroutineJob() {
        try {
            scheduler.deleteJob(jobKey);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }
}
