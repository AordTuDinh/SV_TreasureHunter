package game.dragonhero.controller;

import game.battle.type.AutoMode;
import game.cache.JCache;
import game.cache.JCachePubSub;
import game.config.*;
import game.config.aEnum.BlockType;
import game.config.aEnum.NotifyType;
import game.config.aEnum.PopupType;
import game.config.lang.Lang;
import game.dragonhero.BattleConfig;
import game.dragonhero.mapping.*;
import game.dragonhero.mapping.main.MainUserEntity;
import game.dragonhero.server.Constans;
import game.dragonhero.server.IAction;
import game.dragonhero.service.Services;
import game.dragonhero.service.resource.ResIAP;
import game.dragonhero.service.user.Actions;
import game.dragonhero.table.BaseRoom;
import game.dragonhero.task.dbcache.MailCreatorCache;
import game.monitor.ClanManager;
import game.monitor.Online;
import game.object.MyUser;
import game.object.UserResources;
import game.protocol.CommonProto;
import io.netty.channel.Channel;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.*;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static game.config.lang.Lang.getTitle;

@NoArgsConstructor
public class LoginHandler extends AHandler {
    @Override
    public AHandler newInstance() {
        return new LoginHandler();
    }

    static LoginHandler instance;

    public static LoginHandler getInstance() {
        if (instance == null) {
            instance = new LoginHandler();
        }
        return instance;
    }

    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(LOGOUT, LOGIN_GAME, CHANGE_SERVER, NOTIFY);
        actions.forEach(action -> mHandler.put(action, this));
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        try {
            switch (actionId) {
                case IAction.LOGIN_GAME -> loginGame();
                case IAction.LOGOUT -> logoutGame();
                case IAction.CHANGE_SERVER -> changeServer();
                case IAction.NOTIFY -> checkNotifyFirst();
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }


//    void register() {
//        protocol.Pbmethod.PbRegister regis = CommonProto.parsePbRegister(requestData);
//        String veryfyData = regis.getUsername() + regis.getPassword() + regis.getSalt();
//        if (!Util.getMD5(veryfyData).equals(regis.getChecksum())) {
//            addErrResponse();
//            return;
//        }
//        if (DBJPA.count(CfgServer.DB_MAIN + "main_user", "username", regis.getUsername()) > 0) {
//            addErrResponse(getLang(Lang.err_login));
//            return;
//        }
//        MainUserEntity main = new MainUserEntity(regis, ((InetSocketAddress) channel.localAddress()).getHostString());
//        if (DBJPA.save(main)) {
//            addResponse(main.toProto(regis.getPassword()));
//        } else {
//            addErrResponse("Đăng kí thất bại");
//            return;
//        }
//    }

//    private String getVersionRange(String version) {
//        int currentVersionInt = Util.convertVersion2Int(version);
//        ConfigEntity config = (ConfigEntity) DBJPA.getUnique(CfgServer.DB_MAIN + "config_api", ConfigEntity.class, "k", "asset_version_thresholds");
//        String[] thresholds = new Gson().fromJson(config.getV(), new TypeToken<Thresholds>() {
//        }.getType());
//        String versionRange = null;
//        for (String t : thresholds) {
//            String[] v = t.split("-");
//            int start = Util.convertVersion2Int(v[0]);
//            int end = Util.convertVersion2Int(v[1]);
//            if (start <= currentVersionInt && currentVersionInt <= end) {
//                versionRange = t;
//                break;
//            }
//        }
//        return versionRange;
//    }

    void logoutGame() {
        BaseRoom room = (BaseRoom) ChUtil.get(channel, ChUtil.KEY_ROOM);
        String name = mUser.getUser().getUsername().split("_")[1];
        JCache.getInstance().removeValue("s:" + name);
        if (room != null && mUser != null) {
            room.removePlayer(mUser.getPlayer().getId());
            mUser.userLogout();
        }
        addResponse(getCommonVector(1));
    }

    void checkEvent() {
        // trả quà event nạp lần đầu
        EventInt uInt = mUser.getUEvent().getEventInt();
        UserEventEntity uEvent = mUser.getUEvent();
        int numberBuy = uInt.getValue(EventInt.TIME_BUY_FIRST_PURCHASE);
        if (numberBuy > 0) { // đã mua
            int dif = DateTime.getDayToNumberDay(numberBuy);
            if (dif == 1 && uEvent.getFirstPurchase() == 0 && uEvent.update(Arrays.asList("first_purchase", 1))) {
                MailCreatorCache.sendMail(UserMailEntity.builder().senderId(0).userId(user.getId()).senderName(String.format(getTitle(mUser,"bonus_first_purchase"), user.getName())).title(String.format(Lang.getTitle(mUser,"title_mail_first_purchase"), 2)).bonus(StringHelper.toDBString(ResIAP.bonusDayFirstPurchase.get(0))).build());
                uEvent.setFirstPurchase(1);
            }
            if (dif > 1) {
                int status = 0;
                if (uEvent.getFirstPurchase() == 0) {
                    MailCreatorCache.sendMail(UserMailEntity.builder().senderId(0).userId(user.getId()).senderName(String.format(getTitle(mUser,"bonus_first_purchase"), user.getName())).title(String.format(Lang.getTitle(mUser,"title_mail_first_purchase"), 2)).bonus(StringHelper.toDBString(ResIAP.bonusDayFirstPurchase.get(0))).build());
                    status = 1;
                }
                if (uEvent.getFirstPurchase() == 1) {
                    MailCreatorCache.sendMail(UserMailEntity.builder().senderId(0).userId(user.getId()).senderName(String.format(getTitle(mUser,"bonus_first_purchase"), user.getName())).title(String.format(Lang.getTitle(mUser,"title_mail_first_purchase"), 3)).bonus(StringHelper.toDBString(ResIAP.bonusDayFirstPurchase.get(1))).build());
                    status = 2;
                }
                if (status > 0 && uEvent.update(Arrays.asList("first_purchase", status))) {
                    uEvent.setFirstPurchase(status);
                }
            }
        }
    }


    void checkNotifyFirst() { // Chỉ dùng cho gọi lần đầu
        if (mUser == null) return;
        protocol.Pbmethod.CommonVector.Builder cmm = protocol.Pbmethod.CommonVector.newBuilder();
        if (Services.mailDAO.hasMail(mUser.getUser().getId())) {
            cmm.addALong(NotifyType.MAIL.value); // has mail
        }
        cmm.addAllALong(mUser.checkNotify());
        addResponse(IAction.NOTIFY, cmm.build());
    }

    void changeServer() {
//        BaseRoom room = (BaseRoom) ChUtil.get(channel, ChUtil.KEY_ROOM);
//        if (room != null && mUser != null) {
//            room.removePlayer(mUser.getPlayer().getId());
//            mUser.getPlayer().updateLastSession();
//        }
        addResponse(getCommonVector(1));
    }

    void loginGame() {
        Pbmethod.CommonVector cmm = CommonProto.parseCommonVector(requestData);
        String userName = cmm.getAString(0);
        String version = cmm.getAString(1);
        String osType = cmm.getAString(2);
        String language = cmm.getAString(3).toLowerCase();
        int serverId = (int) cmm.getALong(0);
        int loginType = (int) cmm.getALong(1);
        if (loginType == 1) {
            BaseRoom room = (BaseRoom) ChUtil.get(channel, ChUtil.KEY_ROOM);
            if (room != null && mUser != null) {
                room.removePlayer(mUser.getPlayer().getId());
                mUser.userLogout();
            }
        }
        String session = Online.getSession(userName);
        if (session == null || (loginType == 0 && !getSession().equals(session))) {
            addResponse(LOGIN_GAME_FAIL, CommonProto.getErrorMsg(getLang(Lang.err_login)));
            return;
        }
        userName = serverId + "_" + userName;
        UserEntity user = loginByUsername(userName, cmm,language);
        if (user == null) {
            registerGame(userName);
            user = loginByUsername(userName, cmm,language);
            if (user == null) {
                addResponse(LOGIN_GAME_FAIL, CommonProto.getErrorMsg(getLang(Lang.err_login)));
                return;
            }
        }
        MainUserEntity mainUser = (MainUserEntity) DBJPA.getUnique(CfgServer.DB_MAIN + "main_user", MainUserEntity.class, "id", user.getMainId());
        if (mainUser == null) {
            addErrSystem();
            return;
        }
        //check open server
        if (CfgServer.isRealServer() &&  Calendar.getInstance().getTime().getTime() < Constans.timeOpenServer.getTime() && mainUser.getCp().equals("test")) {
            addResponse(POPUP_INFO, Pbmethod.CommonVector.newBuilder().addALong(1).
                    addAString(getLang(Lang.msg_server_not_open)).build());
            return;
        }
        if(CfgServer.BAOTRI  && !osType.equals("UNITY")){
            addResponse(POPUP_INFO, Pbmethod.CommonVector.newBuilder().addALong(1).
                    addAString(getLang(Lang.msg_server_maintenance)).build());
            return;
        }


        if (user.getBlockType() == BlockType.BLOCK_LOGIN) {
            addResponse(LOGIN_GAME_BLOCK, CommonProto.getErrorMsg(getLang(Lang.err_user_block)));
            return;
        }

        // check tài khoản khác đăng nhậpkey)
        Channel oldChanel = Online.getChannel(user.getId());
        if (oldChanel != null && oldChanel.id() != channel.id()) {
            BaseRoom oldRoom = (BaseRoom) ChUtil.get(oldChanel, ChUtil.KEY_ROOM);
            MyUser oldUser = (MyUser) ChUtil.get(oldChanel, ChUtil.KEY_M_USER);
            if (oldRoom != null && oldUser != null && oldRoom.hasPlayer(user.getId())) {
                oldRoom.removePlayer(user.getId());
                oldUser.userLogout();
            }
            Util.sendProtoData(oldChanel, PopupType.FORCE_LOGOUT.toProto(mUser), IAction.POPUP_INFO);
            Online.logoutChannel(oldChanel);
            Online.addChannel(user, channel);
            ChUtil.set(channel, ChUtil.KEY_M_USER, mUser);
        }

        this.mUser = initUser(user);
        if (mUser == null) {
            addErrResponse();
            return;
        }
        mUser.setVersion(version);
        mUser.setSession(session);
        Online.addChannel(mUser.getUser(), channel);
        Pbmethod.PbLoginGame.Builder builder = Pbmethod.PbLoginGame.newBuilder();
        builder.setSession(session);
        // season
        builder.setSeason(CfgServer.getSeason());
        // get clan
        if (user.getClan() != 0) {
            ClanEntity myClan = ClanManager.getInstance(user.getClan()).getClan();
            addResponse(CLAN_INFO, myClan.toProto());
        }
        // game config
        loadGameConfig(mUser);
        // battleConfig
        loadBattleConfig();
        // user info
        builder.setUser(user.toProto(mUser));
        this.user = user;
        //  user point
        addResponse(builder.build());
        // tra user data luon
        ChUtil.setMUser(channel, mUser);
        // check event
        checkEvent();

        checkNotifyFirst();
        // set server ids
        List<Integer> serverIds = mainUser.getServerIds();
        if (serverIds.contains(user.getServer())) {
            // add to last
            serverIds.remove(Integer.valueOf(user.getServer()));
            serverIds.add(user.getServer());
        } else serverIds.add(user.getServer());
        if (mainUser.update(List.of("server_ids", StringHelper.toDBString(serverIds)))) {
            mainUser.setServerIds(serverIds.toString());
        }
        UserPartyEntity uParty = user.getParty();
        if(uParty!=null) uParty.addChannel(mUser);
        // notify
        CompletableFuture.runAsync(() -> Services.userService.afterLogin(mUser));
    }
    //region logic

    UserEntity loginByUsername(String username, Pbmethod.CommonVector cmm,String language) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            UserEntity user = (UserEntity) DBJPA.getUnique(session, "user", UserEntity.class, "username", username);
            if (user != null) {
                if (!StringHelper.isEmpty(user.getName())) { // đã tạo tài khoản
                    session.getTransaction().begin();
                    // check rr
                    long dayDif = DateTime.getDayDiff(user.getDateCreated(), Calendar.getInstance().getTime());
                    if (dayDif <= 30 && user.getRr() == dayDif - 1) {
                        user.setRr((int) dayDif);
                    } else user.setRr(0);
                    int numDayLogin = user.getNumDayLogin();
                    int dif = (int) DateTime.getDayDiff(user.getLastLogin(), Calendar.getInstance().getTime());
                    if (dif > 0) {
                        user.setNumDayLogin(numDayLogin + dif);
                    }
                    session.createNativeQuery("update user set login_time=login_time+1, last_login=now(),num_day_login=" + user.getNumDayLogin() + ", game_channel = '" + JCachePubSub.gameChannel + "' ,rr=" + user.getRr() + " where id = " + user.getId()).executeUpdate();
                    user.setLastLogin(Calendar.getInstance().getTime());
                    session.getTransaction().commit();
                } else { // lần đầu vào game
                    long dayDif = DateTime.getDayDiff(user.getDateCreated(), Calendar.getInstance().getTime());
                    if (dayDif <= 30 && user.getRr() == dayDif - 1) {
                        user.setRr((int) dayDif);
                    }
                    session.getTransaction().begin();
                    session.createNativeQuery("update user set game_channel= '" + JCachePubSub.gameChannel + "' ,rr=" + user.getRr() + " where id=" + user.getId()).executeUpdate();
                    session.getTransaction().commit();
                }

                String version = cmm.getAString(1).toLowerCase();
                String osType = cmm.getAString(2).toLowerCase();
                Actions.save(user, "user", "login", "ip", channel.localAddress(), "version", version, "os", osType, "lang", language);
                if (!StringHelper.isEmpty(language)) {
                    user.setLang(Lang.getValidLang(language.toLowerCase()));
                    user.update(List.of("lang", user.getLang()));
                }
            }
            return user;
        } catch (Exception ex) {
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return null;
    }

