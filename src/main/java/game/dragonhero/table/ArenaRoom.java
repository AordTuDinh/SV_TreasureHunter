package game.dragonhero.table;

import game.battle.effect.Effect;
import game.battle.effect.EffectRoom;
import game.battle.effect.SkillEffect;
import game.battle.model.ArenaHero;
import game.battle.model.Character;
import game.battle.object.*;
import game.battle.type.CharacterType;
import game.battle.type.RoomState;
import game.battle.type.StateType;
import game.config.CfgArena;
import game.config.CfgBattle;
import game.config.aEnum.DetailActionType;
import game.config.aEnum.PopupType;
import game.dragonhero.BattleConfig;
import game.dragonhero.mapping.*;
import game.dragonhero.server.Constans;
import game.dragonhero.server.IAction;
import game.dragonhero.service.Services;
import game.dragonhero.service.user.Bonus;
import game.object.MyUser;
import game.object.TaskMonitor;
import game.protocol.CommonProto;
import game.protocol.ProtoState;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.Util;
import ozudo.base.log.Logs;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

import static ozudo.base.database.DBJPA.closeSession;


public class ArenaRoom extends BaseBattleRoom {
    BattleTeam team1, team2;
    List<Character> heroTeam1;
    List<Character> heroTeam2;
    MyUser mUser;
    int team1Die, team2Die;
    boolean isWin;
    int targetId;
    long curHpTeam1, curHpTeam2, maxHpTeam1, maxHpTeam2;
    // info
    int myElo, oppElo;
    UserArenaEntity myArena, oppArena;
    UserEntity uOpp;
    ArenaHero pet1, pet2;
    static final int pet1Index = 3, pet2Index = 8;

    public ArenaRoom(MyUser mUser, String keyRoom, BattleTeam team1, BattleTeam team2, int targetId, int myElo, int oppElo, UserArenaEntity myArena, UserArenaEntity oppArena, UserEntity uOpp) {
        this.mUser = mUser;
        this.targetId = targetId;
        this.uOpp = uOpp;
        this.myElo = myElo;
        this.myArena = myArena;
        this.keyRoom = keyRoom;
        this.keys = keyRoom.split("_");
        this.oppArena = oppArena;
        this.oppElo = oppElo;
        this.team1 = team1;
        this.team2 = team2;
        this.id = getCounterId();
        this.aPlayer = new ArrayList<>();
        this.aPet = new ArrayList<>();
        this.timeCreateRoom = System.currentTimeMillis();
        this.roomState = RoomState.INIT;
        this.controller = new GameCore();
        this.aProtoAdd = new ArrayList<>();
        this.aProtoUnitState = new ArrayList<>();
        this.coroutines = new ArrayList<>();
        this.team1Die = 0;
        this.team2Die = 0;
        // init team 1
        heroTeam1 = new ArrayList<>();
        for (int i = 0; i < team1.getBattleHeroes().length; i++) {
            HeroBattle hBattle = team1.getBattleHeroes()[i];
            if (hBattle == null) continue;
            ArenaHero hero = hBattle.toArenaHero(this, 1);
            if (hero != null) {
                if (hero.getType() == CharacterType.HERO) {
                    heroTeam1.add(hero);
                    curHpTeam1 += hero.getPoint().getMaxHp();
                } else if (hero.getType() == CharacterType.PET) pet1 = hBattle.toArenaPet(this);
            }
        }
        maxHpTeam1 = curHpTeam1;
        // init team 2
        heroTeam2 = new ArrayList<>();
        for (int i = 0; i < team2.getBattleHeroes().length; i++) {
            HeroBattle hBattle = team2.getBattleHeroes()[i];
            if (hBattle == null) continue;
            ArenaHero hero = hBattle.toArenaHero(this, 2);
            if (hero != null) {
                if (hero.getType() == CharacterType.HERO) {
                    heroTeam2.add(hero);
                    curHpTeam2 += hero.getPoint().getMaxHp();
                } else if (hero.getType() == CharacterType.PET) pet2 = hBattle.toArenaPet(this);
            }
        }
        maxHpTeam2 = curHpTeam2;
        // job
        key1 = TaskMonitor.getInstance().submit(this, CfgBattle.periodUpdate);
        key2 = TaskMonitor.getInstance().submit(this, CfgBattle.periodFixedUpdate);
        key3 = TaskMonitor.getInstance().submit(this, CfgBattle.periodUpdateLow);
        key4 = TaskMonitor.getInstance().submit(this, CfgBattle.periodEffectUpdate);
        key5 = TaskMonitor.getInstance().submit(this, CfgBattle.periodUpdate1s);
    }


