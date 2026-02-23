package game.dragonhero.mapping.main;


import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
@NoArgsConstructor
public class ResWeaponMapStoneEntity {
    @Id
    int weaponId;
    int stoneId;
}
