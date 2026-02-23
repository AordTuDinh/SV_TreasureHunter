package game.dragonhero.mapping.cms;


import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
@Table(name = "cms.ccu_max")
@Entity
public class CcuMaxEntity implements Serializable {
    @Id
    int serverId;
    @Id
    Date dateCreated;
    int number;
    String hours;

    public String sqlSave() {
        return String.format("INSERT INTO cms.ccu_max (date_created,server_id ,hours,number) VALUES('%s',%s,'%s',%s) ON DUPLICATE KEY UPDATE number=values(number)",
                dateCreated, serverId, hours, number);
    }
}
