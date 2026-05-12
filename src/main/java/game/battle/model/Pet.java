package game.battle.model;

import game.battle.object.Point;
import game.battle.object.Pos;
import game.battle.type.UnitType;
import game.treasure.BattleConfig;
import game.treasure.mapping.UserPetEntity;
import game.treasure.mapping.main.ResPetEntity;
import ozudo.base.helper.DateTime;
import protocol.Pbmethod;

import java.io.Serializable;

public class Pet extends Unit implements Serializable {
    Player owner;

    public Pet(UserPetEntity uPet, Player owner) {
        this.type = UnitType.PET;
        this.model = uPet.getPetId();
        this.direction = Pos.RandomDirection();
        this.clanId = 0;
        this.pos = owner.getPos().clone();
        this.point = new Point();
        this.point.setMoveSpeed(100);
        this.owner = owner;
        ResPetEntity res = uPet.getResPet();
    }

    @Override
    public void Update() {

    }

    @Override
    public Pbmethod.PbUnit toProtoAdd(int chunkId) {
        Pbmethod.PbUnit.Builder pb = Pbmethod.PbUnit.newBuilder();
        pb.setType(UnitType.PET.value);
        pb.setId(id);
        pb.setChunkId(chunkId);
        pb.setIsAdd(true);
        pb.setPos(pos.toProto());
        pb.setDirection(direction.toProto());
        pb.setClanId(clanId);
        pb.setAvatar(model);
        pb.setOwnerId(owner.id);
        pb.setSpeed((int) point.getMoveSpeed());
        return pb.build();
    }

    public boolean isMove() {
        this.isMove = !DateTime.isAfterTime(timeActionMove, BattleConfig.P_timeNoMove);
        return isMove;
    }
}
