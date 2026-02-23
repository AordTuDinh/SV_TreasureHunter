package game.dragonhero.mapping;


import game.config.aEnum.TopType;
import game.dragonhero.service.Services;
import game.monitor.Online;
import game.object.MyUser;
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
    String name, username, intro;
    int level, vip;
    int clan, clanRank, clanPosition, clanAvatar;
    long gold, gem, number, power;
    String avatar, clanName, weapon, itemEquipment;

    public List<Integer> getListAvatar() {
        List<Integer> avatars = GsonUtil.strToListInt(avatar);
        while (avatars.size() < 5) avatars.add(0);
        return avatars;
    }

    public protocol.Pbmethod.PbUser toProto(int rank, TopType topType) {
        protocol.Pbmethod.PbUser.Builder pb = protocol.Pbmethod.PbUser.newBuilder();
        pb.setId(id);
        pb.setUsername(username);
        pb.setName(getName());
        pb.setGold(gold);
        pb.setGem(gem);
        pb.setLevel(level);
        pb.addAllAvatar(getListAvatar());
        pb.addVip(vip);
        pb.setRank(rank);
        pb.setDesc(intro);
        pb.addAllWeaponEquip(GsonUtil.strToListInt(weapon));
        pb.addAllItemEquip(GsonUtil.strToListInt(itemEquipment));
        pb.setPower(power);
        pb.setClanInfo(protocol.Pbmethod.CommonVector.newBuilder().addAString(clanName).addALong(clan).addALong(clanPosition).addALong(clanRank).addALong(clanAvatar).build());
        pb.setInfo(protocol.Pbmethod.CommonVector.newBuilder().addALong(number).build());
        // rank
        if (topType == TopType.USER_POWER) pb.setPointRank(power);
        else if (topType == TopType.USER_LEVEL) pb.setPointRank(level);
        else if (topType == TopType.TOWER_LEVEL) pb.setPointRank(number);
        else if (topType == TopType.ARENA) {
            pb.setPointRank(number);
            UserArenaEntity uArena = Online.getDbUserArena(id);
            if (uArena != null) uArena.setRank(rank);
        } else {
            pb.setPointRank(number);
        }
        return pb.build();
    }



}
