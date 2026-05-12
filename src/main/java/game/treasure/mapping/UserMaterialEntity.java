package game.treasure.mapping;

import game.treasure.mapping.main.ResMaterialEntity;
import game.treasure.service.resource.ResItem;
import game.treasure.service.user.Bonus;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@Table(name = "user_material")
public class UserMaterialEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;
    int userId, materialId;
    String data;

    public UserMaterialEntity(int userId, int materialId) {
        this.userId = userId;
        this.materialId = materialId;
        this.data = "[]";
    }

    public ResMaterialEntity getRes() {
        return ResItem.getMaterial(materialId);
    }

    public int getRank() {
        ResMaterialEntity res = getRes();
        return res == null ? 0 : res.getRank();
    }

    public protocol.Pbmethod.PbMaterial.Builder toProto() {
        protocol.Pbmethod.PbMaterial.Builder pb = protocol.Pbmethod.PbMaterial.newBuilder();
        pb.setId(id);
        pb.setMaterialId(materialId);
        pb.setType(getRank());
        pb.setData(data == null ? "[]" : data);
        return pb;
    }

    public List<Long> viewBonus() {
        return Bonus.viewMaterial(materialId);
    }

    public boolean update(List<Object> updateData) {
        return DBJPA.update("user_material", updateData, Arrays.asList("id", id));
    }
}
