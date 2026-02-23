package game.dragonhero.mapping.main;

import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
public class ResComboWeaponEntity extends BaseEntity {
    @Id
    int id;
    String name, weapons, point, desc;
    int maxLevel;
    @Transient
    List<Integer> aWeapon;
    @Transient
    List<Integer> pointCombo; //[id - value]

    public void initData() {
        aWeapon = GsonUtil.strToListInt(weapons);
        pointCombo = GsonUtil.strToListInt(point);
        checkJson(id, weapons);
        checkJson(id, point);
    }
}
