package game.dragonhero.mapping.cms;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;


@Data
@Entity
@NoArgsConstructor
@Table(name = "money")
public class MoneyEntity implements Serializable {
    @Id
    int serverId;
    @Id
    Date dateCreated;
    long number;

    public String sqlSave() {
        return String.format("INSERT INTO cms.money (date_created,server_id ,number) VALUES('%s',%s,%s) ON DUPLICATE KEY UPDATE number=values(number)",
                dateCreated, serverId, number);
    }
}
