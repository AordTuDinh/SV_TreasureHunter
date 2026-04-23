package game.battle.model;

import game.battle.calculate.IMath;
import game.battle.calculate.MathLab;
import game.battle.object.*;
import game.battle.type.*;
import game.treasure.BattleConfig;
import game.treasure.mapping.main.ResMapEntity;
import game.treasure.table.BaseBattleRoom;
import game.treasure.table.BaseRoom;
import game.object.PointBuff;
import lombok.Data;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.NumberUtil;
import protocol.Pbmethod;

import java.util.*;

@Data
public abstract class Unit {
    // new
    int chunkId;
    long id; // tất cả các sinh vật đều sẽ có id này riêng lẻ, vào map sẽ gen id này
    // info
    // int id; // id in room
    int clanId;

    float rangeAttack;
    int idDameSkin = 0;
    int idChatFrame = 0;
    int idTrial = 0;
    Pos pos = Pos.zero(), direction = Pos.right();
    Point point;
    String name;

    AttackType attackType;
    boolean alive, beDot;
    BaseRoom room;
    int model;
    UnitType type;
    // in battle
    Pos instancePos;  // noi character sinh ra
    ResMapEntity panelMap;//bot left + top right
    long timeDie, timeRevive;
    boolean hasBonusKillMe;
    long timeBeHit = 0;
    long killById;

    public Map<Long, Long> beDameInfo = new HashMap<>();

    // todo test move
    boolean isMove = false; // dang di chuyen
    long timeActionMove;  // thời gian thay đổi action move
    Pos targetMove;
    boolean ready = true;
    long timeJoinRoom;
    long timeActiveRandomMove;
    long timeActionAttack;
    long timeCheckDirectionAttack;
    //long timeBeAttack;
    Pos directionMoveAttack = Pos.zero(); // chưa có hướng move attack melee
    boolean sendDie = true;


    public boolean isPlayer() {
        return type == UnitType.PLAYER;
    }


    public boolean isEnemy() {
        return type == UnitType.ENEMY;
    }


    public void setPosAndDirection(Pos newPos, Pos newDirection) {
        pos = newPos.round();
        direction = newDirection.normalized();
        setMove(true);
        updateChunkByPos(pos);
    }

    protected void updateChunkByPos(Pos worldPos) {
        if (room == null || worldPos == null) return;
        int oldChunk = chunkId;
        int newChunk = room.worldPosToChunkId(worldPos);
        if (room.joinChunk(this, newChunk) && type == UnitType.PLAYER) {
            room.syncViewDeltaForPlayer(this.getPlayer(), oldChunk, newChunk);
        }
    }


    public void attackUnit(Unit target) {
        long[] damage = IMath.calculateDamage(this, target);
        target.beAttackDamage(this, damage[1]);
        target.protoBeDame(this, Arrays.asList(damage[0], -damage[1]));
    }


    // gửi riêng
    public void beAttackDamage(Unit ownerDamage, long atkDame) {
        updateHp(ownerDamage, -atkDame);
        if (!alive) {
            timeDie = System.currentTimeMillis();
            protoDie(ownerDamage);
            if (checkHasBonusKill()) bonusKillMe(ownerDamage);
        } else {
            timeBeHit = System.currentTimeMillis();
        }
    }

    public Player getPlayer() {
        return (Player) this;
    }

    public Pet getPetUse() {
        return (Pet) this;
    }

    public Enemy getEnemy() {
        return (Enemy) this;
    }

    public BaseBattleRoom getBattleRoom() {
        return (BaseBattleRoom) room;
    }

    public boolean hasReceiveEffMelee(Unit attacker) {
        return canBeMelee();
    }

    public boolean isReviveReady() {
        return DateTime.isAfterTime(timeRevive, BattleConfig.E_ReviveReady);
    }

    public boolean canBeAttack(int teamId) {
        return isAlive() && isReady() && isReviveReady() && !sameTeam(teamId);
    }

    public boolean canAttack() {
        return !room.chunkNoAttack.contains(chunkId);
    }


    public boolean canBeMelee() {
        return isAlive() && isReady() && isReviveReady();
    }

    public boolean sameTeam(int teamId) {
        return this.clanId == teamId;
    }


    public void Update() {
    }

