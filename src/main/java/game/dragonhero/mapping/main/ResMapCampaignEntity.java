package game.dragonhero.mapping.main;

import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import java.io.Serializable;


@Entity
@NoArgsConstructor
public class ResMapCampaignEntity extends BaseMap implements Serializable {
    String enemy, bonus;
}