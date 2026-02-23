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

    //public  void Cron2(int userId, String nameCron, long miliS) throws SchedulerException {
    //    final JobKey jobKey = new JobKey(userId + "_" + nameCron, "sv" + CfgServer.serverId);
    //    final JobDetail job = JobBuilder.newJob(Cron.class).withIdentity(jobKey).build();
    //    //final Trigger trigger = TriggerBuilder.newTrigger().withIdentity(userId + "_" + nameCron, "sv" + CfgServer.serverId).
    //    //        withSchedule(CronScheduleBuilder.cronSchedule("0/5 * * * * ?")).build();
    //    System.out.println("Time start---------- " + System.currentTimeMillis() / 1000);
    //    final Trigger trigger = TriggerBuilder.newTrigger().withIdentity(userId + "_" + nameCron, "sv" + CfgServer.serverId).
    //            startAt(Calendar.getInstance().getTime()).withSchedule(simpleSchedule().withIntervalInMilliseconds(miliS).withRepeatCount(1)).build();
    //    final Scheduler scheduler = new StdSchedulerFactory().getScheduler();
    //    scheduler.start();
    //    scheduler.scheduleJob(job, trigger);
    //}
}
