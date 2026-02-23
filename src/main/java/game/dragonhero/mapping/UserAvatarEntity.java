package game.dragonhero.mapping;

import game.config.aEnum.AvatarIndex;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "user_avatar")
@NoArgsConstructor
@Data
public class UserAvatarEntity implements Serializable {
    @Id
    private int userId, avatarId, typeId;


    public UserAvatarEntity(int userId, int avatarId, int type) {
        this.userId = userId;
        this.avatarId = avatarId;
        this.typeId = type;
    }
}