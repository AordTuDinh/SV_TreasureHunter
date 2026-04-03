package game.battle.object;

import game.battle.calculate.MathLab;
import game.battle.model.Character;
import game.battle.model.Player;
import game.battle.type.GeometryType;
import game.battle.type.RoomState;
import game.dragonhero.BattleConfig;
import game.dragonhero.mapping.main.ResMapEntity;
import game.dragonhero.table.BaseBattleRoom;
import game.object.Geometry;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
public class GameCore {
    public Pos findTargetForPlayer(List<Character> lstEnemy, Player currentPlayer) {
        if (lstEnemy.size() <= 0) return Pos.zero();
        if (currentPlayer.getTargetAttack() != null && currentPlayer.getTargetAttack().isAlive() && currentPlayer.targetInSizeAttack())
            return currentPlayer.getTargetAttack().getPos();

        float min = 10000f; // đặt to vì còn phải check enemy alive
        Pos ret = Pos.zero();
        Character target = null;
        for (int i = 0; i < lstEnemy.size(); i++) {
            if (lstEnemy.get(i).isAlive() && min >= currentPlayer.getPos().distance(lstEnemy.get(i).getPos()) && lstEnemy.get(i).isReady()) {
                min = (float) currentPlayer.getPos().distance(lstEnemy.get(i).getPos());
                target = lstEnemy.get(i);
                ret = lstEnemy.get(i).getPos();
            }
        }
        // tim duoc con gan nhat nhung khoang cach qua tam danh thi tra ve 0
        if (min > currentPlayer.getRangeAttack() || target == null) return Pos.zero();
        currentPlayer.setTargetAttack(target);
        return ret;
    }


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
        if (move.x > map.getTopRightP().x - BattleConfig.P_Width / 2)
            move.x = map.getTopRightP().x - BattleConfig.P_Width / 2;
        if (move.x < map.getBotLeftP().x + BattleConfig.P_Width / 2)
            move.x = map.getBotLeftP().x + BattleConfig.P_Width / 2;
        if (move.y > map.getTopRightP().y - BattleConfig.P_Height)
            move.y = map.getTopRightP().y - BattleConfig.P_Height;
        if (move.y < map.getBotLeftP().y) move.y = map.getBotLeftP().y;
        return move;
    }

    static boolean checkHasMoveTopographic(BaseBattleRoom room, Pos move) {
        List<Geometry> geos = new ArrayList<>();// room.getCacheBattle().getMapInfo().getMapData().getGeos();
        if (geos == null) return true;
        boolean hasMove = true;
        for (int i = 0; i < geos.size(); i++) {
            Geometry geo = geos.get(i);
            if (!geo.isInSize()) { // k cho di chuyển vào hinh
                // flash check
                if (geo.getCenter().distance(move) < geo.getRadius()) {
                    // details check
                    if (geo.getType() == GeometryType.Circle) {
                        hasMove = geo.isInSize() == MathLab.pointInCircle(move, geo.getRadius(), geo.getCenter());
                        if (!hasMove) return false;
                    } else if (geo.getType() == GeometryType.Triangle) {
                        hasMove = geo.isInSize() == MathLab.pointInTriangle(move, geo.getPos());
                        if (!hasMove) return false;
                    }
                }
            } else {
                hasMove = false;
                if (geo.getType() == GeometryType.Circle) {
                    hasMove = MathLab.pointInCircle(move, geo.getRadius(), geo.getCenter());
                    if (hasMove) return true;
                } else if (geo.getType() == GeometryType.Triangle) {
                    hasMove = MathLab.pointInTriangle(move, geo.getPos());
                    if (hasMove) return true;
                }
            }

        }
        return hasMove;
    }

    public void reviveEnemy(List<Character> aEnemy) {
        for (int i = 0; i < aEnemy.size(); i++) {
            aEnemy.get(i).revive();
        }
    }


    public void checkHit(BaseBattleRoom room) {
        if (room.getRoomState() != RoomState.ACTIVE) return;
        // check hit melee
//        for (int i = 0; i < room.getAPlayer().size(); i++) {
//            Character player = room.getAPlayer().get(i);
//            for (int j = 0; j < room.getAEnemy().size(); j++) {
//                Character enemy = room.getAEnemy().get(j);
//                if (enemy.isHitMelee(player) && player.hasReceiveEffMelee(enemy)) {
//                    player.beAttackCollider(enemy);
//                }
//            }
//        }
    }

    //Fixme DATE: 7/31/2022 LƯU Ý ---> Gọi trong update: process eff room - effect dạng tác dụng 1 lần
    public void Update(BaseBattleRoom room) {
        if (room.getRoomState() != RoomState.ACTIVE) return;
    }

    //Fixme DATE: 7/31/2022 LƯU Ý ---> Lưu ý đã update trong effect in room thì không có trong room by time nữa
    public void EffectUpdate(BaseBattleRoom room) { // 0.5s gọi 1 lần
        if (room.getRoomState() != RoomState.ACTIVE) return;
    }


    public synchronized void FixedUpdate(BaseBattleRoom room) {
//        if (!room.getAPlayer().isEmpty()) {
//            checkHit(room);
//        }
    }


    public static List<Integer> getVisibleChunkIds(ResMapEntity mapInfo, int centerChunkX, int centerChunkY) {
        int minX = Math.max(mapInfo.getMinChunkX(), centerChunkX - mapInfo.getViewRadius());
        int maxX = Math.min(mapInfo.getMaxChunkX(), centerChunkX + mapInfo.getViewRadius());
        int minY = Math.max(mapInfo.getMinChunkY(), centerChunkY - mapInfo.getViewRadius());
        int maxY = Math.min(mapInfo.getMaxChunkY(), centerChunkY + mapInfo.getViewRadius());

        List<Integer> out = new ArrayList<>();
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                out.add(mapInfo.chunkPosToId(x, y));
            }
        }
        return out;
    }

    public void LastUpdate(BaseBattleRoom room) {
        if (room.getRoomState() != RoomState.ACTIVE) return;
    }
    //endregion
}
