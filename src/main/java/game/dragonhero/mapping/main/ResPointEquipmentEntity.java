package game.dragonhero.mapping.main;

import game.config.aEnum.EquipSlotType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ozudo.base.helper.NumberUtil;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@NoArgsConstructor
public class ResPointEquipmentEntity {
    @Getter
    @Id
    int id;
    int isMain, type;
    @Getter
    int rank, point, per;
    String name;
    @Getter
    float perMin, perMax;

    public EquipSlotType getType() {
        return EquipSlotType.get(type);
    }

    public boolean isMain() {
        return isMain == 1;
    }

    public int getRand() {
        return (int) (NumberUtil.getRandom((int) perMin, (int) perMax) * 100f);
    }

    public int getBonusAdd(float perXu) {
        int randAdd = getRand();
        return randAdd + (int) ((perMax * 100 - randAdd) * perXu / 100);
    }
}
