package game.treasure.mapping;

import game.treasure.mapping.main.ResMountEntity;
import game.treasure.service.resource.ResMount;
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
@Table(name = "user_mount")
public class UserMountEntity implements Serializable {
    @Id
    int userId, mountId;

    public UserMountEntity(int userId, int mountId) {
        this.userId = userId;
        this.mountId = mountId;
    }

    public ResMountEntity getRes() {
        return ResMount.get(mountId);
    }

//    public boolean deleteFromDb() {
//        return DBJPA.delete("user_mount", "user_id", userId, "mount_id", mountId);
//    }
}
