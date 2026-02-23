package game.object;

import game.dragonhero.mapping.UserEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class UserChatInfoObject implements Serializable {
    int id;
    String name;
    List<Integer> avatar;
    int level;

    public UserChatInfoObject(UserEntity user) {
        this.id = user.getId();
        this.name = user.getName();
        this.avatar = user.getAvatar();
        this.level = user.getLevel();
    }
}
