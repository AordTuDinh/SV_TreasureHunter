package game.battle.model;

import game.battle.calculate.MathLab;
import game.battle.effect.EffectRoom;
import game.battle.effect.SkillEffect;
import game.battle.object.*;
import game.battle.type.AttackType;
import game.battle.type.CharacterType;
import game.battle.type.RoomState;
import game.battle.type.StateType;
import game.config.CfgEventDrop;
import game.config.aEnum.DetailActionType;
import game.config.aEnum.FactionType;
import game.config.aEnum.RoomType;
import game.dragonhero.BattleConfig;
import game.dragonhero.mapping.main.ResEnemyEntity;
import game.dragonhero.server.Constans;
import game.dragonhero.service.battle.AnimationType;
import game.dragonhero.service.resource.ResSkill;
import game.dragonhero.service.user.Bonus;
import game.dragonhero.table.BaseBattleRoom;
import game.object.BonusConfig;
import lombok.Data;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.NumberUtil;
import protocol.Pbmethod;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Data
public class Enemy extends Character implements Serializable {
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

    public Enemy(ResEnemyEntity enemy, Pos startPos, Pos direction, int teamId, BaseBattleRoom room) {
        this.name = enemy.getName();
        this.model = enemy.getModel();
        this.faction = enemy.getFaction();
        this.autoAttack = enemy.getAutoAttack() == 1;
        this.radius = enemy.getRadius();
        this.point = enemy.toPoint();
        this.rangeView = enemy.getRangeView();
        canMove = point.getMoveSpeed() > 0;
        this.hasDamage = (point.getMagicDamage() > 0 || point.getAttackDamage() > 0);
        this.enemyKey = enemy.getId();
        this.timeCreate = System.currentTimeMillis();
        this.timeActive = System.currentTimeMillis() + 1000;
        this.attackType = enemy.getAttackType();
        this.delayAnimAttack = enemy.getDelayAnimAttack();
        this.rangeAttack = enemy.getRangeAttack();
        this.forcePush = enemy.getForcePush();
        this.listBonus = enemy.getABonus();
        this.panelMap = new PanelMap(room.getMapInfo().getMapData());
        this.type = CharacterType.MONSTER;
        if (attackType == AttackType.LONG_RANGE) {
            this.weaponEquip = new ArrayList<>();
            weaponEquip.add(new Shuriken(enemy.getWeapon(), 0));
        }
        this.pos = startPos;
        this.isBoss = enemy.getBossType() == 1;
        this.instancePos = pos.clone();
        this.direction = direction;
        this.id = room.getIdNext();
        this.teamId = teamId;
        this.alive = true;
        this.room = room;
        this.hasBonusKillMe = true;
        this.isMove = false;
//        this.f0Toxic = new ArrayList<>();
        this.effectsBody = new ArrayList<>();
        this.attackerInfo = new HashMap<>();
        this.damage = getPoint().getAttackDamage() + getPoint().getMagicDamage();
        this.setTimeRandomMove();
    }

    @Override
    public void revive() {
        if (canRevive() && !isAlive()) {
            timeRevive = System.currentTimeMillis();
            resetData();
            pos = instancePos.clone();
            protoStatus(StateType.REVIVE, (long) (pos.x * 1000), (long) (pos.y * 1000));
        }
    }

    public boolean canRevive() {
        return DateTime.isAfterTime(timeDie, BattleConfig.M_timeRevive);
    }

    @Override
    public boolean isHit() {
        return !DateTime.isAfterTime(timeBeHit, BattleConfig.M_timeBeHit);
    }

    @Override
    public void protoDie(Character killer) {
        super.protoDie(killer);
        protoStatus(StateType.DIE, (long) faction.value);
    }

