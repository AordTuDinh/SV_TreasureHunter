package game.dragonhero.mapping.main;


import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

@Entity
@NoArgsConstructor
@Data
public class ResDynamicTypeEntity implements Serializable {
    @Id
    int type;
    String name, title;
    int number, go;

    public String getName() {
        return String.format(name, number);
    }
}
