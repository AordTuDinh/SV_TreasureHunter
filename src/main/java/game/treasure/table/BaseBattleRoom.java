package game.treasure.table;

import game.battle.model.Unit;
import game.battle.model.ChunkObject;
import game.battle.object.Coroutine;
import game.battle.object.GameCore;
import game.battle.type.RoomState;
import game.config.CfgBattle;
import game.treasure.mapping.main.ResMapEntity;
import game.treasure.server.Constans;
import game.object.TaskMonitor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Map;

@Setter
@NoArgsConstructor
public abstract class BaseBattleRoom extends BaseRoom {
    @Getter
    GameCore controller;


    public BaseBattleRoom(ResMapEntity mapInfo, Map<Integer, ChunkObject> mChunk, String keyRoom) {
        super(mapInfo, mChunk, keyRoom);
    }

    protected void startInit() {
        super.startInit();
        this.serverTime = 0;
        this._dte = System.currentTimeMillis();
        this.controller = new GameCore();
        coroutines = new ArrayList<>();
        // proto
        key1 = TaskMonitor.getInstance().submit(this, CfgBattle.periodUpdate);
        key2 = TaskMonitor.getInstance().submit(this, CfgBattle.periodFixedUpdate);
        key3 = TaskMonitor.getInstance().submit(this, CfgBattle.periodUpdateLow);
        key4 = TaskMonitor.getInstance().submit(this, CfgBattle.periodEffectUpdate);
        key5 = TaskMonitor.getInstance().submit(this, CfgBattle.periodUpdate1s);
    }

    protected void clearRoom() {

    }

    @Override
    public void Update() {
        super.Update();
        controller.Update(this);
    }


    protected void cancelTask() {
        //System.out.println("cancelTask ------------------- " + keyRoom);
        clearRoom();
        if (key1 != null) TaskMonitor.getInstance().cancel(key1);
        if (key2 != null) TaskMonitor.getInstance().cancel(key2);
        if (key3 != null) TaskMonitor.getInstance().cancel(key3);
        if (key4 != null) TaskMonitor.getInstance().cancel(key4);
        if (key5 != null) TaskMonitor.getInstance().cancel(key5);
        TaskMonitor.getInstance().removeRoom(battleId);
    }


    public void EffectUpdate() {
        if (roomState != RoomState.ACTIVE) return;
        controller.EffectUpdate(this);
    }

    public synchronized void FixedUpdate() {
        controller.FixedUpdate(this);
        for (int i = 0; i < coroutines.size(); i++) {
            Coroutine coroutine = coroutines.get(i);
            if (System.currentTimeMillis() > coroutine.timeAction) {
                coroutine.action.Call();
                removeCoroutine(coroutine);
            }
        }
    }

    public void LastUpdate() {
        if (roomState != RoomState.ACTIVE) return; // vẫn phải giữ cái này
        try {
            controller.LastUpdate(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    public void removeCoroutine(Coroutine coroutine) {
        coroutines.remove(coroutine);
    }

    public void ChangeCharacterHp(Unit attacker, Unit beDamage, long atk, long mAtk) {

    }
}
