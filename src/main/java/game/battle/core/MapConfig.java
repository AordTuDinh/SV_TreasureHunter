package game.battle.core;

import game.battle.object.Pos;
import lombok.Data;

@Data
public class MapConfig {
    static final int mapId = 1;
    static final int MAP_CHUNK_W = 10;
    static final int MAP_CHUNK_H = 7;
    static final int CHUNK_SIZE = 10;  // 1 chunk = 10x10 unit
    static final int VIEW_RADIUS = 1; // R=1 => tối đa 3x3 = 9 chunk


    // => pos
    static final Pos botLeft = new Pos(-MAP_CHUNK_W * CHUNK_SIZE / 2f, -MAP_CHUNK_H * CHUNK_SIZE / 2f);
    static final Pos topRight = new Pos(MAP_CHUNK_W * CHUNK_SIZE / 2f, MAP_CHUNK_H * CHUNK_SIZE / 2f);


    // Chunk coordinate range
    static final int minChunkX = -MAP_CHUNK_W / 2;
    static final int maxChunkX = MAP_CHUNK_W / 2 - 1;
    static final int minChunkY = -MAP_CHUNK_H / 2;
    static final int maxChunkY = MAP_CHUNK_H / 2;
}
