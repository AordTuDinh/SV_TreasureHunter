package game.treasure.mapping.main;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

@NoArgsConstructor
@Entity
public class ResObjectEntity extends BaseEntity implements Serializable {
    @Getter
    @Id
    int id;
    @Getter
    String name;
    @Getter
    int hp;
    @Getter
    String drop;
}

