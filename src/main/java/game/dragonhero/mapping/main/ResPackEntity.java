package game.dragonhero.mapping.main;

import game.config.aEnum.PackType;
import game.config.lang.Lang;
import game.object.MyUser;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
public class ResPackEntity {
    @Id
    int id;
    String name, image, desc, desc2, bonus, data, bonusDay, price;
    int time, limit;// time : tính bằng phút. time =-1 -> tính đến cuối ngày

    public List<Long> getBonus() {
        return new ArrayList<>(GsonUtil.strToListLong(bonus));
    }

    public List<Long> getPrice() {
        return GsonUtil.strToListLong(price);
    }

    public String getPriceString() {
        return price;
    }

    public List<List<Long>> getData() {
        return GsonUtil.strTo2ListLong(data);
    }
    public List<Long> getDataList() {
        return GsonUtil.strToListLong(data);
    }


    public String getStringData() {
        return data;
    }

    public protocol.Pbmethod.PbCellEvent toPbCell(MyUser mUser, int numBuy, int buttonStatus, long timeRemain) {
        protocol.Pbmethod.PbCellEvent.Builder pb = protocol.Pbmethod.PbCellEvent.newBuilder();
        pb.setId(id);
        pb.setImage(image);
        pb.addAllBonus(GsonUtil.strToListInt(bonus));
        pb.setNameCell(Lang.getTitle(mUser, name));
        pb.setTextCell(Lang.getTitle(mUser,desc));
        pb.setTextDesc(Lang.getTitle(mUser,desc2));
        pb.setNumBuy(numBuy);
        pb.setLimit(limit);
        pb.addAllPrice(getPrice());
        pb.setButtonStatus(buttonStatus);
        pb.addAllBonusDay(GsonUtil.strToListInt(bonusDay));
        pb.setTimeRemain(timeRemain);
        pb.setTimeExpire(time * DateTime.HOUR_SECOND);
        return pb.build();
    }

    public long getTime() { // conver sang ms
        if (id == PackType.QUEST_B.value) {
            return DateTime.getSecondsToNextDay() * DateTime.SECOND2_MILLI_SECOND;
        }
        // time > 0 thì trả về ms còn <0 sẽ trả về type
        if (time > 0) return time * DateTime.HOUR_MILLI_SECOND;
        else return time;
    }
}
