package game.treasure.mapping.main;

import game.battle.object.Point;
import game.treasure.BattleConfig;
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
public class ResMobEntity extends BaseEntity implements Serializable {
    @Getter
    @Id
    int id;
    @Getter
    String name;
    @Getter
    int hp, atk, def,moveSpeed;
    @Getter
    float rangeAttack;
    String data;
    @Getter
    @Transient
    List<BonusConfig> bonus;

    public void init() {
        checkJson(id, data);
        bonus = BonusConfig.parseBonus(data);
    }

    public Point getPoint() {
        Point point = new Point();
        point.setMoveSpeed(moveSpeed);
        point.setBaseAttack(atk);
        point.setBaseHp(hp);
        point.setBaseDef(def);
        point.calculatorPower();
        return point;
    }
}
