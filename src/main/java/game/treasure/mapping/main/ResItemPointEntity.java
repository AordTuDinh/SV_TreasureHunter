package game.treasure.mapping.main;

import lombok.Getter;
import lombok.NoArgsConstructor;
import protocol.Pbmethod;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;

@NoArgsConstructor
@Entity
@Table(name = "res_item_point")
public class ResItemPointEntity implements Serializable {
    @Getter
    @Id
    int pointId;
    @Getter
    String name;
    @Getter
    String desc;
    @Getter
    int tier;
    @Getter
    int type, sellPrice;

    @Getter
    @Transient
    Pbmethod.ItemPointType itemPointType;

    public void init() {
        itemPointType = type > 0 ? Pbmethod.ItemPointType.valueOf(type) : null;
    }
}
