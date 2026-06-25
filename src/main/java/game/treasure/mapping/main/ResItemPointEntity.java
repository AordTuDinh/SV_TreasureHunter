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
@Table(name = "res_item_point")
public class ResItemPointEntity implements Serializable {
    @Getter
    @Id
    @Column(name = "point_id")
    int pointId;
    @Getter
    String name;
    @Getter
    String desc;
    @Getter
    int tier;
}
