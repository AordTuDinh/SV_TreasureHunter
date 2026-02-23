package game.battle.model;

import game.battle.object.*;
import game.battle.type.*;
import game.config.CfgAchievement;
import game.config.CfgQuest;
import game.config.aEnum.*;
import game.dragonhero.BattleConfig;
import game.dragonhero.controller.UserHandler;
import game.dragonhero.mapping.*;
import game.dragonhero.mapping.main.BaseMap;
import game.dragonhero.server.Constans;
import game.dragonhero.server.IAction;
import game.dragonhero.service.Services;
import game.dragonhero.service.resource.ResMap;
import game.dragonhero.service.user.Bonus;
import game.dragonhero.table.BaseRoom;
import game.object.PointBuff;
import game.object.DataQuest;
import game.object.MyUser;
import game.protocol.CommonProto;
import lombok.Data;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.*;
import protocol.Pbmethod;

import javax.persistence.EntityManager;
import java.io.Serializable;
import java.util.*;

import static game.dragonhero.dao.UserDAO.getLogger;
import static ozudo.base.database.DBJPA.slowLog;

@Data
public class Player extends Character implements Serializable {
    MyUser mUser;
    List<NInput> inputs = new ArrayList<>();
    List<NInput> inputsNew = new ArrayList<>();
    long indexLastInputSeq = -1;
    long timeLastProcessInput;
    // auto
    AutoMode autoMode;
    int skillSlotNext;
    long timeLastAction;
    long timeRunHit, timeAttackRun2;
    Pos targetDirectionAttackRun2;
    boolean isAttackRun2;
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
    int countUpdate;
    Pet pet;

    public Player(MyUser mUser, int teamId) {
        initDefault(mUser.getUser().getId(), teamId, mUser.getUser().getInitPoint(mUser));
        this.type = CharacterType.PLAYER;
        this.name = mUser.getUser().getName();
        this.mUser = mUser;
        this.idDameSkin = mUser.getUData().getDameSkinEquip();
        this.idChatFrame = mUser.getUData().getChatFrameEquip();
        this.idTrial = mUser.getUData().getTrialEquip();
        this.autoMode = AutoMode.get(mUser.getUSetting().getAutoMode());
        List<UserWeaponEntity> wes = mUser.getResources().getWeaponEquip();
        for (int i = 0; i < wes.size(); i++) {
            Shuriken shu = new Shuriken(point, wes.get(i), i);
            weaponEquip.add(shu);
        }
        itemsBuf = mUser.getUSetting().getItemSlot(mUser);
        this.pet = mUser.getPet(this);
    }

    private void initDefault(int id, int teamId, Point point) {
        this.id = id;
        this.teamId = teamId;
        this.radius = BattleConfig.C_Collider;
        this.rangeAttack = BattleConfig.P_RangeAttack;
        this.cacheRangeAttack = BattleConfig.P_RangeAttack;
        this.timeLastAction = 0;
        this.indexLastInputSeq = -1;
        this.timeLastProcessInput = 0;
        this.alive = true;
        this.point = point;
        this.attackType = AttackType.LONG_RANGE;
        this.direction = Pos.right();
        this.isMove = false;
        this.attackerInfo = new HashMap<>();
        this.effectsBody = new ArrayList<>();
        this.skillSlotNext = 0;
        weaponEquip = new ArrayList<>();
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
        buffs.set(2, (long) point.getBuffExp());

        for (int i = 0; i < buffCache.size(); i++) {
            BuffItemType type = BuffItemType.getByIndex(i);
            long timeCache = buffCache.get(i);
            if (timeCache > System.currentTimeMillis()) {
                buffs.set(type.pointIndex, buffs.get(type.pointIndex) + type.valueBuff);
            }
        }
    }

    public void CheckUpdateBuff() {
        List<Long> buffCache = mUser.getUData().getBuff(); // 9 số
        List<Long> buffNew = NumberUtil.genListLong(3, 0L); // drop - gold -exp
        // buff từ phúc lợi bang và trang bị
        Point point = getPoint();
        buffNew.set(0, (long) point.getBuffDrop());
        buffNew.set(1, (long) point.getBuffGold());
        buffNew.set(2, (long) point.getBuffExp());

        for (int i = 0; i < buffCache.size(); i++) {
            BuffItemType type = BuffItemType.getByIndex(i);
            long timeCache = buffCache.get(i);
            if (timeCache > System.currentTimeMillis()) {
                buffNew.set(type.pointIndex, buffNew.get(type.pointIndex) + type.valueBuff);
            }
        }
        if (!buffNew.equals(buffs)) {
            UserHandler.buffInfo(mUser);
        }
    }

