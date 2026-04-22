package game.battle.model;

import com.mysql.cj.util.TimeUtil;
import game.battle.object.Pos;
import game.object.BonusConfig;
import game.treasure.BattleConfig;
import game.treasure.mapping.main.ResMapEntity;
import game.treasure.mapping.main.ResObjectEntity;
import game.treasure.service.resource.ResMap;
import lombok.Data;
import ozudo.base.helper.DateTime;
import protocol.Pbmethod;

import javax.persistence.Transient;

import java.util.List;

import static game.treasure.BattleConfig.CHUNK_SIZE;


@Data
public class CellObject {
    int id;
    Pos pos;
    int chunkId;
    Pbmethod.CellState state;
    Pbmethod.CellObjectType objectType;
    int baseHp;
    // runtime data
    int curHp;
    long timeBeAttack;
    List<BonusConfig> bonusConfig;


    public CellObject(Pos pos, int type, int chunkId, int id) {
        this.pos = pos;
        this.chunkId = chunkId;
        this.state = Pbmethod.CellState.ACTIVE;
        this.objectType = Pbmethod.CellObjectType.valueOf(type);
        this.id = id;
        ResObjectEntity resObject = ResMap.getResObject(type);
        this.curHp = resObject.getHp();
        this.baseHp = resObject.getHp();
        this.bonusConfig = resObject.getBonus();
    }


    public List<Long> getBonusKillMe(){
        return BonusConfig.getRandomOneBonus(bonusConfig);
    }

    public synchronized boolean attack(int damage) {
        timeBeAttack = System.currentTimeMillis();
        this.curHp -= damage;
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
