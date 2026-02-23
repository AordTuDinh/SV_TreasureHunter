package game.dragonhero.mapping.main;

import game.config.CfgFarmQuest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.List;

@Entity
@NoArgsConstructor
public class ResVipEntity extends BaseEntity implements Serializable {
    @Getter
    @Id
    int vip;
    String bonus;
    @Getter
    int exp;
    int farmQuest;
    @Getter
    @Transient
    List<Long> aBonus;

    public void init() {
        aBonus = GsonUtil.strToListLong(bonus);
        checkJson(vip, bonus);
    }

    public int getFarmQuest(int curLevel) {
        return farmQuest + CfgFarmQuest.getQuestFree(curLevel);
    }
}
