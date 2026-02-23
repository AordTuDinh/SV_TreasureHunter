package game.dragonhero.mapping.main;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Data
@NoArgsConstructor
@Table(name = "config_help")
public class ConfigHelpEntity {
    @Id
    String k;
    String vi, en, km, zh,ru,jp;
}
