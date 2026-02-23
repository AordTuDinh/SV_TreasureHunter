package game.dragonhero.controller;

import game.config.*;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.dragonhero.mapping.UserDataEntity;
import game.dragonhero.mapping.UserEntity;
import game.dragonhero.mapping.UserFriendRelationshipEntity;
import game.dragonhero.mapping.UserGiftEntity;
import game.dragonhero.server.IAction;
import game.dragonhero.service.Services;
import game.dragonhero.service.user.Bonus;
import game.monitor.Online;
import game.object.DataQuest;
import game.object.MyUser;
import game.protocol.CommonProto;
import io.netty.channel.Channel;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.GUtil;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.StringHelper;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static game.config.lang.Lang.getTitle;

public class FriendHandler extends AHandler {
    final int STATUS_REQ = 0;
    final int STATUS_IS_FRIEND = 1;

    @Override
    public AHandler newInstance() {
        return new FriendHandler();
    }

    static FriendHandler instance;

    public static FriendHandler getInstance() {
        if (instance == null) {
            instance = new FriendHandler();
        }
        return instance;
    }

    private List<Integer> noCheckFeature = Arrays.asList(FRIEND_LIST);

    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(FRIEND_STATUS, FRIEND_SEND_BONUS, FRIEND_LIST, FRIEND_LIST_REQ, FRIEND_RECOMMEND, FRIEND_SEND_REQUEST, FRIEND_DELETE, FRIEND_RESPONSE_APPLY, FRIEND_CHECK_ONLINE, FRIEND_FIND, FRIEND_GET_BONUS, FRIEND_QUICK_GIFT);
        actions.forEach(action -> mHandler.put(action, this));
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        checkTimeMonitor("s");

        if (!noCheckFeature.contains(actionId) && !CfgFeature.isOpenFeature(FeatureType.FRIENDS, mUser, this)) {
            return;
        }

        try {
            switch (actionId) {
                case FRIEND_STATUS -> status();
                case FRIEND_LIST -> getFriendList();
                case FRIEND_LIST_REQ -> getFriendRequest();
                case FRIEND_RECOMMEND -> recommendFriend();
                case FRIEND_SEND_REQUEST -> sendFriendRequest();
                case FRIEND_DELETE -> deleteFriend();
                case FRIEND_RESPONSE_APPLY -> repApply();
                case FRIEND_SEND_BONUS -> sendBonus();
                case FRIEND_CHECK_ONLINE -> checkOnline();
                case FRIEND_FIND -> findFriend();
                case FRIEND_GET_BONUS -> getBonus();
                case FRIEND_QUICK_GIFT -> quickGift();
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }

    }

    //region Handler
    void status() {
        Pbmethod.ListCommonVector.Builder pb = protocol.Pbmethod.ListCommonVector.newBuilder();
        protocol.Pbmethod.CommonVector.Builder builder = protocol.Pbmethod.CommonVector.newBuilder();
        builder.addALong(CfgFriend.config.limitedFriend);
        builder.addAllALong(NumberUtil.converListIntToLong(mUser.getUserDaily().getFriendSend()));
        pb.addAVector(builder);
        List<UserGiftEntity> userGift = Services.userDAO.getUserSendGift(mUser);
        List<Integer> lst = userGift != null ? userGift.stream().map(UserGiftEntity::getFromId).collect(Collectors.toList()) : new ArrayList<>();
        lst.removeAll(mUser.getUserDaily().getGirtReceive());
        pb.addAVector(getCommonIntVector(lst));
        addResponse(FRIEND_STATUS, pb.build());
    }

    void getFriendList() {
        List<UserFriendRelationshipEntity> listFriends = getListFriends();
        List<UserEntity> aFriendUserEntity = listFriends(listFriends);

        protocol.Pbmethod.PbListUser.Builder builder = protocol.Pbmethod.PbListUser.newBuilder();
        listFriends.forEach(friendRelationship -> {
            int friendId = friendRelationship.getUserId1() == user.getId() ? friendRelationship.getUserId2() : friendRelationship.getUserId1();
            UserEntity userEntity = aFriendUserEntity.stream().filter(user -> user.getId() == friendId).findFirst().orElse(null);
            if (userEntity != null) {
                Pbmethod.PbUser.Builder pbUser = userEntity.toProto().toBuilder();
                pbUser.addAllChannel(Online.getUserChannelInfo(friendId));
                builder.addAUser(pbUser);
            }
        });

        addResponse(IAction.FRIEND_LIST, builder.build());
    }

