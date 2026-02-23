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
@Table(name = "cms.log_buy_iap")
@Builder
public class LogBuyIAPEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;
    int userId, packId;
    int status;
    int price, serverId;
    String orderId;
    String descc;
    long timeBuy;// time ms
    Date dateCreated;
    @Transient
    int number;


    public void save() {
        if(serverId<0) return;
        this.timeBuy = Calendar.getInstance().getTimeInMillis();
        this.dateCreated = Calendar.getInstance().getTime();
        DBJPA.save(this);
    }
}
