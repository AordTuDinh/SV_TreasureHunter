package game.treasure.mapping.main;

import game.battle.object.Pos;
import game.config.aEnum.MapType;
import game.treasure.service.resource.ResTeleport;
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

    public MapType getNextMap() {
        return MapType.get(getNext().map);
    }

    public MapType getMap() {
        return MapType.get(map);
    }

    public ResTeleportEntity getNext() {
        return ResTeleport.getTeleport(nextId);
    }

    public Pos getPlayerPosInit() {
        return playerPosInit.clone();
    }
}
