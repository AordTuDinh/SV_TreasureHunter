package game.battle.model;

import game.battle.calculate.MathLab;
import game.battle.object.*;
import game.battle.type.AttackType;
import game.battle.type.RoomState;
import game.battle.type.StateType;
import game.config.CfgEventDrop;
import game.config.aEnum.DetailActionType;
import game.config.aEnum.ItemKey;
import game.config.aEnum.PopupType;
import game.dragonhero.BattleConfig;
import game.dragonhero.mapping.UserDataEntity;
import game.dragonhero.mapping.UserEventSevenDayEntity;
import game.dragonhero.mapping.main.ResBossEntity;
import game.dragonhero.mapping.main.ResEnemyEntity;
import game.dragonhero.server.Constans;
import game.dragonhero.server.IAction;
import game.dragonhero.service.Services;
import game.dragonhero.service.resource.ResEnemy;
import game.dragonhero.service.user.Bonus;
import game.dragonhero.table.BaseBattleRoom;
import game.protocol.CommonProto;
import lombok.Data;
import lombok.Getter;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.StringHelper;
import ozudo.base.helper.Util;
import protocol.Pbmethod;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//Fixme DATE: 7/31/2022 LƯU Ý --->CẦN TƯ DUY CODE NHIỀU THẰNG 1 ROOM CÙNG ĐÁNH BOSS
@Data
public class BossGod extends Character implements Serializable {
    //Fixme DATE: 8/21/2022 LƯU Ý ---> Boss skill hoat dong doc lap voi danh thuong
    protected static final int skill1Id = 0;
    List<BossSkill> skills;
    int bossId;
    List<ResBossEntity> support;
    AttackType attackType;
    float rangeAttack;
    long timeActiveSkill = 0;
    int avatar;
    long timeActiveEnemy = 0;
    boolean hasMove = false;
    protected static final List<Integer> rateSkill = List.of(70, 80, 90, 100); // tỉ lệ ra skill : đánh thường, skill 1 2 3
    boolean bossStart = false;
    boolean rageMode = false;
    int timeSecondsActiveEnemy = 5; // sau time này kể từ khi join room thì active enemy
    protected static final int maxEnemy = 4;
    int enemyId;
    static final long SKILL0 = 0;
    static final long SKILL1 = 1;
    static final long SKILL2 = 2;
    static final long SKILL3 = 3;
    static final long SKILL4 = 4;
    static final long SKILL5 = 5;
    static final long SKILL6 = 6;
    int indexSkill = 0;
    float capEnemy = 2;
    Pos capBullet = Pos.zero();
    static final int timeDelayActiveEnemy = 3000; // thời gian anim active enemy
    static final int KIM_THAN = 1;
    static final int THUY_THAN = 2;
    static final int HOA_THAN = 3;
    static final int THO_THAN = 4;
    float speedSkill = 1;

    public BossGod(ResBossEntity boss, Pos startPos, int teamId, BaseBattleRoom room) {
        this.name = boss.getName();
        this.model = boss.getModel();
        this.point = boss.toPoint();
        this.bossId = boss.getId();
        this.radius = boss.getRadius();
        this.rangeAttack = boss.getRangeAttack();
        this.type = boss.getCharacterType();
        this.pos = startPos;
        this.id = room.getIdNext();
        this.avatar = boss.getModel();
        this.teamId = teamId;
        this.instancePos = pos.clone();
        this.faction = boss.getFaction();
        this.enemyId = boss.getEnemy();
        this.alive = true;
        this.room = room;
        this.direction = Pos.right();
        this.hasBonusKillMe = false;
        this.isMove = false;
        this.isBoss = true;
        this.effectsBody = new ArrayList<>();
        this.attackerInfo = new HashMap<>();
        this.timeJoinRoom = System.currentTimeMillis();
        this.timeActiveEnemy = System.currentTimeMillis() + timeSecondsActiveEnemy * DateTime.SECOND2_MILLI_SECOND;
        this.setTimeRandomMove();
        this.panelMap = new PanelMap(room.getMapInfo().getMapData());
        skills = boss.getSkills();
        hasMove = this.getPoint().getMoveSpeed() > 0;
        attackType = AttackType.get(boss.getRangeAttack());
        //if (attackType == AttackType.LONG_RANGE) {
        weaponEquip = new ArrayList<>();
        Shuriken shu = new Shuriken(boss.getBossSkillConfig().weaponId, 0);
        weaponEquip.add(shu);
        //}
        support = boss.getSupport();
    }

    protected void addTimeActive(long timeAdd) {
        timeAdd = (long) (timeAdd * speedSkill);
        timeActiveSkill = System.currentTimeMillis() + timeAdd;
        timeActionAttack = System.currentTimeMillis() + timeAdd;
    }

