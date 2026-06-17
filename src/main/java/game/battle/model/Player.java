package game.battle.model;

import game.battle.object.*;
import game.battle.type.*;
import game.config.CfgAchievement;
import game.config.CfgQuest;
import game.config.aEnum.*;
import game.treasure.BattleConfig;
import game.treasure.service.resource.ResMap;
import game.treasure.service.user.Bonus;
import game.treasure.table.BaseRoom;
import game.object.DataQuest;
import game.object.MyUser;
import lombok.Data;
import ozudo.base.helper.*;
import protocol.Pbmethod;

import java.io.Serializable;
import java.util.*;

@Data
public class Player extends Unit implements Serializable {
    MyUser mUser;
    List<NInput> inputs = new ArrayList<>();
    List<NInput> inputsNew = new ArrayList<>();
    long indexLastInputSeq = -1;
    long timeLastProcessInput;
    int skillSlotNext;
    long timeLastAction;
    long timeRunHit, timeAttackRun2;
    Pos targetDirectionAttackRun2;
    boolean isAttackRun2;
    Pos directionHitRun = Pos.zero();
    List<Long> timeBuff; // list time buff theo slot
    //ping logic
    long curTick = 0;
    int poolSizeTick = 5;
    List<Long> buffs = Arrays.asList(0L, 0L, 0L); // size =3 drop - gold - exp : per 100
    public List<Integer> listTick = new ArrayList<>(); // size =5; // test
    // analysis
    int countUpdate;
    Pet petUse;

    public Player(MyUser mUser, int clanId) {
        initDefault( clanId, mUser.getUser().getInitPoint(mUser));
        this.type = UnitType.PLAYER;
        this.name = mUser.getUser().getName();
        this.mUser = mUser;
        this.idDameSkin = mUser.getUData().getDameSkinEquip();
        this.idChatFrame = mUser.getUData().getChatFrameEquip();
        this.idTrial = mUser.getUData().getTrialEquip();
        this.petUse = mUser.getPet(this);
        long protectedEnd = mUser.getUData().getTimeProtected();
        if (protectedEnd > System.currentTimeMillis()) {
            setTimeProtectedEnd(protectedEnd);
        }
    }

    private void initDefault( int clanId, Point point) {
        this.clanId = clanId;
        this.rangeAttack = BattleConfig.P_RangerAttack;
        this.timeLastAction = 0;
        this.indexLastInputSeq = -1;
        this.timeLastProcessInput = 0;
        this.alive = true;
        this.point = point;
        this.direction = Pos.right();
        this.isMove = false;
        this.skillSlotNext = 0;
        timeBuff = new ArrayList<>();
    }

    public void updateBuff() {
        // tính ra list 6 số gồm max buff và thời gian còn lại gần nhất,để đến lúc hẹn giờ theo time đó thì tính lại buff
        List<Long> buffCache = mUser.getUData().getBuff(); // 9 số
        buffs = NumberUtil.genListLong(3, 0L); // drop - gold -exp
        // buff từ phúc lợi bang và trang bị
        Point point = getPoint();
        buffs.set(0, (long) point.getBuffDrop());
        buffs.set(1, (long) point.getBuffGold());

        for (int i = 0; i < buffCache.size(); i++) {
            BuffItemType type = BuffItemType.getByIndex(i);
            long timeCache = buffCache.get(i);
            if (timeCache > System.currentTimeMillis()) {
                buffs.set(type.pointIndex, buffs.get(type.pointIndex) + type.valueBuff);
            }
        }
    }

    public Player( int teamId, Point point, UnitType type) {
        initDefault( teamId, point);
        this.type = type;
    }


    public void clearDataForChangeRoom(Pos... instancePos) {
        mUser.setCachePos();
        clearDataNoCachePos(instancePos);
    }

    public void clearDataNoCachePos(Pos... instancePos) {
        point.initDefault();
        this.ready = false;
        this.pos = instancePos.length > 0 ? instancePos[0] : Pos.zero();
        this.indexLastInputSeq = -1;
        this.alive = true;
        skillSlotNext = 0;
        timeBeHit = 0;
        beDameInfo = new HashMap<>();
        targetMove = Pos.zero();
        if (petUse != null) {
            petUse.setPos(Pos.randomPos(this.pos, 1f, 1f));
        }
    }



    public void addNumKillMonster(Unit beKill) {
        this.countUpdate++;
        CfgQuest.addNumQuest(mUser, DataQuest.KILL_MONSTER, 1);

//        if (beKill.type ==UnitType.BOSS) CfgQuest.addNumQuest(mUser, DataQuest.KILL_BOSS_MAP, 1);
        CfgAchievement.addAchievement(mUser, 1, beKill.getEnemy().getModel(), 1);
        mUser.getUData().checkQuestTutorial(mUser, QuestTutType.KILL_ENEMY, beKill.getEnemy().getModel(), 1);
        if (countUpdate > 100) {
            countUpdate -= 100;
            mUser.getUQuest().update(new ArrayList<>());
        }
    }


