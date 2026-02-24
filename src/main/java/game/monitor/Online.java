package game.monitor;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import game.cache.JCache;
import game.config.CfgClan;
import game.dragonhero.mapping.UserArenaEntity;
import game.dragonhero.mapping.UserEntity;
import game.dragonhero.table.BaseRoom;
import game.dragonhero.table.StandaloneMoveRoom;
import game.object.MyUser;
import io.netty.channel.Channel;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.ChUtil;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.Util;
import ozudo.base.log.Logs;
import ozudo.base.log.slib_Logger;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class Online {
    public static Map<Integer, Channel> mChannel = new HashMap<>();
    public static Map<Integer, List<Channel>> userServer = new HashMap<>();
    static LoadingCache<Integer, UserEntity> cacheDbUser = CacheBuilder.newBuilder().maximumSize(10000).expireAfterAccess(30, TimeUnit.MINUTES).build(new CacheLoader<>() {
        @Override
        public UserEntity load(Integer k) {
            return (UserEntity) DBJPA.getUnique("user", UserEntity.class, "id", k);
        }
    });

    static LoadingCache<Integer, UserArenaEntity> cacheDbUserArena = CacheBuilder.newBuilder().maximumSize(1000).expireAfterAccess(10, TimeUnit.MINUTES).build(new CacheLoader<Integer, UserArenaEntity>() {
        @Override
        public UserArenaEntity load(Integer id) {
            return (UserArenaEntity) DBJPA.getUnique("user_arena", UserArenaEntity.class, "user_id", id);
        }
    });

    public static int getCCU() {
        return mChannel.size();
    }

    public static void addChannel(UserEntity user, Channel channel) {
//        synchronized (lockOnline) {
        mChannel.put(user.getId(), channel);
        if (!userServer.containsKey(user.getServer())) {
            userServer.put(user.getServer(), new ArrayList<>());
        }
        userServer.get(user.getServer()).add(channel);
        cacheDbUser.invalidate(user.getId());
        cacheDbUser.put(user.getId(), user);
        //}
    }

    public static UserEntity getDbUser(int userId) {
        try {
            return cacheDbUser.get(userId);
        } catch (Exception ex) {
        }
        return null;
    }

    public static UserArenaEntity getDbUserArena(int userId) {
        try {
            return cacheDbUserArena.get(userId);
        } catch (Exception ex) {
        }
        return null;
    }

    public static void cacheUserArena(UserArenaEntity user) {
        cacheDbUserArena.put(user.getUserId(), user);
    }


    public static List<Channel> getAllChanel() {
        return new ArrayList<>(mChannel.values());
    }

    public static void removeChannel(int server, int userId) {
//        synchronized (lockOnline) {
        userServer.get(server).remove(mChannel.get(userId));
        mChannel.remove(userId);
//        }
    }

    public static Channel getChannel(int userId) {
        return mChannel.get(userId);
    }


    public static void closeAllChannel() {
        try {
            List<Channel> channels = getAllChanel();
            userServer.clear();
            for (int i = 0; i < channels.size(); i++) {
                channels.get(i).close();
            }
        } catch (Exception ex) {
            Logs.error(ex.getMessage());
        }
    }

    public static List<Channel> getUserInServer(int server) {
        List<Channel> lst = new ArrayList<>();
        List<Channel> lstRs = userServer.get(server);
        if(lstRs==null)  return lst;
        for (int i = 0; i < lstRs.size(); i++) {
            if (!lst.contains(lstRs.get(i))) lst.add(lstRs.get(i));
        }
        return lst;
    }

    public static boolean isOnline(Integer userId) {
        return mChannel.containsKey(userId);
    }

    public static List<Integer> getUserChannelInfo(int userId) {
        MyUser user = getMUser(userId);
        if (user != null && user.getPlayer() != null && user.getPlayer().getRoom() != null) {
            return Arrays.asList(1, user.getPlayer().getRoom().getChannelId());
        }
        return NumberUtil.genListInt(2, 0);
    }

    public static MyUser getMUser(int userId) {
        Channel channel = getChannel(userId);
        return channel == null ? null : ChUtil.getMUser(channel);
    }

    public static MyUser getMUser(Channel channel) {
        return ChUtil.getMUser(channel);
    }

    public static String getRealUsername(String username) {
        if (username.contains("_")) return username.substring(username.indexOf("_") + 1);
        return username;
    }


    public static int getServer(String username) {
        if (username.contains("_")) return Integer.parseInt(username.substring(0, username.indexOf("_")));
        return 1;
    }


    public static String getSession(String userName) {
        long curTime = System.currentTimeMillis();
        String value = JCache.getInstance().getValue("s:" + userName);
        long timePass = System.currentTimeMillis() - curTime;
        if (timePass >= 1000) {
            Logs.slow(String.format("%s -> %s", "JCACHE session", timePass));
        }
        return value;
    }

    public static void logoutChannel(Channel channel) {
        try {
            MyUser mUser = ChUtil.getMUser(channel);
            BaseRoom room = ChUtil.getRoom(channel);
            if (room != null && mUser != null) {
                room.removePlayer(mUser.getPlayer().getId());
                mUser.userLogout();
            }
            if(mUser!=null) Online.removeChannel(mUser.getUser().getServer(), mUser.getUser().getId());

            Object roomObj = ChUtil.get(channel, ChUtil.KEY_ROOM);
            if (roomObj instanceof StandaloneMoveRoom) {
                ((StandaloneMoveRoom) roomObj).removeChannel(channel);
            }
            ChUtil.remove(channel, ChUtil.KEY_ROOM);
            ChUtil.remove(channel, ChUtil.KEY_M_USER);
        } catch (Exception ex) {
            slib_Logger.root().error(Util.exToString(ex));
        }
    }

    public static List<Channel> getListChannel(List<Integer> ids) {
        List<Channel> lstChanel = new ArrayList<>();
        for (Integer i : ids) {
            MyUser iUser = Online.getMUser(i);
            if (iUser != null) lstChanel.add(iUser.getChannel());

        }
        return lstChanel;
    }
}
