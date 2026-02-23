package game.dragonhero.mapping.main;


import game.config.CfgServer;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

@NoArgsConstructor
@Data
@Entity
@Table(name = "dson_main.cache_user_buy_ruby")
public class CacheUserBuyRubyEntity {
    @Id
    int userId;
    int ruby,vipExp,receive;
    Date dateGet;

    public boolean getBonus() {
        this.receive = 1;
        this.dateGet = Calendar.getInstance().getTime();
        return DBJPA.update(this);
    }
}
