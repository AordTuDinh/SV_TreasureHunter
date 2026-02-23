//package game.dragonhero.table;
//
//import game.battle.model.BotPlayer;
//import game.battle.model.Character;
//import game.battle.model.Enemy;
//import game.battle.model.Player;
//import game.battle.object.*;
//import game.battle.type.CharacterType;
//import game.battle.type.RoomState;
//import game.battle.type.StateType;
//import game.config.aEnum.DetailActionType;
//import game.config.aEnum.FactionType;
//import game.config.aEnum.PopupType;
//import game.dragonhero.controller.AHandler;
//import game.dragonhero.mapping.main.BaseMap;
//import game.dragonhero.mapping.main.ResEnemyEntity;
//import game.dragonhero.server.Constans;
//import game.dragonhero.server.IAction;
//import game.dragonhero.service.resource.ResEnemy;
//import game.dragonhero.service.user.Bonus;
//import game.object.MyUser;
//import game.protocol.CommonProto;
//import game.protocol.ProtoState;
//import lombok.Getter;
//import ozudo.base.helper.DateTime;
//import ozudo.base.helper.NumberUtil;
//import ozudo.base.helper.Util;
//import protocol.Pbmethod;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class BattleMode2Room extends BaseBattleRoom {
//    @Getter
//    BotPlayer botPlayer;
//    @Getter
//    Player player;
//    @Getter
//    List<Character> aEnemy1 = new ArrayList<>();
//    @Getter
//    List<Character> aEnemy2 = new ArrayList<>();
//
//    private static final float timeDelayReady = 3f;
//    private static final float timeNextWave = 500f;
//    private static final float timeReviveZombie = 1f;
//    private static final int numWave = 20;
//    private static final int posRevive = 6;
//    private static final int maxTimeAttack = 120; //s
//    private static final int TEAM_1 = 1;
//    private static final int TEAM_2 = 2;
//    private static final int enemyInWave = 1;
//    //    private static final int enemyInWave = 5;
//    private int countP1Kill, countP2Kill;
//    private Point mainPoint;
//    private static final List<Pos> posInitEnemyR1 = List.of(new Pos(-15f, 7.5f), new Pos(-15, 0), new Pos(-15, -8.5f), new Pos(-2f, 7.5f), new Pos(-2, -8.5f));
//    private static final List<Pos> posInitEnemyR2 = List.of(new Pos(2f, 7.5f), new Pos(15f, 7.5f), new Pos(15f, 0), new Pos(15f, -8.5f), new Pos(2, -8.5f));
//    public PanelMap panelMain;// = new PanelMap(new Pos(-16, -9f), new Pos(-4.75f, 9f));
//    public static final PanelMap panel2 = new PanelMap(new Pos(4.75f, -9f), new Pos(16, 9f));
//    private static final List<Integer> enemyFaction = List.of(1000, 1001, 1002, 1003, 1004);
//    private static final List<Long> bonusWin = Bonus.viewGem(10);
//    private static final List<Long> bonusLost = Bonus.viewGold(100);
//    private static final Pos posInitP1 = new Pos(-10.5f, 0f);
//    private static final Pos posInitP2 = new Pos(10.5f, 0f);
//    // SERVICE
//    private static final int SET_MAX_TIME = 1;
//    private static final int CHANGE_FACTION = 2;
//    private static final int CHANGE_POINT1 = 3;
//    private static final int CHANGE_POINT2 = 4;
//
//
//    public BattleMode2Room(BaseMap mapInfo, List<Character> aPlayer, BotPlayer botPlayer, String keyRoom) {
//        super(mapInfo, aPlayer, keyRoom, true);
//        this.player = aPlayer.get(0).getPlayer();
//        player.setTeamId(TEAM_1);
//        player.setPos(posInitP1.clone());
//        panelMain = new PanelMap(mapInfo.getMapData());
//        this.botPlayer = botPlayer;
//        botPlayer.setTeamId(TEAM_2);
//        botPlayer.setPos(posInitP2.clone());
//        // cache main point
//        if (player.getPoint().getPower() >= botPlayer.getPoint().getPower()) {
//            mainPoint = player.getPoint().cloneInstance();
//        } else mainPoint = botPlayer.getPoint().cloneInstance();
//        //genEnemy();
//    }
//
//    @Override
//    protected byte[] genTableState() {
//        int action = IAction.TABLE_STATE;// K dùng nhưng viết ở đây để referent
//        protocol.Pbmethod.PbState.Builder builder = protocol.Pbmethod.PbState.newBuilder();
//        builder.setServerTime(server_time);
//
//        String debug = "";
//        boolean send = false;
//        for (int i = 0; i < aPlayer.size(); i++) {
//            if (aPlayer.get(i) != null && aPlayer.get(i).isAlive() && aPlayer.get(i).isMove()) {
//                builder.addUnitPos(aPlayer.get(i).toProtoPos());
//                send = true;
//            }
//        }
//        if (botPlayer != null && botPlayer.isAlive() && botPlayer.isMove()) {
//            builder.addUnitPos(botPlayer.toProtoPos());
//            send = true;
//        }
//        for (int i = 0; i < aEnemy1.size(); i++) {
//            if (aEnemy1.get(i) != null && aEnemy1.get(i).isAlive() && aEnemy1.get(i).isMove()) {
//                send = true;
//                builder.addUnitPos(aEnemy1.get(i).toProtoPos());
//            }
//        }
//        for (int i = 0; i < aEnemy2.size(); i++) {
//            if (aEnemy2.get(i) != null && aEnemy2.get(i).isAlive() && aEnemy2.get(i).isMove()) {
//                send = true;
//                builder.addUnitPos(aEnemy2.get(i).toProtoPos());
//            }
//        }
//
//        int size = aProtoAdd.size();
//        for (int i = 0; i < size; i++) {
//            builder.addUnitAdd(aProtoAdd.get(0));
//            aProtoAdd.remove(0);
//            send = true;
//        }
//        if (!aProtoUnitState.isEmpty()) {
//            builder.addAUnitUpdate(ProtoState.protoUnitUpdate(Constans.TYPE_UPDATE_CHARACTER, ProtoState.protoListCharacterState(aProtoUnitState)));
//            send = true;
//            aProtoUnitState.clear();
//        }
//        if (!debug.isEmpty()) System.out.println("debug = " + debug);
//        if (send) return ProtoState.convertProtoBuffToState(builder.build());
//        else return null;
//    }
//
//    private Pos getPosZombieByTeam(int team) {
//        return team == TEAM_1 ? new Pos(-2, 0) : new Pos(2, 0);
//    }
//
//    private void sendRoomInfo(int service, int data) {
//        Pbmethod.PbRoomInfo.Builder pb = Pbmethod.PbRoomInfo.newBuilder();
//        pb.setRoomType(roomType.value).setService(service).setCmm(CommonProto.getCommonVector(data));
//        System.out.println("service = " + service + " --- data: " + data);
//        Util.sendProtoData(player.getMUser().getChannel(), pb.build(), IAction.ROOM_INFO);
//    }
//
//    private void genEnemy() {
//        for (int i = 0; i < numWave; i++) {
//            float time = timeDelayReady + timeNextWave * i;
//            int idEnemy = enemyInWave * i * 2; // 2 bên nên x2
//            int curWave = i + 1;
//            addCoroutine(new Coroutine(time, () -> {
//                if (roomState == RoomState.END) return;
//                int faction = NumberUtil.getRandom(1, 5);
//                // send to client
//                sendRoomInfo(CHANGE_FACTION, faction);
//
//                int enemyKey = enemyFaction.get(faction - 1);
//                ResEnemyEntity enemy = ResEnemy.getEnemy(enemyKey);
//                // Room 1
//                for (int j = 0; j < enemyInWave; j++) {
//                    Enemy bot = new Enemy(enemy, posInitEnemyR1.get(j).clone(), Pos.RandomDirection(), TEAM_2, this);
//                    bot.setPanelMap(panelMain);
//                    calPointFaction(bot.getPoint(), mainPoint, curWave, faction);
//                    bot.setTargetAttack(player);
//                    aEnemy.add(bot);
//                    aProtoAdd.add(bot.toProtoAdd());
//                }
//                // Room 2
//                for (int j = 0; j < enemyInWave; j++) {
//                    Enemy bot = new Enemy(enemy, posInitEnemyR2.get(j).clone(), Pos.RandomDirection(), TEAM_1, this);
//                    bot.setPanelMap(panel2);
//                    calPointFaction(bot.getPoint(), mainPoint, curWave * 2 / 10f, faction);
//                    bot.setTargetAttack(botPlayer);
//                    aEnemy.add(bot);
//                    aEnemy2.add(bot);
//                    aProtoAdd.add(bot.toProtoAdd());
//                }
//            }));
//        }
//    }
//
//    @Override
//    public synchronized void FixedUpdate() {
//        if (roomState != RoomState.ACTIVE) return;
//        if (player != null) {
//            controller.checkHit(this, List.of(player, botPlayer));
//            controller.process_bullet(this);
//
//            for (int i = 0; i < aBullets.size(); i++) {
//                Bullet b = aBullets.get(i);
//                PanelMap panel = b.getOwner().getTeamId() == TEAM_1 ? panelMain : panel2;
//                if (b.isAlive()) {
//                    b.moveInPanel(panel);
//                } else {
//                    removeBullet(b);
//                }
//            }
//        }
//    }
//
//    @Override
//    public void joinMap(AHandler handler) {
//        pbInit.clearAMonster();
//        pbInit.clearAPlayer();
//        for (int i = 0; i < aPlayer.size(); i++) {
//            pbInit.addAPlayer(aPlayer.get(i).toProtoAdd());
//        }
//        pbInit.addAPlayer(botPlayer.toProtoAdd());
//        for (int i = 0; i < aEnemy.size(); i++) {
//            pbInit.addAMonster(aEnemy.get(i).toProtoAdd());
//        }
//        handler.addResponse(IAction.JOIN_MAP, pbInit.build());
//        this.roomState = RoomState.ACTIVE;
//        sendRoomInfo(SET_MAX_TIME, maxTimeAttack);
//        this.timeStartGame = System.currentTimeMillis();
//        //System.out.println("getBotLeft = " + botPlayer.getPanelMap().getBotLeft());
//        //System.out.println("getTopRight = " + botPlayer.getPanelMap().getTopRight());
//        //System.out.println("botPlayer.getPos() = " + botPlayer.getPos());
//
//    }
//
//    private void calPointFaction(Point selfPoint, Point basePoint, float perOffset, int faction) {
//        // offset chỉ số
//        selfPoint.add(Point.ATTACK, (long) (basePoint.get(Point.ATTACK) * perOffset));
//        selfPoint.add(Point.MAGIC_ATTACK, (long) (basePoint.get(Point.MAGIC_ATTACK) * perOffset));
//        selfPoint.add(Point.HP, (long) (basePoint.get(Point.HP) * perOffset));
//        selfPoint.add(Point.HP_REGEN, (long) (basePoint.get(Point.HP_REGEN) * perOffset));
//        selfPoint.add(Point.DEFENSE, (long) (basePoint.get(Point.DEFENSE) * perOffset));
//        selfPoint.add(Point.MAGIC_RESIST, (long) (basePoint.get(Point.MAGIC_RESIST) * perOffset));
//        selfPoint.add(Point.CRIT, (long) (basePoint.get(Point.CRIT) * perOffset));
//        selfPoint.add(Point.CRIT_DAMAGE, (long) (basePoint.get(Point.CRIT_DAMAGE) * perOffset));
//        selfPoint.add(Point.AGILITY, (long) (basePoint.get(Point.AGILITY) * perOffset));
//        selfPoint.add(Point.CRIT_DAMAGE_REDUCTION, (long) (basePoint.get(Point.CRIT_DAMAGE_REDUCTION) * perOffset));
//        // cộng thêm faction
//        switch (FactionType.get(faction)) {
//            case KIM -> {
//                selfPoint.add(Point.CRIT, 10);
//                selfPoint.addCritDamage(100);
//            }
//            case MOC -> {
//                selfPoint.add(Point.ATTACK_SPEED, 200);
//                selfPoint.add(Point.P_MOVE_SPEED, 50);
//            }
//            case THUY -> {
//                selfPoint.add(Point.P_HP, 50);
//                selfPoint.add(Point.HP_REGEN, 100);
//            }
//            case HOA -> {
//                selfPoint.add(Point.P_ATTACK, 50);
//                selfPoint.add(Point.P_MAGIC_ATTACK, 50);
//            }
//            case THO -> {
//                selfPoint.add(Point.P_DEFENSE, 50);
//                selfPoint.add(Point.P_MAGIC_RESIST, 50);
//            }
//        }
//        selfPoint.setMaxCurHp();
//        selfPoint.calculatorPower(1);
//    }
//
//    @Override
//    public void Update() {
//        super.Update();
//        if (roomState != RoomState.ACTIVE) return;
//        botPlayer.playerAutoProcess();
//
//    }
//
//    @Override
//    public void characterDie(Character character) {
//        super.characterDie(character);
//        // 1 trong 2 thằng chết trước
//        if (character.isPlayer() || character.getType() == CharacterType.BOT_PLAYER) {
//            sendWin(character.getId() == botPlayer.getId());
//            setEndGameState();
//        }
//        if (character.getType() == CharacterType.MONSTER) {
//            if (character.getTeamId() == TEAM_1) {
//                countP1Kill++;
//                aEnemy1.remove(character);
//                sendRoomInfo(CHANGE_POINT1, countP1Kill);
//
//            } else {
//                countP2Kill++;
//                aEnemy2.remove(character);
//                sendRoomInfo(CHANGE_POINT2, countP2Kill);
//            }
//
//            protoOneRoomState(StateType.UPDATE_ARENA_POINT, (long) countP1Kill, (long) countP2Kill);
//
//            addCoroutine(new Coroutine(timeReviveZombie, () -> {
//                Enemy e = character.getEnemy();
//                e.resetData();
//
//                e.setPos(new Pos(-e.getInstancePos().x, e.getInstancePos().y));
//                if (e.getTeamId() == TEAM_1) {
//                    e.setTargetAttack(botPlayer);
//                    e.setPos(new Pos(-posRevive, 0));
//                    e.setTeamId(TEAM_2);
//                    aEnemy2.add(e);
//                } else {
//                    e.setTargetAttack(player);
//                    e.setTeamId(TEAM_1);
//                    e.setPos(new Pos(posRevive, 0));
//                    aEnemy1.add(e);
//                }
//                e.protoStatus(StateType.REVIVE, (long) (e.getPos().x * 1000), (long) (e.getPos().y * 1000));
//            }));
//        }
//        if (countP1Kill >= 100 || countP2Kill >= 100) {
//            sendWin(countP1Kill >= 100);
//            setEndGameState();
//        }
//    }
//
//    private void sendWin(boolean isWin) {
//        MyUser u1 = player.getPlayer().getMUser();
//        player.getPlayer().toPbEndGame(isWin, 0, Bonus.receiveListItem(u1, DetailActionType.ARENA_ATTACK.getKey(), isWin ? bonusWin : bonusLost), getTimeAttack(), PopupType.POPUP_END_ARENA);
//    }
//
//
//    @Override
//    public void EffectUpdate() {
//        super.EffectUpdate();
//        if (roomState != RoomState.ACTIVE) return;
//        checkEndGame();
//    }
//
//    private void checkEndGame() {
//        // check hết thời gian
//        if (DateTime.isAfterTime(this.timeStartGame, maxTimeAttack)) {
//            sendWin(false);
//            roomState = RoomState.END;
//        }
//    }
//}
