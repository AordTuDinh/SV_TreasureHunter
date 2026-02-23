package game.dragonhero.mapping;

import game.config.CfgLuckySpine;
import game.config.aEnum.StatusType;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.StringHelper;

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
@Table(name = "user_lucky_spine")
public class UserLuckySpineEntity implements Serializable {
    @Id
    int userId;
    int eventId;
    String bonusNormal, statusNormal;
    long timeFreeRefresh;

    public UserLuckySpineEntity(int userId) {
        this.userId = userId;
    }

    public void checkRefreshNormal(UserEntity user) {
        int curDay = DateTime.getNumberDay();
        if (curDay != eventId) {
            updateResetNormal(user, false); // nó là force nên k phải free
        }
    }

    public boolean checkFreeRefresh() {
        return System.currentTimeMillis() - timeFreeRefresh >= CfgLuckySpine.config.timeFreeRefresh * DateTime.HOUR_MILLI_SECOND;
    }

    public List<Integer> getStatusNormal() {
        return GsonUtil.strToListInt(statusNormal);
    }

    public long casinoCountdown() {
        return DateTime.secondsUntilEndDay();
    }

    public boolean updateResetNormal(UserEntity user, boolean free) {
        String status = NumberUtil.genListStringInt(CfgLuckySpine.rateRotate.size(), StatusType.PROCESSING.value);
        int curDay = DateTime.getNumberDay();
        String bonus = StringHelper.toDBString(CfgLuckySpine.getSpineBonusShow(user.getLevel()));
        if (free) {
            if (DBJPA.update("user_lucky_spine", Arrays.asList("bonus_normal", bonus, "event_id", curDay, "status_normal", status, "time_free_refresh",
                    Calendar.getInstance().getTimeInMillis()), Arrays.asList("user_id", userId))) {
                this.bonusNormal = bonus;
                this.eventId = curDay;
                this.statusNormal = status;
                this.timeFreeRefresh = Calendar.getInstance().getTimeInMillis();
                return true;
            } else return false;
        } else {
            if (DBJPA.update("user_lucky_spine", Arrays.asList("bonus_normal", bonus, "event_id", curDay, "status_normal", status), Arrays.asList("user_id", userId))) {
                this.bonusNormal = bonus;
                this.eventId = curDay;
                this.statusNormal = status;
                return true;
            }
            return false;
        }
    }


    public boolean updateStatusNormal(String status) {
        if (DBJPA.update("user_lucky_spine", Arrays.asList("status_normal", status), Arrays.asList("user_id", userId))) {
            this.statusNormal = status;
            return true;
        }
        return false;
    }
}
