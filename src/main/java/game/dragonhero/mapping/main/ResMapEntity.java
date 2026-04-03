package game.dragonhero.mapping.main;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import game.battle.model.ChunkObject;
import game.battle.model.StaticCell;
import game.battle.object.Pos;
import game.dragonhero.BattleConfig;
import game.object.MapData;
import lombok.Getter;

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
        maxChunkX = widthChunk / 2 - 1;
        minChunkY = -heightChunk / 2;
        maxChunkY = heightChunk / 2;

        // gen default chunk
        for (int y = minChunkY; y <= maxChunkY; y++) {
            for (int x = minChunkX; x <= maxChunkX; x++) {
                int chunkId = chunkPosToId(x, y);
                ChunkObject chunk = new ChunkObject(chunkId, new Pos(x, y), new ArrayList<>());
                mChunk.put(chunkId, chunk);
            }
        }

        // parse map object
        for (MapData.CellDto c : mapData.cells) {
            validateType(c.type);
            if (!isInsideWorld(c.x, c.y)) {
                System.out.println("[MapLoad] skip out-of-world cell: x=" + c.x + ", y=" + c.y);
                continue;
            }

            int chunkX = worldToChunkX(c.x);
            int chunkY = worldToChunkY(c.y);

            StaticCell cell = new StaticCell(c.x, c.y, c.type, chunkX, chunkY);
            if (chunkX < minChunkX || chunkX > maxChunkX
                    || chunkY < minChunkY || chunkY > maxChunkY) {
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

    public Map<Integer, ChunkObject> getDataMap() {
        return new HashMap<>(mChunk);
    }


    // từ tọa độ thế giới đổi sang id chunk
    public  int worldPosToChunkId(Pos pos){
        return chunkPosToId(worldToChunkX((int) Math.floor(pos.getX())),worldToChunkY((int) Math.floor(pos.getX())));
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
        int nx = Math.floorDiv(localX, BattleConfig.CHUNK_SIZE); // 0..9
        return minChunkX + nx; // -5..4
    }
    // để private, từ tọa độ thế giới y ra chunk y local
    int worldToChunkY(int worldY) {
        int localY = (int) (worldY - botLeftP.y); // 0..69
        int ny = Math.floorDiv(localY, 10); // 0..6
        return minChunkY + ny; // -3..3
    }

}
