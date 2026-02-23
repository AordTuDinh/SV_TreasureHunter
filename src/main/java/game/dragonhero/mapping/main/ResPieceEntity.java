package game.dragonhero.mapping.main;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

@Data
@NoArgsConstructor
@Entity
public class ResPieceEntity implements Serializable {
    @Id
    int type, id;
    int rank, target;
    String name, desc;
}
