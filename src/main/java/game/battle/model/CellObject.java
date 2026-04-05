package game.battle.model;

import game.battle.object.Pos;
import lombok.Data;
import protocol.Pbmethod;


@Data
public class CellObject {
    Pos pos;
    int type;
    int chunkId;
    Pbmethod.CellState state;


    public CellObject(Pos pos, int type, int chunkId, Pbmethod.CellState state) {
        this.pos = pos;
        this.type = type;
        this.chunkId = chunkId;
        this.state = state;
    }

    public Pbmethod.PbCell toProto() {
        Pbmethod.PbCell.Builder pb = Pbmethod.PbCell.newBuilder();
        pb.setType(type);
        pb.setCellId(chunkId);
        pb.setPos(pos.toProto());
        pb.setState(state);
        return pb.build();
    }
}
