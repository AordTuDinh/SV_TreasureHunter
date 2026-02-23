package game.battle.object;


import game.battle.type.InitEnemyPosType;
import lombok.Data;
import ozudo.base.helper.NumberUtil;

import java.io.Serializable;
import java.util.List;

@Data
public class EnemyWave implements Serializable {
    int enemyId;
    int number;
    InitEnemyPosType posType;

    public EnemyWave(List<Integer> data) {
        this.enemyId = data.get(0);
        this.number = data.get(1);
        this.posType = InitEnemyPosType.get(data.get(2));
    }




}
