package game.dragonhero.mapping.main;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import game.battle.effect.SkillEffect;
import game.battle.effect.SkillObject;
import game.config.aEnum.FactionType;
import game.config.aEnum.RankType;
import game.monitor.Telegram;
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
    SkillObject skill;
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
        if (data != null && !data.isEmpty()) {
            skill = new Gson().fromJson(data, new TypeToken<SkillObject>() {
            }.getType());
        }
        range = GsonUtil.strToListFloat(rangeFly);
        upSkill = GsonUtil.strToListInt(upLevel);
        passives = new ArrayList<>();
        List<Float> lst = GsonUtil.strToListFloat(passive);
        for (int i = 0; i < lst.size(); i += 3) {
            PassiveWeapon ps = new PassiveWeapon(lst.get(i), lst.get(i + 1), lst.get(i + 2));
            passives.add(ps);
        }
//        if (faction == 0) Telegram.sendNotify("ERR res_weapon thiếu faction id " + id);
    }

    public List<Integer> getShots() {
        return GsonUtil.strToListInt(baseShot);
    }

    public RankType getRankType() {
        return RankType.get(rank);
    }

    //Fixme DATE: 8/18/2022 LƯU Ý ---> Dùng cho monster thôi
    public float getRangeFly() {
        return range.get(0);
    }

    public SkillEffect getSkillEffect(int level) {
        return new SkillEffect(this, level);
    }
}
