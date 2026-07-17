package game.treasure.service.arena;

import game.battle.model.Player;
import game.battle.model.Unit;
import game.battle.object.Pos;
import game.config.CfgArena;
import game.config.aEnum.DetailActionType;
import game.config.lang.Lang;
import game.monitor.Online;
import game.object.MyUser;
import game.protocol.CommonProto;
import game.treasure.dao.UserDAO;
import game.treasure.mapping.UserWeekEntity;
import game.treasure.server.IAction;
import game.treasure.service.Services;
import game.treasure.service.user.Bonus;
import game.treasure.table.BaseRoom;
import ozudo.base.helper.Util;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Đấu trường La Mã (PvP 1-1 theo khung giờ).
 * <p>
 * Pool FIFO / server, ghép ±cupMatchRange, trận 90s, tele Home map.
 */
public final class ArenaService {
    public static final int RESULT_WIN = 1;
    public static final int RESULT_LOSE = 2;
    public static final int RESULT_DRAW = 3;
    public static final int RESULT_DISCONNECT = 4;
    public static final int RESULT_SPECTATOR = 5;

    private static final ArenaService INSTANCE = new ArenaService();

    /** serverId → danh sách đăng ký (FIFO) */
    private final Map<Integer, CopyOnWriteArrayList<QueueEntry>> pools = new ConcurrentHashMap<>();
    /** userId → match đang đánh */
    private final Map<Integer, ArenaMatch> userMatch = new ConcurrentHashMap<>();
    private final List<ArenaMatch> activeMatches = new CopyOnWriteArrayList<>();

    private final Map<Integer, Long> lastTickMs = new ConcurrentHashMap<>();

    private ArenaService() {
    }

    public static ArenaService getInstance() {
        return INSTANCE;
    }

    /** Gọi mỗi giây từ HomeRoom — debounce ~1s, tick mọi server có pool/match. */
    public void tick(int serverId) {
        long now = System.currentTimeMillis();
        Long last = lastTickMs.get(-1);
        if (last != null && now - last < 900)
            return;
        lastTickMs.put(-1, now);
        Set<Integer> servers = new HashSet<>();
        servers.add(serverId);
        servers.addAll(pools.keySet());
        for (ArenaMatch m : activeMatches)
            servers.add(m.serverId);
        for (Integer sid : servers)
            tickInternal(sid, now);
    }

