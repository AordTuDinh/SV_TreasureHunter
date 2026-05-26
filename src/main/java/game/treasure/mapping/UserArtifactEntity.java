package game.treasure.mapping;

import game.treasure.mapping.main.ResArtifactEntity;
import game.treasure.service.resource.ResArtifact;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@Table(name = "user_artifact")
public class UserArtifactEntity implements Serializable {
    @Id
    int userId, artifactId;
    int level;
    String data;

    public UserArtifactEntity(int userId, int artifactId) {
        this.userId = userId;
        this.artifactId = artifactId;
        this.level = 1;
        this.data = "[]";
    }

    public ResArtifactEntity getRes() {
        return ResArtifact.get(artifactId);
    }

    public boolean update(List<Object> updateData) {
        return DBJPA.update("user_artifact", updateData,
                Arrays.asList("user_id", userId, "artifact_id", artifactId));
    }

    public boolean deleteFromDb() {
        return DBJPA.delete("user_artifact",
                "user_id", userId, "artifact_id", artifactId);
    }
}
