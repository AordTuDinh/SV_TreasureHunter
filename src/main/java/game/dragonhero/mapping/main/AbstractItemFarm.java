package game.dragonhero.mapping.main;


import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

@Data
@NoArgsConstructor
@Entity
public class AbstractItemFarm extends BaseEntity implements Serializable {
    @Id
    int id;
    int rank, sell;
    String name;


    public ResSeedEntity getSeed() {
        return (ResSeedEntity) this;
    }
}
