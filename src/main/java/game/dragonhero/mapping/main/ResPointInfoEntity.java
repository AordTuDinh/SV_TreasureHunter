package game.dragonhero.mapping.main;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
@NoArgsConstructor
public class ResPointInfoEntity {
    @Id
    int id;
    String name, desc, unit;

    public String getUnit() {
        return unit.equals("%") ? "%" : "";
    }
}
