package game.dragonhero.mapping.main;


import game.config.aEnum.ToolFarmType;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import java.io.Serializable;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
//@Table(name = "res_item_tool")
public class ResItemToolEntity extends AbstractItemFarm implements Serializable {
    String data;
    int type;// type 0 : show, 1 : Phân bón giảm thời gian - 2: Kích thích tăng năng suất.

    public List<Integer> getData() {
        return GsonUtil.strToListInt(data);
    }

    public ToolFarmType getType() {
        return ToolFarmType.get(type);
    }

}
