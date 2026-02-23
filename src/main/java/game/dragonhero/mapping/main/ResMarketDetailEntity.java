package game.dragonhero.mapping.main;

import game.dragonhero.mapping.UserHeroEntity;
import game.dragonhero.service.user.Bonus;
import game.object.MyUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Calendar;
import java.util.List;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ResMarketDetailEntity extends BaseEntity implements Serializable {
    @Id
    int id;
    int marketId, show, stock, itemOrder;
    String price, item, day, require;
    float percent;
    @Transient
    List<Integer> daySell;
    @Transient
    List<Integer> requires;
    @Transient
    List<Long> priceItem;
    @Transient
    List<Long> items;

    public void init() {
        daySell = GsonUtil.strToListInt(day);
        priceItem = GsonUtil.strToListLong(price);
        priceItem.set(priceItem.size() - 1, -priceItem.get(priceItem.size() - 1));
        items = GsonUtil.strToListLong(item);
        requires = GsonUtil.strToListInt(require);
        checkJson(id, price);
        checkJson(id, item);
        checkJson(id, day);
        checkJson(id, require);
    }

    public boolean isShow() {
        return show == 1;
    }


    // check hero đã sở hữu
    public boolean isHasHero(MyUser mUser) {
        List<List<Long>> bms = Bonus.parse(items);
        for (int i = 0; i < bms.size(); i++) {
            List<Long> bonus = bms.get(i);
            if (bonus.get(0) == Bonus.BONUS_HERO) {
                return mUser.getResources().hasHero(Math.toIntExact(bonus.get(1)));
            }
        }
        return false;
    }

    public boolean hasBuy() {
        int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        if (daySell.isEmpty()) return true;
        else return daySell.contains(day);
    }
}
