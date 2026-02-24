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
    //region old
    public static final int NONE = -1;
    public static final int Right = 0;
    public static final int RightUp = 1;
    public static final int UpRight = 2;
    public static final int Up = 3;
    public static final int UpLeft = 4;
    public static final int LeftUp = 5;
    public static final int Left = 6;
    public static final int LeftDown = 7;
    public static final int DownLeft = 8;
    public static final int Down = 9;
    public static final int DownRight = 10;
    public static final int RightDown = 11;
    public static final int Skill1 = 12;
    public static final int Skill2 = 13;
    public static final int Skill3 = 14;
    public static final int Skill4 = 15;
    public static final int Skill5 = 16;

    public static final int offsetSkill = 12;
    public static List<Integer> lstSkillKey = Arrays.asList(Skill1, Skill2, Skill3, Skill4, Skill5);
    public static List<Integer> lstSkillIndex = Arrays.asList(0, 1, 2, 3, 4);
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
    // mode 2
    public List<Byte> keys = new ArrayList<>();


    public static NInput parse(byte[] data) {
        NInput obj = new NInput();
        ByteBuf buffer = Unpooled.wrappedBuffer(data);
        obj.typeId = buffer.readByte();
        if (obj.typeId == INPUT_PLAYER_MOVE) {
            obj.seq = buffer.readInt();
            obj.clientTime = buffer.readInt();
            int left = buffer.readableBytes();
            // 17-byte legacy: seq+clientTime(8) + pos(4) + dir(4) = 16 bytes after typeId. Server ignores pos.
            if (left >= 16) {
                buffer.readShort();
                buffer.readShort();
                float dx = buffer.readShort() / 1000f;
                float dy = buffer.readShort() / 1000f;
                obj.playerDirection = new Pos(dx, dy).normalized();
            }
            // 13-byte (direction only): seq+clientTime(8) + dir(4) = 12 bytes. Server simulates position.
            else if (left >= 4) {
                float dx = buffer.readShort() / 1000f;
                float dy = buffer.readShort() / 1000f;
                obj.playerDirection = new Pos(dx, dy).normalized();
            }
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

//    public static final int Idle = 30;
    public static final Map<Integer, Pos[]> mapInput = new HashMap<>() {{
        put(Right, new Pos[]{Pos.right()});
        put(RightUp, new Pos[]{Pos.right(), Pos.up()});
        put(UpRight, new Pos[]{Pos.up(), Pos.right()});
        put(Up, new Pos[]{Pos.up()});
        put(UpLeft, new Pos[]{Pos.up(), Pos.left()});
        put(LeftUp, new Pos[]{Pos.left(), Pos.up()});
        put(Left, new Pos[]{Pos.left()});
        put(LeftDown, new Pos[]{Pos.left(), Pos.down()});
        put(DownLeft, new Pos[]{Pos.down(), Pos.left()});
        put(Down, new Pos[]{Pos.down()});
        put(DownRight, new Pos[]{Pos.down(), Pos.right()});
        put(RightDown, new Pos[]{Pos.right(), Pos.down()});
    }};

    public static final Map<Pos[], Integer> mapInputType = new HashMap<>() {{
        put(new Pos[]{Pos.right()}, Right);
        put(new Pos[]{Pos.right(), Pos.up()}, RightUp);
        put(new Pos[]{Pos.up(), Pos.right()}, UpRight);
        put(new Pos[]{Pos.up()}, Up);
        put(new Pos[]{Pos.up(), Pos.left()}, UpLeft);
        put(new Pos[]{Pos.left(), Pos.up()}, LeftUp);
        put(new Pos[]{Pos.left()}, Left);
        put(new Pos[]{Pos.left(), Pos.down()}, LeftDown);
        put(new Pos[]{Pos.down(), Pos.left()}, DownLeft);
        put(new Pos[]{Pos.down()}, Down);
        put(new Pos[]{Pos.down(), Pos.right()}, DownRight);
        put(new Pos[]{Pos.right(), Pos.down()}, RightDown);
    }};

}
