package game.config;

import com.google.gson.Gson;
import game.config.aEnum.PopupType;
import game.config.aEnum.RoomType;
import game.dragonhero.mapping.UserEntity;
import game.dragonhero.table.BaseRoom;
import game.object.MyUser;
import game.protocol.CommonProto;
import io.netty.channel.Channel;
import net.sf.json.JSONObject;
import ozudo.base.helper.ChUtil;
import ozudo.base.helper.Util;
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
    public static float decTimeEff = periodUpdate1s / 1000f;
    public static float decTimeEffRoom = periodEffectUpdate / 1000f;
    public static float updateTime = periodUpdate / 1000f;
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

    public static Pbmethod.CommonVector genInitMap(int roomType, int subId, int channelId, int playerCollider, boolean isBattle, PopupType popupType) {
        return CommonProto.getCommonIntVector(genInitMapInt(roomType, subId, channelId, playerCollider, isBattle, popupType));
    }

    public static List<Integer> genInitMapInt(int roomType, int subId, int channelId, int playerCollider, boolean isBattle, PopupType popupType) {
        return List.of(roomType, subId, channelId, playerCollider, isBattle ? 1 : 0, popupType.value);
    }

    public static void removeUserToRoom(Channel channel, String keyRoom, int userId) {
        BaseRoom curRoom = (BaseRoom) ChUtil.get(channel, ChUtil.KEY_ROOM);
        if (channel != null && curRoom.getKeyRoom().equals(keyRoom)) {
            return;
        }
        if (curRoom != null && curRoom.hasPlayer(userId)) {
            curRoom.removePlayer(userId);
        }
    }

    private static final ConcurrentHashMap<String, Lock> keyLocks = new ConcurrentHashMap<>();

    public static String getKeyRoom(MyUser mUser, int roomType, int subId, int... channel) {
        UserEntity u = mUser.getUser();
        int num = (channel.length > 0 ? channel[0] : 0);

        // Chuẩn hóa lại giá trị roomType trước khi tạo key
        switch (RoomType.get(roomType)) {
            case CLAN:           // phòng clan -> theo clanId
                if (u.getClan() != 0) {
                    subId = 0;
                    num = u.getClan();
                }
                break;
            case FARM:           // phòng farm → theo userId
                num = u.getId();
                break;
        }

        // key logic để lấy lock (nhóm phòng)
        String lockKey = buildLockKey(roomType, subId, num);

        // lấy lock riêng cho nhóm này
        Lock lock = keyLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
        lock.lock();
        try {
            // tạo key room cuối cùng
            return buildRoomKey(roomType, subId, num, u.getServer());
        } finally {
            lock.unlock();
        }
    }

    private static String buildLockKey(int roomType, int subId, int num) {
        return roomType + "_" + subId + "_" + num;
    }

    private static String buildRoomKey(int roomType, int subId, int num, int server) {
        // format: room_roomType_subId_num_server
        return ChUtil.KEY_ROOM + "_" + roomType + "_" + subId + "_" + num + "_" + server;
    }


    public static void loadConfig(String strJson) {
        config = new Gson().fromJson(strJson, DataConfig.class);
    }

    public class DataConfig {
        public List<String> svHome;
        public List<String> svCampaign;
    }
}
