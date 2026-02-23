package game.object;

import com.google.protobuf.AbstractMessage;
import game.battle.model.Pet;
import game.battle.model.Player;
import game.battle.object.Pos;
import game.battle.object.TeamObject;
import game.battle.type.StateType;
import game.config.*;
import game.config.aEnum.*;
import game.dragonhero.controller.UserHandler;
import game.dragonhero.mapping.*;
import game.dragonhero.mapping.main.ResComboWeaponEntity;
import game.dragonhero.mapping.main.ResTeleportEntity;
import game.dragonhero.server.IAction;
import game.dragonhero.service.Services;
import game.dragonhero.service.resource.ResItem;
import game.dragonhero.service.resource.ResWeapon;
import game.dragonhero.service.user.Bonus;
import game.monitor.ClanManager;
import game.protocol.CommonProto;
import io.netty.channel.Channel;
import lombok.Data;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.GUtil;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.StringHelper;
import ozudo.base.helper.Util;
import protocol.Pbmethod;


import javax.persistence.EntityManager;
import java.io.Serializable;
import java.util.*;

import static game.dragonhero.dao.UserDAO.getLogger;
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
    TeamObject team;
    UserResources resources;
    String version, udid;
    int counterMemcached;
    UserCache cache = new UserCache();
    List<Pbmethod.PbAction> msgNotify = new ArrayList<>();
    List<Long> aBonus = new ArrayList<>();
    Player player;
    Pet pet;
    Channel channel;
    boolean init = true;
    ResTeleportEntity curTeleport;
    Pos cachePos;
    int roomChanelId = 1; // 1-1000
    Map<Integer, List<FriendChatObject>> aChatFriends = new HashMap<>();
    List<Integer> comboWeapon = NumberUtil.genListInt(6, 0);
    List<Integer> cacheSendParty =new  ArrayList<>(); // [userId,timeSend, number]
    List<Integer> perReceiveBoss = List.of(0,0);  // per tăng đá - per tăng drop

    public MyUser(UserEntity user) {
        this.user = user;
    }

    public void setInitUData(UserDataEntity uData, UserEntity user) {
        uData.initData(user);
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
        // afk
        if (CfgAfk.isFullAfkBonus(this)) {
            ret.add((long) NotifyType.AFK_BONUS.value);
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
        // summon free
        UserSummonEntity uSummon = Services.userDAO.getUserSummon(this);
        if (userClan != null && uSummon.getCDSummonFree() <= 0) {
            ret.add((long) NotifyType.SUMMON_FREE.value);
        }
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

    public List<Integer> toListIdDBItemEquip(UserHeroEntity uHero) {
        List<Integer> lst = new ArrayList<>();
        List<Integer> lstIds = uHero.getListIdEquipmentEquip();
        for (int i = 0; i < lstIds.size(); i++) {
            UserItemEquipmentEntity uItem = resources.getItemEquipment(lstIds.get(i));
            if(uItem!=null) {
                lst.add(uItem.getRes().getId());
            }else {
                lst.add(0);
            }
        }
        return lst;
    }

    public void calComboWeapon() {
        for (int i = 0; i < comboWeapon.size(); i++) {
            int curNum = 0;
            List<UserWeaponEntity> weapons = resources.getWeaponsByRank(i + 1);
            ResComboWeaponEntity rCombo = ResWeapon.mComboWeapon.get(i + 1);
            for (int j = 0; j < weapons.size(); j++) {
                curNum += weapons.get(j).getLevel();
            }
            if (curNum >= rCombo.getMaxLevel()) comboWeapon.set(i, 1);
        }
    }

    public UserQuestEntity getUQuest() {
        if (uQuest == null) {
            uQuest = Services.userDAO.getUserQuest(this);
            uQuest.checkData(user.getLevel());
        }
        return uQuest;
    }

    public DataDaily getDataDaily() {
        return getUserDaily().getUDaily();
    }

    public void sendNotify() {
        Util.sendProtoData(channel, CommonProto.getCommonVectorProto(checkNotify()), IAction.NOTIFY);
    }

    public void addNotify(NotifyType notifyType) {
        Util.sendProtoData(channel, CommonProto.getCommonVector(notifyType.value), IAction.ADD_NOTIFY);
    }

    public Player getPlayer() {
        if (player == null) {
            player = new Player(this, 1);
        }
        return player;
    }

    public Pet getPet(Player player) {
        List<Integer> pets = user.getPet(this);
        if (pets.get(0) != 0 && pet == null) {
            pet = new Pet(resources.getPet(PetType.ANIMAL, pets.get(0)), player);
        }
        return pet;
    }

    public void reCalculatePoint() {
        if (player != null) player.protoStatus(StateType.SET_ALL_POINT, user.reCalculatePoint(this).toProto());
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
        int numItem = 0, numItemEquip = 0;
        for (int i = 0; i < aBonus.size(); i++) {
            int itemId = aBonus.get(i).get(1).intValue();
            if (aBonus.get(i).get(0).intValue() == Bonus.BONUS_ITEM && ResItem.getItem(itemId).getShowBag() == 1 &&
                    getResources().getItem(aBonus.get(i).get(1).intValue()) != null)
                numItem++;
            if (aBonus.get(i).get(0).intValue() == Bonus.BONUS_ITEM_EQUIPMENT) numItemEquip++;
        }
        if (numItem > 0) return resources.getNumItemBag() + numItem <= uData.getNumSlotItem();
        if (numItemEquip > 0) return resources.getMItemEquipment().size() + numItemEquip <= uData.getNumSlotItemEquip();
        return true;
    }

    public void addBuffs(List<Long> aBuffs) {
        uData.update(List.of("buff", StringHelper.toDBString(aBuffs)));
        uData.setBuff(aBuffs.toString());
        player.updateBuff();
        UserHandler.buffInfo(this);
    }

    public void userLogout() {
        long curTime = System.currentTimeMillis();
        UserPartyEntity userParty = user.getParty();
        if(userParty != null) userParty.offlineParty(this);
        getUser().update(Arrays.asList("logout", Calendar.getInstance().getTime()));
        UserAchievementEntity uAchie = Services.userDAO.getUserAchievement(this);
        if (uAchie != null && uAchie.isCanUpdate()) uAchie.updateAll();
        EntityManager session = DBJPA.getEntityManager();
        try {
            session.getTransaction().begin();
            session.createNativeQuery("update user set logout = now(), point_data='" + StringHelper.toDBString(player.getPoint().getValues()) + "' where id = " + user.getId()).executeUpdate();
            int timeAdd = (int) ((Calendar.getInstance().getTime().getTime() - getUser().getLastLogin().getTime()) / 1000);
            session.createNativeQuery("update user_daily set login_time =" + timeAdd + "+login_time, data_int= '" + StringHelper.toDBString(getUserDaily().getUDaily().aInt) + "' where user_id = " + user.getId()).executeUpdate();
            session.createNativeQuery("update user_data set campaign ='" + StringHelper.toDBString(getUData().getCampaign()) + "'where user_id = " + user.getId()).executeUpdate();
            // check hero point
            UserArenaEntity uArena = Services.userDAO.getUserArena(this);
            if (uArena != null && uArena.isActive()) {
                session.createNativeQuery("update user_arena set defense_team ='" + StringHelper.toDBString(uArena.getDefTeam()) + "'where user_id = " + user.getId()).executeUpdate();
            }
            session.getTransaction().commit();
        } catch (Exception ex) {
            getLogger().error(GUtil.exToString(ex));
        } finally {
            DBJPA.closeSession(session);
            slowLog(curTime, String.format("SQL doUpdate %s", getClass().getSimpleName()));
        }
    }
}
