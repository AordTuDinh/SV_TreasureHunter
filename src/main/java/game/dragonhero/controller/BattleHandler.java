package game.dragonhero.controller;

import game.battle.model.BossClan;
import game.battle.model.BossGod;
import game.battle.model.Character;
import game.battle.model.Player;
import game.battle.object.Pos;
import game.battle.type.AutoMode;
import game.config.CfgBattle;
import game.config.CfgEventDrop;
import game.config.CfgQuest;
import game.config.CfgServer;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.dragonhero.mapping.ClanEntity;
import game.dragonhero.mapping.UserItemEntity;
import game.dragonhero.mapping.UserSettingsEntity;
import game.dragonhero.mapping.main.*;
import game.dragonhero.server.IAction;
import game.dragonhero.service.resource.ResEnemy;
import game.dragonhero.service.resource.ResMap;
import game.dragonhero.service.resource.ResTeleport;
import game.dragonhero.service.user.Bonus;
import game.dragonhero.table.*;
import game.monitor.ClanManager;
import game.object.BonusConfig;
import game.object.DataQuest;
import game.object.MyUser;
import game.object.TaskMonitor;
import game.protocol.CommonProto;
import io.netty.channel.Channel;
import ozudo.base.helper.*;
import ozudo.base.log.Logs;
import protocol.Pbmethod;
import protocol.Pbmethod.CommonVector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BattleHandler extends AHandler implements Serializable {
    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(SERVER_INFO, INIT_MAP, CAMPAIGN_DATA, INIT_MAP_BY_TYPE_ID, CAMPAIGN_REWARD, CAMPAIGN_SMART, INIT_BACK_HOME, JOIN_MAP, BOSS_GOD_DATA, REVIVE_PLAYER, CHANGE_AUTO_MODE, CHANGE_ITEM_SLOT, CHANGE_CHANEL, SMART_BOSS, CHANGE_AUTO_SLOT, INIT_BOSS);
        actions.forEach(action -> mHandler.put(action, this));
    }

    static BattleHandler instance;

    public static BattleHandler getInstance() {
        if (instance == null) {
            instance = new BattleHandler();
        }
        return instance;
    }

    @Override
    public AHandler newInstance() {
        return new BattleHandler();
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        try {
            switch (actionId) {
                case SERVER_INFO -> serverInfo();
                case INIT_MAP -> {
                    ResTeleportEntity resTeleport = ResTeleport.getTeleport(getInputInt());
                    if (resTeleport == null) {
                        initBackHome(mUser, PopupType.NULL);
                    } else initMap(resTeleport);
                }
                case INIT_MAP_BY_TYPE_ID -> {
                    CommonVector pbUB = CommonProto.parseCommonVector(requestData);
                    int type = (int) pbUB.getALong(0);
                    int id = (int) pbUB.getALong(1);
                    Pos pInit = Pos.zero();
                    if (RoomType.get(type) == RoomType.CAMPAIGN) {
                        ResTeleportEntity resTeleport = ResTeleport.getTeleport(12);
                        pInit = resTeleport.getPlayerPosInit();
                    }
                    initMapByTypeId(RoomType.get(type), id, pInit);
                }
                case INIT_BOSS -> initBoss();
                case CAMPAIGN_REWARD -> campaignReward();
                case CAMPAIGN_DATA -> campaignData();
                case CAMPAIGN_SMART -> campaignSmart();
                case INIT_BACK_HOME -> initBackHome(mUser, PopupType.get(getInputInt()));
                case JOIN_MAP -> joinMap();
                case REVIVE_PLAYER -> revivePlayer();
                case CHANGE_AUTO_MODE -> changeMode();
                case CHANGE_ITEM_SLOT -> changeItemSlot();
                case CHANGE_AUTO_SLOT -> changeAutoSlot();
                case CHANGE_CHANEL -> changeChanel();
                case SMART_BOSS -> smartBoss();
                case BOSS_GOD_DATA -> bossGodData();

            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    void serverInfo() {
        // todo : phần connect vào sv mới sẽ làm sau
        CommonVector pbUB = CommonProto.parseCommonVector(requestData);
        int mode = (int) pbUB.getALong(0);
        int mapId = (int) pbUB.getALong(0);
        int subMapId = (int) pbUB.getALong(1);
        CommonVector.Builder builder = CommonVector.newBuilder();
        List<String> svInfo = CfgBattle.getInfoServer(mode);
        builder.addALong(mode);
        builder.addALong(mapId);
        builder.addALong(subMapId);
        builder.addAString(svInfo.get(0));
        builder.addAString(svInfo.get(1));
        addResponse(builder.build());
    }

    public void initMap(ResTeleportEntity resTeleport) {
        ResTeleportEntity nextPort = resTeleport.getNext();
        if (nextPort == null) {
            initBackHome(mUser, PopupType.NULL);
        } else {
            initMapByTypeId(nextPort.getMap(), nextPort.getMapId(), nextPort.getPlayerPosInit());
            mUser.setCurTeleport(nextPort);
        }
    }

    public void initMapByTypeId(RoomType roomType, int mapId, Pos posInit) {
        BaseMap map = ResMap.getMap(roomType, mapId);
        if (map == null) {
            addErrParam();
            return;
        }
        int chanelId = mUser.getRoomChanelId();
        BaseRoom curRoom = (BaseRoom) ChUtil.get(channel, ChUtil.KEY_ROOM);
        if (roomType == RoomType.CAMPAIGN) {
            if (mapId == 1) chanelId = user.getId();
            else chanelId = NumberUtil.getRandom(1, CfgServer.maxChannelOpen);
        }
        String keyRoom = CfgBattle.getKeyRoom(mUser, roomType.value, mapId, chanelId);

        if (curRoom != null && (curRoom.getKeyRoom().equals(keyRoom) || !curRoom.allowChangeChanel())) {
            if (curRoom.getKeyRoom().equals(keyRoom)) {
                addErrResponse(getLang(Lang.err_in_room_already));
                return;
            }
            if (!curRoom.allowChangeChanel()) {
                addErrResponse(getLang(Lang.err_unauthorized));
                return;
            }
        }
        // xóa khỏi room cũ
        Player player = mUser.getPlayer();
        if (curRoom != null && curRoom.hasPlayer(player.getId())) {
            curRoom.removePlayer(player.getId());
        }
        // tìm room thỏa mãn điều kiện max player room
        BaseRoom room = (BaseRoom) TaskMonitor.getInstance().getRoom(keyRoom);
        while (room != null && room.getAPlayer().size() > roomType.maxPlayer) {
            chanelId = NumberUtil.getRandom(1, CfgServer.maxChannelOpen);
            keyRoom = CfgBattle.getKeyRoom(mUser, roomType.value, mapId, chanelId);
            if (curRoom.getKeyRoom().equals(keyRoom)) continue;
            room = (BaseRoom) TaskMonitor.getInstance().getRoom(keyRoom);
        }
        // check có room hay chưa, có rồi thì join
        player.clearDataForChangeRoom(posInit);
        player.resetData();
        boolean isBattle = false;
        if (room == null) {  // tao room moi
            List<Character> players = new ArrayList<>();
            players.add(player);
            switch (roomType) {
                case CAMPAIGN:
                    isBattle = true;
                    room = mapId > 0 ? new CampaignRoom(map, players, keyRoom) : new DefaultRoom(map, players, keyRoom);
                    break;
                case FARM:
                    // UI Room nên không create room
                    break;
                case CLAN_BOSS:
                    isBattle = true;
                    if (user.getClan() == 0) {
                        addErrResponse(getLang(Lang.err_no_clan));
                        return;
                    }
                    ClanEntity clan = ClanManager.getInstance(user.getClan()).getClan();
                    if (clan == null) {
                        addErrSystem();
                        return;
                    }
                    room = clan.getCurBossRoom();
                    if (room == null) {
                        player.clearDataNoCachePos(new Pos(0, -3));
                        room = new BossClanRoom(map, players, keyRoom, clan) {
                            @Override
                            protected BossGod bossData() {
                                ResBossEntity bossData = ResEnemy.getBossClan();
                                return new BossClan(bossData, bossData.getInstancePos(), team, this, clan.getBossLevel());
                            }
                        };
                    } else {
                        // check đã đánh boss chưa
                        boolean hasAttack =((BossClanRoom) room).getBoss()!=null && ((BossClanRoom) room).getBoss().getBeDameInfo(user.getId()) > 0;
                        if (hasAttack) {
                            addErrResponse(getLang(Lang.err_already_hit_this_boss));
                            return;
                        }
                        player.clearDataNoCachePos(new Pos(0, -3));
                        ((BossClanRoom) room).addPlayer(player);
                    }
                    break;
                default:
                    room = new DefaultRoom(map, players, keyRoom);
                    break;
            }
            TaskMonitor.getInstance().addRoom(room);
        } else { // join vào room có sẵn
            switch (roomType) {
                case CLAN_BOSS:
                    isBattle = true;
                    if (user.getClan() == 0) {
                        addErrResponse(getLang(Lang.err_no_clan));
                        return;
                    }
                    ClanEntity clan = ClanManager.getInstance(user.getClan()).getClan();
                    if (clan == null) {
                        addErrSystem();
                        return;
                    }
                    room = clan.getCurBossRoom();
                    // check đã đánh boss chưa
                    BossClanRoom bossClanRoom = (BossClanRoom) room;
                    boolean hasAttack =bossClanRoom.getBoss()!=null && bossClanRoom.getBoss().getBeDameInfo(user.getId()) > 0;
                    if (hasAttack) {
                        addErrResponse(getLang(Lang.err_already_hit_this_boss));
                        return;
                    }
                    player.clearDataNoCachePos(new Pos(0, -3));
                    break;
            }
            room.addPlayer(player);
        }
        mUser.setRoomChanelId(chanelId);
        ChUtil.set(channel, ChUtil.KEY_ROOM, room);
        // tra ve id teleport next
        addResponse(INIT_MAP, CfgBattle.genInitMap(roomType.value, mapId, mUser.getRoomChanelId(), map.getMapData().getPlayerCollider(), isBattle, PopupType.NULL));
    }

    public static void initBackHome(MyUser mUser, PopupType popupType) {
        BaseMap map = ResMap.getMap(RoomType.HOME.value, 0);
        BaseRoom curRoom = (BaseRoom) ChUtil.get(mUser.getChannel(), ChUtil.KEY_ROOM);
        // xóa khỏi room cũ
        Player player = mUser.getPlayer();
        if (curRoom != null && curRoom.hasPlayer(player.getId())) {
            curRoom.removePlayer(player.getId());
        }
        player.clearDataForChangeRoom(mUser.getCachePos());
        player.resetData();

        boolean ok = false;
        int channelId = 1;
        BaseRoom room = null;
        while (!ok) {
            String keyRoom = CfgBattle.getKeyRoom(mUser, RoomType.HOME.value, 0, channelId);
            room = (BaseRoom) TaskMonitor.getInstance().getRoom(keyRoom);
            if (room == null) {
                List<Character> players = new ArrayList<>();
                players.add(player);
                room = new DefaultRoom(map, players, keyRoom);
                TaskMonitor.getInstance().addRoom(room);
                ok = true;
            } else {
                if (room.getAPlayer().size() > RoomType.HOME.maxPlayer) {
                    channelId++;
                } else {
                    ok = true;
                    room.addPlayer(player);
                }
            }
        }
        ChUtil.set(mUser.getChannel(), ChUtil.KEY_ROOM, room);
        mUser.setRoomChanelId(channelId);
        // tra ve id teleport next
        Util.sendProtoData(mUser.getChannel(), CfgBattle.genInitMap(RoomType.HOME.value, 0, channelId, map.getMapData().getPlayerCollider(), false, popupType), INIT_MAP);
        mUser.sendNotify();
    }

    public void initBoss() {
        List<Long> inputs = getInputALong();
        int type = Math.toIntExact(inputs.get(0));
        int mode = Math.toIntExact(inputs.get(1));
        // check fee
        List<Long> fee = Bonus.viewItem(ItemKey.BOSS_TICKET, -1);
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }

        RoomType bossType = RoomType.get(type);
        if (bossType == null) {
            addErrParam();
            return;
        }
        LevelType level = LevelType.get(mode);
        if (level == null || level.value > LevelType.NIGHTMARE.value) {
            addErrParam();
            return;
        }

        String keyRoom = CfgBattle.getKeyRoom(mUser, type, mode, user.getId());
        BaseRoom curRoom = (BaseRoom) ChUtil.get(channel, ChUtil.KEY_ROOM);
        boolean isReAttack = false;
        // xóa khỏi room cũ
        Player player = mUser.getPlayer();
        if (curRoom != null && curRoom.hasPlayer(player.getId())) {
            curRoom.removePlayer(player.getId());
            isReAttack = curRoom.getRoomType() == bossType.value;
        }
        // check có room hay chưa, có rồi thì join
        BaseRoom room = null;
        BaseMap baseMap = ResMap.getBossMap(bossType);
        List<Character> players = new ArrayList<>();
        if (isReAttack)
            player.clearDataNoCachePos(new Pos(0, -3));
        else player.clearDataForChangeRoom(new Pos(0, -3));
        players.add(player);
        switch (bossType) {
            case KIM_THAN -> {
                room = new KimThanRoom(baseMap, players, keyRoom, mode);
                mUser.getUData().checkQuestTutorial(mUser, QuestTutType.ATTACK_BOSS_GOD, 1, 1);
            }
            case THUY_THAN -> {
                room = new ThuyThanRoom(baseMap, players, keyRoom, mode);
                mUser.getUData().checkQuestTutorial(mUser, QuestTutType.ATTACK_BOSS_GOD, 2, 1);
            }
            case HOA_THAN -> {
                room = new HoaThanRoom(baseMap, players, keyRoom, mode);
                mUser.getUData().checkQuestTutorial(mUser, QuestTutType.ATTACK_BOSS_GOD, 3, 1);
            }
            case THO_THAN -> {
                room = new ThoThanRoom(baseMap, players, keyRoom, mode);
                mUser.getUData().checkQuestTutorial(mUser, QuestTutType.ATTACK_BOSS_GOD, 4, 1);
            }
        }
        TaskMonitor.getInstance().addRoom(room);
        ChUtil.set(channel, ChUtil.KEY_ROOM, room);
        // tra ve id teleport next
        addResponse(CfgBattle.genInitMap(bossType.value, 0, mode, baseMap.getMapData().getPlayerCollider(), true, PopupType.NULL));
    }

    public void smartBoss() {
        // todo check open
        int vip = mUser.getUser().getVip();
        if (vip < 2) {
            addErrResponse(String.format(getLang(Lang.err_vip_to_use), 2));
            return;
        }
        List<Long> inputs = getInputALong();
        int type = inputs.get(0).intValue();
        int mode = inputs.get(1).intValue();
        int number = inputs.get(2).intValue();
        // check fee
        List<Long> bonus = Bonus.viewItem(ItemKey.BOSS_TICKET, -number);
        String err = Bonus.checkMoney(mUser, bonus);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        RoomType bossType = RoomType.get(type);
        if (bossType == null) {
            addErrParam();
            return;
        }
        LevelType level = LevelType.get(mode);
        if (level == null || level.value > LevelType.NIGHTMARE.value) {
            addErrParam();
            return;
        }
        BaseMap baseMap = ResMap.getBossMap(bossType);
        ResMapBossEntity mapBoss = (ResMapBossEntity) baseMap;
        int bossId = mapBoss.getListEnemy().get(mode - 1);
        ResBossEntity boss = ResEnemy.getBoss(bossId);
        if (boss == null) {
            addErrParam();
            return;
        }
        for (int i = 0; i < number; i++) {
            bonus.addAll(boss.getBonusKillBoss(mUser.getPerReceiveBoss()));
        }
        bonus.addAll(CfgEventDrop.bonusDrop(CfgEventDrop.config.getRateDropBossGod(), number));
        addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.SMART_BOSS.getKey(number), Bonus.merge(bonus))));
        switch (bossType) {
            case KIM_THAN -> CfgQuest.addNumQuest(mUser, DataQuest.KILL_KIM_THAN, number);
            case THUY_THAN -> CfgQuest.addNumQuest(mUser, DataQuest.KILL_THUY_THAN, number);
            case HOA_THAN -> CfgQuest.addNumQuest(mUser, DataQuest.KILL_HOA_THAN, number);
            case THO_THAN -> CfgQuest.addNumQuest(mUser, DataQuest.KILL_THO_THAN, number);
        }

    }

    private void bossGodData() {
        addResponse(getCommonVector(mUser.getUData().getBossGod()));
    }

    void campaignData() {
        List<Integer> rewards = mUser.getUData().getCampaignReward();
        List<Integer> dataCampaign = mUser.getUData().getCampaign(); // id - num
        // check next stage
        ResCampaignEntity map = ResMap.getMapCampaign(dataCampaign.get(0));
        if (map != null && dataCampaign.get(1) >= map.getConquer()) {
            dataCampaign.set(0, dataCampaign.get(0) + 1);
            dataCampaign.set(1, 0);
            if (mUser.getUData().update(List.of("campaign", StringHelper.toDBString(dataCampaign)))) {
                mUser.getUData().setCampaign(dataCampaign.toString());
            }
        }

        Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
        pb.addAVector(getCommonIntVector(rewards));
        pb.addAVector(getCommonIntVector(dataCampaign));
        addResponse(IAction.CAMPAIGN_DATA, pb.build());
    }

    void campaignReward() {
        int mapId = getInputInt();
        List<Integer> rewards = mUser.getUData().getCampaignReward();
        List<Integer> dataCampaign = mUser.getUData().getCampaign();
        ResCampaignEntity map = ResMap.getMapCampaign(mapId);
        if (mapId > rewards.size() || map == null) {
            addErrParam();
            return;
        }
        // check đủ đk nhận chưa
        boolean hasBonus = rewards.get(mapId - 1) == 0 && (dataCampaign.get(0) > mapId || dataCampaign.get(0) == mapId && dataCampaign.get(1) >= map.getConquer());
        if (!hasBonus) {
            addErrParam();
            return;
        }
        rewards.set(mapId - 1, 1);
        if (mUser.getUData().update(List.of("campaign_reward", StringHelper.toDBString(rewards)))) {
            mUser.getUData().setCampaignReward(rewards.toString());
            addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.CAMPAIGN_CONQUER.getKey(mapId), map.getABonus())));
        }
        campaignData();
    }

    void campaignSmart() {
        List<Long> inputs = getInputALong();
        int mapId = inputs.get(0).intValue();
        int number = inputs.get(1).intValue();
        List<Integer> dataCampaign = mUser.getUData().getCampaign(); // id - num
        if (number <= 0 || mapId > dataCampaign.get(0)) {
            addErrParam();
            return;
        }
        List<Long> fee = Bonus.viewItem(ItemKey.THE_CAN_QUET, -number);
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        // hard code number x
        int smartNumber = 20;
        int maxKillSmart = 100;
        ResCampaignEntity rCam = ResMap.getMapCampaign(mapId);
        List<Integer> lstEnemy = rCam.getListEnemy();
        List<Long> bonus = new ArrayList<>();
        for (int i = 0; i < smartNumber; i++) {
            int id = lstEnemy.get(NumberUtil.getRandom(lstEnemy.size() / 2) * 2);
            ResEnemyEntity res = ResEnemy.getEnemy(id);
            bonus.addAll(BonusConfig.getRandomBonusMulti(res.getABonus()));
        }
        bonus = Bonus.xBonus(bonus, (maxKillSmart / smartNumber) * number);
        bonus.addAll(fee);
        addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.CAMPAIGN_SMART.getKey(mapId + "_" + number), bonus)));
        mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.USE_ITEM_CAMPAIGN_SMART, number);
    }

    void joinMap() {
        BaseRoom curRoom = (BaseRoom) ChUtil.get(channel, ChUtil.KEY_ROOM);
        if (curRoom == null) {
            addResponse(LOGIN_REQUIRE, null);
            return;
        }
        mUser.getPlayer().setJoinMap(curRoom);
        curRoom.joinMap(this);
    }

    void revivePlayer() {
        int type = getInputInt();
        Player player = ((MyUser) ChUtil.get(channel, ChUtil.KEY_M_USER)).getPlayer();
        ResTeleportEntity resTeleport = ResTeleport.getHomeTeleport();
        if (type != 0 && type != 1 || player.isAlive()) {
            addErrParam();
            return;
        }
        long checkTime = player.getTimeDie() + 4000;
        int per5 = (int) (mUser.getUser().getExp() * 0.1);
        if (type == 0 && System.currentTimeMillis() < checkTime) { // backHome
            player.revive();
            if (mUser.getUser().getExp() >= per5)
                addBonusPrivate(Bonus.receiveListItem(mUser, DetailActionType.REVIVE_FEE_10.getKey(), Bonus.viewExp(-per5)));
            initMap(resTeleport);
        } else {
            List<Long> fee = Bonus.viewItem(ItemKey.VE_HOI_SINH, -1);
            String err = Bonus.checkMoney(mUser, fee);
            if (err != null) {
                player.revive();
                if (mUser.getUser().getExp() >= per5)
                    addBonusPrivate(Bonus.receiveListItem(mUser, DetailActionType.REVIVE_FEE_10.getKey(), Bonus.viewExp(-per5)));
                initMap(resTeleport);
            } else {
                player.revive();
                addBonusToast(Bonus.receiveListItem(mUser, DetailActionType.REVIVE_PLAYER.getKey(), fee));
            }

        }
    }


    void changeMode() {
        CommonVector cmm = CommonProto.parseCommonVector(requestData);
        AutoMode mode = AutoMode.get((int) cmm.getALong(0));
        if (mode == null) {
            // addErrResponse();
            return;
        }
        UserSettingsEntity uSetting = mUser.getUSetting();
        if (uSetting.getAutoMode() == mode.value) {
            //addErrResponse();
            return;
        }
        if (uSetting.changeMode(mode)) {
            Player player = ((MyUser) ChUtil.get(channel, ChUtil.KEY_M_USER)).getPlayer();
            player.setAutoMode(mode);
            addResponse(getCommonVector(mode.value));
        } else addErrResponse();
    }

    void changeItemSlot() {
        CommonVector cmm = CommonProto.parseCommonVector(requestData);
        int slot = (int) cmm.getALong(0);
        int itemId = (int) cmm.getALong(1);
        if (slot != 0 && slot != 1) {
            addErrResponse(getLang(Lang.err_slot));
            return;
        }
        UserSettingsEntity uSetting = mUser.getUSetting();
        List<Integer> itemSlot = uSetting.getItemSlot(mUser);
        if (itemId == 0) {
            if (itemSlot.get(slot * 2 + 1) == itemId) {
                addErrResponse(getLang(Lang.err_params));
                return;
            }
            itemSlot.set(slot * 2 + 1, itemId);
        } else {
            UserItemEntity item = mUser.getResources().getItem(itemId);
            if (item == null) {
                addErrResponse(getLang(Lang.err_params));
                return;
            }
            if (itemSlot.get(slot * 2 + 1) == itemId) {
                addErrResponse(getLang(Lang.err_item_has_been_used));
                return;
            }
            if (item.getNumber() <= 0) {
                addErrResponse(getLang(Lang.err_item_number));
                return;
            }
            for (int i = 0; i < itemSlot.size(); i += 2) {
                if (itemSlot.get(i + 1) == itemId) {
                    itemSlot.set(i + 1, 0);
                }
            }
            itemSlot.set(slot * 2 + 1, itemId);
        }
        if (uSetting.saveSlot(itemSlot)) {
            Player player = ((MyUser) ChUtil.get(channel, ChUtil.KEY_M_USER)).getPlayer();
            player.setItemsBuf(uSetting.getItemSlot(mUser));
            addResponse(getCommonIntVector(itemSlot));
            mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.USE_ITEM_AUTO, 1);
        } else addErrResponse();
    }


    void changeAutoSlot() {
        CommonVector cmm = CommonProto.parseCommonVector(requestData);
        int slot = (int) cmm.getALong(0);
        int trigger = (int) cmm.getALong(1);
        if (trigger < 0 || trigger > 100) {
            addErrParam();
            return;
        }
        if (slot != 0 && slot != 1) {
            addErrResponse(getLang(Lang.err_slot));
            return;
        }
        UserSettingsEntity uSetting = mUser.getUSetting();
        if (uSetting.saveSlot(mUser, slot, trigger)) {
            Player player = ((MyUser) ChUtil.get(channel, ChUtil.KEY_M_USER)).getPlayer();
            player.setItemsBuf(uSetting.getItemSlot(mUser));
            addResponse(cmm);
        } else addErrResponse();
    }

    void changeChanel() {
        int inputChanel = getInputInt();
        if ((inputChanel < 0 || inputChanel > CfgServer.maxChannelOpen) && inputChanel != user.getId()) {
            addErrResponse(String.format(getLang(Lang.err_open_chanel), CfgServer.maxChannelOpen));
            return;
        }
        BaseRoom room = mUser.getPlayer().getRoom();
        if (room == null) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        int curChanel = mUser.getRoomChanelId();
        if (inputChanel == curChanel) {
            addErrResponse(getLang(Lang.err_use_chanel));
            return;
        }

        RoomType roomType = RoomType.get(room.getRoomType());
        if (!roomType.allowChangeChanel) {
            addErrResponse(getLang(Lang.err_change_chanel));
            return;
        }
        String keyRoom = CfgBattle.getKeyRoom(mUser, room.getRoomType(), room.getSubId(), inputChanel);
        BaseRoom curRoom = (BaseRoom) ChUtil.get(mUser.getChannel(), ChUtil.KEY_ROOM);
        if (curRoom != null && curRoom.getKeyRoom().equals(keyRoom)) {
            return;
        }
        room = (BaseRoom) TaskMonitor.getInstance().getRoom(keyRoom); // room mới
        if (room != null && room.isMaxPlayer()) {
            addErrResponse(getLang(Lang.err_full_player));
            return;
        }

        // xóa khỏi room cũ
        Player player = mUser.getPlayer();
        if (curRoom != null && curRoom.hasPlayer(player.getId())) {
            curRoom.removePlayer(player.getId());
        }
        if (room == null) {  // tao room moi
            List<Character> players = new ArrayList<>();
            players.add(player);
            switch (RoomType.get(curRoom.getRoomType())) {
                case CAMPAIGN:
                    if (curRoom.getSubId() > 0) {
                        room = new CampaignRoom(curRoom.getMapInfo(), players, keyRoom);
                        break;
                    } else {
                        room = new DefaultRoom(curRoom.getMapInfo(), players, keyRoom);
                        break;
                    }
                default:
                    room = new DefaultRoom(curRoom.getMapInfo(), players, keyRoom);
                    break;
            }
            TaskMonitor.getInstance().addRoom(room);
        } else {
            // check số lượng người trong room
            if (room.getAPlayer().size() > roomType.maxPlayer) {
                addErrResponse(getLang(Lang.err_full_player));
                return;
            }
            room.addPlayer(player);

        }
        ChUtil.set(mUser.getChannel(), ChUtil.KEY_ROOM, room);
        addResponse(null);
        mUser.setRoomChanelId(inputChanel);
        if (roomType == RoomType.HOME) mUser.sendNotify();
        // tra ve id teleport next
        addResponse(INIT_MAP, CfgBattle.genInitMap(room.getRoomType(), room.getSubId(), inputChanel, room.getMapInfo().getMapData().getPlayerCollider(), room.getSubId() > 0, PopupType.NULL));
    }

}
