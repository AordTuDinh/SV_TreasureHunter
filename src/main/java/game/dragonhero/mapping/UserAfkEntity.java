package game.dragonhero.mapping;


import game.config.CfgAfk;
import game.dragonhero.service.user.Bonus;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

@Entity
@NoArgsConstructor
@Table(name = "user_afk")
@Data
public class UserAfkEntity {
    @Id
    int userId;
    long timeGetBonus, timeCheckBonus;
    int timeFullBonus, perBonus;
    String bonus;

    public UserAfkEntity(int userId) {
        this.userId = userId;
        this.timeGetBonus = Calendar.getInstance().getTimeInMillis();
        this.timeCheckBonus = Calendar.getInstance().getTimeInMillis();
        this.bonus = "[]";
        this.timeFullBonus = CfgAfk.config.timeMax;
        this.perBonus = 100;
    }

    public List<Long> getBonus() {
        return GsonUtil.strToListLong(bonus);
    }

    public boolean update(List<Object> updateData) {
        return DBJPA.update("user_afk", updateData, Arrays.asList("user_id", userId));
    }

    public boolean updateTimeFullBonus(int addTime) {
        if (update(Arrays.asList("time_full_bonus", timeFullBonus + addTime))) {
            timeFullBonus += addTime;
            return true;
        }
        return false;
    }

    public boolean updateTimePerBonus(int addTime, int addPer) {
        if (update(Arrays.asList("time_full_bonus", timeFullBonus + addTime, "per_bonus", perBonus + addPer))) {
            timeFullBonus += addTime;
            perBonus += addPer;
            return true;
        }
        return false;
    }


    public boolean updatePerBonus(int addPer) {
        if (update(Arrays.asList("per_bonus", perBonus + addPer))) {
            perBonus += addPer;
            return true;
        }
        return false;
    }

    public boolean resetBonus() {
        long curTimeCheck = Calendar.getInstance().getTimeInMillis();
        if (update(Arrays.asList("time_check_bonus", curTimeCheck, "time_get_bonus", curTimeCheck, "bonus", "[]"))) {
            this.timeGetBonus = curTimeCheck;
            this.timeCheckBonus = curTimeCheck;
            this.bonus = "[]";
            return true;
        }
        return false;
    }
}
