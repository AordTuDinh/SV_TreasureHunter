package game.dragonhero.table;

import game.battle.effect.EffectRoom;
import game.battle.model.*;
import game.battle.model.Character;
import game.battle.object.Coroutine;
import game.battle.object.Point;
import game.battle.type.CharacterType;
import game.battle.type.EffectBodyType;
import game.battle.type.RoomState;
import game.battle.type.StateType;
import game.config.aEnum.DetailActionType;
import game.config.aEnum.PopupType;
import game.dragonhero.BattleConfig;
import game.dragonhero.mapping.main.BaseMap;
import game.dragonhero.mapping.main.ResMapBossEntity;
import game.dragonhero.service.battle.EffectStatus;
import game.dragonhero.service.battle.EffectType;
import game.dragonhero.service.user.Bonus;
import lombok.Data;
import ozudo.base.helper.DateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Data
public abstract class BossGodRoom extends BaseBattleRoom {
    protected static final float timeDelayInstanceBoss = 3f; // 3s sau thi cho boss xuat hien
    protected static final float timeOut = 63f;
    protected static final int team = 2;

    List<Support> supports = new ArrayList<>();
    BossGod boss;
    int mode;
    float timeStunHoaThan = 2f;


    public BossGodRoom(BaseMap mapInfo, List<Character> aPlayer, String keyRoom, int mode) {
        super(mapInfo, aPlayer, keyRoom, false);
        addCoroutine(initBoss());
        this.mode = mode;
        if (aPlayer.size() == 1) aPlayer.get(0).getPlayer().addBuffBossGod(1.5f);
    }

    @Override
    public void LastUpdate() {
        super.LastUpdate();
        if (boss != null && roomState == RoomState.ACTIVE) boss.LastUpdate();
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

    protected abstract BossGod bossData();

    @Override
    public void EffectUpdate() {
        super.EffectUpdate();
        if (roomState == RoomState.ACTIVE && DateTime.isAfterTime(timeCreateRoom, timeOut)) {
            lostGame(true); // hết thời gian
        }
    }

    @Override
    protected void processClientEffect(List<Integer> types, List<Long> data) {
        EffectType effectType = EffectType.get(types.get(0));
        if (effectType == null) return;
        int id = types.get(1);
        EffectRoom effect = mEffectClient.get(id);
        if (effect == null || effect.getTarget() == null || !effect.getTarget().isAlive()) return;
        switch (effectType) {
            case THUY_THAN_2 -> {
                EffectStatus effectStatus = EffectStatus.get(Math.toIntExact(data.get(0)));
                Character target = getPlayerId(types.get(2));
                if (target == null || !target.isAlive()) return;
                switch (effectStatus) {
                    case HIT -> {
                        // dec point
                        effect.addRealBuff(target.getPoint().buffChange(Point.CHANGE_MOVE_SPEED, (long) -effect.getSkill().getFirstValues(), BattleConfig.S_maxReduce90));
                        target.protoMultiPoint(List.of((long) Point.CHANGE_MOVE_SPEED, (long) -effect.getSkill().getFirstValues(), (long) Point.MOVE_SPEED, target.getPoint().getMoveSpeed()));
                        long addDef = -(long) effect.getSkill().getValueIndex(1);
                        effect.addRealBuff(addDef);
                        target.protoBuffPoint(target.getPoint().DEFENSE, addDef);
                        long addMagicResist = -(long) effect.getSkill().getValueIndex(2);
                        effect.addRealBuff(addMagicResist);
                        target.protoBuffPoint(target.getPoint().MAGIC_RESIST, addMagicResist);
                    }
                    case OUT -> {
                        target.getPoint().buffChange(Point.CHANGE_MOVE_SPEED, (long) effect.getSkill().getFirstValues(), BattleConfig.S_maxReduce90);
                        target.protoMultiPoint(List.of((long) Point.CHANGE_MOVE_SPEED, (long) effect.getSkill().getFirstValues(), (long) Point.MOVE_SPEED, target.getPoint().getMoveSpeed()));
                        target.protoBuffPoint(Point.DEFENSE, -effect.getRealBuff(1));
                        target.getPoint().addMagicResist(-effect.getRealBuff(2));
                        target.protoBuffPoint(Point.MAGIC_RESIST, -effect.getRealBuff(2));
                    }
                }
            }
            case THUY_THAN_3 -> {
                EffectStatus effectStatus = EffectStatus.get(Math.toIntExact(data.get(0)));
                switch (effectStatus) {
                    case HIT -> {
                        if (effect.isActive() == false) {
                            effect.setActive(true);
                            long atkDame = (long) (effect.getSkill().getFirstPer() * effect.getOwner().getPoint().getAttackDamage());
                            long magDame = (long) (effect.getSkill().getNextPer() * effect.getOwner().getPoint().getMagicDamage());
                            effect.getTarget().beAttackEffect(effect, atkDame, magDame);
                        }
                    }
                    case END -> {
                        if (mEffectClient.containsKey(id)) mEffectClient.remove(id);
                    }
                }
            }
            case HOA_THAN_3 -> {
                EffectStatus effectStatus = EffectStatus.get(Math.toIntExact(data.get(0)));
                switch (effectStatus) {
                    case HIT -> {
                        if (effect.isActive() == false) {
                            effect.setActive(true);
                            Player target = effect.getTarget().getPlayer();
                            long atkDame = (long) (effect.getSkill().getFirstPer() * effect.getOwner().getPoint().getAttackDamage());
                            long magDame = (long) (effect.getSkill().getNextPer() * effect.getOwner().getPoint().getMagicDamage());
                            if (target.isBeDot()) {
                                atkDame *= 2;
                                magDame *= 2;
                                target.stun(timeStunHoaThan);
                            }
                            target.beAttackEffect(effect, atkDame, magDame);
                        }
                    }
                    case END -> {
                        if (mEffectClient.containsKey(id)) mEffectClient.remove(id);
                    }
                }
            }

            case THO_THAN_2 -> {
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
                    case END -> mEffectClient.remove(id);

                }
            }
        }
    }