    public void addSupportPoint(Point spPoint) {
        // check tra ve client nua hoac tra ca cuc do phai nghi
        point.addCurHp(spPoint.getMaxHp());
        point.addBaseHp(spPoint.getMaxHp());
        point.addHpRegen(spPoint.getHpRegen());
        point.addBaseAttack(spPoint.get(Point.ATTACK));
        point.addBaseMagicAttack(spPoint.get(Point.MAGIC_ATTACK));
        point.addDef(spPoint.get(Point.DEFENSE));
        point.addMagicResist(spPoint.get(Point.MAGIC_RESIST));
        point.addCrit(spPoint.get(Point.CRIT) * 10);
        point.addCritDamage(spPoint.get(Point.CRIT_DAMAGE));
        point.addAgility(spPoint.get(Point.AGILITY));
        point.addImmunity(spPoint.get(Point.IMMUNITY));
        protoStatus(StateType.SET_ALL_POINT, point.toProto());
    }

    protected boolean activeRageMode() {
        // check cuồng nộ
        return !rageMode && point.getPerHp() < 30;
    }

    protected void processSkill() {
    }


    public boolean checkStart() {
        // đảo chiều để k phải check nhiều lần
        if (!bossStart) {
            bossStart = DateTime.isAfterTime(timeJoinRoom, BattleConfig.BOSS_DELAY_ATTACK);
        }
        return bossStart;
    }


    @Override
    public void Update() {
        super.Update();
        if (room.getRoomState() != RoomState.ACTIVE) return;
        if (!isAlive() || !isReviveReady() || !checkStart()) return;
        if (targetAttack == null || !targetAttack.alive) targetAttack = findTargetNearest();
        if (delayAction(2)) enemyProcess();
    }

    public void LastUpdate() {//0.1s
    }

    private void enemyProcess() {
        if (beBlock() && !alive && targetAttack == null) return;
        if (hasMove && !inRankAttack()) {//&& enemy.hasActiveMove()) {
            enemyMove();
        } else { // trong tầm đánh thì k move
            setMove(false);
            //if (hasAttack()) {
            //    getBattleRoom().addBullet(this, skill1Id, processActiveSkill());
            //}
        }
    }

    public void enemyMove() {
        if (getTargetAttack() == null) return;
        Pos targetMove = getTargetAttack().pos;
        Pos direction = pos.getDirectionTo(targetMove);
        setDirection(direction);
        Pos nd = Pos.moveFromDirection(getDirection(), getCurSpeed());
        move(nd);
        if (!nd.equals(Pos.zero())) {
            setDirection(nd.normalized());
        }
    }

    // delay start skill
    public boolean delayAction(float time) {
        return DateTime.isAfterTime(timeJoinRoom, BattleConfig.BOSS_DELAY_ATTACK + time);
    }

    public boolean hasAttackNormal() {
        return inRankAttack() && hasActionAttack();
    }


    public boolean hasAttack() {
        return hasActionAttack() && alive && targetAttack != null && targetAttack.alive;
    }


    private boolean hasActionAttack() {
        return DateTime.isAfterTime(timeActionMove, BattleConfig.M_timeDelayMoveToAttack);
    }

    public boolean inRankAttack() {
        if (targetAttack == null) return false;
        return pos.distance(targetAttack.pos) < rangeAttack;
    }

    Character findTargetNearest() {
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
        if (min <= BattleConfig.BOSS_RANGE_VIEW_TARGET) return room.getAPlayer().get(index);
        return null;
    }

    public Pos getPosInitBullet() {
        return new Pos(pos.x + capBullet.x, pos.y + capBullet.y);
    }

    public List<Bullet> processActiveSkill() {
        List<Bullet> bullets = new ArrayList<>();
        Pos direction;
        Pos posInit = getPosInitBullet();
        if (targetAttack == null || !targetAttack.isAlive()) return bullets;
        if (targetAttack.getPos().equals(Pos.zero())) {
            // dùng pos theo hướng trái phải
            if (getDirection().x > 0) direction = Pos.right();
            else direction = Pos.left();
        } else direction = MathLab.getDirection(posInit, targetAttack.getPos());


        if (targetAttack.isMove()) {
            direction = getFutureDirection(15, 20);
        }
        if (!isLikeFace(direction) || getDirection().equals(Pos.zero())) {
            setDirection(direction.clone());
        }

        if (model == THUY_THAN || model == HOA_THAN || model == THO_THAN) { // thủy, thổ, hoả thần bay hết tầm phát nổ
            getShurikenSlot(0).setRangeFly((float) posInit.distance(targetAttack.getPos()));
        }
        bullets.addAll(skillActive(getShurikenSlot(0), direction, posInit, getShurikenSlot(0).getDegree()));
        return bullets;
    }

