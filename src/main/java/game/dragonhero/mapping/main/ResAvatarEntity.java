package game.dragonhero.mapping.main;

import game.config.aEnum.AvatarIndex;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.List;

@Entity
@NoArgsConstructor
public class ResAvatarEntity {
    @Getter
    @Id
    int id;
    int type;
    String conditions, point;
    @Getter
    String desc;


    public AvatarIndex getType() {
        return AvatarIndex.get(type);
    }

    public List<Integer> getConditions() {
        return GsonUtil.strToListInt(conditions);
    }

}
