package game.dragonhero.table;

import game.battle.model.Character;
import game.battle.object.Coroutine;
import game.battle.object.GameCore;
import game.battle.type.RoomState;
import game.config.CfgBattle;
import game.dragonhero.mapping.main.BaseMap;
import game.object.TaskMonitor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Setter
@NoArgsConstructor
public abstract class BaseBattleRoom extends BaseRoom {
    long timeStartGame, timeEndGame;
    @Getter
    long idBullet = 0;  // gen id mỗi lần tấn công để xách minh ai đnáh, tự động tăng dần
    @Getter
    int idEffectClient = 0;
    @Getter
    int numberMonsterDie;
    @Getter
    List<Character> aToxicBoy = new ArrayList<>();// chứa những thằng có effect melee trong người
    @Getter
    Map<Integer, List<Integer>> aEffectRoomInfo = new HashMap<>(); // cache userId -  skill Id active
    @Getter
    GameCore controller;


    public BaseBattleRoom(BaseMap mapInfo, List<Character> aPlayer, String keyRoom, boolean allowReviveEnemy) {
        super(mapInfo, aPlayer, keyRoom, allowReviveEnemy);
    }

    protected void startInit() {
        super.startInit();
        this.idBullet = (long) Math.pow(10, 7);
        this.server_time = 0;
        this.numberMonsterDie = 0;
        this._dte = System.currentTimeMillis();
        this.controller = new GameCore();
        coroutines = new ArrayList<>();
        // proto
        pbInit.setMapId(keyRoom);
        pbInit.setBattleId(id);
        key1 = TaskMonitor.getInstance().submit(this, CfgBattle.periodUpdate);
        key2 = TaskMonitor.getInstance().submit(this, CfgBattle.periodFixedUpdate);
        key3 = TaskMonitor.getInstance().submit(this, CfgBattle.periodUpdateLow);
        key4 = TaskMonitor.getInstance().submit(this, CfgBattle.periodEffectUpdate);
        key5 = TaskMonitor.getInstance().submit(this, CfgBattle.periodUpdate1s);
    }

    protected void clearRoom() {
        aEffectRoomInfo.clear();
    }

    public synchronized long getIdBullet() {
        idBullet++;
        if (idBullet > Long.MAX_VALUE) idBullet = 0;
        return idBullet;
    }


    public synchronized int getIdEffectClient() {
        if (idEffectClient + 1 > Integer.MAX_VALUE) {
            idEffectClient = 0;
        } else idEffectClient++;
        return idEffectClient;
    }

    public void addCharacterToxic(Character character) {
//          debug("---------------------- > character " + character.getId() + " Meeleeeeeeeeeeeeeee ");
        if (roomState != RoomState.ACTIVE) return;
        aToxicBoy.add(character);
    }

    public void removeCharacterMelee(Character character) {
        //  debug("---------------------- > character " + character.getId() + " Meeleeeeeeeeeeeeee ");
        if (roomState != RoomState.ACTIVE) return;
        aToxicBoy.remove(character);
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
        if (keyRoom != null) TaskMonitor.getInstance().removeRoom(keyRoom);
    }


    public void EffectUpdate() {
        if (roomState != RoomState.ACTIVE) return;
        controller.EffectUpdate(this);
        if (isBattleRoom) {
            for (int i = 0; i < aPet.size(); i++) {
                aPet.get(i).getPet().processSkill();
            }
        }

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

    public void ChangeCharacterHp(Character attacker, Character beDamage, long atk, long mAtk) {

    }

    public int getTimeAttack() { // seconds
        return (int) ((timeEndGame - timeStartGame) / 1000f);
    }


    public void setEndGameState() {
        this.timeEndGame = System.currentTimeMillis();
        this.roomState = RoomState.END;
        addCoroutine(new Coroutine(1f, this::cancelTask));
    }
}
