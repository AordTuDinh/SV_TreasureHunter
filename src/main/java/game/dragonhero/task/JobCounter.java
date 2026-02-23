package game.dragonhero.task;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ozudo.base.log.Logs;
import ozudo.base.log.slib_Logger;

public abstract class JobCounter implements Job {

    protected void startingJob() {
        MainTask.jobCounter.incrementAndGet();
    }

    protected void endingJob() {
        MainTask.jobCounter.decrementAndGet();
    }

    protected abstract void executeJob();

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (!MainTask.isShutdown) {
            startingJob();
            try {
                executeJob();
            } catch (Exception ex) {
                Logs.error(ex);
            }
            endingJob();
        }
    }

    protected static Logger getFailLogger() {
        return LoggerFactory.getLogger("DBFAIL");
    }

    protected static Logger getLogger() {
        return slib_Logger.root();
    }
}
