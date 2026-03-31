package game.battle.core;

import lombok.Data;

@Data
public class MapConfig {
    static final int mapId = 1;
    static final int MAP_CHUNK_W = 10; // x: 0..9
    static final int MAP_CHUNK_H = 7;  // y: 0..6
    static final int CHUNK_SIZE = 10;  // 1 chunk = 10x10 unit
    static final int VIEW_RADIUS = 1; // R=1 => tối đa 3x3 = 9 chunk


    // Nếu map bạn đặt gốc giữa, ví dụ x chunk từ -5..4, y chunk từ -3..3:
    static final int minChunkX = -5;
    static final int maxChunkX = 4;
    static final int minChunkY = -3;
    static final int maxChunkY = 3;
}
