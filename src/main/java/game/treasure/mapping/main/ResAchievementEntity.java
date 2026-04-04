package game.treasure.mapping.main;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@NoArgsConstructor
@Table(name = "res_achievement")
public class ResAchievementEntity implements Serializable {
    @Id
    @Getter
    int type, id;
    String name;
    @Getter
    int number, bonus;


}
