package game.battle.model;

import game.battle.calculate.IMath;
import game.battle.object.*;
import game.battle.type.AttackType;
import game.battle.type.AutoMode;
import game.battle.type.CharacterType;
import game.battle.type.StateType;
import game.config.aEnum.*;
import game.dragonhero.BattleConfig;
import game.dragonhero.mapping.*;
import game.dragonhero.mapping.main.BaseMap;
import game.dragonhero.server.Constans;
import game.dragonhero.service.resource.ResMap;
import game.dragonhero.table.BaseRoom;
import game.object.MyUser;
import lombok.Data;
import ozudo.base.helper.DateTime;
import protocol.Pbmethod;

import java.io.Serializable;
import java.util.*;

@Data
public class BotPlayer extends Character implements Serializable {
    List<NInput> inputs = new ArrayList<>();
    List<NInput> inputsNew = new ArrayList<>();
    long indexLastInputSeq = -1;
    long timeLastProcessInput;
    // auto
    AutoMode autoMode = AutoMode.MOVE_ATTACK;
    int skillSlotNext;
    UserHeroEntity uHero;
    long timeLastAction;
    long timeRunHit, timeAttackRun;
    Pos targetDirectionAttackRun;
    int curSkill = NInput.Skill1;
    Pos directionHitRun = Pos.zero();
    List<Integer> itemsBuf; // trigger - itemId
    List<Long> timeBuff; // list time buff theo slot
    //ping logic
    long curTick = 0;
    int poolSizeTick = 5;
    List<Long> buffs = Arrays.asList(0L, 0L, 0L); // size =3 drop - gold - exp : per 100
    public List<Integer> listTick = new ArrayList<>(); // size =5; // test
    //buf
    float cacheRangeAttack;
    // analysis
    int timeDelayActive;
    Player leader;
    MyUser mUser;

    public BotPlayer(MyUser mUser, int botId, int teamId, UserHeroEntity hero, Pos posInit, int timeDelayActive) {
        initDefault(botId, teamId, hero.calPointHero(mUser, IMath.calculatePoint(mUser, false)));
        this.name = hero.getRes().getName();
        this.uHero = hero;
        this.timeDelayActive= timeDelayActive;
        this.mUser = mUser;
        this.autoMode = AutoMode.MOVE_ATTACK;
        this.type = CharacterType.BOT_PLAYER;
        List<UserWeaponEntity> wes = mUser.getResources().getWeaponEquip();
        for (int i = 0; i < wes.size(); i++) {
            Shuriken shu = new Shuriken(point, wes.get(i), i);
            weaponEquip.add(shu);
        }
        this.leader = mUser.getPlayer();
        clearDataForChangeRoom(posInit);
    }

    private void initDefault(int id, int teamId, Point point) {
        this.id = id;
        this.teamId = teamId;
        this.radius = BattleConfig.C_Collider;
        this.rangeAttack = BattleConfig.P_RangeAttack +1f;
        this.indexLastInputSeq = -1;
        this.alive = true;
        this.point = point;
        point.add(Point.P_HP,300);
        point.add(Point.MP,100000);
        this.point.resetHpMp();

        this.attackType = AttackType.LONG_RANGE;
        this.direction = Pos.right();
        this.isMove = false;
        this.attackerInfo = new HashMap<>();
        this.effectsBody = new ArrayList<>();
        this.skillSlotNext = 0;
        weaponEquip = new ArrayList<>();
        timeBuff = new ArrayList<>();
    }


    public void clearDataForChangeRoom(Pos... instancePos) {
        clearDataNoCachePos(instancePos);
    }

    public void clearDataNoCachePos(Pos... instancePos) {
        point.initDefault();
        this.ready = false;
        this.pos = instancePos.length > 0 ? instancePos[0] : Pos.zero();
        this.indexLastInputSeq = -1;
        skillSlotNext = 0;
        timeBeHit = 0;
        beDameInfo = new HashMap<>();
        targetMove = Pos.zero();
        effectsBody = new ArrayList<>();
        timePush = new HashMap<>();
        attackerInfo = new HashMap<>();
    }

