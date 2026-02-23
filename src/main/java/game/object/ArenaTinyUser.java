package game.object;


import game.battle.object.Point;
import game.dragonhero.mapping.UserEntity;
import game.monitor.Online;
import lombok.Data;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.StringHelper;
import protocol.Pbmethod;
import java.io.Serializable;
import java.util.List;

@Data
public class ArenaTinyUser implements Serializable {
    int id;
    String name, intro;
    int clan, clanAvatar, clanRank, clanPosition;
    String clanName, pointData;
    String weapon;
    String itemEquipment;
    int level, vip, rank, activeArena, arenaPoint;
    long power;
    String avatar; // type-  avatarId - heroId - frame - skin


    public ArenaTinyUser(UserEntity user, int arenaPoint) {
        this.id = user.getId();
        this.name = user.getName();
        this.intro = user.getIntro();
        this.clan = user.getClan();
        this.clanAvatar = user.getClanAvatar();
        this.clanRank = user.getClanRank();
        this.clanPosition = user.getClanPosition();
        this.clanName = user.getClanName();
        this.pointData = user.getPointData();
        this.weapon = user.getWeapon();
        this.itemEquipment = user.getItemEquipment();
        this.level = user.getLevel();
        this.vip = user.getVip();
        this.rank = user.getUserRank();
        this.arenaPoint = arenaPoint;
        this.power = user.getPower();
        this.avatar = user.getAvatarString();
    }

    public List<Integer> getAvatar() {
        List<Integer> avatars = GsonUtil.strToListInt(avatar);
        while (avatars.size() < 5) avatars.add(0);
        return avatars;
    }

    public List<Integer> getAllInfoItemEquip() {
        return GsonUtil.strToListInt(itemEquipment);
    }

    public protocol.Pbmethod.PbUser toProto() {
        protocol.Pbmethod.PbUser.Builder pb = protocol.Pbmethod.PbUser.newBuilder();
        pb.setId(id);
        pb.setName(name);
        pb.setLevel(level).setExp(0);
        pb.addAllAvatar(getAvatar());
        pb.addVip(vip).addVip(0);
        pb.setDesc(intro);
        pb.setRank(rank);
        pb.setPointRank(arenaPoint);
//        pb.setArenaRank(arenaRank);
        pb.setPower(power);
        pb.addAllPoint(pointData == null ? new Point().toProto() : new Point(GsonUtil.strToListLong(pointData)).toProto());
        pb.addAllWeaponEquip(GsonUtil.strToListInt(weapon));
        pb.addAllItemEquip(getAllInfoItemEquip());
        pb.addAllChannel(Online.getUserChannelInfo(id));
        pb.setClanInfo(protocol.Pbmethod.CommonVector.newBuilder().addAString(clanName).addALong(clan).addALong(clanPosition).addALong(clanRank).addALong(clanAvatar).build());
        return pb.build();
    }

    @Override
    public String toString() {
        return StringHelper.toDBString(this);
    }
}