    public void startGame() {
        timeStartGame = System.currentTimeMillis();
    }


    @Override
    public void Update1s() {
        for (int i = 0; i < heroTeam1.size(); i++) {
            heroTeam1.get(i).Update1s();
        }
        for (int i = 0; i < heroTeam2.size(); i++) {
            heroTeam2.get(i).Update1s();
        }
    }

    @Override
    public void EffectUpdate() {
        if (roomState != RoomState.ACTIVE) return;
        EffUpdate();
        processPet(pet1);
        processPet(pet2);
    }


    public void EffUpdate() { // 0.5s gọi 1 lần
        for (int i = 0; i < getAEffectRoom().size(); i++) {
            EffectRoom eff = getAEffectRoom().get(i);
            if (eff != null) processRoomByTime(eff);
        }
    }


    public void processRoomByTime(EffectRoom eff) { // 0.5s gọi 1 lần
        if (getRoomState() != RoomState.ACTIVE) return;
        boolean hasRemove = false; // lười thêm vào list nên làm cách này cho tiện - có thể check theo effect type nhưng mà khi thêm eff lại phải thêm vào list => lười
        List<Character> target = eff.getOwner().getTeamId() == 1 ? heroTeam2 : heroTeam1;
        List<Character> myTeam = eff.getOwner().getTeamId() == 1 ? heroTeam1 : heroTeam2;
        switch (eff.getSkill().getEffectType()) {
            case SANDSTORM: // process in room
                if (!eff.checkActiveTime()) {
                    for (int i = 0; i < target.size(); i++) {
                        Character enemy = target.get(i);
                        if (canHitEffectRoom(eff, enemy)) {
                            enemy.addEffectTime(eff.clone());
                        }
                    }
                }
                long magDame = (long) (eff.getSkill().getFirstPer() * eff.getOwner().getPoint().getMagicDamage());
                for (int i = 0; i < target.size(); i++) {
                    Character enemy = target.get(i);
                    if (canHitEffectRoom(eff, enemy)) {
                        enemy.beAttackEffect(eff, 0L, magDame / 2);
                    }
                }
                hasRemove = true;
                break;
            case BLIZZARD: // process in room
                if (!eff.checkActiveTime()) {
                    for (int i = 0; i < target.size(); i++) {
                        Character enemy = target.get(i);
                        if (canHitEffectRoom(eff, enemy)) {
                            Effect effDec = eff.clone(BattleConfig.S_timeDecBlizzard);
                            enemy.addEffectTime(effDec);
                        }
                    }
                }
                if (eff.canActiveByAnim()) {
                    long atkBliz = (long) (eff.getSkill().getFirstPer() * eff.getOwner().getPoint().getMagicDamage());
                    for (int i = 0; i < target.size(); i++) {
                        Character enemy = target.get(i);
                        if (canHitEffectRoom(eff, enemy)) {
                            enemy.beAttackEffect(eff, 0l, atkBliz / 2);
                        }
                    }
                }
                hasRemove = true;
                break;
            case INF: // process in room
                eff.checkActiveTime();
                if (eff.canActiveByAnim()) { // delay by animation
                    for (int i = 0; i < target.size(); i++) {
                        Character enemy = target.get(i);
                        if (canHitEffectRoom(eff, enemy)) {
                            Effect effBody = eff.clone(BattleConfig.S_timePoinson);
                            enemy.addEffectTime(effBody); // phải add eff khác để còn trừ time khác nhau
                        }
                    }
                }
                hasRemove = true;
                break;
//            case OGAMA_SKILL_1: // process in room
//                if (!eff.isActive()) {
//                    eff.getOwner().protoStatus(StateType.EFFECT, eff.toStateType());
//                    eff.active();
//                    eff.setTimeRealActive();
//                }
//                if (eff.canActiveByAnim()) { // delay by animation
//                    for (Character player : room.getAPlayer()) {
//                        if (canHitEffectRoom(eff, player)) {
//                            Effect effBody = eff.clone(BattleConfig.S_timePoisonOgama);
//                            player.addEffectTime(effBody); // phải add eff khác để còn trừ time khác nhau
//                        }
//                    }
//                }
//                hasRemove = true;
//                break;

            case SMOKE: // process in room
                eff.checkActiveTime();
                if (eff.canActiveByAnim()) {
                    for (int i = 0; i < myTeam.size(); i++) {
                        Character hero = target.get(i);
                        if (canHitEffectRoom(eff, hero)) {
                            Effect newEff = eff.clone();
                            hero.addEffectTime(newEff);
                        }
                    }
                }
                hasRemove = true;
                break;
        }
        //Fixme DATE: 7/31/2022 LƯU Ý ---> kiểm tra tồn tại, cần check có đang chờ dame không, nếu đang chò dame thì k đc xóa
        boolean checkExits = eff.checkExist(CfgBattle.decTimeEffRoom);
        if (eff.canActiveByAnim() && !checkExits && hasRemove) {
            removeEffect(eff);
        }
    }

