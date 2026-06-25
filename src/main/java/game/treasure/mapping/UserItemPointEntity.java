package game.treasure.mapping;

import game.treasure.mapping.main.ResItemPointEntity;
import game.treasure.service.resource.ResItemPoint;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Arrays;

@Data
@NoArgsConstructor
@Entity
@Table(name = "user_item_point")
public class UserItemPointEntity implements Serializable {
    @Id
    int userId;
    @Id
    int pointId;
    int number;
    int server;

    public UserItemPointEntity(int userId, int pointId, int server) {
        this.userId = userId;
        this.pointId = pointId;
        this.server = server;
        this.number = 0;
    }

    public ResItemPointEntity getRes() {
        return ResItemPoint.get(pointId);
    }

    public protocol.Pbmethod.PbItemPoint.Builder toProto() {
        protocol.Pbmethod.PbItemPoint.Builder pb = protocol.Pbmethod.PbItemPoint.newBuilder();
        pb.setItemKey(pointId);
        pb.setNumber(number);
        return pb;
    }

    public boolean saveOrUpdate() {
        return DBJPA.saveOrUpdate(this);
    }

    public boolean updateNumber(int newNumber) {
        this.number = newNumber;
        return DBJPA.update("user_item_point",
                Arrays.asList("number", number, "server", server),
                Arrays.asList("user_id", userId, "point_id", pointId));
    }
}
