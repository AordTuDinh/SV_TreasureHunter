package game.object;

import com.google.protobuf.AbstractMessage;
import game.battle.model.Pet;
import game.battle.model.Player;
import game.battle.object.Point;
import game.battle.object.Pos;
import game.config.*;
import game.config.aEnum.*;
import game.treasure.BattleConfig;
import game.treasure.mapping.main.ResMapEntity;
import game.treasure.table.BaseRoom;
import game.treasure.controller.UserHandler;
import game.treasure.mapping.*;
import game.treasure.server.IAction;
import game.treasure.service.Services;
import game.treasure.service.battle.TreasureEventService;
import game.treasure.service.resource.ResItem;
import game.treasure.service.user.Bonus;
import game.monitor.ClanManager;
import game.protocol.CommonProto;
import io.netty.channel.Channel;
import lombok.Data;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.ChUtil;
import ozudo.base.helper.GUtil;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.StringHelper;
import ozudo.base.helper.Util;
import protocol.Pbmethod;


import javax.persistence.EntityManager;
import java.io.Serializable;
import java.util.*;

import static game.treasure.dao.UserDAO.getLogger;
import static ozudo.base.database.DBJPA.slowLog;

@Data
public class MyUser implements Serializable {
    UserEntity user;
    UserDataEntity uData;
    UserSettingsEntity uSetting;
    UserDailyEntity uDaily; // lưu data daily
    UserQuestEntity uQuest; // lưu data daily
    UserEventEntity uEvent; // lưu data event
    String session;
    UserResources resources;
    String version, udid;
    UserCache cache = new UserCache();
    List<Pbmethod.PbAction> msgNotify = new ArrayList<>();
    List<Long> aBonus = new ArrayList<>();
    Player player;
    Pet pet;
    Channel channel;
    Pos cachePos;
    Map<Integer, List<FriendChatObject>> aChatFriends = new HashMap<>();
    List<Integer> comboWeapon = NumberUtil.genListInt(6, 0);
    List<Integer> cacheSendParty = new ArrayList<>(); // [userId,timeSend, number]
    List<Integer> perReceiveBoss = List.of(0, 0);  // per tăng đá - per tăng drop
    List<Pbmethod.PbPointItemUpdate> itemPointUpdates = new ArrayList<>();
    boolean updateBagPending;
    boolean userDataInfoPending;
    boolean vipDataPending;
    /** Cache rate tăng drop (phần nghìn) — sync khi reCalculatePoint. 100 = +10%. */
    int rateDropGold;
    int rateDropGem;
    int rateDropItem;

    /**
     * Đồng bộ rate drop từ Point (point 17 vàng, 28 gem, 29 item).
     * 600 điểm → 100 phần nghìn (CfgStats.calcDropIncreaseRate).
     */
    public void syncDropRates(Point point) {
        if (point == null) {
            rateDropGold = 0;
            rateDropGem = 0;
            rateDropItem = 0;
            return;
        }
        rateDropGold = CfgStats.calcDropIncreaseRate(point.get(Point.P_GOLD_DROP_INCREASE));
        rateDropGem = CfgStats.calcDropIncreaseRate(point.get(Point.P_GEM_INCREASE));
        rateDropItem = CfgStats.calcDropIncreaseRate(point.get(Point.P_MATERIAL_INCREASE));
    }

    public void queueUpdateBag() {
        updateBagPending = true;
    }

    public boolean drainUpdateBagPending() {
        if (!updateBagPending)
            return false;
        updateBagPending = false;
        return true;
    }

    public void queueUserDataInfo() {
        userDataInfoPending = true;
    }

    public boolean drainUserDataInfoPending() {
        if (!userDataInfoPending)
            return false;
        userDataInfoPending = false;
        return true;
    }

    public void queueVipDataSync() {
        vipDataPending = true;
    }

    public boolean drainVipDataPending() {
        if (!vipDataPending)
            return false;
        vipDataPending = false;
        return true;
    }

    public void queueItemPointUpdate(UserItemEntity uItem) {
        Pbmethod.PbPointItemUpdate pb = ResItem.buildPointItemUpdate(uItem);
        if (pb != null)
            itemPointUpdates.add(pb);
    }

    public List<Pbmethod.PbPointItemUpdate> drainItemPointUpdates() {
        if (itemPointUpdates.isEmpty())
            return Collections.emptyList();
        List<Pbmethod.PbPointItemUpdate> copy = new ArrayList<>(itemPointUpdates);
        itemPointUpdates.clear();
        return copy;
    }

    public MyUser(UserEntity user) {
        this.user = user;
    }

    public void setInitUData(UserDataEntity uData, UserEntity user) {
        this.uData = uData;
    }

