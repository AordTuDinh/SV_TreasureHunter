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
    int matRank;
    int level;
    float value;
    @Column(name = "socket_rate")
    float socketRate;

    public UserMaterialEntity(int userId, int materialId, int matRank) {
        this.userId = userId;
        this.materialId = materialId;
        this.matRank = matRank;
        this.level = 1;
        this.socketRate = CfgMaterial.rollSocketRate();
        ResMaterialEntity res = getRes();
        this.value = res != null ? CfgMaterial.rollValue(res, matRank) : 0f;
    }

    /** Giữ stat/rate khi merge fail — trả 1 viên rank cao nhất. */
    public static UserMaterialEntity cloneFrom(UserMaterialEntity src) {
        UserMaterialEntity c = new UserMaterialEntity();
        c.userId = src.userId;
        c.materialId = src.materialId;
        c.matRank = src.matRank;
        c.level = src.level;
        c.value = src.value;
        c.socketRate = src.socketRate;
        return c;
    }

    public ResMaterialEntity getRes() {
        return ResItem.getMaterial(materialId);
    }

    public int getTier() {
        ResMaterialEntity res = getRes();
        return res == null ? 0 : res.getTier();
    }

    public float getSocketSuccessPercent() {
        return CfgMaterial.getSocketSuccessPercent(socketRate, level);
    }

    public protocol.Pbmethod.PbMaterial.Builder toProto() {
        protocol.Pbmethod.PbMaterial.Builder pb = protocol.Pbmethod.PbMaterial.newBuilder();
        pb.setId(id);
        System.out.println("materialId ====== " + materialId);
        pb.setMaterialId(materialId);
        pb.setRank(matRank);
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
