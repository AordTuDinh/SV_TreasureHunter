package game.battle.object;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import protocol.Pbmethod;

import java.util.*;

public class NInput {
    // TYPE ID 0->255 max
    public static final int INPUT_PLAYER_MOVE = 1;
    public static final int INPUT_SLOT = 2;
    public static final int PING_GAME = 3;
    public static final int CLIENT_STATE = 4;
    public static final int PET_MOVE = 5;
    //endregion
    //

    public int seq, clientTime;
    public int typeId, skillIndex;
    public Pos playerPos;
    public Pos playerDirection;
    public Pos petPos;
    public Pos petDirection;
    public Pos targetDirection;
    public int[] slotActive = new int[2];
    public Pbmethod.PbUnitState.Builder clientState;

    public static NInput parse(byte[] data) {
        NInput obj = new NInput();
        ByteBuf buffer = Unpooled.wrappedBuffer(data);
        obj.typeId = buffer.readByte();
        if (obj.typeId == INPUT_PLAYER_MOVE) {
            obj.seq = buffer.readInt();
            obj.clientTime = buffer.readInt();

            float x1 = buffer.readShort() / 1000f;
            float y1 = buffer.readShort() / 1000f;
            float x2 = buffer.readShort() / 1000f;
            float y2 = buffer.readShort() / 1000f;
            obj.playerPos = new Pos(x1, y1).round();
            obj.playerDirection = new Pos(x2, y2).round();
        } else if (obj.typeId == PET_MOVE) {
            float x1 = buffer.readShort() / 1000f;
            float y1 = buffer.readShort() / 1000f;
            float x2 = buffer.readShort() / 1000f;
            float y2 = buffer.readShort() / 1000f;
            obj.petPos = new Pos(x1, y1).round();
            obj.petDirection = new Pos(x2, y2).round();
        } else if (obj.typeId == INPUT_SLOT) {
            obj.skillIndex = buffer.readByte();
            float x1 = buffer.readShort() / 1000f;
            float y1 = buffer.readShort() / 1000f;
            obj.targetDirection = new Pos(x1, y1);
            obj.slotActive[0] = buffer.readByte();
            obj.slotActive[1] = buffer.readByte();
        } else if (obj.typeId == CLIENT_STATE) {
            Pbmethod.PbUnitState.Builder pb = Pbmethod.PbUnitState.newBuilder();
            pb.setId(buffer.readByte());
            // status
            int size = buffer.readInt();
            List<Integer> status = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                status.add(buffer.readInt());
            }
            pb.addAllStatus(status);
            // data
            size = buffer.readInt();
            List<Long> point = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                point.add((long) buffer.readInt());
            }
            pb.addAllPoint(point);
            obj.clientState = pb;
        }
        return obj;
    }
}
