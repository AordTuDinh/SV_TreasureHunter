package game.dragonhero.mapping.main;

import lombok.Getter;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.List;

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
