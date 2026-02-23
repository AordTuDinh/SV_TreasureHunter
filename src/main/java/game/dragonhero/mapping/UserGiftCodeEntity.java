package game.dragonhero.mapping;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Data
@Entity
@NoArgsConstructor
@Table(name = "user_gift_code")
public class UserGiftCodeEntity implements Serializable {
    @Id
    int userId;
    @Id
    String gift;
    int eventGift;

    public UserGiftCodeEntity(int userId, String gift,int eventGift) {
        this.userId = userId;
        this.gift = gift;
        this.eventGift = eventGift;
    }
}
