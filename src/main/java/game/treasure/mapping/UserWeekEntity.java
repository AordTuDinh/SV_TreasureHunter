package game.treasure.mapping;


import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.DateTime;

import javax.persistence.*;

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
    /** Số trận thắng arena trong tuần. Cần cột DB {@code arena_win}. */
    int arenaWin;

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
        arenaWin = 0;
        if (update) DBJPA.update(this);
    }

    public void addDameBoss(int numAdd){
        killBoss+=numAdd;
        DBJPA.update(this);
    }

    public void addArenaWin(int numAdd) {
        arenaWin += numAdd;
        DBJPA.update(this);
    }
}
