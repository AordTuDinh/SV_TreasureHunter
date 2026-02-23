package game.battle.object;

import game.dragonhero.mapping.UserWeaponEntity;
import lombok.Data;
import protocol.Pbmethod;

import java.io.Serializable;
import java.util.List;

@Data
public class WeaponBattle implements Serializable {
    int id;
    int slot;
    int level;
    float countDown;
    List<Integer> shots;


    public WeaponBattle(Point point, UserWeaponEntity uWe, int slot) {
        this.id = uWe.getWeaponId();
        this.slot = slot;
        this.level = uWe.getLevel();
        this.countDown = uWe.getTimeCd(point);
        this.shots = uWe.getInfoAttack();
    }

    public Pbmethod.PbArenaWeapon toProto() {
        Pbmethod.PbArenaWeapon.Builder pb = Pbmethod.PbArenaWeapon.newBuilder();
        pb.setId(id);
        pb.setLevel(level);
        pb.setSlot(slot);
        return pb.build();
    }

    public Shuriken toShuriken(Point point, int level) {
        Shuriken shu = new Shuriken(id, level);
        // tinh lai CountDown
        float maxReduce = 0.3f * shu.getCountDown(); //giam toi da 70% time
        float timeTmp = shu.getCountDown() - point.get(Point.COOLDOWN) * shu.getCountDown() / 100f;
        shu.setCountDown(timeTmp < maxReduce ? maxReduce : timeTmp);
        shu.setForcePush(0);
        shu.setLevel(level);
        shu.setRangeFly(20f);
        shu.getShots().set(0, 1);
        return shu;
    }


    public Pbmethod.PbBattleArenaWeapon toBattleProto() {
        Pbmethod.PbBattleArenaWeapon.Builder pb = Pbmethod.PbBattleArenaWeapon.newBuilder();
        pb.setId(id);
        pb.setSlot(slot);
        pb.setLevel(level);
        pb.addAllShots(shots);
        return pb.build();
    }
}