    List<UserEntity> listFriends(List<UserFriendRelationshipEntity> listFriends) {
        List<Integer> aFriendId = new ArrayList<>();
        for (int i = listFriends.size() - 1; i >= 0; i--) {
            UserFriendRelationshipEntity relationshipEntity = listFriends.get(i);
            int friendId = relationshipEntity.getUserId1() == user.getId() ? relationshipEntity.getUserId2() : relationshipEntity.getUserId1();
            if (!aFriendId.contains(friendId)) aFriendId.add(friendId);
            else {
                listFriends.remove(i);
                DBJPA.delete("user_friend_relationship", "user_id1", relationshipEntity.getUserId1(), "user_id2", relationshipEntity.getUserId2());
            }
        }
        if (user.getNumberFriend() != listFriends.size()) {
            user.setNumberFriend(listFriends.size());
            DBJPA.update("user", Arrays.asList("number_friend", listFriends.size()), Arrays.asList("id", user.getId()));
        }
        return Services.userDAO.getListUser(aFriendId);
    }

    void getFriendRequest() {
        Pbmethod.PbListUser.Builder builder = Pbmethod.PbListUser.newBuilder();
        List<UserFriendRelationshipEntity> lstRequestAdd = dbGetListReqApply();

        for (int i = 0; i < lstRequestAdd.size(); i++) {
            UserEntity userEntity = Online.getDbUser(lstRequestAdd.get(i).getUserId1());
            if (userEntity != null) {
                builder.addAUser(userEntity.toProto());
            }
        }
        addResponse(builder.build());
    }

    void recommendFriend() {
        List<UserEntity> lstUserRandom = dbGetRandomUser();
        List<Integer> hasFriends = new ArrayList<>();
        List<UserFriendRelationshipEntity> listFriends = getListFriends();
        listFriends.stream().forEach(fr -> {
            hasFriends.add(fr.getUserId1());
            hasFriends.add(fr.getUserId2());
        });
        Pbmethod.PbListUser.Builder builder = Pbmethod.PbListUser.newBuilder();
        for (int i = 0; i < lstUserRandom.size(); i++) {
            if (lstUserRandom.get(i).getId() == user.getId()) continue;
            if (hasFriends.contains(lstUserRandom.get(i).getId())) continue;
//            for (UserFriendRelationshipEntity friend : listFriends) {
//                if (friend.getUserId1() == lstUserRandom.get(i).getId() || friend.getUserId2() == lstUserRandom.get(i).getId()) {
//                    continue;
//                }
//            }
            builder.addAUser(lstUserRandom.get(i).toProto());
            if (builder.getAUserCount() >= 6) break;
        }
        addResponse(builder.build());
    }

