package game.treasure.mapping;

import game.battle.object.Point;
import game.config.CfgItem;
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
    int level;
    int lockDestroy;
    int isCraft;
    int icon;
    String data;

    @Transient
    int bagSlot = -1;
    @Transient
    int countWin;
    @Transient
    List<Long> point;


    public UserItemEntity(int userId, int itemId) {
        this.userId = userId;
        this.itemId = itemId;
        level = 1;
        lockDestroy = 0;
        isCraft = 0;
        icon = ResItem.resolveIcon(itemId);
        data = "[]";
    }

    public UserItemEntity(int userId, int itemId, Pbmethod.ItemType type) {
        this(userId, itemId);
    }

    /** user_item.type — lấy từ res_item, không lưu tier trên row. */
    public int getType() {
        ResItemEntity res = getRes();
        return res != null && res.getItemType() != null ? res.getItemType().getNumber() : 0;
    }

    /** Tier consumable/event — lấy từ res_item.rank. */
    public int getTier() {
        ResItemEntity res = getRes();
        return res != null ? res.getTier() : 1;
    }


    public boolean isEquipment() {
        return false;
    }

    public List<Long> getPoint() {
        Pbmethod.ItemType type = getRes().getItemType();
        if (type == Pbmethod.ItemType.CURRENCY)
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

    public int getEffectiveIcon() {
        if (icon > 0)
            return icon;
        ResItemEntity res = getRes();
        if (res != null && res.getIcon() > 0)
            return res.getIcon();
        return itemId;
    }

    public Pbmethod.PbItem.Builder toProto() {
        Pbmethod.PbItem.Builder pb = Pbmethod.PbItem.newBuilder();
        pb.setId(id);
        pb.setItemKey(itemId);
        pb.setLevel(level);
        pb.setLockDestroy(lockDestroy == 1);
        pb.setIsCraft(isCraft);
        pb.setIcon(getEffectiveIcon());
        if (data != null && !data.isEmpty() && !"[]".equals(data))
            pb.setData(data);
        return pb;
    }

    public Pbmethod.ItemType getResItemType() {
        ResItemEntity res = getRes();
        return res != null ? res.getItemType() : null;
    }

    public ResItemEntity getRes() {
        return ResItem.getItem(itemId);
    }

    public ResItemEquipmentEntity getResEquipment() {
        return ResItem.getItemEquipment(itemId);
    }



    public List<Long> viewBonusItem(long number) {
        return Bonus.viewItem(itemId, number);
    }

    public boolean deleteFromDb() {
        return DBJPA.delete("user_item", "id", id, "user_id", userId);
    }

    public boolean update(List<Object> updateData) {
        return DBJPA.update("user_item", updateData, Arrays.asList("id", id));
    }
}
