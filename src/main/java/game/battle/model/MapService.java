package game.battle.model;

import game.battle.object.Pos;
import game.treasure.mapping.main.ResMapEntity;
import protocol.Pbmethod;

public class MapService {

    // để private, từ tọa độ thế giới x ra chunk x local
    public static int worldToChunkX(ResMapEntity map, float worldX) {
        int localX = (int) (worldX - map.getBotLeftP().x);
        int nx = Math.floorDiv(localX, map.getCellsPerChunkX());
        return map.getMinChunkX() + nx;
    }

    // để private, từ tọa độ thế giới y ra chunk y local
    public static int worldToChunkY(ResMapEntity map, float worldY) {
        int localY = (int) (worldY - map.getBotLeftP().y);
        int ny = Math.floorDiv(localY, map.getCellsPerChunkY());
        return map.getMinChunkY() + ny;
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
     * Ô trong chunk: 0 .. cellsPerChunkX*cellsPerChunkY - 1.
     */
    public static int localCellIndexInChunk(ResMapEntity map, int worldX, int worldY) {
        int localX = (int) (worldX - map.getBotLeftP().x);
        int localY = (int) (worldY - map.getBotLeftP().y);
        int cx = Math.floorMod(localX, map.getCellsPerChunkX());
        int cy = Math.floorMod(localY, map.getCellsPerChunkY());
        return cy * map.getCellsPerChunkX() + cx;
    }

    // từ tọa độ thế giới đổi sang chunk id
    public static int worldPosToChunkId(ResMapEntity map, Pos pos) {
        int fx = (int) Math.floor(pos.getX());
        int fy = (int) Math.floor(pos.getY());
        int chunkX = worldToChunkX(map, fx);
        int chunkY = worldToChunkY(map, fy);
        return chunkPosToId(map, chunkX, chunkY);
    }


    public static void localIndexToCellInChunk(ResMapEntity map, int localIndex, int[] outCellXY) {
        int cx = Math.floorMod(localIndex, map.getCellsPerChunkX());
        int cy = Math.floorDiv(localIndex, map.getCellsPerChunkX());
        outCellXY[0] = cx;
        outCellXY[1] = cy;
    }


    public static int chunkAndLocalCellToGlobalId(ResMapEntity map, int chunkId, int localIndex) {
        return chunkId * map.getCellsPerChunkArea() + localIndex;
    }

    public static int globalCellIdToChunkId(ResMapEntity map, int globalCellId) {
        return Math.floorDiv(globalCellId, map.getCellsPerChunkArea());
    }

    public static int globalCellIdToLocalIndex(ResMapEntity map, int globalCellId) {
        return Math.floorMod(globalCellId, map.getCellsPerChunkArea());
    }

    // ===== World -> global cell id =====
    public static int worldCellPosToGlobalCellId(ResMapEntity map, int worldX, int worldY) {
        int chunkX = worldToChunkX(map, worldX);
        int chunkY = worldToChunkY(map, worldY);
        int chunkId = chunkPosToId(map, chunkX, chunkY);
        int local = localCellIndexInChunk(map, worldX, worldY);
        return chunkAndLocalCellToGlobalId(map, chunkId, local);
    }


    public static int worldPosToGlobalCellId(ResMapEntity map, Pos pos) {
        int fx = (int) Math.floor(pos.getX());
        int fy = (int) Math.floor(pos.getY());
        return worldCellPosToGlobalCellId(map, fx, fy);
    }

}
