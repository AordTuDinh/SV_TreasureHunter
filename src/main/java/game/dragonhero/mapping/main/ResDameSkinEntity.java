package game.dragonhero.mapping.main;


import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
public class ResDameSkinEntity {
    @Id
    int id;
    int rank;
    String name;
}