    @Override
    protected void startInit() {
        super.startInit();
        roomState = RoomState.ACTIVE;
    }

    @Override
    public void characterDie(Character character) {
        super.characterDie(character);
        if (character.getType() == CharacterType.BOSS_GOD) {
            addCoroutine(bossDie(1f, (BossGod) character));
        } else if (character.getType() == CharacterType.PLAYER) {
            lostGame(false); // chết user
        }
    }

    protected void lostGame(boolean isTimeUp) {
        setEndGameState();
        for (int i = 0; i < aPlayer.size(); i++) {
            Player player = aPlayer.get(i).getPlayer();
            if (player != null && player.getMUser().getChannel() != null) {
                int per = 0;
                player.removeBuff();
                if (boss.getBeDameInfo().containsKey(player.getId())) {
                    per = (int) (boss.getBeDameInfo().get(player.getId()) * 100f / boss.getPoint().getMaxHp());
                }
                player.toPbEndGame(false, per, null, getTimeAttack(), PopupType.POPUP_END_GAME, List.of(0, 1));
                player.resetData();
            }
        }
    }

    public Long getBossId() { // độ khó sẽ theo map Id
        ResMapBossEntity mapBoss = (ResMapBossEntity) mapInfo;
        return Long.valueOf(mapBoss.getListEnemy().get(mode - 1));
    }

    public Coroutine bossDie(float time, BossGod boss) {
        return new Coroutine(time, () -> {
            setEndGameState();
            for (int i = 0; i < aPlayer.size(); i++) {
                Player player = aPlayer.get(i).getPlayer();
                if (player != null && player.getMUser().getChannel() != null) {
                    boss.bonusKillMe(player);
                }
            }
        });
    }

    public void addSupport(Support sp) {
        supports.add(sp);
        aEnemy.add(sp);
        aProtoAdd.add(sp.toProtoAdd());
    }

    public void removeSupport(Support sp) {
        //trừ chỉ số đã cộng vào boss
        supports.remove(sp);
    }
}
