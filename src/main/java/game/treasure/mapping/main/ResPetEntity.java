package game.treasure.mapping.main;

import game.object.PointRandomConfig;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.StringHelper;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
public class ResPetEntity implements Serializable {
    @Id
    int id;
    String name;
    int showSummon; // showSummon: có thể summon ra
    float rare, epic, legends;
    String data;
    int hh1, hh2, hh3;
    @Transient
    List<PointRandomConfig> aPoint;

    public void init() {
        aPoint = PointRandomConfig.parsePoint(data);
    }

    public String getPointData(int tier) {
        List<Float> ret = PointRandomConfig.getRandomBonusMulti(aPoint);
        float valueTier = getValueTier(tier);
        if (valueTier > 1) {
            for (int i = 0; i < ret.size(); i += 2) {
                ret.set(i + 1, ret.get(i + 1) * valueTier);
            }
        }
        return StringHelper.toDBString(ret);
    }

    public float getValueTier(int tier) {
        if (tier == 2) return rare;
        if (tier == 3) return epic;
        if (tier == 4) return legends;
        return 1;
    }

    public int getTransformIcon(int tier) {
        return switch (tier) {
            case 1 -> hh1;
            case 2 -> hh2;
            case 3 -> hh3;
            default -> 0;
        };
    }
}
