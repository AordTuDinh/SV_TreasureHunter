package game.dragonhero.mapping;

import game.dragonhero.mapping.main.ResPieceEntity;
import game.dragonhero.service.resource.ResWeapon;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Data
@Entity
@NoArgsConstructor
@Table(name = "user_piece")
public class UserPieceEntity implements Serializable {
    @Id
    int userId, type, id;
    int number;

    public UserPieceEntity(int userId, int type, int id, int number) {
        this.userId = userId;
        this.id = id;
        this.number = number;
        this.type = type;
    }

    public ResPieceEntity getRes() {
        return ResWeapon.getPiece(type, id);
    }

    public void add(int value) {
        this.number += value;
    }

    public protocol.Pbmethod.PbPiece.Builder toProto() {
        protocol.Pbmethod.PbPiece.Builder pb = protocol.Pbmethod.PbPiece.newBuilder();
        pb.setType(type);
        pb.setId(id);
        pb.setNumber(number);
        return pb;
    }
}
