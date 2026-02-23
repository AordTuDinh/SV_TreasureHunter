package game.dragonhero.mapping;


import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

@Data
@Entity
@NoArgsConstructor
@Table(name = "user_gift")
public class UserGiftEntity implements Serializable {
    @Id
    int userId, fromId;
    Date timeSend;

    public UserGiftEntity(int targetId, int myId) {
        this.userId = targetId;
        this.fromId = myId;
        this.timeSend = Calendar.getInstance().getTime();
    }

    public boolean update() {
        return DBJPA.saveOrUpdate(this);
    }
}
