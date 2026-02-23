package game.dragonhero.mapping.main;


import game.config.aEnum.ItemFarmType;
import game.dragonhero.service.user.Bonus;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
public class ResItemFoodEntity extends AbstractItemFarm implements Serializable {
    String data, material;
    int feeGold;


    public List<List<Long>> getMaterial() {
        return Bonus.parse(GsonUtil.strToListLong(material));
    }

    public List<Long> getFood(int number) {
        return Bonus.viewItemFarm(ItemFarmType.FOOD, id, number);
    }
}
