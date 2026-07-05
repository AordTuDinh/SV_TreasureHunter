package game.treasure.mapping;

import game.object.MyUser;
import game.treasure.mapping.main.ResMountEntity;
import game.treasure.service.item.ProtoPetMountWire;
import game.treasure.service.resource.ResMount;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.GsonUtil;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@Table(name = "user_mount")
public class UserMountEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;
    int userId;
    int mountId;
    int level;
    int tier = 1;
    int priceTreasure;
    int isCraft;
    int icon;
    int server;
    String craftBy;
    String data;

    @Transient
    boolean isEquip;

    public UserMountEntity(UserEntity user, int mountId,int tier) {
        this.userId = user.getId();
        this.mountId = mountId;
        this.server = user.getServer();
        this.level = 1;
        this.tier = tier;
        this.isCraft = 0;
        this.priceTreasure = 0;
        this.icon = mountId;
        this.data = getRes().getPointData(tier);
    }

    public static boolean isEquipped(MyUser mUser, long mountRowId) {
        if (mUser == null || mountRowId <= 0) {
            return false;
        }
        List<Integer> equip = mUser.getUser().normalizeItemEquipList();
        int idx = UserEntity.equipSlotIndex(protocol.Pbmethod.EquipSlotType.MOUNT.getNumber());
        return idx >= 0 && idx < equip.size() && equip.get(idx) == (int) mountRowId;
    }

    public void syncEquipFlag(MyUser mUser) {
        isEquip = isEquipped(mUser, id);
    }

    public List<Float> getDataListFloat() {
        if (data == null || data.isEmpty() || "[]".equals(data))
            return new java.util.ArrayList<>();
        return GsonUtil.strToListFloat(data);
    }

    public void setData(String data) {
        this.data = data;
    }

    public ResMountEntity getRes() {
        return ResMount.get(mountId);
    }

    public protocol.Pbmethod.PbMount.Builder toProtoBuilder() {
        protocol.Pbmethod.PbMount.Builder pb = protocol.Pbmethod.PbMount.newBuilder();
        pb.setId(id);
        pb.setMountId(mountId);
        pb.setLevel(level);
        pb.setTier(tier > 0 ? tier : 1);
        pb.setIsCraft(isCraft);
        pb.setPriceTreasure(priceTreasure);
        if (icon > 0)
            pb.setIcon(icon);
        if (craftBy != null && !craftBy.isEmpty())
            pb.setCraftBy(craftBy);
        return pb;
    }

    public protocol.Pbmethod.PbMount toProto() {
        try {
            byte[] bytes = ProtoPetMountWire.appendDataAndIsEquip(
                    toProtoBuilder().build().toByteArray(), data, isEquip);
            return protocol.Pbmethod.PbMount.parseFrom(bytes);
        } catch (Exception ex) {
            return toProtoBuilder().build();
        }
    }

    public boolean update(List<Object> lst) {
        return DBJPA.update("user_mount", lst, List.of("id", id));
    }

    public boolean deleteFromDb() {
        return DBJPA.delete("user_mount", "id", id, "user_id", userId);
    }
}
