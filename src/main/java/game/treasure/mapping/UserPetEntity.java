package game.treasure.mapping;

import game.object.MyUser;
import game.treasure.mapping.main.ResPetEntity;
import game.treasure.service.item.ProtoPetMountWire;
import game.treasure.service.resource.ResPet;
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
@Table(name = "user_pet")
public class UserPetEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;
    int userId;
    int petId;
    int level;
    int tier = 1;
    int isCraft;
    int hh;
    int icon;
    int server;
    int priceTreasure;
    int isTrading;
    int inMarket;
    String craftBy;
    String data;

    @Transient
    boolean isEquip;

    public UserPetEntity(UserEntity user, int petId,int tier) {
        this.userId = user.getId();
        this.server = user.getServer();
        this.petId = petId;
        this.level = 1;
        this.tier = tier;
        this.isCraft = 0;
        this.hh = 0;
        this.priceTreasure = 0;
        this.icon = petId;
        this.data = getResPet().getPointData(tier);
    }

    public static boolean isEquipped(MyUser mUser, long petRowId) {
        if (mUser == null || petRowId <= 0) {
            return false;
        }
        List<Integer> equip = mUser.getUser().normalizeItemEquipList();
        int idx = UserEntity.equipSlotIndex(protocol.Pbmethod.EquipSlotType.PET.getNumber());
        return idx >= 0 && idx < equip.size() && equip.get(idx) == (int) petRowId;
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

    public ResPetEntity getResPet() {
        return ResPet.getPet(petId);
    }

    public protocol.Pbmethod.PbPet.Builder toProtoBuilder() {
        protocol.Pbmethod.PbPet.Builder pb = protocol.Pbmethod.PbPet.newBuilder();
        pb.setId(id);
        pb.setPetId(petId);
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

    public protocol.Pbmethod.PbPet toProto() {
        try {
            byte[] bytes = ProtoPetMountWire.appendDataAndIsEquip(
                    toProtoBuilder().build().toByteArray(), data, isEquip);
            bytes = game.treasure.service.item.ProtoTradingWire.appendPetMountTrading(bytes, isTrading, inMarket);
            return protocol.Pbmethod.PbPet.parseFrom(bytes);
        } catch (Exception ex) {
            return toProtoBuilder().build();
        }
    }

    public boolean update(List<Object> lst) {
        return DBJPA.update("user_pet", lst, List.of("id", id));
    }

    public boolean deleteFromDb() {
        return DBJPA.delete("user_pet", "id", id, "user_id", userId);
    }
}
