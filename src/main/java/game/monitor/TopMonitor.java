package game.monitor;

import com.google.gson.Gson;
import game.cache.JCache;
import game.config.aEnum.RankingType;
import game.config.aEnum.TopType;
import game.dragonhero.mapping.ClanEntity;
import game.dragonhero.mapping.TopUserEntity;
import game.object.MyUser;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.GUtil;
import ozudo.base.helper.GsonUtil;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import javax.persistence.EntityManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TopMonitor {
    final String KEY_DATA = "cache_top_";
    Map<String, Pbmethod.PbListUser> mTopUser = new HashMap<>();
    Map<String, Pbmethod.PbListClan> mTopClan = new HashMap<>();
    Map<String, Pbmethod.PbListUser> mTopTower = new HashMap<>();
    Map<String, Pbmethod.PbListUser> mTopArena = new HashMap<>();
    static TopMonitor instance;

    public static TopMonitor getInstance() {
        if (instance == null) instance = new TopMonitor();
        return instance;
    }

    public Object get(TopType topType, String... keyCache) {
        return get(topType, new TopCacheInfo(topType, keyCache));
    }

    private synchronized Object get(TopType topType, TopCacheInfo cacheInfo) {
        if (topType == TopType.CLAN_POWER ||topType == TopType.CLAN_STAR){// clan
            if (!mTopClan.containsKey(cacheInfo.keyCache)) {
                if (cacheTopClan(topType, cacheInfo)) {
                    return mTopClan.get(cacheInfo.keyCache);
                }
                return Pbmethod.PbListClan.newBuilder().build();
            }
            if (JCache.getInstance().getValue(KEY_DATA + cacheInfo.keyCache) == null) {
                JCache.getInstance().setValue(KEY_DATA + cacheInfo.keyCache, "1", JCache.EXPIRE_1M * 5);
                CompletableFuture.supplyAsync(() -> cacheTopClan(topType, cacheInfo));
            }
            return mTopClan.get(cacheInfo.keyCache);
        } else{
            if (!mTopUser.containsKey(cacheInfo.keyCache)) {
                if (cacheTopUser(topType, cacheInfo)) {
                    return mTopUser.get(cacheInfo.keyCache);
                }
                return Pbmethod.PbListUser.newBuilder().build();
            }
            if (JCache.getInstance().getValue(KEY_DATA + cacheInfo.keyCache) == null) {
                JCache.getInstance().setValue(KEY_DATA + cacheInfo.keyCache, "1", JCache.EXPIRE_1M * 5);
                CompletableFuture.supplyAsync(() -> cacheTopUser(topType, cacheInfo));
            }
            return mTopUser.get(cacheInfo.keyCache);
        }
    }

    boolean cacheTopUser(TopType topType, TopCacheInfo cacheInfo) {
        List<TopUserEntity> aUser = dbGetTop(cacheInfo.sql);
        if (aUser != null && aUser.size() > 0) {
            mTopUser.put(cacheInfo.keyCache, toTopProto(topType, aUser));
            JCache.getInstance().setValue(KEY_DATA + cacheInfo.keyCache, "1", JCache.EXPIRE_1M * 5);
            return true;
        }
        return false;
    }

    boolean cacheTopTower(TopType topType, TopCacheInfo cacheInfo) {
        List<TopUserEntity> aUser = dbGetTop(cacheInfo.sql);

        if (aUser != null && aUser.size() > 0) {
            mTopTower.put(cacheInfo.keyCache, toTopProto(topType, aUser));
            JCache.getInstance().setValue(KEY_DATA + cacheInfo.keyCache, "1", JCache.EXPIRE_1M * 5);
            return true;
        }
        return false;
    }

    boolean cacheTopArena(TopType topType, TopCacheInfo cacheInfo) {
        List<TopUserEntity> aUser = dbGetTop(cacheInfo.sql);
        if (aUser != null && aUser.size() > 0) {
            mTopArena.put(cacheInfo.keyCache, toTopProto(topType, aUser));
            JCache.getInstance().setValue(KEY_DATA + cacheInfo.keyCache, "1", JCache.EXPIRE_1M * 5);
            return true;
        }
        return false;
    }


    boolean cacheTopClan(TopType topType, TopCacheInfo cacheInfo) {
        List<ClanEntity> aClan = dbGetTopClan(cacheInfo.sql);
        if (aClan != null && aClan.size() > 0) {
            mTopClan.put(cacheInfo.keyCache, toProtoTopClan(topType, aClan));
            JCache.getInstance().setValue(KEY_DATA + cacheInfo.keyCache, "1", JCache.EXPIRE_1M * 5);
            return true;
        }
        return false;
    }

    Pbmethod.PbListUser toTopProto(TopType topType, List<TopUserEntity> aUser) {
        Pbmethod.PbListUser.Builder builder = Pbmethod.PbListUser.newBuilder();
        for (int i = 0; i < aUser.size(); i++) {
            builder.addAUser(aUser.get(i).toProto(i + 1, topType));
        }
        return builder.build();
    }

    Pbmethod.PbListClan toProtoTopClan(TopType topType, List<ClanEntity> aClan) {
        Pbmethod.PbListClan.Builder builder = Pbmethod.PbListClan.newBuilder();
        for (int i = 0; i < aClan.size(); i++) {
            builder.addClan(aClan.get(i).toProto(i + 1, topType.value));
        }
        return builder.build();
    }

    List<TopUserEntity> dbGetTop(String sql) {
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            return session.createNativeQuery(sql, TopUserEntity.class).getResultList();
        } catch (Exception ex) {
            ex.printStackTrace();
            Logs.error(GUtil.exToString(ex));
        } finally {
            DBJPA.closeSession(session);
        }
        return null;
    }

    List<ClanEntity> dbGetTopClan(String sql) {
        //System.out.println("sql = " + sql);
        EntityManager session = null;
        try {
            session = DBJPA.getEntityManager();
            return session.createNativeQuery(sql, ClanEntity.class).getResultList();
        } catch (Exception ex) {
            ex.printStackTrace();
            Logs.error(GUtil.exToString(ex));
        } finally {
            DBJPA.closeSession(session);
        }
        return null;
    }

    class TopCacheInfo {
        String sql, keyCache;

        public TopCacheInfo(TopType topType, String... keyCache) {
            this.keyCache = String.format("%s_%s_%s", KEY_DATA, topType.name, new Gson().toJson(keyCache));
            switch (keyCache.length) {
                case 0:
                    this.sql = topType.sql;
                    break;
                case 1:
                    this.sql = String.format(topType.sql, keyCache[0]);
                    break;
                case 2:
                    this.sql = String.format(topType.sql, keyCache[0], keyCache[1]);
                    break;
            }
        }

    }
}
