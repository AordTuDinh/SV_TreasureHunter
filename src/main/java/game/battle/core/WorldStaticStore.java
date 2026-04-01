package game.battle.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import game.battle.model.ChunkObject;
import game.battle.model.StaticCell;
import game.battle.object.Pos;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class WorldStaticStore {
    // gen map home static data
    static Map<Integer, ChunkObject> mChunk = new HashMap<>();


    public static Map<Integer, ChunkObject> getChunkHome() {
        return new HashMap<>(mChunk);
    }


    // --------------------------- load static data

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MapFileDto {
        public List<CellDto> cells = new ArrayList<>();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CellDto {
        public int x;
        public int y;
        public int type;
    }

    public static void load(InputStream is) throws IOException {
        mChunk.clear();
        for (int y = MapConfig.minChunkY; y <= MapConfig.maxChunkY; y++) {
            for (int x = MapConfig.minChunkX; x <= MapConfig.maxChunkX; x++) {
                int chunkId = chunkPosToId(x, y);
                ChunkObject chunk = new ChunkObject(chunkId, new Pos(x, y), new ArrayList<>());
                mChunk.put(chunkId, chunk);
            }
        }

        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MapFileDto dto = mapper.readValue(is, MapFileDto.class);

        for (CellDto c : dto.cells) {
            validateType(c.type);
            if (!isInsideWorld(c.x, c.y)) {
                System.out.println("[MapLoad] skip out-of-world cell: x=" + c.x + ", y=" + c.y);
                continue;
            }

            int chunkX = worldToChunkX(c.x);
            int chunkY = worldToChunkY(c.y);

            StaticCell cell = new StaticCell(c.x, c.y, c.type, chunkX, chunkY);
            if (chunkX < MapConfig.minChunkX || chunkX > MapConfig.maxChunkX
                    || chunkY < MapConfig.minChunkY || chunkY > MapConfig.maxChunkY) {
                System.out.println("[MapLoad] skip out-of-bound cell: x=" + c.x + ", y=" + c.y
                        + ", chunkX=" + chunkX + ", chunkY=" + chunkY);
                continue;
            }
            int chunkId = chunkPosToId(chunkX, chunkY);
            ChunkObject targetChunk = mChunk.get(chunkId);
            if (targetChunk == null) {
                System.out.println("[MapLoad] skip missing chunk bucket: chunkId=" + chunkId
                        + ", chunkX=" + chunkX + ", chunkY=" + chunkY);
                continue;
            }
            targetChunk.getCells().add(cell);
        }
    }

    private static int chunkPosToId(int chunkX, int chunkY) {
        int nx = chunkX - MapConfig.minChunkX;   // offset X từ biên trái
        int ny = chunkY - MapConfig.minChunkY;   // offset Y từ biên dưới
        return ny * MapConfig.MAP_CHUNK_W + nx;
    }

    private static boolean isInsideWorld(int worldX, int worldY) {
        return worldX >= MapConfig.botLeft.getX()
                && worldX < MapConfig.topRight.x
                && worldY >= MapConfig.botLeft.y
                && worldY < MapConfig.topRight.y;
    }

    private static int worldToChunkX(int worldX) {
        int localX = (int) (worldX - MapConfig.botLeft.x); // 0..99
        int nx = Math.floorDiv(localX, MapConfig.CHUNK_SIZE); // 0..9
        return MapConfig.minChunkX + nx; // -5..4
    }

    private static int worldToChunkY(int worldY) {
        int localY = (int) (worldY - MapConfig.botLeft.y); // 0..69
        int ny = Math.floorDiv(localY, MapConfig.CHUNK_SIZE); // 0..6
        return MapConfig.minChunkY + ny; // -3..3
    }


    private static void validateType(int type) {
        // đổi rule tùy game của bạn
        if (type < 1 || type > 3) {
            throw new IllegalArgumentException("Invalid type: " + type + " (expected 1..3)");
        }
    }

    // pack 2 int -> 1 long key
    public static long packXY(int x, int y) {
        return (((long) x) << 32) ^ (y & 0xffffffffL);
    }
}