    public List<Long> checkNotify() {
        List<Long> ret = new ArrayList<>();
        UserDailyEntity uDaily = getUserDaily();
        if (uDaily == null) return ret;
        DataDaily uIntDaily = getUserDaily().getUDaily();
        // điểm danh
        if (uData.getStatusCheckIn() == 0) {
            ret.add((long) NotifyType.CHECK_IN.value);
        }
        // điểm danh bang
        UserClanEntity userClan = Services.userDAO.getUserClan(this);
        if (userClan != null && userClan.canCheckin()) {
            ret.add((long) NotifyType.GUILD_CHECKIN.value);
        }
        // lời mời kết bạn mới
        if (uData.getFriendNotify() != 0) {
            ret.add((long) NotifyType.FRIEND_REQUEST.value);
        }
        // bạn bè gửi quà
        List<UserGiftEntity> gifts = Services.userDAO.getUserSendGift(this);
        if (gifts.size() > 0) ret.add((long) NotifyType.FRIEND_SEND_GIFT.value);
        // phúc lợi
        if (notifyPhucLoi(uIntDaily)) ret.add((long) NotifyType.PHUC_LOI.value);

        // Quest 7 day
        UserEventSevenDayEntity uEvent7 = Services.userDAO.getUserSevenDay(this);
        if (uEvent7 != null && uEvent7.hasNotify(this)) {
            ret.add((long) NotifyType.QUEST_7_DAY.value);
        }

        // Free 100 scroll
        if (uEvent.notifyFree100Scroll(user)) ret.add((long) NotifyType.FREE_100_SCROLL.value);
        // fre dame skin
        if (uEvent.notifyFreeDameSkin(user)) ret.add((long) NotifyType.FREE_DAME_SKIN.value);

        // check notify clan
        if (user.getClan() > 0 && CfgClan.CLAN_RULE.contains(user.getClanPosition())) {
            List<ClanReqEntity> aReq = ClanManager.getInstance(user.getClan()).getClan().getAReq();
            if (aReq != null && aReq.size() > 0) {
                ret.add((long) NotifyType.CLAN_REQUEST.value);
            }
        }
        // check notify event 1hour
        if (getUserDaily().isNotifyEvent1H(this)) ret.add((long) NotifyType.EVENT_1_HOUR.value);
        // check notify event buy gold free

        if (uIntDaily.getValue(DataDaily.BUY_GOLD_0) == 0) ret.add((long) NotifyType.EVENT_BUY_GOLD.value);
        // check notify event lunch
        int lunch = uIntDaily.getValue(DataDaily.EAT_LUNCH);
        int dinner = uIntDaily.getValue(DataDaily.EAT_DINNER);
        if ((lunch != StatusType.DONE.value && CfgEvent.haveLunch()) || (dinner != StatusType.DONE.value && CfgEvent.haveDinner())) {
            ret.add((long) NotifyType.EVENT_LUNCH.value);
        }
        // check notify 14 day
        if (uEvent.notifyEvent14()) ret.add((long) NotifyType.EVENT_14_DAYS.value);
        // check notify event month
        if (uEvent.notifyEventMonth()) ret.add((long) NotifyType.EVENT_MONTH.value);
        // achievement
        UserAchievementEntity uAchie = Services.userDAO.getUserAchievement(this);
        if (uAchie != null) ret.addAll(uAchie.listNotify());
        // nhiệm vụ
        if (CfgQuest.isNotifyQuest(this, QuestType.QUEST_D)) ret.add((long) NotifyType.QUEST_D.value);
        if (CfgQuest.isNotifyQuest(this, QuestType.QUEST_C)) ret.add((long) NotifyType.QUEST_C.value);
        return ret;
    }

    /** Mail chưa nhận + toàn bộ notify event — dùng khi login / refresh NOTIFY. */
    public List<Long> buildNotifyList() {
        List<Long> ret = new ArrayList<>();
        if (user != null && Services.mailDAO.hasMail(user.getId())) {
            ret.add((long) NotifyType.MAIL.value);
        }
        ret.addAll(checkNotify());
        return ret;
    }

    private boolean notifyPhucLoi(DataDaily uIntDaily) {
        // Nhận hỗ trợ
        if (CfgEvent.isNotifySupport(uIntDaily)) return true;
        // Online 1h
        if (CfgEvent.isNotify1H(this)) return true;
        // Ưu đãi ngày
        if (CfgEvent.isNotifyUuDaiNgay(uIntDaily)) return true;
        // Đặc quyền
        if (CfgEvent.isNotifyDacQuyen(uIntDaily)) return true;
        // Quà nạp tiền
        if (CfgEvent.isNotifyQuyNapTien(uEvent)) return true;
        // Điểm danh
        if (CfgEvent.isNotifyCheckin(uData)) return true;
        // Quà giới hạn
        if (CfgEvent.isNotifyGioiHan(uIntDaily)) return true;
        // Vip
        if (CfgEvent.isNotifyBonusVip(uEvent, user)) return true;
        return false;
    }