    private boolean canHitEffectRoom(EffectRoom eff, Character target) {
        return target.canReceiveEffect(eff.getSkill().getEffectType()) && !eff.sameTeam(target) && target.isAlive() && target.inSizeHit(eff.getInstancePos(), eff.getSkill().getEffectType().radius);
    }

    @Override
    public void Update() {
//        System.out.println("Update --------");
        Update(this);
        // delay active game
        if (roomState == RoomState.INIT && System.currentTimeMillis() > timeStartGame && timeStartGame != 0) {
            this.roomState = RoomState.ACTIVE;
        }
        if (roomState != RoomState.ACTIVE) return;
        long _dt = System.currentTimeMillis() - _dte;
        _dte = System.currentTimeMillis();
        local_time += _dt / 1000.0;
        server_time = local_time;
        processHero();
        sendArenaState();
        if (roomState == RoomState.ACTIVE && ((DateTime.isAfterTime(timeStartGame, CfgArena.maxTimeAttack) || team1Die == heroTeam1.size() || team2Die == heroTeam2.size()))) {
            roomState = RoomState.END;
            isWin = team2Die == heroTeam2.size();
            endGame(true);
        }
    }

    public void Update(BaseBattleRoom room) {
        if (room.getRoomState() != RoomState.ACTIVE) return;
        for (int i = 0; i < room.getAEffectRoom().size(); i++) {
            EffectRoom eff = room.getAEffectRoom().get(i);
            controller.processEffInRoom(room, eff, heroTeam1, heroTeam2);
        }
    }

    public void endBattle() {
        isWin = false;
        this.roomState = RoomState.END;
        endGame(true);
    }

    private void endGame(boolean isSend) {
        timeEndGame = System.currentTimeMillis();
        toPbEndGame(isSend);
        addCoroutine(new Coroutine(3f, this::cancelTask));
    }

