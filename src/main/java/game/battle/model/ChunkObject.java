package game.battle.model;

import game.battle.object.Pos;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;


@Data
public class ChunkObject {
    int id;
    Pos pos;
    List<StaticCell> cells;

    public ChunkObject(int id, Pos pos, List<StaticCell> cells) {
        this.id = id;
        this.pos = pos;
        this.cells = cells;
    }
}
