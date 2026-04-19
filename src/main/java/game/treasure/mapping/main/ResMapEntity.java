package game.treasure.mapping.main;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import game.battle.model.CellObject;
import game.battle.model.ChunkObject;
import game.battle.model.MapService;
import game.battle.object.Pos;
import game.treasure.BattleConfig;
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

import static game.treasure.BattleConfig.CHUNK_SIZE;

@Entity
public class ResMapEntity extends BaseEntity implements Serializable {
    @Id
    @Getter
    int id;
    @Getter
    int widthChunk, heightChunk;
    @Getter
    int viewRadius;
    String map, botLeft, topRight;


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


    public void init() {
        if (map != null && !map.isEmpty()) {
            mapData = new Gson().fromJson(map, new TypeToken<MapData>() {
            }.getType());
            checkJson(id, map);
        }
        botLeftP = new Pos(botLeft);
        topRightP = new Pos(topRight);

        minChunkX = -widthChunk / 2;
        minChunkY = -heightChunk / 2;
        maxChunkX = minChunkX + widthChunk - 1;
        maxChunkY = minChunkY + heightChunk - 1;

        // gen default chunk
        for (int y = minChunkY; y <= maxChunkY; y++) {
            for (int x = minChunkX; x <= maxChunkX; x++) {
                int chunkId = MapService.chunkPosToId(this, x, y);
                int baseX100 = Math.round(botLeftP.x * 100f);
                int baseY100 = Math.round(botLeftP.y * 100f);
                int worldX100 = baseX100 + (x - minChunkX) * BattleConfig.CHUNK_SIZE * 100;
                int worldY100 = baseY100 + (y - minChunkY) * BattleConfig.CHUNK_SIZE * 100;
                ChunkObject chunk = new ChunkObject(chunkId, new Pos(worldX100, worldY100), new HashMap<>());
                mChunk.put(chunkId, chunk);
            }
        }

        // parse map object
        for (MapData.CellDto c : mapData.cells) {
            MapService.validateType(c.type);
            // c.x, c.y đang là fixed-point x100
            float wx = c.x / 100f;
            float wy = c.y / 100f;
            // snap về world cell int
            int worldX = Math.round(wx);
            int worldY = Math.round(wy);

            if (!isInsideWorld(worldX, worldY)) {
                System.out.println("[MapLoad] skip out-of-world cell: x=" + worldX + ", y=" + worldY);
                continue;
            }

            int chunkX = MapService.worldToChunkX(this, worldX);
            int chunkY = MapService.worldToChunkY(this, worldY);
            int chunkId = MapService.chunkPosToId(this, chunkX, chunkY);
            Pos pos = new Pos(c.x, c.y);
            CellObject cell = new CellObject(pos, c.type, chunkId, Pbmethod.CellState.ACTIVE, this);
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

    public Map<Integer, ChunkObject> getDataMap() {
        return new HashMap<>(mChunk);
    }

    boolean isInsideWorld(int worldX, int worldY) {
        return worldX >= botLeftP.getX()
                && worldX < topRightP.x
                && worldY >= botLeftP.y
                && worldY < topRightP.y;
    }


}
