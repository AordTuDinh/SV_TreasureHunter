package game.battle.model;

import game.battle.object.Point;
import game.battle.object.Pos;
import game.battle.type.CharacterType;
import game.battle.type.StateType;
import game.config.aEnum.FactionType;
import game.dragonhero.BattleConfig;
import game.dragonhero.mapping.UserPetEntity;
import game.dragonhero.mapping.main.ResPetEntity;
import game.dragonhero.server.Constans;
import ozudo.base.helper.DateTime;
import protocol.Pbmethod;

import java.io.Serializable;
import java.util.List;

public class Pet extends Character implements Serializable {
    Player owner;
    float timePetActive;

    public Pet(UserPetEntity uPet, Player owner) {
        this.type = CharacterType.PET;
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
    public Pbmethod.PbUnitAdd.Builder toProtoAdd() {
        Pbmethod.PbUnitAdd.Builder pb = Pbmethod.PbUnitAdd.newBuilder();
        pb.setType(Constans.TYPE_PET);
        pb.setId(id);
        pb.setIsAdd(true);
        pb.setPos(pos.toProto());
        pb.setDirection(direction.toProto());
        pb.setBotLeft(owner.panelMap.getBotLeftP().toProto());
        pb.setTopRight(owner.panelMap.getTopRightP().toProto());
        pb.setTeamId(teamId);
        pb.addAvatar(model);
        pb.setOwnerId(owner.id);
        pb.setSpeed((int) point.getMoveSpeed());
        pb.setFaction(FactionType.NULL.value);
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

    public Pbmethod.PbUnitAdd.Builder toProtoRemove() {
        Pbmethod.PbUnitAdd.Builder builder = Pbmethod.PbUnitAdd.newBuilder();
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
