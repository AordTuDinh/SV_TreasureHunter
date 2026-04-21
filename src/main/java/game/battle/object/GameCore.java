package game.battle.object;

import game.battle.model.MapService;
import game.battle.type.RoomState;
import game.treasure.BattleConfig;
import game.treasure.mapping.main.ResMapEntity;
import game.treasure.table.BaseBattleRoom;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
public class GameCore {

    public static Pos checkWall(ResMapEntity map, Pos move) {
        if (move.x > map.getTopRightP().x - BattleConfig.P_Width / 2)
            move.x = map.getTopRightP().x - BattleConfig.P_Width / 2;
        if (move.x < map.getBotLeftP().x + BattleConfig.P_Width / 2)
            move.x = map.getBotLeftP().x + BattleConfig.P_Width / 2;
        if (move.y > map.getTopRightP().y - BattleConfig.P_Height)
            move.y = map.getTopRightP().y - BattleConfig.P_Height;
        if (move.y < map.getBotLeftP().y) move.y = map.getBotLeftP().y;
        return move;
    }

    public static Pos checkWall2(ResMapEntity map, Pos addPos, Pos curPos) {
        Pos move = new Pos(curPos.x + addPos.x, curPos.y + addPos.y);
        return checkWall(map, move);
    }

    //Fixme DATE: 7/31/2022 LƯU Ý ---> Gọi trong update: process eff room - effect dạng tác dụng 1 lần
    public void Update(BaseBattleRoom room) {
        if (room.getRoomState() != RoomState.ACTIVE) return;
    }

    //Fixme DATE: 7/31/2022 LƯU Ý ---> Lưu ý đã update trong effect in room thì không có trong room by time nữa
    public void EffectUpdate(BaseBattleRoom room) { // 0.5s gọi 1 lần
        if (room.getRoomState() != RoomState.ACTIVE) return;
    }


    public static List<Integer> getVisibleChunkIds(ResMapEntity mapInfo, int centerChunkX, int centerChunkY) {
        int minX = Math.max(mapInfo.getMinChunkX(), centerChunkX - mapInfo.getViewRadius());
        int maxX = Math.min(mapInfo.getMaxChunkX(), centerChunkX + mapInfo.getViewRadius());
        int minY = Math.max(mapInfo.getMinChunkY(), centerChunkY - mapInfo.getViewRadius());
        int maxY = Math.min(mapInfo.getMaxChunkY(), centerChunkY + mapInfo.getViewRadius());

        List<Integer> out = new ArrayList<>();
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                out.add(MapService.chunkPosToId(mapInfo,x, y));
            }
        }
        return out;
    }

    public void LastUpdate(BaseBattleRoom room) {
        if (room.getRoomState() != RoomState.ACTIVE) return;
    }
}
