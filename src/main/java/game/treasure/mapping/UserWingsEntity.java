package game.treasure.mapping;

import game.object.MyUser;
import game.treasure.mapping.main.ResWingsEntity;
import game.treasure.service.item.ProtoPetWingsWire;
import game.treasure.service.resource.ResWings;
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
@Table(name = "user_wings")
public class UserWingsEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;
    int userId;
    int wingsId;
    int level;
    int tier = 1;
    int priceTreasure;
    int isCraft;
    int hh;
    int icon;
    int server;
    int isTrading;
    int inMarket;
    String craftBy;
    String data;

    @Transient
    boolean isEquip;

    public UserWingsEntity(UserEntity user, int wingsId,int tier) {
        this.userId = user.getId();
        this.wingsId = wingsId;
        this.server = user.getServer();
        this.level = 1;
        this.tier = tier;
        this.isCraft = 0;
        this.hh = 0;
        this.priceTreasure = 0;
        this.icon = wingsId;
        this.data = getRes().getPointData(tier);
    }

    public static boolean isEquipped(MyUser mUser, long wingsRowId) {
        if (mUser == null || wingsRowId <= 0) return false;
        java.util.List<Integer> equip = mUser.getUser().normalizeItemEquipList();
        int idx = UserEntity.equipSlotIndex(protocol.Pbmethod.EquipSlotType.WINGS.getNumber());
        return idx >= 0 && idx < equip.size() && equip.get(idx) == (int) wingsRowId;
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

    public ResWingsEntity getRes() {
        return ResWings.get(wingsId);
    }

    public protocol.Pbmethod.PbWings.Builder toProtoBuilder() {
        protocol.Pbmethod.PbWings.Builder pb = protocol.Pbmethod.PbWings.newBuilder();
        pb.setId(id);
        pb.setWingsId(wingsId);
        pb.setLevel(level);
        pb.setTier(tier > 0 ? tier : 1);
        pb.setIsCraft(isCraft);
        pb.setHh(hh);
        pb.setPriceTreasure(priceTreasure);
        if (icon > 0)
            pb.setIcon(icon);
        if (craftBy != null && !craftBy.isEmpty())
            pb.setCraftBy(craftBy);
        return pb;
    }

    public protocol.Pbmethod.PbWings toProto() {
        try {
            byte[] bytes = ProtoPetWingsWire.appendDataAndIsEquip(
                    toProtoBuilder().build().toByteArray(), data, isEquip);
            bytes = game.treasure.service.item.ProtoTradingWire.appendPetWingsTrading(bytes, isTrading, inMarket);
            return protocol.Pbmethod.PbWings.parseFrom(bytes);
        } catch (Exception ex) {
            return toProtoBuilder().build();
        }
    }

    public boolean update(List<Object> lst) {
        return DBJPA.update("user_wings", lst, List.of("id", id));
    }

    public boolean deleteFromDb() {
        return DBJPA.delete("user_wings", "id", id, "user_id", userId);
    }
}
