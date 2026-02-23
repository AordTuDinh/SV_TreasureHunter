package game.dragonhero.mapping.cms;


import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

@Data
@Entity
@NoArgsConstructor
@Table(name = "cms.ccu")
public class CcuEntity implements Serializable {
    @Id
    int serverId;
    Date dateCreated;
    String hours;
    int online; // time current seconds

    public String sqlSave() {
        return String.format("INSERT INTO cms.ccu (date_created,server_id ,hours,online,time) VALUES('%s',%s,'%s',%s,%s) ON DUPLICATE KEY UPDATE number=values(number)",
                dateCreated, serverId, hours, online, Calendar.getInstance().getTimeInMillis() / 1000);
    }
}