    public void setCachePos() {
        this.cachePos = player.getPos().clone();
    }

    public UserDailyEntity getUserDaily() {
        if (uDaily == null) {
            uDaily = Services.userDAO.getUserDaily(this);
        } else uDaily.checkData();
        return uDaily;
    }

    public  int getUserId(){
        return user.getId();
    }

    public UserQuestEntity getUQuest() {
        if (uQuest == null) {
            uQuest = Services.userDAO.getUserQuest(this);
            uQuest.checkData(1);
        }
        return uQuest;
    }

    public DataDaily getDataDaily() {
        return getUserDaily().getUDaily();
    }

    public void sendNotify() {
        Util.sendProtoData(channel, CommonProto.getCommonVectorProto(buildNotifyList()), IAction.NOTIFY);
    }

    public void addNotify(NotifyType notifyType) {
        Util.sendProtoData(channel, CommonProto.getCommonVector(notifyType.value), IAction.ADD_NOTIFY);
    }

    public Pos getCachePos(){
        return cachePos==null? Pos.zero():cachePos;
    }

    /** Lưu vị trí HOME và HP hiện tại khi logout (kể cả đang chết). */
    public void saveLastHomePos() {
        if (user.getBlockType() == BlockType.BLOCK_ACTION) {
            uData.setLastPos("[0,0]");
            uData.update(List.of("last_pos", "[0,0]"));
            return;
        }

        Player player = getPlayer();
        BaseRoom room = player.getRoom();
        if (room == null && channel != null) {
            room = ChUtil.getRoom(channel);
        }
        if (room == null || room.getRoomType() != MapType.HOME) return;

        ResMapEntity map = room.getMapInfo();
        Pos pos = Pos.capPos(
                player.getPos().round(),
                map.getBotLeftP(), map.getTopRightP(),
                BattleConfig.P_Width / 2f
        );
        boolean wasDead = !player.isAlive() || player.getPoint().getCurHP() <= 0;
        String posStr = StringHelper.toDBString(List.of(pos.getX(), pos.getY()));
        uData.setLastPos(posStr);
        uData.setLastDead(wasDead ? 1 : 0);
        uData.update(List.of("last_pos", posStr, "last_dead", uData.getLastDead()));
        cachePointData(player.getPoint());
    }

    /** Ghi HP và chỉ số hiện tại vào point_data để khôi phục khi login lại. */
    public void cachePointData(Point point) {
        if (point == null) return;
        user.setPointData(StringHelper.toDBString(point.getValues()));
    }

    public Pos getLastHomePos() {
        if (user.getBlockType() == BlockType.BLOCK_ACTION) return Pos.zero();
        if (StringHelper.isEmpty(uData.getLastPos())) return Pos.zero();
        try {
            Pos pos = new Pos(uData.getLastPos()).round();
            if (pos.x == 0f && pos.y == 0f) return Pos.zero();
            return pos;
        } catch (Exception e) {
            return Pos.zero();
        }
    }

    public boolean isLastHomeDead() {
        return uData.getLastDead() == 1;
    }

    public void clearLastHomeState() {
        uData.setLastPos("[0,0]");
        uData.setLastDead(0);
        uData.update(List.of("last_pos", "[0,0]", "last_dead", 0));
    }

    public Player getPlayer() {
        if (player == null) {
            player = new Player(this, user.getClan());
        }
        return player;
    }

    public Pet getPet(Player player) {
        List<Integer> pets = user.getPet(this);
        if (pets.get(0) != 0 && pet == null) {
            long petKey = pets.get(0);
            UserPetEntity uPet = resources.getPet(petKey);
            if (uPet == null) uPet = resources.getPetByConfigId((int) petKey);
            if (uPet != null) pet = new Pet(uPet, player);
        }
        return pet;
    }

    public void reCalculatePoint() {
        if (player != null) player.protoStatus(Pbmethod.SubStateType.UPDATE_MULTI_POINT, user.reCalculatePoint(this).toProto());
    }

    public List<FriendChatObject> getChatHistory(int userId) {
        if (!aChatFriends.containsKey(userId)) aChatFriends.put(userId, new ArrayList<>());
        return aChatFriends.get(userId);
    }

