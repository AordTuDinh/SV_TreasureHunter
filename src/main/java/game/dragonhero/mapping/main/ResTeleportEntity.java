package game.dragonhero.mapping.main;

import game.battle.object.Pos;
import game.config.aEnum.RoomType;
import game.dragonhero.service.resource.ResTeleport;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

@Entity
@NoArgsConstructor
public class ResTeleportEntity extends BaseEntity { // Đi từ map->nextId(id của teleport)
    @Getter
    @Id
    int id;
    int nextId, map; // map : curMap, next : đi đến map nào
    @Getter
    int mapId;
    String playerPos, pos;
    @Getter
    int enable;
    @Transient
    Pos playerPosInit;

    public void init() {
        checkJson(id, playerPos);
        checkJson(id, pos);
        playerPosInit = new Pos(GsonUtil.strToListFloat(playerPos));
        if (playerPosInit.equals(Pos.zero())) {
            Pos teleport = new Pos(GsonUtil.strToListFloat(pos));
            playerPosInit = teleport;
        }
    }

    public RoomType getNextMap() {
        return RoomType.get(getNext().map);
    }

    public RoomType getMap() {
        return RoomType.get(map);
    }

    public ResTeleportEntity getNext() {
        return ResTeleport.getTeleport(nextId);
    }

    public Pos getPlayerPosInit() {
        return playerPosInit.clone();
    }
}
