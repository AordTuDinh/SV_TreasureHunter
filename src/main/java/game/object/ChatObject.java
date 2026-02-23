package game.object;

import game.config.aEnum.ChatType;
import game.dragonhero.mapping.UserEntity;
import game.monitor.Online;
import lombok.Data;
import ozudo.base.helper.GsonUtil;
import protocol.Pbmethod;

import java.io.Serializable;
import java.util.List;

@Data
public class ChatObject implements Serializable {
    public ChatType chatType;
    public String name, msg, username;
    int userId, id, clanId, level, clanIcon, vip, rank;
    List<Integer> avatar, pets, weapons, itemEquips;
    String intro;
    long timeSeconds, exp, power;


    int getNewId() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    public ChatObject(UserEntity user, String msg) {
        init(user, msg);
    }

    public protocol.Pbmethod.PbChat toProto() {
        protocol.Pbmethod.PbChat.Builder pb = protocol.Pbmethod.PbChat.newBuilder();
        pb.setReqTime(timeSeconds);
        pb.setMessage(msg);
        pb.setType(chatType.value);
        pb.setUser(toPbUser());
        return pb.build();
    }

    Pbmethod.PbUser toPbUser() {
        Pbmethod.PbUser.Builder pbUser = Pbmethod.PbUser.newBuilder();
        pbUser.setId(id);
        if (username != null) pbUser.setUsername(username);
        pbUser.setName(getName());
        pbUser.setLevel(level).setExp(exp);
        pbUser.addAllAvatar(getAvatar());
        pbUser.setDesc(intro);
        pbUser.setRank(rank);
        pbUser.setPower(getPower());
        pbUser.addAllPet(pets);
        pbUser.addAllWeaponEquip(weapons);
        pbUser.addAllItemEquip(itemEquips);
        pbUser.addAllChannel(Online.getUserChannelInfo(id));
        return pbUser.build();
    }


    void init(UserEntity user, String msg) {
        this.id = getNewId();
        this.vip = user.getVip();
        this.level = user.getLevel();
        this.userId = user.getId();
        this.msg = formatChat(msg);
        this.exp = user.getExp();
        this.intro = user.getIntro();
        this.timeSeconds = System.currentTimeMillis() / 1000;
        this.clanId = user.getClan();
        this.avatar = user.getAvatar();
        this.pets = GsonUtil.strToListInt(user.getPet());
        this.chatType = ChatType.MSG;
        this.rank = user.getUserRank();
        this.power = user.getPower();
        this.name = user.getName();
        this.username = user.getUsername();
        this.weapons = GsonUtil.strToListInt(user.getWeapon());
        this.itemEquips = GsonUtil.strToListInt(user.getItemEquipment());
    }

    private String formatChat(String msg) {
        msg = msg.replaceAll("\r", "");
        String[] tmp = msg.split("\n");
        int size = tmp.length < 3 ? tmp.length : 3;
        String output = tmp[0].trim();
        for (int i = 1; i < size; i++) {
            output += "\n" + tmp[i].trim();
        }
        return output;
    }
}
