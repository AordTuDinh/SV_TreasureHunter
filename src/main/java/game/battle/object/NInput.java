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
    public static final int USE_ITEM = 4;
    public static final int ADD_ZONE_HEATH = 5;
    public static final int REMOVE_ZONE_HEATH = 6;
    //endregion
    //

    public int seq;
    public int typeId, skillIndex;
    public Pos playerPos;
    public Pos playerDirection;
    // attack
    public Pbmethod.TargetAttack targetAttack;
    public long idAttack;
    /** CLIENT_INPUT USE_ITEM — user_item.id */
    public long useItemId;

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
        } else if (obj.typeId == USE_ITEM) {
            obj.useItemId = buffer.readLong();
        }
        return obj;
    }
}