    public Player(int id, int teamId, Point point, CharacterType type) {
        initDefault(id, teamId, point);
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
        effectsBody = new ArrayList<>();
        timePush = new HashMap<>();
        attackerInfo = new HashMap<>();
//        f0Toxic = new ArrayList<>();
        if (pet != null) {
            pet.setPos(Pos.randomPos(this.pos, 1f, 1f));
        }
    }

    public void setPosAndDirection(Pos newPos, Pos newDirection) {
        this.pos = newPos.round();
        this.direction = newDirection.normalized();
        this.setMove(true);
    }

    public void addNumKillMonster(Character beKill) {
        this.countUpdate++;
        CfgQuest.addNumQuest(mUser, DataQuest.KILL_MONSTER, 1);

        if (beKill.isBoss) CfgQuest.addNumQuest(mUser, DataQuest.KILL_BOSS_MAP, 1);
        CfgAchievement.addAchievement(mUser, 1, beKill.getEnemy().getEnemyKey(), 1);
        mUser.getUData().checkQuestTutorial(mUser, QuestTutType.KILL_ENEMY, beKill.getEnemy().getEnemyKey(), 1);
        if (countUpdate > 100) {
            countUpdate -= 100;
            mUser.getUQuest().update(new ArrayList<>());
        }
    }

