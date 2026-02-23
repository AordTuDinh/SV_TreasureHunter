package game.dragonhero.mapping.main;


import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "res_farm_deco")
public class ResFarmDecoEntity implements Serializable {
    @Id
    int id;
    String price, data;
    int number;// số lượng có thể mua


    public List<Float> getData() {
        return GsonUtil.strToListFloat(data);
    }

    public List<Long> getPrice() {
        return GsonUtil.strToListLong(price);
    }
}
