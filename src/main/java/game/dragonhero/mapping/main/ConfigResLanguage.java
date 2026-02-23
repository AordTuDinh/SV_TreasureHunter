package game.dragonhero.mapping.main;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Data
public class ConfigResLanguage implements java.io.Serializable {
    @Id
    private String k;
    private String vi;
    private String en, km, jp, ru, zh;

    public ConfigResLanguage() {
    }
}
