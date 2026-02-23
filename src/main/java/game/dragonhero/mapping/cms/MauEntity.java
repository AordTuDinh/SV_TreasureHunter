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
@Table(name = "cms.mau")
public class MauEntity implements Serializable {
    @Id
    int serverId;
    Date dateCreated;
    String os;
    int number;
}
