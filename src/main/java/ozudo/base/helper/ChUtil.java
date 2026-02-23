package ozudo.base.helper;

import game.dragonhero.table.BaseRoom;
import game.object.MyUser;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

public class ChUtil {
    public static final String KEY_ROOM = "room";
    public static final String KEY_IDLE = "idle";
    public static final String KEY_M_USER = "muser";
    public static final String KEY_SESSION = "KEY_SESSION_1";

    public static Integer getInteger(Channel channel, String key) {
        try {
            return (Integer) channel.attr(AttributeKey.valueOf(key)).get();
        } catch (Exception ex) {
        }
        return null;
    }

    public static Object get(Channel channel, String key) {
        return channel.attr(AttributeKey.valueOf(key)).get();
    }

    public static void set(Channel channel, String key, Object value) {
        channel.attr(AttributeKey.valueOf(key)).set(value);
    }

    public static void remove(Channel channel, String key) {
        if (channel != null) channel.attr(AttributeKey.valueOf(key)).remove();
    }

    public static void setMUser(Channel channel, MyUser myUser) {
        if (channel != null) {
            ChUtil.set(channel, KEY_M_USER, myUser);
        }
    }

    public static MyUser getMUser(Channel channel) {
        if (channel != null) return (MyUser) ChUtil.get(channel, KEY_M_USER);
        return null;
    }

    public static String getSession(Channel channel) {
        if (channel != null) return (String) ChUtil.get(channel, KEY_SESSION);
        return null;
    }

    public static BaseRoom getRoom(Channel channel) {
        if (channel != null) return (BaseRoom) ChUtil.get(channel, KEY_ROOM);
        return null;
    }
}