    private void loadGameConfig(MyUser mUser) {
        Pbmethod.ListCommonVector.Builder lstCmm = Pbmethod.ListCommonVector.newBuilder();
        // 0: list price slot bag
        Pbmethod.CommonVector.Builder cm0 = Pbmethod.CommonVector.newBuilder();
        cm0.addALong(CfgBag.maxSlotItem());
        cm0.addALong(CfgBag.maxSlotEquipment());
        cm0.addALong(CfgBag.maxSlotPiece());
        cm0.addALong(CfgArena.pointPerBonusStar);
        cm0.addAllALong(NumberUtil.converListIntToLong(CfgBag.config.priceSlot));
        //public key
        cm0.addAString(CfgServer.config.publicKey);
        lstCmm.addAVector(cm0);
        // for settings
        Pbmethod.CommonVector.Builder settings = Pbmethod.CommonVector.newBuilder();
        UserSettingsEntity uSet = mUser.getUSetting();
        settings.addALong(uSet.getAutoMode()); // auto mode
        settings.addALong(AutoMode.values().length);// number mode
        settings.addAllALong(GsonUtil.toListLong(uSet.getItemSlot(mUser))); // size 4 : item slot
        settings.addAllALong(uSet.getChatSetting()); // size 2 : chat setting
        lstCmm.addAVector(settings);
        // danh sách các thằng mình block chat
        lstCmm.addAVector(getCommonIntVector(uSet.listBlockChat()));
        // server setting
        Pbmethod.CommonVector.Builder cm4 = Pbmethod.CommonVector.newBuilder();
        cm4.addALong(CfgServer.maxChannelOpen);
        lstCmm.addAVector(cm4.build());
        addResponse(IAction.GAME_CONFIG, lstCmm.build());
    }

