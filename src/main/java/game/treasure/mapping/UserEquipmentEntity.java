package game.treasure.mapping;

import game.battle.object.Point;
import game.config.CfgItem;
import game.treasure.mapping.main.ResItemEntity;
import game.treasure.mapping.main.ResItemEquipmentEntity;
import game.treasure.service.resource.ResItem;
import game.treasure.service.user.Bonus;
import protocol.Pbmethod;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.GsonUtil;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "user_equipment")
public class UserEquipmentEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;
    int userId;
    int itemId;
    int level;
    int lockDestroy;
    int tier;
    int isCraft;
    int icon;
    int priceTreasure;
    String craftBy;
    String data;
    int isTrading;
    int inMarket;

    @Transient
    boolean isEquip;
    @Transient
    int bagSlot = -1;
    @Transient
    List<Long> point;

    public UserEquipmentEntity(int userId, int itemId) {
        this.userId = userId;
        this.itemId = itemId;
        level = 1;
        lockDestroy = 0;
        priceTreasure = 0;
        tier = 1;
        isCraft = 0;
        icon = itemId;
        data = "[]";
    }

    public List<Long> getPoint() {
        if (point == null)
            rebuildPointCache();
        return point;
    }

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

    public Pbmethod.PbEquipment.Builder toProto() {
        Pbmethod.PbEquipment.Builder pb = Pbmethod.PbEquipment.newBuilder();
        pb.setId(id);
        pb.setItemKey(itemId);
        pb.setLevel(level);
        pb.setLockDestroy(lockDestroy == 1);
        pb.setTier(tier);
        pb.setIsCraft(isCraft);
        pb.setIcon(icon);
        pb.setPriceTreasure(priceTreasure);
        if (data != null && !data.isEmpty() && !"[]".equals(data))
            pb.setData(data);
        if (craftBy != null && !craftBy.isEmpty())
            pb.setCraftBy(craftBy);
        return pb;
    }

    public Pbmethod.PbEquipment toProtoWire() {
        try {
            byte[] bytes = toProto().build().toByteArray();
            bytes = game.treasure.service.item.ProtoTradingWire.appendEquipmentTrading(bytes, isTrading, inMarket);
            return Pbmethod.PbEquipment.parseFrom(bytes);
        } catch (Exception ex) {
            return toProto().build();
        }
    }

    public ResItemEquipmentEntity getResEquipment() {
        return ResItem.getItemEquipment(itemId);
    }

    public ResItemEntity getRes() {
        return ResItem.getItem(itemId);
    }

    public void unEquip() {
        isEquip = false;
    }


    public boolean deleteFromDb() {
        return DBJPA.delete("user_equipment", "id", id, "user_id", userId);
    }

    public boolean update(List<Object> updateData) {
        return DBJPA.update("user_equipment", updateData, Arrays.asList("id", id));
    }
}