    public void toPbEndGame(boolean send) {
        int star = 0;
        int[] eloPoint = CfgArena.getRating(myElo, oppElo, isWin);
        if (isWin) {
            if (team1Die == 0) star++;
            if (star == 1 && heroTeam1.stream().filter(hero -> hero.getPoint().getPerHp() > 50).count() == heroTeam1.size())
                star++;
            if (star == 2 && myElo < oppElo) star++;
            eloPoint[0] += (int) (CfgArena.pointPerBonusStar * star);
        }
        UserInt uInt = mUser.getUData().getUInt();
        boolean isFirstAttack = uInt.getValue(UserInt.FIRST_ATTACK_ARENA) == 0;
        int p1 = eloPoint[0];
        int p2 = eloPoint[1];
        int timeAttack = getTimeAttack();
        if (send) { // disconnect thì k send mà chỉ update db
            protocol.Pbmethod.PbEndGame.Builder pb = protocol.Pbmethod.PbEndGame.newBuilder();
            pb.setBattleKey("");
            pb.setIsWin(isWin);
            pb.setStar(star);
            pb.setInfo(CommonProto.getCommonVector(p1, p2));
            pb.setPopupId(PopupType.POPUP_END_ARENA.value);
            pb.addAllBonus(Bonus.receiveListItem(mUser, DetailActionType.BONUS_ATTACK_ARENA.getKey(targetId), CfgArena.getBonusArena(myElo, isWin, p1, isFirstAttack)));
            pb.setTime(timeAttack);
            pb.setPerDame(100);
            Util.sendProtoData(mUser.getChannel(), pb.build(), IAction.END_GAME);
        }
        if (isFirstAttack) {
            uInt.setValueAndUpdate(UserInt.FIRST_ATTACK_ARENA, 1);
        }
        //update to db
        if (dbUpdateArena(mUser.getUser().getId(), p1, targetId, p2, isWin, timeAttack, p1, p2)) {
            myArena.addArenaPoint(p1);
            oppArena.addArenaPoint(p2);
        }

        // event 7 day attack boss day 2
        UserEventSevenDayEntity uEvent = Services.userDAO.getUserSevenDay(mUser);
        if (isWin && uEvent.hasEvent() && uEvent.hasActive(2) && uEvent.update(List.of("attack_arena", uEvent.getAttackArena() + 1))) {
            uEvent.setAttackArena(uEvent.getAttackArena() + 1);
        }
    }

