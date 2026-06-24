package game.object;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MapData implements Serializable {
    public List<CellDto> cells = new ArrayList<>();
    public List<ChunkDto> chunks = new ArrayList<>();
    public List<Integer> chunkNoAttack = new ArrayList<>();
    public  int sizeX;
    public  int sizeY;
    public  int chunkX;
    public  int chunkY;

    public static class CellDto {
        public int x;
        public int y;
        public int type;
    }

    public static class ChunkDto {
        public int id;
        public int typeDrop;  // config từ 1 - 5 loại vật liệu
        public int typeRoom;
    }
}

