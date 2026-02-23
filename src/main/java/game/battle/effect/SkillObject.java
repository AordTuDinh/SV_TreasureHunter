package game.battle.effect;

import game.dragonhero.service.battle.EffectType;
import game.dragonhero.service.battle.TriggerType;
import lombok.Getter;
import org.apache.commons.lang.SerializationUtils;
import ozudo.base.helper.GsonUtil;

import java.io.Serializable;
import java.util.List;

public class SkillObject implements Serializable {
    String trigger;
    int mp;
    String effect;
    String value; // point value - per level
    String time;
    @Getter
    float delayDame;


    public TriggerType getTrigger() {
        return TriggerType.get(trigger);
    }

    public SkillObject clone() {
        return (SkillObject) SerializationUtils.clone(this);
    }

    public EffectType getEffect() {
        return EffectType.get(effect);
    }

    public List<Float> getValue() {
        return GsonUtil.strToListFloat(value);
    }

    public float getPerIndex(int index, int level) {
        if (index % 2 == 1) return 0; // nhận 0 -2 -4 -6
        List<Float> values = getValue();
        return values.get(index) + values.get(index + 1) * level;
    }

    public float getTimeByLevel(int level) {
        List<Float> time = getTime();
        return time.get(0) + level * time.get(1);
    }

    public List<Float> getTime() {
        return GsonUtil.strToListFloat(time);
    }
}