    private void processPet(ArenaHero pet) {
        if (pet != null) {
            SkillEffect eff = pet.getPetSkills();
            List<Character> heroBuff = pet.getTeamId() == 1 ? heroTeam1 : heroTeam2;
            if (eff != null && DateTime.isAfterTime(pet.getTimePetAttack(), eff.getTime())) {
                pet.setTimePetAttack();
                addState(StateType.PET_USE_SKILL, List.of((long) pet.getTeamId()));
                addCoroutine(new Coroutine(eff.getTimeDelayDame(), () -> {
                    switch (eff.getEffectType()) {
                        case PET_RE_HP -> { // create
                            for (int i = 0; i < heroBuff.size(); i++) {
                                ArenaHero hero = (ArenaHero) heroBuff.get(i);
                                long reHp1 = (long) (eff.getFirstPer() * hero.getPoint().getMaxHp());
                                long reMaxHp = (long) (BattleConfig.S_maxReHp75 / 100f * hero.getPoint().getMaxHp());
                                reHp1 = reHp1 > reMaxHp ? reMaxHp : reHp1;
                                hero.reHp(reHp1);
//                                hero.protoStatus(StateType.RE_HP, reHp1);
                            }
                        }
                        case PET_BUFF_SHELL -> {//
                            for (int i = 0; i < heroBuff.size(); i++) {
                                ArenaHero hero = (ArenaHero) heroBuff.get(i);
                                long shell = (long) (eff.getFirstPer() * hero.getPoint().getMaxHp());
                                long maxShell = (long) (BattleConfig.S_maxReHp75 / 100f * hero.getPoint().getMaxHp());
                                shell = shell > maxShell ? maxShell : shell;
                                hero.buffShell(shell);
                                addCoroutine(new Coroutine(pet.getTimePetActive(), () -> {
                                    long curShell = hero.getPoint().getCurShell();
                                    if (curShell > 0) hero.protoBuffPoint(Point.SHELL, -curShell);
                                }));
                            }
                        }
                        case PET_BUFF_ATK ->
                                buffPointPet(heroBuff, Point.CHANGE_ATTACK, eff, BattleConfig.S_maxReduce90, pet);
                        case PET_BUFF_MATK ->
                                buffPointPet(heroBuff, Point.CHANGE_MAGIC_ATTACK, eff, BattleConfig.S_maxReduce90, pet);
                        case PET_BUFF_DEF ->
                                buffPointPet(heroBuff, Point.CHANGE_DEFENSE, eff, BattleConfig.S_maxReduce90, pet);
                        case PET_BUFF_MAGIC_RESIST ->
                                buffPointPet(heroBuff, Point.CHANGE_MAGIC_RESIST, eff, BattleConfig.S_maxReduce90, pet);
                        case PET_BUFF_CRIT ->
                                buffPointPet(heroBuff, Point.CHANGE_CRIT, eff, BattleConfig.S_maxReduce90, pet);
                        case PET_BUFF_CRIT_DAME ->
                                buffPointPet(heroBuff, Point.CHANGE_CRIT_DAMAGE, eff, BattleConfig.S_maxReduce90, pet);
                        case PET_BUFF_DEC_DAME ->
                                buffPointPet(heroBuff, Point.CHANGE_DAME, eff, BattleConfig.S_maxReduce90, pet);
                        case PET_BUFF_TRUE_DAME -> {
                            for (int i = 0; i < heroBuff.size(); i++) {
                                ArenaHero hero = (ArenaHero) heroBuff.get(i);
                                hero.getPoint().setTrueDame(true);
                                addCoroutine(new Coroutine(pet.getTimePetActive(), () -> hero.getPoint().setTrueDame(false)));
                            }
                        }
                        case PET_BUFF_DODGE -> {
                            for (int i = 0; i < heroBuff.size(); i++) {
                                ArenaHero hero = (ArenaHero) heroBuff.get(i);
                                hero.protoBuffPoint(Point.DOGE, (long) eff.getFirstValues());
                                addCoroutine(new Coroutine(pet.getTimePetActive(), () -> {
                                    hero.protoBuffPoint(Point.DOGE, -(long) eff.getFirstValues());
                                }));
                            }
                        }
                        case PET_BUFF_SPD -> {
                            eff.setValues(0,-eff.getFirstValues());
                            buffPointPet(heroBuff, Point.CHANGE_ATTACK_SPEED, eff, BattleConfig.S_maxReduce90, pet);
                        }
                        case PET_BUFF_DAMAGE -> {//
                            for (int i = 0; i < heroBuff.size(); i++) {
                                ArenaHero hero = (ArenaHero) heroBuff.get(i);
                                long realBuff1 = hero.getPoint().buffChange(Point.CHANGE_ATTACK, (long) eff.getFirstValues(), BattleConfig.S_maxReduce90);
                                long realBuff2 = hero.getPoint().buffChange(Point.CHANGE_MAGIC_ATTACK, (long) eff.getValueIndex(1), BattleConfig.S_maxReduce90);

                                addCoroutine(new Coroutine(pet.getTimePetActive(), () -> {
                                    hero.getPoint().buffChange(Point.CHANGE_ATTACK, -realBuff1, BattleConfig.S_maxReduce90);
                                    hero.getPoint().buffChange(Point.CHANGE_MAGIC_ATTACK, -realBuff2, BattleConfig.S_maxReduce90);
                                }));
                            }
                        }
                        case PET_DEF_REST -> {//
                            for (int i = 0; i < heroBuff.size(); i++) {
                                ArenaHero hero = (ArenaHero) heroBuff.get(i);
                                long realBuff1 = hero.getPoint().buffChange(Point.CHANGE_DEFENSE, (long) eff.getFirstValues(), BattleConfig.S_maxReduce90);
                                long realBuff2 = hero.getPoint().buffChange(Point.CHANGE_MAGIC_RESIST, (long) eff.getValueIndex(1), BattleConfig.S_maxReduce90);

                                addCoroutine(new Coroutine(pet.getTimePetActive(), () -> {
                                    hero.getPoint().buffChange(Point.CHANGE_DEFENSE, -realBuff1, BattleConfig.S_maxReduce90);
                                    hero.getPoint().buffChange(Point.CHANGE_MAGIC_RESIST, -realBuff2, BattleConfig.S_maxReduce90);
                                }));
                            }
                        }
                    }
                }));
            }
        }
    }