    // set time join, va clear data old map
    public void setJoinMap(BaseRoom room) {
        timeJoinRoom = System.currentTimeMillis();
        // clear old data
        targetAttack = null;
        //timeBeAttack = 0;
        targetMove = null;
        this.ready = true;
        directionMoveAttack = Pos.zero();
        this.room = room;
        this.panelMap = new PanelMap(room.getMapInfo().getMapData());
        for (int i = 0; i < weaponEquip.size(); i++) {
            weaponEquip.get(i).resetJoinMap();
        }
    }


    public void disableStateRuna() {
        weaponEquip.forEach(shuriken -> {
            shuriken.setRunaState(false);
        });
    }

    public int getNextSkillId() {
        for (int i = 0; i < weaponEquip.size(); i++) {
            if (weaponEquip.get(i).hasActiveSkill()) {
                nextSkillSlot();
                return skillSlotNext;
            }
        }
        return -1;
    }

    void nextSkillSlot() {
        skillSlotNext++;
        if (skillSlotNext > 4) skillSlotNext = 0;
    }

    public void activeSkill2(int skillIndex,Pos direction) {
        setTimeAttack();
        Shuriken shu = weaponEquip.get(skillIndex);
        shu.setActiveSkill();
        protoStatus(StateType.USE_SKILL, (long) (skillIndex), shu.getTimeActiveSkill() - System.currentTimeMillis(), shu.getNumberAttack(), (long) (direction.x * 1000L), (long) (direction.y * 1000L), isPowerSkill ? 1L : 0L);
    }


    public boolean hasActiveSkill() {  // delay giữa 2 lần đánh
        return DateTime.isAfterTime(timeActionAttack, point.getAttackSpeed());
    }

    @Override
    public boolean isReviveReady() {
        return DateTime.isAfterTime(timeRevive, BattleConfig.P_timeImmortal);
    }

    // code đảo chiều dựa theo 2 điều kiện - dùng trong vòng lặp thời gian set liên tục
    public void setTimeAttackRun(Pos target) {
        if (!isAttackRunDone() || target ==null) return;
        this.timeAttackRun = System.currentTimeMillis();
        this.targetDirectionAttackRun = pos.getRandDiectionTo(target, 40f);
    }

    public boolean isAttackRunDone() {
        return DateTime.isAfterTime(timeAttackRun, BattleConfig.P_attackRun2);
    }

    public boolean targetInSizeAttack() {
        if (targetAttack == null) return false;
        return pos.distance(targetAttack.pos) < rangeAttack;
    }

    @Override
    public boolean isReady() {
        return DateTime.isAfterTime(timeJoinRoom, BattleConfig.P_delayReady) && ready;
    }

    public boolean isMove() {
        this.isMove = !DateTime.isAfterTime(timeActionMove, 1.5f);
        return isMove;
    }

    public void setTargetAttack(Character attacker) {
        this.targetAttack = attacker;
        if (attacker != null) {
            protoOneStatus(StateType.SWITCH_TARGET, (long) attacker.id);
        }
    }

    public Pos findEnemyNearest() {
        if (room.getAEnemy().isEmpty()) return Pos.zero();
        if (targetAttack != null && targetAttack.isAlive()) return targetAttack.getPos();
        float min = 999999f;
        Pos ret = Pos.zero();
        Character target = null;
        for (int i = 0; i < room.getAEnemy().size(); i++) {
            if (room.getAEnemy().get(i).isAlive() && min > getPos().distance(room.getAEnemy().get(i).getPos()) && room.getAEnemy().get(i).isReady()) {
                min = (float) getPos().distance(room.getAEnemy().get(i).getPos());
                target = room.getAEnemy().get(i);
                ret = room.getAEnemy().get(i).getPos();
            }
        }
        setTargetAttack(target);
        return ret;
    }

    public void processAuto() {
        if(room==null || !alive || beBlock() && !DateTime.isAfterTime(timeJoinRoom,timeDelayActive) ) return ;
        Pos target = findEnemyNearest();
        Pos targetDirection = null;
        if (!target.equals(Pos.zero())) {
            if (targetInSizeAttack() ) {
                if(!isAttackRunDone()) return;
                int skillIndex = getNextSkillId();
                if (skillIndex != -1 && hasActiveSkill() && !isMove()) {
                    targetDirection = getFutureDirection(targetAttack);
                    activeSkill2(skillIndex,targetDirection);
                    getBattleRoom().addBullet(this, skillIndex, activeSkillByDirection(skillIndex, targetDirection.normalized()));
                }
            } else {
                if (!isAttackRunDone()) { // chua move xong thi move tiep
                    if ( attackDone()) {
                        setTimeAttackRun(targetDirection);
                        Pos nd = Pos.moveFromDirection(targetDirectionAttackRun, getCurSpeed());
                        direction = nd.normalized();
                        move(direction);
                    }
                } else {
                    // ngoai tam danh thi move
                    if ( isAttackRunDone()) {
                        setTimeAttackRun(target);
                        direction = pos.getDirectionTo(target).normalized();
                        move(direction);
                    }
                }
            }
        }
    }

