package game.monitor;

import com.google.gson.Gson;
import game.cache.JCache;
import game.config.aEnum.ChatType;
import game.object.ChatObject;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChatMonitor {

    public static int maxReturn = 30, maxStore = 50;
    private String key;
    private List<ChatObject> aChat;
    private long lastChat = -1;
    private ChatType chatType = ChatType.MSG;

    public ChatMonitor(String key) {
        this.key = key;
        init();
    }

    public void init() {
        aChat = new ArrayList<>();
        List<String> tmp = JCache.getInstance().lrange(key, -maxStore, -1);
        if (tmp != null)
            tmp.forEach(value -> aChat.add(new Gson().fromJson(value, ChatObject.class)));
    }

    public protocol.Pbmethod.PbListChat getChatHistory(long reqTime) {
        protocol.Pbmethod.PbListChat.Builder builder = protocol.Pbmethod.PbListChat.newBuilder();
        for (int i = aChat.size() - 1; i >= 0; i--) {
            if (aChat.get(i).getTimeSeconds() > reqTime / 1000) {
                builder.addAChat(0, aChat.get(i).toProto());
                if (--maxReturn == 0) break;
            }
        }
        return builder.build();
    }

    public void addChat(ChatObject chatObject) {
        aChat.add(chatObject);
        lastChat = System.currentTimeMillis();
        while (aChat.size() > maxStore) aChat.remove(0);
        JCache.getInstance().rpush(key, new Gson().toJson(chatObject));
        if (System.currentTimeMillis() % 20 == 1) {
            JCache.getInstance().ltrim(key, maxStore);
        }
    }
}
