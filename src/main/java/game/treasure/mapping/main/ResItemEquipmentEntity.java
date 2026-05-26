package game.treasure.mapping.main;

import game.config.aEnum.ItemEquipmentType;
import game.treasure.service.resource.ResItem;
import game.treasure.service.user.Bonus;
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
    String name, desc;
    int  rank, type;


    public ItemEquipmentType getEquipmentType() {
        return ItemEquipmentType.get(type);
    }
}
