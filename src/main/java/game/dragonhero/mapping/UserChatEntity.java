package game.dragonhero.mapping;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import game.config.CfgChat;
import game.object.FriendChatObject;
import game.object.MyUser;
import game.object.UserChatInfoObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.StringHelper;
import protocol.Pbmethod;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


@Entity
@Table(name = "user_chat")
@NoArgsConstructor
public class UserChatEntity implements Serializable {
    @Getter
    @Id
    int userId1, userId2;
    String message, info1, info2;
    @Getter
    int notify;
    @Getter
    @Transient
    UserChatInfoObject uInfo1, uInfo2;
    @Getter
    Date activeTime;


    public UserChatEntity(int userId, int targetId) {
        if (userId < targetId) {
            this.userId1 = userId;
            this.userId2 = targetId;
        } else {
            this.userId1 = targetId;
            this.userId2 = userId;
        }
        this.message = "[]";
        this.notify = 0;
    }

    public boolean isId1(int userId) {
        return userId == userId1;
    }

    List<FriendChatObject> getChats(MyUser mUser, int idTarget) {
        if (mUser.getChatHistory(idTarget).size() == 0) {
            mUser.setChatHistory(idTarget, (new Gson().fromJson(message, new TypeToken<List<FriendChatObject>>() {
            }.getType())));
        }
        return mUser.getChatHistory(idTarget);
    }

    public Pbmethod.PbListChatFriend getChatHistory(MyUser mUser, int idTarget) {
        Pbmethod.PbListChatFriend.Builder pbChats = Pbmethod.PbListChatFriend.newBuilder();
        List<FriendChatObject> chats = getChats(mUser, idTarget);
        checkData();
        int id1 = mUser.getUser().getId() < idTarget ? mUser.getUser().getId() : idTarget;
        for (int i = 0; i < chats.size(); i++) {
            UserChatInfoObject info = chats.get(i).getId() == id1 ? uInfo1 : uInfo2;
            if (info != null) pbChats.addChats(chats.get(i).toProto(info));
        }
        return pbChats.build();
    }

    public void clearNotify() { // 2 thằng đều on nên check để xóa notify đi
        if (notify != 0 && update(Arrays.asList("notify", 0))) {
            this.notify = 0;
        }
    }

    private void checkData() {
        if (uInfo1 == null) uInfo1 = new Gson().fromJson(info1, new TypeToken<UserChatInfoObject>() {
        }.getType());
        if (uInfo2 == null) uInfo2 = new Gson().fromJson(info2, new TypeToken<UserChatInfoObject>() {
        }.getType());
    }

    public void addNotify(int targetId) {
        if (targetId == userId1) {
            if (update(Arrays.asList("notify", 1))) this.notify = 1;
        } else if (update(Arrays.asList("notify", 2))) this.notify = 2;
    }

    public boolean addChat(MyUser mUser, int idTarget, FriendChatObject chatObject) {
        String column = "", infoUpdate = "";
        boolean updateInfo = false;
        if (info1 == null || info2 == null) {
            if (mUser.getUser().getId() < idTarget) {
                uInfo1 = new UserChatInfoObject(mUser.getUser());
                info1 = StringHelper.toDBString(uInfo1);
                column = "info1";
                infoUpdate = info1;
                updateInfo = true;
            } else {
                uInfo2 = new UserChatInfoObject(mUser.getUser());
                info2 = StringHelper.toDBString(uInfo2);
                column = "info2";
                infoUpdate = info2;
                updateInfo = true;
            }
        }
        List<FriendChatObject> chatHistory = getChats(mUser, idTarget);
        if (chatHistory.size() > CfgChat.maxSaveChat - 1) {
            chatHistory.remove(0);
        }
        chatHistory.add(chatObject);

        message = new Gson().toJson(chatHistory);
        if (updateInfo)
            return update(Arrays.asList("message", message, column, infoUpdate, "active_time", DateTime.getFullDate()));
        else return update(Arrays.asList("message", message, "active_time", DateTime.getFullDate()));
    }

    public boolean update(List<Object> data) {
        return DBJPA.update("user_chat", data, Arrays.asList("user_id1", userId1, "user_id2", userId2));
    }


}
