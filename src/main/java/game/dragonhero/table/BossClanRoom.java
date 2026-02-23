package game.dragonhero.table;

import game.battle.effect.EffectRoom;
import game.battle.effect.SkillEffect;
import game.battle.model.*;
import game.battle.model.Character;
import game.battle.object.Coroutine;
import game.battle.object.Pos;
import game.battle.type.CharacterType;
import game.battle.type.EffectBodyType;
import game.battle.type.RoomState;
import game.battle.type.StateType;
import game.config.CfgClan;
import game.config.aEnum.DetailActionType;
import game.config.aEnum.ItemKey;
import game.config.aEnum.PopupType;
import game.dragonhero.mapping.ClanEntity;
import game.dragonhero.mapping.main.BaseMap;
import game.dragonhero.mapping.main.ResMapBossEntity;
import game.dragonhero.service.battle.EffectStatus;
import game.dragonhero.service.battle.EffectType;
import game.dragonhero.service.user.Bonus;
import lombok.Data;
import lombok.Getter;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.NumberUtil;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static game.battle.model.BossClan.timeDelayBomb;


public class BossClanRoom extends BossGodRoom {
    @Getter
    protected static final float timeOut = 300;
    protected static final int team = 2;
    Map<Integer, Long> damageHit = new HashMap<>();
    @Getter
    Map<Integer, SeedMocThan> seedMoc = new HashMap<>();
    List<Integer> idGetBonus = new ArrayList<>();
    @Getter
    EffectRoom bomb;
    long timeSwitchBomb;
    float timeDelaySwitchBomb = 0.5f;
    @Getter
    List<Support> supports = new ArrayList<>();
    @Getter
    ClanEntity clan;


    public BossClanRoom(BaseMap mapInfo, List<Character> aPlayer, String keyRoom, ClanEntity clan) {
        super(mapInfo, aPlayer, keyRoom, 0);
        clan.attackBoss(this);
        this.clan = clan;
        for (int i = 0; i < aPlayer.size(); i++) {
            aPlayer.get(i).getPlayer().addBuffBossGod(2f);
        }
    }

    @Override
    public void addPlayer(Player player) {
        super.addPlayer(player);
        if (roomState == RoomState.PAUSE) roomState = RoomState.ACTIVE;
        player.addBuffBossGod(2f);
        player.toPbStartGame((timeStartGame + (timeOut - timeDelayInstanceBoss) * 60 * 1000 - System.currentTimeMillis()) / 60000);
    }

    protected Coroutine initBoss() {
        return new Coroutine(timeDelayInstanceBoss, () -> {
            boss = bossData();
            aEnemy.add(boss);
            aProtoAdd.add(boss.toProtoAdd());
            timeStartGame = System.currentTimeMillis();
            for (int i = 0; i < aPlayer.size(); i++) {
                if (!aPlayer.get(i).isPlayer()) continue;
                aPlayer.get(i).getPlayer().toPbStartGame(timeOut - timeDelayInstanceBoss);
            }
        });
    }


//    @Override
//    public void ChangeCharacterHp(Character attacker, Character beDamage, long atk, long mAtk) {
//        if (beDamage.getId() == boss.getId()) {
//            if (damageHit.containsKey(attacker.getId()))
//                damageHit.put(attacker.getId(), damageHit.get(attacker.getId()) + (atk + mAtk));
//            else damageHit.put(attacker.getId(), atk + mAtk);
//        }
//    }

