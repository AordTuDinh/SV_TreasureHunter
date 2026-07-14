package game.treasure.controller;

import game.cache.JCache;
import game.cache.JCachePubSub;
import game.config.*;
import game.config.aEnum.BlockType;
import game.config.aEnum.PopupType;
import game.config.lang.Lang;
import game.treasure.BattleConfig;
import game.treasure.service.resource.ResAvatar;
import game.treasure.mapping.*;
import game.treasure.mapping.main.MainUserEntity;
import game.treasure.server.Constans;
import game.treasure.server.IAction;
import game.treasure.service.Services;
import game.treasure.service.battle.PvpCupService;
import game.treasure.service.resource.ResIAP;
import game.treasure.service.user.Actions;
import game.treasure.table.BaseRoom;
import game.treasure.task.dbcache.MailCreatorCache;
import game.monitor.ClanManager;
import game.monitor.Online;
import game.monitor.MaintenanceChecker;
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
        if (mUser != null) {
            if (room != null) {
                room.removeUnit(mUser.getPlayer().getId());
            }
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
                MailCreatorCache.sendMail(UserMailEntity.builder().senderId(0).userId(user.getId()).senderName(String.format(getTitle(mUser, "bonus_first_purchase"), user.getName())).title(String.format(Lang.getTitle(mUser, "title_mail_first_purchase"), 2)).bonus(StringHelper.toDBString(ResIAP.bonusDayFirstPurchase.get(0))).build());
                uEvent.setFirstPurchase(1);
            }
            if (dif > 1) {
                int status = 0;
                if (uEvent.getFirstPurchase() == 0) {
                    MailCreatorCache.sendMail(UserMailEntity.builder().senderId(0).userId(user.getId()).senderName(String.format(getTitle(mUser, "bonus_first_purchase"), user.getName())).title(String.format(Lang.getTitle(mUser, "title_mail_first_purchase"), 2)).bonus(StringHelper.toDBString(ResIAP.bonusDayFirstPurchase.get(0))).build());
                    status = 1;
                }
                if (uEvent.getFirstPurchase() == 1) {
                    MailCreatorCache.sendMail(UserMailEntity.builder().senderId(0).userId(user.getId()).senderName(String.format(getTitle(mUser, "bonus_first_purchase"), user.getName())).title(String.format(Lang.getTitle(mUser, "title_mail_first_purchase"), 3)).bonus(StringHelper.toDBString(ResIAP.bonusDayFirstPurchase.get(1))).build());
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
        addResponse(IAction.NOTIFY, CommonProto.getCommonVectorProto(mUser.buildNotifyList()));
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
            if (mUser != null) {
                if (room != null) {
                    room.removeUnit(mUser.getPlayer().getId());
                }
                mUser.userLogout();
            }
        }
        String session = Online.getSession(userName);
        if (session == null || (loginType == 0 && !getSession().equals(session))) {
            addResponse(LOGIN_GAME_FAIL, CommonProto.getErrorMsg(getLang(Lang.err_login)));
            return;
        }
        userName = serverId + "_" + userName;
        UserEntity user = loginByUsername(userName, cmm, language);
        if (user == null) {
            registerGame(userName);
            user = loginByUsername(userName, cmm, language);
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
        if (CfgServer.isRealServer() && Calendar.getInstance().getTime().getTime() < Constans.timeOpenServer.getTime() && mainUser.getCp().equals("test")) {
            addResponse(POPUP_INFO, Pbmethod.CommonVector.newBuilder().addALong(1).
                    addAString(getLang(Lang.msg_server_not_open)).build());
            return;
        }

        if (user.getBlockType() == BlockType.BLOCK_LOGIN) {
            addResponse(LOGIN_GAME_BLOCK, CommonProto.getErrorMsg(getLang(Lang.err_user_block)));
            return;
        }

        if (MaintenanceChecker.isMaintenance()) {
            String mipMsg = MaintenanceChecker.getMipMessage();
            if (mipMsg == null || mipMsg.isEmpty()) {
                mipMsg = getLang(Lang.msg_server_maintenance);
            }
            addResponse(LOGIN_GAME_BLOCK, CommonProto.getErrorMsg(mipMsg));
            return;
        }

        // check tài khoản khác đăng nhậpkey)
        Channel oldChanel = Online.getChannel(user.getId());
        if (oldChanel != null && oldChanel.id() != channel.id()) {
            BaseRoom oldRoom = (BaseRoom) ChUtil.get(oldChanel, ChUtil.KEY_ROOM);
            MyUser oldUser = (MyUser) ChUtil.get(oldChanel, ChUtil.KEY_M_USER);
            if (oldRoom != null && oldUser != null && oldRoom.hasPlayer(oldUser.getPlayer().getId())) {
                oldRoom.removeUnit(oldUser.getPlayer().getId());
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
        // get clan
        if (user.getClan() > 0) {
            ClanEntity myClan = ClanManager.getInstance(user.getClan()).getClan();
            addResponse(CLAN_INFO, myClan.toProto());
        }
        // game config
        loadGameConfig(mUser);
        // battleConfig
        loadBattleConfig();
        // qua ngày + 0 cup → tặng 1 cup (trước user proto để client nhận cup đúng)
        List<Long> dailyCup = PvpCupService.grantDailyFloorIfNeeded(mUser);
        if (!dailyCup.isEmpty())
            addBonusToastPlus(dailyCup);
        // user info
        builder.setUser(user.toProto(mUser));
        this.user = user;
        //  user point
        addResponse(builder.build());
        // tra user data luon
        ChUtil.setMUser(channel, mUser);
        game.treasure.service.user.UserBuff.onLogin(mUser);
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
        // notify — afterLogin có thể tạo mail (system/thẻ); gửi lại NOTIFY khi xong
        CompletableFuture.runAsync(() -> {
            Services.userService.afterLogin(mUser);
            if (mUser != null && mUser.getChannel() != null) {
                mUser.sendNotify();
            }
        });
    }
    //region logic

    UserEntity loginByUsername(String username, Pbmethod.CommonVector cmm, String language) {
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
        //public key
        cm0.addAString(CfgServer.config.publicKey);
        lstCmm.addAVector(cm0);
        // for settings
        Pbmethod.CommonVector.Builder settings = Pbmethod.CommonVector.newBuilder();
        UserSettingsEntity uSet = mUser.getUSetting();
        if (uSet == null) {
            Logs.error("loadGameConfig: uSetting null userId=" + mUser.getUser().getId() + " — dbInitUser thiếu setUSetting");
            uSet = new UserSettingsEntity(mUser.getUser().getId());
            mUser.setUSetting(uSet);
        }
        // size 2 : chat setting
        settings.addAllALong(uSet.getChatSetting());
        lstCmm.addAVector(settings);
        // auto sell item setting (index = AutoSell enum, value = 0/1)
        lstCmm.addAVector(getCommonIntVector(uSet.getAutoSellItemList()));
        // auto sell material setting (index = materialIndex * 4 + tier - 1, value = 0/1)
        lstCmm.addAVector(getCommonIntVector(uSet.getAutoSellMaterialList()));
        // danh sách các thằng mình block chat
        lstCmm.addAVector(getCommonIntVector(uSet.listBlockChat()));
        // fee up item
        lstCmm.addAVector(getCommonIntVector(CfgItem.UPGRADE_FEE_BASE_T1));
        // fee up hp
        lstCmm.addAVector(getCommonIntVector(CfgItem.SELL_PRICE_BASE_T1));
        // artifact coeffs: baseCost, upgradeMult×1000, sellMult×1000, craftCostPerTier
        lstCmm.addAVector(getCommonIntVector(CfgArtifact.getGameConfigCoeffs()));
        // auto range: [tầm đánh, tầm buff HP, autoAttackMob, auto_buff]
        lstCmm.addAVector(getCommonIntVector(uSet.getAutoRangeList()));
        // vip data: [26 int tích lũy theo VipType index]
        lstCmm.addAVector(getCommonIntVector(uSet.getVipDataList()));
        // ret
        addResponse(IAction.GAME_CONFIG, lstCmm.build());
    }

    private void loadBattleConfig() {
        Pbmethod.CommonVector.Builder cmm = Pbmethod.CommonVector.newBuilder();
        cmm.addALong((long) (BattleConfig.attackSpeed * 100L));
        cmm.addALong((long) (BattleConfig.P_Height * 100));
        cmm.addALong((long) (BattleConfig.P_Width * 100));
        cmm.addALong((long) (BattleConfig.hSpeed * 1000));
        cmm.addALong((long) (BattleConfig.P_timeStartAuto * 100));
        cmm.addALong((long) (BattleConfig.P_timeIdleToAuto * 100));
        cmm.addALong((long) (BattleConfig.P_delayReady * 100));
        cmm.addALong((long) (BattleConfig.C_SCALE_SPEED * 100));
        cmm.addALong((long) (BattleConfig.P_TimeDelayMoveDone * 100));
        cmm.addALong((long) (BattleConfig.m_LerpSpeedBar * 100));
        cmm.addALong((BattleConfig.m_LimitUseItem * 100));
        cmm.addALong((long) (BattleConfig.P_timeNoMove * 100));
        cmm.addALong((long) (BattleConfig.P_RangerAttack * 100));
        //
        addResponse(IAction.BATTLE_CONFIG, cmm.build());
    }

    public void registerGame(String username) {
        String realUsername = Online.getRealUsername(username);
        int serverId = Online.getServer(username);
        MainUserEntity mainUser = (MainUserEntity) DBJPA.getUnique(CfgServer.DB_MAIN + "main_user", MainUserEntity.class, "username", realUsername);
        if (mainUser == null) {
            return;
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
        } catch (Exception ex) {
            ex.printStackTrace();
            Logs.error(username + " " + cp + "->" + GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
    }

    MyUser initUser(UserEntity user) {
        MyUser mUser = new MyUser(user);
        if (!dbInitUser(mUser) || !mUser.getResources().isOk()) {
            return null;
        }
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            ResAvatar.ensureDefaultSkins(mUser, session);
        } finally {
            closeSession(session);
        }
        return mUser;
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
            } else if (uData.getItemSlot() == null || uData.getItemSlot().isEmpty()) {
                uData.ensureItemSlotInitialized();
                uData.update(List.of("item_slot", uData.getItemSlot()));
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
            List<UserItemEntity> items = session.createNativeQuery("select * from user_item where user_id=" + userId + " and type <> 2", UserItemEntity.class).getResultList();
            if (items != null) {
                mUser.getResources().setItems(items);
            }

            List<UserEquipmentEntity> equipments = session.createNativeQuery("select * from user_equipment where user_id=" + userId, UserEquipmentEntity.class).getResultList();
            if (equipments != null) {
                mUser.getResources().setEquipments(equipments);
            }



            List<UserArtifactEntity> userArtifacts = session.createNativeQuery("select * from user_artifact where user_id = " + userId, UserArtifactEntity.class).getResultList();
            mUser.getResources().setArtifacts(userArtifacts);

            List<UserPetEntity> pets = session.createNativeQuery("select * from user_pet where user_id = " + userId, UserPetEntity.class).getResultList();
            mUser.getResources().setPets(pets);

            List<UserMountEntity> mounts = session.createNativeQuery("select * from user_mount where user_id = " + userId, UserMountEntity.class).getResultList();
            mUser.getResources().setMounts(mounts);

            List<UserMobEntity> mobs = session.createNativeQuery("select * from user_mob where user_id = " + userId, UserMobEntity.class).getResultList();
            mUser.getResources().setMobs(mobs);

            List<UserPackEntity> packs = session.createNativeQuery("select * from user_pack where user_id = " + userId, UserPackEntity.class).getResultList();
            mUser.getResources().setPacks(packs);

            List<UserMaterialEntity> materials = session.createNativeQuery("select * from user_material where user_id = " + userId, UserMaterialEntity.class).getResultList();
            mUser.getResources().setMaterials(materials);

            List<UserSkinEntity> userSkins = session.createNativeQuery("select * from user_skin where user_id = " + userId, UserSkinEntity.class).getResultList();
            mUser.getResources().setSkins(userSkins);

            List<UserItemPointEntity> itemPoints = session.createNativeQuery("select * from user_item_point where user_id = " + userId, UserItemPointEntity.class).getResultList();
            mUser.getResources().setItemPoints(itemPoints);

            mUser.setInitUData(uData, mUser.getUser());
            uData.reconcileSlotsOnLogin(mUser);
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
