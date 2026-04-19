package game.battle.model;

import game.battle.object.Pos;
import game.treasure.mapping.main.ResMapEntity;
import protocol.Pbmethod;

import static game.treasure.BattleConfig.CHUNK_SIZE;

public class MapService {

    // để private, từ tọa độ thế giới x ra chunk x local
    public static int worldToChunkX(ResMapEntity map, int worldX) {
        int localX = (int) (worldX - map.getBotLeftP().x); // 0..99
        int nx = Math.floorDiv(localX, CHUNK_SIZE); // 0..9
        return map.getMinChunkX() + nx; // -5..4
    }

    // để private, từ tọa độ thế giới y ra chunk y local
    public static int worldToChunkY(ResMapEntity map, int worldY) {
        int localY = (int) (worldY - map.getBotLeftP().y); // 0..69
        int ny = Math.floorDiv(localY, CHUNK_SIZE);
        return map.getMinChunkY() + ny; // -3..3
    }

    public static void validateType(int type) {
        for (Pbmethod.CellObjectType e : Pbmethod.CellObjectType.values()) {
            if (e.getNumber() == type) {
                return;
            }
        }
        throw new IllegalArgumentException("Invalid type ---- : " + type);
    }



    // tính ra idchunk từ x y
    public static int chunkPosToId(ResMapEntity map, int chunkX, int chunkY) {
        int nx = chunkX - map.getMinChunkX();   // offset X từ biên trái
        int ny = chunkY - map.getMinChunkY();   // offset Y từ biên dưới
        return ny * map.getWidthChunk() + nx;
    }

    /**
     * Ô trong chunk: 0 .. CHUNK_SIZE*CHUNK_SIZE - 1 (với CHUNK_SIZE=10 → 0..99).
     */
    public static int localCellIndexInChunk(ResMapEntity map, int worldX, int worldY) {
        int localX = (int) (worldX - map.getBotLeftP().x);
        int localY = (int) (worldY - map.getBotLeftP().y);
        int cx = Math.floorMod(localX, CHUNK_SIZE);
        int cy = Math.floorMod(localY, CHUNK_SIZE);
        return cy * CHUNK_SIZE + cx;
    }

    /** Cách 3: gộp chunkId + index ô trong chunk thành một số duy nhất. */
    public static int chunkAndLocalCellToGlobalId(int chunkId, int localIndex0to99) {
        return chunkId * (CHUNK_SIZE * CHUNK_SIZE) + localIndex0to99;
    }

    /** Từ tọa độ thế giới (ô nguyên) → global cell id (cách 3). */
    public static int worldCellPosToGlobalCellId(ResMapEntity map, int worldX, int worldY) {
        int chunkId = chunkPosToId(map, worldToChunkX(map, worldX), worldToChunkY(map, worldY));
        int local = localCellIndexInChunk(map, worldX, worldY);
        return chunkAndLocalCellToGlobalId(chunkId, local);
    }

    /** Từ Pos (world, floor) → global cell id (cách 3). */
    public static int worldPosToGlobalCellId(ResMapEntity map, Pos pos) {
        int fx = (int) Math.floor(pos.getX());
        int fy = (int) Math.floor(pos.getY());
        return worldCellPosToGlobalCellId(map, fx, fy);
    }

    // lấy ra cellId từ globalkCellId -> đưa ra index cell trong CHUNK_SIZE*CHUNK_SIZE (0-99)
    public static int globalCellIdToChunkId(int globalCellId) {
        return globalCellId / (CHUNK_SIZE * CHUNK_SIZE);
    }


    // lấy ra index cell từ globalkCellId -> đưa ra index cell trong CHUNK_SIZE*CHUNK_SIZE (0-99)
    public static int globalCellIdToLocalIndex(int globalCellId) {
        return Math.floorMod(globalCellId, CHUNK_SIZE * CHUNK_SIZE);
    }

    // từ tọa độ thế giới đổi sang chunk id
    public static int worldPosToChunkId(ResMapEntity map, Pos pos) {
        int fx = (int) Math.floor(pos.getX());
        int fy = (int) Math.floor(pos.getY());
        int chunkX = worldToChunkX(map, fx);
        int chunkY = worldToChunkY(map, fy);
        return chunkPosToId(map, chunkX, chunkY);
    }
}