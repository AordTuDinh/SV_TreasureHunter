package game.battle.model;

import game.battle.calculate.IMath;
import game.battle.calculate.MathLab;
import game.battle.object.*;
import game.battle.type.*;
import game.config.aEnum.FactionType;
import game.config.aEnum.MapType;
import game.treasure.BattleConfig;
import game.treasure.mapping.main.ResMapEntity;
import game.treasure.server.Constans;
import game.treasure.service.resource.ResMap;
import game.treasure.table.BaseBattleRoom;
import game.treasure.table.BaseRoom;
import game.object.PointBuff;
import lombok.Data;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.NumberUtil;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import java.util.*;

@Data
public abstract class Unit {
    // new
    int chunkId;
    long id; // tất cả các sinh vật đều sẽ có id này riêng lẻ, vào map sẽ gen id này
    // info
    // int id; // id in room
    int teamId;

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
    Unit targetAttack;
    public FactionType faction = FactionType.NULL;
    long timeBeHit = 0;
    long killById;

    public Map<Long, Long> beDameInfo = new HashMap<>();

    // todo test move
    boolean isMove = false; // dang di chuyen
    long timeActionMove;  // thời gian thay đổi action move
    boolean isBeAttack; // bi danh
    Pos targetMove;
    boolean ready = true;
    long timeJoinRoom;
    long timeActiveRandomMove;
    long timeActionAttack;
    long timeCheckDirectionAttack;
    long[] timeActiveSlot = new long[]{0, 0, 0};
    //long timeBeAttack;
    Pos directionMoveAttack = Pos.zero(); // chưa có hướng move attack melee
    // save attacker info
    Map<Long, List<Long>> attackerInfo = new HashMap<>(); // playerID   - timeAttack,shurikenIds : dùng để check thẳng đánh mình vừa đánh lúc nào + suriken id nào
    private static final int TIME_ATTACK = 0;
    private static final int SLOT_MELEE = 1;
    private static final int START_INDEX = 2;

    public Map<Long, Unit> targetSelf = new HashMap<>();
    boolean sendDie = true;


    public boolean isPlayer() {
        return type == UnitType.PLAYER;
    }


    public boolean isEnemy() {
        return type == UnitType.ENEMY;
    }

    private void checkBeAttackByEffect(Unit attacker) {
        if (room.getRoomState() != RoomState.ACTIVE) return;
        if (point.getCurHP() > 0 && (targetAttack == null || !targetAttack.alive)) {
            isBeAttack = true;
            targetAttack = attacker;
            addTargetSelf(attacker);
        }
    }

    public void unTarget() {
        targetAttack = null;
        isBeAttack = false;
    }

    public void unTargetAll() {
        targetAttack = null;
        isBeAttack = false;
        targetSelf.clear();
    }

    public void addTargetSelf(Unit attacker) {
        if (!targetSelf.containsKey(attacker.getId())) {
            this.targetSelf.put(attacker.getId(), attacker);
        }
    }

