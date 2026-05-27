package game.treasure.mapping;

import game.battle.object.Point;
import game.treasure.mapping.main.ResPetEntity;
import game.treasure.service.resource.ResPet;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@Table(name = "user_pet")
public class UserPetEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;
    int userId;
    int petId;
    int level;
    int server;
    @Transient
    Point point;

    public UserPetEntity(UserEntity user, int petId) {
        this.userId = user.getId();
        this.server = user.getServer();
        this.petId = petId;
        this.level = 1;
    }

    public ResPetEntity getResPet() {
        return ResPet.getPet(petId);
    }

    public protocol.Pbmethod.PbPet.Builder toProto() {
        protocol.Pbmethod.PbPet.Builder pb = protocol.Pbmethod.PbPet.newBuilder();
        pb.setId(id);
        pb.setPetId(petId);
        pb.setLevel(level);
        return pb;
    }

    public boolean update(List<Object> lst) {
        return DBJPA.update("user_pet", lst, List.of("id", id));
    }
}
