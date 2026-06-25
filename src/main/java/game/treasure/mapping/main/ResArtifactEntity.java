package game.treasure.mapping.main;

import game.battle.calculate.IMath;
import game.config.CfgItem;
import game.config.aEnum.ArtifactType;
import game.object.PointRandomConfig;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.StringHelper;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class ResArtifactEntity {
    @Id
    int id;
    String name, desc, data;
    int rank, type;

    @Column(name = "point_main")
    int pointMain;

    float rare, epic, legends;

    @Transient
    List<PointRandomConfig> aPoint;

    public ArtifactType getArtifactType() {
        return ArtifactType.get(type);
    }

    public void init() {
        if (data == null || data.isBlank() || "[]".equals(data)) {
            aPoint = new ArrayList<>();
            return;
        }
        aPoint = PointRandomConfig.parsePoint(data);
        if (aPoint == null)
            aPoint = new ArrayList<>();
    }

    void ensureInit() {
        if (aPoint == null)
            init();
    }

    public float getValueTier(int tier) {
        return switch (tier) {
            case 2 -> rare > 0 ? rare : 1f;
            case 3 -> epic > 0 ? epic : 1f;
            case 4 -> legends > 0 ? legends : 1f;
            default -> 1f;
        };
    }

    /** Roll base [time, cd, value, range, person] — tier nhân time & value; cd/range/person giữ nguyên. */
    public String getRollData(int tier) {
        ensureInit();
        float time = 0, cd = 0, value = 0, range = 0, person = 0;
        for (PointRandomConfig cfg : aPoint) {
            if (cfg == null)
                continue;
            float rolled = cfg.max == 1f ? 1f : NumberUtil.getRandom(cfg.min, cfg.max);
            if (cfg.rate != -1) {
                int rand = NumberUtil.getRandom(1000);
                if (rand >= cfg.rate)
                    continue;
            }
            if (cfg.pointId == -1)
                time = rolled;
            else if (cfg.pointId == -2)
                cd = rolled;
            else if (cfg.pointId == -3)
                range = rolled;
            else if (cfg.pointId == -4)
                person = rolled;
            else if (cfg.pointId == pointMain)
                value = rolled;
        }
        float tierMult = getValueTier(tier);
        time = IMath.round1(time * tierMult);
        value = CfgItem.formatPointStat(pointMain, value * tierMult);
        cd = Math.round(cd);
        range = IMath.round1(range);
        person = IMath.round1(person);
        return StringHelper.toDBString(List.of(time, cd, value, range, person));
    }
}