    Pos getFutureDirection(Character target) {
        if (target.isMove()) {
            float timeMove = (float) target.getPos().distance(pos);
            Pos posNext = target.pos.clone();
            Pos dirClone = target.direction.clone();
            dirClone.multiple(target.getCurSpeed() * timeMove);
            posNext.add(dirClone);
            return pos.getDirectionTo(posNext);
        }
        return pos.getDirectionTo(target.getPos());
    }

    @Override
    public synchronized void protoDie(Character killer) {
        super.protoDie(killer);
        if (sendDie) {
            protoStatus(StateType.DIE, (long) FactionType.NULL.value);
            sendDie = false;
        }
    }

    boolean attackDone() {
        return DateTime.isAfterTime(timeActionAttack, 0.5f);
    }

    @Override
    public void bonusKillMe(Character killer) {

    }

    @Override
    public void activeSkill(int skillId) {

    }

    @Override
    public Pbmethod.PbUnitPos toProtoPos() {
        Pbmethod.PbUnitPos.Builder pbUser = Pbmethod.PbUnitPos.newBuilder();
        pbUser.setId(id);
        pbUser.setPos(pos.toProto());
        pbUser.setDirection(direction.toProto());
        pbUser.setSpeed((int) point.getMoveSpeed());
        pbUser.setLastInputSeq(indexLastInputSeq);
        return pbUser.build();
    }

    @Override
    public void revive() {
        timeRevive = System.currentTimeMillis();
        this.sendDie = false;
        protoStatus(StateType.REVIVE, (long) (pos.x * 1000), (long) (pos.y * 1000));
        this.alive = true;
        point.resetHpMp();
        protoStatus(StateType.SET_ALL_POINT, point.toProto());
        sendDie = true;
    }

    public Pbmethod.PbUnitAdd.Builder toProtoAdd() {
        Pbmethod.PbUnitAdd.Builder pbAdd = Pbmethod.PbUnitAdd.newBuilder();
        pbAdd.setType(Constans.TYPE_PLAYER);
        pbAdd.setId(id);
        pbAdd.setIsAdd(true);
        pbAdd.setPos(pos.toProto());
        if (panelMap == null) {
            BaseMap map = ResMap.getMap(RoomType.HOME.value, 0);
            panelMap = new PanelMap(map.getMapData());
        }
        pbAdd.setBotLeft(panelMap.botLeft.toProto());
        pbAdd.setTopRight(panelMap.topRight.toProto());
        pbAdd.setDirection(direction.toProto());
        pbAdd.setTeamId(teamId);
        pbAdd.setRangeAttack(rangeAttack);
        pbAdd.addAllAvatar(List.of(0, uHero.getHeroId(), uHero.getHeroId(), 0, 0));
        pbAdd.setSpeed((int) point.getMoveSpeed());
        pbAdd.setCharacterInfo(toCharacterInfo());
        pbAdd.setFaction(FactionType.NULL.value);
        pbAdd.addAllInfo(getListInfo());
        return pbAdd;
    }

    public List<Integer> getListInfo() {
        List<Integer> lst = new ArrayList<>(); // dameSkin,idChat,trial, effectInit,
        lst.add(idDameSkin);
        lst.add(idChatFrame);
        lst.add(idTrial);
        lst.add(0);
        lst.addAll(mUser.toListIdDBItemEquip(uHero));
        return lst;
    }

    private Pbmethod.PbCharInfo toCharacterInfo() {
        Pbmethod.PbCharInfo.Builder pbUser = Pbmethod.PbCharInfo.newBuilder();
        pbUser.setName(name);
        pbUser.setLevel(mUser.getUser().getLevel());
        pbUser.setAlive(alive);
        pbUser.setLastInputSeq(indexLastInputSeq);
        pbUser.addAllPoint(point.toProto());
        return pbUser.build();
    }


}
