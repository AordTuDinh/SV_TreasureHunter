package game.dragonhero.mapping.cms;


import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.helper.DateTime;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Data
@Entity
@NoArgsConstructor
@Table(name = "cms.quit_7")
public class Quit7Entity implements Serializable {
    @Id
    int serverId;

    @Id
    String os;
    int number;

    public String sqlSave() {
        return String.format("INSERT INTO cms.quit_7 (date_created,server_id ,os,number) VALUES('%s',%s,'%s',%s) ON DUPLICATE KEY UPDATE number=values(number)",
                DateTime.getDateyyyyMMdd(), serverId, os, number);
    }
}