    public void setChatHistory(int userId, List<FriendChatObject> chats) {
        aChatFriends.put(userId, chats);
    }

    public void addChatFriend(List<FriendChatObject> newChat, UserChatInfoObject info) {
        List<FriendChatObject> chatHistory = getChatHistory(info.id);
        if (chatHistory.size() == 0) {
            chatHistory.addAll(newChat);
        } else chatHistory.add(newChat.get(newChat.size() - 1));
        Util.sendProtoData(channel, newChat.get(newChat.size() - 1).toProto(info), IAction.CHAT_FRIEND);
    }

    public void addResponse(int service, AbstractMessage... msg) {
        Util.sendProtoData(channel, msg.length > 0 ? msg[0] : null, service);
    }


    public boolean checkSlotAddBonus(List<Long> bonus) {
        List<List<Long>> aBonus = Bonus.parse(bonus);
        int numBag = 0;
        int numEvent = 0;
        int numMaterial = 0;
        for (List<Long> chunk : aBonus) {
            if (chunk.isEmpty()) continue;
            int bonusType = chunk.get(0).intValue();
            if (bonusType == Bonus.BONUS_ITEM) {
                int itemKey = chunk.get(1).intValue();
                Pbmethod.ItemType storageType = Bonus.resolveStorageType(itemKey);
                if (Bonus.usesItemSlotForUserItem(storageType)) {
                    numBag++;
                }
            } else if (bonusType == Bonus.BONUS_ITEM_POINT) {
                int pointId = chunk.get(1).intValue();
                if (chunk.get(2) > 0 && Bonus.usesEventBagPoint(pointId)
                        && resources.getItemPointNumber(pointId) <= 0)
                    numEvent++;
            } else if (bonusType == Bonus.BONUS_EQUIPMENT
                    || bonusType == Bonus.BONUS_PET
                    || bonusType == Bonus.BONUS_MOUNT
                    || bonusType == Bonus.BONUS_MOB
                    || bonusType == Bonus.BONUS_ARTIFACT) {
                numBag++;
            } else if (bonusType == Bonus.BONUS_MATERIAL) {
                numMaterial++;
            } else if (bonusType == Bonus.BONUS_CHANGE_OWNER) {
                int innerType = chunk.get(1).intValue();
                if (innerType == Bonus.BONUS_EQUIPMENT
                        || innerType == Bonus.BONUS_PET
                        || innerType == Bonus.BONUS_MOUNT
                        || innerType == Bonus.BONUS_MOB
                        || innerType == Bonus.BONUS_ARTIFACT)
                    numBag++;
                else if (innerType == Bonus.BONUS_MATERIAL)
                    numMaterial++;
            }
        }
        if (numBag > 0 && !resources.canAddBagItem(numBag)) return false;
        if (numEvent > 0 && !resources.canAddEventItem(numEvent)) return false;
        if (numMaterial > 0 && !resources.canAddMaterial(numMaterial)) return false;
        return true;
    }

    public void addBuffs(List<Long> aBuffs) {
        uData.update(List.of("buff", StringHelper.toDBString(aBuffs)));
        uData.setBuff(StringHelper.toDBString(aBuffs));
        reCalculatePoint();
        UserHandler.buffInfo(this);
    }

    public void userLogout() {
        TreasureEventService.clearKeyOnLogout(this);
        long curTime = System.currentTimeMillis();
        saveLastHomePos();
        cachePointData(getPlayer().getPoint());
        uData.flushItemSlotIfDirty();
        getUser().update(Arrays.asList("logout", Calendar.getInstance().getTime()));
        UserAchievementEntity uAchie = Services.userDAO.getUserAchievement(this);
        if (uAchie != null && uAchie.isCanUpdate()) uAchie.updateAll();
        EntityManager session = DBJPA.getEntityManager();
        try {
            session.getTransaction().begin();
            session.createNativeQuery("update user set logout = now(), point_data='" + StringHelper.toDBString(getPlayer().getPoint().getValues()) + "' where id = " + user.getId()).executeUpdate();
            int timeAdd = (int) ((Calendar.getInstance().getTime().getTime() - getUser().getLastLogin().getTime()) / 1000);
            session.createNativeQuery("update user_daily set login_time =" + timeAdd + "+login_time, data_int= '" + StringHelper.toDBString(getUserDaily().getUDaily().aInt) + "' where user_id = " + user.getId()).executeUpdate();
            session.getTransaction().commit();
        } catch (Exception ex) {
            getLogger().error(GUtil.exToString(ex));
        } finally {
            DBJPA.closeSession(session);
            slowLog(curTime, String.format("SQL doUpdate %s", getClass().getSimpleName()));
        }
    }
}
