package game.treasure.mapping;

import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.DateTime;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@Table(name = "user_trading")
public class UserTradingEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;
    int userId;
    int server;
    int tab;
    int itemType;
    long itemId;
    String itemInfo;
    int price;
    long verifyUntil;
    @Temporal(TemporalType.TIMESTAMP)
    Date dateCreated;

    public UserTradingEntity initDefault() {
        if (dateCreated == null)
            dateCreated = new Date();
        return this;
    }

    public boolean isWaiting() {
        return verifyUntil > System.currentTimeMillis();
    }

    public boolean update(List<Object> lst) {
        return DBJPA.update("user_trading", lst, Arrays.asList("id", id));
    }

    public boolean deleteFromDb() {
        return DBJPA.delete("user_trading", "id", id);
    }

    public static UserTradingEntity insert(UserTradingEntity row) {
        row.initDefault();
        if (!DBJPA.save(row))
            return null;
        return row;
    }

    public static List<UserTradingEntity> loadAll() {
        return DBJPA.getList("user_trading", UserTradingEntity.class);
    }

    public static UserTradingEntity getById(long tradingId) {
        return (UserTradingEntity) DBJPA.getUnique("user_trading", UserTradingEntity.class, "id", tradingId);
    }
}
