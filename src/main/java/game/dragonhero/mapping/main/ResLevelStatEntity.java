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
public class ResLevelStatEntity extends BaseEntity {
    @Id
    int id;
    String condition;
    float pointPerLevel;
    int levelMax, pointId, icon;
    @Transient
    List<Integer> listConditions;

    public void init() {
        listConditions = new ArrayList<>();
        if (!StringHelper.isEmpty(condition)) {
            listConditions = GsonUtil.strToListInt(condition);
        }
        checkJson(id, condition);
    }

    public static void main(String[] args) {
        int sum = 0;
        List<Integer> xPer = List.of(5, 5, 5, 50, 50, 30, 24, 80, 80, 24, 15, 70, 70);
        List<Integer> lst = List.of(140, 140, 140, 50, 50, 100, 100, 100, 100, 100, 100, 100, 100);
        for (int i = 0; i < 1; i++) {
            for (int j = 1; j <= lst.get(i); j++) {
                sum += j * xPer.get(i);
                if (sum > 2200) {
                    System.out.println("j = " + j);
                    break;
                }
            }
        }
        System.out.println("sum = " + sum);

    }

    public boolean hasCondition() {
        List<Integer> lst = GsonUtil.strToListInt(condition);
        return lst.size() != 0 && lst.get(0) != 0;
    }

    public int getConditionKey() {
        return GsonUtil.strToListInt(condition).get(0);
    }

    public int getConditionLevel() {
        return GsonUtil.strToListInt(condition).get(1);
    }


}
