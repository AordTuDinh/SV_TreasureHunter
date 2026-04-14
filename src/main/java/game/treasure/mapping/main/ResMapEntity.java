package game.treasure.mapping.main;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import game.battle.model.CellObject;
import game.battle.model.ChunkObject;
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
    int widthChunk, heightChunk;
    @Getter
    int viewRadius;
    String map, botLeft, topRight;
    @Transient
    @Getter
    List<Long> aMap; // sub map
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
                int chunkId = chunkPosToId(x, y);

                int baseX100 = Math.round(botLeftP.x * 100f);
                int baseY100 = Math.round(botLeftP.y * 100f);
                int worldX100 = baseX100 + (x - minChunkX) * BattleConfig.CHUNK_SIZE * 100;
                int worldY100 = baseY100 + (y - minChunkY) * BattleConfig.CHUNK_SIZE * 100;



                ChunkObject chunk = new ChunkObject(chunkId, new Pos(worldX100, worldY100), new ArrayList<>());

                mChunk.put(chunkId, chunk);
            }
        }

        // parse map object
        for (MapData.CellDto c : mapData.cells) {
            validateType(c.type);
            // c.x, c.y đang là fixed-point x100
            float wx = c.x / 100f;
            float wy = c.y / 100f;
            // snap về world cell int
            int worldX = Math.round(wx);
            int worldY = Math.round(wy);

            if (!isInsideWorld(worldX, worldY)) {
                System.out.println("[MapLoad] skip out-of-world cell: x=" + worldX + ", y=" +worldY);
                continue;
            }

            int chunkX = worldToChunkX(worldX);
            int chunkY = worldToChunkY(worldY);
            int chunkId = chunkPosToId(chunkX, chunkY);
            Pos pos = new Pos(c.x, c.y);

            CellObject cell = new CellObject(pos , c.type, chunkId, Pbmethod.CellState.ACTIVE );
            if (chunkX < minChunkX || chunkX > maxChunkX
                    || chunkY < minChunkY || chunkY > maxChunkY) {
                System.out.println("[MapLoad] skip out-of-bound cell: x=" +worldX + ", y=" +worldY
                        + ", chunkX=" + chunkX + ", chunkY=" + chunkY);
                continue;
            }

            ChunkObject targetChunk = mChunk.get(chunkId);
            if (targetChunk == null) {
                System.out.println("[MapLoad] skip missing chunk bucket: chunkId=" + chunkId
                        + ", chunkX=" + chunkX + ", chunkY=" + chunkY);
                continue;
            }
            targetChunk.getCells().add(cell);
        }
    }

    public Map<Integer, ChunkObject> getDataMap() {
        return new HashMap<>(mChunk);
    }


    // từ tọa độ thế giới đổi sang id chunk
    public  int worldPosToChunkId(Pos pos){
        int fx = (int) Math.floor(pos.getX());
        int fy = (int) Math.floor(pos.getY());
        int chunkX = worldToChunkX(fx);
        int chunkY = worldToChunkY(fy);
        int id = chunkPosToId(chunkX, chunkY);
        return id;


      //  return chunkPosToId(worldToChunkX((int) Math.floor(pos.getX())),worldToChunkY((int) Math.floor(pos.getY())));
    }


    // tính ra idchunk từ x y
    public   int chunkPosToId(int chunkX, int chunkY) {
        int nx = chunkX - minChunkX;   // offset X từ biên trái
        int ny = chunkY - minChunkY;   // offset Y từ biên dưới
        return ny * widthChunk + nx;
    }


    void validateType(int type) {
        // đổi rule tùy game của bạn
        if (type < 1 || type > 3) {
            throw new IllegalArgumentException("Invalid type: " + type + " (expected 1..3)");
        }
    }


    boolean isInsideWorld(int worldX, int worldY) {
        return worldX >= botLeftP.getX()
                && worldX < topRightP.x
                && worldY >= botLeftP.y
                && worldY < topRightP.y;
    }

    // để private, từ tọa độ thế giới x ra chunk x local
    int worldToChunkX(int worldX) {
        int localX = (int) (worldX - botLeftP.x); // 0..99
        int nx = Math.floorDiv(localX, CHUNK_SIZE); // 0..9
        return minChunkX + nx; // -5..4
    }
    // để private, từ tọa độ thế giới y ra chunk y local
    int worldToChunkY(int worldY) {
        int localY = (int) (worldY - botLeftP.y); // 0..69
        int ny = Math.floorDiv(localY, CHUNK_SIZE);
        return minChunkY + ny; // -3..3
    }

}