    public boolean sameTeam(Unit other) {
        return other.getClanId() == this.clanId;
    }

    public void stun(float time) {
        long timStun = (long) (time * 1000);
        point.addStun(timStun);
        protoStatus(StateType.UPDATE_MULTI_POINT, 2, List.of( (long)Point.STUN,  timStun));
    }


    public void setDirection(Pos direction) {
        if (beBlock()) return;
        this.direction = direction;
    }

    public void move(Pos newPos) {
        if (beBlock()) return;
        pos.v_add(panelMap, newPos);
        setMove(true);
        updateChunkByPos(pos);
    }

    public void setMove(boolean isMove) {
        if (isMove) timeActionMove = System.currentTimeMillis();
        this.isMove = isMove;
    }

    public boolean isMove() {
        this.isMove = !DateTime.isAfterTime(timeActionMove, BattleConfig.P_timeNoMove);
        return isMove;
    }


    public boolean inSizeHit(Pos posTarget, float r) {
        return MathLab.pointInCircle(this.pos, r, posTarget);
    }


    public boolean isLikeFace(Pos newDirection) { // check lật mặt
        return direction.x * newDirection.x > 0;
    }


    public Point resetData() {
        point.resetHpMp();
        alive = true;
        hasBonusKillMe = true;
        targetMove = null;
        timeBeHit = 0;
        timeActionAttack = 0;
        return point;
    }


    public Pbmethod.PbUnitPos toProtoPos() {
        // default for all enemy -  override custom for player
        Pbmethod.PbUnitPos.Builder pbUser = Pbmethod.PbUnitPos.newBuilder();
        pbUser.setId(id);
        pbUser.setPos(pos.toProto());
        pbUser.setDirection(direction.toProto());
        pbUser.setSpeed((int) point.getMoveSpeed());
        pbUser.setChunkId(chunkId);
        return pbUser.build();
    }

    public abstract Pbmethod.PbUnit toProtoAdd(int chunkId);

    public void revive() {
    }

    public float getCurSpeed() {
        return point.getMoveSpeed() / BattleConfig.C_SCALE_SPEED;
    }

    public void setTimeAttack() {
        timeActionAttack = System.currentTimeMillis();
    }


    public Pbmethod.PbUnit toProtoRemove(int chunkId) {
        Pbmethod.PbUnit.Builder builder = Pbmethod.PbUnit.newBuilder();
        builder.setType(type.value);
        builder.setChunkId(chunkId);
        builder.setId(id);
        builder.setIsAdd(false);
        return builder.build();
    }

    public synchronized void protoDie(Unit killer) {
        this.killById = killer.getId();
        room.characterDie(this);
    }

    public void updateHp(Unit attacker, long atkDame) {
        List<PointBuff> buffs = new ArrayList<>();
        buffs.add(new PointBuff(Point.CUR_HP, atkDame));
        protoBuffPoint(buffs);
        ((BaseBattleRoom) room).ChangeCharacterHp(attacker, this, atkDame);
    }

    public void reHp(int addNum) {
        protoStatus(StateType.EFFECT_BODY, (long) EffectBodyType.HEALING.value, 0L);
        protoStatus(StateType.RE_HP, (long) addNum);
        List<PointBuff> buffs = new ArrayList<>();
        buffs.add(new PointBuff(Point.CUR_HP, addNum));
        protoBuffPoint(buffs);
    }

    public void reHpNoEff(int addNum) {
        List<PointBuff> buffs = new ArrayList<>();
        buffs.add(new PointBuff(Point.CUR_HP, addNum));
        protoBuffPoint(buffs);
    }


    public void protoBuffPoint(int pointId, int addValue) { // bao gồm add point và trả vè - chỉ dùng cho các point add thưởng, point per phải làm khác
        List<PointBuff> buffs = new ArrayList<>();
        buffs.add(new PointBuff(pointId, addValue));
        protoBuffPoint(buffs);
    }

    public void protoMultiPoint(List<Long> points) {  //[pointId - curValue] :  chỉ trả về, thường dùng cho add change
        protoStatus(StateType.UPDATE_MULTI_POINT, points.size(), points);
    }

    public void protoUpdatePoint(Long pointId, Long value) {  //[pointId - curValue] :  chỉ trả về, thường dùng cho add change
        protoStatus(StateType.UPDATE_MULTI_POINT, 2, Arrays.asList(pointId, value));
    }

