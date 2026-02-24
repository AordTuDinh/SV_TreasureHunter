package game.dragonhero.table;

import game.battle.object.NInput;
import game.battle.object.Pos;
import game.dragonhero.server.Constans;
import game.dragonhero.server.IAction;
import game.protocol.ProtoState;
import io.netty.channel.Channel;
import ozudo.base.helper.Util;
import protocol.Pbmethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Minimal room for standalone mode: no DB/Redis.
 * Receives CLIENT_INPUT (move), updates per-channel state, sends TABLE_STATE periodically.
 */
public class StandaloneMoveRoom {

    private static final int TICK_MS = 50;
    private static final int DEFAULT_SPEED = 100;
    /** Units per second when applying direction (server-authoritative movement). */
    private static final float MOVE_SPEED = 5f;

    private static StandaloneMoveRoom instance;

    public static synchronized StandaloneMoveRoom getInstance() {
        if (instance == null) {
            instance = new StandaloneMoveRoom();
        }
        return instance;
    }

    private final Map<Channel, UnitState> channelToState = new ConcurrentHashMap<>();
    /** Input áp vào state đầu mỗi tick, trước simulateMovement() → không thiếu 1 frame. */
    private final ConcurrentLinkedQueue<PendingInput> pendingInputs = new ConcurrentLinkedQueue<>();

    private int idNext = 0;
    private int tickCount = 0;
    private volatile boolean running = true;

    private static class PendingInput {
        final Channel channel;
        final Pos dir;
        final long seq;

        PendingInput(Channel channel, Pos dir, long seq) {
            this.channel = channel;
            this.dir = dir;
            this.seq = seq;
        }
    }

    private static class UnitState {
        int id;
        Pos pos;
        Pos dir;
        long lastInputSeq;

        UnitState(int id) {
            this.id = id;
            this.pos = Pos.zero();
            this.dir = Pos.right();
            this.lastInputSeq = 0;
        }
    }

    public StandaloneMoveRoom() {
        Thread ticker = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(TICK_MS);
                    tickCount++;
                    applyPendingInputs();
                    simulateMovement();
                    if (tickCount % 20 == 0) {
                        for (UnitState u : channelToState.values())
                            System.out.println("[SV] pos id=" + u.id + " pos=(" + String.format("%.2f", u.pos.x) + "," + String.format("%.2f", u.pos.y) + ")");
                    }
                    byte[] data = genTableState();
                    if (data != null) {
                        for (Map.Entry<Channel, UnitState> e : new ArrayList<>(channelToState.entrySet())) {
                            Channel ch = e.getKey();
                            if (ch != null && ch.isActive() && ch.isWritable()) {
                                Util.sendGameData(ch, data, Constans.MAGIC_IN_PUT);
                            }
                        }
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception ex) {
                    System.err.println("StandaloneMoveRoom tick: " + ex.getMessage());
                }
            }
        }, "StandaloneMoveRoom-tick");
        ticker.setDaemon(true);
        ticker.start();
    }

    public void registerChannel(Channel channel) {
        channelToState.computeIfAbsent(channel, ch -> {
            synchronized (this) {
                idNext++;
                if (idNext > 201190) idNext = 0;
                return new UnitState(idNext);
            }
        });
    }

    public void doSyncAction(Channel channel, int actionId, byte[] body) {
        if (actionId != IAction.CLIENT_INPUT || body == null) return;
        NInput input = NInput.parse(body);
        if (input.typeId != NInput.INPUT_PLAYER_MOVE) return;
        UnitState state = channelToState.get(channel);
        if (state == null) {
            registerChannel(channel);
            state = channelToState.get(channel);
        }
        if (input.seq <= state.lastInputSeq) return;
        Pos dir = Pos.zero();
        if (input.playerDirection != null && input.playerDirection.magnitude() > 0)
            dir = input.playerDirection.normalized();
        pendingInputs.offer(new PendingInput(channel, dir, input.seq));
        if (input.seq % 20 == 0)
            System.out.println("[SV] input id=" + state.id + " dir=(" + String.format("%.2f", dir.x) + "," + String.format("%.2f", dir.y) + ")");
    }

    /** Drain pending inputs và áp vào state ngay trước simulateMovement() để không thiếu 1 frame. */
    private void applyPendingInputs() {
        PendingInput p;
        while ((p = pendingInputs.poll()) != null) {
            UnitState state = channelToState.get(p.channel);
            if (state != null && p.seq > state.lastInputSeq) {
                state.lastInputSeq = p.seq;
                state.dir = p.dir;
            }
        }
    }

    private void simulateMovement() {
        float dt = TICK_MS / 1000f;
        float move = MOVE_SPEED * dt;
        for (UnitState u : channelToState.values()) {
            float nx = u.pos.x + u.dir.x * move;
            float ny = u.pos.y + u.dir.y * move;
            u.pos = new Pos(nx, ny);
        }
    }

    /**
     * Giống BaseRoom (Ninja): chỉ return state khi có gì cần gửi (send = true).
     * Chỉ set send = true khi có ít nhất một unit đang move (dir != 0), tránh gửi pos liên tục khi idle.
     */
    private byte[] genTableState() {
        if (channelToState.isEmpty()) return null;
        float serverTime = System.currentTimeMillis() / 1000f;
        Pbmethod.PbState.Builder builder = Pbmethod.PbState.newBuilder();
        builder.setServerTime(serverTime);
        boolean send = false;
        for (UnitState u : channelToState.values()) {
            if (u.dir.x != 0 || u.dir.y != 0) send = true;
            Pbmethod.PbUnitPos.Builder pb = Pbmethod.PbUnitPos.newBuilder();
            pb.setId(u.id);
            pb.setPos(u.pos.toProto());
            pb.setDirection(u.dir.toProto());
            pb.setSpeed(DEFAULT_SPEED);
            pb.setLastInputSeq(u.lastInputSeq);
            builder.addUnitPos(pb.build());
        }
        if (send) return ProtoState.convertProtoBuffToState(builder.build());
        return null;
    }

    /** Remove channel when disconnected (optional; tick already checks isActive). */
    public void removeChannel(Channel channel) {
        channelToState.remove(channel);
    }
}
