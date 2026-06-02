package game.treasure.mapping;

import game.config.aEnum.ItemType;
import game.treasure.mapping.main.ResItemEntity;
import game.treasure.mapping.main.ResItemEquipmentEntity;
import game.treasure.service.resource.ResItem;
import game.treasure.service.user.Bonus;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.StringHelper;
import protocol.Pbmethod;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static game.config.aEnum.ItemType.*;

@Data
@NoArgsConstructor
@Entity
@Table(name = "user_item")
public class UserItemEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;
    int userId;
    int itemId;
    /**
     * 1=consumable, 2=equipment, 3=currency, 4=event
     */
    int type;
    int level;
    int lockDestroy;
    int tier;
    String point;
    String data;

    @Transient
    boolean isEquip;
    @Transient
    int countWin;

    public UserItemEntity(int userId, int itemId, ItemType type) {
        this.userId = userId;
        this.itemId = itemId;
        this.type = type.value;
        level = 0;
        lockDestroy = 0;
        tier = 1;
        point = "[]";
        genDataItem();
    }

    void genDataItem() {
        if (type != CONSUMABLE.value) {
            if (data == null || data.isEmpty()) data = "[]";
            return;
        }
        ItemType resType = getResItemType();
        switch (resType) {
//            case QUEST_B -> {
//                int day = DateTime.getNumberDay();
//                List<Integer> questData = List.of(day, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0);
//                this.data = StringHelper.toDBString(questData);
//            }
            default -> this.data = "[]";
        }
    }

    public boolean isEquipment() {
        return type == EQUIPMENT.value;
    }

    public boolean isAggregatedItem() {
        if (type != CONSUMABLE.value) return false;
        return itemId == Pbmethod.ItemKey.TICKER_NORMAL.getNumber() || itemId == Pbmethod.ItemKey.TICKER_SPECIAL.getNumber();
    }


    public List<Integer> getDataListInt() {
        return GsonUtil.strToListInt(data);
    }

    public void clearAggregated() {
        this.data = "[]";
    }

    public Pbmethod.PbItem.Builder toProto() {
        Pbmethod.PbItem.Builder pb = Pbmethod.PbItem.newBuilder();
        pb.setId(id);
        pb.setItemKey(itemId);
        pb.setType(type);
        pb.setLevel(level);
        pb.setData(data == null ? "[]" : data);
        pb.setLockDestroy(lockDestroy == 1);
        pb.addAllPoint(getPointList());
        return pb;
    }

    public ItemType getResItemType() {
        ResItemEntity res = getRes();
        return res != null ? res.getItemType() : null;
    }

    public ResItemEntity getRes() {
        return ResItem.getItem(itemId);
    }

    public ResItemEquipmentEntity getResEquipment() {
        return ResItem.getItemEquipment(itemId);
    }

    public List<Long> getPointList() {
        if (point == null || point.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(GsonUtil.strToListLong(point));
    }

    public void setPointList(List<Long> points) {
        this.point = GsonUtil.toJson(points);
    }

    public void addLevel() {
        this.level++;
    }

    public void unEquip() {
        isEquip = false;
    }

    public boolean updateItemId(int idItem) {
        if (update(List.of("item_id", idItem))) {
            this.itemId = idItem;
            return true;
        }
        return false;
    }

    public List<Long> viewBonusItem(int type, long number) {
        if (number >= 0) return Bonus.viewItem(type, itemId, number);
        if (isAggregatedItem()) return Bonus.viewItemRemove(type, id, itemId, (int) -number);
        return Bonus.viewItem(type, itemId, number);
    }

    public boolean deleteFromDb() {
        return DBJPA.delete("user_item", "id", id, "user_id", userId);
    }

    public boolean update(List<Object> updateData) {
        return DBJPA.update("user_item", updateData, Arrays.asList("id", id));
    }
}
