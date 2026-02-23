package game.battle.model;

import game.battle.calculate.IMath;
import game.battle.calculate.MathLab;
import game.battle.effect.Effect;
import game.battle.effect.EffectRoom;
import game.battle.effect.SkillEffect;
import game.battle.object.*;
import game.battle.type.CharacterType;
import game.battle.type.EffectBodyType;
import game.battle.type.RoomState;
import game.battle.type.StateType;
import game.dragonhero.BattleConfig;
import game.dragonhero.mapping.main.ResBossEntity;
import game.dragonhero.mapping.main.ResEnemyEntity;
import game.dragonhero.service.battle.EffectType;
import game.dragonhero.service.battle.TriggerType;
import game.dragonhero.service.resource.ResEnemy;
import game.dragonhero.table.BaseBattleRoom;
import game.dragonhero.table.BossClanRoom;
import game.dragonhero.table.BossGodRoom;
import game.object.PointBuff;
import lombok.Getter;
import lombok.Setter;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;

import java.io.Serializable;
import java.util.*;

//Fixme DATE: 7/31/2022 LƯU Ý --->CẦN TƯ DUY CODE NHIỀU THẰNG 1 ROOM CÙNG ĐÁNH BOSS
public class BossClan extends BossGod implements Serializable {
    static final int turnEnemy = 10;
    static final float timeDelayCage = 10f;
    static final float timeAliveSeed = 5f;
    static final List<Integer> skilLActive = List.of(0,0,1,0,0,0,3,0,0,0,2,0,0,5,0,0,4,0,0,0,0,6,0,0,7,0,0,0,8,6,0,9);
    long timeCreateSeed;
    boolean createSeed = false;
    public static final float timeDelayBomb = 10f;
    static final List<Float> sizeElip2 = List.of(3f, 1f);
    // skill index
    BossClanRoom clanRoom;
    final int decMoveSpeed = 50;
    int timeDecMoveSpeed = 3;//5s
    ResEnemyEntity treeEnemy;
    boolean counterBullet;
    List<Float> valueDameSkill6 = List.of(100f, 100f);
    List<Float> valueDameSkill8 = List.of(100f, 100f);
    List<Float> valueDameSkill9 = List.of(100f, 100f);
    List<Float> valueDameSkill10 = List.of(100f, 100f);
    List<Float> valueDameSkill12 = List.of(100f, 100f);
    @Getter
    @Setter
    List<Integer> cotInfo = NumberUtil.genListInt(4, 0);
    List<Integer> cotAddPoint = NumberUtil.genListInt(4, 0); // atk , hp regen, def, crit


    //Fixme DATE: 8/21/2022 LƯU Ý ---> Boss skill hoat dong doc lap voi danh thuong
    public BossClan(ResBossEntity boss, Pos startPos, int teamId, BaseBattleRoom room, int levelBoss) {
        super(boss, startPos, teamId, room);
        clanRoom = (BossClanRoom) room;
        treeEnemy = ResEnemy.getEnemy(1009);
        // buff suc manh theo level boss
        if (levelBoss > 0) { // tang suc manh boss theo level
            this.point.buffPer(levelBoss * 10);
            this.point.resetHpMp();
        }
        this.enemyId = 809;
        this.capEnemy = 4;
        cotAddPoint.set(0, 100 * (levelBoss + 1));
        cotAddPoint.set(1, 100 * (levelBoss + 1));
        cotAddPoint.set(2, 100 * (levelBoss + 1));
        cotAddPoint.set(3, 70);
    }


    @Override

    public void LastUpdate() {
        super.LastUpdate();
        processSkill();
        processEffectClient();
    }

