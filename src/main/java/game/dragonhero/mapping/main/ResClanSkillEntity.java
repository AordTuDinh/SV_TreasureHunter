package game.dragonhero.mapping.main;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import game.config.aEnum.ItemKey;
import game.dragonhero.service.user.Bonus;
import game.object.PointData;
import lombok.Data;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class ResClanSkillEntity extends BaseEntity implements Serializable {
    @Id
    int id;
    int group, parentId, levelUnlocked;
    String goldUpgrade, coinUpgrade, skillData;
    @Transient
    List<Long> feeGold;
    @Transient
    List<Long> feeCoin;
    @Transient
    int maxLevel = 20;
    @Transient
    List<PointData[]> aEffect = new ArrayList<>();

    public void init() {
        feeGold = GsonUtil.strToListLong(goldUpgrade);
        feeCoin = GsonUtil.strToListLong(coinUpgrade);
        maxLevel = feeGold.size();
        checkJson(id, goldUpgrade);
        checkJson(id, coinUpgrade);
        checkJson(id, skillData);
        aEffect = new Gson().fromJson(skillData, new TypeToken<List<PointData[]>>() {
        }.getType());
    }

    public PointData[] getPointEffect(int index) {
        return aEffect.get(index);
    }

    public List<Long> getFee(int curLevel, int number) {
        List<Long> fee = new ArrayList<>();
        int gold = 0, mat = 0;
        for (int i = curLevel; i < curLevel + number; i++) {
            gold += feeGold.get(i);
            mat += feeCoin.get(i);
        }
        fee.addAll(Bonus.viewGold(-gold));
        fee.addAll(Bonus.viewItem(ItemKey.HUY_HIEU_BANG, -mat));
        return fee;
    }

    public Long getGoldReset(int curLevel) {
        Long gold = 0L;
        for (int i = 0; i <= curLevel; i++) {
            gold += feeGold.get(i);
        }
        return gold;
    }


    public Long getCoinReset(int curLevel) {
        Long coin = 0L;
        for (int i = 0; i <= curLevel; i++) {
            coin += feeCoin.get(i);
        }
        return coin;
    }
}
