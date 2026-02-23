package game.dragonhero.mapping.main;

import game.config.aEnum.FactionType;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class ResHeroEntity implements Serializable {
    @Id
    int heroId;
    int faction;
    String name, desc, point;
    boolean enable;

    public FactionType getFaction() {
        return FactionType.get(faction);
    }

    public List<Long> getPoint() {
        return GsonUtil.strToListLong(point);
    }
}
