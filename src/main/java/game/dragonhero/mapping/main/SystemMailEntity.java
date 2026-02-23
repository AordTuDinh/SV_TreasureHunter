package game.dragonhero.mapping.main;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table(name = "system_mail")
@NoArgsConstructor
@Data
public class SystemMailEntity implements java.io.Serializable {
    @Id
    private int id;
    private int serverId;
    private String title;
    private String message;
    private String bonus;
    private int mailType, isEnabled,  fromVip, toVip;
    private Date dateStart, dateEnd, dateCreated;

}