    void tickInternal(int serverId, long now) {
        try {
            if (!CfgArena.isWindowOpen(now)) {
                clearPoolRefund(serverId);
            } else if (CfgArena.canMatch(now)) {
                tryMatch(serverId, now);
            }
            tickMatches(serverId, now);
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    public static class QueueEntry {
        public final int userId;
        public final int cupAtRegister;
        public final long registerMs;

        public QueueEntry(int userId, int cupAtRegister, long registerMs) {
            this.userId = userId;
            this.cupAtRegister = cupAtRegister;
            this.registerMs = registerMs;
        }
    }

    public boolean isRegistered(int userId) {
        for (CopyOnWriteArrayList<QueueEntry> pool : pools.values()) {
            for (QueueEntry e : pool) {
                if (e.userId == userId)
                    return true;
            }
        }
        return false;
    }

    public boolean isInMatch(int userId) {
        return userMatch.containsKey(userId);
    }

    public ArenaMatch getMatch(int userId) {
        return userMatch.get(userId);
    }

    /** Status wire: [isOpen, remainMs, isRegistered, cup, minCup, arenaCoin, registerGem, queueCount] */
    public List<Long> buildStatus(MyUser mUser) {
        long now = System.currentTimeMillis();
        boolean open = CfgArena.isWindowOpen(now);
        long remain = open ? CfgArena.msUntilClose(now) : CfgArena.msUntilOpen(now);
        int registered = isRegistered(mUser.getUserId()) || isInMatch(mUser.getUserId()) ? 1 : 0;
        long arenaCoin = mUser.getResources() != null
                ? mUser.getResources().getItemPointNumber(CfgArena.arenaCoinPointId())
                : 0;
        int server = mUser.getUser().getServer();
        return Arrays.asList(
                open ? 1L : 0L,
                remain,
                (long) registered,
                (long) mUser.getUser().getCup(),
                (long) CfgArena.minCup(),
                arenaCoin,
                (long) CfgArena.registerGem(),
                (long) queueSize(server)
        );
    }

    public int queueSize(int serverId) {
        CopyOnWriteArrayList<QueueEntry> pool = pools.get(serverId);
        return pool == null ? 0 : pool.size();
    }

    /**
     * @param outBonus gem wire trả client (có thể null)
     * @return null nếu OK, ngược lại message lỗi
     */
    public String register(MyUser mUser, List<Long> outBonus) {
        long now = System.currentTimeMillis();
        if (!CfgArena.isWindowOpen(now))
            return Lang.instance(mUser).get(Lang.err_arena_closed);
        if (mUser.getUser().getCup() < CfgArena.minCup())
            return Lang.instance(mUser).get(Lang.err_arena_not_enough_cup);
        if (isRegistered(mUser.getUserId()) || isInMatch(mUser.getUserId()))
            return Lang.instance(mUser).get(Lang.err_arena_already_registered);

        List<Long> fee = Bonus.viewGem(-CfgArena.registerGem());
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null)
            return err;
        List<Long> wire = Bonus.receiveListItem(mUser, DetailActionType.ARENA_REGISTER.getKey(), fee);
        if (wire.isEmpty())
            return Lang.instance(mUser).get(Lang.err_system_down);

        int server = mUser.getUser().getServer();
        pools.computeIfAbsent(server, s -> new CopyOnWriteArrayList<>())
                .add(new QueueEntry(mUser.getUserId(), mUser.getUser().getCup(), now));
        if (outBonus != null)
            outBonus.addAll(wire);
        return null;
    }

    public String cancel(MyUser mUser, List<Long> outBonus) {
        if (isInMatch(mUser.getUserId()))
            return Lang.instance(mUser).get(Lang.err_arena_in_match);
        QueueEntry removed = removeFromPool(mUser.getUserId());
        if (removed == null)
            return Lang.instance(mUser).get(Lang.err_arena_not_registered);

        List<Long> refund = refundRegisterGem(mUser);
        if (outBonus != null && refund != null)
            outBonus.addAll(refund);
        return null;
    }

    List<Long> refundRegisterGem(MyUser mUser) {
        if (mUser == null)
            return Collections.emptyList();
        return Bonus.receiveListItem(mUser, DetailActionType.ARENA_CANCEL.getKey(),
                Bonus.viewGem(CfgArena.registerGem()));
    }

    QueueEntry removeFromPool(int userId) {
        for (CopyOnWriteArrayList<QueueEntry> pool : pools.values()) {
            for (QueueEntry e : pool) {
                if (e.userId == userId) {
                    pool.remove(e);
                    return e;
                }
            }
        }
        return null;
    }

    void clearPoolRefund(int serverId) {
        CopyOnWriteArrayList<QueueEntry> pool = pools.get(serverId);
        if (pool == null || pool.isEmpty())
            return;
        for (QueueEntry e : new ArrayList<>(pool)) {
            pool.remove(e);
            MyUser mUser = Online.getMUser(e.userId);
            if (mUser != null) {
                List<Long> wire = refundRegisterGem(mUser);
                if (mUser.getPlayer() != null && wire != null && !wire.isEmpty())
                    mUser.getPlayer().protoStatus(Pbmethod.SubStateType.ADD_BONUS, wire);
            }
        }
    }

    void tryMatch(int serverId, long now) {
        CopyOnWriteArrayList<QueueEntry> pool = pools.get(serverId);
        if (pool == null || pool.size() < 2)
            return;

        List<QueueEntry> snapshot = new ArrayList<>(pool);
        Set<Integer> matched = new HashSet<>();
        for (int i = 0; i < snapshot.size(); i++) {
            QueueEntry a = snapshot.get(i);
            if (matched.contains(a.userId) || !poolContains(pool, a.userId))
                continue;
            MyUser userA = Online.getMUser(a.userId);
            if (userA == null) {
                // offline đã xử lý ở onDisconnect; dọn sót
                pool.remove(a);
                continue;
            }
            if (userA.getUser().getCup() < CfgArena.minCup()) {
                pool.remove(a);
                refundRegisterGem(userA);
                continue;
            }
            if (!readyForMatch(userA))
                continue;
            QueueEntry best = null;
            MyUser userB = null;
            for (int j = i + 1; j < snapshot.size(); j++) {
                QueueEntry b = snapshot.get(j);
                if (matched.contains(b.userId) || !poolContains(pool, b.userId))
                    continue;
                MyUser cand = Online.getMUser(b.userId);
                if (cand == null) {
                    pool.remove(b);
                    continue;
                }
                if (cand.getUser().getCup() < CfgArena.minCup()) {
                    pool.remove(b);
                    refundRegisterGem(cand);
                    continue;
                }
                if (!readyForMatch(cand))
                    continue;
                int cupA = userA.getUser().getCup();
                int cupB = cand.getUser().getCup();
                if (Math.abs(cupA - cupB) <= CfgArena.cupMatchRange()) {
                    best = b;
                    userB = cand;
                    break; // FIFO: đối thủ hợp lệ sớm nhất
                }
            }
            if (best == null || userB == null)
                continue;
            pool.remove(a);
            pool.remove(best);
            matched.add(a.userId);
            matched.add(best.userId);
            startMatch(serverId, userA, userB, now);
        }
    }

    boolean readyForMatch(MyUser mUser) {
        Player p = mUser.getPlayer();
        return p != null && p.getRoom() != null && p.isAlive();
    }

    void requeue(int serverId, MyUser mUser) {
        if (mUser == null || isRegistered(mUser.getUserId()) || isInMatch(mUser.getUserId()))
            return;
        pools.computeIfAbsent(serverId, s -> new CopyOnWriteArrayList<>())
                .add(new QueueEntry(mUser.getUserId(), mUser.getUser().getCup(), System.currentTimeMillis()));
    }

    boolean poolContains(CopyOnWriteArrayList<QueueEntry> pool, int userId) {
        for (QueueEntry e : pool) {
            if (e.userId == userId)
                return true;
        }
        return false;
    }

    void startMatch(int serverId, MyUser userA, MyUser userB, long now) {
        if (userA == null || userB == null)
            return;
        Player pA = userA.getPlayer();
        Player pB = userB.getPlayer();
        if (pA == null || pB == null || pA.getRoom() == null || pB.getRoom() == null) {
            // đã remove khỏi pool — trả lại để không mất slot
            requeue(serverId, userA);
            requeue(serverId, userB);
            return;
        }

        ArenaMatch match = new ArenaMatch(serverId, userA.getUserId(), userB.getUserId(), now);
        activeMatches.add(match);
        userMatch.put(userA.getUserId(), match);
        userMatch.put(userB.getUserId(), match);

        float[] a = CfgArena.posA();
        float[] b = CfgArena.posB();
        float[] da = CfgArena.dirA();
        float[] db = CfgArena.dirB();
        teleport(pA, a[0], a[1], da[0], da[1]);
        teleport(pB, b[0], b[1], db[0], db[1]);
        match.resetMoveSpeed(pA);
        match.resetMoveSpeed(pB);

        // notify both: match started [2, opponentId, durationMs]
        Util.sendProtoData(userA.getChannel(),
                CommonProto.getCommonVector(2L, (long) userB.getUserId(), CfgArena.matchDurationSec() * 1000L),
                IAction.ARENA_STATUS);
        Util.sendProtoData(userB.getChannel(),
                CommonProto.getCommonVector(2L, (long) userA.getUserId(), CfgArena.matchDurationSec() * 1000L),
                IAction.ARENA_STATUS);
    }

    void teleport(Player player, float x, float y, float dirX, float dirY) {
        if (player == null)
            return;
        player.setPosAndDirection(new Pos(x, y), new Pos(dirX, dirY));
    }

    void tickMatches(int serverId, long now) {
        for (ArenaMatch match : new ArrayList<>(activeMatches)) {
            if (match.serverId != serverId || match.finished)
                continue;
            MyUser uA = Online.getMUser(match.userA);
            MyUser uB = Online.getMUser(match.userB);
            Player pA = uA != null ? uA.getPlayer() : null;
            Player pB = uB != null ? uB.getPlayer() : null;

            if (pA == null || pB == null) {
                // một bên offline → xử lý disconnect
                if (pA == null && pB != null)
                    finishDisconnect(match, match.userA, match.userB);
                else if (pB == null && pA != null)
                    finishDisconnect(match, match.userB, match.userA);
                else
                    cleanupMatch(match);
                continue;
            }

            match.tickMove(pA, pB);
            match.applyMoveSpeed(pA, true);
            match.applyMoveSpeed(pB, false);

            if (now >= match.endMs) {
                finishByHpPercent(match, pA, pB);
            }
        }
    }

    /**
     * @return true nếu đã xử lý arena (bỏ DeathPenalty / PvpCup open-world)
     */
    public boolean handleDeath(Player victim, Unit killer) {
        if (victim == null || victim.getMUser() == null)
            return false;
        ArenaMatch match = userMatch.get(victim.getMUser().getUserId());
        if (match == null || match.finished)
            return false;

        int loserId = victim.getMUser().getUserId();
        int winnerId = match.opponentOf(loserId);
        if (killer != null && killer.isPlayer() && killer.getPlayer().getMUser() != null) {
            int killerId = killer.getPlayer().getMUser().getUserId();
            if (match.isParticipant(killerId))
                winnerId = killerId;
        }
        finishKill(match, winnerId, loserId);
        return true;
    }

    public void onDisconnect(int userId) {
        ArenaMatch match = userMatch.get(userId);
        if (match != null && !match.finished) {
            int winnerId = match.opponentOf(userId);
            finishDisconnect(match, userId, winnerId);
            return;
        }
        QueueEntry removed = removeFromPool(userId);
        if (removed != null) {
            MyUser mUser = Online.getMUser(userId);
            refundRegisterGem(mUser);
        }
    }

    void finishKill(ArenaMatch match, int winnerId, int loserId) {
        applyResult(match, winnerId, loserId, RESULT_WIN, RESULT_LOSE,
                CfgArena.cupWin(), CfgArena.coinWin(),
                CfgArena.cupLose(), CfgArena.coinLose());
    }

    void finishDisconnect(ArenaMatch match, int loserId, int winnerId) {
        // disconnect: thua −cup, không xu; thắng +cup +xu
        applyResult(match, winnerId, loserId, RESULT_WIN, RESULT_DISCONNECT,
                CfgArena.cupWin(), CfgArena.coinWin(),
                CfgArena.cupLose(), 0);
    }

    void finishByHpPercent(ArenaMatch match, Player pA, Player pB) {
        float pctA = hpPercent(pA);
        float pctB = hpPercent(pB);
        if (Math.abs(pctA - pctB) < 0.0001f) {
            applyDraw(match);
            return;
        }
        if (pctA > pctB)
            finishKill(match, match.userA, match.userB);
        else
            finishKill(match, match.userB, match.userA);
    }

    float hpPercent(Player p) {
        if (p == null || p.getPoint() == null || p.getPoint().getMaxHp() <= 0)
            return 0f;
        return p.getPoint().getCurHP() * 1f / p.getPoint().getMaxHp();
    }

    void applyDraw(ArenaMatch match) {
        match.finished = true;
        MyUser uA = Online.getMUser(match.userA);
        MyUser uB = Online.getMUser(match.userB);
        List<Long> bonusA = grantRewards(uA, 0, CfgArena.coinDraw(), DetailActionType.ARENA_DRAW.getKey());
        List<Long> bonusB = grantRewards(uB, 0, CfgArena.coinDraw(), DetailActionType.ARENA_DRAW.getKey());
        sendEnd(uA, RESULT_DRAW, 0, match.userB, bonusA);
        sendEnd(uB, RESULT_DRAW, 0, match.userA, bonusB);
        teleportEnd(uA, true);
        teleportEnd(uB, false);
        cleanupMatch(match);
    }

    void applyResult(ArenaMatch match, int winnerId, int loserId,
                     int resultWin, int resultLose,
                     int cupWin, int coinWin, int cupLose, int coinLose) {
        if (match.finished)
            return;
        match.finished = true;

        MyUser winner = Online.getMUser(winnerId);
        MyUser loser = Online.getMUser(loserId);

        List<Long> bonusWin = grantRewards(winner, cupWin, coinWin, DetailActionType.ARENA_WIN.getKey());
        List<Long> bonusLose = grantRewards(loser, cupLose, coinLose,
                resultLose == RESULT_DISCONNECT
                        ? DetailActionType.ARENA_DISCONNECT.getKey()
                        : DetailActionType.ARENA_LOSE.getKey());

        if (winner != null) {
            addArenaWinRank(winner);
            sendEnd(winner, resultWin, winnerId, loserId, bonusWin);
            broadcastSpectator(winner, winnerId);
        }
        if (loser != null)
            sendEnd(loser, resultLose, winnerId, loserId, bonusLose);

        teleportEnd(winner, winnerId == match.userA);
        teleportEnd(loser, loserId == match.userA);
        if (winner != null && winner.getPlayer() != null)
            match.resetMoveSpeed(winner.getPlayer());
        if (loser != null && loser.getPlayer() != null)
            match.resetMoveSpeed(loser.getPlayer());
        cleanupMatch(match);
    }

    void addArenaWinRank(MyUser mUser) {
        try {
            UserDAO dao = Services.userDAO;
            if (dao == null)
                return;
            UserWeekEntity week = dao.getUserWeek(mUser);
            if (week == null)
                return;
            week.checkData();
            week.addArenaWin(1);
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    List<Long> grantRewards(MyUser mUser, int cupDelta, int coinDelta, String detail) {
        if (mUser == null)
            return Collections.emptyList();
        List<Long> bonus = new ArrayList<>();
        if (cupDelta != 0)
            bonus.addAll(Bonus.viewCup(cupDelta));
        if (coinDelta != 0)
            bonus.addAll(Bonus.viewArenaCoin(coinDelta));
        if (bonus.isEmpty())
            return Collections.emptyList();
        return Bonus.receiveListItem(mUser, detail, bonus);
    }

    void sendEnd(MyUser mUser, int result, int winnerId, int opponentId, List<Long> bonus) {
        if (mUser == null || mUser.getChannel() == null)
            return;
        Pbmethod.CommonVector.Builder b = Pbmethod.CommonVector.newBuilder();
        b.addALong(result);
        b.addALong(winnerId);
        b.addALong(opponentId);
        if (bonus != null) {
            for (Long v : bonus)
                b.addALong(v);
        }
        Util.sendProtoData(mUser.getChannel(), b.build(), IAction.ARENA_END);
        if (bonus != null && !bonus.isEmpty() && mUser.getPlayer() != null)
            mUser.getPlayer().protoStatus(Pbmethod.SubStateType.ADD_BONUS, bonus);
    }

    void broadcastSpectator(MyUser winner, int winnerId) {
        Player player = winner.getPlayer();
        if (player == null || player.getRoom() == null)
            return;
        BaseRoom room = player.getRoom();
        int chunkId = room.worldPosToChunkId(player.getPos());
        Set<Long> unitIds = room.getChunkCharacterIds(chunkId);
        if (unitIds == null)
            return;
        Pbmethod.CommonVector payload = CommonProto.getCommonVector(RESULT_SPECTATOR, winnerId);
        for (Long uid : unitIds) {
            Unit u = room.getPlayerId(uid);
            if (u == null || !u.isPlayer())
                continue;
            Player p = u.getPlayer();
            if (p == null || p.getMUser() == null)
                continue;
            int id = p.getMUser().getUserId();
            if (id == winnerId)
                continue;
            if (isInMatch(id))
                continue;
            Util.sendProtoData(p.getMUser().getChannel(), payload, IAction.ARENA_END);
        }
    }

    void teleportEnd(MyUser mUser, boolean isA) {
        if (mUser == null || mUser.getPlayer() == null)
            return;
        float[] end = isA ? CfgArena.posEndA() : CfgArena.posEndB();
        float[] dir = isA ? CfgArena.dirA() : CfgArena.dirB();
        teleport(mUser.getPlayer(), end[0], end[1], dir[0], dir[1]);
    }

    void cleanupMatch(ArenaMatch match) {
        match.finished = true;
        activeMatches.remove(match);
        userMatch.remove(match.userA);
        userMatch.remove(match.userB);
    }
}
