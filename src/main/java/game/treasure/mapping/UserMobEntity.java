package game.treasure.mapping;

import game.treasure.mapping.main.ResMobEntity;
import game.treasure.service.resource.ResMob;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@Table(name = "user_mob")
public class UserMobEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;
    int userId;
    int mobId;
    int tier = 1;

    public UserMobEntity(UserEntity user, int mobId, int tier) {
        this.userId = user.getId();
        this.mobId = mobId;
        this.tier = tier > 0 ? Math.min(tier, 4) : 1;
    }

    public ResMobEntity getResMob() {
        return ResMob.getMob(mobId);
    }

    public boolean deleteFromDb() {
        return DBJPA.delete("user_mob", "id", id, "user_id", userId);
    }

    public boolean update(List<Object> lst) {
        return DBJPA.update("user_mob", lst, List.of("id", id));
    }
}
