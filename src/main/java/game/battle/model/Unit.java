package game.battle.model;

import game.battle.calculate.IMath;
import game.battle.calculate.MathLab;
import game.battle.object.*;
import game.battle.type.*;
import game.config.CfgStats;
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
    /** Thời điểm (ms) hết bảo vệ sau khi chết — 0 = không bảo vệ. */
    long timeProtectedEnd;
    Unit poisonOwner;
    long poisonEndMs;
    long poisonNextTickMs;
    int poisonDamagePerTick;
    int poisonMoveSlowApplied;
    long windDodgeBackup;
    long windAccuracyBackup;
    float windReduceRate;
    long windEndMs;
    Unit fireOwner;
    long fireEndMs;
    long fireNextTickMs;
    int fireDamagePerTick;
    /** Đếm giây (Update1s) tới lần hồi máu tiếp theo. */
    int healSecondCounter;


    public boolean isProtected() {
        return timeProtectedEnd > System.currentTimeMillis();
    }

    public void setTimeProtectedEnd(long timeProtectedEnd) {
        this.timeProtectedEnd = timeProtectedEnd;
    }


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


    public void attackUnit(Unit primaryTarget) {
        if (room == null) {
            attackSingleUnit(primaryTarget);
            return;
        }
        for (Unit target : room.collectAttackTargets(this, primaryTarget)) {
            attackSingleUnit(target);
        }
    }

    void attackSingleUnit(Unit target) {
        if (target == null) {
            return;
        }
        long[] damage = IMath.calculateDamage(this, target);
        long normalDame = damage[1];
        long critDame = damage[2];
        long totalDame = normalDame + critDame;
        boolean hitLanded = totalDame > 0;
        if (hitLanded) {
            target.beAttackDamage(this, totalDame);
            IMath.applyLifeSteal(this, totalDame);
            IMath.tryApplyPoison(this, target);
            IMath.tryApplyWind(this, target);
            IMath.tryApplyFire(this, target);
        }
        target.protoStatus(Pbmethod.SubStateType.BE_DAMAGE,
                Arrays.asList(id, damage[0], -normalDame, -critDame));
        if (hitLanded && target.isAlive()) {
            IMath.tryCounterAttack(target, this, normalDame);
        }
    }

    public boolean targetInSizeAttack(Unit target) {
        return target != null && pos.distance(target.pos) < rangeAttack;
    }


    // gửi riêng
    public void beAttackDamage(Unit ownerDamage, long atkDame) {
        updateHp(ownerDamage, -atkDame);
        if (!alive) {
            clearCombatEffects();
            timeDie = System.currentTimeMillis();
            protoDie(ownerDamage);
            if (checkHasBonusKill()) bonusKillMe(ownerDamage);
        } else {
            timeBeHit = System.currentTimeMillis();
            accelerateHealRegenOnHit();
        }
    }

    public Player getPlayer() {
        return (Player) this;
    }

    public Pet getPetUse() {
        return null;
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
        processPoison();
        processFire();
        processWind();
    }

    /** Tick 1s — hồi máu passive (không chạy mỗi frame). */
    public void Update1s() {
        processHealRegen();
    }

    /** Apply/refresh poison: damage % max HP mỗi giây, slow move speed, duration theo giáp target. */
    public void applyPoison(Unit owner, long poisonStat) {
        if (!alive || poisonStat <= 0) {
            return;
        }
        clearPoisonSlow();
        poisonOwner = owner;
        poisonDamagePerTick = Math.max(1, (int) (point.getMaxHp() * CfgStats.calcPoisonHpDamageRate(poisonStat)));
        long now = System.currentTimeMillis();
        poisonEndMs = now + CfgStats.calcPoisonDurationSeconds(point.getDefense()) * 1000L;
        poisonNextTickMs = now + 1000L;
        applyPoisonSlow(CfgStats.calcPoisonMoveSlowPercent(poisonStat));
        protoStatus(Pbmethod.SubStateType.EFFECT_BODY, (long) EffectBodyType.POISON.value, 0L);
    }

    /** Lửa: Lửa/2 máu mỗi giây (true damage), proc successRate, thời gian 10s + Giáp/100. */
    public void applyFire(Unit owner, long fireStat) {
        if (!alive || fireStat <= 0) {
            return;
        }
        int damagePerTick = CfgStats.calcFireDamagePerTick(fireStat);
        if (damagePerTick <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        long durationMs = CfgStats.calcFireDurationSeconds(point.getDefense()) * 1000L;
        long newEnd = now + durationMs;
        boolean wasActive = fireEndMs > now;
        if (wasActive) {
            newEnd = Math.max(newEnd, fireEndMs);
            fireDamagePerTick = Math.max(fireDamagePerTick, damagePerTick);
        } else {
            fireOwner = owner;
            fireDamagePerTick = damagePerTick;
            fireNextTickMs = now + 1000L;
        }
        fireEndMs = newEnd;
        long remainingMs = fireEndMs - now;
        protoStatus(Pbmethod.SubStateType.EFFECT_BODY, (long) EffectBodyType.DOT_FIRE.value, remainingMs);
    }

    /** Gió: giảm Né / Chính Xác theo successRate(Gió), mặc định 10s. */
    public void applyWind(long windStat) {
        if (!alive || windStat <= 0) {
            return;
        }
        float reduceRate = CfgStats.calcWindReduceRate(windStat);
        if (reduceRate <= 0f) {
            return;
        }
        long now = System.currentTimeMillis();
        long durationMs = CfgStats.calcWindDurationSeconds() * 1000L;
        long newEnd = now + durationMs;
        boolean wasActive = windEndMs > now;
        if (!wasActive) {
            windDodgeBackup = point.getDoge();
            windAccuracyBackup = point.getAccuracy();
            windReduceRate = reduceRate;
        } else {
            windReduceRate = Math.max(windReduceRate, reduceRate);
            newEnd = Math.max(newEnd, windEndMs);
        }
        windEndMs = newEnd;
        applyWindReducedStats();
        protoUpdatePoint((long) Point.DOGE, point.get(Point.DOGE));
        protoUpdatePoint((long) Point.ACCURACY, point.get(Point.ACCURACY));
    }

    private void processWind() {
        if (windEndMs <= 0) {
            return;
        }
        if (!alive || System.currentTimeMillis() >= windEndMs) {
            clearWind();
        }
    }

    private void applyWindReducedStats() {
        long newDodge = Math.max(0, (long) (windDodgeBackup * (1f - windReduceRate)));
        long newAccuracy = Math.max(0, (long) (windAccuracyBackup * (1f - windReduceRate)));
        point.getValues()[Point.DOGE] = newDodge;
        point.getValues()[Point.ACCURACY] = newAccuracy;
    }

    private void clearWind() {
        if (windEndMs <= 0) {
            return;
        }
        point.getValues()[Point.DOGE] = windDodgeBackup;
        point.getValues()[Point.ACCURACY] = windAccuracyBackup;
        windDodgeBackup = 0;
        windAccuracyBackup = 0;
        windReduceRate = 0f;
        windEndMs = 0;
        protoUpdatePoint((long) Point.DOGE, point.get(Point.DOGE));
        protoUpdatePoint((long) Point.ACCURACY, point.get(Point.ACCURACY));
    }

    private void processPoison() {
        if (poisonEndMs <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        if (!alive || now >= poisonEndMs) {
            clearPoison();
            return;
        }
        if (now < poisonNextTickMs) {
            return;
        }
        poisonNextTickMs += 1000L;
        Unit owner = poisonOwner != null ? poisonOwner : this;
        beAttackDamage(owner, poisonDamagePerTick);
        protoStatus(Pbmethod.SubStateType.BE_DAMAGE,
                Arrays.asList(owner.getId(), 0L, -(long) poisonDamagePerTick, 0L));
        if (!alive) {
            clearPoison();
        }
    }

    private void processFire() {
        if (fireEndMs <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        if (!alive || now >= fireEndMs) {
            clearFire();
            return;
        }
        if (now < fireNextTickMs) {
            return;
        }
        fireNextTickMs += 1000L;
        Unit owner = fireOwner != null ? fireOwner : this;
        applyFireTickDamage(owner, fireDamagePerTick);
        if (!alive) {
            clearFire();
        }
    }

    /** Tick Lửa: trừ thẳng máu, không phản đòn / hút máu. */
    private void applyFireTickDamage(Unit owner, int damage) {
        if (damage <= 0 || !alive) {
            return;
        }
        updateHp(owner, -damage);
        protoStatus(Pbmethod.SubStateType.BE_DAMAGE,
                Arrays.asList(owner.getId(), 0L, -(long) damage, 0L));
        if (!alive) {
            clearCombatEffects();
            timeDie = System.currentTimeMillis();
            protoDie(owner);
            if (checkHasBonusKill()) {
                bonusKillMe(owner);
            }
        }
    }

    private void clearFire() {
        fireOwner = null;
        fireEndMs = 0;
        fireNextTickMs = 0;
        fireDamagePerTick = 0;
    }

    /**
     * Hồi máu passive (point 15): mỗi 10s hồi % max HP (600 → 10%, scale tuyến tính).
     * Không hồi nếu chết, full máu, dưới healMinStat / HP hồi &lt; 1, hoặc bị stun/đóng băng.
     */
    private void processHealRegen() {
        if (!alive || point == null) {
            return;
        }
        long healing = point.getHealing();
        if (healing < CfgStats.getHealMinStat()) {
            return;
        }
        int healAmount = CfgStats.calcHealPerTick(healing, point.getMaxHp());
        if (healAmount < 1) {
            return;
        }
        healSecondCounter++;
        tryHealRegenTick(healAmount);
    }

    /** Mỗi lần bị đánh: rút ngắn 1s chu kỳ hồi máu (có thể kích hoạt hồi ngay). */
    private void accelerateHealRegenOnHit() {
        if (!alive || point == null) {
            return;
        }
        long healing = point.getHealing();
        if (healing < CfgStats.getHealMinStat()) {
            return;
        }
        int accel = CfgStats.calcHealAccelSecondsOnHit();
        if (accel <= 0) {
            return;
        }
        int healAmount = CfgStats.calcHealPerTick(healing, point.getMaxHp());
        if (healAmount < 1) {
            return;
        }
        healSecondCounter += accel;
        tryHealRegenTick(healAmount);
    }

    private void tryHealRegenTick(int healAmount) {
        int intervalSec = Math.max(1, CfgStats.calcHealIntervalSeconds());
        if (healSecondCounter < intervalSec) {
            return;
        }
        healSecondCounter = 0;
        if (beBlock()) {
            return;
        }
        if (point.getCurHP() >= point.getMaxHp()) {
            return;
        }
        reHpFixed(healAmount);
    }

    private void applyPoisonSlow(int slowPercent) {
        if (slowPercent <= 0) {
            poisonMoveSlowApplied = 0;
            return;
        }
        long currentChange = point.get(Point.CHANGE_MOVE_SPEED);
        long minChange = Math.max(0, 100 - Math.round(CfgStats.getPoisonSlowMax() * 100f));
        int actualSlow = (int) Math.min(slowPercent, Math.max(0, currentChange - minChange));
        if (actualSlow <= 0) {
            poisonMoveSlowApplied = 0;
            return;
        }
        poisonMoveSlowApplied = actualSlow;
        point.add(Point.CHANGE_MOVE_SPEED, -actualSlow);
        protoUpdatePoint((long) Point.CHANGE_MOVE_SPEED, point.get(Point.CHANGE_MOVE_SPEED));
    }

    private void clearPoison() {
        clearPoisonSlow();
        poisonOwner = null;
        poisonEndMs = 0;
        poisonNextTickMs = 0;
        poisonDamagePerTick = 0;
    }

    private void clearCombatEffects() {
        clearPoison();
        clearFire();
        clearWind();
    }

    private void clearPoisonSlow() {
        if (poisonMoveSlowApplied <= 0) {
            return;
        }
        point.add(Point.CHANGE_MOVE_SPEED, poisonMoveSlowApplied);
        poisonMoveSlowApplied = 0;
        protoUpdatePoint((long) Point.CHANGE_MOVE_SPEED, point.get(Point.CHANGE_MOVE_SPEED));
    }

    public boolean sameTeam(Unit other) {
        return other.getClanId() == this.clanId;
    }

    public void stun(float time) {
        long timStun = (long) (time * 1000);
        point.addStun(timStun);
        protoStatus(Pbmethod.SubStateType.UPDATE_MULTI_POINT, (long) Point.STUN, timStun);
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

    /** Huong nhin ve target; gui UPDATE_DIRECTION neu can lat mat. */
    public void faceToward(Unit target) {
        if (target == null) return;
        Pos faceDir = pos.getDirectionTo(target.getPos());
        if (faceDir.equals(Pos.zero()) || Math.abs(faceDir.x) < 0.01f) return;
        if (Math.abs(direction.x) < 0.01f || !isLikeFace(faceDir)) {
            setDirection(faceDir);
            protoStatus(Pbmethod.SubStateType.UPDATE_DIRECTION, (long) (faceDir.x * 1000), (long) (faceDir.y * 1000));
        }
    }


    public Point resetData() {
        point.resetHp();
        return resetCombatState();
    }

    /** Reset trạng thái combat khi đổi map, giữ nguyên HP đã cache lúc logout. */
    public Point resetDataKeepHp() {
        long curHp = Math.min(point.getCurHP(), point.getMaxHp());
        resetCombatState();
        point.setCurHp(curHp);
        return point;
    }

    private Point resetCombatState() {
        clearCombatEffects();
        healSecondCounter = 0;
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
        protoStatus(Pbmethod.SubStateType.EFFECT_BODY, (long) EffectBodyType.HEALING.value, 0L);
        protoStatus(Pbmethod.SubStateType.RE_HP, (long) addNum);
        List<PointBuff> buffs = new ArrayList<>();
        buffs.add(new PointBuff(Point.CUR_HP, addNum));
        protoBuffPoint(buffs);
    }

    /** Hồi máu cố định — không áp dụng CHANGE_HEATH (zone hồi máu home). */
    public void reHpFixed(int addNum) {
        if (!alive || addNum <= 0) return;
        long cur = point.getCurHP();
        long max = point.getMaxHp();
        if (cur >= max) return;
        int actual = (int) Math.min(addNum, max - cur);
        point.setCurHp(cur + actual);
        protoStatus(Pbmethod.SubStateType.EFFECT_BODY, (long) EffectBodyType.HEALING.value, 0L);
        protoStatus(Pbmethod.SubStateType.RE_HP, (long) actual);
        protoUpdatePoint((long) Point.CUR_HP, point.getCurHP());
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
        protoStatus(Pbmethod.SubStateType.UPDATE_MULTI_POINT, points);
    }

    public void protoUpdatePoint(Long pointId, Long value) {  //[pointId - curValue] :  chỉ trả về, thường dùng cho add change
        protoStatus(Pbmethod.SubStateType.UPDATE_MULTI_POINT, Arrays.asList(pointId, value));
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
                    pointBuff.add(point.getCurHP());
                }
                default -> {
                    point.add(buffs.get(i), this);
                    pointBuff.add((long) pointId);
                    pointBuff.add(point.get(pointId));
                }
            }
        }
        protoStatus(Pbmethod.SubStateType.UPDATE_MULTI_POINT, pointBuff);
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

    public List<Integer> getListInfo(int effInit) {
        List<Integer> lst = new ArrayList<>(); // dameSkin,idChat,trial, effectInit,
        lst.add(idDameSkin);
        lst.add(idChatFrame);
        lst.add(idTrial);
        lst.add(effInit);
        return lst;
    }

    // region proto
    public void protoStatus(Pbmethod.SubStateType status) {
        if (room != null) room.addProtoUnitState(protoState(status, new ArrayList<>()));
    }

    public void protoStatus(Pbmethod.SubStateType status, Long... info) {
        if (room != null) room.addProtoUnitState(protoState(status, Arrays.asList(info)));
    }


    public void protoStatus(Pbmethod.SubStateType status, List<Long> info) {
        if (room != null) room.addProtoUnitState(protoState(status, info));
    }

    Pbmethod.PbUnitState protoState(Pbmethod.SubStateType stateType, List<Long> aInfo) {
        Pbmethod.PbUnitState.Builder builder = Pbmethod.PbUnitState.newBuilder();
        builder.setId(id);
        builder.addStatus(stateType.getNumber());
        builder.addStatus(aInfo.size());
        builder.addAllPoint(aInfo);
        return builder.build();
    }

//    Pbmethod.PbUnitState protoState(List<Pbmethod.SubStateType> aStatus, List<Integer> size, List<Long> aInfo) {
//        Pbmethod.PbUnitState.Builder builder = Pbmethod.PbUnitState.newBuilder();
//        builder.setId(id);
//        for (int i = 0; i < aStatus.size(); i++) {
//            builder.addStatus(aStatus.get(i).getNumber());
//            builder.addStatus(size.get(i));
//        }
//        if (aInfo == null) aInfo = new ArrayList<>();
//        builder.addAllPoint(aInfo);
//        return builder.build();
//    }
    // endregion
}
