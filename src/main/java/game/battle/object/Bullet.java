package game.battle.object;

import game.battle.effect.SkillEffect;
import game.battle.model.Character;
import game.battle.model.Player;
import game.battle.type.CharacterType;
import game.config.CfgBattle;
import game.config.aEnum.FactionType;
import game.dragonhero.BattleConfig;
import game.dragonhero.server.Constans;
import lombok.Data;
import ozudo.base.helper.DateTime;
import protocol.Pbmethod;

import java.util.ArrayList;
import java.util.List;

@Data
public class Bullet {
    //region field
    long id;
    float radius;
    int shurikenId, slot;
    Character owner;
    Pos pos;
    Pos instancePos;
    Pos direction;
    int forcePush;
    int faction;
    PanelMap panelMap;
    boolean isAlive;
    CharacterType ownerType; // chủ sở hữu là người chơi
    SkillEffect effectSkill;
    int penetration = 1;// số lượng mục tiêu tối đa được phép xuyên qua
    long dameOfEnemy;
    boolean trigger3TH, trigger4TH, trigger5TH;
    // sau khi tính toán ra mới được dùng
    long atkDame, magDame, timeCreate;
    float rangeFly;
    int speed;
    // for proto
    boolean isHit; // tra ve cho client show effect
    List<Integer> characterAttack;
    // endregion


    public Bullet(Character owner, Pos direction, Pos pos, Shuriken shuriken) {
        this.id = owner.getBattleRoom().getIdBullet();
        this.shurikenId = shuriken.getId();
        this.radius = shuriken.getRadius();
        this.slot = shuriken.getSlot();
        this.owner = owner;
        this.ownerType = owner.getType();
        this.pos = pos.clone();
        this.instancePos = pos.clone();
        this.direction = direction.clone();
        this.panelMap = owner.getPanelMap();
        this.effectSkill = shuriken.getEffectSkill();
        this.isAlive = true;
        if (shuriken.getId() == 29) { // weapon 29 k giới hạn số lần xuyên vật thể.
            this.penetration = 10000;
        } else {
            this.penetration = shuriken.getShots().get(2);
        }
        this.forcePush = shuriken.getForcePush();
        this.trigger3TH = shuriken.trigger3TH();
        this.trigger4TH = shuriken.trigger4TH();
        this.trigger5TH = shuriken.trigger5TH();
        this.faction = shuriken.getFaction();
        this.speed = shuriken.speed;
        this.rangeFly = shuriken.rangeFly;
        this.isHit = false;
        this.timeCreate = System.currentTimeMillis();
        this.characterAttack = new ArrayList<>();
    }

    public FactionType getFaction(){
        return  FactionType.get(faction);
    }

    public boolean initDone() {
        return DateTime.isAfterTime(timeCreate, BattleConfig.B_timeDelayAnim);
    }

    public void setHit() {
        this.isHit = true;
    }

    public void minusPenetration(int id) {
        penetration--;
        characterAttack.add(id);
    }

    public void move() {
        float deltaTime = CfgBattle.periodFixedUpdate / 1000f;
        if (!initDone()) return;
        //Fixme DATE: 7/31/2022 LƯU Ý ---> max size + collision wall -> delete bullet
        if (instancePos.distance(pos) > rangeFly || wallCollisionBullet(owner.getPanelMap())) {
            isAlive = false;
            return;
        }
        float curSpeed = speed / BattleConfig.C_SCALE_SPEED + BattleConfig.B_acceleration * deltaTime;
        Pos nd = Pos.moveFromDirection(direction, curSpeed);
        pos.v_addBullet(owner.getPanelMap(), nd);
    }

    public void moveInPanel(PanelMap panelMap) {
        float deltaTime = CfgBattle.periodFixedUpdate / 1000f;
        if (!initDone()) return;
        //Fixme DATE: 7/31/2022 LƯU Ý ---> max size + collision wall -> delete bullet
        if (instancePos.distance(pos) > rangeFly || wallCollisionBullet(panelMap)) {
            isAlive = false;
            return;
        }
        float curSpeed = speed / BattleConfig.C_SCALE_SPEED + BattleConfig.B_acceleration * deltaTime;
        Pos nd = Pos.moveFromDirection(direction, curSpeed);
        pos.v_add(owner.getPanelMap(), nd);
    }

    public boolean wallCollisionBullet(PanelMap panelMap) {
        if (pos.x > panelMap.topRight.x - radius) return true;
        if (pos.x < panelMap.botLeft.x + radius) return true;
        if (pos.y > panelMap.topRight.y - radius) return true;
        if (pos.y < panelMap.botLeft.y + -radius) return true;
        return false;
    }

    public Player getPlayer() {
        if (ownerType == CharacterType.PLAYER) return (Player) owner;
        return null;
    }

    public Pbmethod.PbUnitAdd.Builder toProtoAdd() {
        Pbmethod.PbUnitAdd.Builder weapon = Pbmethod.PbUnitAdd.newBuilder();
        weapon.setType(Constans.TYPE_BULLET);
        weapon.setId(id);
        weapon.setOwnerId(owner.getId());
        weapon.setDirection(direction.toProto());
        weapon.setIsAdd(true);
        weapon.setSpeed((int) (speed / BattleConfig.C_SCALE_SPEED));
        weapon.setPos(pos.toProto());
        weapon.addAvatar(shurikenId);
        weapon.addInfo(penetration);
        weapon.setFaction(faction);
        weapon.addInfo((int) (rangeFly * 100));
        weapon.setFaction(faction);
        return weapon;
    }

    public Pbmethod.PbUnitAdd.Builder toProtoRemove() {
        Pbmethod.PbUnitAdd.Builder weapon = Pbmethod.PbUnitAdd.newBuilder();
        weapon.setType(Constans.TYPE_BULLET);
        weapon.setId(id);
        weapon.setOwnerId(owner.getId());
        weapon.setIsAdd(false);
        weapon.addInfo(isHit ? 1 : 0);
        weapon.setPos(pos.toProto());
        return weapon;
    }
}
