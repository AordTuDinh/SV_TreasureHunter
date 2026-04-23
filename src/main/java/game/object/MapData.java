package game.object;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MapData implements Serializable {
    public List<CellDto> cells = new ArrayList<>();
    public List<Integer> chunkNoAttack = new ArrayList<>();


    public static class CellDto {
        public int x;
        public int y;
        public int type;
    }
}

