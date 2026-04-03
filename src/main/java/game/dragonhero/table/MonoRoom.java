package game.dragonhero.table;

import game.battle.object.Coroutine;
import game.battle.object.Mono;
import game.battle.type.RoomState;
import game.battle.type.StateType;
import game.config.aEnum.MapType;
import game.dragonhero.server.Constans;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.quartz.JobKey;
import protocol.Pbmethod.*;

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
    protected long battleId; // id room khác với id init
    List<Coroutine> coroutines;
    MapType mapType;


    // region proto
    @Getter
    PbInitMap.Builder pbInit;
    @Getter
    List<PbUnit.Builder> aProtoAdd;
    @Getter
    List<PbUnitState.Builder> aProtoUnitState;


    public MonoRoom(String keyRoom) {
        this.roomState = RoomState.INIT;
        this.timeCreateRoom = System.currentTimeMillis();
        this.coroutines = new ArrayList<>();
        this.battleId = Constans.getCounterId();
        this.mapType = MapType.get(Integer.parseInt(Constans.getKeyRoomById(this.battleId)[1]));
        this.aProtoAdd = new ArrayList<>();
        this.aProtoUnitState = new ArrayList<>();
        this.pbInit = protocol.Pbmethod.PbInitMap.newBuilder();
        Constans.mIdToBattleId.put(this.battleId, keyRoom);
    }


    public void addCoroutine(Coroutine coroutine) {
        coroutines.add(coroutine);
    }


    public boolean isRoomType(MapType mapType) {
        return mapType == this.mapType;
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
