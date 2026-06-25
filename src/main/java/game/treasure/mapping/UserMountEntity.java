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
    int tier = 1;
    int isCraft;
    int server;
    String data;

    public UserMountEntity(UserEntity user, int mountId,int tier) {
        this.userId = user.getId();
        this.mountId = mountId;
        this.server = user.getServer();
        this.level = 1;
        this.tier = tier;
        this.isCraft = 0;
        this.data = getRes().getPointData(tier);
    }

    public ResMountEntity getRes() {
        return ResMount.get(mountId);
    }

    public protocol.Pbmethod.PbMount.Builder toProto() {
        protocol.Pbmethod.PbMount.Builder pb = protocol.Pbmethod.PbMount.newBuilder();
        pb.setId(id);
        pb.setMountId(mountId);
        pb.setLevel(level);
        pb.setTier(tier > 0 ? tier : 1);
        pb.setIsCraft(isCraft);
        return pb;
    }

    public boolean update(List<Object> lst) {
        return DBJPA.update("user_mount", lst, List.of("id", id));
    }
}
