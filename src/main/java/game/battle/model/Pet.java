package game.battle.model;

import game.battle.object.Point;
import game.battle.object.Pos;
import game.battle.type.UnitType;
import game.battle.type.StateType;
import game.config.aEnum.FactionType;
import game.treasure.BattleConfig;
import game.treasure.mapping.UserPetEntity;
import game.treasure.mapping.main.ResPetEntity;
import game.treasure.server.Constans;
import ozudo.base.helper.DateTime;
import protocol.Pbmethod;

import java.io.Serializable;
import java.util.List;

public class Pet extends Character implements Serializable {
    Player owner;
    float timePetActive;

    public Pet(UserPetEntity uPet, Player owner) {
        this.type = UnitType.PET;
        this.model = uPet.getPetId();
        this.direction = Pos.RandomDirection();
        this.teamId = 0;
        this.pos = owner.getPos().clone();
        this.point = new Point();
        this.point.setMoveSpeed(100);
        this.owner = owner;
        ResPetEntity res = uPet.getResPet();
        this.faction = FactionType.get(res.getFaction());
        this.timePetActive = res.getTimeActive();
    }

    @Override
    public void Update() {

    }

    @Override
    public Pbmethod.PbUnit.Builder toProtoAdd() {
        Pbmethod.PbUnit.Builder pb = Pbmethod.PbUnit.newBuilder();
        pb.setType(Constans.TYPE_PET);
        pb.setId(id);
        pb.setIsAdd(true);
        pb.setPos(pos.toProto());
        pb.setDirection(direction.toProto());
        pb.setTeamId(teamId);
        pb.addAvatar(model);
        pb.setOwnerId(owner.id);
        pb.setSpeed((int) point.getMoveSpeed());
        return pb;
    }


    public boolean isMove() {
        this.isMove = !DateTime.isAfterTime(timeActionMove, BattleConfig.P_timeNoMove);
        return isMove;
    }

    public void setPosAndDirection(Pos newPos, Pos newDirection) {
        this.pos = newPos.round();
        this.direction = newDirection.normalized();
        this.setMove(true);
    }

    public void processSkill() {

    }

    public Pbmethod.PbUnit.Builder toProtoRemove() {
        Pbmethod.PbUnit.Builder builder = Pbmethod.PbUnit.newBuilder();
        builder.setType(Constans.TYPE_PET);
        builder.setId(id);
        builder.setIsAdd(false);
        return builder;
    }

    @Override
    public void activeSkill(int skillId) {
        setTimeAttack();
        protoStatus(StateType.PET_USE_SKILL, List.of((long) teamId));
    }
}
