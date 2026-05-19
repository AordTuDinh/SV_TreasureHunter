package game.treasure.mapping.main;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@NoArgsConstructor
@Entity
@Table(name = "res_material")
public class ResMaterialEntity implements Serializable {
    @Getter
    @Id
    int id;
    @Getter
    String name;
    @Getter
    int tier;
    @Getter
    @Column(name = "point_id")
    int pointId;
    @Getter
    @Column(name = "base_point")
    String basePoint;
    @Getter
    double rare;
    @Getter
    double epic;
    @Getter
    double legend;
}
