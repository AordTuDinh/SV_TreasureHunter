package game.treasure.mapping;

import game.treasure.mapping.main.ResArtifactEntity;
import game.treasure.service.resource.ResArtifact;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.GsonUtil;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@Table(name = "user_artifact")
public class UserArtifactEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;
    int userId;
    int artifactId;
    int level;
    String data;

    @Transient
    int bagSlot = -1;

    public UserArtifactEntity(int userId, int artifactId) {
        this.userId = userId;
        this.artifactId = artifactId;
        this.level = 1;
        this.data = resolveDataFromRes(artifactId);
    }

    static String resolveDataFromRes(int artifactId) {
        ResArtifactEntity res = ResArtifact.get(artifactId);
        if (res == null || res.getData() == null || res.getData().isBlank() || "[]".equals(res.getData()))
            return "[]";
        return res.getData();
    }

    public ResArtifactEntity getRes() {
        return ResArtifact.get(artifactId);
    }

    public List<Float> getDataListFloat() {
        if (data == null || data.isEmpty() || "[]".equals(data))
            return new ArrayList<>();
        return GsonUtil.strToListFloat(data);
    }

    public protocol.Pbmethod.PbArtifact.Builder toProto() {
        protocol.Pbmethod.PbArtifact.Builder pb = protocol.Pbmethod.PbArtifact.newBuilder();
        pb.setId(id);
        pb.setArtifactId(artifactId);
        pb.setLevel(level);
        if (data != null && !data.isEmpty() && !"[]".equals(data))
            pb.setData(data);
        return pb;
    }

    public boolean update(List<Object> updateData) {
        return DBJPA.update("user_artifact", updateData, Arrays.asList("id", id));
    }

    public boolean deleteFromDb() {
        return DBJPA.delete("user_artifact", "id", id, "user_id", userId);
    }
}