    private void loadBattleConfig() {
        Pbmethod.CommonVector.Builder cmm = Pbmethod.CommonVector.newBuilder();
        cmm.addALong((long) (BattleConfig.P_Height * 100));
        cmm.addALong((long) (BattleConfig.P_Width * 100));
        cmm.addALong((long) (BattleConfig.hSpeed * 1000));
        cmm.addALong((long) (BattleConfig.P_timeQuestRevive * 100));
        cmm.addALong((long) (BattleConfig.P_timeImmortal * 100));
        cmm.addALong((long) (BattleConfig.B_timeDelayAnim * 100));
        cmm.addALong(BattleConfig.B_acceleration * 100);
        cmm.addALong((long) (BattleConfig.C_Collider * 100));
        cmm.addALong((long) (BattleConfig.C_timeDelayAttackToMove * 100));
        cmm.addALong((long) (BattleConfig.M_timeBeHit * 100));
        cmm.addALong((long) (BattleConfig.CL_timeAliveTextHit * 100));
        cmm.addALong((long) (BattleConfig.CL_timeAliveComboHit * 100));
        cmm.addALong((long) (BattleConfig.P_timeStartAuto * 100));
        cmm.addALong((long) (BattleConfig.P_timeIdleToAuto * 100));
        cmm.addALong((long) (BattleConfig.P_delayReady * 100));
        cmm.addALong((long) (BattleConfig.P_attackRun2 * 100));
        cmm.addALong((long) (BattleConfig.C_SCALE_SPEED * 100));
        cmm.addALong((long) (BattleConfig.P_attackBlockMove * 100));
        cmm.addALong((long) (BattleConfig.P_TimeDelayActiveItem * 100));
        cmm.addALong((long) (BattleConfig.P_TimeDelayMoveDone * 100));
        cmm.addALong((long) (BattleConfig.M_timeBeHitClient * 100));
        cmm.addALong((long) (BattleConfig.m_LerpSpeedBar * 100));
        cmm.addALong(BattleConfig.timeSendBonusAfk * 100);
        cmm.addALong(BattleConfig.maxNumberOpenItem * 100);
        cmm.addALong((long) (BattleConfig.P_timeNoMove * 100));

        //
        addResponse(IAction.BATTLE_CONFIG, cmm.build());
    }

