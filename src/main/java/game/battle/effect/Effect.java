package game.battle.effect;

import game.battle.model.Character;
import game.battle.object.Pos;
import game.battle.type.StateType;
import game.config.CfgBattle;
import game.config.aEnum.FactionType;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.helper.DateTime;

import java.util.ArrayList;
import java.util.List;

/*
NOTE: effect chia làm 4 loại
Loại 1 : effect cộng thẳng vào damage
Loại 2 : effect gán vào target(player, enemy ...), thường sẽ tác dụng theo thời gian
Loại 3 : effect gán vào room(tạo bão, sấm sét tác dụng lên đâu...)
Loại 4 : effect gán vào room tác dụng ngược lại character
 */
@Data
@NoArgsConstructor
public class Effect {
    SkillEffect skill;
    Character owner;    // use
    boolean active, realActive; // active dùng để gửi về client, real active dùng để check active 1 lần
    List<Long> realBuff; // cache số thực tác dụng lên character
    float timeExits;
    long timeRealActive; // thời gian active - dùng để chờ animation tác dụng ở client
    FactionType faction;


    // add to character
    public Effect(Character owner, SkillEffect skill, FactionType faction) {
        this.owner = owner;
        this.skill = skill;
        this.active = false;
        timeExits = skill.getTime();
        realBuff = new ArrayList<>();
        this.faction = faction;
    }

    public void setTimeRealActive() {
        timeRealActive = System.currentTimeMillis() + (long) (skill.getTimeDelayDame() * DateTime.SECOND2_MILLI_SECOND);
    }

    public boolean canActiveByAnimOne() {
        boolean active = System.currentTimeMillis() > timeRealActive;
        if (active) {
            if (!realActive) {
                realActive = true;
                return true;
            }
        }
        return System.currentTimeMillis() > timeRealActive;
    }

    public boolean canActiveByAnim() {
        return System.currentTimeMillis() > timeRealActive;
    }


    public Effect clone() {
        Effect eff = new Effect();
        eff.owner = this.getOwner();
        eff.skill = this.skill;
        eff.active = false;
        eff.timeExits = skill.getTime();
        eff.realBuff = new ArrayList<>();
        eff.faction = this.faction;
        return eff;
    }

    public Effect clone(float timeExits) {
        Effect eff = clone();
        eff.setTimeExits(timeExits);
        return eff;
    }

    public void addRealBuff(long realBuff) {
        this.realBuff.add(realBuff);
    }

    public long getFirstRealBuff() {
        return realBuff.get(0);
    }

    public long getRealBuff(int index) {
        return realBuff.get(index);
    }

    public boolean checkExist(float time) {
        timeExits -= time;
        return timeExits > -CfgBattle.decTimeEff;
    }

    public boolean sameTeam(Character target) {
        return owner.sameTeam(target);
    }

    public void active() {
        this.setActive(true);
    }

    public void realActive() {
        this.setRealActive(true);
    }


    public List<Long> toStateOne(Pos instancePos) { // tác dụng 1 lần theo time animation   - EFFECT_ONE
        List<Long> lst = new ArrayList<>();
        lst.add((long) skill.getEffectType().id);
        lst.add((long) (instancePos.x * 1000));
        lst.add((long) (instancePos.y * 1000));
        lst.add(skill.getEffectType().timeEffect);
        return lst;
    }
}
