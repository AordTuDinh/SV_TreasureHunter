package game.dragonhero.mapping.main;


import game.config.aEnum.ItemFarmType;
import game.dragonhero.service.user.Bonus;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
public class ResFarmQuestRequireEntity extends BaseEntity implements Serializable {
    @Id
    @Getter
    int level;
    String dataTree, rate, treeNum;

    @Transient
    List<List<Integer>> data;
    @Transient
    List<Integer> rates;
    @Transient
    List<Integer> treeRange;


    public void init() {
        data = GsonUtil.strTo2ListInt(dataTree);
        rates = GsonUtil.strToListInt(rate);
        treeRange = GsonUtil.strToListInt(treeNum);
        checkJson(level, dataTree);
        checkJson(level, rate);
        checkJson(level, treeNum);
    }


    // random theo rate -> data tree-> random data_tree(list id cây) ra id tree -> random tree_num để ra bonus
    public List<Long> getRequire(int numberFee) { // random yêu cầu nhiệm vụ
        List<Long> ret = new ArrayList<>();
        int numBonus = numberFee;
        List<Integer> hasData = new ArrayList<>();
        while (numBonus > 0) {
            int rand = NumberUtil.getRandom(100);
            for (int i = 0; i < rates.size(); i++) {
                if (rand < rates.get(i)) {
                    ret.addAll(getBonusRequire(data.get(i), hasData));
                    numBonus--;
                }
            }
        }
        return ret;
    }

    // random theo rate -> data tree-> random data_tree(list id cây) ra id tree -> random tree_num để ra bonus
    private List<Long> getBonusRequire(List<Integer> ids, List<Integer> hasData) {
        int idRand = getIdBonus(ids);
        while (hasData.contains(idRand)) {
            idRand = getIdBonus(ids);
        }
        int num = getRandTreeRank();
        hasData.add(idRand);
        return Bonus.viewItemFarm(ItemFarmType.AGRI, idRand, -num);
    }

    public int getIdBonus(List<Integer> ids) {
        return ids.get(NumberUtil.getRandom(ids.size()));
    }

    public int getRandTreeRank() {
        return NumberUtil.getRandom(treeRange.get(0), treeRange.get(1));
    }

}
