package game.dragonhero.controller;


import game.dragonhero.mapping.UserEntity;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Data
@Entity
@NoArgsConstructor
@Table(name = "user_event_top")
public class UserEventTopEntity  implements Serializable {
    @Id
    int userId,eventType;
    int server;
    int point;

    public UserEventTopEntity(UserEntity  user, int eventType) {
        this.userId = user.getId();
        this.eventType = eventType;
        this.server =  user.getServer();
        this.point = 0;
    }

    public UserEventTopEntity(int userId, int eventType, int server, int point) {
        this.userId = userId;
        this.eventType = eventType;
        this.server = server;
        this.point = point;
    }

    public void addPoint(int point){
        this.point += point;
    }

    public boolean update(){
        return DBJPA.update(this);
    }
}
