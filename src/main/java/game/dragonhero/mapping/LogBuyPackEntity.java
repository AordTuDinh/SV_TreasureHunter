package game.dragonhero.mapping;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cms.log_buy_pack")
@Builder
public class LogBuyPackEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;
    int userId, packId;
    int serverId;
    String price;
    long timeBuy;// time ms
    Date dateCreated;
    @Transient
    int number;


    public void saveLog() {
        if(serverId<0) return;
        this.timeBuy = Calendar.getInstance().getTimeInMillis();
        this.dateCreated = Calendar.getInstance().getTime();
        DBJPA.save(this);
    }
}
