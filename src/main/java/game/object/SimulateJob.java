package game.object;

import game.battle.model.Character;
import game.battle.object.Mono;
import game.config.CfgBattle;
import game.dragonhero.table.BaseRoom;
import lombok.NoArgsConstructor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@NoArgsConstructor
public class SimulateJob implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        BaseRoom room = (BaseRoom) context.getJobDetail().getJobDataMap().get("room");
        int intervals = context.getJobDetail().getJobDataMap().getInt("intervals");
        if (intervals == CfgBattle.periodUpdate) {
            room.Update();
            for (int i = 0; i < room.getAPlayer().size(); i++) {
                room.getAPlayer().get(i).Update();
            }
            for (int i = 0; i < room.getAEnemy().size(); i++) {
                room.getAEnemy().get(i).Update();
            }
        } else if (intervals == CfgBattle.periodFixedUpdate) {
            room.FixedUpdate();
        } else if (intervals == CfgBattle.periodUpdateLow) {
            room.LastUpdate();
        } else if (intervals == CfgBattle.periodEffectUpdate) {
            room.EffectUpdate();
        } else if (intervals == CfgBattle.periodUpdate1s) {
            room.Update1s();
            for (int i = 0; i < room.getAPlayer().size(); i++) {
                room.getAPlayer().get(i).Update1s();
            }
            for (int i = 0; i < room.getAEnemy().size(); i++) {
                room.getAEnemy().get(i).Update1s();
            }
        }
    }
}
