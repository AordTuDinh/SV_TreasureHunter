package game.treasure.mapping.main;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

@NoArgsConstructor
@Entity
public class ResMaterialEntity implements Serializable {
    @Getter
    @Id
    int id;
    @Getter
    String name;
    @Getter
    String desc;
    @Getter
    int rank;
    @Getter
    int maxPoint;
}