    public void updateWeapon(Point point, int slot, UserWeaponEntity uWea) {
        weaponEquip.set(slot, new Shuriken(point, uWea, slot));
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
        updateBuff();
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

    public void setTick(long time) {
        if (curTick == 0) curTick = time;
        else {
            int tick = (int) (time - curTick);
            listTick.add(tick);
            curTick = time;
            if (listTick.size() > poolSizeTick) {
                listTick.remove(0);
            }
        }
    }

    public boolean isAuto() {
        return DateTime.isAfterTime(timeLastAction, BattleConfig.P_timeIdleToAuto);
    }

    public int getLangOfset() {
        if (listTick.isEmpty()) return 45; // 45 ms = fix update min
        IntSummaryStatistics intStats = listTick.stream().mapToInt((x) -> x).summaryStatistics();
        return intStats.getMin();
    }

    public void playerAutoBuff() {
//        for (int i = 0; i < itemsBuf.size() - 3; i += 3) {
//            long triggerHp = itemsBuf.get(i);
//            long triggerMp = itemsBuf.get(i + 1);
//            int itemId = Math.toIntExact(itemsBuf.get(i + 2));
//            if (triggerHp != 0 && itemId != 0) {
//                TriggerAutoItem trigger = getTriggerHp();
//                if (trigger != TriggerAutoItem.NULL && triggerHp >= trigger.value) {
//                    buff(itemId, i / 3);
//                }
//
//            }
//            if (triggerMp != 0 && itemId != 0) {
//                TriggerMp trigger = getTriggerMp();
//                if (trigger != TriggerMp.NULL && triggerMp >= trigger.value) {
//                    buff(itemId, i / 3);
//                }
//            }
//        }
    }

    boolean hasBuffItem(int slot) {
        if (timeBuff.isEmpty()) return true;
        return DateTime.isAfterTime(timeBuff.get(slot), BattleConfig.P_delayUseItemSlot);
    }

    public synchronized void addGold(long gold) {
        mUser.getUser().addGold(gold);
    }

    @Override
    public void activeSkill(int skillIndex) {
        setTimeAttack();
        Shuriken shu = weaponEquip.get(skillIndex);
        shu.setActiveSkill();
        protoStatus(StateType.USE_SKILL, (long) (skillIndex), shu.getTimeActiveSkill() - System.currentTimeMillis(), shu.getNumberAttack(), (long) (getDirection().x * 1000L), (long) (getDirection().y * 1000L), isPowerSkill ? 1L : 0L);
    }

    // todo : edit to use item in slot
    public void useItem(int slot) {
        int itemId = itemsBuf.get(slot * 2 + 1);
        UserItemEntity uItem = mUser.getResources().getItem(itemId);
        if (uItem == null || uItem.getNumber() <= 0) return;
        if (uItem.getType() != ItemType.ITEM_USE) return;
        List<Long> bonus = Bonus.viewItem(itemId, -1);
        sendBonus(bonus, DetailActionType.SU_DUNG_ITEM.getKey(id));
        List<PointBuff> buffs = uItem.getRes().getBuffs();
        mUser.getPlayer().protoBuffPoint(buffs);
        protoStatus(StateType.USE_ITEM_SLOT, (long) slot);
        if (uItem.getNumber() <= 0) {
            mUser.getUSetting().saveSlot(mUser, slot, 0);
            itemsBuf.set(slot * 2 + 1, 0);
            protoStatus(StateType.UPDATE_ITEM_SLOT, GsonUtil.toListLong(itemsBuf));
        }
    }

    public Pos getDirectionHitRun() {
        float nearest = BattleConfig.P_distionHitRun;
        if (distionTop() > nearest && distionBot() > nearest && distionLeft() > nearest && distionRight() > nearest) {
            if (distionTop() <= distionBot()) {
                direction = Pos.up();
            } else if (distionTop() > distionBot()) {
                direction = Pos.down();
            }
        } else if (distionTop() <= nearest && distionRight() > nearest) {
            direction = Pos.right();
        } else if (distionTop() <= nearest && distionRight() <= nearest) {
            direction = Pos.down();
        } else if (distionRight() <= nearest && distionBot() > nearest) {
            direction = Pos.down();
        } else if (distionRight() <= nearest && distionBot() <= nearest) {
            direction = Pos.left();
        } else if (distionBot() <= nearest && distionLeft() > nearest) {
            direction = Pos.left();
        } else if (distionBot() <= nearest && distionLeft() <= nearest) {
            direction = Pos.up();
        } else if (distionLeft() <= nearest && distionTop() > nearest) {
            direction = Pos.up();
        } else if (distionLeft() <= nearest && distionTop() <= nearest) {
            direction = Pos.right();
        }
        // System.out.println("directionHitRun return = " + directionHitRun);
        return directionHitRun;
    }

    public void setTimeRunHit() {
        timeRunHit = System.currentTimeMillis();
    }

    public boolean hasActiveSkill() {  // delay giữa 2 lần đánh
        return DateTime.isAfterTime(timeActionAttack, point.getAttackSpeed());
    }

    public boolean hasUseItem(int slot) {  // delay giữa 2 lần buff
        return DateTime.isAfterTime(timeActiveSlot[slot], BattleConfig.P_TimeDelayActiveItem);
    }

    @Override
    public boolean isReviveReady() {
        return DateTime.isAfterTime(timeRevive, BattleConfig.P_timeImmortal);
    }

    // code đảo chiều dựa theo 2 điều kiện - dùng trong vòng lặp thời gian set liên tục
    public void setTimeAttackRun2(Pos target) {
        if (isAttackRun2Done()) isAttackRun2 = false;
        if (isAttackRun2 == true) return;
        this.timeAttackRun2 = System.currentTimeMillis();
        this.targetDirectionAttackRun2 = getPos().getRandDiectionTo(target, 10f);
        this.isAttackRun2 = true;
    }

    public boolean hasSkillKey(int keyInput) { // check CD
        return NInput.lstSkillKey.contains(keyInput) && weaponEquip.get(keyInput - NInput.offsetSkill).hasActiveSkill();
    }

    public boolean hasSkillIndex(int skillIndex) { // check CD
        return NInput.lstSkillIndex.contains(skillIndex) && weaponEquip.get(skillIndex).hasActiveSkill();
    }

    public boolean hasUseItemIndex(int slot) { // check dung skill
        return BattleConfig.itemUseSlot.contains(slot);
    }

    public boolean isAttackRun2Done() {
        return DateTime.isAfterTime(timeAttackRun2, BattleConfig.P_attackRun2);
    }

    public boolean isRunHit() {
        return !DateTime.isAfterTime(timeRunHit, BattleConfig.P_timeRunHit);
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
        this.isMove = !DateTime.isAfterTime(timeActionMove, BattleConfig.P_timeNoMove);
        return isMove;
    }

//    public boolean hasAttackAfterMove() {
//        return true;
//    }

    public void setTargetAttack(Character attacker) {
        this.targetAttack = attacker;
        if (attacker != null) {
            protoOneStatus(StateType.SWITCH_TARGET, (long) attacker.id);
        }
    }


//    @Override
//    public void FixedUpdate() {
//        super.FixedUpdate();
//        int ic = getInputs().size();
//        if (isAlive() && ic > 0) {
//            for (int index = 0; index < ic; ++index) {
//                NInput input = getAndRemoveInput(0);
//                if (input == null) return;
//                for (int i = 0; i < input.keys.size(); i++) {
//                    int typeInput = input.keys.get(i);
//                    if (typeInput == NInput.NONE) {
//                        continue;
//                    }
//                    List<Pos> direction = new ArrayList<>();
//                    if (typeInput <= NInput.RightDown) {
//                        direction.addAll(Arrays.asList(NInput.mapInput.get(typeInput)));
//                    }
//                    for (Pos direct : direction) {
//                        if (beBlock()) return;
//                        Pos nd = Pos.moveFromDirection(direct, getCurSpeed());
//                        move(nd);
//                        System.out.println(" --- pos ==== " + pos.toString());
//                        if (!nd.equals(Pos.zero())) {
//                            setDirection(nd.normalized());
//                        }
//                    }
//                }
//            }
//        }
//    }


    public Pos findEnemyNearest(List<Character> lstEnemy) {
        if (lstEnemy.size() <= 0) return Pos.zero();
        if (getTargetAttack() != null && getTargetAttack().isAlive()) return getTargetAttack().getPos();
        float min = 999999f;
        Pos ret = Pos.zero();
        Character target = null;
        for (int i = 0; i < lstEnemy.size(); i++) {
            if (lstEnemy.get(i).isAlive() && min > getPos().distance(lstEnemy.get(i).getPos()) && lstEnemy.get(i).isReady()) {
                min = (float) getPos().distance(lstEnemy.get(i).getPos());
                target = lstEnemy.get(i);
                ret = lstEnemy.get(i).getPos();
            }
        }
        setTargetAttack(target);
        return ret;
    }

    @Override
    public synchronized void protoDie(Character killer) {
        super.protoDie(killer);
        if (sendDie) {
            protoStatus(StateType.DIE, (long) FactionType.NULL.value);
            sendDie = false;
        }
    }

    @Override
    public void bonusKillMe(Character killer) {

    }

    @Override
    public Pbmethod.PbUnitPos toProtoPos() {
        Pbmethod.PbUnitPos.Builder pbUser = Pbmethod.PbUnitPos.newBuilder();
        pbUser.setId(id);
        pbUser.setPos(pos.toProto());
//        System.out.println("pos.toString() = " + pos.toString());
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
        pbAdd.addAllAvatar(mUser.getUser().getAvatar());
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
        lst.add(mUser.getUData().getEffInit());
        UserHeroEntity uHero = mUser.getResources().getHero(mUser.getUser().getHeroMain());
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

    public void removeBuff() {
        this.rangeAttack = cacheRangeAttack;
        for (int i = 0; i < weaponEquip.size(); i++) {
            weaponEquip.get(i).removeBuff();
        }
    }

    public void toPbEndGame() {
        toPbEndGame(false, 0, null, 0, PopupType.POPUP_DEAD,List.of(0,0));
    }

    public void toPbEndGame(boolean isWin, int per, List<Long> bonus, int timeAttack, PopupType popup, List<Integer> info) {
        protocol.Pbmethod.PbEndGame.Builder pb = protocol.Pbmethod.PbEndGame.newBuilder();
        pb.setBattleKey(getRoom().getKeyRoom());
        pb.setIsWin(isWin);
        pb.setPopupId(popup.value);
        if (bonus != null) {
            pb.addAllBonus(bonus);
        }
        pb.setTime(timeAttack);
        pb.setPerDame(Math.min(per, 100));
        pb.setInfo(CommonProto.getCommonIntVectorProto(info));
        Util.sendProtoData(mUser.getChannel(), pb.build(), IAction.END_GAME);
    }

    public void toPbStartGame(float timeRemain) {
        Util.sendProtoData(mUser.getChannel(), CommonProto.getCommonVector((long) (timeRemain)), IAction.START_GAME);
    }


    public NInput getAndRemoveInput(int index) {
        if (!isReady()) return null;
        NInput input = inputs.get(index);
        inputs.remove(input);
        return input;
    }

    public NInput getAndRemoveInput2(int index) {
        if (!isReady()) return null;
        NInput input = inputsNew.get(index);
        inputsNew.remove(input);
        return input;
    }


    public void sendBonus(List<Long> bonus, String title) {
        List<Long> bm = Bonus.receiveListItem(mUser, title, bonus);
        protoStatus(StateType.ADD_BONUS, bm.size(), bm);
    }


    // bonus bắn ra từ điểm
    public void sendForceBonus(BonusKillEnemy bonus, String title, Pos posInstance) {
        // tính vào exp party

        UserPartyEntity uParty = mUser.getUser().getParty();
        if (uParty != null) {
            uParty.shareBonusParty(mUser, bonus);
        }
        List<Long> bonusReal = bonus.getBonus();
        if (bonus.getGold() > 0) bonusReal.addAll(Bonus.viewGold(bonus.getGold()));
        if (bonus.getExp() > 0) bonusReal.addAll(Bonus.viewExp(bonus.getExp()));
        List<Long> bm = Bonus.receiveListItem(mUser, title, bonusReal);
        bm.add(0, (long) (posInstance.x * 1000));
        bm.add(1, (long) (posInstance.y * 1000));
        protoStatus(StateType.BONUS_ADD_FORCE, bm.size(), bm);

    }

}
