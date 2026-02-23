package game.dragonhero.controller;

import game.battle.object.*;
import game.cache.CacheStoreBeans;
import game.config.CfgArena;
import game.config.CfgBattle;
import game.config.CfgFeature;
import game.config.aEnum.PetType;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.dragonhero.BattleConfig;
import game.dragonhero.mapping.*;
import game.dragonhero.mapping.main.BaseMap;
import game.dragonhero.server.IAction;
import game.dragonhero.service.Services;
import game.dragonhero.service.resource.ResMap;
import game.dragonhero.service.user.Bonus;
import game.dragonhero.table.ArenaRoom;
import game.dragonhero.table.BaseRoom;
import game.monitor.Online;
import game.object.DataDaily;
import io.netty.channel.Channel;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.*;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import javax.persistence.EntityManager;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ArenaHandler extends AHandler implements Serializable {

    @Override
    public AHandler newInstance() {
        return new ArenaHandler();
    }

    static ArenaHandler instance;
    UserArenaEntity uArena;

    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(ARENA_VIEW_OPP, ARENA_STATUS, ARENA_ATTACK, ARENA_HISTORY, ARENA_REFRESH, ARENA_BUY_TICKET, ARENA_SET_DEF, ARENA_START_BATTLE, ARENA_QUIT);
        actions.forEach(action -> mHandler.put(action, this));

    }

    public static ArenaHandler getInstance() {
        if (instance == null) {
            instance = new ArenaHandler();
        }
        return instance;
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        checkTimeMonitor("s");
        if (!CfgFeature.isOpenFeature(FeatureType.ARENA, mUser, this)) {
            return;
        }
        uArena = Services.userDAO.getUserArena(mUser);
        try {
            switch (actionId) {
                case ARENA_STATUS -> status();
                case ARENA_ATTACK -> attack();
                case ARENA_HISTORY -> history();
                case ARENA_REFRESH -> refresh();
                case ARENA_BUY_TICKET -> buyTicket();
                case ARENA_SET_DEF -> setDefenseTeam();
                case ARENA_VIEW_OPP -> viewInfo();
                case ARENA_QUIT -> quit();
                case ARENA_START_BATTLE -> startBattle();

            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }


//    private void findBattle() {
//        int status = getInputInt();
//        List<Integer> userServer = ArenaJob.userArena.get(user.getServer());
//        if (status == 1) {
//            if (userServer == null) {
//                List<Integer> users = new ArrayList<>();
//                users.add(user.getId());
//                ArenaJob.userArena.put(user.getServer(), users);
//            } else {
//                boolean add = true;
//                for (int i = 0; i < userServer.size(); i++) {
//                    if (userServer.get(i) == user.getId()) add = false;
//                }
//                if (add) {
//                    ArenaJob.userArena.get(user.getServer()).add(user.getId());
//                }
//            }
//        } else {
//            if (userServer != null) {
//                for (int i = 0; i < userServer.size(); i++) {
//                    if (userServer.get(i) != null && user.getId() == userServer.get(i)) {
//                        userServer.remove(userServer.get(i));
//                    }
//                }
//            }
//        }
//        addResponse(getCommonVector(status));
//    }
//
//    public static boolean first1(int id1, int id2) {
//        return id1 < id2;
//    }
//
//
//    private void arena2FindBattle() {
//        int status = getInputInt();
//        List<Integer> userServer = ArenaJob.userArena2.get(user.getServer());
//        if (status == 1) {
//            if (userServer == null) {
//                List<Integer> users = new ArrayList<>();
//                users.add(user.getId());
//                ArenaJob.userArena2.put(user.getServer(), users);
//            } else {
//                boolean add = true;
//                for (int i = 0; i < userServer.size(); i++) {
//                    if (userServer.get(i) == user.getId()) add = false;
//                }
//                if (add) {
//                    ArenaJob.userArena2.get(user.getServer()).add(user.getId());
//                }
//            }
//        } else {
//            if (userServer != null) {
//                for (int i = 0; i < userServer.size(); i++) {
//                    if (userServer.get(i) != null && user.getId() == userServer.get(i)) {
//                        userServer.remove(userServer.get(i));
//                    }
//                }
//            }
//        }
//        addResponse(getCommonVector(status));
//    }

    private void status() {
        addResponse(IAction.ARENA_STATUS, uArena.toProto(mUser));
    }


    //todo arena mode 2 attack
    //void attack() {
    //    int attackId = getInputInt();
    //    // add new room
    //    RoomType arena = RoomType.ARENA;
    //    UserEntity botUser = Online.getDbUser(attackId);
    //    if (botUser == null) {
    //        addErrResponse(getLang(Lang.err_user_not_exist));
    //        return;
    //    }
    //    // sắp xếp cho key id thằng user nhỏ hơn lên trước
    //    boolean first1 = first1(user.getId(), attackId);
    //    String keyRoom = CfgBattle.getKeyRoom(null, arena.value, first1 ? user.getId() : attackId, first1 ? attackId : user.getId());
    //    // user 1
    //    BaseRoom curRoom = (BaseRoom) ChUtil.get(channel, ChUtil.KEY_ROOM);
    //    if (curRoom != null && (curRoom.getKeyRoom().equals(keyRoom) || !curRoom.allowChangeChanel())) {
    //        addErrResponse(getLang(Lang.err_unauthorized));
    //        return;
    //    }
    //    // xóa khỏi room cũ
    //    CfgBattle.removeUserToRoom(channel, keyRoom, user.getId());
    //    BaseMap baseMap = ResMap.getMap(arena.value, 0);
    //    List<Character> players = new ArrayList<>();
    //    Player p1 = mUser.getPlayer();
    //    p1.clearDataForChangeRoom(new Pos(-10.5f, 0));
    //    p1.setPanelMap(new PanelMap(baseMap.getMapData()));
    //    players.add(p1);
    //    // create bot user
    //    BotPlayer botPlayer = new BotPlayer(botUser, 2);
    //    botPlayer.setDirection(Pos.left());
    //    botPlayer.setPanelMap(ArenaRoom.panel2.Clone());
    //    //room
    //    ArenaRoom room = new ArenaRoom(baseMap, players, botPlayer, keyRoom);
    //    botPlayer.setRoom(room);
    //    TaskMonitor.getInstance().addRoom(room);
    //    ChUtil.set(channel, ChUtil.KEY_ROOM, room);
    //    Util.sendProtoData(channel, CommonProto.getCommonVector(arena.value, baseMap.getMapData().getPlayerCollider(), 1), IAction.ARENA_INIT_ATTACK);
    //}


    private void attack() {
        //check fee
        List<Long> fee = Bonus.viewItem(ItemKey.ARENA_TICKET, -1);
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        List<Long> inputs = getInputALong(); //oopId, [heroId - weaponx3] x3 , petId,monsterId
        int oppId = inputs.get(inputs.size() - 1).intValue();
        if (inputs.size() != BattleConfig.maxSizeInputArena + 1) {
            addErrParam();
            return;
        }
        BattleTeam team1 = new BattleTeam();
        HeroBattle[] heroes = new HeroBattle[5];
        //pet
        UserPetEntity pet = mUser.getResources().getPet(PetType.ANIMAL, inputs.get(BattleConfig.petIndex).intValue());
        if (pet != null) {
            if (pet.getHp() <= 0) {
                addErrResponse(getLang(Lang.err_pet_can_care));
                return;
            }
            heroes[CfgArena.SLOT_T1_PET - 1] = pet.toHeroBattle(1, CfgArena.SLOT_T1_PET);
        }
        //monster
        UserPetEntity monster = mUser.getResources().getPet(PetType.MONSTER, inputs.get(BattleConfig.monsterIndex).intValue());
        if (monster != null) {
            if (monster.getHp() <= 0) {
                addErrResponse(getLang(Lang.err_monster_can_care));
                return;
            }
            heroes[CfgArena.SLOT_T1_MONSTER - 1] = monster.toHeroBattle(1, CfgArena.SLOT_T1_MONSTER);
        }
//        if (uArena.getActiveArena() == 0 || !uArena.getHasSetDefTeam()) {
//            addErrResponse(getLang(Lang.err_arena_has_defense));
//            return;
//        }
        // hero
        for (int i = 0; i < BattleConfig.sizeHeroArena; i += 4) {
            int idIndex = inputs.get(i).intValue();
            if (idIndex == 0) continue;
            UserHeroEntity uHero = mUser.getResources().getHero(idIndex);
            if (uHero == null) continue;
            WeaponBattle[] shu = new WeaponBattle[3];
            for (int j = 1; j <= 3; j++) {
                idIndex = inputs.get(i + j).intValue();
                UserWeaponEntity weapon = mUser.getResources().getWeapon(idIndex);
                if (weapon == null) {
                    addErrResponse(getLang(Lang.err_arena_has_select_weapon));
                    return;
                }
                int slot = j - 1;
                shu[slot] = weapon.toWeaponBattle(user.getCachePoint(), slot);
            }
            int slot = i / 4;
            heroes[slot] = new HeroBattle(1, slot + 1, uHero.getHeroId(), uHero.getPoint(mUser).cloneInstance(), slot, shu, monster);
        }
        team1.setBattleHeroes(heroes);
        // add new room
        RoomType arena = RoomType.ARENA;
        UserArenaEntity arenaOpp = Online.getDbUserArena(oppId);
        if (arenaOpp == null) {
            addErrResponse(getLang(Lang.err_user_not_exist));
            return;
        }
        String keyRoom = CfgBattle.getKeyRoom(mUser, arena.value, user.getId());
        // xóa khỏi room cũ
        CfgBattle.removeUserToRoom(channel, keyRoom, user.getId());
        mUser.setCachePos();
        BaseMap baseMap = ResMap.getMap(arena.value, 0);
        //todo init team
        BattleTeam team2 = arenaOpp.getDefTeam();
        if (team2 == null) {
            addErrResponse(getLang(Lang.err_system_down));
            return;
        }
        UserEntity uOpp = Online.getDbUser(oppId);
        BaseRoom room = new ArenaRoom(mUser, keyRoom, team1, team2, oppId, uArena.getArenaPoint(), arenaOpp.getArenaPoint(), uArena, arenaOpp, uOpp);
        // pb init map
        Pbmethod.PbBattleArena.Builder pb = Pbmethod.PbBattleArena.newBuilder();
        pb.addAllMapInfo(CfgBattle.genInitMapInt(arena.value, 0, 0, baseMap.getMapData().getPlayerCollider(), true, PopupType.NULL));
        pb.setMyInfo(user.toProtoArenaInfo(uArena.getArenaPoint()));

        if (uOpp != null) pb.setOppInfo(uOpp.toProtoArenaInfo(arenaOpp.getArenaPoint()));
        pb.setTime(CfgArena.maxTimeAttack);
        // my team
        Pbmethod.PbBattleListArenaHero oTeam = arenaOpp.toProtoDefTeam();
        if (team1 == null || oTeam == null || uOpp == null) {
            addErrParam();
            return;
        }
        pb.setMyTeam(team1.toProto());
        pb.setOppTeam(oTeam);
        List<Long> bonus = Bonus.receiveListItem(mUser, DetailActionType.ATTACK_ARENA.getKey(), fee);
        if (bonus.isEmpty()) {
            addErrSystem();
            return;
        }
        addBonusPrivate(bonus);
        addResponse(pb.build());
        ChUtil.set(channel, ChUtil.KEY_ROOM, room);
        mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.ATTACK_ARENA, 1);
    }

    private void startBattle() {
        BaseRoom curRoom = (BaseRoom) ChUtil.get(mUser.getChannel(), ChUtil.KEY_ROOM);
        if (curRoom == null || curRoom.getRoomType() != RoomType.ARENA.value) {
            addErrParam();
            return;
        }
        ArenaRoom arenaRoom = (ArenaRoom) curRoom;
        arenaRoom.startGame();
        addResponseSuccess();
    }

    private void history() {
        if (uArena.getActiveArena() == 0) {
            addErrResponse(getLang(Lang.err_arena_has_defense));
            return;
        }
        List<UserArenaHistoryEntity> history = dbGetArenaHistory();
        Pbmethod.PbListHistory.Builder pb = Pbmethod.PbListHistory.newBuilder();
        for (int i = 0; i < history.size(); i++) {
            pb.addHistory(history.get(i).toProto(user.getId()));
        }
        addResponse(pb.build());
    }


    private void refresh() {
        if (uArena.getActiveArena() == 0) {
            addErrResponse(getLang(Lang.err_arena_has_defense));
            return;
        }
        List<Integer> ops = uArena.findOpponents(user);
        if (!ops.isEmpty() && uArena.update(Arrays.asList("active_arena", 1))) {
            uArena.setOpps(ops);
            uArena.setActiveArena(1);
        }
        protocol.Pbmethod.PbArena.Builder pb = protocol.Pbmethod.PbArena.newBuilder();
        for (int i = 0; i < ops.size(); i++) {
            UserEntity uOp = Online.getDbUser(ops.get(i));
            UserArenaEntity arena = Online.getDbUserArena(uArena.getOpps().get(i));
            if (uOp != null && arena != null) {
                pb.addOpponents(uOp.toProto(arena.getArenaPoint()));
            }
        }
        addResponse(pb.build());
    }

    private void buyTicket() {
        if (uArena.getActiveArena() == 0) {
            addErrResponse(getLang(Lang.err_arena_has_defense));
            return;
        }
        int number = getInputInt();
        if (number < 0) {
            addErrParam();
            return;
        }
        DataDaily daily = mUser.getDataDaily();
        int numBuy = daily.getValue(DataDaily.NUMBER_BUY_TICKET_ARENA);
        if (numBuy + number > CfgArena.config.maxBuyTicket) {
            addErrResponse(getLang(Lang.err_max_buy));
            return;
        }
        List<Long> fee = CfgArena.getFeeBuyTicket(number);
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        fee.addAll(Bonus.viewItem(ItemKey.ARENA_TICKET, number));
        daily.setValue(DataDaily.NUMBER_BUY_TICKET_ARENA, numBuy + number);
        if (daily.update())
            addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.BUY_ARENA_TICKET.getKey(number), fee)));
        else addErrSystem();
    }

    private void setDefenseTeam() {
        List<Long> inputs = getInputALong();
        if (inputs.size() != BattleConfig.maxSizeInputArena) {
            addErrParam();
            return;
        }
        boolean hasData = false;
        int team = 2; // team def
        BattleTeam battleTeam = new BattleTeam();
        // pet
        UserPetEntity pet = mUser.getResources().getMPetAnimal().get(inputs.get(BattleConfig.petIndex).intValue());
        if (pet != null) {
            hasData = true;
            battleTeam.getBattleHeroes()[3] = pet.toHeroBattle(team, CfgArena.SLOT_T2_PET);
        }
        // monster
        UserPetEntity monster = mUser.getResources().getMPetMonster().get(inputs.get(BattleConfig.monsterIndex).intValue());
        if (monster != null) {
            hasData = true;
            battleTeam.getBattleHeroes()[4] = monster.toHeroBattle(team, CfgArena.SLOT_T2_MONSTER);
        }
        // hero
        int step = 4;//heroId  - [weapon]x3
        for (int i = 0; i < BattleConfig.sizeHeroArena; i += step) { // hero ID - weponId x3
            UserHeroEntity userHero = mUser.getResources().getHero(inputs.get(i).intValue());
            if (userHero == null) continue;
            hasData = true;
            WeaponBattle[] weaponBattles = new WeaponBattle[3];
            for (int j = 0; j < 3; j++) {
                UserWeaponEntity u = mUser.getResources().getWeapon(Math.toIntExact(inputs.get(i + j + 1)));
                if(u==null) {
                    addErrResponse(getLang(Lang.err_save_fail_need_hero_weapon));
                    return;
                }
                weaponBattles[j] = u.toWeaponBattle(user.getCachePoint(), j);
            }
            int slot = i / step;
            battleTeam.getBattleHeroes()[slot] = userHero.toHeroBattle(team, 6 + slot, slot, weaponBattles, monster);
        }

        // save DB
        if (!hasData) {
            addErrResponse(getLang(Lang.def_team_empty));
            return;
        }
        String data = StringHelper.toDBString(battleTeam);
        if (uArena.update(List.of("defense_team", data, "active_arena", 1))) {
            uArena.setDefenseTeam(data);
            uArena.setActiveArena(1);
            uArena.setDefTeam(battleTeam);
            Online.cacheUserArena(uArena);
            addResponseSuccess();
        } else addResponseError();

    }


    private void viewInfo() {
        int oppId = getInputInt();
        UserArenaEntity uOpp = Online.getDbUserArena(oppId);
        if (uOpp == null) {
            addErrResponse(getLang(Lang.err_user_not_exist));
            return;
        }
        Pbmethod.PbArenaTeamInfo.Builder pb = Pbmethod.PbArenaTeamInfo.newBuilder();
        pb.setTeam(2);
        BattleTeam battleTeam = uOpp.getDefTeam();
        if (battleTeam == null) {
            addErrResponse(getLang(Lang.err_user_not_exist));
            return;
        }
        // heroes
        for (int i = 0; i < 3; i++) {
            if (battleTeam.getBattleHeroes()[i] != null) pb.addHeroes(battleTeam.getBattleHeroes()[i].toProto());
            else pb.addHeroes(defaultHero());
        }
        //pet
        for (int i = 3; i < 5; i++) {
            if (battleTeam.getBattleHeroes()[i] != null) {
                pb.addPets(battleTeam.getBattleHeroes()[i].toPetProto());
            } else pb.addPets(Pbmethod.PbArenaPetInfo.newBuilder().setAvatar(0));
        }
        addResponse(pb.build());
    }

    private void quit() {
        BaseRoom curRoom = (BaseRoom) ChUtil.get(mUser.getChannel(), ChUtil.KEY_ROOM);
        if (curRoom == null || curRoom.getRoomType() != RoomType.ARENA.value) {
            addErrParam();
            return;
        }
        ArenaRoom arenaRoom = (ArenaRoom) curRoom;
        arenaRoom.endBattle();
        addResponseSuccess();
    }

    private Pbmethod.PbArenaHeroInfo defaultHero() {
        Pbmethod.PbArenaHeroInfo.Builder pb = Pbmethod.PbArenaHeroInfo.newBuilder();
        pb.setAvatar(0);
        pb.addWeapons(Pbmethod.PbArenaWeapon.newBuilder().setId(0));
        pb.addWeapons(Pbmethod.PbArenaWeapon.newBuilder().setId(0));
        pb.addWeapons(Pbmethod.PbArenaWeapon.newBuilder().setId(0));
        return pb.build();
    }

    private List<UserArenaHistoryEntity> dbGetArenaHistory() {
        Integer number = CacheStoreBeans.cache1Min.get(mUser.getUser().getId() + "_arena_history");
        List<UserArenaHistoryEntity> history = (List<UserArenaHistoryEntity>) mUser.getCache().get("user_arena_history");
        if (number == null || history == null) {
            CacheStoreBeans.cache1Min.add(mUser.getUser().getId() + "_arena_history", 1);
            history = dbGetUserArenaHistory(mUser.getUser().getId());
            if (history != null) {
                mUser.getCache().set("user_arena_history", history);
            }
        }
        return history;
    }

    List<UserArenaHistoryEntity> dbGetUserArenaHistory(int userId) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            return session.createNativeQuery("select * from user_arena_history where atk_id=" + userId + " or def_id=" + userId + " order by time desc limit 20", UserArenaHistoryEntity.class).getResultList();
        } catch (Exception ex) {
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return null;
    }

}
