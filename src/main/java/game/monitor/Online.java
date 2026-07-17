package game.monitor;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import game.cache.JCache;
import game.config.CfgServer;
import game.config.lang.Lang;
import game.protocol.CommonProto;
import game.config.aEnum.BlockType;
import game.treasure.controller.BattleHandler;
import game.treasure.mapping.UserEntity;
import game.treasure.server.IAction;
import game.treasure.table.BaseRoom;
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
            if (mUser != null) {
                game.treasure.service.arena.ArenaService.getInstance().onDisconnect(mUser.getUserId());
                if (room != null && mUser.getPlayer() != null) {
                    room.removeUnit(mUser.getPlayer().getId());
                }
                mUser.userLogout();
            }
            if(mUser!=null) Online.removeChannel(mUser.getUser().getServer(), mUser.getUser().getId());
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

    public static void kickUser(int userId, int blockType) {
        if (blockType != 1 && blockType != 2) return;
        Channel ch = getChannel(userId);
        if (ch == null || !ch.isActive()) return;
        try {
            MyUser mUser = getMUser(ch);
            if (mUser != null && mUser.getUser() != null) {
                mUser.getUser().setBlockType(blockType);
                String username = mUser.getUser().getUsername();
                String name = username.contains("_")
                        ? username.substring(username.indexOf("_") + 1)
                        : username;
                JCache.getInstance().removeValue("s:" + name);
            }
            String kickMsg = Lang.getTitle(CfgServer.config.mainLanguage, Lang.err_user_block);
            if (blockType == 1) {
                Util.sendProtoData(ch, CommonProto.getErrorMsg(kickMsg), IAction.LOGIN_GAME_BLOCK);
            } else {
                Util.sendProtoData(ch, CommonProto.getCommonVector(kickMsg), IAction.DISCONNECT_MSG);
            }
            logoutChannel(ch);
            ch.close();
        } catch (Exception ex) {
            slib_Logger.root().error(Util.exToString(ex));
        }
    }

    public static void unblockUser(int userId) {
        Channel ch = getChannel(userId);
        if (ch == null || !ch.isActive()) return;
        MyUser mUser = getMUser(ch);
        if (mUser == null || mUser.getUser() == null) return;
        if (mUser.getUser().getBlockType() != BlockType.BLOCK_ACTION) return;
        BattleHandler.teleportHomeOnUnblock(mUser, ch);
    }
}
