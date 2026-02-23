package game.dragonhero.mapping.main;

import lombok.Getter;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.util.List;


@NoArgsConstructor
@Entity
public class ResQuestBEntity extends BaseEntity {
    @Getter
    @Id
    int id;
    @Getter
    int number;
    String name, bonus, bonusVip;
    @Transient
    @Getter
    List<Long> aBonus, aBonusVip;

    public void init() {
        aBonus = GsonUtil.strToListLong(bonus);
        aBonusVip = GsonUtil.strToListLong(bonusVip);
        checkJson(id, bonus);
        checkJson(id, bonusVip);
    }

}