    public Character getHeroMaxDamage() {
        Map<Integer, Long> sortDamage = damageHit.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), LinkedHashMap::putAll);
        for (var entry : sortDamage.entrySet()) {
            Character ret = getPlayerId(entry.getKey());
            if (ret != null) return ret;
        }
        return null;
    }

    public EffectRoom createBomb() {
        if (getAPlayer().isEmpty()) return null;
        Character target = getAPlayer().get(NumberUtil.getRandom(getAPlayer().size()));
        if (target == null || !target.isAlive()) return null;
        SkillEffect skill = new SkillEffect();
        skill.setEffectType(EffectType.MOC_THAN_3);
        skill.setValues(getDameBoom());
        bomb = new EffectRoom(boss, target.getPos(), skill);
        bomb.setId(getIdEffectClient());
        bomb.setTarget(target);
        addEffectClient(bomb);
        return bomb;
    }

    public List<Float> getDameBoom() {
        float dame = 500f + 50f * clan.getBossLevel();
        return List.of(dame, dame);
    }

    public void removeBomb() {
        List<StateType> state = new ArrayList<>();
        List<Long> newEffect = new ArrayList<>();
        newEffect.add((long) EffectType.MOC_THAN_8.id);
        newEffect.add((long) bomb.getTarget().getPos().getX() * 1000);
        newEffect.add((long) bomb.getTarget().getPos().getY() * 1000);
        newEffect.add(EffectType.MOC_THAN_8.timeEffect);
        //send
        state.add(StateType.EFFECT);
        state.add(StateType.CHANGE_TARGET_EFFECT);
        List<List<Long>> data = new ArrayList<>();
        data.add(newEffect);
        data.add(Arrays.asList((long) bomb.getSkill().getEffectType().id, (long) bomb.getId(), -1L));
        protoRoomState(state, data);
        removeEffect(bomb);
        bomb = null;
    }

    List<Long> getBonusAttack(boolean isWin) {
        int numHuyHieu = (isWin ? 80 : 40) + clan.getBossLevel() * 2;
        return Bonus.viewItem(ItemKey.HUY_HIEU_BANG, numHuyHieu);
    }

    public Coroutine bossDie(float time, BossGod boss) {
        return new Coroutine(time, () -> {
            clearBossRoom();
            clan.upLevelBoss();
            List<Long> bonus = getBonusAttack(true);
            for (int i = 0; i < aPlayer.size(); i++) {
                Player player = aPlayer.get(i).getPlayer();
                if (player != null && player.getMUser().getChannel() != null) {
                    int per = 0;
                    player.removeBuff();
                    int dame = Math.toIntExact(boss.getBeDameInfo(player.getId()));
                    if (boss.getBeDameInfo().containsKey(player.getId())) {
                        per = (int) (dame * 100f / boss.getPoint().getMaxHp());
                    }
                    player.toPbEndGame(true, per, Bonus.receiveListItem(player.getMUser(), DetailActionType.BONUS_ATTACK_BOSS_CLAN.getKey(), bonus), getTimeAttack(), PopupType.POPUP_END_BOSS_CLAN, List.of(dame));
                    player.resetData();
                }
            }
        });
    }

    @Override
    protected BossGod bossData() {
        return boss;
    }

    @Override
    public void EffectUpdate() {
        if ((roomState == RoomState.ACTIVE || roomState == RoomState.PAUSE) && DateTime.isAfterTime(timeCreateRoom, timeOut)) {
            lostGame(true); // check tu dong
        }
        if (roomState != RoomState.ACTIVE) return;
        controller.EffectUpdate(this);
        for (int i = 0; i < aPet.size(); i++) {
            aPet.get(i).getPet().processSkill();
        }
    }

    @Override
    protected void processClientEffect(List<Integer> types, List<Long> data) {
        EffectType effectType = EffectType.get(types.get(0));
        if (effectType == null) return;

        switch (effectType) {
            case MOC_THAN_1 -> {
                int effectId = types.get(1);
                SeedMocThan seed = seedMoc.get(effectId);
                if (seed == null) return;
                if (!seed.isActive()) {
                    seed.setActive(true);
                    seedMoc.remove(effectId);
                    List<Long> dataClient = new ArrayList<>();
                    dataClient.add((long) EffectType.MOC_THAN_2.id);
                    dataClient.add((long) effectId);
                    boss.protoStatus(StateType.CLIENT_SKILL, dataClient.size(), dataClient);
                }
            }
            case MOC_THAN_3 -> { //boom
                int targetId = types.get(1);
                Character newTarget = aPlayer.stream().filter(u -> u.getId() == targetId).findAny().orElse(null);
                if (newTarget != null && bomb != null && DateTime.isAfterTime(timeSwitchBomb, timeDelaySwitchBomb)) {
                    timeSwitchBomb = System.currentTimeMillis();
                    bomb.setTarget(newTarget);
                    List<Long> dataRet = new ArrayList<>();
                    dataRet.add((long) bomb.getSkill().getEffectType().id); // type
                    dataRet.add((long) bomb.getId()); // effect Id
                    dataRet.add((long) bomb.getTarget().getId()); // target Id
                    dataRet.add(bomb.getTimeInit()); // thời gian tạo
                    long timeRemain = (long) (timeDelayBomb * 1000 - (System.currentTimeMillis() - bomb.getTimeInit()));
                    dataRet.add((timeRemain / 1000 * 1000)); // thời gian n
                    protoRoomState(StateType.CHANGE_TARGET_EFFECT, effectType.id, bomb.getId(), -1);
                    newTarget.protoStatus(StateType.CLIENT_SKILL, dataRet.size(), dataRet);
                }

            }
            case MOC_THAN_4 -> {
                int id = types.get(1);
                Character target = getPlayerId(types.get(2));
                EffectRoom effect = mEffectClient.get(id);
                if (target == null || !target.isAlive() || effect == null) return;
                EffectStatus effectStatus = EffectStatus.get(Math.toIntExact(data.get(0)));
                switch (effectStatus) {
                    case HIT -> {
                        long atkDame = (long) (effect.getSkill().getFirstPer() * effect.getOwner().getPoint().getAttackDamage());
                        long magDame = (long) (effect.getSkill().getNextPer() * effect.getOwner().getPoint().getMagicDamage());
                        target.beAttackEffect(effect, atkDame, magDame);
                        target.stun(effect.getTimeExits());
                    }
                }
            }
            case MOC_THAN_5, MOC_THAN_6, MOC_THAN_8 -> {
                int id = types.get(1);
                Character target = getPlayerId(types.get(2));
                if (target == null || target.isAlive() == false) return;
                EffectRoom effect = mEffectClient.get(id);
                if (effect == null) return;
                EffectStatus effectStatus = EffectStatus.get(Math.toIntExact(data.get(0)));
                switch (effectStatus) {
                    case HIT -> {
                        target.stun(effect);
                        long atkDame = (long) (effect.getSkill().getFirstPer() * effect.getOwner().getPoint().getAttackDamage());
                        long magDame = (long) (effect.getSkill().getNextPer() * effect.getOwner().getPoint().getMagicDamage());
                        target.beAttackEffect(effect, atkDame, magDame);
                    }
                    case END -> {
                        if (mEffectClient.containsKey(id)) mEffectClient.remove(id);
                    }
                }
            }
            case MOC_THAN_7 -> {
                EffectStatus effectStatus = EffectStatus.get(Math.toIntExact(data.get(0)));
                int id = types.get(1);
                EffectRoom effect = mEffectClient.get(id);
                if (effect == null) return;
                Character target = getPlayerId(types.get(2));
                if (target == null || target.isAlive() == false) return;
                switch (effectStatus) {
                    case HIT -> {
                        target.stun(effect);
                        long atkDame = (long) (effect.getSkill().getFirstPer() * effect.getOwner().getPoint().getAttackDamage());
                        long magDame = (long) (effect.getSkill().getNextPer() * effect.getOwner().getPoint().getMagicDamage());
                        target.beAttackEffect(effect, atkDame, magDame);
                    }
                    case OUT -> {
                        protoRoomState(StateType.CHANGE_TARGET_EFFECT, effectType.id, effect.getId(), target.getId());
                    }
                    case END -> {
                        if (mEffectClient.containsKey(id)) mEffectClient.remove(id);
                    }
                }
            }
            case MOC_THAN_9 -> {
                int id = types.get(1);
                EffectRoom effect = mEffectClient.get(id);
                if (effect == null) return;
                EffectStatus effectStatus = EffectStatus.get(Math.toIntExact(data.get(0)));
                switch (effectStatus) {
                    case HIT -> {
                        if (!effect.isActive()) {
                            effect.setActive(true);
                            Player target = effect.getTarget().getPlayer();
                            long atkDame = (long) (effect.getSkill().getFirstPer() * effect.getOwner().getPoint().getAttackDamage());
                            long magDame = (long) (effect.getSkill().getNextPer() * effect.getOwner().getPoint().getMagicDamage());
                            target.beAttackEffect(effect, atkDame, magDame);
                            target.stun(effect);
                        }
                    }
                    case END -> {
                        if (mEffectClient.containsKey(id)) mEffectClient.remove(id);
                    }
                }
            }
        }
    }

    @Override
    public void Update1s() {
        super.Update1s();
        // check end game
        if (this.roomState == RoomState.END || this.roomState == RoomState.PAUSE) return;
        boolean end = true;
        for (int i = 0; i < aPlayer.size(); i++) {
            if (aPlayer.get(i) != null && aPlayer.get(i).isPlayer()) {
                Player p = aPlayer.get(i).getPlayer();
                if (p.isAlive()) end = false;
            }
        }
        if (end) lostGame(false); // chết hết user
    }

    @Override
    public void characterDie(Character character) {
        if (character.getType() == CharacterType.BOSS_GOD) {
            addCoroutine(bossDie(1f, (BossGod) character));
        } else if (character.getType() == CharacterType.PLAYER) {
            Player player = character.getPlayer();
            if(idGetBonus.contains(player.getId())) return;
            idGetBonus.add(player.getId());
            List<Long> bonus = getBonusAttack(false);
            if (player != null && player.getMUser().getChannel() != null) {
                int per = 0;
                player.removeBuff();
                int dame = boss.getBeDameInfo().get(character.getId()) == null ? 0 : Math.toIntExact(boss.getBeDameInfo().get(character.getId()));
                if (boss.getBeDameInfo().containsKey(character.getId())) {
                    per = (int) (dame * 100f / boss.getPoint().getMaxHp());
                }
                player.toPbEndGame(false, per, Bonus.receiveListItem(player.getMUser(), DetailActionType.BONUS_ATTACK_BOSS_CLAN.getKey(), bonus), getTimeAttack(), PopupType.POPUP_END_BOSS_CLAN, List.of(dame));
                player.resetData();
                clan.checkDynamic(player.getMUser(), CfgClan.ATTACK_BOSS, 1);
            }
        } else if (character.getType() == CharacterType.MOC1 || character.getType() == CharacterType.MOC2
                || character.getType() == CharacterType.MOC3 || character.getType() == CharacterType.MOC4) {
            BossClan bossClan = (BossClan) boss;
            bossClan.getCotInfo().set(character.getType().value - CharacterType.MOC1.value, 0);
            bossClan.removeSupport(character.getType().value - CharacterType.MOC1.value);
        }
    }

    void clearBossRoom() {
        setEndGameState();
        clan.checkEndBoss();
        bomb = null;
    }

    protected void lostGame(boolean isTimeUp) {
        if (isTimeUp) clearBossRoom();
        if (aPlayer.isEmpty()) roomState = RoomState.PAUSE;
        List<Long> bonus = getBonusAttack(false);
        for (int i = 0; i < aPlayer.size(); i++) {
            Player player = aPlayer.get(i).getPlayer();
            if (player != null && player.getMUser().getChannel() != null) {
                int per = 0;
                player.removeBuff();
                int dame = boss.getBeDameInfo().get(player.getId()) == null ? 0 : Math.toIntExact(boss.getBeDameInfo().get(player.getId()));
                if (boss.getBeDameInfo().containsKey(player.getId())) {
                    per = (int) (dame * 100f / boss.getPoint().getMaxHp());
                }
                player.toPbEndGame(false, per, Bonus.receiveListItem(player.getMUser(), DetailActionType.BONUS_ATTACK_BOSS_CLAN.getKey(), bonus), getTimeAttack(), PopupType.POPUP_END_BOSS_CLAN, List.of(dame));
                player.resetData();
                clan.checkDynamic(player.getMUser(), CfgClan.ATTACK_BOSS, 1);
            }
        }

    }

    @Override
    public void Update() {
        long _dt = System.currentTimeMillis() - _dte;
        _dte = System.currentTimeMillis();
        local_time += _dt / 1000.0;
        server_time = local_time;
        // send data
        try {
            if (aPlayer.size() > 0) {
                sendTableState();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    public void removePlayer(int userId) {
        protocol.Pbmethod.CommonVector.Builder pbLeave = protocol.Pbmethod.CommonVector.newBuilder();
        pbLeave.addALong(getRoomType());
        pbLeave.addALong(userId);
        pbLeave.addALong(mapInfo != null ? mapInfo.getId() : 0);
        Character playerRemove = aPlayer.stream().filter(player -> player.getId() == userId).findAny().orElse(null);
        if (playerRemove != null) {
            aPlayer.remove(playerRemove);
            aPet.remove(playerRemove.getPet());
            aProtoAdd.add(playerRemove.toProtoRemove());
            if (playerRemove.getPet() != null) aProtoAdd.add(playerRemove.getPet().toProtoRemove());
        }
    }
}
