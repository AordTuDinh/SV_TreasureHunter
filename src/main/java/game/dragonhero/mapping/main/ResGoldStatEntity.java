package game.dragonhero.mapping.main;


import lombok.Data;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.StringHelper;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class ResGoldStatEntity extends BaseEntity {
    @Id
    int id;
    String condition, formula;
    int levelMax, pointId, icon;
    float pointPerLevel;

    @Transient
    long goldStartLong;
    @Transient
    List<Integer> listConditions;
    @Transient
    List<Float> aFormular;

    public void init() {
        listConditions = new ArrayList<>();
        if (!StringHelper.isEmpty(condition)) {
            listConditions = GsonUtil.strToListInt(condition);
        }
        aFormular = GsonUtil.strToListFloat(formula);
        checkJson(id, condition);
        checkJson(id, formula);
    }

    public boolean hasCondition() {
        List<Integer> lst = GsonUtil.strToListInt(condition);
        return lst.size() > 1 && lst.get(0) != 0;
    }

    public int getConditionKey() {
        return GsonUtil.strToListInt(condition).get(0);
    }

    public int getConditionLevel() {
        return GsonUtil.strToListInt(condition).get(1);
    }

}
