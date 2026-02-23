package game.dragonhero.mapping;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Calendar;
import java.util.Date;

@Entity
@Table(name = "clan_req")
@Data
public class ClanReqEntity implements java.io.Serializable {

    @Id
    private int clanId, userId;
    private Date dateCreated;

    public ClanReqEntity() {
    }

    public ClanReqEntity(int clanId, int userId) {
        this.clanId = clanId;
        this.userId = userId;
        this.dateCreated = Calendar.getInstance().getTime();
    }

}
