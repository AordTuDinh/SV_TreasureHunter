package game.dragonhero.mapping.cms;


import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.helper.DateTime;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Data
@NoArgsConstructor
@Table(name = "cms.rr")
public class RetentionRateEntity implements Serializable {
    @Id
    int id;
    int serverId, nru, rr_1, rr_2, rr_3, rr_4, rr_5, rr_6, rr_7, rr_8, rr_9, rr_10, rr_11, rr_12, rr_13, rr_14, rr_15;
    int rr_16, rr_17, rr_18, rr_19, rr_20, rr_21, rr_22, rr_23, rr_24, rr_25, rr_26, rr_27, rr_28, rr_29, rr_30;


    public RetentionRateEntity(int serverId) {
        this.serverId = serverId;
    }

    public String sqlSave() {
        return String.format("INSERT INTO cms.rr (date_created,server_id ,nru,  rr_1, rr_2, rr_3, rr_4, rr_5, rr_6, rr_7, rr_8, rr_9, rr_10, rr_11, rr_12, rr_13, rr_14, rr_15," +
                        "rr_16, rr_17, rr_18, rr_19, rr_20, rr_21, rr_22, rr_23, rr_24, rr_25, rr_26, rr_27, rr_28, rr_29, rr_30 ) VALUES('%s',%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s," +
                        "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)",
                DateTime.getDateyyyyMMdd(), serverId, nru, rr_1, rr_2, rr_3, rr_4, rr_5, rr_6, rr_7, rr_8, rr_9, rr_10, rr_11, rr_12, rr_13, rr_14, rr_15,
                rr_16, rr_17, rr_18, rr_19, rr_20, rr_21, rr_22, rr_23, rr_24, rr_25, rr_26, rr_27, rr_28, rr_29, rr_30);
    }
}
