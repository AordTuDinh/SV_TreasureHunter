package game.battle.model;

import game.battle.effect.EffectRoom;
import game.battle.effect.SkillEffect;
import game.battle.object.BossSkill;
import game.battle.object.Coroutine;
import game.battle.object.Pos;
import game.battle.type.EffectBodyType;
import game.battle.type.RoomState;
import game.battle.type.StateType;
import game.config.CfgQuest;
import game.dragonhero.mapping.main.ResBossEntity;
import game.dragonhero.mapping.main.ResSkillEntity;
import game.dragonhero.service.battle.EffectType;
import game.dragonhero.service.resource.ResSkill;
import game.dragonhero.table.BaseBattleRoom;
import game.object.DataQuest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HoaThan extends BossGod implements Serializable {
    static final int timeRageMode = 3000; // thời gian dùng skill rage
    static final int timeAttackNormal = 3000; //
    List<Float> valueDameSkill3 = List.of(100f, 100f);

    public HoaThan(ResBossEntity boss, Pos startPos, int teamId, BaseBattleRoom room) {
        super(boss, startPos, teamId, room);
        if(skills.isEmpty()) {
            skills= new ArrayList<>();
            ResSkillEntity skill = ResSkill.getSkills(7);
            skills.add(new BossSkill(skill.getId(), new SkillEffect(skill)));
        }
    }

    @Override
    public void LastUpdate() {
        super.LastUpdate();
        processSkill();
    }

    static final List<Integer> skilLActive = List.of(0, 0, 2, 0, 1, 0, 3, 0, 1, 0, 0, 3, 0, 0, 2);


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
            addTimeActive(timeRageMode);
            point.addCrit(50);
            point.addCritDamage(100);
            protoStatus(StateType.PLAY_ANIMATION, SKILL4);
            protoStatus(StateType.EFFECT_BODY, Arrays.asList(EffectBodyType.CRAZY.value, 60000L));
            return;
        }
        switch (skillNext) {
            case 0 -> {
                if (hasAttackNormal()) { //HOA_THAN_NORMAL - Khi phát nổ tạo ra 1 vùng lửa đốt tồn tại trong 2s
                    addTimeActive(timeAttackNormal);
                    protoStatus(StateType.PLAY_ANIMATION, SKILL0);
                    room.addCoroutine(new Coroutine(0.2f, () -> {
                        getBattleRoom().addBullet(this, skill1Id, processActiveSkill());
                    }));
                }
            }
            case 1 -> {
                protoStatus(StateType.PLAY_ANIMATION, SKILL2);
                room.addCoroutine(new Coroutine(1f, () -> {
                    if(targetAttack==null)return;
                    BossSkill skill = skills.get(0);
                    EffectRoom eff = new EffectRoom(this, targetAttack.getPos().clone(), skill.getEffect());
                    getBattleRoom().addEffectRoom(eff);
                }));
                addTimeActive(3000);
            }
            case 2 -> { //triệu hồi quái
                if (targetAttack != null) {
                    genEnemy(getBattleRoom(), SKILL1, 0.4f);
                }
                addTimeActive(5000);
            }
            case 3 -> { // ném 3 quả cầu đến các vị trí ngẫu nhiên, player đi gần quả cầu sẽ bay theo và gây sát thương lên player
                protoStatus(StateType.PLAY_ANIMATION, SKILL3);
                SkillEffect skill = new SkillEffect();
                skill.setEffectType(EffectType.HOA_THAN_3);
                skill.setValues(valueDameSkill3);
                for (int i = 0; i < 5; i++) {
                    EffectRoom effectRoom = new EffectRoom(this, targetAttack.getPos(), skill);
                    effectRoom.setId(getBattleRoom().getIdEffectClient());
                    effectRoom.setTarget(targetAttack);
                    getBattleRoom().addEffectClient(effectRoom);
                    // todo gen pos
                    Pos randPos = Pos.randomPos(getTargetAttack().getPos(), 10f, 10f, panelMap.botLeft, panelMap.topRight, 0.5f);
                    effectRoom.setTargetPos(randPos);
                    // send data
                    List<Long> data = new ArrayList<>();
                    data.add((long) skill.getEffectType().id);
                    data.add((long) effectRoom.getId());
                    data.add((long) (getPos().x * 1000)); // pos init
                    data.add((long) (getPos().y * 1000)); // pos init
                    data.add((long) (randPos.x * 1000)); // pos target
                    data.add((long) (randPos.y * 1000)); // pos target
                    data.add(3 * 1000L); // move speed
                    data.add(2 * 1000L); // time delay
                    data.add(3 * 1000L); // range check
                    data.add(5 * 1000L); // speed 2
                    data.add(10 * 1000L); // time alive
                    protoStatus(StateType.CLIENT_SKILL, data.size(), data);
                }
                this.timeActionAttack = System.currentTimeMillis() + 10000; // block move
                addTimeActive(10000);
            }
        }
    }


    @Override
    public void bonusKillMe(Character killer) {
        super.bonusKillMe(killer);
        Player player = killer.getPlayer();
        if (player != null) {
            CfgQuest.addNumQuest(player.getMUser(), DataQuest.KILL_HOA_THAN, 1);
        }
    }
}
