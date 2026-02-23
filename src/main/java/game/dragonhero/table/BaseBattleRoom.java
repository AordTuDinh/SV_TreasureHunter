package game.dragonhero.table;

import game.battle.effect.EffectRoom;
import game.battle.effect.SkillEffect;
import game.battle.model.Character;
import game.battle.model.Support;
import game.battle.object.Bullet;
import game.battle.object.Coroutine;
import game.battle.object.GameCore;
import game.battle.type.RoomState;
import game.config.CfgBattle;
import game.dragonhero.mapping.main.BaseMap;
import game.dragonhero.mapping.main.ResBossEntity;
import game.dragonhero.service.battle.TriggerType;
import game.dragonhero.service.resource.ResEnemy;
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
    List<Bullet> aBullets = new ArrayList<>();
    @Getter
    List<EffectRoom> aEffectRoom = new ArrayList<>(); // cùng add vào đây nhưng xử lí ở nhiều chỗ, tùy vào thời gian tác dụng
    @Getter
    Map<Integer, EffectRoom> mEffectClient = new HashMap<>();
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
        this.aBullets = new ArrayList<>();
        this.aEffectRoom = new ArrayList<>();
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
//        aPlayer.forEach(player -> removeCharacter(player));
//        aToxicBoy.forEach(player -> removeCharacter(player));
//        aMonster.forEach(monster -> removeCharacter(monster));
        for (int i = 0; i < aBullets.size(); i++) {
            removeBullet(aBullets.get(i));
        }
        for (int i = 0; i < aEffectRoom.size(); i++) {
            removeEffect(aEffectRoom.get(i));
        }
        aEffectRoomInfo.clear();
    }

    public void addEffectClient(EffectRoom effectRoom) {
        mEffectClient.put(effectRoom.getId(), effectRoom);
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

    public void addEffectRoom(EffectRoom eff) {
        try {
            if (roomState != RoomState.ACTIVE) return;
            if (!aEffectRoomInfo.containsKey(eff.getOwner().getId())) {
                aEffectRoomInfo.put(eff.getOwner().getId(), new ArrayList<>());
            }
            List<Integer> effActive = aEffectRoomInfo.get(eff.getOwner().getId());
            if (eff.getSkill().getEffectType().isIncreRoom) {
                aEffectRoom.add(eff);
                effActive.add(eff.getSkill().getEffectType().id);
            } else {
                if (effActive.stream().filter(effId -> effId == eff.getSkill().getEffectType().id).count() == 0) {
                    aEffectRoom.add(eff);
//                    System.out.println("eff.getSkill().getEffectType().id = " + eff.getSkill().getEffectType().id);
                    effActive.add(eff.getSkill().getEffectType().id);
//                    System.out.println("effActive = " + effActive);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public synchronized void addBullet(Character attacker, int skillIndex, List<Bullet> lstB) {
        if (roomState != RoomState.ACTIVE) return;
        aBullets.addAll(lstB);
        for (int i = 0; i < lstB.size(); i++) {
            aProtoAdd.add(lstB.get(i).toProtoAdd());
        }
    }

    public void removeBullet(Bullet bullet) {
        if (roomState != RoomState.ACTIVE) return;
        //check trigger destroy trước khi remove
        processTriggerDestroy(bullet);
        aProtoAdd.add(bullet.toProtoRemove());
        aBullets.remove(bullet);
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


    protected void processTriggerDestroy(Bullet bullet) {
        if (roomState != RoomState.ACTIVE) return;
        SkillEffect eff = bullet.getEffectSkill();
        if (!eff.isHasEffect() || eff.getTriggerType() != TriggerType.DESTROY) return;
        switch (eff.getEffectType()) {
            case EXPLODE: // Create
            case INF: // Create
                if (bullet.getOwner().getPoint().getCurMP() >= eff.getMP()) {
                    if (eff.getMP() > 0) bullet.getOwner().updateMp(-eff.getMP());
                    EffectRoom effAdd = new EffectRoom(bullet.getOwner(), bullet.getPos().clone(), eff);
                    addEffectRoom(effAdd);
                }
//                else debug("Không đủ mana ---->>> ");
                break;
            case HOA_THAN_NORMAL: // Create
                EffectRoom effAdd = new EffectRoom(bullet.getOwner(), bullet.getPos(), eff);
                addEffectRoom(effAdd);
                break;
            case THO_THAN_1:
                if (!bullet.isHit()) { // Không trúng player mới tạo đá
                    BossGodRoom room = ((BossGodRoom) this);
                    ResBossEntity support = ResEnemy.getBoss(41);
                    Support stone = new Support(support, bullet.getPos(), bullet.getOwner().getTeamId(), room);
                    room.addSupport(stone);
                }
                break;
        }

    }

    public void removeEffect(EffectRoom effect) {
        if (roomState != RoomState.ACTIVE) return;
        List<Integer> effActive = aEffectRoomInfo.get(effect.getOwner().getId()); // never null
        if (effActive != null) effActive.remove(Integer.valueOf(effect.getSkill().getEffectType().id));
        aEffectRoom.remove(effect);
    }

    public void removeSupport(Support support) {
    }
}
