package game.treasure.mapping;

import game.treasure.mapping.main.ResItemEquipmentEntity;
import game.treasure.service.resource.ResItem;
import game.treasure.service.resource.ResPointEquipment;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.StringHelper;

import javax.persistence.*;
import java.util.Arrays;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@Table(name = "user_item_equipment")
public class UserItemEquipmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;
    int userId, itemId, level, isLock, lockDestroy;
    int bless; // phúc lành
    long expire;
    String point;// [mainId,point,addx100,subId,point,addx100...]
    @Transient
    int heroIdEquip;

    public UserItemEquipmentEntity(int userId, int itemId, long expire, int isLock) {
        this.userId = userId;
        this.itemId = itemId;
        this.level = 0;
        this.isLock = isLock;
        this.bless = 0;
        ResItemEquipmentEntity res = getRes();
        this.point = StringHelper.toDBString(ResPointEquipment.genItemEquipData(res));
        if (expire > -1) expire = System.currentTimeMillis() / 1000 + expire;
        this.expire = expire;
        this.lockDestroy = 0;
    }

    public boolean isLock() {
        return this.isLock == 1;
    }


    public boolean isEquip() {
        return heroIdEquip > 0;
    }

    public void equip( int heroIdEquip) {
        this.heroIdEquip = heroIdEquip;
    }

    public void unEquip() {
        setHeroIdEquip(0);
    }

    public List<Integer> getPoint() {
        return GsonUtil.strToListInt(point);
    }

    public List<Long> getPointLong() {
        return GsonUtil.strToListLong(point);
    }

    public ResItemEquipmentEntity getRes() {
        return ResItem.getItemEquipment(itemId);
    }

    public boolean isMaxLevel() {
        return level >= getRes().getMaxLevel();
    }

    public void addLevel() {
        this.level++;
    }

    public boolean isForever() {
        return expire == -1;
    }

    public boolean hasExpire() {
        if (expire == -1) return true;
        return System.currentTimeMillis() / 1000 < expire;
    }

    public boolean updateUpLevel(List<Integer> point) {
        if (update(Arrays.asList("point", StringHelper.toDBString(point), "level", level + 1, "bless", 0))) {
            this.point = point.toString();
            this.level++;
            this.bless = 0;
            return true;
        }
        return false;
    }

    public void addBless() {
        if (update(List.of("bless", bless + 1))) bless++;
    }

    public boolean updateMainPoint(String point, int itemId) {
        if (update(Arrays.asList("point", point, "level", 0, "item_id", itemId))) {
            this.point = point;
            this.itemId = itemId;
            this.level = 0;
            return true;
        }
        return false;
    }

    public boolean updateItemId(int idItem) {
        if (update(List.of("item_id", idItem))) {
            this.itemId = idItem;
            return true;
        }
        return false;
    }

    public boolean updateNextItem(int idItem, int level, String point) {
        if (update(List.of("item_id", idItem, "level", level, "point", point))) {
            this.level = level;
            this.itemId = idItem;
            this.point = point;
            return true;
        }
        return false;
    }

    public boolean update(List<Object> updateData) {
        return DBJPA.update("user_item_equipment", updateData, Arrays.asList("id", id));
    }

    public protocol.Pbmethod.PbItemEquipment.Builder toProto() {
        protocol.Pbmethod.PbItemEquipment.Builder pb = protocol.Pbmethod.PbItemEquipment.newBuilder();
        pb.setId(id);
        pb.setItemKey(itemId);
        pb.setLevel(level);
        pb.setExpire(expire);
        pb.addAllPoint(getPoint());
        pb.setLock(this.getIsLock() == 1);
        pb.setLockDestroy(lockDestroy == 1);
        pb.setBless(bless);
        return pb;
    }
}
