package game.dragonhero.mapping.main;

import lombok.Getter;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@NoArgsConstructor
@Entity
public class ResMapBossEntity extends BaseMap implements Serializable {
    String enemy, bonus;
    @Transient
    @Getter
    List<Integer> listEnemy = new ArrayList<>();

    public void init() {
        listEnemy = GsonUtil.strToListInt(enemy);
        checkJson(id, enemy);
        super.init();
    }
}