    private void buffPointPet(List<Character> heroBuff, int pointId, SkillEffect eff, int reMax, ArenaHero pet) {
        for (int i = 0; i < heroBuff.size(); i++) {
            ArenaHero hero = (ArenaHero) heroBuff.get(i);
            long realBuff = hero.getPoint().buffChange(pointId,(long) eff.getFirstValues(), reMax);
            addCoroutine(new Coroutine(pet.getTimePetActive(), () -> {
                hero.getPoint().buffChange(pointId, -realBuff, reMax);
            }));
        }
    }

    public int getTimeAttack() { // seconds
        return (int) ((timeEndGame - timeStartGame) / 1000f);
    }

    @Override
    protected byte[] genTableState() {
        int action = IAction.TABLE_STATE;// K dùng nhưng viết ở đây để referent
        protocol.Pbmethod.PbState.Builder builder = protocol.Pbmethod.PbState.newBuilder();
        builder.setServerTime(server_time);
        String debug = "";
        boolean send = false;
        int size = aProtoAdd.size();
        for (int i = 0; i < size; i++) {
            builder.addUnitAdd(aProtoAdd.get(0));
            aProtoAdd.remove(0);
            send = true;
        }
        if (!aProtoUnitState.isEmpty()) {
            builder.addAUnitUpdate(ProtoState.protoUnitUpdate(Constans.TYPE_UPDATE_CHARACTER, ProtoState.protoListCharacterState(aProtoUnitState)));
            send = true;
            aProtoUnitState.clear();
        }
        if (send) return ProtoState.convertProtoBuffToState(builder.build());
        else return null;
    }


    // process hero
    private void processHero() {
        processTeam(heroTeam1);
        processTeam(heroTeam2);
    }

    @Override
    public void ChangeCharacterHp(Character attacker, Character beDamage, long atk, long mAtk) {
        if (beDamage.getTeamId() == 1) {
            curHpTeam1 += (atk + mAtk);
            addState(StateType.UPDATE_PER_HP_ARENA, List.of(1L, (curHpTeam1 * 100 / maxHpTeam1)));
        } else {
            curHpTeam2 += (atk + mAtk);
            addState(StateType.UPDATE_PER_HP_ARENA, List.of(2L, (curHpTeam2 * 100 / maxHpTeam2)));
        }
    }

    private void addState(StateType stateType, List<Long> data) {
        aProtoUnitState.add(protoState(-1, List.of(stateType), data));
    }

    private void processTeam(List<Character> team) {
        for (int i = 0; i < team.size(); i++) {
            ArenaHero hero = (ArenaHero) team.get(i);
            if (hero.getType() == CharacterType.HERO) {
                if (hero.isAlive() && !hero.beBlock() && DateTime.isAfterTime(hero.getTimeAttack(), hero.getPoint().getAttackSpeed())) {
                    // check target attack
                    if (hero.getTargetAttack() == null || !hero.getTargetAttack().isAlive()) {
                        ArenaHero target = findTargetAttack(hero);
                        hero.setTargetAttack(target);
                    }
                    // create shuriken
                    addBullet(hero.attackTarget());
                }
            }
        }
    }