    @Override
    public void bonusKillMe(Character killer) {
        Player player = killer.getPlayer();
        ResBossEntity boss = ResEnemy.getBoss(bossId);
        List<Long> aBonus = boss.getBonusKillBoss(player.getMUser().getPerReceiveBoss());
        aBonus.addAll(Bonus.viewItem(ItemKey.BOSS_TICKET, -1));
        aBonus.addAll(CfgEventDrop.bonusDrop(CfgEventDrop.config.getRateDropBossGod(), 1));
        List<Long> bonus = Bonus.receiveListItem(killer.getPlayer().getMUser(), DetailActionType.PHAN_THUONG_BOSS_GOD.getKey(room.getKeyRoom()), aBonus);
        player.toPbEndGame(true, 100, bonus, getBattleRoom().getTimeAttack(), PopupType.POPUP_END_GAME, List.of(1, 1));
        player.resetData();
        UserDataEntity uData = player.getMUser().getUData();
        List<Integer> bossGod = uData.getBossGod();
        if (bossGod.get(boss.getFaction().value - 1) < boss.getLevel()) {
            bossGod.set(boss.getFaction().value - 1, boss.getLevel());
            if (uData.update(List.of("boss_god", StringHelper.toDBString(bossGod)))) {
                uData.setBossGod(bossGod.toString());
                Util.sendProtoData(player.getMUser().getChannel(), CommonProto.getCommonIntVector(bossGod), IAction.BOSS_GOD_DATA);
            }
        }
        // event 7 day attack boss day 2
        UserEventSevenDayEntity uEvent = Services.userDAO.getUserSevenDay(player.getMUser());
        if (uEvent.hasEvent() && uEvent.hasActive(1) && uEvent.update(List.of("attack_boss", uEvent.getAttackBoss() + 1))) {
            uEvent.setAttackBoss(uEvent.getAttackBoss() + 1);
        }
    }

    @Override
    public void protoDie(Character killer) {
        super.protoDie(killer);
        protoStatus(StateType.DIE, (long) faction.value);
        //    getBattleRoom().addCoroutine(((BossGodRoom) room).bossDie(1f,this));
    }

    @Override
    public void activeSkill(int skillIndex) {
        setTimeAttack();
        Shuriken shu = weaponEquip.get(skillIndex);
        protoStatus(StateType.USE_SKILL, (long) (skillIndex), shu.getTimeActiveSkill(), shu.getNumberAttack(), (long) (getDirection().x * 1000L), (long) (getDirection().y * 1000L), 0L);
        weaponEquip.get(skillIndex).setActiveSkill();
    }

    public Pbmethod.PbCharInfo toProtoInfo() {
        Pbmethod.PbCharInfo.Builder pbUser = Pbmethod.PbCharInfo.newBuilder();
        pbUser.setName(name);
        pbUser.setLevel(0);
        pbUser.setAlive(alive);
        pbUser.addAllPoint(point.toProto());
        return pbUser.build();
    }

    protected void genEnemy(BaseBattleRoom room, long skill, float delay) {
        ResEnemyEntity enemy = ResEnemy.getEnemy(enemyId);
        if (enemy == null) return;
        protoStatus(StateType.PLAY_ANIMATION, skill);
        room.addCoroutine(new Coroutine(delay, () -> {
            for (int i = 0; i < maxEnemy; i++) {
                Pos monsterPos = Pos.zero();
                switch (i) {
                    case 0 -> monsterPos = new Pos(pos.x - capEnemy, pos.y - capEnemy);
                    case 1 -> monsterPos = new Pos(pos.x - capEnemy, pos.y + capEnemy);
                    case 2 -> monsterPos = new Pos(pos.x + capEnemy, pos.y + capEnemy);
                    case 3 -> monsterPos = new Pos(pos.x + capEnemy, pos.y - capEnemy);
                }
                monsterPos = Pos.capPos(monsterPos, panelMap.botLeft, panelMap.topRight, BattleConfig.C_Collider);
                Enemy bot = new Enemy(enemy, monsterPos, Pos.RandomDirection(), teamId, room);
                bot.setTargetAttack(targetAttack);
                bot.setBeAttack(true);
                room.getAEnemy().add(bot);
                room.getAProtoAdd().add(bot.toProtoAdd());
            }
        }));
        timeActiveSkill = System.currentTimeMillis() + timeDelayActiveEnemy;
    }

    public Pbmethod.PbUnitAdd.Builder toProtoAdd() {
        Pbmethod.PbUnitAdd.Builder pbAdd = Pbmethod.PbUnitAdd.newBuilder();
        pbAdd.setType(Constans.TYPE_MONSTER);
        pbAdd.setId(id);
        pbAdd.addAvatar(model);
        pbAdd.setTeamId(teamId);
        pbAdd.addAvatar(avatar);
        pbAdd.setIsAdd(true);
        pbAdd.setRangeAttack(rangeAttack);
        pbAdd.setPos(pos.toProto());
        pbAdd.setBotLeft(panelMap.botLeft.toProto());
        pbAdd.setTopRight(panelMap.topRight.toProto());
        pbAdd.setDirection(direction.toProto());
        pbAdd.setSpeed((int) point.getMoveSpeed());
        pbAdd.addInfo(type.value); // info[0] : enemy Type
        pbAdd.addInfo(avatar); // boss ID
        pbAdd.addInfo(idDameSkin);
        pbAdd.addInfo(idChatFrame);
        pbAdd.addInfo(idTrial);
        pbAdd.setCharacterInfo(toProtoInfo());
        return pbAdd;
    }
}
