package game.dragonhero.mapping.main;

import game.config.aEnum.FactionType;
import game.object.PassiveWeapon;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;


@NoArgsConstructor
@Entity
public class ResWeaponEntity extends BaseEntity {
    @Getter
    @Id
    int id;
    @Getter
    String name, desc, baseShot, upLevel, passive, rangeFly;
    String attackDamage, data;
    @Getter
    float radius;
    @Getter
    int rank, maxLevel, cooldown, forcePush, speed, degree;
    int faction;
    @Getter
    @Transient
    List<Integer> upSkill; // shot =số tia-số đạn mỗi tia-số lần xuyên
    @Getter
    @Transient
    List<Float> range;
    @Getter
    @Transient
    List<PassiveWeapon> passives;
    @Getter
    @Transient
    List<Integer> atkDame;
    @Getter
    @Transient
    FactionType factionType;


    public void init() {
        checkJson(id, attackDamage);
        checkJson(id, baseShot);
        checkJson(id, rangeFly);
        checkJson(id, upLevel);
        checkJson(id, passive);
        atkDame = GsonUtil.strToListInt(attackDamage);
        factionType = FactionType.get(faction);
        range = GsonUtil.strToListFloat(rangeFly);
        upSkill = GsonUtil.strToListInt(upLevel);
        passives = new ArrayList<>();
        List<Float> lst = GsonUtil.strToListFloat(passive);
        for (int i = 0; i < lst.size(); i += 3) {
            PassiveWeapon ps = new PassiveWeapon(lst.get(i), lst.get(i + 1), lst.get(i + 2));
            passives.add(ps);
        }
    }

    public List<Integer> getShots() {
        return GsonUtil.strToListInt(baseShot);
    }
}
