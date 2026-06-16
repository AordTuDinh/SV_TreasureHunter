package game.treasure.mapping;

import game.config.CfgMaterial;
import game.treasure.mapping.main.ResMaterialEntity;
import game.treasure.service.resource.ResItem;
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
    int tier;
    int level;
    float value;
    @Column(name = "socket_rate")
    float socketRate;

    public UserMaterialEntity(int userId, int materialId, int tier) {
        this.userId = userId;
        this.materialId = materialId;
        this.tier = tier;
        this.level = 1;
        this.socketRate = CfgMaterial.rollSocketRate();
        ResMaterialEntity res = getRes();
        this.value = res != null ? CfgMaterial.rollValue(res, tier) : 0f;
    }

    /** Giữ stat/rate khi merge fail — trả 1 viên rank cao nhất. */
    public static UserMaterialEntity cloneFrom(UserMaterialEntity src) {
        UserMaterialEntity c = new UserMaterialEntity();
        c.userId = src.userId;
        c.materialId = src.materialId;
        c.tier = src.tier;
        c.level = src.level;
        c.value = src.value;
        c.socketRate = src.socketRate;
        return c;
    }

    public ResMaterialEntity getRes() {
        return ResItem.getMaterial(materialId);
    }

    public float getSocketSuccessPercent() {
        return CfgMaterial.getSocketSuccessPercent(socketRate, level);
    }

    public protocol.Pbmethod.PbMaterial.Builder toProto() {
        protocol.Pbmethod.PbMaterial.Builder pb = protocol.Pbmethod.PbMaterial.newBuilder();
        pb.setId(id);
        pb.setMaterialId(materialId);
        pb.setTier(tier);
        pb.setValue(value);
        pb.setLevel(level);
        return pb;
    }

    public boolean update(List<Object> updateData) {
        return DBJPA.update("user_material", updateData, Arrays.asList("id", id));
    }

    public boolean deleteFromDb() {
        return DBJPA.delete("user_material", "id", id, "user_id", userId);
    }
}
