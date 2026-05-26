package game.treasure.mapping.main;

import game.config.aEnum.ArtifactType;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Data
@NoArgsConstructor
public class ResArtifactEntity {
    @Id
    int id;
    String name, desc, data;
    int rank, type;

    public ArtifactType getArtifactType() {
        return ArtifactType.get(type);
    }
}
