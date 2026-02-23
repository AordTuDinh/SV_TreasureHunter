package game.dragonhero.mapping;

import game.battle.calculate.IMath;
import game.battle.object.HeroBattle;
import game.battle.object.Point;
import game.config.CfgPet;
import game.config.aEnum.PetType;
import game.dragonhero.mapping.main.ResEnemyEntity;
import game.dragonhero.mapping.main.ResPetEntity;
import game.dragonhero.service.resource.ResEnemy;
import game.dragonhero.service.resource.ResPet;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.StringHelper;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@Table(name = "user_pet")
public class UserPetEntity implements Serializable {
    @Id
    int userId, petId, type;
    int star;
    int server;
    String bonusStar;
    Date timeCare;
    @Transient
    Point point;

    public UserPetEntity(UserEntity user , int type, int petId) {
        this.userId = user.getId();
        this.server = user.getServer();
        this.type = type;
        this.petId = petId;
        this.star = 0;
        this.timeCare = Calendar.getInstance().getTime();
        this.bonusStar = "[]";
    }

    public PetType getType() {
        return PetType.get(type);
    }

    public List<Integer> getBonusStar() {
        return GsonUtil.strToListInt(bonusStar);
    }

    public ResPetEntity getResPet() {
        return ResPet.getPet(petId);
    }

    public ResEnemyEntity getResMonster() {
        return ResEnemy.getEnemy(petId);
    }

    public int getHp() {
        int day = (int) ((Calendar.getInstance().getTime().getTime() - timeCare.getTime()) / DateTime.DAY_MILLI_SECOND);
        return Math.max(CfgPet.getMaxHpByStar(star) - day * CfgPet.HP_1_DAY, 0);
    }

    public void addHp(int num) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -(CfgPet.getMaxHpByStar(star) / CfgPet.HP_1_DAY));
        if (timeCare.getTime() > cal.getTimeInMillis()) cal.setTime(timeCare);
        cal.add(Calendar.DATE, num);
        this.timeCare = cal.getTime();
    }

    public int getNeedFood() {
        int hp = getHp();
        if (hp == CfgPet.getMaxHpByStar(star)) return 0;
        return (CfgPet.getMaxHpByStar(star) - hp) / CfgPet.HP_1_DAY + 1;
    }

    public HeroBattle toHeroBattle(int team, int id) {
        return new HeroBattle(this, team, id, point == null ? new Point() : point);
    }

    public protocol.Pbmethod.PbPet.Builder toProto() {
        protocol.Pbmethod.PbPet.Builder pb = protocol.Pbmethod.PbPet.newBuilder();
        pb.setId(petId);
        pb.setType(type);
        pb.setStar(star);
        pb.setHp(getHp());
        pb.setMaxHp(CfgPet.getMaxHpByStar(star));
        pb.setPower(IMath.calPowerPet(this));
        pb.addAllBonusStar(getBonusStar());
        return pb;
    }

    public boolean updateStar() {
        List<Integer> bonusStar = getBonusStar();
        bonusStar.add(CfgPet.config.bonusStar.get(star));
        if (getHp() / 10 > 0) {
            int oldHp = CfgPet.getMaxHpByStar(star);
            int curHp = CfgPet.getMaxHpByStar(star + 1);
            long time = timeCare.getTime() - DateTime.DAY_MILLI_SECOND * (curHp - oldHp) / 10;
            timeCare = new Date(time);
        }
        if (update(List.of("star", star + 1, "bonus_star", StringHelper.toDBString(bonusStar), "time_care", timeCare))) {
            this.star += 1;
            this.bonusStar = bonusStar.toString();
            return true;
        } else return false;
    }

    public boolean update(List<Object> lst) {
        return DBJPA.update("user_pet", lst, List.of("user_id", userId, "type", type, "pet_id", petId));
    }
}