    public synchronized void addBullet(List<Bullet> lstB) {
        if (roomState != RoomState.ACTIVE) return;
        aBullets.addAll(lstB);
        if (aProtoAdd != null) lstB.forEach(bu -> aProtoAdd.add(bu.toProtoAdd()));
    }

    private ArenaHero findTargetAttack(ArenaHero hero) {
        List<Character> targets = hero.getTeamId() == 1 ? heroTeam2 : heroTeam1;
        for (int i = 0; i < targets.size(); i++) {
            if (targets.get(i).isAlive()) {
                return (ArenaHero) targets.get(i);
            }
        }
        return null;
    }

    @Override
    public synchronized void FixedUpdate() {
//        for (int i = 0; i < coroutines.size(); i++) {
//            Coroutine coroutine = coroutines.get(i);
//            if (System.currentTimeMillis() > coroutine.timeAction) {
//                coroutine.action.Call();
//                removeCoroutine(coroutine);
//            }
//        }
        checkHit();
        process_bullet();
    }

    public void process_bullet() {
        for (int i = 0; i < aBullets.size(); i++) {
            Bullet b = aBullets.get(i);
            if (b.isAlive()) {
                b.move();
            } else {
                removeBullet(b);
            }
        }
    }

    @Override
    public void characterDie(Character character) {
        if (character.getTeamId() == 1) team1Die++;
        if (character.getTeamId() == 2) team2Die++;
    }

    public void checkHit() {
        if (roomState != RoomState.ACTIVE) return;
        for (int i = 0; i < aBullets.size(); i++) {
            Bullet b = aBullets.get(i);
            List<Character> lstCheck = b.getOwner().getTeamId() == 1 ? heroTeam2 : heroTeam1;
            for (int j = 0; j < lstCheck.size(); j++) {
                Character hero = lstCheck.get(j);
                if (hero.inSizeHitBullet(b)) {
                    hero.beAttackBullet(b);
                    if (b.initDone()) {
                        b.setHit();
                        b.setAlive(false);
                        removeBullet(b);
                    }
                }
            }
        }
    }

    protected void clearRoom() {
        for (int i = 0; i < aBullets.size(); i++) {
            removeBullet(aBullets.get(i));
        }
        for (int i = 0; i < aEffectRoom.size(); i++) {
            removeEffect(aEffectRoom.get(i));
        }
        aEffectRoomInfo.clear();
    }

    @Override
    public void LastUpdate() {
        if (roomState == RoomState.ACTIVE) controller.LastUpdate(this);
        if (roomState != RoomState.END && mUser.getChannel().isOpen() == false) endGame(false);
    }


    private void sendArenaState() {
        byte[] data = genTableState();
        if (data == null || mUser.getChannel() == null) return;
        Util.sendGameData(mUser.getChannel(), data, Constans.MAGIC_IN_PUT);
    }


    private boolean dbUpdateArena(int u1Id, int p1, int u2Id, int p2, boolean isWin, int timeAttack, int elo1,
                                  int elo2) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            session.getTransaction().begin();
            session.createNativeQuery("UPDATE dson.user_arena SET arena_point =arena_point+" + p1 + " WHERE user_id=" + u1Id).executeUpdate();
            session.createNativeQuery("UPDATE dson.user_arena SET arena_point =arena_point+" + p2 + " WHERE user_id=" + u2Id).executeUpdate();
            UserArenaHistoryEntity history = new UserArenaHistoryEntity(mUser.getUser(), uOpp, isWin ? 1 : 0, timeAttack, elo1, elo2, myArena, oppArena);
            session.persist(history);
            session.getTransaction().commit();
            return true;

        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            closeSession(session);
        }
        return false;
    }

}
