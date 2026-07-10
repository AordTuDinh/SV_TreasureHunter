package game.treasure.mapping.main;

import lombok.Getter;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.StringHelper;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
public class ResVipEntity extends BaseEntity implements Serializable {
    @Getter
    @Id
    int vip;
    String bonus, bonusDay, vipData;
    @Getter
    int exp;
    @Getter
    @Transient
    List<Long> aBonus;
    @Getter
    @Transient
    List<Long> aBonusDay;
    @Getter
    @Transient
    List<Integer> aVipData;

    public void init() {
        aBonus = GsonUtil.strToListLong(bonus);
        aBonusDay = GsonUtil.strToListLong(bonusDay);
        aVipData = StringHelper.isEmpty(vipData) ? new ArrayList<>() : GsonUtil.strToListInt(vipData);
        checkJson(vip, bonus);
        checkJson(vip, bonusDay);
        checkJson(vip, vipData);
    }

}
