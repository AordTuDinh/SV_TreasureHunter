package game.dragonhero.mapping.main;

import game.config.aEnum.MarketType;
import lombok.Data;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class ResMarketEntity extends BaseEntity implements Serializable {
    @Id
    int id;
    int enable, showNumber, type;
    long timeRefresh;
    String priceReset;

    public MarketType getMarketType() { // 1 trong 4 type bán item
        return MarketType.get(type);
    }

    public List<Long> getListPriceReset() {
        if (priceReset.equals("[0,0]")) return new ArrayList<>();
        return GsonUtil.strToListLong(priceReset);
    }
}
