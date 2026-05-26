package game.treasure.mapping;

import game.battle.calculate.IMath;
import game.battle.object.Point;
import game.config.CfgPet;
import game.treasure.mapping.main.ResPetEntity;
import game.treasure.service.resource.ResPet;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.StringHelper;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@Table(name = "user_pet")
public class UserPetEntity implements Serializable {
    @Id
    int userId, petId;
    int level;
    int server;
    @Transient
    Point point;

    public UserPetEntity(UserEntity user , int petId) {
        this.userId = user.getId();
        this.server = user.getServer();
        this.petId = petId;
        this.level =1;
    }



    public ResPetEntity getResPet() {
        return ResPet.getPet(petId);
    }



    public protocol.Pbmethod.PbPet.Builder toProto() {
        protocol.Pbmethod.PbPet.Builder pb = protocol.Pbmethod.PbPet.newBuilder();
        pb.setId(petId);
        return pb;
    }


    public boolean update(List<Object> lst) {
        return DBJPA.update("user_pet", lst, List.of("user_id", userId, "pet_id", petId));
    }
}
