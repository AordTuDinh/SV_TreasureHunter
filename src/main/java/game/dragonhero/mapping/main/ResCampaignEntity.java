package game.dragonhero.mapping.main;

import lombok.Getter;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@Entity
@NoArgsConstructor
public class ResCampaignEntity extends BaseMap implements Serializable {
    String enemy, bonus;
    @Getter
    Long minPower;
    @Getter
    int conquer;
    @Transient
    @Getter
    List<Integer> listEnemy = new ArrayList<>();

    @Transient
    @Getter
    List<Integer> listEnemyIds = new ArrayList<>();

    public void init() {
        checkJson(id, enemy);
        checkJson(id, bonus);
        listEnemy = GsonUtil.strToListInt(enemy);
        for (int i = 0; i < listEnemy.size(); i+=2) {
            listEnemyIds.add(listEnemy.get(i));
        }
        aBonus = GsonUtil.strToListLong(bonus);
        super.init();
    }
}
