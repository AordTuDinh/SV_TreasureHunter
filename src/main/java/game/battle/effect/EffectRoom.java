package game.battle.effect;

import game.battle.model.Character;
import game.battle.object.Pos;
import game.battle.type.StateType;
import game.config.CfgBattle;
import game.dragonhero.BattleConfig;
import lombok.Data;
import ozudo.base.helper.DateTime;

import java.util.ArrayList;
import java.util.List;


// effect tác dụng lên room, từ room có thể tác dụng lên character tiếp
@Data
public class EffectRoom extends Effect {
    int id;
    Pos instancePos;
    Pos targetPos;
    Pos direction;
    Character target;
    long timeInit;
    // for client

    // for spin
    Pos spin1, spin2;
    float angle1, angle2;
    List<Float> sizeElip;
    public static final int EFFECT_TIME = 1;
    public static final int EFFECT_ONE = 2;

    public EffectRoom(Character attacker, Pos instancePos, SkillEffect skill) {
        this.owner = attacker;
        this.skill = skill;
        this.instancePos = instancePos;
        this.timeExits = skill.getTime();
        this.active = false;
        this.spin1 = instancePos.clone();
        this.timeInit = System.currentTimeMillis();
        this.spin2 = instancePos.clone();
    }

    public EffectRoom() {
        this.timeInit = System.currentTimeMillis();
    }

    public EffectRoom(Character owner, Character target, SkillEffect skill) {
        this.owner = owner;
        this.skill = skill;
        this.target = target;
        this.timeExits = skill.getTime();
        this.timeInit = System.currentTimeMillis();
        this.active = false;
    }

    public boolean canActiveByTime(float time) {
        return DateTime.isAfterTime(timeInit, time);
    }

    public Pos getSpin1() {
        spin1.x = (float) (instancePos.x + Math.cos(Math.toRadians(angle1)) * BattleConfig.S_radiusSpin);
        spin1.y = (float) (instancePos.y + Math.sin(Math.toRadians(angle1)) * BattleConfig.S_radiusSpin);
        angle1 += CfgBattle.updateTime * BattleConfig.S_speedSpin;
        if (angle1 >= 360) angle1 = 0;
        return spin1;
    }

    public Pos getSpin2() {
        spin2.x = (float) (instancePos.x + Math.cos(Math.toRadians(angle2)) * BattleConfig.S_radiusSpin);
        spin2.y = (float) (instancePos.y + Math.sin(Math.toRadians(angle2)) * BattleConfig.S_radiusSpin);
        angle2 += CfgBattle.updateTime * BattleConfig.S_speedSpin;
        if (angle2 >= 360) angle2 = 0;
        return spin2;
    }

    public boolean checkActive(int effectShow) {
        if (!isActive()) {
            if (effectShow == EFFECT_TIME) {
                getOwner().protoStatus(StateType.EFFECT, toStateTime());
            } else if (effectShow == EFFECT_ONE) {
                getOwner().protoStatus(StateType.EFFECT, toStateOne());
            }
            active();
            setTimeRealActive();
            return false;
        }
        return true;
    }

    public boolean checkActiveOne() {
        return checkActive(EFFECT_ONE);
    }

    public boolean checkActiveTime() {
        return checkActive(EFFECT_TIME);
    }

    private List<Long> toStateTime() { // thời gian hoạt động - EFFECT_TIME
        List<Long> lst = new ArrayList<>();
        lst.add((long) skill.getEffectType().id);
        lst.add((long) (instancePos.x * 1000));
        lst.add((long) (instancePos.y * 1000));
        lst.add((long) (this.timeExits * 1000));
        return lst;
    }

    public List<Long> toStateOne() { // tác dụng 1 lần theo time animation   - EFFECT_ONE
        List<Long> lst = new ArrayList<>();
        lst.add((long) skill.getEffectType().id);
        lst.add((long) (instancePos.x * 1000));
        lst.add((long) (instancePos.y * 1000));
        lst.add(skill.getEffectType().timeEffect);
        return lst;
    }

}
