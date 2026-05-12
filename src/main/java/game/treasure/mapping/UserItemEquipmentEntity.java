package game.treasure.mapping;

import game.treasure.mapping.main.ResItemEquipmentEntity;
import game.treasure.service.resource.ResItem;
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
    int userId, itemId, level,  lockDestroy;
    @Transient
    int heroIdEquip;

    public UserItemEquipmentEntity(int userId, int itemId) {
        this.userId = userId;
        this.itemId = itemId;
        this.level = 0;
        this.lockDestroy = 0;
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

    public ResItemEquipmentEntity getRes() {
        return ResItem.getItemEquipment(itemId);
    }


    public void addLevel() {
        this.level++;
    }


    public boolean updateItemId(int idItem) {
        if (update(List.of("item_id", idItem))) {
            this.itemId = idItem;
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
        pb.setLockDestroy(lockDestroy == 1);
        return pb;
    }
}
