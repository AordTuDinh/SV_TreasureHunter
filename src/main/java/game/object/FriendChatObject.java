package game.object;


import game.treasure.mapping.UserEntity;
import lombok.Data;

import java.io.Serializable;

@Data
public class FriendChatObject implements Serializable {
    int id;
    String msg;
    long time;

    public FriendChatObject(UserEntity user, String msg) {
        this.id = user.getId();
        this.msg = msg;
        this.time = System.currentTimeMillis() / 1000;
    }

    public protocol.Pbmethod.PbChatFriend toProto(UserChatInfoObject info) {
        protocol.Pbmethod.PbChatFriend.Builder pb = protocol.Pbmethod.PbChatFriend.newBuilder();
        pb.setUserId(id);
        pb.setMessage(msg);
        pb.setTime(time);
        pb.setName(info.name);
        pb.addAllAvatar(info.avatar);
        pb.setLevel(info.level);
        return pb.build();
    }
}
