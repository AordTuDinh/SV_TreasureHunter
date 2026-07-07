package game.object;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MapData implements Serializable {
    public List<CellDto> cells = new ArrayList<>();
    public List<ChunkDto> chunks = new ArrayList<>();
    public List<Integer> chunkNoAttack = new ArrayList<>();
    public List<CampFire> campFires = new ArrayList<>();
    public CampFire heath = new CampFire();
    public  int sizeX;
    public  int sizeY;
    public  int chunkX;
    public  int chunkY;

    public static class CellDto {
        public int x;
        public int y;
        public int type;
    }

    public static class CampFire{
        public int id;
        public float radius;
        public float x;
        public float y;
    }

    public static class Zone {
        public int type;  // =0 là có thể đánh nhau mà k trừ cup  =1 là không đánh nhau
        public float minX;
        public float minY;
        public float maxX;
        public float maxY;
    }

    public static class ChunkDto {
        public int id;
        public int typeDrop;
        public int typeRoom;
        public int typeEvent ; // từ 1 -> 5 loại item sự kiện
    }
}

