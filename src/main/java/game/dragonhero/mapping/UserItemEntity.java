package game.dragonhero.mapping;

import game.config.aEnum.ItemKey;
import game.config.aEnum.ItemType;
import game.config.aEnum.StatusType;
import game.dragonhero.mapping.main.ResItemEntity;
import game.dragonhero.service.resource.ResItem;
import game.dragonhero.service.user.Bonus;
import game.monitor.Telegram;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.StringHelper;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import static game.config.aEnum.ItemType.*;

@Data
@NoArgsConstructor
@Entity
@Table(name = "user_item")
public class UserItemEntity implements Serializable {
    @Id
    int userId, itemId;
    int number;
    String data; // eventId,lst NUm
    @Transient
    int countWin;

    public UserItemEntity(int userId, int itemId, int number) {
        genDataItem(userId, itemId, number);
    }

    public UserItemEntity(int userId, ItemKey itemKey, int number) {
        genDataItem(userId, itemKey.id, number);
    }

    public void genDataItem(int userId, int itemId, int number) {
        this.userId = userId;
        this.itemId = itemId;
        this.number = number;
        int type = getRes().getItemType().value;
        switch (ItemType.get(type)) {
            case QUEST_B -> {
                this.number = 1;
                int day = DateTime.getNumberDay();
                List<Integer> data = List.of(day, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0);//[status - number]x5
                this.data = StringHelper.toDBString(data);
            }
        }
    }

    public List<Integer> getDataListInt() {
        return GsonUtil.strToListInt(data);
    }

    public boolean expired() {
        try {
            int type = getType().value;
            if (type == QUEST_B.value) {
                int day = DateTime.getNumberDay();
                List<Integer> data = getDataListInt(); // day - [status-number] x5
                int saveDay = data.get(0);
                if (day != saveDay) {
                    number = 0;
                    return DBJPA.update(this);
                } else return false;
            } else if (type == LOTTE_SPECIAL.value || type == LOTTE_NORMAL.value) {
                int day = DateTime.getNumberDay();
                List<Integer> aData = GsonUtil.strToListInt(data);
                if (aData.isEmpty()) return true;
                int saveDay = aData.get(0);
                if (day != saveDay) {
                    number = 0;
                    this.data = "[]";
                    return DBJPA.update(this);
                } else return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
//            Telegram.sendNotify("ERR ITEM ->" + e + " user_id = " + userId + " itemId = " + itemId);
            return false;
        }
        return false;
    }


    public protocol.Pbmethod.PbItem.Builder toProto() {
        if (number == 0 || expired()) return null;
        protocol.Pbmethod.PbItem.Builder pb = protocol.Pbmethod.PbItem.newBuilder();
        pb.setId(itemId);
        pb.setType(getType().value);
        pb.setNumber(number);
        pb.setData(data == null ? "[]" : data);
        return pb;
    }

    public ItemType getType() {
        return getRes().getItemType();
    }

    public List<Long> viewBonus(long number) {
        return Bonus.viewItem(itemId, number);
    }

    public ResItemEntity getRes() {
        return ResItem.getItem(itemId);
    }

    public void add(int value) {
        this.number += value;
        if (getType() == QUEST_B) {
            this.number = 1;
            this.data = DateTime.getNumberDay() + "";
        }
    }


    public boolean update(List<Object> updateData) {
        return DBJPA.update("user_item", updateData, Arrays.asList("user_id", userId));
    }
}
