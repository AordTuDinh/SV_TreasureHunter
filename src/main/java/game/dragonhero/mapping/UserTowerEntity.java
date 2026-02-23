package game.dragonhero.mapping;


import game.config.CfgTower;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.DateTime;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "user_tower")
public class UserTowerEntity implements Serializable {
    @Id
    int userId;
    int level, numberKey, numberBuy, server;
    long countdown, lastAttack, eventId;

    public UserTowerEntity(UserEntity user) {
        this.userId = user.getId();
        this.level = 1;
        this.numberKey = CfgTower.config.maxKey;
        this.countdown = -1;
        this.numberBuy = 0;
        this.lastAttack = 0;
        this.server = user.getServer();
        this.eventId = DateTime.getNumberDay();
    }

    public void checkKey() { // return cd
        if (countdown != -1 && numberKey < CfgTower.config.maxKey) {
            // client gửi sớm hơn 500ms nên + 1s cho chắc
            long pt = System.currentTimeMillis() + 1000 - countdown;
            int addKey = (int) (pt / CfgTower.getTimeMinuteReceiveKey());
            int realAdd = numberKey + addKey > CfgTower.config.maxKey ? CfgTower.config.maxKey - numberKey : addKey;
            if (realAdd > 0) {
                int numberKey = this.numberKey + realAdd;
                long countdown = System.currentTimeMillis();
                if (numberKey >= CfgTower.config.maxKey) {
                    countdown = -1;
                }
                updateKey(countdown, numberKey);
            }
        }
        int curDate = DateTime.getNumberDay();
        if (eventId != curDate && update(Arrays.asList("number_buy", 0, "event_id", curDate))) {
            this.numberBuy = 0;
            this.eventId = curDate;
        }
    }


    public boolean update(List<Object> updateData) {
        return DBJPA.update("user_tower", updateData, Arrays.asList("user_id", userId));
    }

    public void updateWin() {
        if (update(Arrays.asList("level", level + 1, "last_attack", System.currentTimeMillis()))) {
            level++;
            this.lastAttack = System.currentTimeMillis();
        }
    }

    public boolean updateKey(long timeKey, int numberKey) {
        if (update(Arrays.asList("countdown", timeKey, "number_key", numberKey))) {
            this.countdown = timeKey;
            this.numberKey = numberKey;
            return true;
        }
        return false;
    }

    public boolean minusKey(int numMinus) {
        if (numberKey == CfgTower.config.maxKey) { // update CD
            if (update(Arrays.asList("number_key", numberKey - numMinus, "countdown", Calendar.getInstance().getTimeInMillis()))) {
                this.numberKey = numberKey - numMinus;
                this.countdown = Calendar.getInstance().getTimeInMillis();
                return true;
            } else return false;
        } else { // không update CD
            if (update(Arrays.asList("number_key", numberKey - numMinus))) {
                this.numberKey = numberKey - numMinus;
                return true;
            } else return false;
        }
    }
}