    void sendFriendRequest() {
        Pbmethod.CommonVector comm = CommonProto.parseCommonVector(requestData);
        int friendId = (int) comm.getALong(0);
        UserEntity userEntity = Online.getDbUser(friendId);
        if (userEntity == null) {
            addErrResponse(getLang(Lang.err_user_not_exist));
            return;
        }
//        boolean checkSV = (CfgServer.isRealServer() && userEntity.getServer() > 0) || (!CfgServer.isRealServer() && userEntity.getServer() < 0);
//        if (!checkSV) {
//            addErrResponse(getTitle(Lang.err_user_not_exist));
//            return;
//        }
        //else {
        //    if (!CfgFriend.FRIEND_GLOBAL) {
        //        if (userEntity.getServer() != mUser.getUser().getServer()) {
        //            addErrResponse(getLang(Lang.err_friend_not_same_server));
        //            return;
        //        }
        //    }
        //}
        List<UserFriendRelationshipEntity> listFriends = getListFriends();
        if (listFriends.size() >= CfgFriend.config.limitedFriend) {
            addErrResponse(getLang(Lang.err_max_number_friend));
            return;
        }
        List<UserFriendRelationshipEntity> otherFriendList = dbGetListFriends(mUser);
        if (otherFriendList != null && otherFriendList.size() >= CfgFriend.config.limitedFriend) {
            addErrResponse(getLang(Lang.err_max_number_friend));
            return;
        }

        for (UserFriendRelationshipEntity friend : listFriends) {
            if (friend.getUserId1() == friendId || friend.getUserId2() == friendId) {
                addErrResponse(getLang(Lang.err_friend_already_be));
                return;
            }
        }

        List<UserFriendRelationshipEntity> lstRequestAdd = dbGetAllListReqApply();
        for (int i = 0; i < lstRequestAdd.size(); i++) {
            // neu minh da gui loi moi ket ban
            if (lstRequestAdd.get(i).getUserId2() == friendId) {
                Pbmethod.CommonVector.Builder builder = Pbmethod.CommonVector.newBuilder();
                builder.addALong(friendId);
                addResponse(builder.build());
                addErrResponse(getLang(Lang.request_add_friend_success));
                return;
            } else if (lstRequestAdd.get(i).getUserId1() == friendId) { // neu ban minh da gui loi moi ket ban
                addErrResponse(getLang(Lang.player_was_applied));
                return;
            }
        }

        // set notify to friend
        MyUser userFriend = Online.getMUser(userEntity.getId());
        if (userFriend != null) {
            if (userFriend.getUData().getFriendNotify() != 1) {
                userFriend.getUData().setFriendNotify(1);
                userFriend.getUData().update(Arrays.asList("friend_notify", 1));
            }
            userFriend.addNotify(NotifyType.FRIEND_REQUEST);
        } else {
            UserDataEntity userDataEntity = dbGetUserData(friendId);
            if (userDataEntity != null) {
                userDataEntity.setFriendNotify(1);
                boolean updateUserData = userDataEntity.update(Arrays.asList("friend_notify", 1));
                if (!updateUserData) {
                    addErrResponse();
                    return;
                }
            }
        }
        //
        UserFriendRelationshipEntity userFriendEntity = new UserFriendRelationshipEntity(mUser.getUser().getId(), friendId, STATUS_REQ);
        boolean isOk = DBJPA.save(userFriendEntity);
        if (isOk) {
            Pbmethod.CommonVector.Builder builder = Pbmethod.CommonVector.newBuilder();
            builder.addALong(friendId);
            addResponse(builder.build());
            addErrResponse(getLang(Lang.request_add_friend_success));
            mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.SEND_REQUEST_FRIEND, 1);
        } else addErrResponse();
    }

    void repApply() {
        Pbmethod.CommonVector comm = CommonProto.parseCommonVector(requestData);
        Pbmethod.CommonVector.Builder builder = Pbmethod.CommonVector.newBuilder();
        List<UserFriendRelationshipEntity> listFriends = getListFriends();
        if (comm.getALongCount() == 2) { // tra loi ket ban
            int friendId = (int) comm.getALong(0);
            int answer = (int) comm.getALong(1);
            boolean isOk = answer > 0 ? true : false;
            UserFriendRelationshipEntity userFriendEntity = dbGetUserApply(friendId);

            if (userFriendEntity != null) {
                if (isOk) {
                    if (listFriends.size() >= CfgFriend.config.limitedFriend) {
                        addErrResponse(getLang(Lang.err_max_number_friend));
                        return;
                    }
                    //
                    List<UserFriendRelationshipEntity> otherFriendList = dbGetListFriends(mUser);
                    if (otherFriendList != null && otherFriendList.size() >= CfgFriend.config.limitedFriend) {
                        addErrResponse(getLang(Lang.err_max_number_friend));
                        return;
                    }
                    //
                    for (UserFriendRelationshipEntity friend : listFriends) {
                        if (friend.getUserId2() == friendId || friend.getUserId1() == friendId) {
                            DBJPA.delete("user_friend_relationship", "user_id1", friend.getUserId1(), "user_id2", friend.getUserId2());
                            addErrResponse(getLang(Lang.err_friend_already_be));
                            return;
                        }
                    }
                    //
                    userFriendEntity.setRelationship(STATUS_IS_FRIEND);
                    userFriendEntity.setTimeCreated(System.currentTimeMillis());
                    boolean isDone = userFriendEntity.update();
                    if (isDone) {
                        listFriends.add(userFriendEntity);
                        MyUser friend = Online.getMUser(friendId);
                        if (friend != null) {
                            friend.addResponse(IAction.FRIEND_NEW, user.toProto());
                        }
                    } else addErrResponse();
                } else {
                    boolean isDone = userFriendEntity.deleteFriend();
                    if (!isDone) {
                        addErrResponse();
                        return;
                    }
                }
                builder.addALong(friendId).addALong(answer);
            } else addErrResponse();
        } else { // tu choi tat ca loi moi
            List<Object> listUserReq = new ArrayList<>();
            for (int i = 0; i < comm.getALongCount(); i++) {
                listUserReq.add(comm.getALong(i));
                i += 1;
            }
            builder.addAllALong(comm.getALongList());

            boolean deleteDone = listUserReq.isEmpty() ? true : DBJPA.deleteIn("user_friend_relationship", "user_id1", listUserReq, "user_id2", mUser.getUser().getId());
            if (!deleteDone) {
                addErrResponse();
                return;
            }
        }

        if (dbGetListReqApply().size() == 0) {
            if (mUser.getUData().getFriendNotify() != 0) {
                mUser.getUData().setFriendNotify(0);
                mUser.getUData().update(Arrays.asList("friend_notify", 0));
            }
        }
        addResponse(builder.build());
    }

    void deleteFriend() {
        Pbmethod.CommonVector comm = CommonProto.parseCommonVector(requestData);
        int friendId = (int) comm.getALong(0);
        UserFriendRelationshipEntity userFriendEntity = dbGetFriendRelationship(friendId);
        //
        if (userFriendEntity == null) {
            addErrResponse(getLang(Lang.no_friend_in_list));
            return;
        }

        boolean isdone = userFriendEntity.deleteFriend();
        if (!isdone) {
            addErrResponse();
            return;
        } else {
            List<UserFriendRelationshipEntity> listFriends = getListFriends();
            listFriends.remove(userFriendEntity);
            Pbmethod.CommonVector.Builder builder = Pbmethod.CommonVector.newBuilder();
            builder.addALong(friendId);
            addResponse(FRIEND_DELETE, builder.build());
        }
    }


    void sendBonus() {
        int friendId = (int) CommonProto.parseCommonVector(requestData).getALong(0);
        List<Integer> lstSend = mUser.getUserDaily().getFriendSend();
        boolean isFriends = false;
        List<UserFriendRelationshipEntity> listFriends = getListFriends();
        for (int i = 0; i < listFriends.size(); i++) {
            if (listFriends.get(i).hasId(friendId)) {
                isFriends = true;
                break;
            }
        }
        if (!isFriends) {
            addErrResponse(getLang(Lang.no_friend_in_list));
            return;
        }
        if (lstSend.contains(friendId)) {
            addErrResponse(getLang(Lang.err_friend_send_bonus));
            return;
        }
        UserGiftEntity gift = new UserGiftEntity(friendId, user.getId());
        if (gift.update()) {
            lstSend.add(friendId);
            mUser.getUserDaily().updateSendFriend(StringHelper.toDBString(lstSend));
            addResponse(getCommonIntVector(lstSend));
            addBonusToastPlus(Bonus.receiveListItem(mUser, DetailActionType.BONUS_FRIEND_SEND.getKey(friendId), Bonus.viewItem(ItemKey.THAN_THIEN, 1)));
            status();
            CfgQuest.addNumQuest(mUser, DataQuest.SEND_FRIEND_GIFT, 1);
            mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.SEND_FRIEND_GIFT, 1);
        } else addErrResponse();
    }

    void checkOnline() {
        List<Long> inputs = getInputALong();
        List<Long> status = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            status.add(Online.isOnline(Math.toIntExact(inputs.get(i))) ? 1L : 0L);
        }
        addResponse(getCommonVector(status));
    }

    void findFriend() {
        Pbmethod.CommonVector cmm = getInputCmv();
        String input = cmm.getAString(0);
        List<UserEntity> listFind = dbFindFriend(input);
        List<Integer> hasFriends = new ArrayList<>();
        List<UserFriendRelationshipEntity> listFriends = getListFriends();
        listFriends.stream().forEach(fr -> {
            hasFriends.add(fr.getUserId1());
            hasFriends.add(fr.getUserId2());
        });
        Pbmethod.PbListUser.Builder builder = Pbmethod.PbListUser.newBuilder();
        for (int i = 0; i < listFind.size(); i++) {
            if (listFind.get(i).getId() == user.getId()) continue;
            if (hasFriends.contains(listFind.get(i).getId())) continue;
            builder.addAUser(listFind.get(i).toProto());
        }
        addResponse(builder.build());
    }

    void getBonus() {
        int friendId = getInputInt();
        List<UserGiftEntity> gift = Services.userDAO.getUserSendGift(mUser);
        if (gift == null) {
            addErrResponse(getLang(Lang.err_no_bonus));
            return;
        }
        List<UserGiftEntity> myGift = gift.stream().filter(i -> i.getFromId() == friendId).toList();
        if (myGift.isEmpty()) {
            addErrResponse(getLang(Lang.err_no_bonus));
            return;
        }
        List<Integer> idSend = mUser.getUserDaily().getGirtReceive();
        if (idSend.contains(friendId)) {
            addErrResponse(getLang(Lang.err_received_gift));
            return;
        }
        idSend.add(friendId);
        if (mUser.getUserDaily().updateGiftReceive(StringHelper.toDBString(idSend))) {
            addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.BONUS_FRIEND_GET.getKey(friendId), Bonus.viewItem(ItemKey.THAN_THIEN, 1))));
            status();
        } else addErrSystem();
    }

    void quickGift() {
        List<UserGiftEntity> gift = Services.userDAO.getUserSendGift(mUser);
        List<Integer> idReceive = mUser.getUserDaily().getGirtReceive(); //todo update
        List<UserFriendRelationshipEntity> listFriends = getListFriends();
        List<UserEntity> aFriendUserEntity = listFriends(listFriends);
        List<Integer> friendIds = aFriendUserEntity.stream().map(UserEntity::getId).toList();
        int numBonus = 0;
        if (gift != null) {
            for (int i = 0; i < gift.size(); i++) {
                UserGiftEntity curGif = gift.get(i);
                // còn là bạn bè và chưa nhận quà
                if (friendIds.contains(curGif.getFromId()) && !idReceive.contains(curGif.getFromId())) {
                    idReceive.add(curGif.getFromId());
                    numBonus++;
                }
            }
        }
        List<Long> bonus = Bonus.xBonus(Bonus.viewGem(10), numBonus);
        List<Integer> lstSend = mUser.getUserDaily().getFriendSend();
        String giftsUpdate = "";
        boolean update = false;
        int countSend = 0;
        for (Integer friendId : friendIds) {
            if (!lstSend.contains(friendId)) {
                lstSend.add(friendId);
                giftsUpdate += "(" + friendId + "," + user.getId() + ",NOW()),";
                update = true;
                countSend++;
            }
        }
        if (update) giftsUpdate = giftsUpdate.substring(0, giftsUpdate.length() - 1);
        else {
            addErrResponse(getLang(Lang.err_friend_send_bonus_all));
            return;
        }
        if (updateQuickGift(mUser, update, giftsUpdate, idReceive, lstSend)) {
            mUser.getUserDaily().setFriendSend(lstSend.toString());
            mUser.getUserDaily().setGiftReceive(idReceive.toString());
            addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.BONUS_QUICK_SEND.getKey(), bonus)));
            addBonusToastPlus(Bonus.receiveListItem(mUser, DetailActionType.BONUS_FRIEND_SEND.getKey(-1), Bonus.viewItem(ItemKey.THAN_THIEN, countSend)));
            mUser.getCache().del("user_send_gift");
            status();
            // quest tutorial
            mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.SEND_FRIEND_GIFT, lstSend.size());
            // quest
            CfgQuest.addNumQuest(mUser, DataQuest.SEND_FRIEND_GIFT, lstSend.size());
            CfgQuest.addNumQuest(mUser, DataQuest.GET_FRIEND_GIFT, idReceive.size());
        } else addErrResponse();
    }


    List<UserFriendRelationshipEntity> getListFriends() {
        List<UserFriendRelationshipEntity> lstUserFriend = dbGetListFriends(mUser);
        if (lstUserFriend == null) {
            addErrResponse();
            return null;
        }
        return lstUserFriend;
    }


    // endregion


    // region db
    List<UserEntity> dbFindFriend(String input) {
        List<UserEntity> aUser = new ArrayList<>();
        Query query;
        if (CfgServer.isRealServer()) {
            query = DBJPA.getEntityManager().createNativeQuery("select * from user where server>=4 and (NAME LIKE  :input OR id LIKE :input)  order by last_action desc limit 15", UserEntity.class);
            query.setParameter("input", "%" + input + "%");
        } else {
            query = DBJPA.getEntityManager().createNativeQuery("select * from user where server<4 and (NAME LIKE  :input OR id LIKE :input) order by last_action desc limit 15", UserEntity.class);
            query.setParameter("input", "%" + input + "%");
        }
        aUser = query.getResultList();
        DBJPA.closeSession(DBJPA.getEntityManager());
        return aUser;
    }

    private boolean updateQuickGift(MyUser mUser, boolean updateGift, String gifts, List<Integer> idReceive, List<Integer> lstSend) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            session.getTransaction().begin();
            session.createNativeQuery("update user_daily set friend_send=:send ,gift_receive=:receive  where user_id=" + mUser.getUser().getId()).setParameter("send", StringHelper.toDBString(lstSend)).setParameter("receive", StringHelper.toDBString(idReceive)).executeUpdate();
            if (updateGift)
                session.createNativeQuery("insert into dson.user_gift(user_id,from_id,time_send) VALUES" + gifts + " ON DUPLICATE KEY UPDATE time_send=NOW()").executeUpdate();
            session.getTransaction().commit();
            return true;
        } catch (Exception ex) {
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return false;
    }

    List<UserFriendRelationshipEntity> dbGetListReqApply() {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            List<UserFriendRelationshipEntity> lstRequest = new ArrayList<>();
            lstRequest = session.createNativeQuery("select * from user_friend_relationship where user_id2=" + user.getId() + " and relationship= 0 limit 10", UserFriendRelationshipEntity.class).getResultList();
            return lstRequest;
        } catch (Exception ex) {
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return null;
    }

    UserFriendRelationshipEntity dbGetFriendRelationship(int userIdFriend) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            List<UserFriendRelationshipEntity> lstResult = new ArrayList<>();
            lstResult = session.createNativeQuery("select * from user_friend_relationship where (user_id2=" + user.getId() + " and user_id1=" + userIdFriend + ") or (user_id1=" + user.getId() + " and user_id2=" + userIdFriend + ")", UserFriendRelationshipEntity.class).getResultList();
            if (lstResult.size() > 0) return lstResult.get(0);
            else return null;
        } catch (Exception ex) {
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return null;
    }

    List<UserFriendRelationshipEntity> dbGetAllListReqApply() {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            List<UserFriendRelationshipEntity> lstRequest = new ArrayList<>();
            lstRequest = session.createNativeQuery("select * from user_friend_relationship where user_id1=" + user.getId() + " or user_id2=" + user.getId() + " and relationship= 0", UserFriendRelationshipEntity.class).getResultList();
            return lstRequest;
        } catch (Exception ex) {
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return null;
    }

    public static List<UserFriendRelationshipEntity> dbGetListFriends(MyUser mUser) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            List<UserFriendRelationshipEntity> lstFriends = new ArrayList<>();
            lstFriends = session.createNativeQuery("select * from user_friend_relationship where (user_id1=" + mUser.getUser().getId() + " or user_id2=" + mUser.getUser().getId() + ") and relationship= 1", UserFriendRelationshipEntity.class).getResultList();
            while (lstFriends.size() > CfgFriend.config.limitedFriend) {
                DBJPA.delete("user_friend_relationship", "user_id1", lstFriends.get(0).getUserId1(), "user_id1", lstFriends.get(0).getUserId2());
                lstFriends.remove(0);
            }
            return lstFriends;
        } catch (Exception ex) {
            Logs.error(GUtil.exToString(ex));
        } finally {
            DBJPA.closeSession(session);
        }
        return null;
    }

    UserDataEntity dbGetUserData(int userIdFriend) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            List<UserDataEntity> lstResult = new ArrayList<>();
            lstResult = session.createNativeQuery("select * from user_data where user_id = " + userIdFriend, UserDataEntity.class).getResultList();
            if (lstResult.size() > 0) return lstResult.get(0);
            else return null;
        } catch (Exception ex) {
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return null;
    }


    List<UserEntity> dbGetRandomUser() {
        return CfgFriend.getASuggestUser(user.getServer());
    }

    UserFriendRelationshipEntity dbGetUserApply(int userIdFriend) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            List<UserFriendRelationshipEntity> lstResult = new ArrayList<>();
            lstResult = session.createNativeQuery("select * from user_friend_relationship where user_id2=" + user.getId() + " and user_id1=" + userIdFriend, UserFriendRelationshipEntity.class).getResultList();
            if (lstResult.size() > 0) return lstResult.get(0);
            else return null;
        } catch (Exception ex) {
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return null;
    }
    // endregion
}
