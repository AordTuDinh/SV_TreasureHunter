package game.dragonhero.mapping.main;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import game.battle.effect.SkillEffect;
import game.battle.object.BossSkill;
import game.battle.object.Point;
import game.battle.object.Pos;
import game.battle.type.CharacterType;
import game.config.aEnum.FactionType;
import game.dragonhero.service.resource.ResEnemy;
import game.dragonhero.service.resource.ResSkill;
import game.dragonhero.service.user.Bonus;
import game.object.BonusConfig;
import game.object.BossSkillConfig;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
public class ResBossEntity extends BaseEntity {
    @Id
    int id;
    String name, hp, hpRegen, attack, magicAttack, reward, skill, pos;
    int type, model, level, defense, magic_resist, crit, critDamage, agility, immunity;
    float radius, rangeAttack;
    int weight, moveSpeed, faction, enemy;// trọng lượng enemy (từ 0 ->100) , -1 = never be push
    @Transient
    long longHp, longHpRegen, longAttack, longMagicAttack;
    @Transient
    BossSkillConfig bossSkillConfig;
    @Transient
    List<BonusConfig> aReward;
    @Transient
    CharacterType characterType;
    @Transient
    Pos instancePos;
    @Transient
    Point point;

    public void init() {
        if (skill != null && !skill.isEmpty()) {
            bossSkillConfig = new Gson().fromJson(skill, new TypeToken<BossSkillConfig>() {
            }.getType());
        }
        longHp = NumberUtil.castStr2Long(hp);
        longHpRegen = NumberUtil.castStr2Long(hpRegen);
        longAttack = NumberUtil.castStr2Long(attack);
        longMagicAttack = NumberUtil.castStr2Long(magicAttack);
        checkJson(id, reward);
        aReward = new Gson().fromJson(reward, new TypeToken<List<BonusConfig>>() {
        }.getType());
        characterType = CharacterType.get(type);
        instancePos = new Pos(GsonUtil.strToListFloat(pos));
        // point
        point = new Point();
        point.setBaseHp(longHp);
        point.setCurHp(longHp);
        point.setBaseHpRegen(longHpRegen);
        point.setBaseAttack(longAttack);
        point.setBaseMagicAttack(longMagicAttack);
        point.setDefense(defense);
        point.setMagicResist(magic_resist);
        point.setBaseCritChange(crit * 10L);
        point.setCritDamage(critDamage);
        point.setAgility(agility);
        point.setImmunity(immunity);
        point.setMoveSpeed(moveSpeed);
        point.setWeight(weight);
        point.calculatorPower(level,0);
    }

    public List<Long> getBonusKillBoss(List<Integer> perBonusAdd) {
        return BonusConfig.getRandomBonusBoss(aReward,perBonusAdd);
    }

    public FactionType getFaction() {
        return FactionType.get(faction);
    }

    public float getRangeAttack() {
        return rangeAttack / 10;
    }

    public Pos getInstancePos() {
        return instancePos.clone();
    }

    public List<BossSkill> getSkills() {
        List<BossSkill> skills = new ArrayList<>();
        for (int i = 0; i < bossSkillConfig.skills.size(); i++) {
            ResSkillEntity skill = ResSkill.getSkills(bossSkillConfig.skills.get(i));
            skills.add(new BossSkill(skill.getId(), new SkillEffect(skill)));
        }
        return skills;
    }

    public List<ResBossEntity> getSupport() {
        List<ResBossEntity> supports = new ArrayList<>();
        if (bossSkillConfig.support == null) return new ArrayList<>();
        for (int i = 0; i < bossSkillConfig.support.size(); i++) {
            ResBossEntity sp = ResEnemy.getBoss(bossSkillConfig.support.get(i));
            supports.add(sp);
        }
        return supports;
    }

    public Point toPoint() {
        return point.cloneInstance();
    }
}
