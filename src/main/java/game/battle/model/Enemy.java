package game.battle.model;

import game.battle.object.BonusKillEnemy;
import game.battle.object.Coroutine;
import game.battle.object.Point;
import game.battle.object.Pos;
import game.battle.type.AnimationType;
import game.battle.type.RoomState;
import game.battle.type.UnitType;
import game.config.CfgMob;
import game.treasure.BattleConfig;
import game.treasure.mapping.main.ResMobEntity;
import game.treasure.service.resource.ResMob;
import game.treasure.service.user.Bonus;
import game.object.BonusConfig;
import lombok.Data;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.NumberUtil;
import protocol.Pbmethod;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class Enemy extends Unit implements Serializable {
    public List<BonusConfig> listBonus;
    public float delayAnimAttack;
    public int forcePush;
    long damage;
    int skillNormal = 0;
    int spawnTier = 1;
    float tierMult = 1f;
    Unit targetAttack;
    long timeLastChaseRefresh;
    Pos lastMoveCheckPos;
    int stuckMoveTicks;

    static final int STUCK_MOVE_TICKS = 32; // ~0.5s @ 16ms/tick

    public Enemy(int enemyKey, Unit player, Pos pos) {
        this(enemyKey, player, pos, 1);
    }

    public Enemy(int enemyKey, Unit player, Pos pos, int tier) {
        setRoom(player.getRoom());
        setPanelMap(player.getPanelMap());
        this.model = enemyKey;
        this.clanId = -1;
        this.pos = pos;
        this.instancePos = pos.clone();
        this.spawnTier = tier > 0 ? Math.min(tier, 4) : 1;
        this.tierMult = CfgMob.getTierMult(spawnTier);
        ResMobEntity mob = ResMob.getMob(enemyKey);
        this.point = mob.getPoint();
        if (tierMult != 1f) {
            point.setBaseHp(CfgMob.scaleStat((int) point.get(Point.HP), spawnTier));
            point.setBaseAttack(CfgMob.scaleStat((int) point.get(Point.ATTACK), spawnTier));
            point.setBaseDef(CfgMob.scaleStat((int) point.get(Point.DEFENSE), spawnTier));
            point.calculatorPower();
        }
        this.name = mob.getName();
        float mobRange = mob.getRangeAttack();
        this.rangeAttack = mobRange > 0 ? mobRange : BattleConfig.M_rangeAttack;
        setListBonus(mob.getBonus());
        setType(UnitType.ENEMY);
        this.delayAnimAttack = BattleConfig.M_delayAttackDamage;
        resetData();
    }

    @Override
    public Point resetData() {
        targetAttack = null;
        clearMoveStuck();
        return super.resetData();
    }

    @Override
    public void revive() {
        if (canRevive() && !isAlive()) {
            timeRevive = System.currentTimeMillis();
            resetData();
            pos = instancePos.clone();
            protoStatus(Pbmethod.SubStateType.REVIVE, (long) (pos.x * 1000), (long) (pos.y * 1000));
        }
    }

    public boolean canRevive() {
        return DateTime.isAfterTime(timeDie, BattleConfig.M_timeRevive);
    }

    @Override
    public void protoDie(Unit killer) {
        super.protoDie(killer);
        protoStatus(Pbmethod.SubStateType.DIE, 1L);
    }

    @Override
    public synchronized void beAttackDamage(Unit ownerDamage, long atkDame) {
        if (ownerDamage != null && ownerDamage.isPlayer()) {
            if (targetAttack == null) {
                targetAttack = ownerDamage;
            }
            targetMove = null;
        }
        super.beAttackDamage(ownerDamage, atkDame);
    }

    @Override
    public synchronized void bonusKillMe(Unit killer) {
        if (killer.isPlayer()) {
            hasBonusKillMe = false;
        }
    }

    BonusKillEnemy getBonusWithPer(List<BonusConfig> aBonusConfig, List<Long> perBuff) {
        BonusKillEnemy result = new BonusKillEnemy();
        for (int i = 0; i < aBonusConfig.size(); i++) {
            BonusConfig bm = aBonusConfig.get(i);
            if (bm.getBonus().get(0).intValue() == Bonus.BONUS_GOLD) {
                int num = bm.getMax() == 1 ? 1 : NumberUtil.getRandom(bm.getMin(), bm.getMax());
                num += num * perBuff.get(1) / 100f;
                if (tierMult != 1f)
                    num = (int) CfgMob.scaleBonusAmount(num, spawnTier);
                result.setGold(num);
            } else {
                int rand = NumberUtil.getRandom(1000);
                if (rand < bm.getRate() + perBuff.get(0) / 10) {
                    int roll = NumberUtil.getRandom(bm.getMin(), bm.getMax());
                    if (tierMult != 1f)
                        roll = (int) CfgMob.scaleBonusAmount(roll, spawnTier);
                    result.getBonus().addAll(Bonus.viewXNumber(new ArrayList<>(bm.getBonus()), roll));
                }
            }
        }
        return result;
    }

    public void genRandomMove() {
        if (!hasActionMove() || !isReady()) return;
        if (targetMove != null) return;
        int rand = NumberUtil.getRandom(100);
        if (rand < BattleConfig.M_idleMoveChance) {
            setMove(true);
            targetMove = Pos.v_add(panelMap, instancePos, randomMove());
            clearMoveStuck();
        }
        setTimeRandomMove();
    }

    public boolean moveToTargetDone() {
        if (targetMove == null) return true;
        return pos.likeEquals(targetMove);
    }

    private Pos randomMove() {
        float dist = NumberUtil.getRandom(BattleConfig.M_rangeMove * 0.5f, BattleConfig.M_rangeMove);
        double rad = Math.toRadians(NumberUtil.getRandom(360));
        return new Pos((float) (Math.cos(rad) * dist), (float) (Math.sin(rad) * dist));
    }

    public boolean inRankAttackMelee() {
        if (targetAttack == null || !targetAttack.isAlive()) return false;
        return pos.distance(targetAttack.pos) < rangeAttack
                && Math.abs(pos.y - targetAttack.getPos().y) < BattleConfig.E_RangeYAttack;
    }

    public boolean hasAttackMelee() {
        return inRankAttackMelee() && hasActionAttack() && alive && targetAttack.isAlive();
    }

    @Override
    public boolean beBlock() {
        return super.beBlock() || isAttackBlockingMove();
    }

    /** Đang trong animation attack — không cho move (khớp {@link BattleConfig#E_timeDelayAttackToMove}). */
    private boolean isAttackBlockingMove() {
        return !DateTime.isAfterTime(timeActionAttack, BattleConfig.E_timeDelayAttackToMove);
    }

    private boolean hasActionAttack() {
        return DateTime.isAfterTime(timeActionAttack, BattleConfig.M_attackSpeed) && !isMove()
                && DateTime.isAfterTime(timeActionMove, 0.3f);
    }

    /** Hướng nhìn khi đứng yên / đánh — hysteresis theo vị trí world X của target. */
    private void setDirectionChase(Pos lookAt) {
        if (lookAt == null) return;
        applyFaceTowardWorldX(lookAt.x);
    }

    /** Hướng nhìn khi đang di chuyển — theo dấu vector move (trục X). */
    private void setDirectionFromMove(Pos moveDir) {
        if (moveDir == null || moveDir.equals(Pos.zero())) return;
        applyFaceFromMoveX(moveDir.x);
    }

    private void applyFaceFromMoveX(float moveX) {
        if (Math.abs(moveX) < BattleConfig.M_directionMinDx) return;
        applyFaceSign(moveX > 0 ? 1f : -1f);
    }

    /** Chỉ lật khi target lệch đủ xa (tránh player lượn sát quái). */
    private void applyFaceTowardWorldX(float worldX) {
        float dx = worldX - pos.x;
        if (Math.abs(dx) < BattleConfig.M_directionMinDx) return;
        float wantSign = dx > 0 ? 1f : -1f;
        float curSign = direction.x >= 0 ? 1f : -1f;
        if (Math.abs(direction.x) >= 0.01f && wantSign != curSign
                && Math.abs(dx) < BattleConfig.M_faceFlipHysteresisX) {
            return;
        }
        applyFaceSign(wantSign);
    }

    private void applyFaceSign(float sign) {
        Pos faceDir = new Pos(sign, 0);
        if (Math.abs(direction.x) < 0.01f || !isLikeFace(faceDir)) {
            setDirection(faceDir);
            protoStatus(Pbmethod.SubStateType.UPDATE_DIRECTION, (long) (faceDir.x * 1000), 0L);
        }
    }

    private void clearMoveStuck() {
        lastMoveCheckPos = null;
        stuckMoveTicks = 0;
    }

    /** Không tiến thêm được → đổi target về vị trí spawn. */
    private void redirectToSpawnIfStuck() {
        if (targetMove == null) return;
        Pos cur = pos.round();
        if (lastMoveCheckPos == null || !lastMoveCheckPos.likeEquals(cur)) {
            lastMoveCheckPos = cur.clone();
            stuckMoveTicks = 0;
            return;
        }
        stuckMoveTicks++;
        if (stuckMoveTicks < STUCK_MOVE_TICKS) return;
        clearMoveStuck();
        targetMove = instancePos.clone();
        setMove(true);
    }

    private void snapToTargetMove() {
        pos.x = targetMove.x;
        pos.y = targetMove.y;
        updateChunkByPos(pos);
    }

    private void finishIdleArrival() {
        clearMoveStuck();
        targetMove = null;
        setMove(false);
    }

    private boolean isBeyondLeash() {
        return instancePos != null && pos.distance(instancePos) > BattleConfig.M_maxLeashFromSpawn;
    }

    @Override
    public void Update() {
        super.Update();
        if (room.getRoomState() != RoomState.ACTIVE) return;
        enemyProcess();
    }

    private void enemyProcess() {
        if (!isAlive() || beBlock()) return;

        if (targetAttack != null && !targetAttack.isAlive()) {
            targetAttack = null;
            moveToInstancePos();
            return;
        }

        if (isBeyondLeash()) {
            targetAttack = null;
            moveToInstancePos();
            return;
        }

        if (targetAttack != null) {
            chaseAndAttack();
            return;
        }

        if (targetMove != null) {
            if (!moveToTargetDone()) {
                enemyMove();
            } else {
                finishIdleArrival();
            }
            return;
        }

        idleWander();
    }

    private void moveToInstancePos() {
        targetMove = instancePos.clone();
        clearMoveStuck();
        if (!moveToTargetDone()) {
            enemyMove();
        } else {
            finishIdleArrival();
        }
    }

    private void chaseAndAttack() {
        if (inRankAttackMelee() && hasActionAttack()) {
            setMove(false);
            setDirectionChase(targetAttack.getPos());
            setTimeAttack();
            protoStatus(Pbmethod.SubStateType.PLAY_ANIM, (long) AnimationType.ATTACK.value);
            scheduleAttackDamage(targetAttack);
        } else if (!inRankAttackMelee()) {
            if (targetMove == null || DateTime.isAfterTime(timeLastChaseRefresh, BattleConfig.M_chaseMoveRefresh)) {
                targetMove = getChasePos(targetAttack);
                timeLastChaseRefresh = System.currentTimeMillis();
            }
            setMove(true);
            enemyMove();
        } else {
            setMove(false);
            if (DateTime.isAfterTime(timeCheckDirectionAttack, BattleConfig.E_timeCheckDirection)) {
                setDirectionChase(targetAttack.getPos());
                timeCheckDirectionAttack = System.currentTimeMillis();
            }
        }
    }

    private void idleWander() {
        genRandomMove();
        if (!moveToTargetDone()) {
            enemyMove();
        } else {
            finishIdleArrival();
        }
    }

    public void enemyMove() {
        if (targetMove == null || beBlock()) return;
        redirectToSpawnIfStuck();

        if (moveToTargetDone()) {
            if (targetAttack == null) finishIdleArrival();
            return;
        }

        Pos moveDir = pos.getDirectionTo(targetMove);
        if (moveDir.equals(Pos.zero())) {
            snapToTargetMove();
            if (targetAttack == null) finishIdleArrival();
            return;
        }

        float stepLen = getCurSpeed() * BattleConfig.hSpeed;
        float remain = (float) pos.distance(targetMove);
        if (remain <= stepLen) {
            setDirectionFromMove(moveDir);
            snapToTargetMove();
            if (targetAttack == null) finishIdleArrival();
            return;
        }

        setDirectionFromMove(moveDir);
        move(Pos.moveFromDirection(moveDir, getCurSpeed()));
        if (moveToTargetDone() && targetAttack == null) {
            finishIdleArrival();
        }
    }

    private void scheduleAttackDamage(Unit target) {
        if (target == null || room == null) return;
        float delay = delayAnimAttack > 0 ? delayAnimAttack : BattleConfig.M_delayAttackDamage;
        getBattleRoom().addCoroutine(new Coroutine(delay, () -> applyAttackDamage(target)));
    }

    private void applyAttackDamage(Unit target) {
        if (!isAlive() || target == null || !target.isAlive()) return;
        if (targetAttack != target) return;
        if (!inRankAttackMelee()) return;
        attackUnit(target);
    }

    private Pos getChasePos(Unit target) {
        Pos targetPos = Pos.capPos(target.getPos(), panelMap.getBotLeftP(), panelMap.getTopRightP(), BattleConfig.P_Width / 2);
        float dx = targetPos.x - pos.x;
        float stopShort = Math.max(0.4f, rangeAttack * 0.55f);
        if (Math.abs(dx) <= stopShort) {
            return new Pos(pos.x, targetPos.y);
        }
        float sign = dx > 0 ? 1f : -1f;
        return new Pos(targetPos.x - sign * stopShort, targetPos.y);
    }

    @Override
    public float getCurSpeed() {
        return point.getMoveSpeed() / BattleConfig.C_SCALE_SPEED;
    }

    public Pbmethod.PbUnit toProtoAdd(int chunkId) {
        Pbmethod.PbUnit.Builder pbAdd = Pbmethod.PbUnit.newBuilder();
        pbAdd.setType(UnitType.ENEMY.value);
        pbAdd.setId(id);
        pbAdd.setChunkId(chunkId);
        pbAdd.setIsAdd(true);
        pbAdd.setPos(pos.toProto());
        pbAdd.setDirection(direction.toProto());
        pbAdd.setClanId(clanId);
        pbAdd.setRangeAttack(rangeAttack);
        pbAdd.setAvatar(model);
        pbAdd.setSpeed((int) point.getMoveSpeed());
        pbAdd.setName(name);
        pbAdd.setAlive(alive);
        pbAdd.addAllPoint(point.toProto());
        pbAdd.addAllInfo(getListInfo(0));
        return pbAdd.build();
    }
}