    // buff point and send
    public synchronized void protoBuffPoint(List<PointBuff> buffs) {
        if (!alive) return;
        List<Long> pointBuff = new ArrayList<>();
        for (int i = 0; i < buffs.size(); i++) {
            int pointId = buffs.get(i).getPointId();
            Long value = buffs.get(i).getValue();
            switch (pointId) {
                case Point.CUR_HP -> {
                    point.add(buffs.get(i), this);
                    pointBuff.add((long) Point.CUR_HP);
                    pointBuff.add((long) point.getCurHP());
                }
                default -> {
                    point.add(buffs.get(i), this);
                    pointBuff.add((long) pointId);
                    pointBuff.add((long) point.get(pointId));
                }
            }
        }
        protoStatus(StateType.UPDATE_MULTI_POINT, pointBuff.size(), pointBuff);
    }


    public void bonusKillMe(Unit killer) {

    }

    public synchronized boolean checkHasBonusKill() {
        return hasBonusKillMe;
    }

    public boolean hasActionMove() { // check random move
        return System.currentTimeMillis() - timeActiveRandomMove > BattleConfig.M_delayMove * 1000;
    }

    public boolean beBlock() {
        return isHit() || point.beBlock();
    }

    public boolean isHit() {
        return false;
    }

    public void setTimeRandomMove() { //  vừa randomm move xong, time này dùng để xác định thời gian cho phép random move tiếp theo
        timeActiveRandomMove = System.currentTimeMillis() + NumberUtil.getRandom((int) (BattleConfig.M_delayMove * 1000));
    }

    // region proto
    public void protoBeDame(Unit attacker, List<Long> aInfo) {
//        if (type == UnitType.BOSS) {
//            if (!beDameInfo.containsKey(attacker.getId())) beDameInfo.put(attacker.getId(), 0L);
//            beDameInfo.put(attacker.getId(), beDameInfo.get(attacker.getId()) + aInfo.get(2) + aInfo.get(3));
//        }
        protoStatus(List.of(StateType.BE_DAMAGE), aInfo);
    }

    public void protoStatus(StateType status) {
        if (room != null) room.getAProtoUnitState().add(protoState(List.of(status), new ArrayList<>()));
    }

    public void protoStatus(StateType status, Long... info) {
        if (room != null) room.getAProtoUnitState().add(protoState(List.of(status), Arrays.asList(info)));
    }


    public void protoStatus(StateType status, List<Long> info) {
        if (room != null) room.getAProtoUnitState().add(protoState(List.of(status), info));
    }


    public void protoStatus(List<StateType> aStatus, List<Long> aInfo) {
        if (room != null) room.getAProtoUnitState().add(protoState(aStatus, aInfo));
    }

    public void protoOneStatus(StateType status, Long... info) {
        if (room != null)
            room.getAProtoUnitState().add(protoState(List.of(status), List.of(status.length), Arrays.asList(info)));
    }

    public void protoStatus(StateType status, int size, List<Long> aInfo) {
        if (room != null) room.getAProtoUnitState().add(protoState(List.of(status), List.of(size), aInfo));
    }

    Pbmethod.PbUnitState protoState(List<StateType> aStatus, List<Long> aInfo) {
        Pbmethod.PbUnitState.Builder builder = Pbmethod.PbUnitState.newBuilder();
        builder.setId(id);
        for (int i = 0; i < aStatus.size(); i++) {
            builder.addStatus(aStatus.get(i).id);
            builder.addStatus(aStatus.get(i).length);
        }
        if (aInfo == null) aInfo = new ArrayList<>();
        builder.addAllPoint(aInfo);
        return builder.build();
    }

    Pbmethod.PbUnitState protoState(List<StateType> aStatus, List<Integer> size, List<Long> aInfo) {
        Pbmethod.PbUnitState.Builder builder = Pbmethod.PbUnitState.newBuilder();
        builder.setId(id);
        for (int i = 0; i < aStatus.size(); i++) {
            builder.addStatus(aStatus.get(i).id);
            builder.addStatus(size.get(i));
        }
        if (aInfo == null) aInfo = new ArrayList<>();
        builder.addAllPoint(aInfo);
        return builder.build();
    }
    // endregion
}
