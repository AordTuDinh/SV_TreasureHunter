package ozudo.base.helper;

import org.quartz.*;
import ozudo.base.log.Logs;

import static org.quartz.CronScheduleBuilder.dailyAtHourAndMinute;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

public class QuartzUtil {

    private static final String groupName = "aordgame";

    public static JobDetail getJob(Class clazz, String name) {
        return JobBuilder.newJob(clazz)
                .withIdentity(name, groupName)
                .build();
    }

    public static Trigger getTrigger1Second(String name) {
        return TriggerBuilder.newTrigger()
                .withIdentity(name, groupName)
                .startNow().withSchedule(simpleSchedule().withIntervalInSeconds(1).repeatForever())
                .build();
    }

    public static Trigger getTriggerMinute(String name, int minute) {
        return TriggerBuilder.newTrigger()
                .withIdentity(name, groupName)
                .startNow().withSchedule(simpleSchedule().withIntervalInMinutes(minute).repeatForever())
                .build();
    }

    private static ScheduleBuilder<SimpleTrigger> getScheduleBuilder() {
        return simpleSchedule().repeatForever().withIntervalInSeconds(1);
    }

    public static Trigger getTriggerDaily(String name, int hour, int minute) {
        return TriggerBuilder.newTrigger()
                .withIdentity(name, groupName)
                .startNow().withSchedule(dailyAtHourAndMinute(hour, minute))
                .build();
    }

    public static Trigger getTriggerMonthly(String name, int hour, int minute) {
        DateBuilder.validateHour(hour);
        DateBuilder.validateMinute(minute);
        try {
            CronScheduleBuilder cron = CronScheduleBuilder.cronSchedule(String.format("0 %d %d L * ?", minute, hour));
            return TriggerBuilder.newTrigger()
                    .withIdentity(name, groupName)
                    .startNow().withSchedule(cron)
                    .build();
        } catch (Exception e) {
            Logs.error(e);
        }
        return null;
    }
}