    @Override
    protected void processSkill() {
        if (targetAttack == null) targetAttack = findTargetNearest();
        if (room.getRoomState() == RoomState.END || attackBlockMove() || targetAttack == null || !targetAttack.isAlive() || !isAlive() || beBlock() || !isReviveReady() || !checkStart() || System.currentTimeMillis() < timeActiveSkill) {
            return;
        }
        indexSkill++;
        if (indexSkill >= skilLActive.size()) indexSkill = 0;
        int skillNext = skilLActive.get(indexSkill);
        //buff chí mạng 30% + 100% sát thương chí mạng
        if (activeRageMode()) {
            rageMode = true;
            addTimeActive(3000);
            point.addCrit(50);
            point.addCritDamage(100);
            protoStatus(StateType.PLAY_ANIMATION, SKILL5);
            protoStatus(StateType.EFFECT_BODY, Arrays.asList(EffectBodyType.CRAZY.value, 60000L));
            return;
        }
//        skillNext = 3;
//        System.out.println("skillNext = " + skillNext);
        switch (skillNext) {
            case 0 -> { // đánh thường
                if (hasAttackNormal()) {
                    protoStatus(StateType.PLAY_ANIMATION, SKILL3);
                    activeSkill(skill1Id);
                    room.addCoroutine(new Coroutine(BattleConfig.B_timeDelayAnim, () -> {
                        getBattleRoom().addBullet(this, skill1Id, processActiveSkill());
                    }));
                    addTimeActive(1000);
                }
            }
            case 1 -> { //triệu hồi quái
                List<Long> dataClient = new ArrayList<>();
                dataClient.add((long) EffectType.MOC_THAN_1.id);
                for (int i = 0; i < turnEnemy; i++) {
                    Pos posInit = Pos.randomInPanel(room.getMapInfo().getMapData(), 3f, 3f);
                    int idEnemy = clanRoom.getIdEffectClient();
                    SeedMocThan seed = new SeedMocThan(idEnemy, posInit);
                    clanRoom.getSeedMoc().put(idEnemy, seed);
                    dataClient.add((long) idEnemy);
                    dataClient.add((long) (posInit.x * 1000));
                    dataClient.add((long) (posInit.y * 1000));
                }
                timeCreateSeed = System.currentTimeMillis();
                createSeed = true;
                protoStatus(StateType.CLIENT_SKILL, dataClient.size(), dataClient);
                addTimeActive(2000);
            }
            case 2 -> { // Boss hồi 10% máu
                long reHp = (long) (0.1f * point.getMaxHp());
                List<PointBuff> buffs = new ArrayList<>();
                buffs.add(new PointBuff(Point.CUR_HP, reHp));
                protoStatus(StateType.PLAY_ANIMATION, SKILL4);
                protoBuffPoint(buffs);
                addTimeActive(2000);
            }
            case 3 -> { //Gán bom lên player, player có thể gán lên player khác, sau 10s quả bom phát nổ sát thương lớn làm player die
                EffectRoom bomb = clanRoom.createBomb();
                if (bomb == null) return;
                List<Long> data = new ArrayList<>();
                data.add((long) bomb.getSkill().getEffectType().id); // type
                data.add((long) bomb.getId()); // effect Id
                data.add((long) bomb.getTarget().getId()); // target Id
                data.add(bomb.getTimeInit()); // thời gian tạo
                data.add((long) (timeDelayBomb * 1000)); // thời gian n
                protoStatus(StateType.PLAY_ANIMATION, SKILL2);
                protoStatus(StateType.CLIENT_SKILL, data.size(), data);
                addTimeActive(3000);
            }
            case 4 -> { // tao long giam player
                protoStatus(StateType.PLAY_ANIMATION, SKILL1);
                clanRoom.addCoroutine(new Coroutine(0.5f, () -> {
                    Character target = room.getAPlayer().get(NumberUtil.getRandom(room.getAPlayer().size()));
                    if (target == null || !target.isAlive()) return;
                    Support cage = new Support(support.get(0), target.getPos(), teamId, clanRoom);
                    cage.setTargetAttack(target);
                    clanRoom.addSupport(cage);
                }));
                addTimeActive(3000);
            }
            case 5 -> { // Boss hống lên làm chậm player
                protoStatus(StateType.PLAY_ANIMATION, SKILL5);
                clanRoom.addCoroutine(new Coroutine(0.5f, () -> {
                    List<Character> lstPlayer = clanRoom.getAPlayer();
                    long timeDec = timeDecMoveSpeed * 1000L;
                    long buff = decMoveSpeed;
                    for (int i = 0; i < lstPlayer.size(); i++) {
                        Character target = lstPlayer.get(i);
                        if (target == null || !target.isAlive()) break;
                        buff = target.point.buffChange(Point.CHANGE_MOVE_SPEED, -decMoveSpeed, BattleConfig.S_maxReduce90);
                        target.protoStatus(StateType.EFFECT_BODY, Arrays.asList(EffectBodyType.SLOW.value, timeDec));
                        target.protoStatus(StateType.UPDATE_MULTI_POINT, 2, List.of((long) Point.CHANGE_MOVE_SPEED, (long) -decMoveSpeed));
                    }
                    long finalBuff = -buff;
                    clanRoom.addCoroutine(new Coroutine(timeDecMoveSpeed, () -> {
                        for (int i = 0; i < lstPlayer.size(); i++) {
                            Character target = lstPlayer.get(i);
                            if (target == null || !target.isAlive()) break;
                            target.point.buffChange(Point.CHANGE_MOVE_SPEED, finalBuff, BattleConfig.S_maxReduce90);
                            target.protoStatus(StateType.UPDATE_MULTI_POINT, 2, List.of((long) Point.CHANGE_MOVE_SPEED, finalBuff));
                        }
                    }));
                }));
                addTimeActive(3000);
            }
            case 6 -> {  // Hất tung player
                BossSkill skill = skills.get(0);
                for (int i = 0; i < clanRoom.getAPlayer().size(); i++) {
                    Character target = clanRoom.getAPlayer().get(i);
                    EffectRoom effectRoom = new EffectRoom(this, target.getPos(), skill.getEffect());
                    effectRoom.setId(getBattleRoom().getIdEffectClient());
                    effectRoom.setTarget(target);
                    getBattleRoom().addEffectClient(effectRoom);
                    // send data
                    List<Long> data = new ArrayList<>();
                    data.add((long) skill.getEffect().getEffectType().id);
                    data.add((long) effectRoom.getId());
                    data.add((long) (target.getPos().x * 1000)); // pos initsa
                    data.add((long) (target.getPos().y * 1000)); // pos init
                    data.add((long) (sizeElip2.get(0) * 1000)); //xRadius
                    data.add((long) (sizeElip2.get(1) * 1000)); // yRadius
                    data.add((long) (3000)); // y hất tung
                    protoStatus(StateType.CLIENT_SKILL, data.size(), data);
                }
                addTimeActive(2000);
            }
            case 7 -> { // buff giap cho bosss
                protoStatus(StateType.PLAY_ANIMATION, SKILL6);
                counterBullet = true;
                float timeActive = 5;
                clanRoom.addCoroutine(new Coroutine(timeActive, () -> {
                    counterBullet = false;
                }));
                clanRoom.addCoroutine(new Coroutine(0.3f, () -> {
                    protoStatus(StateType.EFFECT_BODY, Arrays.asList(EffectBodyType.SHIELD_BOSS.value, (long) (timeActive * 1000)));
                }));
                addTimeActive(3000);
            }
            case 8 -> { // Triệu hồi quái
                genEnemy(getBattleRoom(), SKILL2, 0.8f);
                addTimeActive(2000);
            }
            case 9 -> {
                // đang có cột thì bỏ qua
                if (cotInfo.stream().allMatch(i -> i == 1)) return;
                BossGodRoom bossRoom = ((BossGodRoom) room);
                protoStatus(StateType.PLAY_ANIMATION, SKILL2);
                bossRoom.addCoroutine(new Coroutine(0.5f, () -> {
                    List<ResBossEntity> support = getSupport();
                    for (int i = 1; i < support.size(); i++) {
                        int indexCot = i - 1;
                        if (cotInfo.get(indexCot) == 1) continue;
                        Support totem = new Support(support.get(i), support.get(i).getInstancePos(), teamId, bossRoom);
                        bossRoom.addSupport(totem);
                        cotInfo.set(indexCot, 1);
                        if (indexCot == 0) {
                            point.addAttack(cotAddPoint.get(indexCot));
                            point.addMagicAttack(cotAddPoint.get(indexCot));
                        } else if (indexCot == 1) point.addHpRegen(cotAddPoint.get(indexCot));
                        else if (indexCot == 2) {
                            point.addDef(cotAddPoint.get(indexCot));
                            point.addMagicResist(cotAddPoint.get(indexCot));
                        } else if (indexCot == 3) speedSkill = cotAddPoint.get(indexCot) / 100f;
                    }
                }));
                addTimeActive(3000);
            }
            case 13 -> { // boss dẫm chân hất tung thành 1 đường thẳng // làm sau
                Character target = clanRoom.getHeroMaxDamage();
                if (target == null) target = targetAttack;
                // SkillEffect
                SkillEffect skill = new SkillEffect();
                skill.setEffectType(EffectType.MOC_THAN_4);
                skill.setValues(valueDameSkill6);
                skill.setTime(3f); // stun 3s
                //EffectRoom
                EffectRoom effectRoom = new EffectRoom(this, targetAttack.getPos(), skill);
                effectRoom.setId(getBattleRoom().getIdEffectClient());
                effectRoom.setTarget(targetAttack);
                clanRoom.addEffectClient(effectRoom);
                // send data
                List<Long> data = new ArrayList<>();
                data.add((long) skill.getEffectType().id);
                data.add((long) effectRoom.getId());
                data.add((long) (getPos().x * 1000)); // pos init
                data.add((long) (getPos().y * 1000)); // pos init
                Pos direction = MathLab.getDirection(pos, target.getPos());
                data.add((long) (direction.x * 1000)); // direction
                data.add((long) (direction.y * 1000)); // direction
                data.add(3 * 1000L); // range fly
                protoStatus(StateType.CLIENT_SKILL, data.size(), data);
                addTimeActive(5000);
                // 3s sau xoá luôn effect client
                clanRoom.addCoroutine(new Coroutine(5f, () -> {
                    if (clanRoom.getMEffectClient().containsKey(effectRoom.getId()))
                        clanRoom.getMEffectClient().remove(effectRoom.getId());
                }));
            } //todo

            case 14 -> { //Thủy triều dữ dội  -> phát nổ 3 lần xung quanh b0ss
                addTimeActive(3000);
                // SkillEffect
                SkillEffect skill = new SkillEffect();
                skill.setEffectType(EffectType.MOC_THAN_5);
                skill.setValues(valueDameSkill8);
                //EffectRoom
                EffectRoom effectRoom = new EffectRoom(this, pos, skill);
                effectRoom.setId(getBattleRoom().getIdEffectClient());
                clanRoom.addEffectClient(effectRoom);
                // send data
                List<Long> data = new ArrayList<>();
                data.add((long) skill.getEffectType().id);
                data.add((long) effectRoom.getId());
                data.add((long) (getPos().x * 1000)); // pos init
                data.add((long) (getPos().y * 1000)); // pos init
                protoStatus(StateType.CLIENT_SKILL, data.size(), data);
                // 3s sau xoá luôn effect client
                clanRoom.addCoroutine(new Coroutine(5f, () -> {
                    if (clanRoom.getMEffectClient().containsKey(effectRoom.getId()))
                        clanRoom.getMEffectClient().remove(effectRoom.getId());
                }));
            }
            case 10 -> {// Tạo các vùng nổ gây sát thương lên player, mỗi player là 1 quả cầu nổ
                addTimeActive(3000);
                List<Character> lstPlayer = clanRoom.getAPlayer();
                for (int i = 0; i < lstPlayer.size(); i++) {
                    Character target = lstPlayer.get(i);
                    if (target == null && !target.isAlive()) break;
                    // SkillEffect
                    SkillEffect skill = new SkillEffect();
                    skill.setEffectType(EffectType.MOC_THAN_6);
                    skill.setValues(valueDameSkill9);
                    //EffectRoom
                    EffectRoom effectRoom = new EffectRoom(this, target.getPos(), skill);
                    effectRoom.setId(getBattleRoom().getIdEffectClient());
                    clanRoom.addEffectClient(effectRoom);
                    // send data
                    List<Long> data = new ArrayList<>();
                    data.add((long) skill.getEffectType().id);
                    data.add((long) effectRoom.getId());
                    data.add((long) (getPos().x * 1000)); // pos init
                    data.add((long) (getPos().y * 1000)); // pos init
                    protoStatus(StateType.CLIENT_SKILL, data.size(), data);
                    // 5s sau xoá luôn effect client
                    clanRoom.addCoroutine(new Coroutine(5f, () -> {
                        if (clanRoom.getMEffectClient().containsKey(effectRoom.getId()))
                            clanRoom.getMEffectClient().remove(effectRoom.getId());
                    }));
                }
            }
            case 11 -> { // Ném quả cầu lên player và nảy qua player khác ở gần
                addTimeActive(3000);
                // SkillEffect
                SkillEffect skill = new SkillEffect();
                skill.setEffectType(EffectType.MOC_THAN_7);
                skill.setValues(valueDameSkill10);
                //EffectRoom
                EffectRoom effectRoom = new EffectRoom(this, targetAttack, skill);
                effectRoom.setId(getBattleRoom().getIdEffectClient());
                clanRoom.addEffectClient(effectRoom);
                // send data
                List<Long> data = new ArrayList<>();
                data.add((long) skill.getEffectType().id);
                data.add((long) effectRoom.getId());
                data.add((long) 5); // số lần nảy
                data.add((long) 3); // tầm bay nảy
                data.add((long) 1); // thời gian tồn tại
                data.add((long) targetAttack.getId()); // target id
                data.add((long) (getPos().x * 1000)); // pos init
                data.add((long) (getPos().y * 1000)); // pos init
                protoStatus(StateType.CLIENT_SKILL, data.size(), data);
                // 15s sau xoá luôn effect client
                clanRoom.addCoroutine(new Coroutine(15f, () -> {
                    if (clanRoom.getMEffectClient().containsKey(effectRoom.getId()))
                        clanRoom.getMEffectClient().remove(effectRoom.getId());
                }));
            }
            case 12 -> { // Vòng xoáy tuyệt vọng
                addTimeActive(3000);
                // SkillEffect
                SkillEffect skill = new SkillEffect();
                skill.setEffectType(EffectType.MOC_THAN_8);
                skill.setValues(valueDameSkill12);
                //EffectRoom
                EffectRoom effectRoom = new EffectRoom(this, targetAttack, skill);
                effectRoom.setId(clanRoom.getIdEffectClient());
                clanRoom.addEffectClient(effectRoom);
                // send data
                List<Long> data = new ArrayList<>();
                data.add((long) skill.getEffectType().id);
                data.add((long) effectRoom.getId());
                data.add((long) (getPos().x * 1000)); // pos init
                data.add((long) (getPos().y * 1000)); // pos init
                protoStatus(StateType.CLIENT_SKILL, data.size(), data);
                // 10s sau xoá luôn effect client
                clanRoom.addCoroutine(new Coroutine(10f, () -> {
                    if (clanRoom.getMEffectClient().containsKey(effectRoom.getId()))
                        clanRoom.getMEffectClient().remove(effectRoom.getId());
                }));
            }
        }
    }

