package game.dragonhero.task;

import game.cache.JCache;
import game.cache.JCachePubSub;
import game.dragonhero.server.AppInit;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;
import ozudo.base.helper.QuartzUtil;

import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

public class MainTask {
    public static boolean isShutdown = false;
    Scheduler scheduler;
    public static AtomicInteger jobCounter = new AtomicInteger(0);


    public static void main(String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+07:00"));
        new MainTask().start();
    }

    void start() throws Exception {
        AppInit.initAll();
        JCache.getInstance();
        JCachePubSub.getInstance().subscriberTelegram();
        if (true) {
            try {
                this.scheduler = StdSchedulerFactory.getDefaultScheduler();
                scheduler.start();
                scheduler.scheduleJob(QuartzUtil.getJob(MailCreator.class, "mailCreator"), QuartzUtil.getTrigger1Second("mailCreator"));
                scheduler.scheduleJob(QuartzUtil.getJob(CCUProcess.class, "ccuProcess"), QuartzUtil.getTriggerMinute("ccuProcess", 1));
                scheduler.scheduleJob(QuartzUtil.getJob(EndDayProcess.class, "endDayProcess"), QuartzUtil.getTriggerDaily("endDayProcess", 23, 58));
                scheduler.scheduleJob(QuartzUtil.getJob(EndMonthProcess.class, "endMonthProcess"), QuartzUtil.getTriggerMonthly("endMonthProcess", 23, 58));
            } catch (Exception se) {
                se.printStackTrace();
            }
            System.out.println("*********************************************");
            System.out.println("************ Start Game Task Server **************");
            System.out.println("*********************************************");
        }


    }

}
