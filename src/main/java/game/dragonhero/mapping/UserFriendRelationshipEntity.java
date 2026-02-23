package game.dragonhero.mapping;

import com.google.gson.Gson;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Calendar;

@Data
@Entity
@Table(name = "user_friend_relationship")
@NoArgsConstructor
public class UserFriendRelationshipEntity implements Serializable {
    @Id
    int userId1, userId2;
    int relationship;
    long timeCreated;

    public int getFriendId(int userId) {
        return userId == userId1 ? userId2 : userId1;
    }

    public UserFriendRelationshipEntity(int userId1, int userId2, int relationship) {
        this.userId1 = userId1;
        this.userId2 = userId2;
        this.relationship = relationship;
    }

    public boolean hasId(int id) {
        return userId1 == id || userId2 == id;
    }

    public boolean deleteFriend() {
        return DBJPA.delete("user_friend_relationship", "user_id1", userId1, "user_id2", userId2);
    }

    public boolean update() {
        return DBJPA.update(this);
    }
}