    // set time join, va clear data old map
    public void setJoinMap(BaseRoom room) {
        timeJoinRoom = System.currentTimeMillis();
        //timeBeAttack = 0;
        targetMove = null;
        this.ready = true;
        directionMoveAttack = Pos.zero();
        this.room = room;
        this.panelMap = room.getMapInfo();
        long protectedEnd = mUser.getUData().getTimeProtected();
        if (protectedEnd > System.currentTimeMillis()) {
            setTimeProtectedEnd(protectedEnd);
        }
        updateBuff();
    }

    public boolean isAuto() {
        return DateTime.isAfterTime(timeLastAction, BattleConfig.P_timeIdleToAuto);
    }



    public synchronized void addGold(long gold) {
        mUser.getUser().addGold(gold);
    }

    public boolean hasAttack() {  // delay giữa 2 lần đánh
        return DateTime.isAfterTime(timeActionAttack, point.getAttackSpeed());
    }


    @Override
    public boolean isReviveReady() {
        return true;
    }

    @Override
    public void Update() {

    }


    public boolean targetInSizeAttack(Unit target) {
        return pos.distance(target.pos) < rangeAttack;
    }

    @Override
    public boolean isReady() {
        return DateTime.isAfterTime(timeJoinRoom, BattleConfig.P_delayReady) && ready;
    }

    public boolean isMove() {
        this.isMove = !DateTime.isAfterTime(timeActionMove, BattleConfig.P_timeNoMove);
        return isMove;
    }

    @Override
    public synchronized void protoDie(Unit killer) {
        long protectedUntil = System.currentTimeMillis() + BattleConfig.P_timeProtectedMs;
        setTimeProtectedEnd(protectedUntil);
        mUser.getUData().setTimeProtected(protectedUntil);
        mUser.getUData().update(Arrays.asList("time_protected", protectedUntil));
        super.protoDie(killer);
        if (sendDie) {
            protoStatus(Pbmethod.SubStateType.DIE);
            sendDie = false;
        }
    }

    @Override
    public void bonusKillMe(Unit killer) {

    }

    @Override
    public void revive() {
        timeRevive = System.currentTimeMillis();
        this.sendDie = false;
        pos = Pos.zero();
        long protectedEnd = mUser.getUData().getTimeProtected();
        setTimeProtectedEnd(protectedEnd);
        protoStatus(Pbmethod.SubStateType.REVIVE, 0L, 0L, BattleConfig.toWireProtectedMs(protectedEnd));
        this.alive = true;
        point.initDefault();
        point.resetHpPercent(BattleConfig.P_reviveHpPercent);
        protoStatus(Pbmethod.SubStateType.UPDATE_MULTI_POINT, point.toProto());
        sendDie = true;
    }

    /**
     * PbUnit remove phải có userId khớp client — Unit.toProtoRemove không set userId (0),
     * khiến client tưởng là người khác và gọi RemoveUnit nên tự hủy PlayerController.
     */
    @Override
    public Pbmethod.PbUnit toProtoRemove(int chunkId) {
        Pbmethod.PbUnit base = super.toProtoRemove(chunkId);
        return base.toBuilder().setUserId(mUser.getUserId()).build();
    }

    @Override
    public List<Integer> getListInfo(int effInit) {
        List<Integer> lst = new ArrayList<>(super.getListInfo(effInit));
        lst.addAll(mUser.getUser().getListItemKeyEquip());
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
        pbAdd.setClanId(clanId);
        pbAdd.setRangeAttack(rangeAttack);
        pbAdd.setAvatar(mUser.getUser().getBodySkinId());
        pbAdd.setSpeed((int) point.getMoveSpeed());
        pbAdd.setName(name);
        pbAdd.setAlive(alive);
        pbAdd.setLastInputSeq(indexLastInputSeq);
        pbAdd.addAllPoint(point.toProto());
        pbAdd.addAllInfo(getListInfo(mUser.getUData().getEffInit()));
        pbAdd.setUserId(mUser.getUserId());
        long protectedEnd = mUser.getUData().getTimeProtected();
        if (protectedEnd > System.currentTimeMillis()) {
            pbAdd.setTimeProtected(BattleConfig.toWireProtectedMs(protectedEnd));
            setTimeProtectedEnd(protectedEnd);
        }
        return pbAdd.build();
    }




    public void sendBonus(List<Long> bonus, String title) {
        List<Long> bm = Bonus.receiveListItem(mUser, title, bonus);
        protoStatus(Pbmethod.SubStateType.ADD_BONUS, bm);
    }


    // bonus bắn ra từ điểm
    public void sendForceBonus(BonusKillEnemy bonus, String title, Pos posInstance) {
        // tính vào exp party
//        UserPartyEntity uParty = mUser.getUser().getParty();
//        if (uParty != null) {
//            uParty.shareBonusParty(mUser, bonus);
//        }
//        List<Long> bonusReal = bonus.getBonus();
//        if (bonus.getGold() > 0) bonusReal.addAll(Bonus.viewGold(bonus.getGold()));
//        List<Long> bm = Bonus.receiveListItem(mUser, title, bonusReal);
//        bm.add(0, (long) (posInstance.x * 1000));
//        bm.add(1, (long) (posInstance.y * 1000));
      //  protoStatus(StateType.BONUS_ADD_FORCE, bm.size(), GsonUtil.toListInt(bm));

    }

}
