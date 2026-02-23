package game.battle.model;

import game.battle.calculate.IMath;
import game.battle.calculate.MathLab;
import game.battle.effect.Effect;
import game.battle.effect.EffectRoom;
import game.battle.effect.SkillEffect;
import game.battle.object.*;
import game.battle.type.*;
import game.config.CfgBattle;
import game.config.aEnum.FactionType;
import game.config.aEnum.RankType;
import game.config.aEnum.RoomType;
import game.dragonhero.BattleConfig;
import game.dragonhero.mapping.main.BaseMap;
import game.dragonhero.server.Constans;
import game.dragonhero.service.battle.EffectType;
import game.dragonhero.service.battle.TriggerType;
import game.dragonhero.table.BaseBattleRoom;
import game.dragonhero.table.BaseRoom;
import game.object.PointBuff;
import lombok.Data;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;
import protocol.Pbmethod;

import java.util.*;
import java.util.stream.Collectors;

@Data
public abstract class Character extends Mono {
    // info
    int id; // id in room
    int teamId;
    float radius, rangeAttack;
    Pos pos = Pos.zero(), direction = Pos.right();
    Point point;
    String name;
    AttackType attackType;
    boolean alive, beDot;
    BaseRoom room;
    int model;
    CharacterType type;
    // in battle
    Pos instancePos;  // noi character sinh ra
    PanelMap panelMap;//bot left + top right
    long timeDie, timeRevive;
    boolean hasBonusKillMe;
    // todo attack
    List<Shuriken> weaponEquip;
    Character targetAttack;
    public FactionType faction = FactionType.NULL;
    long timeBeHit = 0;
    int killById;
    int idDameSkin = 0;
    int idChatFrame = 0;
    int idTrial = 0;
    boolean isPowerSkill;
    public Map<Integer, Long> beDameInfo = new HashMap<>();
    // todo test move
    boolean isMove = false; // dang di chuyen
    long timeActionMove;  // thời gian thay đổi action move
    boolean isBeAttack; // bi danh
    Pos targetMove;
    List<Effect> effectsBody;
    Map<Integer, Long> timePush = new HashMap<>();
    boolean isAtkByHit = true, ready = true;
    long timeJoinRoom;
    long timeActiveRandomMove, timeCreate;
    boolean isBoss;
    long timeActionAttack;
    long timeCheckDirectionAttack;
    long[] timeActiveSlot = new long[]{0, 0, 0};
    //long timeBeAttack;
    Pos directionMoveAttack = Pos.zero(); // chưa có hướng move attack melee
    // save attacker info
    Map<Integer, List<Long>> attackerInfo = new HashMap<>(); // playerID   - timeAttack,shurikenIds : dùng để check thẳng đánh mình vừa đánh lúc nào + suriken id nào
    private static final int TIME_ATTACK = 0;
    private static final int SLOT_MELEE = 1;
    private static final int START_INDEX = 2;
    // skill eff
//    public List<Integer> f0Toxic = new ArrayList<>();
    // các thằng đang target đánh mình
    public Map<Integer, Character> targetSelf = new HashMap<>();
    boolean hasDamage = true;
    boolean sendDie = true;

    public void beAttackEffect(Effect effect, long atkDame, long magicDame, PointBuff... buffs) {
        if (!canBeAttack(effect.getOwner().getTeamId())) return;
        long[] damage = IMath.calculateDamageBase(atkDame, magicDame, effect.getFaction(), this, 1f, buffs.length > 0 ? buffs[0] : null, effect.getOwner());
        beAttackDamage(effect.getOwner(), damage[0], damage[1]);
        protoBeDameEffect(Arrays.asList((long) effect.getOwner().getId(), -damage[0], -damage[1]));
        checkBeAttackByEffect(effect.getOwner());
    }

    public boolean isPlayer() {
        return type == CharacterType.PLAYER;
    }


    public boolean isEnemy() {
        return type == CharacterType.MONSTER || type == CharacterType.BOSS_GOD || type == CharacterType.TOTEM;
    }

