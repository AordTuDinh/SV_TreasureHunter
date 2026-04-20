package game.battle.model;

import game.battle.object.Pos;
import game.treasure.mapping.main.ResMapEntity;
import protocol.Pbmethod;

import static game.treasure.BattleConfig.CHUNK_SIZE;

public class MapService {

    // để private, từ tọa độ thế giới x ra chunk x local
    public static int worldToChunkX(ResMapEntity map, float worldX) {
        int localX = (int) (worldX - map.getBotLeftP().x); // 0..99
        int nx = Math.floorDiv(localX, CHUNK_SIZE); // 0..9
        return map.getMinChunkX() + nx; // -5..4
    }

    // để private, từ tọa độ thế giới y ra chunk y local
    public static int worldToChunkY(ResMapEntity map, float worldY) {
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
        int cx = Math.floorMod( localX, CHUNK_SIZE);
        int cy = Math.floorMod( localY, CHUNK_SIZE);
        return cy * CHUNK_SIZE + cx;
    }

    // từ tọa độ thế giới đổi sang chunk id
    public static int worldPosToChunkId(ResMapEntity map, Pos pos) {
        int fx = (int) Math.floor(pos.getX());
        int fy = (int) Math.floor(pos.getY());
        int chunkX = worldToChunkX(map, fx);
        int chunkY = worldToChunkY(map, fy);
        return chunkPosToId(map, chunkX, chunkY);
    }


    public static void localIndexToCellInChunk(int localIndex, int[] outCellXY) {
        int cx = Math.floorMod(localIndex, CHUNK_SIZE);
        int cy = Math.floorDiv(localIndex, CHUNK_SIZE);
        outCellXY[0] = cx;
        outCellXY[1] = cy;
    }


    // ===== NEW RULE: globalCellId = chunkId*100 + localIndex =====
    public static int chunkAndLocalCellToGlobalId(int chunkId, int localIndex) {
        return chunkId * (CHUNK_SIZE * CHUNK_SIZE) + localIndex; // chunkId*100 + local
    }
    public static int globalCellIdToChunkId(int globalCellId) {
        return Math.floorDiv(globalCellId, (CHUNK_SIZE * CHUNK_SIZE));
    }
    public static int globalCellIdToLocalIndex(int globalCellId) {
        return Math.floorMod(globalCellId, (CHUNK_SIZE * CHUNK_SIZE));
    }
    // ===== World -> global cell id =====
    public static int worldCellPosToGlobalCellId(ResMapEntity map, int worldX, int worldY) {
        int chunkX = worldToChunkX(map, worldX);
        int chunkY = worldToChunkY(map, worldY);
        int chunkId = chunkPosToId(map, chunkX, chunkY);
        int local = localCellIndexInChunk(map,
                worldX, worldY);
        return chunkAndLocalCellToGlobalId(chunkId, local);
    }

//    public static void chunkIdToChunkPos(ResMapEntity map, int chunkId, int[] outChunkXY) {
//        int nx = Math.floorMod(chunkId, map.getWidthChunk());
//        int ny = Math.floorDiv(chunkId, map.getWidthChunk());
//        outChunkXY[0] = map.getMinChunkX() + nx;
//        outChunkXY[1] = map.getMinChunkY() + ny;
//    }


    public static int worldPosToGlobalCellId(ResMapEntity map, Pos pos) {
        int fx = (int) Math.floor(pos.getX());
        int fy = (int) Math.floor(pos.getY());
        return worldCellPosToGlobalCellId(map, fx, fy);
    }


    // (optional) globalCellId -> world cell (int) nếu cần debug/trace
//    public static int[] globalCellIdToWorldCell(ResMapEntity map, int globalCellId) {
//        int chunkId = globalCellIdToChunkId(globalCellId);
//        int local = globalCellIdToLocalIndex(globalCellId);
//        int[] chunkXY = new int[2];
//        chunkIdToChunkPos(map, chunkId, chunkXY);
//        int[] cellXY = new int[2];
//        localIndexToCellInChunk(local, cellXY);
//        int worldX = (int) map.getBotLeftP().x + (chunkXY[0] - map.getMinChunkX()) * CHUNK_SIZE + cellXY[0];
//        int worldY = (int) map.getBotLeftP().y + (chunkXY[1] - map.getMinChunkY()) * CHUNK_SIZE + cellXY[1];
//        return new int[]{worldX, worldY};
//    }

}