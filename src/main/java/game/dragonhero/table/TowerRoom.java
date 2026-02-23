package game.dragonhero.table;

import game.battle.model.Character;
import game.battle.model.Enemy;
import game.battle.model.Player;
import game.battle.object.Coroutine;
import game.battle.object.EnemyWave;
import game.battle.object.Pos;
import game.battle.type.RoomState;
import game.battle.type.StateType;
import game.config.CfgEventDrop;
import game.config.aEnum.DetailActionType;
import game.config.aEnum.PopupType;
import game.dragonhero.controller.AHandler;
import game.dragonhero.mapping.main.BaseMap;
import game.dragonhero.mapping.main.ResEnemyEntity;
import game.dragonhero.mapping.main.ResTowerEntity;
import game.dragonhero.service.Services;
import game.dragonhero.service.resource.ResEnemy;
import game.dragonhero.service.resource.ResTower;
import game.dragonhero.service.user.Bonus;
import ozudo.base.helper.DateTime;

import java.util.*;

public class TowerRoom extends BaseBattleRoom {
    protected static final float timeDelayInstanceEnemy = 3f; //  sau time thi cho enemy xuat hien
    ResTowerEntity resTower;
    int maxEnemy = 0;
    float offsetEnemy = 3f;
    int indexWave = 0;
    int maxWaveInstance;
    int numKillWave = 0;
    List<Long> timeInstanceWave = new ArrayList<>();
    static final Map<Integer, Pos> mapEnemy = new HashMap<>() {{
        put(0, new Pos(-2, 2f));
        put(1, new Pos(-2, -2f));
        put(2, new Pos(2, 2f));
        put(3, new Pos(2, -2f));
        put(4, new Pos(-2, 0));
        put(5, new Pos(2, 0));
        put(6, new Pos(0, 2f));
        put(7, new Pos(0, -2f));
        put(8, new Pos(-1, 1f));
        put(9, new Pos(-1, -1f));
        put(10, new Pos(1, 1f));
        put(11, new Pos(1, -1f));
    }};

    public TowerRoom(BaseMap mapInfo, List<Character> aPlayer, String keyRoom, int level) {
        super(mapInfo, aPlayer, keyRoom, false);
        resTower = ResTower.mTower.get(level);
        this.timeStartGame = System.currentTimeMillis();
        List<EnemyWave> enemyWaves = resTower.getAEnemy();
        maxWaveInstance = enemyWaves.size() - 1;
        for (int i = 0; i < enemyWaves.size(); i++) {
            maxEnemy += enemyWaves.get(i).getNumber();
            // wave đầu thì instance luôn
            if (i == 0) {
                addCoroutine(new Coroutine(timeDelayInstanceEnemy, () -> {
                    if (aPlayer != null && (aPlayer.get(0) == null || !aPlayer.get(0).isAlive())) return;
                    EnemyWave wave = enemyWaves.get(0);
                    ResEnemyEntity enemy = ResEnemy.getEnemy(wave.getEnemyId());
                    for (int j = 0; j < wave.getNumber(); j++) {
                        Pos pos = getPosInit(wave, j);
                        Enemy bot = new Enemy(enemy, pos, Pos.RandomDirection(), 2, this);
                        aEnemy.add(bot);
                        aProtoAdd.add(bot.toProtoAdd());
                    }
                }));
            } else {
                long time = (Calendar.getInstance().getTimeInMillis() + (long) resTower.getTimeNextWave() * i * DateTime.SECOND2_MILLI_SECOND);
                timeInstanceWave.add(time);
            }
        }
    }

    private Pos getPosInit(EnemyWave wave, int index) {
        int rand = 0;
        switch (wave.getPosType()) {
            case CORNERS_4 -> rand = index % 4;
            case CORNERS_6 -> rand = index % 6;
            case CORNERS_8 -> rand = index % 8;
            case CORNERS_10 -> rand = index % 10;
            case CORNERS_12 -> rand = index % 12;

        }
        return Pos.randomPos(mapEnemy.get(rand).clone(), offsetEnemy, offsetEnemy);
    }

    protected void startInit() {
        super.startInit();
        roomState = RoomState.ACTIVE;
    }


    @Override
    public void characterDie(Character character) {
        if (character.isEnemy()) {
            numberMonsterDie++;
            numKillWave++;
            aPlayer.get(0).protoStatus(StateType.UPDATE_NUMBER_KILL_TOWER, List.of((long) numberMonsterDie, (long) maxEnemy));
            if (indexWave < maxWaveInstance && timeInstanceWave.size() > 0 && resTower.getAEnemy().get(indexWave).getNumber() == numKillWave) {
                numKillWave = 0;
                timeInstanceWave.set(indexWave, Calendar.getInstance().getTimeInMillis());
            }
        }
        if (character.isPlayer()) {
            int per = numberMonsterDie * 100 / aEnemy.size();
            addCoroutine(new Coroutine(1f, () -> {
                endGame(false, per);
            }));
        }
    }

    @Override
    public void joinMap(AHandler handler) {
        super.joinMap(handler);
    }

    public void endGame(boolean isWin, int per) {
        setEndGameState();
        Player player = aPlayer.get(0).getPlayer();
        List<Long> bonus = isWin ? Bonus.receiveListItem(player.getMUser(), DetailActionType.PHAN_THUONG_LEO_THAP.getKey(resTower.getId()), resTower.getBonus()) : new ArrayList<>();
        if (isWin) bonus.addAll(CfgEventDrop.bonusDrop(CfgEventDrop.config.getRateDropTower(), 1));
        player.toPbEndGame(isWin, per, bonus, getTimeAttack(), PopupType.POPUP_END_TOWER, List.of(numberMonsterDie, maxEnemy));
        if (isWin) Services.userDAO.getUserTower(player.getMUser()).updateWin();
    }

    @Override
    public void EffectUpdate() {
        super.EffectUpdate();
        if (roomState == RoomState.ACTIVE) {
            if (numberMonsterDie > 0 && numberMonsterDie == maxEnemy) {
                endGame(true, 100);
            }
            // chưa max wave và đến h instance thì active wave
            //todo check kill hết thì instance lượt mới luôn
            if (indexWave < maxWaveInstance && timeInstanceWave.size() > 0 && System.currentTimeMillis() > timeInstanceWave.get(indexWave)) {
                if (aPlayer != null && (aPlayer.get(0) == null || !aPlayer.get(0).isAlive())) return;
                EnemyWave wave = resTower.getAEnemy().get(indexWave + 1);
                ResEnemyEntity enemy = ResEnemy.getEnemy(wave.getEnemyId());
                for (int j = 0; j < wave.getNumber(); j++) {
                    Pos pos = getPosInit(wave, j);
                    Enemy bot = new Enemy(enemy, pos, Pos.RandomDirection(), 2, this);
                    aEnemy.add(bot);
                    aProtoAdd.add(bot.toProtoAdd());
                }
                indexWave++;
            }
        }
    }

    @Override
    public synchronized void FixedUpdate() {
        super.FixedUpdate();
        if (getRoomState() != RoomState.ACTIVE) return;
        //System.out.println("aPlayer.get(0).getPos() = " + aPlayer.get(0).getPos());
    }
}
