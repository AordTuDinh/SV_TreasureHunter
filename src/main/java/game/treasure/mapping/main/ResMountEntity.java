package game.treasure.mapping.main;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

@Data
@NoArgsConstructor
@Entity
public class ResMountEntity implements Serializable {
    @Id
    int id;
    String name,data;
    int rank;
}
