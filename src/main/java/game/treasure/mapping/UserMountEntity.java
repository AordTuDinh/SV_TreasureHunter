package game.treasure.mapping;

import game.battle.object.Point;
import game.treasure.mapping.main.ResMountEntity;
import game.treasure.service.resource.ResMount;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@Table(name = "user_mount")
public class UserMountEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;
    int userId;
    int mountId;
    int level;
    int server;
    String data;
    @Transient
    Point point;

    public UserMountEntity(UserEntity user, int mountId) {
        this.userId = user.getId();
        this.mountId = mountId;
        this.server = user.getServer();
        this.level = 1;
        this.data = "[]";
    }

    public ResMountEntity getRes() {
        return ResMount.get(mountId);
    }

    public protocol.Pbmethod.PbMount.Builder toProto() {
        protocol.Pbmethod.PbMount.Builder pb = protocol.Pbmethod.PbMount.newBuilder();
        pb.setId(id);
        pb.setMountId(mountId);
        pb.setLevel(level);
        return pb;
    }

    public boolean update(List<Object> lst) {
        return DBJPA.update("user_mount", lst, List.of("id", id));
    }
}
