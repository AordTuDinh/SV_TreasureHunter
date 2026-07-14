package game.battle.model;

import game.battle.object.Pos;
import lombok.Data;
import protocol.Pbmethod;

import java.util.*;


@Data
public class ChunkObject {
    int chunkId;
    Map<Integer, CellObject> mCells; // id - cell
    Pos pos;
    int typeDrop;
    int typeRoom;
    int typeEvent;

    public ChunkObject(int chunkId, Pos pos, Map<Integer, CellObject> cells) {
        this.chunkId = chunkId;
        this.pos = pos;
        this.mCells = cells;
    }

    public Pbmethod.PbChunk toProtoAdd() {
        Pbmethod.PbChunk.Builder pb = Pbmethod.PbChunk.newBuilder();
        pb.setId(chunkId);
        pb.setPos(pos.toProto());
        pb.setIsAdd(true);
        for (Map.Entry<Integer, CellObject> entry : mCells.entrySet()) {
            pb.addCells(entry.getValue().toProto());
        }
        return pb.build();
    }


    public Pbmethod.PbChunk toProtoUpdate(Set<Integer> ids) {
        Pbmethod.PbChunk.Builder pb = Pbmethod.PbChunk.newBuilder();
        pb.setId(chunkId);
        pb.setIsAdd(true);
        // Ensure pos on partial updates (revive) — tránh chunk spawn lệch (0,0)
        pb.setPos(pos.toProto());
        for (Map.Entry<Integer, CellObject> entry : mCells.entrySet()) {
            if( ids.contains(entry.getKey())) pb.addCells(entry.getValue().toProto());
        }
        return pb.build();
    }

    public Pbmethod.PbChunk toProtoRemove() {
        return Pbmethod.PbChunk.newBuilder().setIsAdd(false).setId(chunkId).build();
    }
}
