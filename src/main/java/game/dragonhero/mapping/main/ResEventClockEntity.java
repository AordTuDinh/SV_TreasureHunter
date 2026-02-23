package game.dragonhero.mapping.main;


import com.mysql.cj.util.TimeUtil;
import game.config.aEnum.TriggerEventTimer;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;
import ozudo.base.log.Logs;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class ResEventClockEntity extends BaseEntity {
    @Id
    int id;
    String trigger, bonus, price, oldPrice, name, desc, sale, bgr, data;
    int timeAlive; // thời gian tồn tại gói tính  bằng giờ
    @Transient
    List<Integer> triggerData;
    @Transient
    TriggerEventTimer triggerType;
    @Transient
    List<Date> time; // dành riêng cho type time

    public void init() {
        triggerData = GsonUtil.strToListInt(trigger);
        triggerType = TriggerEventTimer.get(triggerData.get(0));
        checkJson(id, bonus);
        checkJson(id, trigger);
        if (triggerType == TriggerEventTimer.TIME) {
            List<String> dateStrings = GsonUtil.strToListString(data);
            SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy");
            time = new ArrayList<>();
            for (String s : dateStrings) {
                try {
                    time.add(sdf.parse(s));
                } catch (ParseException e) {
                    Logs.error(e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public boolean inEvent() {
        long curTime = System.currentTimeMillis();
        return time.size() == 2 && curTime > time.get(0).getTime() && curTime < time.get(1).getTime();
    }

    public List<Long> getBonus() {
        return GsonUtil.strToListLong(bonus);
    }

    public List<Long> getPriceLong() {
        return GsonUtil.strToListLong(price);
    }

    public List<Long> getOldPriceLong() {
        return GsonUtil.strToListLong(oldPrice);
    }


}
