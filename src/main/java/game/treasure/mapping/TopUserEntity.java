package game.treasure.mapping;


import game.config.aEnum.TopType;
import game.treasure.mapping.UserSkinEntity;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.List;

@Entity
@Data
@Table(name = "user")
@NoArgsConstructor
public class TopUserEntity implements Serializable {
    @Id
    int id;
    String name, username;
    int  vip;
    int clan, clanRank, clanPosition, clanAvatar;
    long gold, gem, number, power;
    String skins, clanName, itemEquipment;

    public List<Integer> getSkinsList() {
        return UserSkinEntity.normalize(GsonUtil.strToListInt(skins));
    }

    public protocol.Pbmethod.PbUser toProto(int rank, TopType topType) {
        protocol.Pbmethod.PbUser.Builder pb = protocol.Pbmethod.PbUser.newBuilder();
        pb.setId(id);
        pb.setUsername(username);
        pb.setName(getName());
        pb.setGold(gold);
        pb.setGem(gem);
        pb.addAllSkins(getSkinsList());
        pb.addVip(vip);
        pb.setRank(rank);
        pb.addAllItemEquip(GsonUtil.strToListInt(itemEquipment));
        pb.setPower(power);
        pb.setClanInfo(protocol.Pbmethod.CommonVector.newBuilder().addAString(clanName).addALong(clan).addALong(clanPosition).addALong(clanRank).addALong(clanAvatar).build());
        pb.setInfo(protocol.Pbmethod.CommonVector.newBuilder().addALong(number).build());
        // rank
        if (topType == TopType.USER_POWER) pb.setPointRank(power);
        return pb.build();
    }


}
