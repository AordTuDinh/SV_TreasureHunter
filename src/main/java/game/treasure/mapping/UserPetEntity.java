package game.treasure.mapping;

import game.treasure.mapping.main.ResPetEntity;
import game.treasure.service.resource.ResPet;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.GsonUtil;

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
    int tier = 1;
    int isCraft;
    int icon;
    int server;
    String data;


    public UserPetEntity(UserEntity user, int petId,int tier) {
        this.userId = user.getId();
        this.server = user.getServer();
        this.petId = petId;
        this.level = 1;
        this.tier = tier;
        this.isCraft = 0;
        this.icon = petId;
        this.data = getResPet().getPointData(tier);
    }

    public List<Float> getDataListFloat() {
        if (data == null || data.isEmpty() || "[]".equals(data))
            return new java.util.ArrayList<>();
        return GsonUtil.strToListFloat(data);
    }

    public void setData(String data) {
        this.data = data;
    }

    public ResPetEntity getResPet() {
        return ResPet.getPet(petId);
    }

    public protocol.Pbmethod.PbPet.Builder toProto() {
        protocol.Pbmethod.PbPet.Builder pb = protocol.Pbmethod.PbPet.newBuilder();
        pb.setId(id);
        pb.setPetId(petId);
        pb.setLevel(level);
        pb.setTier(tier > 0 ? tier : 1);
        pb.setIsCraft(isCraft);
        if (icon > 0)
            pb.setIcon(icon);
        return pb;
    }

    public boolean update(List<Object> lst) {
        return DBJPA.update("user_pet", lst, List.of("id", id));
    }
}
