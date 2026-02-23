package game.dragonhero.mapping.main;

import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class ResQuestEntity implements Serializable {
    @Id
    int id;
    String desc;
    int number, numberMonth, level, bonus, type;
    @Column(name = "number_c")
    int numberC;
    @Column(name = "bonus_c")
    int bonusC;
    String bonusMonth;


    public List<Long> getBonusMonth() {
        return GsonUtil.strToListLong(bonusMonth);
    }
}