    @Override
    public synchronized void bonusKillMe(Character killer) {
        if (killer.isPlayer()) {
            hasBonusKillMe = false;
            Player player = ((Player) killer);
            BonusKillEnemy bonus = getBonusWithPer(listBonus, player.getBuffs());
            bonus.addBonus(CfgEventDrop.bonusDrop(CfgEventDrop.config.getRateDropCampaign(), 1));
            int oldLevel = player.getMUser().getUser().getLevel();
            // add buff exp, gold, itemDrop
            player.sendForceBonus(bonus, DetailActionType.BONUS_KILL_ENEMY.getKey(), pos);
            int newLevel = player.getMUser().getUser().getLevel();
            if (newLevel > oldLevel) {
                EffectRoom eff = new EffectRoom(killer, killer.getPos(), new SkillEffect(ResSkill.getSkills(0)));
                getBattleRoom().addEffectRoom(eff);
            }
            player.addNumKillMonster(this);
            if (room.getRoomType() == RoomType.CAMPAIGN.value) {
                player.getMUser().getUData().addCampaignNormal(room.getSubId(), 1);
            }

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
            } else if (bm.getBonus().get(0).intValue() == Bonus.BONUS_EXP) {
                int num = bm.getMax() == 1 ? 1 : NumberUtil.getRandom(bm.getMin(), bm.getMax());
                num += num * perBuff.get(2) / 100f;
                result.setExp(num);
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

    public boolean hasAttackLongRange() {
        return inRankAttack(AttackType.LONG_RANGE) && hasActionAttack()&& !isMove() && alive && targetAttack != null && targetAttack.isAlive() && hasActiveSkillNormal();
    }

    public boolean hasAttackMelee() {
        return inRankAttack(AttackType.MELEE) && hasActionAttack() && alive && targetAttack != null && targetAttack.isAlive();
    }

    public boolean isAttackDone() {
        return DateTime.isAfterTime(timeActionAttack, 1f);
    }


    boolean hasActiveSkillNormal() { // check CD
        return weaponEquip.get(0).hasActiveSkill();
    }

    @Override
    public void activeSkill(int skillId) {
        setTimeAttack();
        Shuriken shu = weaponEquip.get(skillId);
        weaponEquip.get(skillId).setActiveSkill();
        protoStatus(StateType.USE_SKILL, (long) (skillId), shu.getTimeActiveSkill(), shu.getNumberAttack(), (long) (getDirection().x * 1000L), (long) (getDirection().y * 1000L), 0L);
    }


    public boolean inRankAttack(AttackType attackType) {
        if (targetAttack == null) return false;
        if (attackType == AttackType.LONG_RANGE) {
            return pos.distance(targetAttack.pos) < rangeAttack;
        } else {
            return pos.distance(targetAttack.pos) < rangeAttack && Math.abs(pos.y - targetAttack.getPos().y) < BattleConfig.E_RangeYAttack;
        }
    }

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
        enemyProcess();
    }

    private void enemyProcess() {
        if (!beBlock()) {

            if (targetAttack == null || !targetAttack.isAlive() || !targetAttack.isReady()) { //
                unTarget();
            }
            // check target attack
            if (autoAttack && (targetAttack == null || !inRankAttack(attackType))) {
                targetAttack = findTargetForEnemy(rangeAttack);
            }
            if (attackType == AttackType.LONG_RANGE) {
                E_attackLongRange();
            } else if (attackType == AttackType.COLLIDE) {
                E_attackCollider();
            } else {
                E_attackMelee();
            }
        }
    }

    public void E_attackMelee() {
        if (autoAttack) {
            if (targetAttack == null && isReady()) {
                Character target = findTargetForEnemy(10000f);
                if (target != null) {
                    target.addTargetSelf(this);
                    targetAttack = target;
                }
            }
            isBeAttack = targetAttack != null;
        }
        if (isBeAttack) {
            if (moveToTargetDone() && hasAttackMelee()) {
                setTimeAttack();
                protoStatus(StateType.PLAY_ANIMATION, AnimationType.ATTACK.id);
                room.addCoroutine(new Coroutine(delayAnimAttack, () -> {
                    if (targetAttack != null) targetAttack.beAttackMelee(this);
                }));
            }
            if (!inRankAttack(attackType) && isAttackDone()) {
                enemyMove();
            } else { // trong tầm đánh thì k move
                setMove(false);
            }
        } else {
            genRandomMove(); // move idle
            if (!moveToTargetDone()) {
                enemyMove();
            } else {
                setTargetMove(null);
                setMove(false);
            }
        }
    }

    public void E_attackLongRange() {
        // bi danh thi danh lai, k du range thi di chuyen den target
        if (autoAttack) {
            if (targetAttack == null && isReady()) {
                Character target = findTargetForEnemy(rangeView);
                if (target != null) {
                    target.addTargetSelf(this);
                    targetAttack = target;
                }
            }
            isBeAttack = targetAttack != null;
        }
        //System.out.println("isBeAttacktack = " + isBeAttack);
        if (isBeAttack) {
            boolean isMove = moveToTargetDone();
            if (isMove && hasAttackLongRange()) {
                setDirection(getFutureDirection(10,20));
                activeSkill(skillNormal);
                room.addCoroutine(new Coroutine(delayAnimAttack, () -> {
                    getBattleRoom().addBullet(this, skillNormal, enemyAttackLongRange());
                }));
            }
            if (!inRankAttack(attackType) && isAttackDone()) {
                enemyMove();
            } else { // trong tầm đánh thì k move
                setMove(false);
            }
        } else {
            genRandomMove(); // move idle
            if (!moveToTargetDone()) {
                enemyMove();
            } else {
                setTargetMove(null);
                setMove(false);
            }
        }

        // mode hard  ->  find target, tim thay thi duoi theo danh, yeu cau phai o trong 1 khoang view
//        if (room.getCacheBattle().getMode() == RoomMode.CAMPAIGN_HARD) {
//            //luc nay chua co target nen phai tim
//            if (!isBeAttack()) {
//                Character target = findTargetForEnemy();
//                if (target != null) {
//                    setTargetAttack(target);
//                    setBeAttack(true);
//                }
//            }
//        }

        // gio se check truong hop k bi danh cung k co target -> cho no idle -> move linh tinh

    }

    public List<Bullet> enemyAttackLongRange() {
        List<Bullet> bullets = new ArrayList<>();
        bullets.addAll(skillActive(getShurikenSlot(skillNormal), direction, pos, getShurikenSlot(skillNormal).getDegree()));
        return bullets;
    }

    @Override
    public boolean beBlock() {
        return super.beBlock() || System.currentTimeMillis() < timeActive;
    }

    // chạy qua chạy lại chỗ player va vào làm player mất máu
    public void E_attackCollider() {
        // mode hard  ->  find target, tim thay thi duoi theo danh, yeu cau phai o trong 1 khoang view
        if (point.getMoveSpeed() == 0) return;
        if (autoAttack) {
            if (targetAttack == null && isReady()) {
                Character target = findTargetForEnemy(1000f);
                if (target != null) {
                    target.addTargetSelf(this);
                    targetAttack = target;
                }
            }
            isBeAttack = targetAttack != null;
        }

        if (isBeAttack) {
            if (targetAttack != null && targetAttack.canBeAttack(getTeamId())) {
                activeSkill(skillNormal);
                if (hasAttackLongRange()) {
                    getBattleRoom().addBullet(this, skillNormal, enemyAttackLongRange());
                }
                if (canMove && !inRankAttack(attackType)) {
                    enemyMove();
                } else { // trong tầm đánh thì k move
                    setMove(false);
                }
            }
        } else {
            genRandomMove(); // move idle
            if (!moveToTargetDone()) {
                enemyMove();
            } else {
                setTargetMove(null);
                setMove(false);
            }
        }
    }


    //Fixme DATE: 8/18/2022 LƯU Ý ---> sắp xếp thứ tự ưu tiên
    // move attack(move target) -> move Idle(targetmove !=null)
    // target move đã đc xác định rồi, chỉ move thôi
    public void enemyMove() {
        Pos targetMove;
        if (isBeAttack() && targetAttack != null) targetMove = getPosTargetMove(targetAttack);
        else targetMove = getTargetMove();
        if (targetMove == null || beBlock()) return;
        if (Math.abs(targetMove.x - pos.x) > 1f) {
            Pos direction = pos.getDirectionTo(targetMove);
            setDirection(direction);
        }
        Pos nd = Pos.moveFromDirection(direction, getCurSpeed());
        move(nd);
        if (!nd.equals(Pos.zero())) {
            setDirection(nd.normalized());
        }
    }

    private Pos getPosTargetMove(Character target) {
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


    Character findTargetForEnemy(float rangeView) {// BattleConfig.M_rangeViewTarget
        int index = 0;
        double min = 99999f;
        for (int i = 0; i < room.getAPlayer().size(); i++) {
            if (room.getAPlayer().get(i).isAlive()) {
                double dis = room.getAPlayer().get(i).getPos().distance(getPos());
                if (dis < min) {
                    min = dis;
                    index = i;
                }
            }
        }
        if (min <= rangeView) return room.getAPlayer().get(index);
        return null;
    }

    private boolean checkTimeAttack() {
        return DateTime.isAfterTime(timeCheckDirectionAttack, BattleConfig.E_timeCheckDirection);
    }

    @Override
    public float getCurSpeed() {
        if (!isBeAttack) {
            float idleSpeed = BattleConfig.M_speedMoveIdle / BattleConfig.C_SCALE_SPEED;
            return idleSpeed < 1f ? 1f : idleSpeed;
        } else {
            return point.getMoveSpeed() / BattleConfig.C_SCALE_SPEED;
        }
    }


    // region proto

    public Pbmethod.PbCharInfo toProtoInfo() {
        Pbmethod.PbCharInfo.Builder pbUser = Pbmethod.PbCharInfo.newBuilder();
        pbUser.setLevel(0);
        pbUser.setAlive(alive);
        pbUser.addAllPoint(point.toProto());
        return pbUser.build();
    }

    public Pbmethod.PbUnitAdd.Builder toProtoAdd() {
        Pbmethod.PbUnitAdd.Builder pbAdd = Pbmethod.PbUnitAdd.newBuilder();
        pbAdd.setType(Constans.TYPE_MONSTER);
        pbAdd.setId(id);
        pbAdd.addAvatar(model);
        pbAdd.setTeamId(teamId);
        pbAdd.setRangeAttack(rangeAttack);
        pbAdd.setIsAdd(true);
        pbAdd.setPos(pos.toProto());
        pbAdd.setBotLeft(panelMap.getBotLeft().toProto());
        pbAdd.setTopRight(panelMap.getTopRight().toProto());
        pbAdd.setDirection(direction.toProto());
        pbAdd.setSpeed((int) point.getMoveSpeed());
        pbAdd.addInfo(type.value);// info[0]= type
        pbAdd.addInfo(enemyKey);// info[1]= key
        pbAdd.addInfo(idDameSkin);
        pbAdd.addInfo(idChatFrame);
        pbAdd.addInfo(idTrial);
        pbAdd.setCharacterInfo(toProtoInfo());
        pbAdd.setFaction(FactionType.NULL.value);
        return pbAdd;
    }
}
