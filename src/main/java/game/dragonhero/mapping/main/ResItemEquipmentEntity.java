package game.dragonhero.mapping.main;

import game.config.aEnum.EquipSlotType;
import game.dragonhero.service.resource.ResItem;
import game.dragonhero.service.user.Bonus;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class ResItemEquipmentEntity {
    @Id
    int id;
    String name, desc, dataAccessory, feeAccessory;
    int enable, rank, type, maxLevel, target,levelRequire;


    public EquipSlotType getType() {
        return EquipSlotType.get(type);
    }

    public ResItemEquipmentEntity getTarget() {
        return ResItem.getItemEquipment(target);
    }

    public int getNextId() {
        return target;
    }

    public List<Long> getDataAccessory() {
        return GsonUtil.strToListLong(dataAccessory);
    }

    public List<Long> getFeeUpgradeAccessory() {
        return Bonus.reverseBonus(GsonUtil.strToListLong(feeAccessory));
    }
}
