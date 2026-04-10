package game.battle.model;

import game.battle.object.Pos;
import lombok.Data;
import protocol.Pbmethod;

import java.util.ArrayList;
import java.util.List;


@Data
public class ChunkObject {
    int id;
    Pos pos;
    List<CellObject> cells;

    public ChunkObject(int id, Pos pos, List<CellObject> cells) {
        this.id = id;
        this.pos = pos;
        this.cells = cells;
    }

    public Pbmethod.PbChunk toProto() {
        Pbmethod.PbChunk.Builder pb = Pbmethod.PbChunk.newBuilder();
        pb.setId(id);
        pb.setIsAdd(true);
        pb.setPos(pos.toProto());
        for (CellObject cell : cells) {
            pb.addCells(cell.toProto());
        }
        return pb.build();
    }
    public Pbmethod.PbChunk toProtoRemove() {
        return Pbmethod.PbChunk.newBuilder().setIsAdd(false).setId(id).build();
    }
}
