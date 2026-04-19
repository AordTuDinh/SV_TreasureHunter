package game.object;

import game.config.CfgServer;
import game.treasure.table.MonoRoom;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import ozudo.base.log.Logs;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class TaskMonitor {
    static TaskMonitor instance;

    public static TaskMonitor getInstance() {
        if (instance == null) instance = new TaskMonitor();
        return instance;
    }

    public Map<Long, MonoRoom> mRoom = new HashMap<>();
    public static Map<Long, String> mBattleIdToKey = new HashMap<>();
    public static Map<String, Long> mKeyToBattleId = new HashMap<>();
    int threadCount = 0;
    static long counterId;

    public void addRoom(MonoRoom room) {
        if (room != null) mRoom.put(room.getBattleId(), room);
    }

    public void removeRoom(long id) {
        mRoom.remove(id);
    }

    public MonoRoom getRoom(long id) {
        return mRoom.getOrDefault(id,null);
    }

    public MonoRoom getRoom(String roomKey) {
        if(!mKeyToBattleId.containsKey(roomKey)) return null;
        return getRoom(mKeyToBattleId.get(roomKey));
    }

    static Scheduler sched;

    static {
        try {
            sched = new StdSchedulerFactory().getScheduler();
            sched.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public JobKey submit(MonoRoom room, int intervals) {
        JobDetail job = newJob(SimulateJob.class).withIdentity("job_" + room.getBattleId() + "_" + intervals, "sv" + CfgServer.serverId).storeDurably(false).build();
        SimpleTrigger trigger = newTrigger().withIdentity("trigger_" + room.getBattleId() + "_" + intervals, "sv" + CfgServer.serverId).
                startAt(Calendar.getInstance().getTime()).withSchedule(simpleSchedule().withIntervalInMilliseconds(intervals).repeatForever()).build();
        job.getJobDataMap().put("room", room);
        job.getJobDataMap().put("intervals", intervals);
        try {
            sched.scheduleJob(job, trigger);
        } catch (Exception ex) {
            Logs.warn("Failed to submit job " + ex.getMessage());
        }
        threadCount++;
        return job.getKey();
    }

    public void cancel(JobKey key) {
        try {
            if (sched.checkExists(key)) {
                sched.deleteJob(key);
            }
        } catch (SchedulerException e) {
            Logs.warn("Failed to delete job " + key + ": " + e.getMessage());
        }
        threadCount--;
    }


    public static void addBattleKey(long battleId, String key){
        mBattleIdToKey.put(battleId,key);
        mKeyToBattleId.put(key,battleId);
    }

    public static String[] getKeyRoomById(long battleId) {
        return (mBattleIdToKey.getOrDefault(battleId, "0_0")).split("_");
    }

    //region state
    public static synchronized long getCounterId() {
        return ++counterId;
    }

}
