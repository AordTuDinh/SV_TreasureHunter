package game.battle.model;

import game.battle.effect.EffectRoom;
import game.battle.object.BossSkill;
import game.battle.object.Coroutine;
import game.battle.object.Pos;
import game.battle.type.AttackType;
import game.battle.type.EffectBodyType;
import game.battle.type.RoomState;
import game.battle.type.StateType;
import game.config.CfgQuest;
import game.dragonhero.mapping.main.ResBossEntity;
import game.dragonhero.table.BaseBattleRoom;
import game.dragonhero.table.BossGodRoom;
import game.object.DataQuest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ThoThan extends BossGod implements Serializable {
    static final List<Integer> skilLActive = List.of(0, 0, 1, 0, 0, 2, 0, 3, 0, 0, 1, 0, 0, 3, 0, 0, 2);
    static final List<Float> sizeElip2 = List.of(1.65f, 0.5f);


    public ThoThan(ResBossEntity boss, Pos startPos, int teamId, BaseBattleRoom room) {
        super(boss, startPos, teamId, room);
        attackType = AttackType.LONG_RANGE;
        capBullet = new Pos(0f, 1.84f);
    }

    @Override
    public void LastUpdate() {
        super.LastUpdate();
        processSkill();
    }

    @Override
    protected void processSkill() {
        if (room.getRoomState() == RoomState.END || attackBlockMove() || targetAttack == null || !targetAttack.isAlive() || !isAlive() || beBlock() || !isReviveReady() || !checkStart() || System.currentTimeMillis() < timeActiveSkill) {
            return;
        }
        int skillNext = skilLActive.get(indexSkill);
        if(hasAttack())  indexSkill++;
        else return;
        if (indexSkill >= skilLActive.size()) indexSkill = 0;
        // crazy
        if (activeRageMode()) {
            rageMode = true;
            addTimeActive(3000);
            point.addDef(100);
            point.addMagicResist(100);
            protoStatus(StateType.PLAY_ANIMATION, SKILL2);
            protoStatus(StateType.EFFECT_BODY, Arrays.asList(EffectBodyType.CRAZY.value, 60000L));
            return;
        }
        switch (skillNext) {
            case 0 -> { // đánh thường
                if (hasAttackNormal()) {
                    protoStatus(StateType.PLAY_ANIMATION, SKILL0);
                    room.addCoroutine(new Coroutine(0.3f, () -> {
                        getBattleRoom().addBullet(this, skill1Id, processActiveSkill());
                    }));
                    addTimeActive(3000);
                }
            }
            case 1 -> { // tạo cột totem buff chỉ số cho boss
                BossGodRoom bossRoom = ((BossGodRoom) room);
                protoStatus(StateType.PLAY_ANIMATION, SKILL2);
                if(bossRoom.getBoss()==null) return;
                bossRoom.addCoroutine(new Coroutine(0.5f, () -> {
                    List<ResBossEntity> support = getSupport();
                    for (int i = 0; i < support.size(); i++) {
                        Pos random = Pos.randomPos(support.get(i).getInstancePos(), 2f, 2f);
                        Support totem = new Support(support.get(i), random, teamId, bossRoom);
                        bossRoom.addSupport(totem);
                        bossRoom.getBoss().addSupportPoint(totem.getPoint().cloneInstance());
                    }
                }));
                addTimeActive(5000);
            }
            case 2 -> { // q chogath
                BossSkill skill = skills.get(0);
                EffectRoom effectRoom = new EffectRoom(this, targetAttack.getPos(), skill.getEffect());
                effectRoom.setId(getBattleRoom().getIdEffectClient());
                effectRoom.setTarget(targetAttack);
                getBattleRoom().addEffectClient(effectRoom);
                // send data
                List<Long> data = new ArrayList<>();
                data.add((long) skill.getEffect().getEffectType().id);
                data.add((long) effectRoom.getId());
                data.add((long) (targetAttack.getPos().x * 1000)); // pos initsa
                data.add((long) (targetAttack.getPos().y * 1000)); // pos init
                data.add((long) (sizeElip2.get(0) * 1000)); //xRadius
                data.add((long) (sizeElip2.get(1) * 1000)); // yRadius
                data.add((long) (3000)); // y hất tung
                protoStatus(StateType.CLIENT_SKILL, data.size(), data);
                addTimeActive(5000);
            }
            case 3 -> { //triệu hồi quái
                protoStatus(StateType.PLAY_ANIMATION, SKILL1);
                room.addCoroutine(new Coroutine(0.15f, () -> {
                    genEnemy(getBattleRoom(), SKILL1, 0.8f);
                }));
                addTimeActive(5000);
            }
        }

    }

    @Override
    public void bonusKillMe(Character killer) {
        super.bonusKillMe(killer);
        Player player = killer.getPlayer();
        if (killer.getPlayer() != null) {
            CfgQuest.addNumQuest(player.getMUser(), DataQuest.KILL_THO_THAN, 1);
        }
    }
}