    private void checkBeAttackByEffect(Character attacker) {
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

    public void addTargetSelf(Character attacker) {
        if (!targetSelf.containsKey(attacker.getId())) {
            this.targetSelf.put(attacker.getId(), attacker);
        }
    }

    public void removeTargetSelf(Character attacker) {
        for (int i = 0; i < targetSelf.size(); i++) {
            if (targetSelf.get(i) != null && attacker.getId() == targetSelf.get(i).getId()) {
                targetSelf.remove(i);
                return;
            }
        }
    }


    public void beAttackMelee(Character attacker) {
        if (!attacker.hasDamage) return;
        long[] damage = IMath.calculateDamage(attacker, this, attacker.getFaction());
        beAttackDamage(attacker, damage[1], damage[2]);
        addAtkInfoMelee(attacker);
        protoBeDame(attacker, Arrays.asList((long) attacker.getId(), damage[0], -damage[1], -damage[2]));
    }

    public void beAttackCollider(Character attacker) {
        if (!attacker.hasDamage) return;
        addAtkInfoMelee(attacker);
        long[] damage = IMath.calculateDamage(attacker, this, attacker.getFaction());
        damage[1] = (long) (damage[1] * BattleConfig.M_PerDameCollider);
        damage[2] = (long) (damage[2] * BattleConfig.M_PerDameCollider);
        if (damage[1] <= 0 && damage[2] <= 0) damage[1] = 1;
        beAttackDamage(attacker, damage[1], damage[2]);
        protoBeDame(attacker, Arrays.asList((long) attacker.getId(), damage[0], -damage[1], -damage[2]));
    }


    public long getBeDameInfo(int userId){
        if(beDameInfo.containsKey(userId)){
            return beDameInfo.get(userId);
        }
        return 0;
    }

    public Pos getFutureDirection(int min, int max) {
        if (targetAttack == null) return Pos.zero();
        if (targetAttack.isMove()) {
            int rand = NumberUtil.getRandom(min, max);
            float distance = (float) pos.distance(targetAttack.getPos())/rand;
            Pos posNext = targetAttack.getPos().clone();
            Pos dirClone = targetAttack.direction.clone();
            dirClone.multiple(targetAttack.getCurSpeed() * distance);
            posNext.add(dirClone);
            return pos.getDirectionTo(posNext);
        } else return pos.getDirectionTo(targetAttack.pos);
    }

    // gửi riêng
    public void beAttackDamage(Character ownerDamage, long atkDame, long mAtkDame) {
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

    public BaseMap getBaseMap() {
        return room.getMapInfo();
    }


    public void beAttackBullet(Bullet bullet) {
        if (!canBeAttack(bullet.getOwner().getTeamId()) || !bullet.isAlive()) return;
        if (bullet.getCharacterAttack().contains(id)) return;
        bullet.minusPenetration(id);
        long[] damage = new long[3];
        List<Long> effs = new ArrayList<>();
        List<Long> eff = processTrigger(TriggerType.HIT, bullet);
        if (eff.size() > 0) effs.addAll(eff);
        if (triggerFirstAttack(bullet.getOwner(), bullet)) {
            eff = processTrigger(TriggerType.FIRST_HIT, bullet);
            if (eff.size() > 0) effs.addAll(eff);
        }
        if (bullet.isTrigger3TH()) {
            eff = processTrigger(TriggerType.TH3, bullet);
            if (eff.size() > 0) effs.addAll(eff);
        }

        if (bullet.isTrigger4TH()) {
            processTrigger(TriggerType.TH4, bullet);
        }
        if (bullet.isTrigger5TH()) {
            eff = processTrigger(TriggerType.TH5, bullet);
            if (eff.size() > 0) effs.addAll(eff);
        }
        damage = IMath.calculateDamage(bullet, this, effs);
        addAtkInfoMelee(bullet);
        beAttackDamage(bullet.getOwner(), damage[1], damage[2]);
        protoRangeDame(bullet.getOwner(), Arrays.asList((long) bullet.getOwner().getId(), damage[0], damage[1], damage[2], (long) bullet.getFaction().value, (long) (pos.x * 1000), (long) (pos.y * 1000)));
        processTriggerLastDame(bullet.getOwner(), bullet.getEffectSkill(), damage, bullet.getFaction());
    }


    List<Long> processTrigger(TriggerType triggerType, Bullet bullet) {
        SkillEffect eff = bullet.getEffectSkill();
        List<Long> effs = new ArrayList<>();
        if (!eff.isHasEffect() || !triggerType.equals(eff.getTriggerType())) return effs;
        if (bullet.getOwner().point.getCurMP() >= eff.getMP()) {
            if (eff.getMP() > 0) bullet.getOwner().updateMp(-eff.getMP());
            isPowerSkill = true;
            switch (triggerType) {
                case FIRST_HIT:
                    Long effectFirst = processTriggerFirstHit(bullet);
                    if (effectFirst != null) effs.add(effectFirst);
                    break;
                case HIT:
                    Long effectHit = processTriggerHit(bullet);
                    if (effectHit != null) effs.add(effectHit);
                    break;
                case TH3:
                    Long effect3 = processTrigger3TH(bullet);
                    if (effect3 != null) effs.add(effect3);
                    break;
                case TH4:
                    Long effect4 = processTrigger4TH(bullet);
                    if (effect4 != null) effs.add(effect4);
                    break;
                case TH5:
                    Long effect5 = processTrigger5TH(bullet);
                    if (effect5 != null) effs.add(effect5);
                    break;
            }
        }
        return effs;
    }

    Long processTriggerHit(Bullet bullet) {
        SkillEffect eff = bullet.getEffectSkill();
        switch (eff.getEffectType()) {
            case DEC_28: // Create
            case POISON: // create
            case BURNED: // create
                addEffectTime(new Effect(bullet.getOwner(), eff, bullet.getFaction()));
                break;
            case RE_HP:  // create
                if (bullet.getOwner().getPoint().getCurMP() >= eff.getMP()) {
                    if (eff.getMP() > 0) bullet.getOwner().updateMp(-eff.getMP());
                    long reHp = (long) (eff.getFirstPer() * point.getMaxHp() + eff.getNextPer() * point.getMagicDamage());
                    long reMaxHp = (long) (BattleConfig.S_maxReHp75 / 100f * point.getMaxHp());
                    reHp = reHp > reMaxHp ? reMaxHp : reHp;
                    reHp(reHp);
//                    protoStatus(StateType.RE_HP, reHp);
                    break;
                }
        }
        return null;
    }

    public Player getPlayer() {
        return (Player) this;
    }

    public void addBuffBossGod(float numRange) {
        this.rangeAttack += numRange;
        for (int i = 0; i < weaponEquip.size(); i++) {
            weaponEquip.get(i).buffBossGod(numRange);
        }
    }

    public Pet getPet() {
        return (Pet) this;
    }

    public Enemy getEnemy() {
        return (Enemy) this;
    }

    Long processTriggerFirstHit(Bullet bullet) {
        SkillEffect eff = bullet.getEffectSkill();
        switch (eff.getEffectType()) {
            case POISON: // Create
            case TOXIC: // Create
                addEffectTime(new Effect(bullet.getOwner(), eff, bullet.getFaction()));
                break;
            case DEC_DEF: // create
            case DEC_DEF2: // create
            case DEC_SPEED: // Create
                addEffectNow(new Effect(bullet.getOwner(), eff, bullet.getFaction()));
                break;
            case PARALYZE: // Create
                addEffectNow(new Effect(bullet.getOwner(), eff, bullet.getFaction()));
                break;
            case DMG_HP: // Create
                long dameMax = (long) (bullet.getOwner().point.getAttackDamage() * BattleConfig.S_celiDameMaxHp); // max 1000% attack dame
                long dameHp = eff.getFirstPer() * point.getMaxHp() > dameMax ? dameMax : (long) (eff.getFirstPer() * point.getMaxHp());
                protoStatus(StateType.EFFECT, eff.toEffect(bullet.getOwner(), bullet.getFaction()).toStateOne(pos));
                return dameHp;
//            case PUSH: // Create
//                bullet.setForcePush(bullet.getForcePush() + BattleConfig.S_addForcePush);
//                break;
        }
        return null;
    }


    Long processTrigger3TH(Bullet bullet) {
        SkillEffect eff = bullet.getEffectSkill();
        switch (eff.getEffectType()) {
            case DAME_MAGIC2: // Create
            case DAME_MAGIC3: // Create
                getBattleRoom().addEffectRoom(new EffectRoom(this, getPos().clone(), eff));
                long damage = (long) (eff.getFirstPer() * bullet.getOwner().point.getMagicDamage());
                return damage;
            case SANDSTORM: // Create
            case BOMB: // Create
            case BLIZZARD: // Create
                EffectRoom effectRoom = new EffectRoom(bullet.getOwner(), bullet.getPos().clone(), eff);
                getBattleRoom().addEffectRoom(effectRoom);
                break;
            case DMG_HP: // Create
                long dameMax = (long) (bullet.getOwner().point.getAttackDamage() * BattleConfig.S_celiDameMaxHp); // max 1000% attack dame
//                System.out.println("dameMax = " + dameMax);
                long dameHp = eff.getFirstPer() * point.getMaxHp() > dameMax ? dameMax : (long) (eff.getFirstPer() * point.getMaxHp());
//                System.out.println("dameHp = " + dameHp);
                protoStatus(StateType.EFFECT, eff.toEffect(bullet.getOwner(), bullet.getFaction()).toStateOne(pos));
                return dameHp;
            case INF: // Create
                if (bullet.getOwner().getPoint().getCurMP() >= eff.getMP()) {
                    if (eff.getMP() > 0) bullet.getOwner().updateMp(-eff.getMP());
                    EffectRoom effAdd = new EffectRoom(bullet.getOwner(), bullet.getPos().clone(), eff);
                    getBattleRoom().addEffectRoom(effAdd);
                }
                //else debug("Không đủ mana ---->>> ");
                break;
            case SHIELD_FIRE: // create
                if (bullet.getOwner().getPoint().getCurMP() >= eff.getMP()) {
                    if (eff.getMP() > 0) bullet.getOwner().updateMp(-eff.getMP());
                    bullet.getOwner().addEffectTime(new Effect(bullet.getOwner(), eff, bullet.getFaction()));
                    break;
                }
                break;
            case DOT_FIRE: //create
                if (bullet.getOwner().getPoint().getCurMP() >= eff.getMP()) {
                    if (eff.getMP() > 0) bullet.getOwner().updateMp(-eff.getMP());
                    addEffectTime(new Effect(bullet.getOwner(), eff, bullet.getFaction()));
                    break;
                }
                break;
        }
        return null;
    }

    public BaseBattleRoom getBattleRoom() {
        return (BaseBattleRoom) room;
    }

    public Shuriken getShurikenSlot(int slot) {
        return weaponEquip.get(slot);
    }

    Long processTrigger4TH(Bullet bullet) {
        SkillEffect eff = bullet.getEffectSkill();
        switch (eff.getEffectType()) {

        }
        return 0L;
    }

    Long processTrigger5TH(Bullet bullet) {
        SkillEffect eff = bullet.getEffectSkill();
        switch (eff.getEffectType()) {
            case DAME_MAGIC: // create
                Effect efx = new Effect(bullet.getOwner(), eff, bullet.getFaction());
                protoStatus(StateType.EFFECT, efx.toStateOne(pos));
                long attackDame = (long) (eff.getFirstPer() * bullet.getOwner().point.getMagicDamage());
                return attackDame;
            case SHIELD_FIRE: // create
                if (bullet.getOwner().getPoint().getCurMP() >= eff.getMP()) {
                    if (eff.getMP() > 0) bullet.getOwner().updateMp(-eff.getMP());
                    bullet.getOwner().addEffectTime(new Effect(bullet.getOwner(), eff, bullet.getFaction()));
                    break;
                }
                break;
            case DEC_DEF: // create
            case DEC_DEF2: // create
                addEffectNow(new Effect(bullet.getOwner(), eff, bullet.getFaction()));
                break;
        }
        return null;
    }

    void processTriggerLastDame(Character attacker, SkillEffect eff, long[] damage, FactionType faction) {
        if (!eff.isHasEffect() || eff.getTriggerType() != TriggerType.LAST_DAME) return;
        long dame = damage[1];
        isPowerSkill = true;
        switch (eff.getEffectType()) {
            case RE_HP:  // create
                if (attacker.getPoint().getCurMP() >= eff.getMP()) {
                    if (eff.getMP() > 0) attacker.updateMp(-eff.getMP());
                    long reHp = (long) (eff.getFirstPer() * dame);
                    long reMaxHp = (long) (BattleConfig.S_maxReHp75 / 100f * point.getMaxHp());
                    reHp = reHp > reMaxHp ? reMaxHp : reHp;
                    attacker.reHp(reHp);
                }
                break;
            case RE_HP12:  // create
                if (attacker.getPoint().getCurMP() >= eff.getMP()) {
                    if (eff.getMP() > 0) attacker.updateMp(-eff.getMP());
                    long reHp = (long) (eff.getFirstPer() * dame);
                    long reMaxHp = (long) (BattleConfig.S_maxReHp75 / 100f * point.getMaxHp());
                    reHp = reHp > reMaxHp ? reMaxHp : reHp;
                    attacker.reHp(reHp);
                    protoStatus(StateType.EFFECT, eff.toEffect(attacker, faction).toStateOne(pos));
                }
                break;
        }

    }

    //public boolean hasPush(int userId) {
    //    if (timePush.containsKey(userId)) { // get và set luôn
    //        boolean can = DateTime.isAfterTime(timePush.get(userId), BattleConfig.P_delayBePush);
    //        timePush.put(userId, System.currentTimeMillis());
    //        return can;
    //    } else {
    //        timePush.put(userId, System.currentTimeMillis());
    //        return true;
    //    }
    //}

    public void disableStateRuna() {
        weaponEquip.forEach(shuriken -> {
            shuriken.setRunaState(false);
        });
    }

    boolean triggerFirstAttack(Character attacker, Bullet bullet) {
        if (attackerInfo.containsKey(attacker.getId())) {
            List<Long> shuIds = attackerInfo.get(attacker.getId()).subList(START_INDEX, attackerInfo.get(attacker.getId()).size());
            return !shuIds.contains((long) bullet.getShurikenId());
        } else {
            return true;
        }
    }

    void addAtkInfoMelee(Bullet b) {
        if (attackerInfo.containsKey(b.getOwner().getId())) {
            attackerInfo.get(b.getOwner().getId()).set(TIME_ATTACK, System.currentTimeMillis());
            List<Long> lst2 = attackerInfo.get(b.getOwner().getId()).subList(START_INDEX, attackerInfo.get(b.getOwner().getId()).size());
            if (lst2 == null || lst2.isEmpty() || !lst2.contains((long) b.getShurikenId())) {
                List<Long> lst3 = new ArrayList<>(attackerInfo.get(b.getOwner().getId()));
                lst3.add((long) b.getShurikenId());
                attackerInfo.put(b.getOwner().getId(), lst3);
            }
        } else {
            attackerInfo.put(b.getOwner().getId(), Arrays.asList(System.currentTimeMillis(), 0L, (long) b.getShurikenId()));
        }
    }

    public boolean canReceiveEffect(EffectType effectType) {
        if (effectsBody.isEmpty()) return true;
        for (int i = 0; i < effectsBody.size(); i++) {
            Effect eff = effectsBody.get(i);
            if (eff.getSkill().getEffectType() == effectType && eff.getSkill().getEffectType().isIncreBody ||
                    effectsBody.stream().filter(effect1 ->effect1!=null && effect1.getSkill().getEffectType().id == eff.getSkill().getEffectType().id).count() == 0) {
                return true;
            }
        }
        return false;
    }

    void addAtkInfoMelee(Character enemy) {
        if (attackerInfo.containsKey(enemy.getId())) {
            attackerInfo.get(enemy.getId()).set(TIME_ATTACK, System.currentTimeMillis());
            attackerInfo.get(enemy.getId()).set(SLOT_MELEE, attackerInfo.get(enemy.getId()).get(SLOT_MELEE) + 1);
        } else {
            attackerInfo.put(enemy.getId(), Arrays.asList(System.currentTimeMillis(), 0L));
        }
    }

    // tránh trường hợp ăn đòn liên hoàn, sau 1 khoảng time mới ăn đòn từ thằng đó tiếp
    public boolean hasReciveMelee(Character attacker) {
        return canBeAttack(attacker.teamId) && DateTime.isAfterTime(getTimeAttack(attacker.getId()), BattleConfig.C_haSReciveDamage);
    }

    public boolean hasReceiveEffMelee(Character attacker) {
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


    //public List<Bullet> processActiveSkill(int skillId, Pos target) {
    //    Pos direction;
    //    if (target.equals(Pos.zero())) {
    //        direction = getDirection();
    //    } else {
    //        direction = IMath.getDirection(pos, target);
    //    }
    //    return activeSkillByDirection(skillId, direction);
    //}

    public List<Bullet> activeSkillByDirection(int skillInput, Pos direction) {
        skillInput += NInput.offsetSkill;
        List<Bullet> bullets = new ArrayList<>();
        if (direction.equals(Pos.zero())) {
            direction = getDirection().normalized();
        }
        if (skillInput == NInput.Skill1) {
            bullets.addAll(skillActive(getShurikenSlot(0), direction, pos, getShurikenSlot(0).getDegree()));
        } else if (skillInput == NInput.Skill2) {
            bullets.addAll(skillActive(getShurikenSlot(1), direction, pos, getShurikenSlot(1).getDegree()));
        } else if (skillInput == NInput.Skill3) {
            bullets.addAll(skillActive(getShurikenSlot(2), direction, pos, getShurikenSlot(2).getDegree()));
        } else if (skillInput == NInput.Skill4) {
            bullets.addAll(skillActive(getShurikenSlot(3), direction, pos, getShurikenSlot(3).getDegree()));
        } else if (skillInput == NInput.Skill5) {
            bullets.addAll(skillActive(getShurikenSlot(4), direction, pos, getShurikenSlot(4).getDegree()));
        }
        return bullets;
    }


    public List<Bullet> skillActive(Shuriken shuriken, Pos direction, Pos posInit, int degree) {
        // check trigger init
        isPowerSkill = false;
        List<Bullet> bulletInit = processTriggerInit(shuriken, direction, posInit);
        if (bulletInit != null) return bulletInit;
        // không có init thì khởi tạo như thường
        int row = shuriken.getShots().get(1);
        List<Bullet> bullets = new ArrayList<>();
        Bullet bullet = null;
        Pos pos = null;
        Pos dir = null;
        float off = 4f, off1 = 6f;
        for (int i = 1; i <= row; i++) {
            int tia = shuriken.getShots().get(0);
            if (tia == 1) {  // 1 tia
                dir = MathLab.angle2Direction(30, direction);
//                pos = new Pos(position.x - dir.x * i / off, position.y + BattleConfig.P_offsetYColTop);
//                pos = new Pos(position.x - dir.x * i / off, position.y + BattleConfig.P_offsetYColTop);
                bullet = new Bullet(this, direction, posInit, shuriken);
                bullets.add(bullet);
            } else if (tia == 2) {
                if (i <= 1) {
                    dir = MathLab.angle2Direction(30, direction);
                    pos = new Pos(posInit.x - dir.x * i / off, posInit.y + BattleConfig.P_offsetYColTop);
                } else {
                    pos = new Pos(posInit.x - dir.x * i / off, posInit.y + BattleConfig.P_offsetYColTop);
//                    pos = new Pos(bullets.get(0).getPos().x - direction.x * i / off1, bullets.get(0).getPos().y + BattleConfig.P_offsetYColTop);
                }
                bullet = new Bullet(this, direction, pos, shuriken);
                bullets.add(bullet);
                dir = MathLab.angle2Direction(-30, direction);

                if (i <= 1) {
//                    pos = new Pos(position.x - dir.x * 2 * i / off, position.y + dir.y * i / off + BattleConfig.P_offsetYColTop);
                    pos = new Pos(posInit.x - dir.x * i / off, posInit.y + BattleConfig.P_offsetYColTop);
                } else {
//                    pos = new Pos(bullets.get(1).getPos().x - direction.x * 2 * i / off1, bullets.get(1).getPos().y + direction.y * i / off1 + BattleConfig.P_offsetYColTop);
                    pos = new Pos(bullets.get(1).getPos().x - direction.x * i / off1, bullets.get(1).getPos().y + BattleConfig.P_offsetYColTop);
                }
                bullet = new Bullet(this, direction, pos, shuriken);
                bullets.add(bullet);
            } else if (tia >= 3) {
//                pos = new Pos(position.x - direction.x * 2 * i / off, position.y + direction.y * i / off + BattleConfig.P_offsetYColTop);
                pos = new Pos(posInit.x - direction.x * i / off, posInit.y + BattleConfig.P_offsetYColTop);
                bullet = new Bullet(this, direction, pos, shuriken);
                bullets.add(bullet);
                int count = shuriken.getShots().get(0);
                for (int j = 1; j < count; j++) {
                    int index = Math.round(j / 2f);
                    int dau = j % 2 == 1 ? 1 : -1;
                    dir = MathLab.angle2Direction(degree * dau * index, direction);
//                    pos = new Pos(position.x - dir.x * 2 * i / off, position.y + dir.y * i / off + BattleConfig.P_offsetYColTop);
                    pos = new Pos(posInit.x - dir.x * i / off, posInit.y + BattleConfig.P_offsetYColTop);
                    bullet = new Bullet(this, dir, pos, shuriken);
                    bullets.add(bullet);
                }
            }
        }

        Bullet bullet0 = bullets.get(0);
        if (isPlayer()) { // de cho player truoc - sau ok se nghien cuu cho monster
            if (bullet0.isTrigger3TH()) processTriggerLastInit(bullets.get(0), TriggerType.INIT3, shuriken);
            if (bullet0.isTrigger4TH()) processTriggerLastInit(bullets.get(0), TriggerType.INIT4, shuriken);
            if (bullet0.isTrigger5TH()) processTriggerLastInit(bullets.get(0), TriggerType.INIT5, shuriken);
        }
        return bullets;
    }


    void processTriggerLastInit(Bullet bullet, TriggerType triggerType, Shuriken shuriken) {
        SkillEffect eff = bullet.getEffectSkill();
        if (!eff.isHasEffect() || !triggerType.equals(eff.getTriggerType())) return;
        if (bullet.getOwner().point.getCurMP() >= eff.getMP()) {
            if (eff.getMP() > 0) bullet.getOwner().updateMp(-eff.getMP());
            isPowerSkill = true;
            switch (triggerType) {
                case INIT3:
                    switch (eff.getEffectType()) {
                        case SMOKE: // create
                            EffectRoom effectRoom = new EffectRoom(bullet.getOwner(), getPos().clone(), eff);
                            getBattleRoom().addEffectRoom(effectRoom);
                            shuriken.setBlockCountTime(eff.getTime());
                            break;
                        case BERSERK:  // create
                            //System.out.println("----add BERSERK--------------------- ");
                            addEffectNow(new Effect(bullet.getOwner(), eff, bullet.getFaction()));
                            shuriken.setBlockCountTime(eff.getTime());
                            break;
                        case RUNA:  // create
                            //System.out.println("----add RUNA--------------------- ");
                            addEffectNow(new Effect(bullet.getOwner(), eff, bullet.getFaction()));
                            break;
                    }
                    break;
                case INIT4:
                case INIT5:
                    break;
            }
        }
    }

    List<Bullet> processTriggerInit(Shuriken shuriken, Pos direction, Pos position) {
        SkillEffect eff = shuriken.getEffectSkill();
        if (!eff.isHasEffect() || eff.getTriggerType() != TriggerType.INIT) return null;
        if (point.getCurMP() >= eff.getMP()) {
            if (eff.getMP() > 0) updateMp(-eff.getMP());
            isPowerSkill = true;
            switch (eff.getEffectType()) {
                case RANDOM: // create
                    int randSlot = NumberUtil.getRandomDif(5, shuriken.getSlot());
                    if (shuriken.getRank().value < RankType.DIVINE.value) {
                        return skillActive(getShurikenSlot(randSlot), direction, position, getShurikenSlot(randSlot).getDegree());
                    } else {
                        List<Bullet> bullets = new ArrayList<>();
                        int dir = direction.x >= 0 ? 1 : -1;
                        Pos pos = new Pos(position.x + BattleConfig.P_offsetXRow2 * dir, position.y + BattleConfig.P_offsetYColTop);
                        bullets.add(new Bullet(this, direction, pos.clone(), shuriken));
                        return bullets;
                    }
                case DECO:  // create // giảm mẹ chỉ số luôn cho nhanh, eff loằng ngoằng
                    List<Long> timeCd = new ArrayList<>();
                    for (int i = 0; i < weaponEquip.size(); i++) {
                        weaponEquip.get(i).decCoolDown(eff.getFirstPer());
                        timeCd.add(weaponEquip.get(i).getCoolDown());
                    }
                    // trả về effect cho client update time CD
                    protoStatus(StateType.UPDATE_COOL_DOWN, timeCd);
                    protoStatus(StateType.EFFECT_BODY, Arrays.asList(EffectBodyType.DECO.value, 1000L));
                    break;
                case RE_HP_INIT:  // create
                    Effect effectBody = new Effect(this, eff, faction);
                    addEffectTime(effectBody);
                    long reHp = (long) (eff.getValuePerIndex(2) * point.getMaxHp() + eff.getValuePerIndex(3) * point.getMagicDamage());
                    long reMax = (long) (BattleConfig.S_maxReHp50 / 100f * point.getMaxHp());
                    reHp = reHp > reMax ? reMax : reHp;
                    reHp(reHp);
//                    protoStatus(StateType.RE_HP, reHp);
                    break;
            }
        }
        return null;
    }

    //Fixme DATE: 7/31/2022 LƯU Ý ---> Xử lí các eff đang gắn trên bản thân - Sau 0.5s sẽ gọi 1 lần, dame mỗi giây sẽ được chia 2, point thì giữ nguyên
    public void processEffectByTime() {
        // - add Eff- check time - trừ time
        boolean hasRemove = false; // lười thêm vào list nên làm cách này cho tiện - có thể check theo effect type nhưng mà khi thêm eff lại phải thêm vào list => lười
//        if (teamId == 2) System.out.println("effectsBody.size() = " + effectsBody.size());
        for (int i = 0; i < effectsBody.size(); i++) {
            Effect eff = effectsBody.get(i);
            switch (eff.getSkill().getEffectType()) {
                case POISON: // Process in character
                    if (!eff.isActive()) {
//                        System.out.println("POISON = " + (eff.getTimeExits() * 1000));
                        protoStatus(StateType.EFFECT_BODY, Arrays.asList(EffectBodyType.POISON.value, (long) (eff.getTimeExits() * 1000)));
                        eff.active();
                        eff.setTimeRealActive();
                    }
                    if (eff.canActiveByAnim()) {
                        long mAtkPoison = (long) (eff.getSkill().getFirstPer() * eff.getOwner().getPoint().getMagicDamage());
                        beAttackEffect(eff, 0, mAtkPoison);
                    }
                    hasRemove = true;
                    break;
                case DEC_28: // Process in character
                    if (!eff.isActive()) { // trừ rồi thì thôi
                        eff.active();
                        eff.addRealBuff(point.buffChange(Point.CHANGE_DEFENSE, (long) -eff.getSkill().getFirstValues(), BattleConfig.S_maxReduce80));
                        eff.addRealBuff(point.buffChange(Point.CHANGE_MAGIC_RESIST, (long) -eff.getSkill().getValueIndex(1), BattleConfig.S_maxReduce80));
                    }
                    hasRemove = true;
                    break;
                case INF: // Process in character
//                case OGAMA_SKILL_1: // Process in character
                    long damageInf = (long) (eff.getSkill().getFirstPer() / 2f * eff.getOwner().getPoint().getMagicDamage());
                    beAttackEffect(eff, 0L, damageInf);
                    protoStatus(StateType.EFFECT_BODY, Arrays.asList(EffectBodyType.POISON.value, (long) (eff.getTimeExits() * 1000)));
                    // TODO: 31-08-2022   System.out.println("-------- Dame INF : " + damageInf);
                    hasRemove = true;
                    break;
                case HOA_THAN_NORMAL: // Process in character
                    long dameKagu1 = (long) (eff.getSkill().getFirstPer() / 2f * eff.getOwner().getPoint().getMagicDamage());
                    beAttackEffect(eff, 0L, dameKagu1);
                    protoStatus(StateType.EFFECT_BODY, Arrays.asList(EffectBodyType.DOT_FIRE.value, (long) (eff.getTimeExits() * 1000)));
                    beDot = true;
                    getBattleRoom().addCoroutine(new Coroutine(eff.getTimeExits(), () -> beDot = false));
                    // TODO: 31-08-2022   System.out.println("-------- Dame INF : " + damageInf);
                    hasRemove = true;
                    break;
                case SMOKE: // Process in character
                    if (!eff.isActive()) {
                        eff.active();
                        eff.addRealBuff(point.buffChange(Point.CHANGE_AGILITY, (long) eff.getSkill().getFirstValues(), BattleConfig.S_maxReduce80));
                        // buff def
                        long def = (long) eff.getSkill().getValueIndex(1);
                        protoBuffPoint(point.P_DEFENSE, def);
                        eff.addRealBuff(def);
                        // buff magic resist
//                        System.out.println("point.getMagicResist() 1= " + point.getMagicResist());
                        long magicRes = (long) eff.getSkill().getValueIndex(2);
                        protoBuffPoint(point.P_MAGIC_RESIST, magicRes);
                        eff.addRealBuff(magicRes);
//                        System.out.println("point.getMagicResist() 2= " + point.getMagicResist());
                    }
                    hasRemove = true;
                    break;
                case SANDSTORM: // Process in character
                    if (!eff.isActive()) {
                        // buff move speed
                        eff.addRealBuff(point.buffChange(Point.CHANGE_MOVE_SPEED, (long) -eff.getSkill().getValueIndex(1), BattleConfig.S_maxReduce90));
                        // buff magic resist
                        long addMagicResist = (long) -eff.getSkill().getValueIndex(2);
                        point.addMagicResist(addMagicResist);
                        eff.addRealBuff(addMagicResist);
                        protoBuffPoint(point.MAGIC_RESIST, addMagicResist);
                        eff.active();
                    }
                    hasRemove = true;
                    break;
                case DOT_FIRE: // Process in character
                    if (!eff.isActive()) {
                        eff.addRealBuff(point.buffChange(Point.CHANGE_HEATH, (long) -eff.getSkill().getFirstValues(), BattleConfig.S_maxReduce90));
                        eff.active();
                        protoStatus(StateType.EFFECT_BODY, Arrays.asList(EffectBodyType.DOT_FIRE.value, (long) (eff.getTimeExits() * 1000)));
                    }
                    hasRemove = true;
                    break;
                case BLIZZARD: // Process in character
                    if (!eff.isActive()) {
                        // buff move speed
                        eff.addRealBuff(point.buffChange(Point.CHANGE_MOVE_SPEED, (long) -eff.getSkill().getValueIndex(1), BattleConfig.S_maxReduce90));
//                        buffPoint(point.CHANGE_MOVE_SPEED, -eff.getFirstRealBuff());

                        long decDef = (long) -eff.getSkill().getValueIndex(2);
                        point.addDef(decDef);
                        eff.addRealBuff(decDef);
                        protoBuffPoint(point.DEFENSE, decDef);

                        // buff magic resist
                        long addMagicResist = (long) -eff.getSkill().getValueIndex(3);
                        point.addMagicResist(addMagicResist);
                        eff.addRealBuff(addMagicResist);
                        protoBuffPoint(point.MAGIC_RESIST, addMagicResist);
                        eff.active();
                    }
                    hasRemove = true;
                    break;
                case TOXIC: // Process in character
                    if (!eff.isActive()) {
                        getBattleRoom().addCharacterToxic(this);
                        protoStatus(StateType.EFFECT_BODY, Arrays.asList(EffectBodyType.TOXIC.value, (long) (BattleConfig.S_timePoinson * 1000)));
                        eff.active();
                        eff.setTimeRealActive();
                    }
                    if (eff.canActiveByAnim()) {
                        long dameMaxHp = (long) (BattleConfig.S_celiDameToxic * eff.getOwner().getPoint().getMagicDamage());
                        long dameHp = (long) (eff.getSkill().getNextPer() * point.getMaxHp());
                        dameHp = dameHp > dameMaxHp ? dameMaxHp : dameHp;
                        long magDame = (long) ((eff.getSkill().getFirstPer() * eff.getOwner().getPoint().getMagicDamage() + dameHp) / 2);
                        beAttackEffect(eff, 0L, magDame);
                    }
                    hasRemove = true;
                    break;
//                case RUNA: // Process in character
//                    if (!eff.isActive()) {
//                        eff.active();
//                        eff.getOwner().getPlayer().setRunaState(true);
//                        //System.out.println("ATTACK_SPEED TRUOC = " + point.getAttackSpeed());
//                        eff.addRealBuff(point.buffChange(Point.CHANGE_ATTACK_SPEED, (long) eff.getSkill().getFirstValues(), BattleConfig.S_maxReduce80));
////                        buffPoint(point.CHANGE_ATTACK_SPEED, eff.getFirstRealBuff());
//                        //System.out.println("ATTACK_SPEED SAU = " + point.getAttackSpeed());
//                        // crit
//                        //System.out.println("CRIT_DAMAGE TRUOC = " + point.getCritDamage());
//                        long addCritDame = (long) eff.getSkill().getValueIndex(1);
//                        point.addCritDamage(addCritDame);
//                        //System.out.println("addCritDame = " + addCritDame);
//                        eff.addRealBuff(addCritDame);
//                        protoBuffPoint(point.CRIT_DAMAGE, addCritDame);
//                        protoStatus(StateType.EFFECT_BODY, Arrays.asList(EffectBodyType.RUNA.value, (long) (eff.getTimeExits() * 1000)));
//                        System.out.println("CRIT_DAMAGE sau khi add = " + point.getCritDamage());
//                    }
//                    hasRemove = true;
//                    break;
                case SHIELD_FIRE: // Process in character
                    if (!eff.isActive()) {
                        eff.active();
                        //System.out.println("-------- SHIELD_FIRE truoc khi tru = " + point.getChangeDame());
                        eff.addRealBuff(eff.getOwner().getPoint().buffChange(Point.CHANGE_DAME, (long) -eff.getSkill().getFirstValues(), BattleConfig.S_maxReduce80));
//                        eff.getOwner().buffPoint(point.CHANGE_DAME, eff.getFirstRealBuff());
//                        System.out.println("eff.getTimeExits() = " + eff.getTimeExits());
                        eff.getOwner().protoStatus(StateType.EFFECT_BODY, Arrays.asList(EffectBodyType.SHIELD_FIRE.value, (long) (eff.getTimeExits() * 1000)));
                        //System.out.println("CRIT_DAMAGE SAU = " + point.getCritDamage());
                    }
                    hasRemove = true;
                    break;
                case RE_HP_INIT: // Process in character
                    long reHp = (long) (point.getMaxHp() * eff.getSkill().getFirstPer() + eff.getSkill().getNextPer() * point.getMagicDamage());
                    long reMaxHp = (long) (point.getMaxHp() * BattleConfig.S_maxReHp50 / 100f);
//                    System.out.println("RE_HP_INIT =================> " + reHp);
                    //System.out.println("id = " + id);
                    reHpBasic(reHp > reMaxHp ? reMaxHp / 2 : reHp / 2);
                    hasRemove = true;
                    break;
                case BURNED: // Process in character
                    long damage = (long) (eff.getSkill().getFirstPer() * eff.getOwner().point.getMagicDamage() + point.getMaxHp() * eff.getSkill().getNextPer());
                    long maxDameHp = (long) (eff.getOwner().getPoint().getMagicDamage() * BattleConfig.S_maxReHPBurned / 100f);
                    beAttackEffect(eff, 0, damage > maxDameHp ? maxDameHp / 2 : damage / 2);
                    if (!eff.isActive()) {
                        protoStatus(StateType.EFFECT_BODY, Arrays.asList(EffectBodyType.BURNED.value, (long) (eff.getTimeExits() * 1000)));
                        eff.active();
                    }
                    hasRemove = true;
                    break;
                case BOMB: // Process in character
                    if (!eff.isActive()) {
                        eff.active();
                        stun(eff);
                    }
                    hasRemove = true;
                    break;
//                case BERSERK: // Process in character
//                    if (!eff.isActive()) {
//                        eff.active();
//                        System.out.println("ATTACK SPEED 1111111  = " + point.getAttackSpeed());
//                        protoStatus(StateType.EFFECT_BODY, Arrays.asList(EffectBodyType.BERSERK.value, (long) (eff.getTimeExits() * 1000)));
//                        eff.addRealBuff(point.buffChange(Point.CHANGE_ATTACK_SPEED, (long) eff.getSkill().getFirstValues(), BattleConfig.S_maxReduce80));
//                        protoBuffPoint(point.CHANGE_ATTACK_SPEED, eff.getRealBuff(0));
//                        System.out.println("ATTACK SPEED 222222  = " + point.getAttackSpeed());
//
////                        System.out.println("ZEN_ATTACK truoc = " + point.getAttackDamage());
//                        long zenAttack = (long) eff.getSkill().getValueIndex(1);
//                        point.addZenAttack(zenAttack);
//                        eff.addRealBuff(zenAttack);
//                        protoBuffPoint(point.ZEN_ATTACK, zenAttack);
////                        System.out.println("ZEN_ATTACK sau = " + point.getAttackDamage());
//
//                        long zenMAttack = (long) eff.getSkill().getValueIndex(2);
////                        System.out.println("ZEN_MAGIC_ATTACK truoc khi add = " + point.getValues()[point.ZEN_MAGIC_ATTACK]);
//                        point.addZenMagicAttack(zenMAttack);
//                        eff.addRealBuff(zenMAttack);
//                        protoBuffPoint(point.ZEN_MAGIC_ATTACK, zenMAttack);
//                    }
//                    hasRemove = true;
//                    break;
                case THUY_THAN_1: // Process in character
                case THO_THAN_2: // Process in character
                    if (!eff.isActive()) {
                        stun(eff);
                        long atkSKill = (long) (eff.getSkill().getFirstPer() * eff.getOwner().point.getAttackDamage());
                        long magSKill = (long) (eff.getSkill().getNextPer() * eff.getOwner().point.getMagicDamage());
                        beAttackEffect(eff, atkSKill, magSKill);
                        eff.active();
                    }
                    hasRemove = true;
                    break;
                case HOA_THAN_2: // Process in character
                    long atkSKill = (long) (eff.getSkill().getFirstPer() * eff.getOwner().point.getAttackDamage() / 10f);
                    long magSKill = (long) (eff.getSkill().getNextPer() * eff.getOwner().point.getMagicDamage() / 10f);
                    beAttackEffect(eff, atkSKill, magSKill);
                    protoStatus(StateType.EFFECT_BODY, Arrays.asList(EffectBodyType.DOT_FIRE.value, (long) (eff.getTimeExits() * 1000)));
                    beDot = true;
                    getBattleRoom().addCoroutine(new Coroutine(eff.getTimeExits(), () -> beDot = false));
                    hasRemove = true;
                    break;
//                case KAGU_SKILL_3: // Process in character
//                    if (!eff.isActive()) {
//                        point.stun();
//                        protoStatus(StateType.EFFECT_BODY, Arrays.asList(EffectBodyType.STUN.value, (long) (eff.getTimeExits() * 1000)));
//                        eff.active();
//                    }
            }

            //Fixme DATE: 7/31/2022 LƯU Ý ---> Trả lại chỉ số + Remove Eff
            boolean checkExits = eff.checkExist(CfgBattle.decTimeEff);
//            System.out.println("checkExits = " + checkExits);
            if (eff.canActiveByAnim() && !checkExits && hasRemove) {
                switch (eff.getSkill().getEffectType()) {
//                    case DEC_SPEED: // Remove from character
//                        point.buffChange(Point.CHANGE_MOVE_SPEED, -eff.getFirstRealBuff(), BattleConfig.S_maxReduce90);
//                        // TODO: 31-08-2022     System.out.println("move speed tra lai = " + point.getMoveSpeed());
//                        break;
//                    case DEC_DEF: // Remove from character
//                        point.addDef(-eff.getFirstRealBuff());
//                        protoBuffPoint(Point.DEFENSE, -eff.getFirstRealBuff());
//                        // TODO: 31-08-2022     System.out.println("DEFENSE trả lại = " + point.getDefense());
//                        break;
                    case SMOKE: // Remove from character
                        point.buffChange(Point.CHANGE_AGILITY, -eff.getFirstRealBuff(), BattleConfig.S_maxReduce80);
                        // def
                        protoBuffPoint(Point.P_DEFENSE, -eff.getRealBuff(1));
                        // magic res
                        protoBuffPoint(Point.P_MAGIC_RESIST, -eff.getRealBuff(2));
                        break;
                    case DEC_28: // Remove from character
                        point.buffChange(Point.CHANGE_DEFENSE, -eff.getFirstRealBuff(), BattleConfig.S_maxReduce80);
//                        buffPoint(Point.CHANGE_DEFENSE, -eff.getFirstRealBuff());
                        // TODO: 31-08-2022     System.out.println("DEFENSE  remove ===== " + point.getDefense());

                        point.buffChange(Point.CHANGE_MAGIC_RESIST, -eff.getRealBuff(1), BattleConfig.S_maxReduce80);
//                        buffPoint(Point.CHANGE_MAGIC_RESIST, -eff.getRealBuff(1));
                        // TODO: 31-08-2022      System.out.println("MAGIC_RESIST  remove ===== " + point.getMagicResist());
                        break;
//                    case PARALYZE: // Remove from character
//                        //System.out.println("Remove BLOCK_PARALYZE = " + System.currentTimeMillis());
//                        point.getValues()[Point.BLOCK_PARALYZE] = 0;
//                        break;
//                    case RUNA: // Remove from character
//                        eff.getOwner().getPlayer().setRunaState(false);
//                        point.buffChange(Point.CHANGE_ATTACK_SPEED, -eff.getFirstRealBuff(), BattleConfig.S_maxReduce80);
////                        buffPoint(Point.CHANGE_ATTACK_SPEED, -eff.getFirstRealBuff());
//                        //System.out.println("ATTACK_SPEED sau khi remove = " + point.getAttackSpeed());
//                        point.addCritDamage(-eff.getRealBuff(1));
//                        protoBuffPoint(Point.CRIT_DAMAGE, -eff.getRealBuff(1));
//                        //System.out.println("CRIT_DAMAGE sau khi remove = " + point.getCritDamage());
//                        break;
                    case SHIELD_FIRE: // Remove from character
                        //System.out.println("eff.getFirstRealBuff() = " + (-eff.getFirstRealBuff()));
                        eff.getOwner().point.buffChange(Point.CHANGE_DAME, -eff.getFirstRealBuff(), BattleConfig.S_maxReduce80);
//                        eff.getOwner().buffPoint(Point.CHANGE_DAME, -eff.getFirstRealBuff());
                        //System.out.println("CHANGE_DAME sau khi remove = " + point.getChangeDame());
                        break;
//                    case BERSERK: // Remove from character
//                        point.buffChange(Point.CHANGE_ATTACK_SPEED, -eff.getFirstRealBuff(), BattleConfig.S_maxReduce80);
//                        System.out.println("ATTACK SPEED 333333333 = " + point.getAttackSpeed());
//                        protoBuffPoint(Point.ATTACK_SPEED, (long) (point.getAttackSpeed() * 100));
//                        point.addZenAttack(-eff.getRealBuff(1));
//                        protoBuffPoint(Point.ZEN_ATTACK, -eff.getRealBuff(1));
////                        System.out.println("ZEN_ATTACK sau khi remove = " + point.getAttackDamage());
//                        point.addZenMagicAttack(-eff.getRealBuff(2));
//                        protoBuffPoint(Point.ZEN_MAGIC_ATTACK, -eff.getRealBuff(2));
////                        System.out.println("ZEN_MAGIC_ATTACK sau khi remove  = " + point.getValues()[point.ZEN_MAGIC_ATTACK]);
//                        break;
//                    case TOXIC: // Remove from character
//                        f0Toxic.clear();
//                        break;
                    case SANDSTORM: // Remove from character
                        point.buffChange(Point.CHANGE_MOVE_SPEED, -eff.getRealBuff(0), BattleConfig.S_maxReduce90);
//                        buffPoint(Point.CHANGE_MOVE_SPEED, -eff.getRealBuff(0));

                        point.addMagicResist(-eff.getRealBuff(1));
                        protoBuffPoint(Point.MAGIC_RESIST, -eff.getRealBuff(1));
                        break;
                    case DOT_FIRE: // Remove from character
                        point.buffChange(Point.CHANGE_HEATH, -eff.getRealBuff(0), BattleConfig.S_maxReduce90);
                        // add dame
                        long magDame = (long) (eff.getSkill().getNextPer() * eff.getOwner().getPoint().getMagicDamage());
                        beAttackEffect(eff, 0L, magDame);
                        break;
                    case BLIZZARD: // Remove from character
                        point.buffChange(Point.CHANGE_MOVE_SPEED, -eff.getRealBuff(0), BattleConfig.S_maxReduce90);
//                        buffPoint(Point.CHANGE_MOVE_SPEED, -eff.getRealBuff(0));

                        point.addDef(-eff.getRealBuff(1));
                        protoBuffPoint(Point.DEFENSE, -eff.getRealBuff(1));

                        point.addMagicResist(-eff.getRealBuff(2));
                        protoBuffPoint(Point.MAGIC_RESIST, -eff.getRealBuff(2));
                        break;
                }
                removeEffect(eff);
            }
        }
    }

    @Override
    public void Update1s() { //1s update 1 lần
        if (room == null || room.getRoomState() != RoomState.ACTIVE) return;
        if (alive) {
            processEffectByTime();
            long reHp = point.getHpRegen();
            if (reHp > 0 && point.getCurHP() < point.getMaxHp()) reHpNoEff(reHp);
            long reMp = point.getMpRegen();
            if (reMp > 0 && point.getCurMP() < point.getMaxMp()) updateMp(reMp);
        }
    }

    public boolean sameTeam(Character other) {
        return other.getTeamId() == this.teamId;
    }

    public long getTimeAttack(int attackerId) {
        if (attackerInfo.containsKey(attackerId)) { // chưa có thông tin gì
            return attackerInfo.get(attackerId).get(TIME_ATTACK);
        } else {
            return 0;
        }
    }

    public Effect getToxic() {
        List<Effect> effToxic = effectsBody.stream().filter(effect1 -> effect1.getSkill().getEffectType() == EffectType.TOXIC).collect(Collectors.toList());
        return effToxic.size() > 0 ? effToxic.get(0) : null;
    }

    public void addEffectTime(Effect effect) { // k xử lí các effect point dame,magicDame
        if (effect.getSkill().getEffectType().isIncreBody || effectsBody.stream().filter(effect1 -> effect1 != null && effect1.getSkill().getEffectType().id == effect.getSkill().getEffectType().id).count() == 0) { // cộng dồn trên character
            effectsBody.add(effect);
        }
    }

    public void addEffectNow(Effect eff) { // xử lí trực tiếp luôn
        // check có đc xử lí k
        if (!eff.getSkill().getEffectType().isIncreBody || effectsBody.stream().filter(efx -> efx.getSkill().getEffectType().id == eff.getSkill().getEffectType().id).count() == 0) { // cộng dồn trên character và chưa có
            effectsBody.add(eff);
        } else return;
        // chắc chắn đc xử lí
//        System.out.println("eff.getSkill().getEffectType() xxxxxxxxx= " + eff.getSkill().getEffectType());
        switch (eff.getSkill().getEffectType()) {
            case KIM_THAN_SKILL_1 -> {// Process in character
                stun(eff.getTimeExits());
                long atkSKill = (long) (eff.getSkill().getFirstPer() * eff.getOwner().point.getAttackDamage());
                long magSKill = (long) (eff.getSkill().getNextPer() * eff.getOwner().point.getMagicDamage());
                beAttackEffect(eff, atkSKill, magSKill);
                removeEffect(eff);
            }
            case PARALYZE -> {
                int random = NumberUtil.getRandom(1000);
                if (random < eff.getSkill().getFirstValues() * 10) { // dính đòn, gádsn effect vào
                    stun(eff);
                    protoStatus(StateType.EFFECT, eff.toStateOne(pos));
                    removeEffect(eff);
                }
            }
            case DEC_DEF -> {
                eff.addRealBuff(point.buffChange(Point.CHANGE_DEFENSE, (long) -eff.getSkill().getFirstValues(), BattleConfig.S_maxReduce80));
                protoStatus(StateType.EFFECT_BODY, Arrays.asList(EffectBodyType.BROKEN_SHIELD.value, (long) (eff.getTimeExits() * 1000)));
                room.addCoroutine(new Coroutine(eff.getTimeExits(), () -> {
                    protoBuffPoint(Point.CHANGE_DEFENSE, -eff.getFirstRealBuff());
                    removeEffect(eff);
                }));
            }
            case DEC_DEF2 -> {
                eff.addRealBuff(point.buffChange(Point.CHANGE_DEFENSE, (long) -eff.getSkill().getFirstValues(), BattleConfig.S_maxReduce80));
                protoStatus(StateType.EFFECT_BODY, Arrays.asList(EffectBodyType.BROKEN_SHIELD.value, (long) (eff.getTimeExits() * 1000)));
                protoStatus(StateType.EFFECT, eff.toStateOne(pos));
                room.addCoroutine(new Coroutine(eff.getTimeExits(), () -> {
                    protoBuffPoint(Point.CHANGE_DEFENSE, -eff.getFirstRealBuff());
                    removeEffect(eff);
                }));
            }
            case DEC_SPEED -> {
                eff.addRealBuff(point.buffChange(Point.CHANGE_MOVE_SPEED, (long) -eff.getSkill().getFirstValues(), BattleConfig.S_maxReduce90));
                protoStatus(StateType.EFFECT_BODY, Arrays.asList(EffectBodyType.SLOW.value, (long) (eff.getTimeExits() * 1000)));
                room.addCoroutine(new Coroutine(eff.getTimeExits(), () -> {
                    point.buffChange(Point.CHANGE_MOVE_SPEED, -eff.getFirstRealBuff(), BattleConfig.S_maxReduce90);
                    removeEffect(eff);
                }));
            }
            case RUNA -> { //process character
                protoStatus(StateType.EFFECT_BODY, Arrays.asList(EffectBodyType.RUNA.value, (long) (eff.getTimeExits() * 1000)));
                eff.getOwner().setRunaState(50);
                eff.addRealBuff(point.buffChange(Point.CHANGE_ATTACK_SPEED, (long) -eff.getSkill().getFirstValues(), BattleConfig.S_maxReduce80));
                protoUpdatePoint(Point.CHANGE_ATTACK_SPEED, point.getChangeAttackSpeed());
                // crit
                long addCritDame = (long) eff.getSkill().getValueIndex(1);
                eff.addRealBuff(addCritDame);
                protoBuffPoint(Point.CRIT_DAMAGE, addCritDame);
                room.addCoroutine(new Coroutine(eff.getTimeExits(), () -> {
                    eff.getOwner().disableStateRuna();
                    point.buffChange(Point.CHANGE_ATTACK_SPEED, -eff.getFirstRealBuff(), BattleConfig.S_maxReduce80);
                    protoUpdatePoint(Point.CHANGE_ATTACK_SPEED, point.getChangeAttackSpeed());
                    protoBuffPoint(Point.CRIT_DAMAGE, -eff.getRealBuff(1));
                    removeEffect(eff);
                }));
            }
            case BERSERK -> { //process character
                protoStatus(StateType.EFFECT_BODY, Arrays.asList(EffectBodyType.BERSERK.value, (long) (eff.getTimeExits() * 1000)));
                long buffPer = point.buffChange(Point.CHANGE_ATTACK_SPEED, (long) -eff.getSkill().getFirstValues(), BattleConfig.S_maxReduce80);
                eff.addRealBuff(buffPer);
                protoUpdatePoint(Point.CHANGE_ATTACK_SPEED, eff.getRealBuff(0));

//                        System.out.println("ZEN_ATTACK truoc = " + point.getAttackDamage());
                long zenAttack = (long) eff.getSkill().getValueIndex(1);
                eff.addRealBuff(zenAttack);
                protoBuffPoint(Point.ZEN_ATTACK, zenAttack);
//                        System.out.println("ZEN_ATTACK sau = " + point.getAttackDamage());

                long zenMAttack = (long) eff.getSkill().getValueIndex(2);
//                        System.out.println("ZEN_MAGIC_ATTACK truoc khi add = " + point.getValues()[point.ZEN_MAGIC_ATTACK]);
                eff.addRealBuff(zenMAttack);
                protoBuffPoint(Point.ZEN_MAGIC_ATTACK, zenMAttack);
                room.addCoroutine(new Coroutine(eff.getTimeExits(), () -> {
                    point.buffChange(Point.CHANGE_ATTACK_SPEED, -eff.getFirstRealBuff(), BattleConfig.S_maxReduce80);
                    protoUpdatePoint(Point.CHANGE_ATTACK_SPEED, point.get(Point.CHANGE_ATTACK_SPEED));
                    point.addZenAttack(-eff.getRealBuff(1));
                    protoBuffPoint(Point.ZEN_ATTACK, -eff.getRealBuff(1));
//                        System.out.println("ZEN_ATTACK sau khi remove = " + point.getAttackDamage());
                    point.addZenMagicAttack(-eff.getRealBuff(2));
                    protoBuffPoint(Point.ZEN_MAGIC_ATTACK, -eff.getRealBuff(2));
//                        System.out.println("ZEN_MAGIC_ATTACK sau khi remove  = " + point.getValues()[point.ZEN_MAGIC_ATTACK]);
                    removeEffect(eff);
                }));
            }
        }
    }

    public void setRunaState(long per) {
        weaponEquip.forEach(shuriken -> {
            shuriken.setStateRuna(per);
        });
    }

    public void stun(float time) {
        long timStun = (long) (time * 1000);
        point.addStun(timStun);
        protoStatus(StateType.UPDATE_MULTI_POINT, 2, List.of((long) Point.STUN, timStun));
    }

    public void stun(Effect effect) {
        stun(effect.getTimeExits());
    }

    public void removeEffect(Effect effect) {
        effectsBody.remove(effect);
    }

    public void setDirection(Pos direction) {
        if (beBlock()) return;
        this.direction = direction;
    }

    public void move(Pos newPos) {
        if (beBlock()) return;
        pos.v_add(panelMap, newPos);
        setMove(true);
    }

    public void setMove(boolean isMove) {
        if (isMove) timeActionMove = System.currentTimeMillis();
        this.isMove = isMove;
    }

    public boolean inSizeHitBullet(Bullet bullet) {
        return MathLab.pointInCircle(this.pos, radius + bullet.getRadius(), bullet.getPos());
    }

    public boolean inSizeHit(Pos posTarget, float r) {
        return MathLab.pointInCircle(this.pos, r, posTarget);
    }

    public boolean isHitMelee(Character target) {
        if (!isAlive()) return false;
        return MathLab.pointInCircle(this.pos, target.getRadius() + radius, target.pos);
    }

    public float distionTop() {
        return Math.abs(room.getMapInfo().getMapData().getTopRight().y - pos.y);
    }

    public float distionBot() {
        return Math.abs(room.getMapInfo().getMapData().getBotLeft().y - pos.y);
    }

    public float distionLeft() {
        return Math.abs(room.getMapInfo().getMapData().getBotLeft().x - pos.x);
    }

    public float distionRight() {
        return Math.abs(room.getMapInfo().getMapData().getTopRight().x - pos.x);
    }

    public boolean isLikeFace(Pos newDirection) { // check lật mặt
        return direction.x * newDirection.x > 0;
    }


    public Point resetData() {
        point.resetHpMp();
        alive = true;
        attackerInfo = new HashMap<>();
        effectsBody = new ArrayList<>();
//        f0Toxic = new ArrayList<>();
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
        pbUser.setSpeed((int) point.getMoveSpeed());
        return pbUser.build();
    }

    public abstract Pbmethod.PbUnitAdd.Builder toProtoAdd();

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

    public Pbmethod.PbUnitAdd.Builder toProtoRemove() {
        Pbmethod.PbUnitAdd.Builder builder = Pbmethod.PbUnitAdd.newBuilder();
        if (isPlayer()) builder.setType(Constans.TYPE_PLAYER);
        else builder.setType(Constans.TYPE_MONSTER);
        builder.setId(id);
        builder.setIsAdd(false);
        return builder;
    }

    public synchronized void protoDie(Character killer) {
        unTargetAll();
        this.killById = killer.getId();
        room.characterDie(this);
    }

    public void updateHp(Character attacker, long atkDame, long magDame) {
        List<PointBuff> buffs = new ArrayList<>();
        buffs.add(new PointBuff(Point.CUR_HP, atkDame + magDame));
        protoBuffPoint(buffs);
        ((BaseBattleRoom) room).ChangeCharacterHp(attacker, this, atkDame, magDame);
    }

    public void reHp(long addNum) {
        protoStatus(StateType.EFFECT_BODY, EffectBodyType.HEALING.value, 0L);
        protoStatus(StateType.RE_HP, addNum);
        List<PointBuff> buffs = new ArrayList<>();
        buffs.add(new PointBuff(Point.CUR_HP, addNum));
        protoBuffPoint(buffs);
    }

    public void buffShell(long addNum) {
        List<PointBuff> buffs = new ArrayList<>();
        buffs.add(new PointBuff(Point.SHELL, addNum));
        protoBuffPoint(buffs);
    }

    public void reHpNoEff(long addNum) {
        List<PointBuff> buffs = new ArrayList<>();
        buffs.add(new PointBuff(Point.CUR_HP, addNum));
        protoBuffPoint(buffs);
    }

    public void reHpBasic(long addNum) {
        protoStatus(StateType.EFFECT_BODY, EffectBodyType.HEALING_BASIC.value, 0L);
        List<PointBuff> buffs = new ArrayList<>();
        buffs.add(new PointBuff(Point.CUR_HP, addNum));
        protoBuffPoint(buffs);
    }

    public void updateMp(long addMp) {
        List<PointBuff> buffs = new ArrayList<>();
        buffs.add(new PointBuff(Point.CUR_MP, addMp));
        protoBuffPoint(buffs);
    }

    public void protoBuffPoint(int pointId, long addValue) { // bao gồm add point và trả vè - chỉ dùng cho các point add thưởng, point per phải làm khác
        List<PointBuff> buffs = new ArrayList<>();
        buffs.add(new PointBuff(pointId, addValue));
        protoBuffPoint(buffs);
    }

    public void protoMultiPoint(List<Long> points) {  //[pointId - curValue] :  chỉ trả về, thường dùng cho add change
        protoStatus(StateType.UPDATE_MULTI_POINT, points.size(), points);
    }

    public void protoUpdatePoint(int pointId,long value) {  //[pointId - curValue] :  chỉ trả về, thường dùng cho add change
        protoStatus(StateType.UPDATE_MULTI_POINT, 2,Arrays.asList( (long) pointId,value));
    }

    // buff point and send
    public synchronized void protoBuffPoint(List<PointBuff> buffs) {
        if (!alive) return;
        List<Long> pointBuff = new ArrayList<>();
        for (int i = 0; i < buffs.size(); i++) {
            int pointId = buffs.get(i).getPointId();
            long value = buffs.get(i).getValue();
            switch (pointId) {
                case Point.BUFF_CUR_PER_HP -> {
                    long reMax = (long) (value / 100f * point.getMaxHp());
                    point.addCurHp(reMax);
                    pointBuff.add((long) Point.CUR_HP);
                    pointBuff.add(point.getCurHP());
                }
                case Point.BUFF_CUR_PER_MP -> {
                    long reMax = (long) (value / 100f * point.getMaxMp());
                    point.addCurMp(reMax);
                    pointBuff.add((long) Point.CUR_MP);
                    pointBuff.add(point.getCurMP());
                }
                case Point.CUR_HP -> {
                    long shell = point.getCurShell();
                    if (shell > 0 && value < 0) {
                        shell += value;
                        point.setShell(shell > 0 ? shell : 0);
                        // trừ giáp trước, nếu hết giáp mới trừ máu
                        pointBuff.add((long) Point.SHELL);
                        pointBuff.add(point.getCurShell());
                        // dame > shell
                        if (value + shell < 0) {
                            buffs.get(i).setValue(value + shell);
                            point.add(buffs.get(i), this);
                            pointBuff.add((long) Point.CUR_HP);
                            pointBuff.add(point.getCurHP());
                        }
                    } else {
                        point.add(buffs.get(i), this);
                        pointBuff.add((long) Point.CUR_HP);
                        pointBuff.add(point.getCurHP());
                    }
                }
                default -> {
                    point.add(buffs.get(i), this);
                    pointBuff.add((long) pointId);
                    pointBuff.add(point.get(pointId));
                }
            }
        }
        protoStatus(StateType.UPDATE_MULTI_POINT, pointBuff.size(), pointBuff);
    }


    public void bonusKillMe(Character killer) {

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

    public abstract void activeSkill(int skillId);

    public boolean attackBlockMove() {
        return !DateTime.isAfterTime(timeActionAttack, BattleConfig.P_attackBlockMove);
    }

    public boolean isHit() {
        return false;
    }

//    public void protoPush(Bullet bullet) {
//        if (point.getWeight() == -1) return;
//        if (!hasPush(bullet.getOwner().getId())) return;
//        float force = bullet.getForcePush() * 10f;
//        float a = force / point.getWeight();
//        float s = (float) (a / 2f * Math.pow(BattleConfig.M_timeBeHit, 2));
//        if (s > 0) {
//            if (isLikeFace(bullet.getDirection())) {
//                setDirection(bullet.getDirection().oppositeDirection().normalized());
//            }
//            this.timeBeHit = System.currentTimeMillis();
//            // tính toán ra pos mới

    /// /            Pos push = IMath.calculatePushPos(bullet.getDirection().normalized(), s);
    /// /            pos.v_add(getBattleRoom(), push);
    /// /            protoStatus(StateType.PUSH, (long) (pos.x * 1000), (long) (pos.y * 1000), (long) (getDirection().x * 1000L), (long) (getDirection().y * 1000L));
//        }
//    }
    public void setTimeRandomMove() { //  vừa randomm move xong, time này dùng để xác định thời gian cho phép random move tiếp theo
        timeActiveRandomMove = System.currentTimeMillis() + NumberUtil.getRandom((int) (BattleConfig.M_delayMove * 1000));
    }

    // region proto
    public void protoBeDame(Character attacker, List<Long> aInfo) {
        if (type == CharacterType.BOSS_GOD) {
            if (!beDameInfo.containsKey(attacker.getId())) beDameInfo.put(attacker.getId(), 0L);
            beDameInfo.put(attacker.getId(), beDameInfo.get(attacker.getId()) + aInfo.get(2) + aInfo.get(3));
        }
        protoStatus(List.of(StateType.BE_DAMAGE), aInfo);
    }

    public void protoBeDameEffect(List<Long> aInfo) { // size 3
        protoStatus(List.of(StateType.EFFECT_DAME), aInfo);
    }

    public void protoStatus(StateType status) {
        if (room != null) room.getAProtoUnitState().add(protoState(List.of(status), new ArrayList<>()));
    }

    public void protoStatus(StateType status, Long... info) {
        if (room != null) room.getAProtoUnitState().add(protoState(List.of(status), Arrays.asList(info)));
    }

    public void protoStatus(StateType status, Long id, Long size, List<Long> info) {
        if (room != null) room.getAProtoUnitState().add(protoState(List.of(status), info));
    }

    public void protoStatus(StateType status, List<Long> info) {
        if (room != null) room.getAProtoUnitState().add(protoState(List.of(status), info));
    }


    public void protoRangeDame(Character attacker, List<Long> aInfo) {
        if (type == CharacterType.BOSS_GOD) {
            if (!beDameInfo.containsKey(attacker.getId())) beDameInfo.put(attacker.getId(), 0L);
            beDameInfo.put(attacker.getId(), beDameInfo.get(attacker.getId()) + aInfo.get(2) + aInfo.get(3));
        }
        protoStatus(List.of(StateType.RANGE_DAMAGE), aInfo);
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

    Pbmethod.PbUnitState.Builder protoState(List<StateType> aStatus, List<Long> aInfo) {
        Pbmethod.PbUnitState.Builder builder = Pbmethod.PbUnitState.newBuilder();
        builder.setId(id);
        for (int i = 0; i < aStatus.size(); i++) {
            builder.addStatus(aStatus.get(i).id);
            builder.addStatus(aStatus.get(i).length);
        }
        if (aInfo == null) aInfo = new ArrayList<>();
        builder.addAllPoint(aInfo);
        return builder;
    }

    Pbmethod.PbUnitState.Builder protoState(List<StateType> aStatus, List<Integer> size, List<Long> aInfo) {
        Pbmethod.PbUnitState.Builder builder = Pbmethod.PbUnitState.newBuilder();
        builder.setId(id);
        for (int i = 0; i < aStatus.size(); i++) {
            builder.addStatus(aStatus.get(i).id);
            builder.addStatus(size.get(i));
        }
        if (aInfo == null) aInfo = new ArrayList<>();
        builder.addAllPoint(aInfo);
        return builder;
    }
    // endregion
}
