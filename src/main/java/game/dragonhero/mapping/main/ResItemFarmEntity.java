package game.dragonhero.mapping.main;


import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import java.io.Serializable;

@Data
@NoArgsConstructor
//@Table(name = "res_item_farm")
@Entity
public class ResItemFarmEntity extends AbstractItemFarm implements Serializable {

}
