package game.battle.model;

import game.battle.object.*;
import game.battle.type.RoomState;
import game.battle.type.UnitType;
import game.config.CfgEventDrop;
import game.config.aEnum.DetailActionType;
import game.treasure.BattleConfig;
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
    private boolean moveTargetDone;
    long damage;
    int skillNormal = 0;
    int enemyKey;
    boolean autoAttack, canMove;
    long timeActive;
    float rangeView;


    public Enemy() {
        this.clanId = -1;
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
    public synchronized void bonusKillMe(Unit killer) {
        if (killer.isPlayer()) {
            hasBonusKillMe = false;
            Player player = ((Player) killer);
            BonusKillEnemy bonus = getBonusWithPer(listBonus, player.getBuffs());
            bonus.addBonus(CfgEventDrop.bonusDrop(CfgEventDrop.config.getRateDropCampaign(), 1));
            player.sendForceBonus(bonus, DetailActionType.BONUS_KILL_ENEMY.getKey(), pos);
            player.addNumKillMonster(this);
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
        // 1 /3 co hoi move -> move xong cho 1s roi random move tiep
        if (!hasActionMove() || !isReady()) return;
        if (targetMove != null) return;
        int rand = NumberUtil.getRandom(3);
        if (rand == 0) { // move
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
        float x = NumberUtil.randomRange(BattleConfig.M_rangeMove);
        float y = NumberUtil.randomRange(BattleConfig.M_rangeMove);
        return new Pos(x, y);
    }


    public boolean hasAttackMelee() {
       //  return inRankAttack(AttackType.MELEE) && hasActionAttack() && alive && targetAttack != null && targetAttack.isAlive();
        return true;
    }

    public boolean isAttackDone() {
        return DateTime.isAfterTime(timeActionAttack, 1f);
    }


    public void activeSkill(int skillId) {
        setTimeAttack();
    }


//    public boolean inRankAttack(AttackType attackType) {
//        if (targetAttack == null) return false;
//        if (attackType == AttackType.LONG_RANGE) {
//            return pos.distance(targetAttack.pos) < rangeAttack;
//        } else {
//            return pos.distance(targetAttack.pos) < rangeAttack && Math.abs(pos.y - targetAttack.getPos().y) < BattleConfig.E_RangeYAttack;
//        }
//    }

    public boolean hasActiveMove() {
        return DateTime.isAfterTime(timeActionAttack, BattleConfig.E_timeDelayAttackToMove);
    }

    private boolean hasActionAttack() {
//        System.out.println("point.getAttackSpeed() = " + point.getAttackSpeed());
        return DateTime.isAfterTime(timeActionAttack, point.getAttackSpeed()) && !isMove && DateTime.isAfterTime(timeActionMove, 0.3f);
    }

    @Override
    public void Update() {
        if (room.getRoomState() != RoomState.ACTIVE) return;
        //enemyProcess();
    }

//    private void enemyProcess() {
//        if (!beBlock()) {
//            // check target attack
//            if (autoAttack && (targetAttack == null || !inRankAttack(attackType))) {
//                targetAttack = findTargetForEnemy(rangeAttack);
//            }
//            if (attackType == AttackType.LONG_RANGE) {
//                E_attackLongRange();
//            } else if (attackType == AttackType.COLLIDE) {
//                E_attackCollider();
//            } else {
//                E_attackMelee();
//            }
//        }
//    }

//    public void E_attackLongRange() {
//        // bi danh thi danh lai, k du range thi di chuyen den target
//        if (autoAttack) {
//            if (targetAttack == null && isReady()) {
//                Unit target = findTargetForEnemy(rangeView);
//                if (target != null) {
//                    target.addTargetSelf(this);
//                    targetAttack = target;
//                }
//            }
//            isBeAttack = targetAttack != null;
//        }
//        //System.out.println("isBeAttacktack = " + isBeAttack);
//        if (isBeAttack) {
//            boolean isMove = moveToTargetDone();
//            if (isMove && hasAttackLongRange()) {
//                setDirection(getFutureDirection(10, 20));
//                activeSkill(skillNormal);
//                room.addCoroutine(new Coroutine(delayAnimAttack, () -> {
//                    //   getBattleRoom().addBullet(this, skillNormal, enemyAttackLongRange());
//                }));
//            }
//            if (!inRankAttack(attackType) && isAttackDone()) {
//                enemyMove();
//            } else { // trong tầm đánh thì k move
//                setMove(false);
//            }
//        } else {
//            genRandomMove(); // move idle
//            if (!moveToTargetDone()) {
//                enemyMove();
//            } else {
//                setTargetMove(null);
//                setMove(false);
//            }
//        }
//
//        // mode hard  ->  find target, tim thay thi duoi theo danh, yeu cau phai o trong 1 khoang view

    /// /        if (room.getCacheBattle().getMode() == RoomMode.CAMPAIGN_HARD) {
    /// /            //luc nay chua co target nen phai tim
    /// /            if (!isBeAttack()) {
    /// /                Character target = findTargetForEnemy();
    /// /                if (target != null) {
    /// /                    setTargetAttack(target);
    /// /                    setBeAttack(true);
    /// /                }
    /// /            }
    /// /        }
//
//        // gio se check truong hop k bi danh cung k co target -> cho no idle -> move linh tinh
//
//    }
    @Override
    public boolean beBlock() {
        return super.beBlock() || System.currentTimeMillis() < timeActive;
    }

    //Fixme DATE: 8/18/2022 LƯU Ý ---> sắp xếp thứ tự ưu tiên
    // move attack(move target) -> move Idle(targetmove !=null)
    // target move đã đc xác định rồi, chỉ move thôi
//    public void enemyMove() {
//        Pos targetMove;
//        if (isBeAttack() && targetAttack != null) targetMove = getPosTargetMove(targetAttack);
//        else targetMove = getTargetMove();
//        if (targetMove == null || beBlock()) return;
//        if (Math.abs(targetMove.x - pos.x) > 1f) {
//            Pos direction = pos.getDirectionTo(targetMove);
//            setDirection(direction);
//        }
//        Pos nd = Pos.moveFromDirection(direction, getCurSpeed());
//        move(nd);
//        if (!nd.equals(Pos.zero())) {
//            setDirection(nd.normalized());
//        }
//    }

    private Pos getPosTargetMove(Unit target) {
        return new Pos(target.pos.x + NumberUtil.getRandom(-1, 1), target.pos.y + NumberUtil.getRandom(-1, 1));
    }

    //public void enemyMoveAttackMelee() {
    //    // đã check null và dead ở trên rồi
    //    Pos atkPos = targetAttack.getPos();
    //    // check trường hợp player move thì direction sẽ thay đổi liên tuc
    //    // dùng biến để check move attack
    //    directionMoveAttack = MathLab.getDirection(pos, atkPos);
    //    if (directionMoveAttack.equals(Pos.zero()) || targetAttack.isMove || checkTimeAttack()) {
    //        // cong nghe moi
    //        if (targetAttack.isMove) { // đang di chuyển thì tách nhau ra
    //            for (Map.Entry<Integer, Character> character : targetAttack.targetSelf.entrySet()) {
    //                Character enemy = character.getValue();
    //                if (id != enemy.getId()) {
    //                    float distance = (float) enemy.getPos().distance(pos);
    //                    if (distance < BattleConfig.E_distance_attack) {
    //                        int randAngle = NumberUtil.getRandom(-50, 50);
    //                        directionMoveAttack = MathLab.angle2Direction(randAngle, directionMoveAttack);
    //                    } else directionMoveAttack = Pos.zero();
    //                }
    //            }
    //        } else { // đứng yên
    //            Pos randMove = new Pos(atkPos.x + NumberUtil.randomRange(0.2f), atkPos.y + NumberUtil.randomRange(0.2f));
    //            directionMoveAttack = pos.getDirectionTo(randMove);
    //            setTargetMove(randMove);
    //            moveTargetDone = false;
    //        }
    //        timeCheckDirectionAttack = System.currentTimeMillis();
    //    }
    //    if (directionMoveAttack.equals(Pos.zero())) return;
    //    Pos nd = Pos.moveFromDirection(directionMoveAttack, getCurSpeed());
    //    move(nd);
    //    if (!nd.equals(Pos.zero()) && targetAttack != null && targetAttack.direction.x != 0) {
    //        setDirection(directionMoveAttack);
    //    }
    //    // move qua lại quanh player
    //    if (moveToTargetDone()) moveTargetDone = true;
    //    if (targetMove != null && moveTargetDone && getPos().distance(targetMove) > BattleConfig.M_rangeMoveAttack) {
    //        directionMoveAttack = Pos.zero();
    //    }
    //}


//    public void enemyMoveAttackMelee() {
//        // đã check null và dead ở trên rồi
//        Pos atkPos = targetAttack.getPos();
//        // check trường hợp player move thì direction sẽ thay đổi liên tuc
//        // dùng biến để check move attack
//        directionMoveAttack = MathLab.getDirection(pos, atkPos);
//        if (directionMoveAttack.equals(Pos.zero()) || targetAttack.isMove || checkTimeAttack()) {
//            // cong nghe moi

    /// /            System.out.println("targetAttack.isMove = " + targetAttack.isMove());
//            if (targetAttack.isMove()) { // đang di chuyển thì tách nhau ra
//                for (Map.Entry<Integer, Character> character : targetAttack.targetSelf.entrySet()) {
//                    Character enemy = character.getValue();
//                    if (id != enemy.getId()) {
//                        float distance = (float) enemy.getPos().distance(pos);
//                        if (distance < BattleConfig.E_distance_attack) {
//                            int randAngle = NumberUtil.getRandom(-50, 50);
//                            directionMoveAttack = MathLab.angle2Direction(randAngle, directionMoveAttack);
//                        }
//                        //else directionMoveAttack = Pos.zero();
//                    }
//                }
//            } else { // đứng yên
//                Pos randMove = new Pos(atkPos.x + NumberUtil.randomRange(0.2f), atkPos.y + NumberUtil.randomRange(0.2f));
//                directionMoveAttack = pos.getDirectionTo(randMove);
//                setTargetMove(randMove);
//                moveTargetDone = false;
//            }
//            timeCheckDirectionAttack = System.currentTimeMillis();
//        }
//        if (directionMoveAttack.equals(Pos.zero())) return;
//        Pos nd = Pos.moveFromDirection(directionMoveAttack, getCurSpeed());
//        move(nd);
//        if (!nd.equals(Pos.zero()) && targetAttack != null && targetAttack.direction.x != 0) {
//            setDirection(directionMoveAttack);
//        }
//        // move qua lại quanh player
//        if (moveToTargetDone()) moveTargetDone = true;
//        if (targetMove != null && moveTargetDone && getPos().distance(targetMove) > BattleConfig.M_rangeMoveAttack) {
//            directionMoveAttack = Pos.zero();
//        }
//    }


    Unit findTargetForEnemy(float rangeView) {// BattleConfig.M_rangeViewTarget
        int index = 0;
        double min = 99999f;
//        for (int i = 0; i < room.getAPlayer().size(); i++) {
//            if (room.getAPlayer().get(i).isAlive()) {
//                double dis = room.getAPlayer().get(i).getPos().distance(getPos());
//                if (dis < min) {
//                    min = dis;
//                    index = i;
//                }
//            }
//        }
//        if (min <= rangeView) return room.getAPlayer().get(index);
        return null;
    }

    private boolean checkTimeAttack() {
        return DateTime.isAfterTime(timeCheckDirectionAttack, BattleConfig.E_timeCheckDirection);
    }

    @Override
    public float getCurSpeed() {
        return point.getMoveSpeed() / BattleConfig.C_SCALE_SPEED;
    }


    // region proto

    public Pbmethod.PbUnit toProtoAdd(int chunkId) {
        Pbmethod.PbUnit.Builder pbAdd = Pbmethod.PbUnit.newBuilder();
        pbAdd.setType(UnitType.ENEMY.value);
        pbAdd.setId(id);
        pbAdd.setAvatar(model);
        pbAdd.setChunkId(chunkId);
        pbAdd.setClanId(clanId);
        pbAdd.setRangeAttack(rangeAttack);
        pbAdd.setIsAdd(true);
        pbAdd.setPos(pos.toProto());
        pbAdd.setDirection(direction.toProto());
        pbAdd.setSpeed((int) point.getMoveSpeed());
        pbAdd.addInfo(type.value);// info[0]= type
        pbAdd.addInfo(enemyKey);// info[1]= key
        pbAdd.addInfo(idDameSkin);
        pbAdd.addInfo(idChatFrame);
        pbAdd.addInfo(idTrial);
        pbAdd.setAlive(alive);
        pbAdd.addAllPoint(point.toProto());
        return pbAdd.build();
    }
}
