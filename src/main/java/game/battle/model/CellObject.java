package game.battle.model;

import game.battle.object.Pos;
import lombok.Data;
import protocol.Pbmethod;

import javax.persistence.Transient;


@Data
public class CellObject {
    Pos pos;
    int chunkId;
    Pbmethod.CellState state;
    Pbmethod.CellObjectType objectType;

    public CellObject(Pos pos, int type, int chunkId, Pbmethod.CellState state) {
        this.pos = pos;
        this.chunkId = chunkId;
        this.state = state;
        this.objectType = Pbmethod.CellObjectType.valueOf(type);
    }

    public Pbmethod.PbCell toProto() {
        Pbmethod.PbCell.Builder pb = Pbmethod.PbCell.newBuilder();
        pb.setType(objectType.getNumber());
        pb.setChunkId(chunkId);
        pb.setPos(pos.toProto());
        pb.setState(state);
        return pb.build();
    }
}
