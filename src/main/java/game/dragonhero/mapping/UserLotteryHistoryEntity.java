package game.dragonhero.mapping;

import game.config.aEnum.ItemType;
import game.dragonhero.service.user.Bonus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.GsonUtil;
import protocol.Pbmethod;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Entity
@NoArgsConstructor
@Table(name = "user_lottery_history")
public class UserLotteryHistoryEntity implements Serializable {
    @Id
    @Getter
    int userId, eventId;
    int type, lucky;
    @Setter
    @Getter
    int status;
    String number, bonus, result;
    Date time;

    public List<Integer> getNumber() {
        return GsonUtil.strToListInt(number);
    }

    public List<Integer> getResult() {
        return GsonUtil.strToListInt(result);
    }

    public List<Long> getBonus() {
        return GsonUtil.strToListLong(bonus);
    }


    public ItemType getItemType() {
        return ItemType.get(type);
    }

    public Pbmethod.PbLotteryHistory toProto() {
        Pbmethod.PbLotteryHistory.Builder pb = Pbmethod.PbLotteryHistory.newBuilder();
        pb.setEventId(eventId);
        pb.setType(type);
        pb.setLuckyNum(lucky);
        pb.addAllNumber(getNumber());
        pb.setTime(time.getTime());
        pb.setStatus(status);
        List<Long> bonus = getBonus();
        pb.addAllBonus(bonus);
        pb.addAllListBonus(Bonus.merge(bonus));
        pb.addAllListResult(getResult());
        return pb.build();
    }

    public boolean update(List<Object> objects) {
        return DBJPA.update("user_lottery_history", objects, Arrays.asList("user_id", userId, "event_id", eventId));
    }
}
