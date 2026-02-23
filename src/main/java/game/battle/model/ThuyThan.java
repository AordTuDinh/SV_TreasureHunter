package game.battle.model;

import game.battle.calculate.MathLab;
import game.battle.effect.EffectRoom;
import game.battle.effect.SkillEffect;
import game.battle.object.Coroutine;
import game.battle.object.Pos;
import game.battle.type.EffectBodyType;
import game.battle.type.RoomState;
import game.battle.type.StateType;
import game.config.CfgQuest;
import game.dragonhero.mapping.main.ResBossEntity;
import game.dragonhero.service.battle.EffectType;
import game.dragonhero.table.BaseBattleRoom;
import game.object.DataQuest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ThuyThan extends BossGod implements Serializable {
    //Fixme DATE: 8/21/2022 LƯU Ý ---> Boss skill hoat dong doc lap voi danh thuong
    static final int timeAttackNormal = 3000; //
    static final float timeDelayAnimAttack = 0.5f;
    List<Float> valueDameSkill2 = List.of(100f, 100f);
    static final int timeRageMode = 3000; // thời gian dùng skill rage
    // skill
    static final List<Integer> skilLActive = List.of(0, 0, 3, 0, 0, 1, 0, 2, 0, 0, 3, 0, 1, 0, 0, 2, 0, 0, 2);

    public ThuyThan(ResBossEntity boss, Pos startPos, int teamId, BaseBattleRoom room) {
        super(boss, startPos, teamId, room);
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
            timeActiveSkill = System.currentTimeMillis() + timeRageMode;
            point.addCrit(50);
            point.addCritDamage(100);
            protoStatus(StateType.PLAY_ANIMATION, SKILL3);
            protoStatus(StateType.EFFECT_BODY, Arrays.asList(EffectBodyType.CRAZY.value, 60000L));
            return;
        }
        switch (skillNext) {
            case 0 -> { // đánh thường
                if (hasAttackNormal()) {
                    protoStatus(StateType.PLAY_ANIMATION, SKILL0);
                    room.addCoroutine(new Coroutine(timeDelayAnimAttack, () -> getBattleRoom().addBullet(this, skill1Id, processActiveSkill())));
                    addTimeActive(timeAttackNormal);
                }
            }
            case 1 -> { // Bãi độc THUY_THAN_2
                // Bãi độc
                EffectRoom effectRoom = new EffectRoom(this, targetAttack.getPos(), skills.get(0).getEffect());
                effectRoom.setId(getBattleRoom().getIdEffectClient());
                getBattleRoom().addEffectClient(effectRoom);
                // send data
                List<Long> data = new ArrayList<>();
                data.add((long) effectRoom.getSkill().getEffectType().id);
                data.add((long) effectRoom.getId());
                data.add((long) (targetAttack.getPos().x * 1000)); // pos init
                data.add((long) (targetAttack.getPos().y * 1000)); // pos init
                data.add(effectRoom.getSkill().getTimeMs()); // time skill
                protoStatus(StateType.CLIENT_SKILL, data.size(), data);
                addTimeActive(3000);
                room.addCoroutine(new Coroutine(effectRoom.getSkill().getTime(), () -> {
                    getBattleRoom().removeEffect(effectRoom);
                }));
            }
            case 2 -> { // Create
                // tạo một cơn sóng chạy qua player, gây sát thương lớn
                SkillEffect skill = new SkillEffect();
                skill.setValues(new ArrayList<>(valueDameSkill2));
                skill.setEffectType(EffectType.THUY_THAN_3);

                EffectRoom effectRoom = new EffectRoom(this, targetAttack.getPos(), skill);
                effectRoom.setId(getBattleRoom().getIdEffectClient());
                effectRoom.setTarget(targetAttack);
                getBattleRoom().addEffectClient(effectRoom);
                // send data
                List<Long> data = new ArrayList<>();
                data.add((long) skill.getEffectType().id);
                data.add((long) effectRoom.getId());
                data.add((long) (getPos().x * 1000)); // pos init
                data.add((long) (getPos().y * 1000)); // pos init
                Pos direction = MathLab.getDirection(pos, targetAttack.getPos());
                data.add((long) (direction.x * 1000)); // direction
                data.add((long) (direction.y * 1000)); // direction
                data.add(5 * 1000L); // move speed
                data.add(10 * 1000L); // range fly
                protoStatus(StateType.CLIENT_SKILL, data.size(), data);
                addTimeActive(5000);
            }
            case 3 -> { //triệu hồi quái
                protoStatus(StateType.PLAY_ANIMATION, SKILL4);
                room.addCoroutine(new Coroutine(0.15f, () -> {
                    genEnemy(getBattleRoom(), SKILL4, 0.8f);
                }));
                addTimeActive(5000);
            }
        }
    }

    @Override
    public void bonusKillMe(Character killer) {
        super.bonusKillMe(killer);
        Player player = killer.getPlayer();
        if (player != null) {
            CfgQuest.addNumQuest(player.getMUser(), DataQuest.KILL_THUY_THAN, 1);
        }
    }

}
