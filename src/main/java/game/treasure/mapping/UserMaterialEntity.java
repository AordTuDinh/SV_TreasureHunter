package game.treasure.mapping;

import game.config.CfgMaterial;
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
    int rank, level;
    float value;

    public UserMaterialEntity(int userId, int materialId, int rank) {
        this.userId = userId;
        this.materialId = materialId;
        this.rank = rank;
        this.level = 1;
        ResMaterialEntity res = getRes();
        this.value = res != null ? CfgMaterial.rollValue(res, rank) : 0f;
    }

    public ResMaterialEntity getRes() {
        return ResItem.getMaterial(materialId);
    }

    public int getTier() {
        ResMaterialEntity res = getRes();
        return res == null ? 0 : res.getTier();
    }

    public protocol.Pbmethod.PbMaterial.Builder toProto() {
        protocol.Pbmethod.PbMaterial.Builder pb = protocol.Pbmethod.PbMaterial.newBuilder();
        pb.setId(id);
        pb.setMaterialId(materialId);
        pb.setRank(rank);
        pb.setValue(value);
        pb.setLevel(level);
        return pb;
    }

    public boolean update(List<Object> updateData) {
        return DBJPA.update("user_material", updateData, Arrays.asList("id", id));
    }
}
