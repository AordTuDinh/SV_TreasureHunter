package game.battle.object;

import game.battle.effect.SkillEffect;
import lombok.Data;

import java.io.Serializable;

@Data
public class BossSkill implements Serializable {
    int id;
    float range;
    int speed;
    SkillEffect effect;
    long timeActiveSkill;
    int degree;

    public BossSkill(int id, SkillEffect effect) {
        this.id = id;
        this.effect = effect;
    }
}
