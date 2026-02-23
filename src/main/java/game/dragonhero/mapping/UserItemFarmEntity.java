package game.dragonhero.mapping;

import game.config.aEnum.ItemFarmType;
import game.dragonhero.mapping.main.AbstractItemFarm;
import game.dragonhero.service.resource.ResFarm;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Data
@Entity
@NoArgsConstructor
@Table(name = "user_item_farm")
public class UserItemFarmEntity implements Serializable {
    @Id
    int userId, id, type;
    int number;

    public UserItemFarmEntity(int userId, int type, int id, int number) {
        this.userId = userId;
        this.id = id;
        this.number = number;
        this.type = type;
    }

    public AbstractItemFarm getRes() {
        switch (ItemFarmType.get(type)) {
            case SEED -> {
                return ResFarm.getSeed(id);
            }
            case AGRI -> {
                return ResFarm.getItemFarm(id);
            }
            case TOOL -> {
                return ResFarm.getTool(id);
            }
            case FOOD -> {
                return ResFarm.getItemFood(id);
            }
        }
        return null;
    }

    public void add(int value) {
        this.number += value;
    }

    public protocol.Pbmethod.PbItemFarm.Builder toProto() {
        protocol.Pbmethod.PbItemFarm.Builder pb = protocol.Pbmethod.PbItemFarm.newBuilder();
        pb.setType(type);
        pb.setId(id);
        pb.setNumber(number);
        return pb;
    }
}
