package game.dragonhero.mapping;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;


@Data
@Entity
@Table(name = "user_buy_qr")
@NoArgsConstructor
public class UserBuyQrEntity implements Serializable {
    @Id
    int userId, packId, code;
    int status;

}
