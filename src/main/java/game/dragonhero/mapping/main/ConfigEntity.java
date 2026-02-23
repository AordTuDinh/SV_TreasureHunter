package game.dragonhero.mapping.main;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Data
@NoArgsConstructor
public class ConfigEntity {
    @Id
    private String k;
    private String v;

    public ConfigEntity(String k, String v) {
        this.k = k;
        this.v = v;
    }

}