    public boolean registerGame(String username) {
        String realUsername = Online.getRealUsername(username);
        int serverId = Online.getServer(username);
        MainUserEntity mainUser = (MainUserEntity) DBJPA.getUnique(CfgServer.DB_MAIN + "main_user", MainUserEntity.class, "username", realUsername);
        if (mainUser == null) {
            return false;
        }
        String version = mainUser.getVersion();
        if (version == null) version = "";

        String cp = mainUser.getCp();
        cp = cp == null || cp.equals("null") ? "" : cp;
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            session.getTransaction().begin();
            UserEntity user = new UserEntity(username, "", serverId, JCachePubSub.gameChannel, mainUser.getId(), version);
            session.persist(user);
            // add cho 1 vai item
            // session.persist(new UserItemEntity(user.getId(), 22, 1));
            session.getTransaction().commit();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            Logs.error(username + " " + cp + "->" + GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return false;
    }

    MyUser initUser(UserEntity user) {
        MyUser mUser = new MyUser(user);
        if (dbInitUser(mUser) && mUser.getResources().isOk()) {
            return mUser;
        }
        return null;
    }

    boolean dbInitUser(MyUser mUser) {
        int userId = mUser.getUser().getId();
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            UserDataEntity uData = (UserDataEntity) DBJPA.getUnique(session, "user_data", UserDataEntity.class, "user_id", userId);
            if (uData == null) {
                uData = new UserDataEntity(userId);
                session.getTransaction().begin();
                session.persist(uData);
                session.getTransaction().commit();
            }
            UserSettingsEntity uSetting = (UserSettingsEntity) DBJPA.getUnique(session, "user_settings", UserSettingsEntity.class, "user_id", userId);
            if (uSetting == null) {
                uSetting = new UserSettingsEntity(userId);
                session.getTransaction().begin();
                session.persist(uSetting);
                session.getTransaction().commit();
            }

            UserEventEntity uEvent = (UserEventEntity) DBJPA.getUnique(session, "user_event", UserEventEntity.class, "user_id", userId);
            if (uEvent == null) {
                uEvent = new UserEventEntity(userId);
                session.getTransaction().begin();
                session.persist(uEvent);
                session.getTransaction().commit();
            }
            uEvent.checkEvent(mUser);

            mUser.setResources(new UserResources(mUser));
            List<UserItemEntity> items = session.createNativeQuery("select * from user_item where user_id=" + userId, UserItemEntity.class).getResultList();
            if (items != null) {
                List<UserItemEntity> itemSave = items.stream().filter(item -> !item.expired()).collect(Collectors.toList());
                mUser.getResources().setItems(itemSave);
            }

            List<UserWeaponEntity> weapons = session.createNativeQuery("select * from user_weapon where user_id = " + userId, UserWeaponEntity.class).getResultList();
            mUser.getResources().setWeapons(weapons);
            List<UserItemEquipmentEntity> itemEquips = session.createNativeQuery("select * from user_item_equipment where user_id = " + userId, UserItemEquipmentEntity.class).getResultList();
            mUser.getResources().setItemEquipments(itemEquips);
            List<UserPetEntity> pets = session.createNativeQuery("select * from user_pet where user_id = " + userId, UserPetEntity.class).getResultList();
            mUser.getResources().setPets(pets);
            List<UserItemFarmEntity> farms = session.createNativeQuery("select * from user_item_farm where user_id = " + userId, UserItemFarmEntity.class).getResultList();
            mUser.getResources().setFarms(farms);
            List<UserHeroEntity> heroes = session.createNativeQuery("select * from user_hero where user_id = " + userId, UserHeroEntity.class).getResultList();
            mUser.getResources().setHeroes(heroes);
            List<UserPieceEntity> pieces = session.createNativeQuery("select * from user_piece where user_id = " + userId, UserPieceEntity.class).getResultList();
            mUser.getResources().setPieces(pieces);
            List<UserPackEntity> packs = session.createNativeQuery("select * from user_pack where user_id = " + userId, UserPackEntity.class).getResultList();
            mUser.getResources().setPacks(packs);
            List<UserLandEntity> lands = session.createNativeQuery("select * from user_land where user_id = " + userId, UserLandEntity.class).getResultList();
            mUser.getResources().setLands(lands);

            mUser.setInitUData(uData, mUser.getUser());
            mUser.setUSetting(uSetting);
            mUser.setUEvent(uEvent);
            return true;
        } catch (Exception ex) {
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return false;
    }

    //endregion

    //region Entity
    @Data
    class Thresholds {
        private String[] thresholds;
    }
    //endregion
}
