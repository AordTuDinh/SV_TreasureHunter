package game.dragonhero.mapping.main;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Data
@NoArgsConstructor
public class ResTitleEntity {
    @Id
    private String k;
    private String vi;
    private String en, km, jp, ru, zh;
}
