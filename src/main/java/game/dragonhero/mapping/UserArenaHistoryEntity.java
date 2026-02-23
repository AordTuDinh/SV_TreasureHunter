package game.dragonhero.mapping;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import game.object.ArenaTinyUser;
import lombok.Data;
import lombok.NoArgsConstructor;
import protocol.Pbmethod;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Calendar;
import java.util.Date;

@Data
@Entity
@NoArgsConstructor
@Table(name = "user_arena_history")
public class UserArenaHistoryEntity {
    @Id
    int id;
    int atkId, defId, status, timeAttack, point1, point2;
    String atkInfo, defInfo;
    Date time;

    public UserArenaHistoryEntity(UserEntity atk, UserEntity def, int status, int timeAttack, int p1, int p2, UserArenaEntity a1, UserArenaEntity a2) {
        this.atkId = atk.getId();
        this.defId = def.getId();
        this.status = status;// 1 thắng 0 thua
        this.timeAttack = timeAttack;
        this.point1 = p1;
        this.point2 = p2;
        this.time = Calendar.getInstance().getTime();
        this.atkInfo = new ArenaTinyUser(atk, a1.getArenaPoint()).toString();
        this.defInfo = new ArenaTinyUser(def, a2.getArenaPoint()).toString();
    }

    public Pbmethod.PbHistory toProto(int userId) {
        Pbmethod.PbHistory.Builder pb = Pbmethod.PbHistory.newBuilder();
        pb.setIsAttack(atkId == userId);
        pb.setTargetId(atkId == userId ? defId : atkId);
        pb.setStatus(status);
        pb.setTime(time.getTime());
        pb.setTimeAttack(timeAttack);
        pb.setPoint1(atkId == userId ? point1 : point2);
        pb.setPoint2(atkId == userId ? point2 : point1);
        ArenaTinyUser uInfo = null;
        ArenaTinyUser myInfo = null;
        if (userId == atkId) {
            uInfo = new Gson().fromJson(defInfo, new TypeToken<ArenaTinyUser>() {
            }.getType());
            myInfo = new Gson().fromJson(atkInfo, new TypeToken<ArenaTinyUser>() {
            }.getType());
        } else {
            uInfo = new Gson().fromJson(atkInfo, new TypeToken<ArenaTinyUser>() {
            }.getType());
            myInfo = new Gson().fromJson(defInfo, new TypeToken<ArenaTinyUser>() {
            }.getType());
        }
//        System.out.println("myInfo.getArenaPoint() = " + myInfo.getArenaPoint());
        pb.setMyPoint(myInfo.getArenaPoint());
        if (uInfo != null) pb.setUser(uInfo.toProto());
        return pb.build();
    }

}