    public void removeSupport(int index) {
        switch (index){
            case 0 ->{
                point.addAttack(-cotAddPoint.get(index));
                point.addMagicAttack(-cotAddPoint.get(index));
            }
            case 1 ->point.addHpRegen(-cotAddPoint.get(index));
            case 2 ->{
                point.addDef(-cotAddPoint.get(index));
                point.addMagicResist(-cotAddPoint.get(index));
            }
            case 3 ->speedSkill =1;
        }
    }

    private void processEffectClient() {
        if (room.getRoomState() == RoomState.END || !isAlive() || !checkStart()) {
            return;
        }
        if (createSeed && DateTime.isAfterTime(timeCreateSeed, timeAliveSeed)) {
            createSeed = false;

            try {
                for (var entry : clanRoom.getSeedMoc().entrySet()) {
                    SeedMocThan seed = entry.getValue();
                    if (seed != null && seed.alive) {
                        Enemy bot = new Enemy(treeEnemy, seed.getPos(), Pos.RandomDirection(), 2, clanRoom);
                        clanRoom.getAEnemy().add(bot);
                        clanRoom.getAProtoAdd().add(bot.toProtoAdd());
                    }
                }
                clanRoom.getSeedMoc().clear();
                List<Long> data = List.of((long) EffectType.MOC_THAN_2.id, -1L);
                protoStatus(StateType.CLIENT_SKILL, data.size(), data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (clanRoom.getBomb() != null && clanRoom.getBomb().canActiveByTime(timeDelayBomb)) {
            EffectRoom bomb = clanRoom.getBomb();
            Character target = clanRoom.getBomb().getTarget();

            if (target.isAlive()) {
                long atkBomb = (long) (bomb.getSkill().getFirstPer() * bomb.getOwner().getPoint().getAttackDamage());
                long magBomb = (long) (bomb.getSkill().getNextPer() * bomb.getOwner().getPoint().getMagicDamage());

                for (Character p : room.getAPlayer()) {
                    if (p.inSizeHit(target.getPos(), EffectType.MOC_THAN_3.radius)) {
                        p.beAttackEffect(bomb, atkBomb, magBomb);
                    }
                }
            }
            clanRoom.removeBomb();
        }

        for (int i = 0; i < clanRoom.getSupports().size(); i++) {
            Support cage = clanRoom.getSupports().get(i);
            if (cage != null && cage.alive && cage.type == CharacterType.CAGE && DateTime.isAfterTime(cage.timeJoinRoom, timeDelayCage)) {
                cage.getTargetAttack().beAttackDamage(cage, 0L, 99999999L);
                clanRoom.removeSupport(cage);
            }
        }
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
                bot.getPoint().buffPer((clanRoom.getClan().getBossLevel() + 1) * 50);
                bot.setBeAttack(true);
                room.getAEnemy().add(bot);
                room.getAProtoAdd().add(bot.toProtoAdd());
            }
        }));
    }


    public void beAttackBullet(Bullet bullet) {
        if (!canBeAttack(bullet.getOwner().getTeamId())) return;
        if (bullet.getCharacterAttack().contains(id)) return;
        if (counterBullet) { // phản đòn
            long[] damage = IMath.calculateDamage(bullet, bullet.getOwner(), new ArrayList<>());
            damage[1] = (long) (damage[1] * 0.03);
            damage[2] = (long) (damage[2] * 0.03);
            bullet.getOwner().updateHp(this, -damage[1], -damage[2]);
            bullet.getOwner().protoBeDameEffect(Arrays.asList((long) getId(), -damage[0], -damage[1]));
        } else {
            bullet.minusPenetration(id);
            long[] damage = new long[3];
            List<Long> effs = new ArrayList<>();
            List<Long> eff = processTrigger(TriggerType.HIT, bullet);
            if (!eff.isEmpty()) effs.addAll(eff);
            if (triggerFirstAttack(bullet.getOwner(), bullet)) {
                eff = processTrigger(TriggerType.FIRST_HIT, bullet);
                if (!eff.isEmpty()) effs.addAll(eff);
            }
            if (bullet.isTrigger3TH()) {
                eff = processTrigger(TriggerType.TH3, bullet);
                if (!eff.isEmpty()) effs.addAll(eff);
            }

            if (bullet.isTrigger4TH()) {
                processTrigger(TriggerType.TH4, bullet);
            }
            if (bullet.isTrigger5TH()) {
                eff = processTrigger(TriggerType.TH5, bullet);
                if (!eff.isEmpty()) effs.addAll(eff);
            }
            damage = IMath.calculateDamage(bullet, this, effs);
            addAtkInfoMelee(bullet);
            updateHp(bullet.getOwner(), -damage[1], -damage[2]);
            processTriggerLastDame(bullet.getOwner(), bullet.getEffectSkill(), damage, bullet.getFaction());
            protoRangeDame(bullet.getOwner(), Arrays.asList((long) bullet.getOwner().getId(), damage[0], damage[1], damage[2], (long) bullet.getFaction().value, (long) (pos.x * 1000), (long) (pos.y * 1000)));
            if (point.getCurHP() <= 0) {
                alive = false;
                timeDie = System.currentTimeMillis();
                protoDie(bullet.getOwner());
                if (checkHasBonusKill()) // tránh trường hợp gửi nhiều lần
                    bonusKillMe(bullet.getOwner());
            } else {
                isBeAttack = true;
                timeBeHit = System.currentTimeMillis();
                targetAttack = bullet.getOwner();
            }
        }
    }
}
