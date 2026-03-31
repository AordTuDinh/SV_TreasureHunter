package game.battle.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import game.battle.model.Chunk;
import game.battle.model.StaticCell;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

public class WorldStaticStore {
      // cellId -> cell
    public final Map<Integer, StaticCell> cellById = new HashMap<>();
    // (x,y) -> cellId
    public final Map<Long, Integer> cellByPos = new HashMap<>();
    // chunk -> list cellId
    public final Map<Chunk, List<Integer>> cellsByChunk = new HashMap<>();


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

    public static WorldStaticStore load(InputStream is) throws IOException {
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MapFileDto dto = mapper.readValue(is, MapFileDto.class);
        WorldStaticStore store = new WorldStaticStore();
        int nextId = 1;
        for (CellDto c : dto.cells) {
            int chunkX = Math.floorDiv(c.x, MapConfig.CHUNK_SIZE);
            int chunkY = Math.floorDiv(c.y, MapConfig.CHUNK_SIZE);
            long posKey = packXY(c.x, c.y);
            if (store.cellByPos.containsKey(posKey)) {
                throw new IllegalStateException("Duplicate cell at (" + c.x + "," + c.y + ")");
            }
            StaticCell cell = new StaticCell(nextId, c.x, c.y, c.type, chunkX, chunkY);
            store.cellById.put(nextId, cell);
            store.cellByPos.put(posKey, nextId);
            store.cellsByChunk.computeIfAbsent(new Chunk(chunkX, chunkY), k -> new ArrayList<>()).add(nextId);
            nextId++;
        }
        return store;
    }

    public List<StaticCell> getCellsInChunk(int chunkX, int chunkY) {
        List<Integer> ids = cellsByChunk.getOrDefault(new Chunk(chunkX, chunkY), Collections.emptyList());
        List<StaticCell> out = new ArrayList<>(ids.size());
        for (Integer id : ids) out.add(cellById.get(id));
        return out;
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
