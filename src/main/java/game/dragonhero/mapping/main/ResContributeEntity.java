package game.dragonhero.mapping.main;


import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
public class ResContributeEntity implements Serializable {
    @Id
    int level;
    int gold;
    String data, bonus;


    public List<Integer> getData() {
        return GsonUtil.strToListInt(data); // num quest - per up
    }

    public List<Long> getBonus() {
        return GsonUtil.strToListLong(bonus);
    }
}
