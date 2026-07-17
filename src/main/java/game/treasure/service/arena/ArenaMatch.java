package game.treasure.service.arena;

import game.battle.model.Player;
import game.battle.object.Point;
import game.battle.object.Pos;
import game.config.CfgArena;

/** Trạng thái 1 trận arena 1v1. */
public class ArenaMatch {
    public final int serverId;
    public final int userA;
    public final int userB;
    public final long startMs;
    public final long endMs;

    public int moveSecA;
    public int moveSecB;
    public long lastMoveSpeedA = 100;
    public long lastMoveSpeedB = 100;
    public boolean finished;

    public ArenaMatch(int serverId, int userA, int userB, long startMs) {
        this.serverId = serverId;
        this.userA = userA;
        this.userB = userB;
        this.startMs = startMs;
        this.endMs = startMs + CfgArena.matchDurationSec() * 1000L;
    }

    public boolean isParticipant(int userId) {
        return userId == userA || userId == userB;
    }

    public int opponentOf(int userId) {
        if (userId == userA)
            return userB;
        if (userId == userB)
            return userA;
        return 0;
    }

    public void tickMove(Player playerA, Player playerB) {
        if (playerA != null && playerA.isAlive() && playerA.isMove())
            moveSecA = Math.min(CfgArena.moveNormalSec() + CfgArena.moveSlowSec(), moveSecA + 1);
        if (playerB != null && playerB.isAlive() && playerB.isMove())
            moveSecB = Math.min(CfgArena.moveNormalSec() + CfgArena.moveSlowSec(), moveSecB + 1);
    }

    public static long calcChangeMoveSpeed(int moveSec) {
        int normal = CfgArena.moveNormalSec();
        if (moveSec <= normal)
            return 100;
        int over = moveSec - normal;
        long speed = 100L - (long) over * CfgArena.moveSlowPercentPerSec();
        return Math.max(0, speed);
    }

    public void applyMoveSpeed(Player player, boolean isA) {
        if (player == null || player.getPoint() == null)
            return;
        int moveSec = isA ? moveSecA : moveSecB;
        long target = calcChangeMoveSpeed(moveSec);
        long last = isA ? lastMoveSpeedA : lastMoveSpeedB;
        if (target == last)
            return;
        Point point = player.getPoint();
        int current = (int) point.get(Point.CHANGE_MOVE_SPEED);
        point.add(Point.CHANGE_MOVE_SPEED, (int) target - current);
        player.protoUpdatePoint((long) Point.CHANGE_MOVE_SPEED, point.get(Point.CHANGE_MOVE_SPEED));
        if (isA)
            lastMoveSpeedA = target;
        else
            lastMoveSpeedB = target;
    }

    public void resetMoveSpeed(Player player) {
        if (player == null || player.getPoint() == null)
            return;
        Point point = player.getPoint();
        int current = (int) point.get(Point.CHANGE_MOVE_SPEED);
        if (current == 100)
            return;
        point.add(Point.CHANGE_MOVE_SPEED, 100 - current);
        player.protoUpdatePoint((long) Point.CHANGE_MOVE_SPEED, point.get(Point.CHANGE_MOVE_SPEED));
    }

    public static Pos toPos(float[] xy) {
        return new Pos(xy[0], xy[1]);
    }
}
