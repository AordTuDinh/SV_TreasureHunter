package game.battle.core;

import game.battle.model.Chunk;
import game.battle.model.DiffResult;
import game.battle.model.StaticCell;
import protocol.Pbmethod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class MapService {
    private final WorldStaticStore store;


    public MapService(WorldStaticStore store) {
        this.store = store;
    }


    // Build gói InitMap cho player vừa vào map
    public Pbmethod.PbInitMap buildInitMap(float playerX, float playerY, long seq) {
        Chunk centerChunk = worldToChunk(playerX, playerY);
        Pbmethod.PbInitMap.Builder init = Pbmethod.PbInitMap.newBuilder()
                .setMapId(MapConfig.mapId)
                .setChunkSize(MapConfig.CHUNK_SIZE)
                .setViewRadius(MapConfig.VIEW_RADIUS)
                .setMapChunkWidth(MapConfig.MAP_CHUNK_W)
                .setMapChunkHeight(MapConfig.MAP_CHUNK_H)
                .setSeq(seq)
                .setCenterChunk(Pbmethod.PbPos.newBuilder()
                        .setX(centerChunk.x())
                        .setY(centerChunk.y())
                        .build());
        // AOI ban đầu: tối đa 9 chunk
        Set<Chunk> visible = getVisibleChunks(centerChunk, MapConfig.VIEW_RADIUS);
        for (Chunk cc : visible) {
            Pbmethod.PbChunk.Builder chunkBuilder = Pbmethod.PbChunk.newBuilder()
                    .setChunk(Pbmethod.PbPos.newBuilder().setX(cc.x()).setY(cc.y()).build());
            List<StaticCell> cells = store.getCellsInChunk(cc.x(), cc.y());
            for (StaticCell c : cells) {
                chunkBuilder.addCells(
                        Pbmethod.PbCell.newBuilder()
                                .setCellId(c.id())
                                .setX(c.x())
                                .setY(c.y())
                                .setType(c.type())
                                .setState(0)
                                .build()
                );
            }
            init.addChunks(chunkBuilder.build());
        }
        return init.build();
    }

    public DiffResult diffVisibleChunks(Chunk oldCenter, Chunk newCenter, int radius) {
        Set<Chunk> oldSet = getVisibleChunks(oldCenter, radius);
        Set<Chunk> newSet = getVisibleChunks(newCenter, radius);
        return diff(oldSet, newSet);
    }

    private Set<Chunk> getVisibleChunks(Chunk center, int radius) {
        Set<Chunk> out = new HashSet<>();
        int minX = Math.max(MapConfig.minChunkX, center.x() - radius);
        int maxX = Math.min(MapConfig.maxChunkX, center.x() + radius);
        int minY = Math.max(MapConfig.minChunkY, center.y() - radius);
        int maxY = Math.min(MapConfig.maxChunkY, center.y() + radius);
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                out.add(new Chunk(x, y));
            }
        }
        return out;
    }

    private Chunk worldToChunk(float x, float y) {
        int wx = (int) Math.floor(x);
        int wy = (int) Math.floor(y);
        int cx = Math.floorDiv(wx, MapConfig.CHUNK_SIZE);
        int cy = Math.floorDiv(wy, MapConfig.CHUNK_SIZE);
        cx = clamp(cx, MapConfig.minChunkX, MapConfig.maxChunkX);
        cy = clamp(cy, MapConfig.minChunkY, MapConfig.maxChunkY);
        return new Chunk(cx, cy);
    }

    private DiffResult diff(Set<Chunk> oldSet, Set<Chunk> newSet) {
        List<Chunk> enter = new ArrayList<>();
        List<Chunk> leave = new ArrayList<>();
        for (Chunk c : newSet) {
            if (!oldSet.contains(c)) enter.add(c);
        }
        for (Chunk c : oldSet) {
            if (!newSet.contains(c)) leave.add(c);
        }
        return new DiffResult(enter, leave);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    // ===== AddChunk: gửi nhiều chunk 1 lúc =====
    public Pbmethod.AddChunk buildAddChunk(List<Chunk> enterChunks) {
        Pbmethod.AddChunk.Builder out = Pbmethod.AddChunk.newBuilder();
        for (Chunk cc : enterChunks) {
            Pbmethod.PbChunk.Builder chunkPb = Pbmethod.PbChunk.newBuilder()
                    .setChunk(Pbmethod.PbPos.newBuilder().setX(cc.x()).setY(cc.y()).build());
            List<StaticCell> cells = store.getCellsInChunk(cc.x(), cc.y());
            for (StaticCell c : cells) {
                chunkPb.addCells(Pbmethod.PbCell.newBuilder()
                        .setCellId(c.id())
                        .setX(c.x())
                        .setY(c.y())
                        .setType(c.type())
                        .setState(0)
                        .build());
            }
            out.addChunkSnapshot(chunkPb.build());
        }
        return out.build();
    }


    // ===== RemoveChunk: gửi từng chunk =====
    public Pbmethod.RemoveChunk buildRemoveChunk(Chunk leaveChunk) {
        return Pbmethod.RemoveChunk.newBuilder()
                .setChunk(Pbmethod.PbChunk.newBuilder().setChunk(Pbmethod.PbPos.newBuilder()
                                .setX(leaveChunk.x())
                                .setY(leaveChunk.y()))
                        .build())
                .build();
    }

    // Nếu muốn gửi 1 batch remove chunk (theo cơ chế riêng của bạn)
    public List<Pbmethod.RemoveChunk> buildRemoveChunks(List<Chunk> leaveChunks) {
        List<Pbmethod.RemoveChunk> out = new ArrayList<>(leaveChunks.size());
        for (Chunk cc : leaveChunks) {
            out.add(buildRemoveChunk(cc));
        }
        return out;
    }
    // ===== AddCell =====
    public Pbmethod.AddCell buildAddCell(Chunk chunk, int cellId, int x, int y, int type, int state) {
        return Pbmethod.AddCell.newBuilder()
                .setChunk(Pbmethod.PbChunk.newBuilder().setChunk( Pbmethod.PbPos.newBuilder().setX(chunk.x()).setY(chunk.y()).build()))
                .setCell(Pbmethod.PbCell.newBuilder()
                        .setCellId(cellId)
                        .setX(x)
                        .setY(y)
                        .setType(type)
                        .setState(state)
                        .build())
                .build();
    }
    // ===== RemoveCell =====
    public Pbmethod.RemoveCell buildRemoveCell(int cellId) {
        return Pbmethod.RemoveCell.newBuilder()
                .setCellId(cellId)
                .build();
    }


}
