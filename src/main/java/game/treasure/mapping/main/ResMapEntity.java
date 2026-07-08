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
    public static final int ZONE_TYPE_NO_CUP_LOSS = 0;
    public static final int ZONE_TYPE_NO_PVP = 1;
    public static final int ZONE_TYPE_JAIL = 2;

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
    @Transient
    CampFireCache heathZoneCache;
    @Transient
    List<ZoneCache> zoneCacheList = new ArrayList<>();
    @Transient
    Map<Integer, List<ZoneCache>> zonesByChunk = new HashMap<>();
    @Transient
    ZoneCache jailZoneCache;

    public static class CampFireCache {
        public int id;
        public float x;
        public float y;
        public float radius;
        public int chunkId;
    }

    public static class ZoneCache {
        public int type;
        public float minX;
        public float minY;
        public float maxX;
        public float maxY;
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
        buildHeathZoneCache();
        buildZoneCache();

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
            cell.setCampFire(isInCampFireSafeZone(pos));
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

    void buildHeathZoneCache() {
        heathZoneCache = null;
        if (mapData != null && mapData.heath != null && mapData.heath.radius > 0) {
            MapData.CampFire hf = mapData.heath;
            CampFireCache cache = new CampFireCache();
            cache.id = hf.id;
            cache.x = hf.x;
            cache.y = hf.y;
            cache.radius = hf.radius;
            heathZoneCache = cache;
            return;
        }
        // Fallback map home khi JSON/db chưa có field heath (MapCampFireLayout)
        if (id == 0) {
            CampFireCache cache = new CampFireCache();
            cache.id = 0;
            cache.x = 11f;
            cache.y = 14f;
            cache.radius = 4f;
            heathZoneCache = cache;
        }
    }

    public boolean isInHeathZone(Pos pos) {
        if (pos == null || heathZoneCache == null) return false;
        float dx = pos.getX() - heathZoneCache.x;
        float dy = pos.getY() - heathZoneCache.y;
        float radiusSq = heathZoneCache.radius * heathZoneCache.radius;
        return dx * dx + dy * dy <= radiusSq;
    }

    void buildZoneCache() {
        zoneCacheList.clear();
        zonesByChunk.clear();
        jailZoneCache = null;
        if (mapData == null || mapData.zones == null) return;

        for (MapData.Zone zone : mapData.zones) {
            if (zone == null) continue;

            ZoneCache cache = new ZoneCache();
            cache.type = zone.type;
            cache.minX = Math.min(zone.minX, zone.maxX);
            cache.minY = Math.min(zone.minY, zone.maxY);
            cache.maxX = Math.max(zone.minX, zone.maxX);
            cache.maxY = Math.max(zone.minY, zone.maxY);

            float centerX = (cache.minX + cache.maxX) * 0.5f;
            float centerY = (cache.minY + cache.maxY) * 0.5f;
            int centerChunkX = MapService.worldToChunkX(this, centerX);
            int centerChunkY = MapService.worldToChunkY(this, centerY);
            cache.chunkId = MapService.chunkPosToId(this, centerChunkX, centerChunkY);

            if (!isZoneInsideSingleChunk(cache, cache.chunkId)) {
                System.out.println("[MapLoad] zone spans multiple chunks: mapId=" + id
                        + ", type=" + cache.type
                        + ", rect=[" + cache.minX + "," + cache.minY + "," + cache.maxX + "," + cache.maxY + "]"
                        + ", centerChunk=" + cache.chunkId);
            }

            if (cache.type == ZONE_TYPE_JAIL) {
                if (jailZoneCache != null) {
                    System.out.println("[MapLoad] multiple jail zones on mapId=" + id + " — using first only");
                } else {
                    jailZoneCache = cache;
                }
            }

            zoneCacheList.add(cache);
            zonesByChunk.computeIfAbsent(cache.chunkId, k -> new ArrayList<>()).add(cache);
        }
    }

    boolean isZoneInsideSingleChunk(ZoneCache zone, int chunkId) {
        return zoneChunkId(zone.minX, zone.minY) == chunkId
                && zoneChunkId(zone.minX, zone.maxY) == chunkId
                && zoneChunkId(zone.maxX, zone.minY) == chunkId
                && zoneChunkId(zone.maxX, zone.maxY) == chunkId;
    }

    int zoneChunkId(float x, float y) {
        int chunkX = MapService.worldToChunkX(this, x);
        int chunkY = MapService.worldToChunkY(this, y);
        return MapService.chunkPosToId(this, chunkX, chunkY);
    }

    public boolean isInZone(Pos pos, int type) {
        if (pos == null || zonesByChunk.isEmpty()) return false;

        int chunkId = MapService.worldPosToChunkId(this, pos);
        List<ZoneCache> list = zonesByChunk.get(chunkId);
        if (list == null) return false;

        float px = pos.getX();
        float py = pos.getY();
        for (ZoneCache zone : list) {
            if (zone.type != type) continue;
            if (px >= zone.minX && px <= zone.maxX && py >= zone.minY && py <= zone.maxY)
                return true;
        }
        return false;
    }

    public boolean isInNoCupLossZone(Pos pos) {
        return isInZone(pos, ZONE_TYPE_NO_CUP_LOSS);
    }

    public boolean isInNoPvpZone(Pos pos) {
        return isInZone(pos, ZONE_TYPE_NO_PVP);
    }

    public boolean isInJailZone(Pos pos) {
        return isInZone(pos, ZONE_TYPE_JAIL);
    }

    public boolean isInBlockedPvpZone(Pos pos) {
        return isInNoPvpZone(pos) || isInJailZone(pos);
    }

    public Pos getJailSpawnPos() {
        if (jailZoneCache == null) return null;
        float centerX = (jailZoneCache.minX + jailZoneCache.maxX) * 0.5f;
        float centerY = (jailZoneCache.minY + jailZoneCache.maxY) * 0.5f;
        return clampToJailZone(new Pos(centerX, centerY));
    }

    public Pos clampToJailZone(Pos pos) {
        if (pos == null || jailZoneCache == null || !isInJailChunk(pos)) return pos;
        float minX = jailZoneCache.minX + game.treasure.BattleConfig.P_Width / 2f;
        float maxX = jailZoneCache.maxX - game.treasure.BattleConfig.P_Width / 2f;
        float minY = jailZoneCache.minY;
        float maxY = jailZoneCache.maxY - game.treasure.BattleConfig.P_Height;
        if (pos.getX() > maxX) pos.setX(maxX);
        if (pos.getX() < minX) pos.setX(minX);
        if (pos.getY() > maxY) pos.setY(maxY);
        if (pos.getY() < minY) pos.setY(minY);
        return pos.round();
    }

    boolean isInJailChunk(Pos pos) {
        if (pos == null || jailZoneCache == null) return false;
        return MapService.worldPosToChunkId(this, pos) == jailZoneCache.chunkId;
    }

    boolean playerOverlapsJail(Pos pos) {
        if (pos == null || jailZoneCache == null) return false;
        float halfW = game.treasure.BattleConfig.P_Width / 2f;
        float height = game.treasure.BattleConfig.P_Height;
        float px = pos.getX();
        float py = pos.getY();
        return px - halfW < jailZoneCache.maxX && px + halfW > jailZoneCache.minX
                && py < jailZoneCache.maxY && py + height > jailZoneCache.minY;
    }

    /** Player thường không được vào nhà giam — đẩy tâm player ra ngoài theo hitbox. */
    public Pos pushOutOfJailZone(Pos pos) {
        if (pos == null || jailZoneCache == null || !playerOverlapsJail(pos)) return pos;

        float halfW = game.treasure.BattleConfig.P_Width / 2f;
        float height = game.treasure.BattleConfig.P_Height;
        float edgeEps = 0.05f;
        float px = pos.getX();
        float py = pos.getY();

        float outLeftX = jailZoneCache.minX - halfW - edgeEps;
        float outRightX = jailZoneCache.maxX + halfW + edgeEps;
        float outBottomY = jailZoneCache.minY - height - edgeEps;
        float outTopY = jailZoneCache.maxY + edgeEps;

        float dLeft = Math.abs(px - outLeftX);
        float dRight = Math.abs(px - outRightX);
        float dBottom = Math.abs(py - outBottomY);
        float dTop = Math.abs(py - outTopY);

        int edge = 0;
        float min = dLeft;
        if (dRight < min) {
            min = dRight;
            edge = 1;
        }
        if (dBottom < min) {
            min = dBottom;
            edge = 2;
        }
        if (dTop < min) edge = 3;

        if (edge == 0) pos.setX(outLeftX);
        else if (edge == 1) pos.setX(outRightX);
        else if (edge == 2) pos.setY(outBottomY);
        else pos.setY(outTopY);
        return pos.round();
    }

    /** Chặn player thường bước vào nhà giam — trượt dọc tường nếu có thể. */
    public Pos blockJailEntry(Pos prevPos, Pos nextPos) {
        if (nextPos == null || jailZoneCache == null || !isInJailChunk(nextPos)) return nextPos;
        if (!playerOverlapsJail(nextPos)) return nextPos;

        if (prevPos != null) {
            Pos tryX = new Pos(nextPos.getX(), prevPos.getY());
            if (!playerOverlapsJail(tryX)) return tryX.round();

            Pos tryY = new Pos(prevPos.getX(), nextPos.getY());
            if (!playerOverlapsJail(tryY)) return tryY.round();
        }
        return pushOutOfJailZone(nextPos);
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
