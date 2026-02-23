package game.protocol;

import com.google.protobuf.ByteString;
import game.dragonhero.server.Constans;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import protocol.Pbmethod.*;

import java.util.List;

public class ProtoState {
    public static PbUnitUpdate.Builder protoUnitUpdate(int type, ByteString data) {
        PbUnitUpdate.Builder builder = PbUnitUpdate.newBuilder();
        builder.setType(type);
        builder.addData(data);
        return builder;
    }

    public static ByteString protoListCharacterState(List<PbUnitState.Builder> characterState) {
        PbListUnitState.Builder builder = PbListUnitState.newBuilder();
        int size = characterState.size();
        for (int i = 0; i < size; i++) {
            if (characterState.get(0) != null) {
                builder.addAUnitState(characterState.get(0));
                characterState.remove(0);
            }
        }
        return builder.build().toByteString();
    }

    public static byte[] convertProtoBuffToState(protocol.Pbmethod.PbState proto) {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeFloat(proto.getServerTime());
        // region add
        parsePbUnitAdd(buffer, proto.getUnitAddList());
        parsePbUnitPos(buffer, proto.getUnitPosList());
        parsePbUnitUpdate(buffer, proto);
        // endregion
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        return bytes;
    }

    public static void parsePbUnitAdd(ByteBuf buffer, List<PbUnitAdd> aUnitAdd) {
        try {
            if (aUnitAdd.size() > 0) {
                buffer.writeByte(Constans.TYPE_ADD_OR_REMOVE);
                buffer.writeByte(aUnitAdd.size());
                for (int i = 0; i < aUnitAdd.size(); i++) {
                    PbUnitAdd tmp = aUnitAdd.get(i);
                    buffer.writeByte(tmp.getType());
                    buffer.writeLong(tmp.getId());
                    buffer.writeBoolean(tmp.getIsAdd());

                    if (tmp.getIsAdd()) {
                        buffer.writeInt(tmp.getTeamId());
                        buffer.writeByte(tmp.getAvatarCount());
                        for (int j = 0; j < tmp.getAvatarCount(); j++) {
                            buffer.writeInt(tmp.getAvatar(j));
                        }
                        buffer.writeInt(tmp.getOwnerId());
                        buffer.writeFloat(tmp.getPos().getX());
                        buffer.writeFloat(tmp.getPos().getY());
                        buffer.writeFloat(tmp.getDirection().getX());
                        buffer.writeFloat(tmp.getDirection().getY());
                        buffer.writeInt(tmp.getSpeed());
                        buffer.writeInt(tmp.getFaction());
                        buffer.writeByte(tmp.getInfoCount());
                        for (int j = 0; j < tmp.getInfoCount(); j++) {
                            buffer.writeInt(tmp.getInfo(j));
                        }

                        if (tmp.getType() == Constans.TYPE_MONSTER || tmp.getType() == Constans.TYPE_PLAYER) {
                            PbCharInfo infor = tmp.getCharacterInfo();
                            buffer.writeInt(infor.getLevel());
                            buffer.writeBoolean(infor.getAlive());
                            //  buffer.writeLong(infor.getLastInputSeq());  có vẻ không cần đến
                            buffer.writeByte(infor.getPointCount());
                            for (int j = 0; j < infor.getPointCount(); j++) {
                                buffer.writeLong(infor.getPoint(j));
                            }
                            buffer.writeByte(infor.getInfoCount());
                            for (int j = 0; j < infor.getInfoCount(); j++) {
                                buffer.writeInt(infor.getInfo(j));
                            }
                            byte[] data;
                            try {
                                data = infor.getName().getBytes("UTF-8");
                            } catch (Exception ex) {
                                data = new byte[0];
                            }
                            buffer.writeByte(data.length);
                            if (data.length > 0) {
                                buffer.writeBytes(data);
                            }
                        }
                        buffer.writeFloat(tmp.getBotLeft().getX());
                        buffer.writeFloat(tmp.getBotLeft().getY());
                        buffer.writeFloat(tmp.getTopRight().getX());
                        buffer.writeFloat(tmp.getTopRight().getY());
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("parsePbUnitAdd---->" + ex.getMessage());
        }
    }

    public static void parsePbUnitPos(ByteBuf buffer, List<PbUnitPos> aUnitPos) {
        try {
            if (aUnitPos.size() > 0) {
                buffer.writeByte(Constans.TYPE_POS);
                buffer.writeByte(aUnitPos.size());
                for (int i = 0; i < aUnitPos.size(); i++) {
                    PbUnitPos tmp = aUnitPos.get(i);
                    buffer.writeInt(tmp.getId());
                    buffer.writeInt(tmp.getSpeed());
                    buffer.writeLong(tmp.getLastInputSeq());
                    buffer.writeFloat(tmp.getPos().getX());
                    buffer.writeFloat(tmp.getPos().getY());
                    buffer.writeFloat(tmp.getDirection().getX());
                    buffer.writeFloat(tmp.getDirection().getY());
                }
            }
        } catch (Exception ex) {
            System.out.println("parsePbUnitPos---->" + ex.getMessage());
        }
    }

    public static void parsePbUnitUpdate(ByteBuf buffer, protocol.Pbmethod.PbState proto) {
        try {
            int size = proto.getAUnitUpdateCount();
            for (int i = 0; i < size; i++) {
                PbUnitUpdate update = proto.getAUnitUpdate(i);
                switch (update.getType()) {
                    case Constans.TYPE_UPDATE_CHARACTER:
                        PbListUnitState aPlayerState = PbListUnitState.parseFrom(update.getData(0).toByteArray());
                        parsePbUpdatePlayer(buffer, aPlayerState.getAUnitStateList());
                        break;
                }
            }
        } catch (Exception ex) {
            System.out.println("parsePbUnitUpdate---->" + ex.getMessage());
        }
    }

    public static void parsePbUpdatePlayer(ByteBuf buffer, List<PbUnitState> aCharacterState) {
        if (aCharacterState.size() > 0) {
            buffer.writeByte(Constans.TYPE_UPDATE_CHARACTER);
            buffer.writeByte(aCharacterState.size());
            for (int i = 0; i < aCharacterState.size(); i++) {
                PbUnitState tmp = aCharacterState.get(i);
                buffer.writeInt(tmp.getId());
                buffer.writeByte(tmp.getStatusCount());
                for (int j = 0; j < tmp.getStatusCount(); j++) {
                    buffer.writeByte(tmp.getStatus(j));
                }
                //buffer.writeByte(tmp.getPointCount()); -> k can nua vi co size o tren roi
                for (int j = 0; j < tmp.getPointCount(); j++) {
                    buffer.writeLong(tmp.getPoint(j));
                }
            }
        }
    }

//    public static void parsePbUpdateBullet(ByteBuf buffer, List<PbWeapon> aBullet) {
//        if (aBullet.size() > 0) {
//            buffer.writeByte(Constans.TYPE_ADD_BULLET);
//            buffer.writeByte(aBullet.size());
//            for (int i = 0; i < aBullet.size(); i++) {
//                PbWeapon tmp = aBullet.get(i);
//                buffer.writeInt(tmp.getId());
//                buffer.writeFloat(tmp.getPos().getX());
//                buffer.writeFloat(tmp.getPos().getY());
//                buffer.writeByte(tmp.getInfoCount());
//                for (int j = 0; j < tmp.getInfoCount(); j++) {
//                    buffer.writeInt(tmp.getInfo(j));
//                }
//            }
//        }
//    }
}
