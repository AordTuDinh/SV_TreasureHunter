package game.battle.effect;

import game.battle.model.Character;
import game.config.aEnum.FactionType;
import game.dragonhero.mapping.main.ResSkillEntity;
import game.dragonhero.mapping.main.ResWeaponEntity;
import game.dragonhero.service.battle.EffectType;
import game.dragonhero.service.battle.TriggerType;
import lombok.Data;
import org.apache.commons.lang.SerializationUtils;
import ozudo.base.helper.DateTime;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class SkillEffect implements Serializable {
    long dameBuff;
    TriggerType triggerType;
    int mP;
    EffectType effectType;
    List<Float> values;
    float time, timeDelayDame;
    boolean hasEffect; // check có effect đi kèm hay k hay chỉ có mỗi dame thôi

    public SkillEffect(ResWeaponEntity resSkill, int curLevel) {
        int level = curLevel - 1;
        dameBuff = resSkill.getAtkDame().isEmpty() ? 0L : resSkill.getAtkDame().get(0) + resSkill.getAtkDame().get(1) * (level);

        SkillObject skill = resSkill.getSkill();
        if (skill != null) {
            timeDelayDame = skill.getDelayDame();
            hasEffect = true;
            values = new ArrayList<>();
            triggerType = skill.getTrigger();
            mP = skill.mp;
            effectType = skill.getEffect();
            for (int i = 0; i < skill.getValue().size(); i += 2) {
                values.add(skill.getPerIndex(i, level));
            }
            time = skill.getTimeByLevel(level);
        } else hasEffect = false;
    }


    public SkillEffect(SkillObject sOj, int curLevel) {
        SkillObject skill = sOj.clone();
        if (skill != null) {
            timeDelayDame = skill.getDelayDame();
            hasEffect = true;
            values = new ArrayList<>();
            triggerType = skill.getTrigger();
            mP = skill.mp;
            effectType = skill.getEffect();
            for (int i = 0; i < skill.getValue().size(); i += 2) {
                values.add(skill.getPerIndex(i, curLevel));
            }
            time = skill.getTimeByLevel(curLevel);
        } else hasEffect = false;
    }

    public SkillEffect() {
        this.timeDelayDame = 0;
        this.hasEffect = false;
    }

    public long getTimeMs() {
        return (long) (time * DateTime.SECOND2_MILLI_SECOND);
    }

    // Fixme : Custom skill
    public SkillEffect(EffectType eff, List<Float> values, float time, float timeDelayDame) {
        triggerType = TriggerType.NULL;
        hasEffect = true;
        mP = 0;
        effectType = eff;
        this.values = values;
        this.time = time;
        this.timeDelayDame = timeDelayDame;
    }


    public SkillEffect clone() {
        return (SkillEffect) SerializationUtils.clone(this);
    }

    public SkillEffect(ResSkillEntity resSkill) {
        int level = 0;
        dameBuff = resSkill.getAtkDame().isEmpty() ? 0L : resSkill.getAtkDame().get(0) + resSkill.getAtkDame().get(1) * (level);
        SkillObject skill = resSkill.getSkill();
        if (skill != null) {
            timeDelayDame = skill.getDelayDame();
            hasEffect = true;
            values = new ArrayList<>();
            triggerType = skill.getTrigger();
            mP = skill.mp;
            effectType = skill.getEffect();
            for (int i = 0; i < skill.getValue().size(); i += 2) {
                values.add(skill.getPerIndex(i, level));
            }
            time = skill.getTimeByLevel(level);
        } else hasEffect = false;
    }

    public float getFirstValues() {
        return values.get(0);
    }


    public float getValueIndex(int index) {
        return values.get(index);
    }


    public float getFirstPer() {
        return values.get(0) / 100f;
    }

    public float getNextPer() { // slot 2
        return values.size() > 1 ? values.get(1) / 100f : 0;
    }

    public float getValuePerIndex(int index) {
        return values.get(index) / 100f;
    }

    public Effect toEffect(Character owner, FactionType factionType) {
        return new Effect(owner, this, factionType);
    }

    public void setValues(int i, float v) {
        values.set(i, v);
    }
}
