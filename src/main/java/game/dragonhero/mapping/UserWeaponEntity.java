package game.dragonhero.mapping;

import game.battle.object.Point;
import game.battle.effect.SkillEffect;
import game.battle.object.WeaponBattle;
import game.dragonhero.mapping.main.ResWeaponEntity;
import game.dragonhero.service.resource.ResWeapon;
import game.object.PassiveWeapon;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import protocol.Pbmethod;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@Entity
@Table(name = "user_weapon")
@NoArgsConstructor
public class UserWeaponEntity implements Serializable {
    @Id
    int userId, weaponId;
    int level, bless;

    // luc tao tai khoan thi cho no 5 cai phi tieu
    public UserWeaponEntity(int userId, int weaponId) {
        this.userId = userId;
        this.weaponId = weaponId;
        this.level = 1;
        this.bless = 0;
    }

    public UserWeaponEntity(UserEntity user, int weaponId, int level) {
        this.userId = user.getId();
        this.weaponId = weaponId;
        this.level = level;
    }

    public float getTimeCd(Point point) {
        float maxReduce = 0.3f * getRes().getCooldown(); //giam toi da 70% time
        float timeTmp = getRes().getCooldown() - point.get(Point.COOLDOWN) * getRes().getCooldown() / 100f;
        return timeTmp < maxReduce ? maxReduce : timeTmp;
    }

    public float getRankFly() {
        List<Float> range = getRes().getRange();
        return range.get(0) + range.get(1) * level;
    }

    public ResWeaponEntity getRes() {
        return ResWeapon.getWeapon(weaponId);
    }

    public float getPerPower() {
        ResWeaponEntity res = getRes();
        return res.getAtkDame().isEmpty() ? 0L : (res.getAtkDame().get(0) + res.getAtkDame().get(1) * (level)) / 100f;
    }

    public SkillEffect getSkillEffect() {
        return new SkillEffect(getRes(), level);
    }

    public List<Integer> getInfoAttack() {
        ResWeaponEntity res = getRes();
        List<Integer> info = new ArrayList<>(res.getShots()); // số tia - số đạn mỗi tia - số lần xuyên
        // set theo level
        List<Integer> skills = res.getUpSkill();
        for (int i = 0; i < skills.size(); i += 2) {
            if (level >= skills.get(i)) {
                if (skills.get(i + 1) == 1) { // cộng thêm tia
                    info.set(0, info.get(0) + 1);
                } else { // cộng thêm số lần xuyên thấu
                    info.set(2, info.get(2) + 1);
                }
            } else break; // break luôn vì sô sau luôn cao hơn số trước
        }
        return info;
    }

    public WeaponBattle toWeaponBattle(Point point, int slot) {
        return new WeaponBattle(point, this, slot);
    }

    public boolean updateUpLevel() {
        if (update(List.of("level", level + 1, "bless", 0))) {
            level++;
            bless = 0;
            return true;
        }
        return false;
    }

    public boolean updateBless() {
        if (update(List.of("bless", bless + 1))) {
            bless++;
            return true;
        }
        return false;
    }

    public boolean update(List<Object> updateData) {
        return DBJPA.update("user_weapon", updateData, Arrays.asList("user_id", userId, "weapon_id", weaponId));
    }

    public Pbmethod.PbUserWeapon toProto(UserEntity user, Point point) {
        Pbmethod.PbUserWeapon.Builder pb = Pbmethod.PbUserWeapon.newBuilder();
        pb.setId(weaponId);
        pb.setLevel(level);
        pb.setNumber(1);
        pb.setTimeCd(getTimeCd(point));
        pb.setIsEquid(user.isEquipWeapon(weaponId) ? 1 : 0);
        pb.setBless(bless);
        return pb.build();
    }
}
