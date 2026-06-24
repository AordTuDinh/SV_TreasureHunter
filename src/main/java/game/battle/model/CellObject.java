package game.battle.model;

import game.battle.object.Pos;
import game.treasure.BattleConfig;
import game.treasure.mapping.main.ResObjectEntity;
import game.treasure.service.resource.ResMap;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ozudo.base.helper.DateTime;
import protocol.Pbmethod;

import java.util.ArrayList;
import java.util.List;

/**
 * Không dùng {@code @Data} cho equals/hashCode trên toàn bộ field: {@code curHp}/{@code state} đổi sau khi chết/revive
 * sẽ làm thay đổi hash khi object vẫn nằm trong {@code HashSet} (vd. {@code cellObjectDie}) → một số cell không revive.
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CellObject {
    @EqualsAndHashCode.Include
    int id;
    Pos pos;
    int chunkId;
    Pbmethod.CellState state;
    Pbmethod.CellObjectType objectType;
    int baseHp;
    // runtime data
    int curHp;
    long timeBeAttack;
    int resObjectType;


    public CellObject(Pos pos, int type, int chunkId, int id) {
        this.pos = pos;
        this.chunkId = chunkId;
        this.state = Pbmethod.CellState.ACTIVE;
        this.objectType = Pbmethod.CellObjectType.valueOf(type);
        this.id = id;
        this.resObjectType = type;
        ResObjectEntity resObject = ResMap.getResObject(type);
        this.curHp = resObject.getHp();
        this.baseHp = resObject.getHp();
    }


    public List<Long> getBonusKillMe() {
        ResObjectEntity resObject = ResMap.getResObject(resObjectType);
        if (resObject == null) return new ArrayList<>();
        return resObject.randomBonus();
    }

    /** Mỗi lần đánh trừ 1 máu, không dùng damage của player. */
    public synchronized boolean attack() {
        timeBeAttack = System.currentTimeMillis();
        this.curHp -= 1;
        curHp = Math.max(curHp, 0);
        if (curHp == 0) {
            state = Pbmethod.CellState.HIDE;
            return true;
        }
        return false;
    }


    public boolean canRevive() {
        return curHp < baseHp && DateTime.isAfterTime(timeBeAttack, BattleConfig.timeReviveObject);
    }

    public boolean canAttack() {
        return curHp > 0;
    }

    public void revive() {
        this.curHp = baseHp;
        state = Pbmethod.CellState.ACTIVE;
    }

    public Pbmethod.PbCell toProto() {
        Pbmethod.PbCell.Builder pb = Pbmethod.PbCell.newBuilder();
        pb.setId(id);
        pb.setState(state);
        pb.setHp(curHp);
        return pb.build();
    }


}
