package game.battle.object;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Data
public class BonusKillEnemy {
    long gold;
    long exp;
    List<Long> bonus=new ArrayList<>();

    public void addBonus(List<Long> bonus){
        this.bonus.addAll(bonus);
    }

    public void set75(){
        gold =Math.max(1, (long) (gold*0.75f));
        exp =Math.max(1, (long) (exp*0.75f));
    }
}
