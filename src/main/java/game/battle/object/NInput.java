package game.battle.object;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import protocol.Pbmethod;

import java.util.*;

public class NInput {
    // TYPE ID 0->255 max
    public static final int INPUT_PLAYER_MOVE = 1;
    public static final int PING_GAME = 2;
    public static final int ATTACK = 3;
    //endregion
    //

    public int seq;
    public int typeId, skillIndex;
    public Pos playerPos;
    public Pos playerDirection;
    public Pbmethod.TargetAttack targetAttack;
    public long idAttack;

    public static NInput parse(byte[] data) {
        NInput obj = new NInput();
        ByteBuf buffer = Unpooled.wrappedBuffer(data);
        obj.typeId = buffer.readByte();
        if (obj.typeId == INPUT_PLAYER_MOVE) {
            obj.seq = buffer.readInt();
            float x1 = buffer.readShort() / 100f;
            float y1 = buffer.readShort() / 100f;
            float x2 = buffer.readShort() / 100f;
            float y2 = buffer.readShort() / 100f;
            obj.playerPos = new Pos(x1, y1).round();
            obj.playerDirection = new Pos(x2, y2).round();
        } else if (obj.typeId == ATTACK) {
            obj.targetAttack = Pbmethod.TargetAttack.valueOf(buffer.readByte());
            obj.idAttack = buffer.readLong();
        }
        return obj;
    }
}
