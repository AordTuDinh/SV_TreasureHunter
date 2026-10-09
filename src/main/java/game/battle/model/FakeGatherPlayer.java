package game.battle.model;

import game.battle.object.Point;
import game.battle.object.Pos;
import game.battle.type.UnitType;
import game.treasure.BattleConfig;
import protocol.Pbmethod;

import java.util.ArrayList;
import java.util.List;

/**
 * Người chơi giả trên làng tân thủ. Chỉ số theo đồ đang mặc.
 * Đào ô, đánh quái và chết như người chơi, nhưng không có túi đồ.
 */
public class FakeGatherPlayer extends Player {
    final int fakeUserId;
    final int bodySkinId;
    final List<Integer> equipView;
    CellObject gatherCell;
    Pos standPos = Pos.zero();
    long nextRetargetMs;
    long threatId;
    long reviveAtMs;

    public FakeGatherPlayer(String name, int fakeUserId, int bodySkinId, List<Integer> equipView, Point point) {
        super(0, point, UnitType.PLAYER);
        this.name = name;
        this.fakeUserId = fakeUserId;
        this.bodySkinId = bodySkinId;
        this.equipView = equipView;
        this.alive = true;
        this.ready = true;
    }

    public CellObject getGatherCell() {
        return gatherCell;
    }

    public Pos getStandPos() {
        return standPos;
    }

    public int getFakeUserId() {
        return fakeUserId;
    }

    public long getNextRetargetMs() {
        return nextRetargetMs;
    }

    public long getThreatId() {
        return threatId;
    }

    public void clearThreat() {
        threatId = 0;
    }

    public long getReviveAtMs() {
        return reviveAtMs;
    }

    public void noteAggro(Unit attacker) {
        if (attacker != null && attacker.isEnemy() && attacker.isAlive()) threatId = attacker.getId();
    }

    public void assignCell(CellObject cell, Pos standPos, long mineUntilMs) {
        this.gatherCell = cell;
        this.standPos = standPos != null ? standPos : Pos.zero();
        this.nextRetargetMs = mineUntilMs;
        if (cell != null && this.pos != null) {
            Pos face = this.pos.getDirectionTo(cell.getPos());
            if (face != null && (face.x != 0f || face.y != 0f)) {
                this.direction = face;
            }
        }
    }

    @Override
    public void beAttackDamage(Unit ownerDamage, long atkDame) {
        noteAggro(ownerDamage);
        super.beAttackDamage(ownerDamage, atkDame);
    }

    @Override
    public synchronized void protoDie(Unit killer) {
        if (killer != null) this.killById = killer.getId();
        if (room != null) room.characterDie(this);
        if (sendDie) {
            protoStatus(Pbmethod.SubStateType.DIE);
            sendDie = false;
        }
        threatId = 0;
        reviveAtMs = System.currentTimeMillis() + 4000L;
    }

    /** Hồi sinh tại làng, 50% máu như người chơi bấm hồi sinh. Không trừ đồ. */
    public void reviveAtVillage(Pos spawnPos) {
        alive = true;
        sendDie = true;
        reviveAtMs = 0;
        threatId = 0;
        if (point != null) point.resetHpPercent(BattleConfig.P_reviveHpPercent);
        Pos spawn = spawnPos != null ? spawnPos.clone() : Pos.zero();
        setPosAndDirection(spawn, direction != null ? direction : Pos.right());
        protoStatus(Pbmethod.SubStateType.REVIVE,
                (long) (pos.x * 1000), (long) (pos.y * 1000), 0L);
        if (point != null) protoStatus(Pbmethod.SubStateType.UPDATE_MULTI_POINT, point.toProto());
    }

    @Override
    public Pbmethod.PbUnit toProtoRemove(int chunkId) {
        Pbmethod.PbUnit.Builder builder = Pbmethod.PbUnit.newBuilder();
        builder.setType(type.value);
        builder.setChunkId(chunkId);
        builder.setId(id);
        builder.setIsAdd(false);
        builder.setUserId(fakeUserId);
        return builder.build();
    }

    @Override
    public List<Integer> getListInfo(int effInit) {
        List<Integer> lst = new ArrayList<>();
        lst.add(idDameSkin);
        lst.add(idChatFrame);
        lst.add(idTrial);
        lst.add(effInit);
        if (equipView != null) lst.addAll(equipView);
        return lst;
    }

    @Override
    public Pbmethod.PbUnit toProtoAdd(int chunkId) {
        Pbmethod.PbUnit.Builder pbAdd = Pbmethod.PbUnit.newBuilder();
        pbAdd.setType(UnitType.PLAYER.value);
        pbAdd.setId(id);
        pbAdd.setChunkId(chunkId);
        pbAdd.setIsAdd(true);
        pbAdd.setPos(pos.toProto());
        pbAdd.setDirection(direction.toProto());
        pbAdd.setClanId(0);
        pbAdd.setRangeAttack(rangeAttack);
        pbAdd.setAvatar(bodySkinId);
        pbAdd.setSpeed((int) point.getMoveSpeed());
        pbAdd.setName(name);
        pbAdd.setAlive(alive);
        pbAdd.setLastInputSeq(0);
        pbAdd.addAllPoint(point.toProto());
        pbAdd.addAllInfo(getListInfo(0));
        pbAdd.setUserId(fakeUserId);
        return pbAdd.build();
    }
}
