package game.dragonhero.table;

import com.google.gson.Gson;
import game.battle.object.Coroutine;
import game.battle.object.Mono;
import game.battle.type.RoomState;
import game.battle.type.StateType;
import game.config.aEnum.RoomType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.quartz.JobKey;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor

public abstract class MonoRoom extends Mono {
    JobKey key1, key2, key3, key4, key5;
    public static int counterId = 0;
    @Getter
    RoomState roomState;
    @Getter
    long timeCreateRoom = 0;
    @Getter
    float local_time = 0.016f; // The local timer in seconds
    long _dte; // The local timer last frame time
    @Getter
    protected long id; // id room khác với id init
    List<Coroutine> coroutines;
    RoomType roomType;
    // region proto
    @Getter
    protocol.Pbmethod.PbInitRoom.Builder pbInit;
    @Getter
    List<protocol.Pbmethod.PbUnitAdd.Builder> aProtoAdd;
    @Getter
    List<protocol.Pbmethod.PbUnitState.Builder> aProtoUnitState;
    // endregion
    @Setter
    @Getter
    String keyRoom; // room_mapId_subId_chanelId
    @Setter
    @Getter
    String[] keys; // room_mapId_subId_chanelId


    public MonoRoom(String keyRoom) {
        this.roomState = RoomState.INIT;
        this.timeCreateRoom = System.currentTimeMillis();
        this.keyRoom = keyRoom;
        this.keys = keyRoom.split("_");
        this.roomType = RoomType.get(Integer.parseInt(keys[1]));
        this.coroutines = new ArrayList<>();
        this.id = getCounterId();
        this.aProtoAdd = new ArrayList<>();
        this.aProtoUnitState = new ArrayList<>();
        this.pbInit = protocol.Pbmethod.PbInitRoom.newBuilder();
    }


    public void addCoroutine(Coroutine coroutine) {
        coroutines.add(coroutine);
    }



    public boolean isRoomType(RoomType roomType) {
        return roomType == this.roomType;
    }

    //region state
    public static synchronized long getCounterId() {
        return ++counterId;
    }

    protocol.Pbmethod.PbUnitState.Builder protoState(int id, List<StateType> aStatus, List<Long> aInfo) {
        protocol.Pbmethod.PbUnitState.Builder builder = protocol.Pbmethod.PbUnitState.newBuilder();
        builder.setId(id);
        aStatus.forEach(status -> {
            builder.addStatus(status.id);
            builder.addStatus(status.length);
        });
        if (aInfo == null) aInfo = new ArrayList<>();
        builder.addAllPoint(aInfo);
        return builder;
    }

    protocol.Pbmethod.PbUnitState.Builder protoState(int id, List<StateType> aStatus, List<Integer> size, List<Long> aInfo) {
        protocol.Pbmethod.PbUnitState.Builder builder = protocol.Pbmethod.PbUnitState.newBuilder();
        builder.setId(id);
        for (int i = 0; i < aStatus.size(); i++) {
            builder.addStatus(aStatus.get(i).id);
            builder.addStatus(size.get(i));
        }
        if (aInfo == null) aInfo = new ArrayList<>();
        builder.addAllPoint(aInfo);
        return builder;
    }
    //endregion

}
