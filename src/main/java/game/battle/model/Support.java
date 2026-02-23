package game.battle.model;

import game.battle.calculate.IMath;
import game.battle.effect.Effect;
import game.battle.object.Bullet;
import game.battle.object.PanelMap;
import game.battle.object.Pos;
import game.battle.type.CharacterType;
import game.config.aEnum.FactionType;
import game.battle.type.StateType;
import game.dragonhero.mapping.main.ResBossEntity;
import game.dragonhero.server.Constans;
import game.dragonhero.service.battle.TriggerType;
import game.dragonhero.table.BaseBattleRoom;
import game.object.PointBuff;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class Support extends Character implements Serializable {
    private static final int MODEL_STONE = 6;
    private static final int MODEL_TOTEM = 5;
    private static final int MODEL_CAGE = 8;
    int enemyKey;

    public Support(ResBossEntity boss, Pos startPos, int teamId, BaseBattleRoom room) {
        this.name = boss.getName();
        this.model = boss.getModel();
        this.point = boss.toPoint();
        this.hasDamage = (point.getMagicDamage() > 0 || point.getAttackDamage() > 0);
        this.radius = boss.getRadius();
        this.type = boss.getCharacterType();
        this.pos = startPos;
        this.id = room.getIdNext();
        this.enemyKey = boss.getId();
        this.teamId = teamId;
        this.instancePos = pos.clone();
        this.alive = true;
        this.room = room;
        this.direction = Pos.right();
        this.hasBonusKillMe = false;
        this.panelMap = new PanelMap(room.getMapInfo().getMapData());
        this.isMove = false;
//        this.f0Toxic = new ArrayList<>();
        this.effectsBody = new ArrayList<>();
        this.attackerInfo = new HashMap<>();
        this.timeJoinRoom = System.currentTimeMillis();
    }

    @Override
    public void protoDie(Character killer) {
        super.protoDie(killer);
        protoStatus(StateType.DIE, (long) FactionType.NULL.value);
        if (type == CharacterType.TOTEM || type == CharacterType.STONE || type == CharacterType.CAGE
                || type == CharacterType.MOC1|| type == CharacterType.MOC2|| type == CharacterType.MOC3|| type == CharacterType.MOC4) {
            ((BaseBattleRoom) room).removeSupport(this);
        }
    }

    @Override
    public void beAttackEffect(Effect effect, long atkDame, long magicDame, PointBuff... buffs) {

    }

    @Override
    public void beAttackBullet(Bullet bullet) {
        if (!canBeAttack(bullet.getOwner().getTeamId())) return;
        if (bullet.getCharacterAttack().contains(id)) return;
        bullet.minusPenetration(id);
        List<Long> effs = new ArrayList<>();
        List<Long> eff = processTrigger(TriggerType.HIT, bullet);
        if (eff.size() > 0) effs.addAll(eff);
        if (triggerFirstAttack(bullet.getOwner(), bullet)) {
            eff = processTrigger(TriggerType.FIRST_HIT, bullet);
            if (eff.size() > 0) effs.addAll(eff);
        }
        if (bullet.isTrigger3TH()) {
            eff = processTrigger(TriggerType.TH3, bullet);
            if (eff.size() > 0) effs.addAll(eff);
        }

        if (bullet.isTrigger4TH()) {
            processTrigger(TriggerType.TH4, bullet);
        }
        if (bullet.isTrigger5TH()) {
            eff = processTrigger(TriggerType.TH5, bullet);
            if (eff.size() > 0) effs.addAll(eff);
        }
        long[] damage;
        if (type == CharacterType.STONE || type == CharacterType.CAGE ||  type == CharacterType.MOC1 || type == CharacterType.MOC2
        || type == CharacterType.MOC3 || type == CharacterType.MOC4) {
            damage = new long[]{0L, 1L, 0L};
        } else damage = IMath.calculateDamage(bullet, this, effs);

        addAtkInfoMelee(bullet);
        updateHp(bullet.getOwner(), -damage[1], -damage[2]);
        processTriggerLastDame(bullet.getOwner(), bullet.getEffectSkill(), damage, bullet.getFaction());
        protoRangeDame(bullet.getOwner(), Arrays.asList((long) bullet.getOwner().getId(), damage[0], damage[1], damage[2], (long) bullet.getFaction().value, (long) (pos.x * 1000), (long) (pos.y * 1000)));
        if (point.getCurHP() <= 0) {
            alive = false;
            timeDie = System.currentTimeMillis();
            protoDie(bullet.getOwner());
            if (checkHasBonusKill()) // tránh trường hợp gửi nhiều lần
                bonusKillMe(bullet.getOwner());
        } else {
            isBeAttack = true;
            timeBeHit = System.currentTimeMillis();
            targetAttack = bullet.getOwner();
        }
    }

    @Override
    public void activeSkill(int skillId) {

    }

    public protocol.Pbmethod.PbCharInfo toProtoInfo() {
        protocol.Pbmethod.PbCharInfo.Builder pbUser = protocol.Pbmethod.PbCharInfo.newBuilder();
        pbUser.setName(name);
        pbUser.setLevel(0);
        pbUser.setAlive(alive);
        pbUser.addAllPoint(point.toProto());
        return pbUser.build();
    }

    public protocol.Pbmethod.PbUnitAdd.Builder toProtoAdd() {
        protocol.Pbmethod.PbUnitAdd.Builder pbAdd = protocol.Pbmethod.PbUnitAdd.newBuilder();
        pbAdd.setType(Constans.TYPE_MONSTER);
        pbAdd.setId(id);
        pbAdd.setTeamId(teamId);
        pbAdd.addAvatar(enemyKey);
        pbAdd.setIsAdd(true);
        pbAdd.setRangeAttack(rangeAttack);
        pbAdd.setPos(pos.toProto());
        pbAdd.setDirection(direction.toProto());
        pbAdd.setSpeed((int) point.getMoveSpeed());
        pbAdd.addInfo(type.value);
        pbAdd.addInfo(model);// info[1]= key
        pbAdd.addInfo(idDameSkin);
        pbAdd.addInfo(idChatFrame);
        pbAdd.addInfo(idTrial);
        pbAdd.setCharacterInfo(toProtoInfo());
        pbAdd.setFaction(FactionType.NULL.value);
        return pbAdd;
    }
}
