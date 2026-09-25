package game.object;

import game.treasure.mapping.UserEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class UserChatInfoObject implements Serializable {
    int id;
    String name;
    List<Integer> itemEquips;
    int level;

    public UserChatInfoObject(UserEntity user) {
        this.id = user.getId();
        this.name = user.getName();
        this.itemEquips = user.getAllInfoItemEquip();
        this.level = 1;
    }
}
