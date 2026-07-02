package game.treasure.mapping.main;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import game.battle.model.CellObject;
import game.battle.model.ChunkObject;
import game.battle.model.MapService;
import game.battle.object.Pos;
import game.object.MapData;
import lombok.Getter;
import protocol.Pbmethod;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
public class ResMapEntity extends BaseEntity implements Serializable {
    @Id
    @Getter
    int id;
    @Getter
    int viewRadius;
    String map;


    @Transient
    @Getter
    int widthChunk, heightChunk;
    @Transient
    @Getter
    int cellsPerChunkX, cellsPerChunkY;
    @Transient
    @Getter
    MapData mapData;
    @Getter
    @Transient
    Pos botLeftP, topRightP;
    @Getter
    @Transient
    int minChunkX, maxChunkX, minChunkY, maxChunkY;
    @Transient
    Map<Integer, ChunkObject> mChunk = new HashMap<>();
    @Transient
    Map<Integer, MapData.ChunkDto> chunkMetaById = new HashMap<>();
    @Transient
    List<CampFireCache> campFireCacheList = new ArrayList<>();
    @Transient
    Map<Integer, List<CampFireCache>> campFiresByChunk = new HashMap<>();

    public static class CampFireCache {
        public int id;
        public float x;
        public float y;
        public float radius;
        public int chunkId;
    }


    public void init() {
        if (map != null && !map.isEmpty()) {
            mapData = new Gson().fromJson(map, new TypeToken<MapData>() {
            }.getType());
            checkJson(id, map);
        }
        resolveMapLayout();

        // gen default chunk
        for (int y = minChunkY; y <= maxChunkY; y++) {
            for (int x = minChunkX; x <= maxChunkX; x++) {
                int chunkId = MapService.chunkPosToId(this, x, y);
                int baseX100 = Math.round(botLeftP.x * 100f);
                int baseY100 = Math.round(botLeftP.y * 100f);
                int worldX100 = baseX100 + (x - minChunkX) * cellsPerChunkX * 100;
                int worldY100 = baseY100 + (y - minChunkY) * cellsPerChunkY * 100;
                ChunkObject chunk = new ChunkObject(chunkId, new Pos(worldX100, worldY100), new HashMap<>());
                mChunk.put(chunkId, chunk);
            }
        }

        applyChunkMeta();
        buildCampFireCache();

        // parse map object
        for (MapData.CellDto c : mapData.cells) {
            MapService.validateType(c.type);
            // c.x, c.y là tọa độ world trực tiếp (không còn x100)
            int worldX = c.x;
            int worldY = c.y;

            if (!isInsideWorld(worldX, worldY)) {
                System.out.println("[MapLoad] skip out-of-world cell: x=" + worldX + ", y=" + worldY);
                continue;
            }

            int chunkX = MapService.worldToChunkX(this, worldX);
            int chunkY = MapService.worldToChunkY(this, worldY);
            int chunkId = MapService.chunkPosToId(this, chunkX, chunkY);
            Pos pos = new Pos(worldX, worldY);
            int cellId = MapService.worldCellPosToGlobalCellId(this, worldX, worldY);
            int materialId = getTypeDrop(chunkId);
            int typeEvent = getTypeEvent(chunkId);
            int itemEventId = typeEvent > 0 ? mapTypeEventToItemEventMaterialId(typeEvent) : 0;
            CellObject cell = new CellObject(pos, c.type, chunkId, cellId, materialId, itemEventId);
            if (chunkX < minChunkX || chunkX > maxChunkX
                    || chunkY < minChunkY || chunkY > maxChunkY) {
                System.out.println("[MapLoad] skip out-of-bound cell: x=" + worldX + ", y=" + worldY
                        + ", chunkX=" + chunkX + ", chunkY=" + chunkY);
                continue;
            }

            ChunkObject targetChunk = mChunk.get(chunkId);
            if (targetChunk == null) {
                System.out.println("[MapLoad] skip missing chunk bucket: chunkId=" + chunkId
                        + ", chunkX=" + chunkX + ", chunkY=" + chunkY);
                continue;
            }
            targetChunk.getMCells().put(cell.getId(), cell);
        }
    }

