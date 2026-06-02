package game.battle.model;

import game.battle.object.*;
import game.battle.type.AnimationType;
import game.battle.type.RoomState;
import game.battle.type.UnitType;
import game.config.CfgEventDrop;
import game.config.aEnum.DetailActionType;
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
    boolean autoAttack;
    Unit targetAttack;

    public Enemy(int enemyKey, Unit player, Pos pos) {
        setRoom(player.getRoom());
        setPanelMap(player.getPanelMap());
        this.model = enemyKey;
        this.clanId = -1;
        this.pos = pos;
        this.instancePos = pos.clone();
        ResMobEntity mob = ResMob.getMob(enemyKey);
        this.point = mob.getPoint();
        this.name = mob.getName();
        this.rangeAttack = mob.getRangeAttack();
        setListBonus(mob.getBonus());
        setType(UnitType.ENEMY);
        this.autoAttack = false;
        this.delayAnimAttack = BattleConfig.M_delayAttackDamage;
        resetData();
    }

    @Override
    public Point resetData() {
        targetAttack = null;
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
        if (ownerDamage != null && ownerDamage.isPlayer() && targetAttack == null) {
            targetAttack = ownerDamage;
            targetMove = null;
        }
        super.beAttackDamage(ownerDamage, atkDame);
    }

    @Override
    public synchronized void bonusKillMe(Unit killer) {
        if (killer.isPlayer()) {
            hasBonusKillMe = false;
            Player player = ((Player) killer);
//            BonusKillEnemy bonus = getBonusWithPer(listBonus, player.getBuffs());
//            bonus.addBonus(CfgEventDrop.bonusDrop(Ote CfgEventDrop.config.getRateDropCampaign(), 1));
//            player.sendForceBonus(bonus, DetailActionType.BONUS_KILL_ENEMY.getKey(), pos);
//            player.addNumKillMonster(this);
        }
    }

    BonusKillEnemy getBonusWithPer(List<BonusConfig> aBonusConfig, List<Long> perBuff) {
        BonusKillEnemy result = new BonusKillEnemy(); // gold, gem, bonus
        for (int i = 0; i < aBonusConfig.size(); i++) {
            BonusConfig bm = aBonusConfig.get(i);
            if (bm.getBonus().get(0).intValue() == Bonus.BONUS_GOLD) {
                int num = bm.getMax() == 1 ? 1 : NumberUtil.getRandom(bm.getMin(), bm.getMax());
                num += num * perBuff.get(1) / 100f;
                result.setGold(num);
            } else {
                int rand = NumberUtil.getRandom(1000);
                if (rand < bm.getRate() + perBuff.get(0) / 10)
                    result.getBonus().addAll(Bonus.viewXNumber(new ArrayList<>(bm.getBonus()), NumberUtil.getRandom(bm.getMin(), bm.getMax())));
            }
        }
        return result;
    }

    public void genRandomMove() { // move idle
        if (!hasActionMove() || !isReady()) return;
        if (targetMove != null) return;
        int rand = NumberUtil.getRandom(100);
        if (rand < BattleConfig.M_idleMoveChance) {
            setMove(true);
            targetMove = Pos.v_add(panelMap, instancePos, randomMove());
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

    public boolean isAttackDone() {
        return DateTime.isAfterTime(timeActionAttack, 1f);
    }

    private boolean hasActionAttack() {
        return DateTime.isAfterTime(timeActionAttack, BattleConfig.M_attackSpeed) && !isMove()
                && DateTime.isAfterTime(timeActionMove, 0.3f);
    }

    private void setDirectionChase(Pos lookAt) {
        if (lookAt == null) return;
        Pos faceDir = pos.getDirectionTo(lookAt);
        if (faceDir.equals(Pos.zero()) || Math.abs(faceDir.x) < 0.01f) return;
        setDirection(faceDir);
    }

    private boolean isBeyondLeash() {
        return instancePos != null && pos.distance(instancePos) > BattleConfig.M_maxLeashFromSpawn;
    }

    @Override
    public void Update() {
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

        // Dang di den targetMove (idle hoac ve spawn) — khong cat vi lech instancePos
        if (targetMove != null) {
            if (!moveToTargetDone()) {
                enemyMove();
            } else {
                targetMove = null;
                setMove(false);
            }
            return;
        }

        idleWander();
    }

    private void moveToInstancePos() {
        targetMove = instancePos.clone();
        if (!moveToTargetDone()) {
            enemyMove();
        } else {
            targetMove = null;
            setMove(false);
        }
    }

    private void chaseAndAttack() {
        if (inRankAttackMelee() && hasActionAttack()) {
            setMove(false);
            setDirectionChase(targetAttack.getPos());
            setTimeAttack();
            protoStatus(Pbmethod.SubStateType.PLAY_ANIM, (long) AnimationType.ATTACK.value);
            scheduleAttackDamage(targetAttack);
        } else if (!inRankAttackMelee() && isAttackDone()) {
            targetMove = getChasePos(targetAttack);
            enemyMove();
        } else if (inRankAttackMelee()) {
            setMove(false);
            setDirectionChase(targetAttack.getPos());
        }
    }

    private void idleWander() {
        genRandomMove();
        if (!moveToTargetDone()) {
            enemyMove();
        } else {
            targetMove = null;
            setMove(false);
        }
    }

    public void enemyMove() {
        if (targetMove == null || beBlock()) return;
        Pos moveDir = pos.getDirectionTo(targetMove);
        if (moveDir.equals(Pos.zero())) return;
        setDirectionChase(targetAttack != null ? targetAttack.getPos() : targetMove);
        Pos nd = Pos.moveFromDirection(moveDir, getCurSpeed());
        move(nd);
        if (moveToTargetDone() && targetAttack == null) {
            targetMove = null;
            setMove(false);
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
        return Pos.capPos(target.getPos(), panelMap.getBotLeftP(), panelMap.getTopRightP(), BattleConfig.P_Width / 2);
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
