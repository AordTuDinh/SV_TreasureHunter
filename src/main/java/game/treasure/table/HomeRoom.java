package game.treasure.table;

import game.battle.model.ChunkObject;
import game.battle.model.Player;
import game.battle.model.Unit;
import game.battle.object.NInput;
import game.battle.type.RoomState;
import game.treasure.mapping.main.ResMapEntity;
import protocol.Pbmethod;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class HomeRoom extends BaseBattleRoom {
    private static final int HEAL_PER_SECOND = 20;
    private static final int VERIFY_INTERVAL_SECONDS = 30;

    private final Set<Long> healZonePlayers = new HashSet<>();
    private int healZoneVerifyCounter;

    public HomeRoom(ResMapEntity mapInfo, Map<Integer, ChunkObject> mChunk, String keyRoom) {
        super(mapInfo, mChunk, keyRoom);
    }

    @Override
    protected void startInit() {
        super.startInit();
        roomState = RoomState.ACTIVE;
    }

    public void handleZoneHeathInput(Player player, NInput input) {
        if (player == null || !player.isAlive()) {
            return;
        }

        boolean wantIn = input.typeId == NInput.ADD_ZONE_HEATH;
        boolean inZone = mapInfo.isInHeathZone(player.getPos());

        if (wantIn) {
            if (!inZone) {
                pushHealZoneStatus(player, 0);
                return;
            }
            healZonePlayers.add(player.getId());
            pushHealZoneStatus(player, 1);
            return;
        }

        if (inZone) {
            pushHealZoneStatus(player, 1);
            return;
        }
        healZonePlayers.remove(player.getId());
        pushHealZoneStatus(player, 0);
    }

    void pushHealZoneStatus(Player player, int status) {
        player.protoStatus(Pbmethod.SubStateType.IN_HEAL_ZONE, (long) status);
    }

    @Override
    public void Update1s() {
        super.Update1s();
        processHealZoneTick();
        // Arena tick theo server của room
        try {
            if (mapInfo != null) {
                // keyRoom / players share same server — lấy từ player đầu tiên nếu có
                for (Long pid : aPlayerIds) {
                    Unit u = getPlayerId(pid);
                    if (u != null && u.isPlayer() && u.getPlayer().getMUser() != null) {
                        int serverId = u.getPlayer().getMUser().getUser().getServer();
                        game.treasure.service.arena.ArenaService.getInstance().tick(serverId);
                        break;
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    void processHealZoneTick() {
        healZoneVerifyCounter++;
        boolean doVerify = healZoneVerifyCounter >= VERIFY_INTERVAL_SECONDS;
        if (doVerify) healZoneVerifyCounter = 0;

        Iterator<Long> it = healZonePlayers.iterator();
        while (it.hasNext()) {
            Long playerId = it.next();
            Player player = (Player) getPlayerId(playerId);
            if (player == null) {
                it.remove();
                continue;
            }
            if (!player.isAlive()) {
                it.remove();
                pushHealZoneStatus(player, 0);
                continue;
            }

            if (doVerify && !mapInfo.isInHeathZone(player.getPos())) {
                it.remove();
                pushHealZoneStatus(player, 0);
                continue;
            }

            healPlayer(player);
        }
    }

    void healPlayer(Player player) {
        if (!player.isAlive()) return;
        long cur = player.getPoint().getCurHP();
        long max = player.getPoint().getMaxHp();
        if (cur >= max) return;

        int heal = (int) Math.min(HEAL_PER_SECOND, max - cur);
        player.reHpFixed(heal);
    }

    @Override
    public void removeUnit(long idInMap) {
        healZonePlayers.remove(idInMap);
        super.removeUnit(idInMap);
    }

    @Override
    public void characterDie(Unit unit) {
        super.characterDie(unit);
        if (unit == null || !unit.isPlayer()) return;
        if (!healZonePlayers.remove(unit.getId())) return;
        pushHealZoneStatus(unit.getPlayer(), 0);
    }
}
