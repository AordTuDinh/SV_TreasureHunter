package game.object;

import game.treasure.mapping.UserEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class UserChatInfoObject implements Serializable {
    int id;
    String name;
    List<Integer> skins;
    int level;

    public UserChatInfoObject(UserEntity user) {
        this.id = user.getId();
        this.name = user.getName();
        this.skins = user.getSkins();
        this.level = 1;
    }
}
