package game.battle.object;

import lombok.Data;

import java.io.Serializable;

@Data
public class BossSkill implements Serializable {
    int id;
    float range;
    int speed;
    long timeActiveSkill;
    int degree;

    public BossSkill(int id) {
        this.id = id;
    }
}
