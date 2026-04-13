package game.config;

import com.google.gson.Gson;
import game.config.aEnum.PopupType;
import game.treasure.mapping.UserEntity;
import game.object.MyUser;
import game.protocol.CommonProto;
import net.sf.json.JSONObject;
import ozudo.base.helper.ChUtil;
import protocol.Pbmethod;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CfgBattle {
    public static JSONObject json;
    public static DataConfig config;
    public static int periodUpdate = 16;//16:33 //0.016
    public static int periodUpdateLow = 100;//0.1s
    public static int periodFixedUpdate = 20;//0.02s
    public static int periodEffectUpdate = 500; //0.5s
    public static int periodUpdate1s = 1000; //1s
    //


    public static List<String> getInfoServer(int mode) {
        switch (mode) {
            case 0:
                return config.svHome;
            case 1:
            case 2:
                return config.svCampaign;
        }
        return null;
    }

    public static Pbmethod.CommonVector genInitMap(int roomType, int channelId, PopupType popupType) {
        return CommonProto.getCommonIntVector(List.of(roomType, channelId, popupType.value));
    }

//    public static void removeUserToRoom(Channel channel, String keyRoom, int userId) {
//        BaseRoom curRoom = (BaseRoom) ChUtil.get(channel, ChUtil.KEY_ROOM);
//        if (channel != null && curRoom.getKeyRoom().equals(keyRoom)) {
//            return;
//        }
//        if (curRoom != null && curRoom.hasPlayer(userId)) {
//            curRoom.removePlayer(userId);
//        }
//    }

    private static final ConcurrentHashMap<String, Lock> keyLocks = new ConcurrentHashMap<>();

    public static String getKeyRoom(MyUser mUser, int roomType, int... channel) {
        UserEntity u = mUser.getUser();
        int num = (channel.length > 0 ? channel[0] : 0);
        // key logic để lấy lock (nhóm phòng)
        String lockKey = roomType + "_" + num;
        // lấy lock riêng cho nhóm này
        Lock lock = keyLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
        lock.lock();
        try {
            return ChUtil.KEY_ROOM + "_" + roomType + "_" + num + "_" + u.getServer();
        } finally {
            lock.unlock();
        }
    }

    public static void loadConfig(String strJson) {
        config = new Gson().fromJson(strJson, DataConfig.class);
    }

    public class DataConfig {
        public List<String> svHome;
        public List<String> svCampaign;
    }
}
