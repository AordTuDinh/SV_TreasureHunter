package game.dragonhero.dao;

import game.cache.JCache;
import game.config.CfgClan;
import game.config.aEnum.ClanPosition;
import game.dragonhero.controller.ClanHandler;
import game.dragonhero.mapping.ClanEntity;
import game.dragonhero.mapping.ClanReqEntity;
import game.dragonhero.mapping.UserClanEntity;
import game.dragonhero.mapping.UserEntity;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.GUtil;
import ozudo.base.helper.StringHelper;
import ozudo.base.log.Logs;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

import static ozudo.base.database.DBJPA.closeSession;
import static ozudo.base.helper.Filer.getLogger;

public class ClanDAO extends AbstractDAO {
    public List<UserClanEntity> getTopDonate(int clanId) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            return session.createNativeQuery("select c.* from user u, user_clan c where u.clan=" + clanId + " and u.id=c.user_id order by mill_donate desc", UserClanEntity.class).getResultList();
        } catch (Exception ex) {
            ex.printStackTrace();
            getLogger().error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return null;
    }

    public ClanEntity getClan(String name) {
        return (ClanEntity) DBJPA.getUnique("clan", ClanEntity.class, "name", name);
    }


    public ClanEntity getClan(int clanId) {
        return (ClanEntity) DBJPA.getUnique("clan", ClanEntity.class, "id", clanId);
    }

    public int createClan(UserEntity user, String clanName, int gem, String status, int avatar,int joinRule,int level) {
        EntityManager session = DBJPA.getEntityManager();
        try {
            session.getTransaction().begin();
            ClanEntity clan = new ClanEntity(user, status, avatar, clanName,joinRule,level);
            clan.setServer(user.getServer());
            session.persist(clan);
            Query query = session.createNativeQuery("update user set gem=gem-" + gem + ", clan=" + clan.getId() + ",clan_avatar=:clanAvatar, clan_position=:clanPosition, clan_name=:clanName where id=" + user.getId());
            query.setParameter("clanName", clanName);
            query.setParameter("clanAvatar", avatar);
            query.setParameter("clanPosition", ClanPosition.LEADER.value);
            query.executeUpdate();

            session.getTransaction().commit();
            return clan.getId();
        } catch (Exception ex) {
            ex.printStackTrace();
            getLogger().error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return -1;
    }

    private boolean dbUpdateName(int clanId, String name) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            session.getTransaction().begin();
            session.createNativeQuery("update clan set name=:name where id=" + clanId).setParameter("name", name).executeUpdate();
            session.createNativeQuery("update user set clan_name=:name where clan=" + clanId).setParameter("name", name).executeUpdate();
            session.getTransaction().commit();
            return true;
        } catch (Exception ex) {
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return false;
    }

    public List<ClanEntity> topClan(int server) {
        return DBJPA.getEntityManager().createQuery("select c from ClanEntity c where c.server=:server order by rank desc limit 0,100", ClanEntity.class).
                setParameter("server", server).getResultList();
    }

    public List<ClanReqEntity> getClanReq(int clanId) {
        return DBJPA.getEntityManager().createQuery("select c from ClanReqEntity c where c.clanId=:clanId", ClanReqEntity.class)
                .setParameter("clanId", clanId).getResultList();
    }

    public boolean destroyClan(int clanId) {
        return doUpdate(em -> {
            em.createNativeQuery("update user set clan=0, clan_name='', clan_position='' where clan=" + clanId).executeUpdate();
            return em.createNativeQuery("delete from clan where id=" + clanId).executeUpdate() == 1;
        });
    }

    public List<UserEntity> getListMember(int clanId) {
        return DBJPA.getEntityManager().createNativeQuery("select * from user where clan=:clanId order by level desc", UserEntity.class)
                .setParameter("clanId", clanId).getResultList();
    }

    public List<ClanReqEntity> getListUserReq(int userId) {
        return doQuery(em -> em.createQuery("select c from ClanReqEntity c where c.userId=:userId", ClanReqEntity.class)
                .setParameter("userId", userId).getResultList());
    }

    public boolean addNewMember(ClanEntity clan, int userId, List<Integer> memberIds) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            session.getTransaction().begin();
            Query query = session.createNativeQuery("update clan set member_id=:member_id where id=:id");
            query.setParameter("member_id", StringHelper.toDBString(memberIds));
            query.setParameter("id", clan.getId());
            query.executeUpdate();
            Query query2 = session.createNativeQuery("delete from clan_req where clan_id=:clan_id and user_id=:user_id");
            query2.setParameter("clan_id", clan.getId());
            query2.setParameter("user_id", userId);
            query2.executeUpdate();
            Query tmp = session.createNativeQuery("update user set clan=" + clan.getId() + ",clan_avatar=:clanAvatar, clan_name=:clanName, clan_position=0, clan_join=now(), clan_position=0 where id=" + userId);
            tmp.setParameter("clanName", clan.getName());
            tmp.setParameter("clanAvatar", clan.getAvatar());
            tmp.executeUpdate();
            session.getTransaction().commit();
            return true;
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            closeSession(session);
        }
        return false;
    }

    public boolean promote(long userId, int myPosition, UserEntity promoteUser, int newPosition) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            session.getTransaction().begin();
            session.createNativeQuery("update user set clan_position=" + newPosition + " where id=" + promoteUser.getId()).executeUpdate();
            if (myPosition != -1) {
                session.createNativeQuery("update user set clan_position=" + myPosition + " where id=" + userId).executeUpdate();
            }
            if (ClanPosition.isLeader(newPosition)) {
                Query query = session.createNativeQuery("update clan set master=:master, master_id=:masterId where id=:id");
                query.setParameter("id", promoteUser.getClan());
                query.setParameter("masterId", promoteUser.getId());
                query.setParameter("master", promoteUser.getName());
                query.executeUpdate();
            }
            session.getTransaction().commit();
            return true;
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            closeSession(session);
        }
        return false;
    }

    public boolean updateClanJoinRule(int clanId, int rule, int trophy) {
        return DBJPA.update("update clan set join_rule=" + rule + ",join_trophy=" + trophy + " where id=" + clanId);
    }

    public List<ClanEntity> suggestClan(int server) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            Query query = session.createNativeQuery(String.format("select * from clan where server=%s AND member>0  ORDER BY RAND() limit 5", server, CfgClan.config.maxMember), ClanEntity.class);
            return query.getResultList();
        } catch (Exception ex) {
            ex.printStackTrace();
            getLogger().error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return null;
    }

    public List<ClanEntity> findClan(int server, String clanName) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            Query query = session.createNativeQuery("select * from clan where server=" + server + " and name like :likedName", ClanEntity.class);
            query.setParameter("likedName", "%" + clanName + "%");
            return query.getResultList();
        } catch (Exception ex) {
            ex.printStackTrace();
            getLogger().error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return null;
    }

    public int removeMember(ClanEntity clan, UserEntity user, boolean isKick) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            session.getTransaction().begin();
            session.createNativeQuery("update user set clan=0,clan_avatar=0, clan_name='', clan_position=0 where id=" + user.getId()).executeUpdate();
            session.createNativeQuery("update user_clan set contribute=0 where user_id=" + user.getId()).executeUpdate();
            session.getTransaction().commit();
            user.setClan(0);
            user.setClanName("");
            user.setClanPosition(0);
            user.setClanAvatar(0);
            // doi lai tat ca nguoi choi phai cho 2 ngay moi duoc join clan moi
            String key = ClanHandler.KEY_CLAN_LEAVE + user.getId();
            JCache.getInstance().setValue(key, System.currentTimeMillis() + "", JCache.EXPIRE_1H * 12);

            return 0;
        } catch (Exception ex) {
            ex.printStackTrace();
            getLogger().error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return -1;
    }

    public boolean updateClanName(int clanId, String name) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            session.getTransaction().begin();
            session.createNativeQuery("update clan set name=:name where id=" + clanId).setParameter("name", name).executeUpdate();
            session.createNativeQuery("update user set clan_name=:name where clan=" + clanId).setParameter("name", name).executeUpdate();
            session.getTransaction().commit();
            return true;
        } catch (Exception ex) {
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return false;
    }

    public boolean updateClanJoinRule(int clanId, int rule) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            session.getTransaction().begin();
            session.createNativeQuery("update clan set join_rule=:rule where id=" + clanId).setParameter("rule", rule).executeUpdate();
            session.getTransaction().commit();
            return true;
        } catch (Exception ex) {
            Logs.error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return false;
    }



    public int acceptMemberReq(ClanEntity clan, UserEntity user) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            session.getTransaction().begin();

            Query tmp = session.createNativeQuery("update user set clan=" + clan.getId() + ",clan_avatar=:clanAvatar, clan_name=:clanName, clan_position=0, clan_join=now(), clan_position=0 where id=" + user.getId());
            tmp.setParameter("clanName", clan.getName());
            tmp.setParameter("clanAvatar", clan.getAvatar());
            tmp.executeUpdate();
            session.getTransaction().commit();
            return 0;
        } catch (Exception ex) {
            ex.printStackTrace();
            getLogger().error(GUtil.exToString(ex));
        } finally {
            closeSession(session);
        }
        return -1;
    }
}
