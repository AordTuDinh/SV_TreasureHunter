package game.battle.model;

import game.battle.effect.EffectRoom;
import game.battle.object.BossSkill;
import game.battle.object.Coroutine;
import game.battle.object.Pos;
import game.battle.type.EffectBodyType;
import game.battle.type.RoomState;
import game.battle.type.StateType;
import game.config.CfgQuest;
import game.dragonhero.mapping.main.ResBossEntity;
import game.dragonhero.table.BaseBattleRoom;
import game.object.DataQuest;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

//Fixme DATE: 7/31/2022 LƯU Ý --->CẦN TƯ DUY CODE NHIỀU THẰNG 1 ROOM CÙNG ĐÁNH BOSS
public class KimThan extends BossGod implements Serializable {
    static final int timeAttackNormal = 2000; //
    static final int timeRageMode = 3000; // thời gian dùng skill rage
    static final int numSkill2 = 5;
    // skill index
    static final List<Integer> skilLActive = List.of(0, 0, 2, 0, 1, 0, 3, 0, 1, 0, 3);


    //Fixme DATE: 8/21/2022 LƯU Ý ---> Boss skill hoat dong doc lap voi danh thuong
    public KimThan(ResBossEntity boss, Pos startPos, int teamId, BaseBattleRoom room) {
        super(boss, startPos, teamId, room);
        capBullet = new Pos(0f, -0.5f);
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
        indexSkill++;
        if (indexSkill >= skilLActive.size()) indexSkill = 0;
        //buff chí mạng 30% + 100% sát thương chí mạng
        if (activeRageMode()) {
            rageMode = true;
            addTimeActive(timeRageMode);
            point.addCrit(50);
            point.addCritDamage(100);
            protoStatus(StateType.PLAY_ANIMATION, SKILL2);
            protoStatus(StateType.EFFECT_BODY, Arrays.asList(EffectBodyType.CRAZY.value, 60000L));
            return;
        }
        switch (skillNext) {
            case 0 -> {
                // đánh tường
                if (hasAttackNormal()) {
                    getBattleRoom().addBullet(this, skill1Id, processActiveSkill());
                    protoStatus(StateType.PLAY_ANIMATION, SKILL0);
                    addTimeActive(timeAttackNormal);
                }

            }
            case 1 -> { //Create kim1
                float nextSkillTime = 2.2f;
                BossSkill skill = skills.get(0);
                for (int j = 0; j < numSkill2; j++) {
                    room.addCoroutine(new Coroutine(j * nextSkillTime, () -> {
                        if (targetAttack == null) return;
                        EffectRoom eff = new EffectRoom(this, targetAttack.getPos(), skill.getEffect());
                        getBattleRoom().addEffectRoom(eff);
                        protoStatus(StateType.PLAY_ANIMATION, SKILL1);
                    }));
                }
                addTimeActive((long) ((nextSkillTime * numSkill2 + 3) * 1000));
            }
            case 2 -> { //triệu hồi quái
                genEnemy(getBattleRoom(), SKILL2, 0.5f);
            }
            case 3 -> { //kim2 : Chém 1 phát cực mạnh xung quanh boss
                EffectRoom eff = new EffectRoom(this, getPos(), skills.get(1).getEffect());
                getBattleRoom().addEffectRoom(eff);
                addTimeActive(5000);
                protoStatus(StateType.PLAY_ANIMATION, SKILL3);
            }

        }


    }

    @Override
    public void bonusKillMe(Character killer) {
        super.bonusKillMe(killer);
        Player player = killer.getPlayer();
        if (player != null) {
            CfgQuest.addNumQuest(player.getMUser(), DataQuest.KILL_KIM_THAN, 1);
        }
    }
}
