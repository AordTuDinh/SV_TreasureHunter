package game.dragonhero.mapping.main;

import game.config.CfgFarm;
import game.config.aEnum.DecoPointType;
import game.dragonhero.mapping.UserDataEntity;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
public class ResSeedEntity extends AbstractItemFarm implements Serializable {
    int number, time, isTree, exp, levelRequire;
    String season, price;

    @Transient
    int timeSeconds;


    public void init() {
        timeSeconds = DateTime.minuteToSeconds(time);
        checkJson(id, season);
        checkJson(id, price);
    }

    public List<Long> getPrice() {
        return GsonUtil.strToListLong(price);
    }

    private int getTime() {
        return timeSeconds;
    }

    public int getTimeHarvest(UserDataEntity uData) {
        List<Integer> buff = uData.getFarmPoint();
        float curPer = buff.get(DecoPointType.DEC_TIME.value) / 100f;
        float per = curPer > CfgFarm.MAX_TIME_DEC_HARVEST ? CfgFarm.MAX_TIME_DEC_HARVEST : curPer;
        return (int) (timeSeconds - per / 100f * timeSeconds);
    }

    public List<Integer> getSeason() {
        return GsonUtil.strToListInt(season);
    }
}
