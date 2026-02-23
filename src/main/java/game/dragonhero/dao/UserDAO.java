package game.dragonhero.dao;

import game.cache.CacheStoreBeans;
import game.config.CfgChat;
import game.dragonhero.controller.UserEventTopEntity;
import game.dragonhero.mapping.*;
import game.dragonhero.service.Services;
import game.monitor.Online;
import game.object.MyUser;
import org.slf4j.Logger;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.GUtil;
import ozudo.base.log.Logs;
import ozudo.base.log.slib_Logger;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ozudo.base.database.DBJPA.closeSession;
import static ozudo.base.database.DBJPA.getEntityManager;

public class UserDAO {
    public UserEntity getFirstUser() {
        EntityManager session = null;
        try {
            session = getEntityManager();
            List<UserEntity> list = session.createNativeQuery("select * from user limit 1", UserEntity.class).getResultList();
            return list.get(0);
        } catch (Exception ex) {
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return null;
    }

    public UserMarketEntity getUserMarket(MyUser mUser) {
        UserMarketEntity market = (UserMarketEntity) mUser.getCache().get("user_market");
        if (market == null) {
            market = dbGetUserMarket(mUser.getUser().getId());
            if (market == null || !market.getFileData()) {
                return null;
            }
            market.checkData();
            mUser.getCache().set("user_market", market);
        }
        return market;
    }

    public List<UserEventCloEntity> getListEventTimer(MyUser mUser) {
        Object data = mUser.getCache().get("list_event_timer");
        List<UserEventCloEntity> uEvents = null;
        if (data != null) uEvents = (List<UserEventCloEntity>) data;
        if (uEvents == null) {
            uEvents = DBJPA.getList("user_event_clo", List.of("user_id", mUser.getUser().getId()), "", UserEventCloEntity.class);
            if (uEvents == null) {
                return null;
            }
            mUser.getCache().set("list_event_timer", uEvents);
        }
        if (uEvents != null) {
            boolean update = false;
            for (int i = 0; i < uEvents.size(); i++) {
                if (!uEvents.get(i).isAlive()) {
                    uEvents.remove(uEvents.get(i));
                    update = true;
                }
            }
            if (update) mUser.getCache().set("list_event_timer", uEvents);
        }
        return uEvents;

    }

    private UserMarketEntity dbGetUserMarket(int userId) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            List<UserMarketEntity> aMarket = session.createNativeQuery("select * from user_market where user_id=" + userId, UserMarketEntity.class).getResultList();
            if (aMarket == null || aMarket.isEmpty()) {
                UserMarketEntity market = new UserMarketEntity(userId);
                session.getTransaction().begin();
                session.persist(market);
                session.getTransaction().commit();
                return market;
            }
            return aMarket.get(0);
        } catch (Exception ex) {
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return null;
    }

    public List<UserGiftEntity> getUserSendGift(MyUser mUser) {
        // cache 1p reload 1 lần
        Integer number = CacheStoreBeans.cache1Min.get(mUser.getUser().getId() + "_user_send_gift");
        List<UserGiftEntity> gift = (List<UserGiftEntity>) mUser.getCache().get("user_send_gift");
        if (number == null || gift == null) {
            CacheStoreBeans.cache1Min.add(mUser.getUser().getId() + "_user_send_gift", 1);
            gift = dbGetUserSendGift(mUser.getUser().getId());
            if (gift != null) {
                mUser.getCache().set("user_send_gift", gift);
            }
        }
        return gift;
    }

