package game.dragonhero.mapping.main;


import game.battle.object.EnemyWave;
import game.object.BonusConfig;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;


@Entity
@NoArgsConstructor
public class ResTowerEntity extends BaseMap {
    String enemy, bonus, bonusSmart;
    @Getter
    float timeNextWave;

    @Transient
    List<BonusConfig> aBonus, aBonusSmart;
    @Getter
    @Transient
    List<EnemyWave> aEnemy = new ArrayList<>();


    @Override
    public void init() {
        super.init();
        aBonus = GsonUtil.strToListBonusConfig(bonus);
        aBonusSmart = GsonUtil.strToListBonusConfig(bonusSmart);
        checkJson(id, bonus);
        checkJson(id, bonusSmart);
        checkJson(id, enemy);
        List<List<Integer>> dataEnemy = GsonUtil.strTo2ListInt(enemy);
        for (int i = 0; i < dataEnemy.size(); i++) {
            aEnemy.add(new EnemyWave(dataEnemy.get(i)));
        }
    }

    public List<Long> getBonus() {
        return BonusConfig.getRandomBonus(aBonus);
    }

    public List<Long> getABonusSmart() {
        return BonusConfig.getRandomBonusMulti(aBonusSmart);
    }
}
