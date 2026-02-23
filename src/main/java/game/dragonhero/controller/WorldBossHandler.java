package game.dragonhero.controller;

import game.battle.model.ArenaHero;
import game.battle.model.BotPlayer;
import game.battle.model.Character;
import game.battle.model.Player;
import game.battle.object.HeroBattle;
import game.battle.object.Pos;
import game.battle.object.WeaponBattle;
import game.battle.type.CharacterType;
import game.config.CfgBattle;
import game.config.CfgServer;
import game.config.CfgWorldBoss;
import game.config.lang.Lang;
import game.config.aEnum.*;
import game.dragonhero.mapping.UserEntity;
import game.dragonhero.mapping.UserHeroEntity;
import game.dragonhero.mapping.UserPartyEntity;
import game.dragonhero.mapping.UserWeaponEntity;
import game.dragonhero.mapping.main.BaseMap;
import game.dragonhero.server.IAction;
import game.dragonhero.service.resource.ResMap;
import game.dragonhero.service.resource.ResParty;
import game.dragonhero.service.user.Bonus;
import game.dragonhero.table.*;
import game.monitor.Online;
import game.object.MyUser;
import game.object.TaskMonitor;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.ChUtil;
import ozudo.base.helper.ListUtil;
import ozudo.base.helper.StringHelper;
import ozudo.base.helper.Util;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class WorldBossHandler extends AHandler {
    @Override
    public AHandler newInstance() {
        return new WorldBossHandler();
    }

    static WorldBossHandler instance;

    public static WorldBossHandler getInstance() {
        if (instance == null) {
            instance = new WorldBossHandler();
        }
        return instance;
    }

    CfgWorldBoss.DataConfig cfgWorldBoss;
    private UserPartyEntity uParty;

    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(WORLD_BOSS_STATUS, WORLD_BOSS_INFO, WORLD_BOSS_JOIN, WORLD_BOSS_LEAVE,
                WORLD_BOSS_INVITE, WORLD_BOSS_ATTACK, WORLD_BOSS_SOLO_ATTACK,WORLD_BOSS_SOLO_INFO);
        actions.forEach(action -> mHandler.put(action, this));
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        cfgWorldBoss = CfgWorldBoss.config;
        try {
            switch (actionId) {
                case IAction.WORLD_BOSS_STATUS -> status(); // done
                case IAction.WORLD_BOSS_SOLO_ATTACK -> soloAttack();
                case IAction.WORLD_BOSS_SOLO_INFO -> soloInfo();
                default -> {
                    uParty = user.getParty();
                    if (uParty == null) {
                        addErrResponse(getLang(Lang.err_need_party_to_boss));
                        return;
                    }
                    switch (actionId) {
                        case IAction.WORLD_BOSS_JOIN -> join(); // doninvite
                        case IAction.WORLD_BOSS_INVITE -> invite();
                        case IAction.WORLD_BOSS_ATTACK -> attack();
                        case IAction.WORLD_BOSS_LEAVE -> leave();

                    }
                }

            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    private void status() {
        long timeToOpen = CfgWorldBoss.getBossTime();
        addResponse(getCommonVector(timeToOpen));
    }

    // tham gia đánh boss
    private void join() {
        uParty.addChannelAttackBoss(mUser);
        addResponseSuccess();
        sendInfoToAllUser(uParty);
    }

    public static void sendInfoToAllUser(UserPartyEntity uParty) {
        Pbmethod.PbListUser.Builder pb = Pbmethod.PbListUser.newBuilder();
        List<MyUser> lstUser = uParty.getChannelsAttackBoss().stream()
                .filter(u -> u.getChannel() != null && u.getChannel().isActive())
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(u -> u.getUser().getId(), u -> u, (a, b) -> a),
                        m -> new ArrayList<>(m.values())
                ));

        for (int i = 0; i < lstUser.size(); i++) {
            pb.addAUser(lstUser.get(i).getUser().toProto());
        }
        for (int i = 0; i < lstUser.size(); i++) {
            Channel channel = lstUser.get(i).getChannel();
            Util.sendProtoData(channel, pb.build(), IAction.WORLD_BOSS_INFO);
        }
    }

    private void invite() {
        List<MyUser> lstUser = uParty.getChannels();
        boolean send = false;
        if (lstUser != null && !lstUser.isEmpty()) {
            for (int i = 0; i < lstUser.size(); i++) {
                if (lstUser.get(i).getUser().getId() != user.getId()) {
                    Util.sendProtoData(lstUser.get(i).getChannel(), getCommonVector(getLang(lstUser.get(i), Lang.msg_x_invite_you_boss, user.getName())), IAction.WORLD_BOSS_NEW_INVITE);
                    send = true;
                }
            }
        }
        if (send)
            addResponse(getCommonVector(getLang(Lang.msg_invite_sent_to_party)));
        else addResponse(getCommonVector(getLang(Lang.msg_no_member_online)));
    }


    private void attack() {
        if (!uParty.isLeader(user.getId())) {
            addErrResponse(getLang(Lang.err_only_leader_can_start));
            return;
        }

        long timeToOpen = CfgWorldBoss.getBossTime();
        if (timeToOpen < 0) {
            addErrResponse(getLang(Lang.err_not_time_yet));
            return;
        }
        List<MyUser> lstMyUser = uParty.getChannelAttackBoss();
        if (lstMyUser.size() < 2) {
            addErrResponse(getLang(Lang.err_need_at_least_2_members));
            return;
        }

        RoomType bossType = RoomType.WORLD_BOSS;

        String keyRoom = CfgBattle.getKeyRoom(mUser, bossType.value, 0, uParty.getUserId());

        List<Character> players = new ArrayList<>();

        List<Pos> lstPos = List.of(new Pos(0, -3), new Pos(-2, -3), new Pos(2, -3));
        for (int i = 0; i < lstMyUser.size(); i++) {
            Channel chanel = lstMyUser.get(i).getChannel();
            BaseRoom curRoom = (BaseRoom) ChUtil.get(chanel, ChUtil.KEY_ROOM);
            // xóa khỏi room cũ
            Player player = lstMyUser.get(i).getPlayer();
            if (curRoom != null && curRoom.hasPlayer(player.getId())) {
                curRoom.removePlayer(player.getId());
            }
            player.clearDataForChangeRoom(lstPos.get(i));
            players.add(player);
        }


        // check có room hay chưa, có rồi thì join
        BaseMap baseMap = ResMap.getBossMap(bossType);
        BaseRoom room = new BossPartyRoom(baseMap, players, keyRoom,false);
        TaskMonitor.getInstance().addRoom(room);

        for (int i = 0; i < lstMyUser.size(); i++) {
            Channel chanel = lstMyUser.get(i).getChannel();
            ChUtil.set(chanel, ChUtil.KEY_ROOM, room);
            Util.sendProtoData(chanel, CfgBattle.genInitMap(bossType.value, 0, uParty.getUserId(), baseMap.getMapData().getPlayerCollider(), true, PopupType.NULL), IAction.WORLD_BOSS_ATTACK);
        }
    }

    private void soloAttack() {
        List<Long> inputHero = getInputALong();
        if(inputHero.size()<2){
            addErrResponse(getLang(Lang.err_need_3_heroes));
            return;
        }
        if (inputHero.size() !=2){
            addErrParam();
            return;
        }
        long timeToOpen = CfgWorldBoss.getBossTime();
        if (timeToOpen > 0) { // >0 là đang đánh party
            addErrParam();
            return;
        }
        List<Long> fee = Bonus.viewItem(ItemKey.BOSS_SOLO_TICKER,-1);
        String err = Bonus.checkMoney(mUser,fee);
        if(err!=null){
            addErrResponse(err);
            return;
        }

        RoomType bossType = RoomType.WORLD_BOSS;
        String keyRoom = CfgBattle.getKeyRoom(mUser, bossType.value, mUser.getUser().getId(),mUser.getUser().getId() );
        BaseRoom curRoom = (BaseRoom) ChUtil.get(mUser.getChannel(), ChUtil.KEY_ROOM);
        if (curRoom != null && curRoom.hasPlayer(mUser.getPlayer().getId())) {
            curRoom.removePlayer(mUser.getPlayer().getId());
        }
        List<Character> players = new ArrayList<>();
        List<Character> bots = new ArrayList<>();
        mUser.getPlayer().clearDataForChangeRoom(new Pos(0f,-3f));
        players.add(mUser.getPlayer());
        BaseMap baseMap = ResMap.getBossMap(bossType);
        for (int i = 0; i < inputHero.size(); i++) {
            UserHeroEntity userHero = mUser.getResources().getHero(inputHero.get(i).intValue());
            if (userHero == null) continue;
            int fakeId =mUser.getUser().getId()+i+1;
            Pos posInit = i==0?new Pos(-3f,-3f):new Pos(3f,-3f);
            BotPlayer bot =new BotPlayer(mUser, fakeId,1,userHero,posInit,10);
            players.add( bot);
            bots.add(bot);
        }

        BaseRoom room = new BossPartyRoom(baseMap, players, keyRoom,true);
        for (int i = 0; i < bots.size(); i++) {
            ((BotPlayer) bots.get(i)).setJoinMap(room);
        }
        TaskMonitor.getInstance().addRoom(room);
        addBonusToast(Bonus.receiveListItem(mUser,DetailActionType.ATTACK_BOSS_SOLO.getKey(),fee));
        ChUtil.set(mUser.getChannel(), ChUtil.KEY_ROOM, room);
        addResponse(CfgBattle.genInitMap(bossType.value, mUser.getUser().getId(), mUser.getUser().getId(), baseMap.getMapData().getPlayerCollider(), true, PopupType.NULL));
    }

    private void soloInfo(){
        List<UserHeroEntity> uHero = mUser.getResources().getHeroes();
        uHero = uHero.stream()
                .filter(hero -> hero.getHeroId() != user.getHeroMain())
                .sorted(Comparator.comparingLong(UserHeroEntity::getPower).reversed())
                .toList();

        Pbmethod.PbListHero.Builder heroBuilder = Pbmethod.PbListHero.newBuilder();
        int max = Math.min(uHero.size(), 2);
        for (int i = 0; i < max; i++) {
            heroBuilder.addAHero(uHero.get(i).toProto());
        }
        addResponse(heroBuilder.build());
    }

    private void leave() {
        uParty.leaveChannelAttackBoss(mUser);
        addResponseSuccess();
        sendInfoToAllUser(uParty);
    }
}