    public List<Integer> getChunkNoAttack() {
        return new ArrayList<>(mapData.chunkNoAttack);
    }

    public Map<Integer, ChunkObject> getDataMap() {
        return new HashMap<>(mChunk);
    }

    public MapData.ChunkDto getChunkMeta(int chunkId) {
        return chunkMetaById.get(chunkId);
    }

    public int getTypeRoom(int chunkId) {
        MapData.ChunkDto dto = chunkMetaById.get(chunkId);
        return dto != null ? dto.typeRoom : 0;
    }

    public int getTypeDrop(int chunkId) {
        MapData.ChunkDto dto = chunkMetaById.get(chunkId);
        return dto != null ? dto.typeDrop : 0;
    }

    public int getTypeEvent(int chunkId) {
        MapData.ChunkDto dto = chunkMetaById.get(chunkId);
        return dto != null ? dto.typeEvent : 0;
    }

    public static int mapTypeEventToItemEventMaterialId(int typeEvent) {
        return 23 + typeEvent;
    }

    void buildCampFireCache() {
        campFireCacheList.clear();
        campFiresByChunk.clear();
        if (mapData == null || mapData.campFires == null) return;

        for (MapData.CampFire cf : mapData.campFires) {
            if (cf == null) continue;
            CampFireCache cache = new CampFireCache();
            cache.id = cf.id;
            cache.x = cf.x;
            cache.y = cf.y;
            cache.radius = cf.radius;
            int chunkX = MapService.worldToChunkX(this, cf.x);
            int chunkY = MapService.worldToChunkY(this, cf.y);
            cache.chunkId = MapService.chunkPosToId(this, chunkX, chunkY);
            campFireCacheList.add(cache);
            campFiresByChunk.computeIfAbsent(cache.chunkId, k -> new ArrayList<>()).add(cache);
        }
    }

    public boolean isInCampFireSafeZone(Pos pos) {
        if (pos == null || campFiresByChunk.isEmpty()) return false;

        int chunkId = MapService.worldPosToChunkId(this, pos);
        List<CampFireCache> list = campFiresByChunk.get(chunkId);
        if (list == null) return false;

        float px = pos.getX();
        float py = pos.getY();
        for (CampFireCache cf : list) {
            float dx = px - cf.x;
            float dy = py - cf.y;
            float radiusSq = cf.radius * cf.radius;
            if (dx * dx + dy * dy <= radiusSq) return true;
        }
        return false;
    }

    void applyChunkMeta() {
        chunkMetaById.clear();
        if (mapData == null || mapData.chunks == null) return;

        for (MapData.ChunkDto dto : mapData.chunks) {
            chunkMetaById.put(dto.id, dto);
            ChunkObject chunk = mChunk.get(dto.id);
            if (chunk != null) {
                chunk.setTypeDrop(dto.typeDrop);
                chunk.setTypeRoom(dto.typeRoom);
                chunk.setTypeEvent(dto.typeEvent);
            }
        }
    }

    boolean isInsideWorld(int worldX, int worldY) {
        return worldX >= botLeftP.getX()
                && worldX < topRightP.x
                && worldY >= botLeftP.y
                && worldY < topRightP.y;
    }

    /**
     * sizeX/sizeY = cell mỗi chunk; chunkX/chunkY = số chunk theo trục (từ map JSON).
     */
    void resolveMapLayout() {
        cellsPerChunkX = Math.max(1, mapData.sizeX);
        cellsPerChunkY = Math.max(1, mapData.sizeY);
        widthChunk = Math.max(1, mapData.chunkX);
        heightChunk = Math.max(1, mapData.chunkY);
        minChunkX = 0;
        minChunkY = 0;

        maxChunkX = minChunkX + widthChunk - 1;
        maxChunkY = minChunkY + heightChunk - 1;

        int worldWidth = widthChunk * cellsPerChunkX;
        int worldHeight = heightChunk * cellsPerChunkY;
        botLeftP = new Pos(-worldWidth / 2f, -worldHeight / 2f);
        topRightP = new Pos(botLeftP.x + worldWidth, botLeftP.y + worldHeight);
    }

    public int getCellsPerChunkArea() {
        return cellsPerChunkX * cellsPerChunkY;
    }


}