    public void removeTargetSelf(Unit attacker) {
        for (int i = 0; i < targetSelf.size(); i++) {
            if (targetSelf.get(i) != null && attacker.getId() == targetSelf.get(i).getId()) {
                targetSelf.remove(i);
                return;
            }
        }
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

    public void beAttackMelee(Unit attacker) {
        int[] damage = IMath.calculateDamage(attacker, this, attacker.getFaction());
        beAttackDamage(attacker, damage[1], damage[2]);
        addAtkInfoMelee(attacker);
        protoBeDame(attacker, Arrays.asList(damage[0], -damage[1], -damage[2]));
    }

//    public void beAttackCollider(Unit attacker) {
//        addAtkInfoMelee(attacker);
//        int[] damage = IMath.calculateDamage(attacker, this, attacker.getFaction());
//        damage[1] = (int) (damage[1] * BattleConfig.M_PerDameCollider);
//        damage[2] = (int) (damage[2] * BattleConfig.M_PerDameCollider);
//        if (damage[1] <= 0 && damage[2] <= 0) damage[1] = 1;
//        beAttackDamage(attacker, damage[1], damage[2]);
//        protoBeDame(attacker, Arrays.asList(damage[0], -damage[1], -damage[2]));
//    }


    public long getBeDameInfo(int userId) {
        if (beDameInfo.containsKey(userId)) {
            return beDameInfo.get(userId);
        }
        return 0;
    }

    public Pos getFutureDirection(int min, int max) {
        if (targetAttack == null) return Pos.zero();
        if (targetAttack.isMove()) {
            int rand = NumberUtil.getRandom(min, max);
            float distance = (float) pos.distance(targetAttack.getPos()) / rand;
            Pos posNext = targetAttack.getPos().clone();
            Pos dirClone = targetAttack.direction.clone();
            dirClone.multiple(targetAttack.getCurSpeed() * distance);
            posNext.add(dirClone);
            return pos.getDirectionTo(posNext);
        } else return pos.getDirectionTo(targetAttack.pos);
    }

    // gửi riêng
    public void beAttackDamage(Unit ownerDamage, int atkDame, int mAtkDame) {
        updateHp(ownerDamage, -atkDame, -mAtkDame);
        if (!alive) {
            timeDie = System.currentTimeMillis();
            ownerDamage.unTarget();
            ownerDamage.removeTargetSelf(this);
            protoDie(ownerDamage);
            if (checkHasBonusKill()) bonusKillMe(ownerDamage);
        } else {
            timeBeHit = System.currentTimeMillis();
            isBeAttack = true;
            targetAttack = ownerDamage;
            addTargetSelf(ownerDamage);
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


    void addAtkInfoMelee(Unit enemy) {
        if (attackerInfo.containsKey(enemy.getId())) {
            attackerInfo.get(enemy.getId()).set(TIME_ATTACK, System.currentTimeMillis());
            attackerInfo.get(enemy.getId()).set(SLOT_MELEE, attackerInfo.get(enemy.getId()).get(SLOT_MELEE) + 1);
        } else {
            attackerInfo.put(enemy.getId(), Arrays.asList(System.currentTimeMillis(), 0L));
        }
    }

    // tránh trường hợp ăn đòn liên hoàn, sau 1 khoảng time mới ăn đòn từ thằng đó tiếp
    public boolean hasReciveMelee(Unit attacker) {
        return canBeAttack(attacker.teamId) && DateTime.isAfterTime(getTimeAttack(attacker.getId()), BattleConfig.C_haSReciveDamage);
    }

    public boolean hasReceiveEffMelee(Unit attacker) {
        return canBeMelee() && hasReciveMelee(attacker);
    }

    public boolean isReviveReady() {
        return DateTime.isAfterTime(timeRevive, BattleConfig.E_ReviveReady);
    }

    public boolean canBeAttack(int teamId) {
        return isAlive() && isReady() && isReviveReady() && !sameTeam(teamId);
    }

    public boolean canBeMelee() {
        return isAlive() && isReady() && isReviveReady();
    }

    public boolean sameTeam(int teamId) {
        return this.teamId == teamId;
    }



    public void Update() {
    }

    public boolean sameTeam(Unit other) {
        return other.getTeamId() == this.teamId;
    }

    public long getTimeAttack(long attackerId) {
        if (attackerInfo.containsKey(attackerId)) { // chưa có thông tin gì
            return attackerInfo.get(attackerId).get(TIME_ATTACK);
        } else {
            return 0;
        }
    }


    public void stun(float time) {
        int timStun = (int) (time * 1000);
        point.addStun(timStun);
        protoStatus(StateType.UPDATE_MULTI_POINT, 2, List.of(Point.STUN, timStun));
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

//    public boolean isHitMelee(Unit target) {
//        if (!isAlive()) return false;
//        return MathLab.pointInCircle(this.pos, target.getRadius() + radius, target.pos);
//    }

    public float distionTop() {
        return Math.abs(room.getMapInfo().getTopRightP().y - pos.y);
    }

    public float distionBot() {
        return Math.abs(room.getMapInfo().getBotLeftP().y - pos.y);
    }

    public float distionLeft() {
        return Math.abs(room.getMapInfo().getBotLeftP().x - pos.x);
    }

    public float distionRight() {
        return Math.abs(room.getMapInfo().getTopRightP().x - pos.x);
    }

    public boolean isLikeFace(Pos newDirection) { // check lật mặt
        return direction.x * newDirection.x > 0;
    }


    public Point resetData() {
        point.resetHpMp();
        alive = true;
        attackerInfo = new HashMap<>();
        hasBonusKillMe = true;
        unTargetAll();
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
        pbUser.setSpeed(point.getMoveSpeed());
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

    public void setTimeUseItem(int slot) {
        timeActiveSlot[slot] = System.currentTimeMillis();
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
        unTargetAll();
        this.killById = killer.getId();
        room.characterDie(this);
    }

    public void updateHp(Unit attacker, int atkDame, int magDame) {
        List<PointBuff> buffs = new ArrayList<>();
        buffs.add(new PointBuff(Point.CUR_HP, atkDame + magDame));
        protoBuffPoint(buffs);
        ((BaseBattleRoom) room).ChangeCharacterHp(attacker, this, atkDame, magDame);
    }

    public void reHp(int addNum) {
        protoStatus(StateType.EFFECT_BODY, EffectBodyType.HEALING.value, 0);
        protoStatus(StateType.RE_HP, addNum);
        List<PointBuff> buffs = new ArrayList<>();
        buffs.add(new PointBuff(Point.CUR_HP, addNum));
        protoBuffPoint(buffs);
    }

    public void buffShell(int addNum) {
        List<PointBuff> buffs = new ArrayList<>();
        buffs.add(new PointBuff(Point.SHELL, addNum));
        protoBuffPoint(buffs);
    }

    public void reHpNoEff(int addNum) {
        List<PointBuff> buffs = new ArrayList<>();
        buffs.add(new PointBuff(Point.CUR_HP, addNum));
        protoBuffPoint(buffs);
    }

    public void reHpBasic(int addNum) {
        protoStatus(StateType.EFFECT_BODY, EffectBodyType.HEALING_BASIC.value, 0);
        List<PointBuff> buffs = new ArrayList<>();
        buffs.add(new PointBuff(Point.CUR_HP, addNum));
        protoBuffPoint(buffs);
    }

    public void updateMp(int addMp) {
        List<PointBuff> buffs = new ArrayList<>();
        buffs.add(new PointBuff(Point.CUR_MP, addMp));
        protoBuffPoint(buffs);
    }

    public void protoBuffPoint(int pointId, int addValue) { // bao gồm add point và trả vè - chỉ dùng cho các point add thưởng, point per phải làm khác
        List<PointBuff> buffs = new ArrayList<>();
        buffs.add(new PointBuff(pointId, addValue));
        protoBuffPoint(buffs);
    }

    public void protoMultiPoint(List<Integer> points) {  //[pointId - curValue] :  chỉ trả về, thường dùng cho add change
        protoStatus(StateType.UPDATE_MULTI_POINT, points.size(), points);
    }

    public void protoUpdatePoint(int pointId, int value) {  //[pointId - curValue] :  chỉ trả về, thường dùng cho add change
        protoStatus(StateType.UPDATE_MULTI_POINT, 2, Arrays.asList(pointId, value));
    }

    // buff point and send
    public synchronized void protoBuffPoint(List<PointBuff> buffs) {
        if (!alive) return;
        List<Integer> pointBuff = new ArrayList<>();
        for (int i = 0; i < buffs.size(); i++) {
            int pointId = buffs.get(i).getPointId();
            int value = buffs.get(i).getValue();
            switch (pointId) {
                case Point.BUFF_CUR_PER_HP -> {
                    int reMax = (int) (value / 100f * point.getMaxHp());
                    point.addCurHp(reMax);
                    pointBuff.add(Point.CUR_HP);
                    pointBuff.add(point.getCurHP());
                }
                case Point.BUFF_CUR_PER_MP -> {
                    int reMax = (int) (value / 100f * point.getMaxMp());
                    point.addCurMp(reMax);
                    pointBuff.add(Point.CUR_MP);
                    pointBuff.add(point.getCurMP());
                }
                case Point.CUR_HP -> {
                    int shell = point.getCurShell();
                    if (shell > 0 && value < 0) {
                        shell += value;
                        point.setShell(shell > 0 ? shell : 0);
                        // trừ giáp trước, nếu hết giáp mới trừ máu
                        pointBuff.add(Point.SHELL);
                        pointBuff.add(point.getCurShell());
                        // dame > shell
                        if (value + shell < 0) {
                            buffs.get(i).setValue(value + shell);
                            point.add(buffs.get(i), this);
                            pointBuff.add(Point.CUR_HP);
                            pointBuff.add(point.getCurHP());
                        }
                    } else {
                        point.add(buffs.get(i), this);
                        pointBuff.add(Point.CUR_HP);
                        pointBuff.add(point.getCurHP());
                    }
                }
                default -> {
                    point.add(buffs.get(i), this);
                    pointBuff.add(pointId);
                    pointBuff.add(point.get(pointId));
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
        return isHit() || attackBlockMove() || point.beBlock();
    }

//    public abstract void activeSkill(int skillId);

    public boolean attackBlockMove() {
        return !DateTime.isAfterTime(timeActionAttack, BattleConfig.P_attackBlockMove);
    }

    public boolean isHit() {
        return false;
    }

    public void setTimeRandomMove() { //  vừa randomm move xong, time này dùng để xác định thời gian cho phép random move tiếp theo
        timeActiveRandomMove = System.currentTimeMillis() + NumberUtil.getRandom((int) (BattleConfig.M_delayMove * 1000));
    }

    // region proto
    public void protoBeDame(Unit attacker, List<Integer> aInfo) {
//        if (type == UnitType.BOSS) {
//            if (!beDameInfo.containsKey(attacker.getId())) beDameInfo.put(attacker.getId(), 0L);
//            beDameInfo.put(attacker.getId(), beDameInfo.get(attacker.getId()) + aInfo.get(2) + aInfo.get(3));
//        }
        protoStatus(List.of(StateType.BE_DAMAGE), aInfo);
    }

    public void protoBeDameEffect(List<Integer> aInfo) { // size 3
        protoStatus(List.of(StateType.EFFECT_DAME), aInfo);
    }

    public void protoStatus(StateType status) {
        if (room != null) room.getAProtoUnitState().add(protoState(List.of(status), new ArrayList<>()));
    }

    public void protoStatus(StateType status, Integer... info) {
        if (room != null) room.getAProtoUnitState().add(protoState(List.of(status), Arrays.asList(info)));
    }

    public void protoStatus(StateType status, Integer id, Integer size, List<Integer> info) {
        if (room != null) room.getAProtoUnitState().add(protoState(List.of(status), info));
    }

    public void protoStatus(StateType status, List<Integer> info) {
        if (room != null) room.getAProtoUnitState().add(protoState(List.of(status), info));
    }


    public void protoRangeDame(Unit attacker, List<Integer> aInfo) {
//        if (type == UnitType.BOSS) {
//            if (!beDameInfo.containsKey(attacker.getId())) beDameInfo.put(attacker.getId(), 0L);
//            beDameInfo.put(attacker.getId(), beDameInfo.get(attacker.getId()) + aInfo.get(2) + aInfo.get(3));
//        }
        protoStatus(List.of(StateType.RANGE_DAMAGE), aInfo);
    }


    public void protoStatus(List<StateType> aStatus, List<Integer> aInfo) {
        if (room != null) room.getAProtoUnitState().add(protoState(aStatus, aInfo));
    }

    public void protoOneStatus(StateType status, Integer... info) {
        if (room != null)
            room.getAProtoUnitState().add(protoState(List.of(status), List.of(status.length), Arrays.asList(info)));
    }

    public void protoStatus(StateType status, int size, List<Integer> aInfo) {
        if (room != null) room.getAProtoUnitState().add(protoState(List.of(status), List.of(size), aInfo));
    }

    Pbmethod.PbUnitState protoState(List<StateType> aStatus, List<Integer> aInfo) {
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

    Pbmethod.PbUnitState protoState(List<StateType> aStatus, List<Integer> size, List<Integer> aInfo) {
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
