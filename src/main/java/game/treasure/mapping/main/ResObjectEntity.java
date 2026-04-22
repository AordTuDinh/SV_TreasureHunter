package game.treasure.mapping.main;

import game.object.BonusConfig;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.List;

@NoArgsConstructor
@Entity
public class ResObjectEntity extends BaseEntity implements Serializable {
    @Getter
    @Id
    int id;
    @Getter
    String name;
    @Getter
    int hp;
    String drop;
    @Getter
    @Transient
    List<BonusConfig> bonus;

    public void init() {
        checkJson(id, drop);
        bonus = BonusConfig.parseBonus(drop);
    }
}

