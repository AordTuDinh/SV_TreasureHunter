package game.battle.object;

import game.battle.effect.SkillEffect;
import game.config.aEnum.RankType;
import game.dragonhero.mapping.UserEntity;
import game.dragonhero.mapping.UserWeaponEntity;
import game.dragonhero.mapping.main.ResWeaponEntity;
import game.dragonhero.service.resource.ResWeapon;
import lombok.Data;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.GsonUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class Shuriken implements Serializable {
    int id;
    int avatar;
    RankType rank;
    int slot; // vị trí skill lúc trang bị
    int level;
    float countDown, rangeFly, radius, cacheRangeFly;
    int forcePush, speed, faction;
    List<Integer> shots = new ArrayList<>(); // số tia - số lượng mỗi tia - số lần xuyên
    long timeActiveSkill; // lưu lại thời điểm được sử dụng skill
    SkillEffect effectSkill;
    long numberAttack;
    boolean runaState;
    long runaStateValue;
    long timeBlockCount; // đến time thì mới được đếm
    int degree; // góc bắn ra từ phi tiêu thứ 3

    public Shuriken(Point point, UserWeaponEntity uWe, int slot) {
        ResWeaponEntity res = uWe.getRes();
        this.id = uWe.getWeaponId();
        this.countDown = uWe.getTimeCd(point);
        this.radius = res.getRadius();
        this.slot = slot;
        this.level = uWe.getLevel();
        this.shots = uWe.getInfoAttack();
        this.timeActiveSkill = System.currentTimeMillis();
        this.forcePush = res.getForcePush();
        this.numberAttack = 1;
        this.rangeFly = uWe.getRankFly();
        this.cacheRangeFly = rangeFly;
        this.speed = res.getSpeed();
        runaState = false;
        this.rank = res.getRankType();
        this.faction = res.getFactionType().value;
        effectSkill = uWe.getSkillEffect();
        this.timeBlockCount = 0;
        this.degree = res.getDegree();
    }

    public Shuriken(int weaponId, int level) {
        ResWeaponEntity we = ResWeapon.getWeapon(weaponId);
        this.id = we.getId();
        this.countDown = we.getCooldown();
        this.radius = we.getRadius();
        this.slot = 0;
        this.level = level;
        this.shots = we.getShots();
        this.timeActiveSkill = 0;
        this.forcePush = we.getForcePush();
        this.numberAttack = 1;
        this.faction = we.getFactionType().value;
        this.rangeFly = we.getRangeFly();
        this.cacheRangeFly = rangeFly;
        this.speed = we.getSpeed();
        runaState = false;
        this.rank = we.getRankType();
        effectSkill = we.getSkillEffect(level);
        this.degree = we.getDegree();
        this.timeBlockCount = 0;
    }

    public void buffBossGod(float num) {
        this.rangeFly += num;
    }

    public void removeBuff() {
        this.rangeFly = cacheRangeFly;
    }

    public void setStateRuna(long per) {
        // force reset CD all skill
        decCoolDown(1);
        runaState = true;
        numberAttack = 1; // không đếm nữa
        runaStateValue = per;
    }

    public void setActiveSkill() {
        this.timeActiveSkill = System.currentTimeMillis() + (long) (countDown * DateTime.SECOND2_MILLI_SECOND);
        if (!blockCount()) numberAttack++;
    }

    public void resetJoinMap() {
        this.numberAttack = 1;
    }

    public boolean blockCount() {
        return runaState || System.currentTimeMillis() < timeBlockCount;
    }

    public void setBlockCountTime(float seconds) {
        numberAttack = 1;
        timeBlockCount = System.currentTimeMillis() + (long) (seconds * DateTime.SECOND2_MILLI_SECOND);
    }

    public boolean trigger3TH() {
        return numberAttack > 0 && numberAttack % 3 == 0;
    }

    public boolean trigger4TH() {
        return numberAttack > 0 && numberAttack % 4 == 0;
    }

    public boolean trigger5TH() {
        return numberAttack > 0 && numberAttack % 5 == 0;
    }

    public void decCoolDown(float per) { // %
        float curCD = countDown * per;
        if (!hasActiveSkill()) timeActiveSkill -= (long) (curCD * 1000f);
    }

    public long getCoolDown() { //ms
        if (hasActiveSkill()) return 0;
        long cd = timeActiveSkill - System.currentTimeMillis();
        if (runaState) {
            long reduceCd = (long) (countDown - runaStateValue / 100 * countDown);
            if (cd > reduceCd) {
                cd = reduceCd;
                timeActiveSkill -= (cd - reduceCd);
            }
        }
        return cd < 0 ? 0 : cd;
    }

    public boolean hasActiveSkill() {
        return System.currentTimeMillis() > timeActiveSkill;
    }
}