    List<UserGiftEntity> dbGetUserSendGift(int userId) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            return session.createNativeQuery("select * from user_gift where user_id=" + userId + " and time_send=" + DateTime.getDateyyyyMMdd(), UserGiftEntity.class).getResultList();
        } catch (Exception ex) {
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return null;
    }



    public UserClanEntity getUserClan(MyUser mUser) {
        UserClanEntity clan = (UserClanEntity) mUser.getCache().get("user_clan");
        if (clan == null) {
            clan = Services.userDAO.dbGetUserClan(mUser.getUser().getId(), mUser.getUser().getClan(), mUser.getUser().getServer());
            if (clan != null) {
                mUser.getCache().set("user_clan", clan);
            }
        }
        return clan;
    }

    private UserClanEntity dbGetUserClan(int userId, int clanId, int server) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            List<UserClanEntity> aUserClan = session.createNativeQuery("select * from user_clan where user_id=" + userId, UserClanEntity.class).getResultList();
            if (aUserClan.isEmpty()) {
                UserClanEntity uClan = new UserClanEntity(userId, clanId, server);
                session.getTransaction().begin();
                session.persist(uClan);
                session.getTransaction().commit();
                return uClan;
            }
            return aUserClan.get(0);
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            closeSession(session);
        }
        return null;
    }

    public UserArenaEntity getUserArena(MyUser mUser) {
        UserArenaEntity uArena = (UserArenaEntity) mUser.getCache().get("user_arena");
        if (uArena == null) {
            uArena = dbGetUserArena(mUser.getUser());
            if (uArena != null) {
                Online.cacheUserArena(uArena);
                mUser.getCache().set("user_arena", uArena);
            }
        }
        return uArena;
    }

    private UserArenaEntity dbGetUserArena(UserEntity user) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            List<UserArenaEntity> uArenas = session.createNativeQuery("select * from user_arena where user_id=" + user.getId(), UserArenaEntity.class).getResultList();
            if (uArenas.isEmpty()) {
                session = DBJPA.getEntityManager();
                UserArenaEntity uArena = new UserArenaEntity(user);
                session.getTransaction().begin();
                session.persist(uArena);
                session.getTransaction().commit();
                return uArena;
            }
            return uArenas.get(0);
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            closeSession(session);
        }
        return null;
    }


    public UserEventTopEntity getUserEventTop(MyUser mUser,int eventType) {
        UserEventTopEntity uTop = (UserEventTopEntity) mUser.getCache().get("user_event_top_"+eventType);
        if (uTop == null) {
            uTop = dbGetUserEventTop(mUser.getUser(), eventType);
            if (uTop != null) {
                mUser.getCache().set("user_event_top_"+eventType, uTop);
            }
        }
        return uTop;
    }

    private UserEventTopEntity dbGetUserEventTop(UserEntity user,int eventType) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            List<UserEventTopEntity> uETop = session.createNativeQuery("select * from user_event_top where user_id=" + user.getId() +" and event_type=" +eventType, UserEventTopEntity.class).getResultList();
            if (uETop.isEmpty()) {
                session = DBJPA.getEntityManager();
                UserEventTopEntity uArena = new UserEventTopEntity(user,eventType);
                session.getTransaction().begin();
                session.persist(uArena);
                session.getTransaction().commit();
                return uArena;
            }
            return uETop.get(0);
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            closeSession(session);
        }
        return null;
    }

    public UserAfkEntity getUserAfk(MyUser mUser) {
        UserAfkEntity uAfk = (UserAfkEntity) mUser.getCache().get("user_afk");
        if (uAfk == null) {
            uAfk = dbGetUserAfk(mUser.getUser().getId());
            if (uAfk != null) {
                mUser.getCache().set("user_afk", uAfk);
            }
        }
        return uAfk;
    }

    private UserAfkEntity dbGetUserAfk(int userId) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            List<UserAfkEntity> aUserAfk = session.createNativeQuery("select * from user_afk where user_id=" + userId, UserAfkEntity.class).getResultList();
            if (aUserAfk.isEmpty()) {
                UserAfkEntity uAfk = new UserAfkEntity(userId);
                session.getTransaction().begin();
                session.persist(uAfk);
                session.getTransaction().commit();
                return uAfk;
            }
            return aUserAfk.get(0);
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            closeSession(session);
        }
        return null;
    }

    public UserEventSevenDayEntity getUserSevenDay(MyUser mUser) {
        UserEventSevenDayEntity uEvent = (UserEventSevenDayEntity) mUser.getCache().get("user_event_seven_day");
        if (uEvent == null) {
            uEvent = dbGetUserEvent7Day(mUser);
            if (uEvent != null) {
                mUser.getCache().set("user_event_seven_day", uEvent);
            }
        }
        return uEvent;
    }


    private UserEventSevenDayEntity dbGetUserEvent7Day(MyUser mUser) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            List<UserEventSevenDayEntity> aUser = session.createNativeQuery("select * from user_event_seven_day where user_id=" + mUser.getUser().getId(), UserEventSevenDayEntity.class).getResultList();
            if (aUser.isEmpty()) {
                UserEventSevenDayEntity uEvent = new UserEventSevenDayEntity(mUser);
                session.getTransaction().begin();
                session.persist(uEvent);
                session.getTransaction().commit();
                return uEvent;
            }
            return aUser.get(0);
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            closeSession(session);
        }
        return null;
    }

    public UserEventPanelMonthEntity getUserEventMonth(MyUser mUser, int eventId) {
        UserEventPanelMonthEntity uAfk = (UserEventPanelMonthEntity) mUser.getCache().get("user_event_month_" + eventId);
        if (uAfk == null) {
            uAfk = dbGetUserEventMonth(mUser.getUser().getId(), eventId);
            if (uAfk != null) {
                mUser.getCache().set("user_event_month_" + eventId, uAfk);
            }
        }
        return uAfk;
    }

    private UserEventPanelMonthEntity dbGetUserEventMonth(int userId, int eventId) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            List<UserEventPanelMonthEntity> aUserAfk = session.createNativeQuery("select * from user_event_panel_month where user_id=" + userId + " and event_id=" + eventId, UserEventPanelMonthEntity.class).getResultList();
            if (aUserAfk.isEmpty()) {
                UserEventPanelMonthEntity uAfk = new UserEventPanelMonthEntity(userId, eventId);
                session.getTransaction().begin();
                session.persist(uAfk);
                session.getTransaction().commit();
                return uAfk;
            }
            return aUserAfk.get(0);
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            closeSession(session);
        }
        return null;
    }


    public UserEntity getUser(int userId) {
        return (UserEntity) DBJPA.getUnique("user", UserEntity.class, "id", userId);
    }

    public UserEntity getUser(String username) {
        return (UserEntity) DBJPA.getUnique("user", UserEntity.class, "username", username);
    }

    public List<UserEntity> getListUser(List<Integer> aUserId) {
        return getListUser(aUserId.stream().map(value -> String.valueOf(value)).collect(Collectors.joining(",")));
    }


    public List<UserEntity> getListUser(String ids) {
        if (ids.length() == 0) return new ArrayList<>();
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            return session.createNativeQuery("select * from user where id in (" + ids + ")", UserEntity.class).getResultList();
        } catch (Exception ex) {
            ex.printStackTrace();
            getLogger().error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return new ArrayList<>();
    }

    public UserAchievementEntity getUserAchievement(MyUser mUser) {
        UserAchievementEntity achi = (UserAchievementEntity) mUser.getCache().get("user_achievement");
        if (achi == null) {
            achi = Services.userDAO.getUserAchievement(mUser.getUser().getId());
            if (achi != null) {
                mUser.getCache().set("user_achievement", achi);
                achi.checkData();
            }
        }
        return achi;
    }


    private UserAchievementEntity getUserAchievement(int userId) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            Query query = session.createNativeQuery("select * from user_achievement where user_id =:user_id", UserAchievementEntity.class);
            query.setParameter("user_id", userId);
            List<UserAchievementEntity> aUserSum = query.getResultList();
            if (aUserSum.isEmpty()) {
                UserAchievementEntity userS = new UserAchievementEntity(userId);
                session.getTransaction().begin();
                session.persist(userS);
                session.getTransaction().commit();
                return userS;
            }
            return aUserSum.get(0);
        } catch (Exception ex) {
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return null;
    }


    public UserSummonEntity getUserSummon(MyUser mUser) {
        UserSummonEntity summon = (UserSummonEntity) mUser.getCache().get("user_summon");
        if (summon == null) {
            summon = getUserSummon(mUser.getUser().getId());
            if (summon != null) mUser.getCache().set("user_summon", summon);
        }
        return summon;
    }


    private UserSummonEntity getUserSummon(int userId) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            Query query = session.createNativeQuery("select * from user_summon where user_id =:user_id", UserSummonEntity.class);
            query.setParameter("user_id", userId);
            List<UserSummonEntity> aUserSum = query.getResultList();
            if (aUserSum.isEmpty()) {
                UserSummonEntity userS = new UserSummonEntity(userId);
                session.getTransaction().begin();
                session.persist(userS);
                session.getTransaction().commit();
                return userS;
            }
            return aUserSum.get(0);
        } catch (Exception ex) {
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return null;
    }

    public UserLuckySpineEntity getUserSpine(MyUser mUser) {
        UserLuckySpineEntity userSpine = (UserLuckySpineEntity) mUser.getCache().get("user_spine");
        if (userSpine == null) {
            userSpine = getUserSpine(mUser.getUser().getId());
            if (userSpine != null) mUser.getCache().set("user_spine", userSpine);
        }

        return userSpine;
    }

    private UserLuckySpineEntity getUserSpine(int userId) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            Query query = session.createNativeQuery("select * from user_lucky_spine where user_id =:user_id", UserLuckySpineEntity.class);
            query.setParameter("user_id", userId);
            List<UserLuckySpineEntity> aUser = query.getResultList();
            if (aUser.isEmpty()) {
                UserLuckySpineEntity userS = new UserLuckySpineEntity(userId);
                session.getTransaction().begin();
                session.persist(userS);
                session.getTransaction().commit();
                return userS;
            }
            return aUser.get(0);
        } catch (Exception ex) {
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return null;
    }

    public UserTowerEntity getUserTower(MyUser mUser) {
        UserTowerEntity userTower = (UserTowerEntity) mUser.getCache().get("user_tower");
        if (userTower == null) {
            userTower = getUserTower(mUser.getUser());
            if (userTower != null) mUser.getCache().set("user_tower", userTower);
        }
        return userTower;
    }

    private UserTowerEntity getUserTower(UserEntity user) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            Query query = session.createNativeQuery("select * from user_tower where user_id =:user_id", UserTowerEntity.class);
            query.setParameter("user_id", user.getId());
            List<UserTowerEntity> aUser = query.getResultList();
            if (aUser.isEmpty()) {
                UserTowerEntity userS = new UserTowerEntity(user);
                session.getTransaction().begin();
                session.persist(userS);
                session.getTransaction().commit();
                return userS;
            }
            return aUser.get(0);
        } catch (Exception ex) {
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return null;
    }

    public UserDailyEntity getUserDaily(MyUser mUser) {
        UserDailyEntity userDaily = (UserDailyEntity) mUser.getCache().get("user_daily");
        if (userDaily == null) {
            userDaily = getUserDaily(mUser.getUser().getId(), mUser.getUser().getLevel());
            if (userDaily != null) mUser.getCache().set("user_daily", userDaily);
        }
        if(userDaily!=null) userDaily.checkData();
        return userDaily;
    }


    private UserDailyEntity getUserDaily(int userId, int level) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            Query query = session.createNativeQuery("select * from user_daily where user_id =:user_id", UserDailyEntity.class);
            query.setParameter("user_id", userId);
            List<UserDailyEntity> aUserSum = query.getResultList();
            if (aUserSum.isEmpty()) {
                UserDailyEntity userS = new UserDailyEntity(userId, level);
                session.getTransaction().begin();
                session.persist(userS);
                session.getTransaction().commit();
                return userS;
            }
            return aUserSum.get(0);
        } catch (Exception ex) {
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return null;
    }


    public UserWeekEntity getUserWeek(MyUser mUser) {
        UserWeekEntity userWeek = (UserWeekEntity) mUser.getCache().get("user_week");
        if (userWeek == null) {
            userWeek = getUserWeek(mUser.getUser().getId(), mUser.getUser().getServer());
            if (userWeek != null) mUser.getCache().set("user_week", userWeek);
        }
        if(userWeek!=null) userWeek.checkData();
        return userWeek;
    }


    private UserWeekEntity getUserWeek(int userId, int server) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            Query query = session.createNativeQuery("select * from user_week where user_id =:user_id", UserWeekEntity.class);
            query.setParameter("user_id", userId);
            List<UserWeekEntity> aUserSum = query.getResultList();
            if (aUserSum.isEmpty()) {
                UserWeekEntity userS = new UserWeekEntity(userId, server);
                session.getTransaction().begin();
                session.persist(userS);
                session.getTransaction().commit();
                return userS;
            }
            return aUserSum.get(0);
        } catch (Exception ex) {
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return null;
    }

    public UserQuestEntity getUserQuest(MyUser mUser) {
        UserQuestEntity userQuest = (UserQuestEntity) mUser.getCache().get("user_quest");
        if (userQuest == null) {
            userQuest = getUserQuest(mUser.getUser().getId(), mUser.getUser().getLevel());
            if (userQuest != null) mUser.getCache().set("user_quest", userQuest);
        }
        return userQuest;
    }


    private UserQuestEntity getUserQuest(int userId, int level) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            Query query = session.createNativeQuery("select * from user_quest where user_id =:user_id", UserQuestEntity.class);
            query.setParameter("user_id", userId);
            List<UserQuestEntity> aUserWeek = query.getResultList();
            if (aUserWeek.isEmpty()) {
                UserQuestEntity userS = new UserQuestEntity(userId, level);
                session.getTransaction().begin();
                session.persist(userS);
                session.getTransaction().commit();
                return userS;
            }
            return aUserWeek.get(0);
        } catch (Exception ex) {
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return null;
    }


    public UserChatEntity getUserChat(MyUser mUser, int targetId) {
        String keyCache = CfgChat.getKeyChatFriend(mUser.getUser().getId(), targetId);
        UserChatEntity userChat = (UserChatEntity) mUser.getCache().get("friend_chat_" + keyCache);
        if (userChat == null) {
            userChat = getUserChat(mUser.getUser().getId(), targetId);
            if (userChat != null) mUser.getCache().set("friend_chat_" + keyCache, userChat);
        }
        return userChat;
    }


    private UserChatEntity getUserChat(int userId, int targetId) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            int id1 = 0, id2 = 0;
            if (userId < targetId) {
                id1 = userId;
                id2 = targetId;
            } else {
                id1 = targetId;
                id2 = userId;
            }
            Query query = session.createNativeQuery("select * from user_chat where user_id1 =:id1 and user_id2 =:id2", UserChatEntity.class);
            query.setParameter("id1", id1);
            query.setParameter("id2", id2);
            List<UserChatEntity> uChat = query.getResultList();
            if (uChat.isEmpty()) {
                UserChatEntity userS = new UserChatEntity(userId, targetId);
                session.getTransaction().begin();
                session.persist(userS);
                session.getTransaction().commit();
                return userS;
            }
            return uChat.get(0);
        } catch (Exception ex) {
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return null;
    }

    //endregion

    public static Logger getLogger() {
        return slib_Logger.root();
    }
}
