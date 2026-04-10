package game.protocol;

import com.google.protobuf.ByteString;
import game.battle.type.UnitType;
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

    public static ByteString protoListCharacterState(List<PbUnitState> characterState) {
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

    public static byte[] convertProtoBuffToState(PbState proto) {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeFloat(proto.getServerTime());
        // region add
        parsePbUnitAdd(buffer, proto.getUnitAddList());
        parsePbUnitPos(buffer, proto.getUnitPosList());
        parsePbUnitUpdate(buffer, proto);
        parsePbChunkState(buffer, proto.getChunkStateList());
        // endregion
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        return bytes;
    }

    private static void parsePbChunkState(ByteBuf buffer, List<PbChunk> chunkStateList) {
        try {
            if (!chunkStateList.isEmpty()) {
                buffer.writeByte(StateType.CHUNK_STATE);
                buffer.writeByte(chunkStateList.size());
                for (int i = 0; i < chunkStateList.size(); i++) {
                    PbChunk tmp = chunkStateList.get(i);
                    buffer.writeInt(tmp.getId());
                    boolean isAdd = tmp.getIsAdd();
                    if(isAdd){
                        buffer.writeFloat(tmp.getPos().getX());
                        buffer.writeFloat(tmp.getPos().getY());

                        int sizeChunk =  tmp.getCellsCount();
                        buffer.writeInt(sizeChunk);
                        for (int j = 0; j < sizeChunk; j++) {
                            PbCell cell = tmp.getCells(j);
                            buffer.writeByte(cell.getType());
                            buffer.writeInt(cell.getChunkId());
                            buffer.writeFloat(cell.getPos().getX());
                            buffer.writeFloat(cell.getPos().getY());
                            // cell state
                            buffer.writeByte(cell.getState().getNumber());
                        }

                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("parsePbUnitAdd---->" + ex.getMessage());
        }
    }

    public static void parsePbUnitAdd(ByteBuf buffer, List<PbUnit> aUnitAdd) {
        try {
            if (!aUnitAdd.isEmpty()) {
                buffer.writeByte(StateType.TYPE_ADD_REMOVE_VALUE);
                buffer.writeByte(aUnitAdd.size());
                for (int i = 0; i < aUnitAdd.size(); i++) {
                    PbUnit tmp = aUnitAdd.get(i);
                    buffer.writeByte(tmp.getType());
                    buffer.writeByte(tmp.getChunkId());
                    buffer.writeLong(tmp.getId());
                    buffer.writeBoolean(tmp.getIsAdd());
                    // riêng add
                    if (tmp.getIsAdd()) {
                        buffer.writeInt(tmp.getTeamId());
                        buffer.writeInt(tmp.getAvatar());
                        buffer.writeLong(tmp.getOwnerId());
                        buffer.writeFloat(tmp.getPos().getX());
                        buffer.writeFloat(tmp.getPos().getY());
                        buffer.writeFloat(tmp.getDirection().getX());
                        buffer.writeFloat(tmp.getDirection().getY());
                        buffer.writeInt(tmp.getSpeed());
                        buffer.writeByte(tmp.getInfoCount());
                        for (int j = 0; j < tmp.getInfoCount(); j++) {
                            buffer.writeInt(tmp.getInfo(j));
                        }
                        buffer.writeFloat(tmp.getRangeAttack());

                        writeString(buffer, tmp.getName());
                        buffer.writeBoolean(tmp.getAlive());
                        buffer.writeLong(tmp.getLastInputSeq());
                        // point
                        buffer.writeByte(tmp.getPointCount());
                        for (int j = 0; j < tmp.getPointCount(); j++) {
                            buffer.writeInt(tmp.getPoint(j));
                        }
                        buffer.writeInt(tmp.getUserId());
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("parsePbUnitAdd---->" + ex.getMessage());
        }
    }

    public static void writeString(ByteBuf buffer, String str) {
        byte[] data;
        try {
            data = str.getBytes("UTF-8");
        } catch (Exception ex) {
            data = new byte[0];
        }
        buffer.writeByte(data.length);
        if (data.length > 0) {
            buffer.writeBytes(data);
        }
    }

    public static void parsePbUnitPos(ByteBuf buffer, List<PbUnitPos> aUnitPos) {
        if (!aUnitPos.isEmpty()) {
            buffer.writeByte(StateType.TYPE_POS_VALUE);
            buffer.writeByte(aUnitPos.size());
            
            for (int i = 0; i < aUnitPos.size(); i++) {
                PbUnitPos tmp = aUnitPos.get(i);
                buffer.writeLong(tmp.getId());
                buffer.writeInt(tmp.getSpeed());
                buffer.writeLong(tmp.getLastInputSeq());
                buffer.writeFloat(tmp.getPos().getX());
                buffer.writeFloat(tmp.getPos().getY());
                buffer.writeFloat(tmp.getDirection().getX());
                buffer.writeFloat(tmp.getDirection().getY());
            }
        }
    }

    public static void parsePbUnitUpdate(ByteBuf buffer, PbState proto) {
        int size = proto.getAUnitUpdateCount();
        for (int i = 0; i < size; i++) {
            try {
                PbUnitUpdate update = proto.getAUnitUpdate(i);
                switch (update.getType()) {
                    case StateType.TYPE_UNIT_STATE_VALUE:
                        PbListUnitState aPlayerState = PbListUnitState.parseFrom(update.getData(0).toByteArray());
                        parsePbUpdatePlayer(buffer, aPlayerState.getAUnitStateList());
                        break;
                }
            } catch (Exception ex) {
                System.err.println("Error at index " + i + ": " + ex);
            }
        }
    }

    public static void parsePbUpdatePlayer(ByteBuf buffer, List<PbUnitState> aCharacterState) {
        if (!aCharacterState.isEmpty()) {
            buffer.writeByte(StateType.TYPE_UNIT_STATE_VALUE);
            buffer.writeByte(aCharacterState.size());
            for (int i = 0; i < aCharacterState.size(); i++) {
                PbUnitState tmp = aCharacterState.get(i);
                buffer.writeLong(tmp.getId());
                buffer.writeByte(tmp.getStatusCount());
                for (int j = 0; j < tmp.getStatusCount(); j++) {
                    buffer.writeByte(tmp.getStatus(j));
                }
                for (int j = 0; j < tmp.getPointCount(); j++) {
                    buffer.writeInt(tmp.getPoint(j));
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
