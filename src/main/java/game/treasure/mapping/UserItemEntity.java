package game.treasure.mapping;

import game.battle.object.Point;
import game.config.CfgItem;
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
    int type; //1=consumable, 2=equipment, 3=currency, 4=event
    int level;
    int lockDestroy;
    int tier;
    int slot;
    String data;

    @Transient
    boolean isEquip;
    @Transient
    int countWin;
    @Transient
    List<Long> point;


    public UserItemEntity(int userId, int itemId, ItemType type) {
        this.userId = userId;
        this.itemId = itemId;
        this.type = type.value;
        level = 1;
        slot = -1;
        lockDestroy = 0;
        tier = 1;
        data = "[]";
    }


    public boolean isEquipment() {
        return type == EQUIPMENT.value;
    }

    public boolean isAggregatedItem() {
        if (type != CONSUMABLE.value) return false;
        return itemId == Pbmethod.ItemKey.TICKER_NORMAL.getNumber() || itemId == Pbmethod.ItemKey.TICKER_SPECIAL.getNumber();
    }

    public List<Long> getPoint() {
        if (type == EVENT.value || type == CURRENCY.value)
            return null;
        if (point == null)
            rebuildPointCache();
        return point;
    }

    /**
     * Xóa cache point — gọi khi level/data đổi; lần getPoint() sau sẽ tính lại.
     */
    public void invalidatePointCache() {
        point = null;
    }

    void rebuildPointCache() {
        List<Float> raw = getDataListFloat();
        point = new ArrayList<>(raw.size());
        if (raw.isEmpty())
            return;
        int itemLevel = level > 0 ? level : 1;
        for (int i = 0; i + 1 < raw.size(); i += 2) {
            int pointId = Math.round(raw.get(i));
            float base = raw.get(i + 1);
            float scaled = CfgItem.formatPointStat(pointId, CfgItem.getStatAtLevel(base, itemLevel));
            point.add((long) pointId);
            point.add((long) scaled);
        }
    }

    public void setLevel(int level) {
        if (this.level == level)
            return;
        this.level = level;
        invalidatePointCache();
    }

    public void setData(String data) {
        this.data = data;
        invalidatePointCache();
    }


    /**
     * Chỉ số hiệu dụng tại level item (đọc từ cache point).
     */
    public float readEffectivePointValue(int pointId) {
        List<Long> pts = getPoint();
        if (pts == null)
            return 0f;
        for (int i = 0; i + 1 < pts.size(); i += 2) {
            if (pts.get(i).intValue() == pointId)
                return pts.get(i + 1).floatValue();
        }
        return 0f;
    }

    public float readHpBonus() {
        return readEffectivePointValue(Point.HP);
    }

    public boolean hasUpgradePointData() {
        if (data == null || data.length() < 2)
            return false;
        List<Float> list = GsonUtil.strToListFloat(data);
        if (list.size() >= 2) {
            for (int i = 0; i + 1 < list.size(); i += 2) {
                if (list.get(i + 1) > 0f)
                    return true;
            }
            return false;
        }
        return list.size() == 1 && list.get(0) > 0f;
    }

    public List<Float> getDataListFloat() {
        if (data == null || data.isEmpty() || "[]".equals(data))
            return new ArrayList<>();
        return GsonUtil.strToListFloat(data);
    }

    public List<Long> getDataListLong() {
        List<Float> raw = getDataListFloat();
        List<Long> result = new ArrayList<>(raw.size());
        for (Float f : raw)
            result.add(f != null ? Math.round(f) : 0L);
        return result;
    }

    public void clearAggregated() {
        setData("[]");
    }

    public Pbmethod.PbItem.Builder toProto() {
        Pbmethod.PbItem.Builder pb = Pbmethod.PbItem.newBuilder();
        pb.setId(id);
        pb.setItemKey(itemId);
        pb.setType(type);
        pb.setLevel(level);
        pb.setLockDestroy(lockDestroy == 1);
        pb.setTier(tier);
        pb.setSlot(slot);
        if (data != null && !data.isEmpty() && !"[]".equals(data))
            pb.setData(data);
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


    public void unEquip() {
        isEquip = false;
    }

    public boolean updateSlot(int newSlot) {
        if (update(List.of("slot", newSlot))) {
            this.slot = newSlot;
            return true;
        }
        return false;
    }

    public List<Long> viewBonusItem(int type, long number) {
        if (number >= 0) return Bonus.viewItem(type, itemId, number);
        if (isAggregatedItem()) return Bonus.viewItemRemove(type, id, itemId, tier, (int) -number);
        return Bonus.viewItem(type, itemId, number);
    }

    public boolean deleteFromDb() {
        return DBJPA.delete("user_item", "id", id, "user_id", userId);
    }

    public boolean update(List<Object> updateData) {
        return DBJPA.update("user_item", updateData, Arrays.asList("id", id));
    }
}
