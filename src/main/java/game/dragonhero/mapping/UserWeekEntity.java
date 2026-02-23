package game.dragonhero.mapping;


import game.config.CfgEvent;
import game.config.aEnum.StatusType;
import game.object.DataDaily;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.StringHelper;

import javax.persistence.*;
import java.util.List;

@Entity
@NoArgsConstructor
@Table(name = "user_week")
@Data
public class UserWeekEntity {
    @Id
    int userId;
    int weekId;
    int server;
    int killBoss;

    public UserWeekEntity(int userId, int server) {
        this.userId = userId;
        this.server = server;
        genNewData(DateTime.getNumberWeek(), false);
    }

    public void checkData() {
        int weekOfYeah = DateTime.getNumberWeek();
        if (weekOfYeah != weekId) {
            genNewData(weekOfYeah, true);
        }
    }

    void genNewData(int weekOfYeah, boolean update) {
        weekId = weekOfYeah;
        killBoss = 0;
        if (update) DBJPA.update(this);
    }

    public void addDameBoss(int numAdd){
        killBoss+=numAdd;
        DBJPA.update(this);
    }
}
