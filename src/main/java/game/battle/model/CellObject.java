package game.battle.model;

import game.battle.object.Pos;
import game.treasure.mapping.main.ResMapEntity;
import lombok.Data;
import protocol.Pbmethod;

import javax.persistence.Transient;

import static game.treasure.BattleConfig.CHUNK_SIZE;


@Data
public class CellObject {
    int id;
    Pos pos;
    int chunkId;
    Pbmethod.CellState state;
    Pbmethod.CellObjectType objectType;

    public CellObject(Pos pos, int type, int chunkId, Pbmethod.CellState state,ResMapEntity map ) {
        this.pos = pos;
        this.chunkId = chunkId;
        this.state = state;
        this.objectType = Pbmethod.CellObjectType.valueOf(type);
        this.id = MapService.worldPosToGlobalCellId(map, pos);
    }


    public Pbmethod.PbCell toProto() {
        Pbmethod.PbCell.Builder pb = Pbmethod.PbCell.newBuilder();
        pb.setId(id);
        pb.setType(objectType.getNumber());
        pb.setChunkId(chunkId);
        pb.setPos(pos.toProto());
        pb.setState(state);
        return pb.build();
    }
}
