package game.dragonhero.controller;

import game.config.CfgChat;
import game.config.CfgFeature;
import game.config.CfgServer;
import game.config.aEnum.NotifyType;
import game.config.aEnum.ChatType;
import game.config.aEnum.FeatureType;
import game.config.lang.Lang;
import game.dragonhero.mapping.UserChatEntity;
import game.dragonhero.server.IAction;
import game.dragonhero.service.Services;
import game.dragonhero.table.BaseRoom;
import game.monitor.Online;
import game.object.FriendChatObject;
import game.object.MyUser;
import game.object.UserChatInfoObject;
import game.protocol.CommonProto;
import io.netty.channel.Channel;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.ChUtil;
import ozudo.base.helper.StringHelper;
import ozudo.base.helper.Util;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ChatHandler extends AHandler {
    static ChatHandler instance;

    @Override
    public AHandler newInstance() {
        return new ChatHandler();
    }

    public static ChatHandler getInstance() {
        if (instance == null) {
            instance = new ChatHandler();
        }
        return instance;
    }

    List<Integer> noCheckFeature = Arrays.asList(CHAT_FRIEND_NOTIFY);

    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(CHAT_SERVER, CHAT_MAP, CHAT_BLOCK, CHAT_UN_BLOCK, CHAT_FRIEND_LIST, CHAT_FRIEND,
                CHAT_SETTING, CHAT_FRIEND_NOTIFY);
        actions.forEach(action -> mHandler.put(action, this));
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        checkTimeMonitor("s");
        if (!noCheckFeature.contains(actionId) && !CfgFeature.isOpenFeature(FeatureType.CHAT, mUser, this)) {
            return;
        }
        try {
            switch (actionId) {
                case IAction.CHAT_SERVER -> serverChat();
                case IAction.CHAT_MAP -> mapChat();
                case IAction.CHAT_FRIEND_LIST -> friendChatList();
                case IAction.CHAT_FRIEND -> friendChat();
                case IAction.CHAT_BLOCK -> chatBlock();
                case IAction.CHAT_UN_BLOCK -> chatUnBlock();
                case IAction.CHAT_SETTING -> chatSetting();
                case IAction.CHAT_FRIEND_NOTIFY -> chatNotifyList();
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    void serverChat() {
        if (user.isLockChat() != null) {
            addErrResponse(getLang(Lang.err_chat_block));
            return;
        }
        Pbmethod.CommonVector cmm = parseCommonVector(requestData);
        String content = cmm.getAString(0);
        String chatMsg = CfgChat.replaceInvalidWord(content);
        if (chatMsg.isEmpty()){
            addErrResponse(getLang(Lang.chat_too_quick));
            return;
        }
        if (System.currentTimeMillis() - user.getLastChatServer() > CfgChat.chatSpam * 1000) {
            if (chatMsg.length() > CfgChat.maxCharacter) {
                chatMsg = chatMsg.substring(0, CfgChat.maxCharacter);
            }
            if (!CfgChat.isValidChat(chatMsg, user.getLastMsgChatMap())) {
                addErrResponse(getLang(Lang.chat_msg_invalid));
                return;
            }
            user.setLastChatServer(System.currentTimeMillis());
            user.setLastMsgChatServer(chatMsg);

            Util.sendProtoDataToListChanel(Online.getUserInServer(user.getServer()), chat(chatMsg), IAction.CHAT_SERVER);
            //Util.sendProtoDataToListChanel(Online.getUserInServer(user.getServer()),CommonProto.getCommonVector(NotifyType.MESSAGE.value), IAction.ADD_NOTIFY);
        } else {
            addErrResponse(getLang(Lang.chat_too_quick));
        }
    }


    void mapChat() {
        if (user.isLockChat() != null) {
            addErrResponse(getLang(Lang.err_chat_block));
            return;
        }
        BaseRoom curRoom = mUser.getPlayer().getRoom();
        if (curRoom == null) {
            addErrResponse(getLang(Lang.err_room_not_found));
            return;
        }
        String content = getInputString();
        String chatMsg = CfgChat.replaceInvalidWord(content);
        if (chatMsg.isEmpty()){
            addErrResponse(getLang(Lang.chat_too_quick));
            return;
        }
        if (System.currentTimeMillis() - user.getLastChatMap() > CfgChat.chatSpam * 1000) {
            if (chatMsg.length() > CfgChat.maxChatMap) {
                chatMsg = chatMsg.substring(0, CfgChat.maxChatMap);
            }
            if (!CfgChat.isValidChat(chatMsg, user.getLastMsgChatMap())) {
                addErrResponse(getLang(Lang.chat_msg_invalid));
                return;
            }
            user.setLastChatMap(System.currentTimeMillis());
            user.setLastMsgChatMap(chatMsg);
            curRoom.sendDataAllUser(IAction.CHAT_MAP, chat(chatMsg));
        } else {
            addErrResponse(getLang(Lang.chat_too_quick));
        }
    }

    void friendChatList() {
        Pbmethod.CommonVector cmm = CommonProto.parseCommonVector(requestData);
        int idTarget = (int) cmm.getALong(0);
        UserChatEntity uChat = Services.userDAO.getUserChat(mUser, idTarget);
        if (uChat == null) {
            addResponse(Pbmethod.PbListChatFriend.newBuilder().build());
            return;
        }

        if (uChat.getNotify() > 0) {
            boolean isId1 = uChat.isId1(user.getId());
            if (isId1 && uChat.getNotify() == 1 || !isId1 && uChat.getNotify() == 2) {
                uChat.clearNotify();
            }
        }
        addResponse(uChat.getChatHistory(mUser, idTarget));
    }

    void friendChat() {
        Pbmethod.CommonVector cmm = CommonProto.parseCommonVector(requestData);
        int idTarget = (int) cmm.getALong(0);
        Channel uTarget = Online.getChannel(idTarget);
        UserChatEntity uChat = Services.userDAO.getUserChat(mUser, idTarget);
        if (uChat == null) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        String content = cmm.getAString(0);
        String chatMsg = CfgChat.replaceInvalidWord(content);
        if (chatMsg.isEmpty()){
            addErrResponse(getLang(Lang.chat_too_quick));
            return;
        }
        if (chatMsg.length() > CfgChat.maxCharacter) {
            chatMsg = chatMsg.substring(0, CfgChat.maxCharacter);
        }
        if (!CfgChat.isValidChat(chatMsg, user.getLastMsgChatMap())) {
            addErrResponse(getLang(Lang.chat_msg_invalid));
            return;
        }
        FriendChatObject newChat = new FriendChatObject(user, chatMsg);
        MyUser mUTarget = ChUtil.getMUser(uTarget);
        if (uChat.addChat(mUser, idTarget, newChat)) {
            UserChatInfoObject info = new UserChatInfoObject(user);
            if (mUTarget != null) {
                mUTarget.addChatFriend(mUser.getChatHistory(idTarget), info);
                uChat.clearNotify();
                mUTarget.addNotify(NotifyType.MESSAGE);
            } else { // đang off phải lưu vào danh sách notify
                uChat.addNotify(idTarget);
            }
            addResponse(newChat.toProto(info));
        } else addErrResponse();
    }

    void chatBlock() {
        Pbmethod.CommonVector cmm = CommonProto.parseCommonVector(requestData);
        int idTarget = (int) cmm.getALong(0);
        if (mUser.getUSetting().blockChatId(idTarget)) {
            addResponse(getCommonIntVector(mUser.getUSetting().listBlockChat()));
        } else addErrResponse();
    }

    void chatUnBlock() {
        int idTarget = getInputInt();
        if (mUser.getUSetting().unBlockChat(idTarget)) {
            addResponse(getCommonIntVector(mUser.getUSetting().listBlockChat()));
        } else addErrResponse();
    }

    void chatSetting() {
        List<Long> input = getInputALong();
        long in1 = input.get(0);
        long in2 = input.get(1);
        if (input.size() != 2 || in1 < 0 || in1 > 100 || in2 < 0 || in2 > 100) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        String data = StringHelper.toDBString(input);
        if (mUser.getUSetting().update(Arrays.asList("chat_setting", data))) {
            mUser.getUSetting().setChatSetting(data);
            addResponse(getCommonVector(input));
        } else addErrResponse(getLang(Lang.err_system_down));
    }

    void chatNotifyList() { // Chỉ gọi 1 lần lúc vào game
        List<UserChatEntity> uChat = DBJPA.getSelectQuery("select * from " + CfgServer.DB_DSON + "user_chat where user_id1=" + user.getId() + " or user_id2=" + user.getId(), UserChatEntity.class);
        List<Long> data = new ArrayList<>();
        for (int i = 0; i < uChat.size(); i++) {
            UserChatEntity chat = uChat.get(i);
            boolean isId1 = chat.isId1(user.getId());
            if (isId1 && chat.getNotify() == 1 || !isId1 && chat.getNotify() == 2) {
                int targetId = isId1 ? chat.getUserId2() : chat.getUserId1();
                long online = Online.isOnline(targetId) ? 1L : 0L;
                data.add((long) targetId);
                data.add(online);
            }
        }
        addResponse(getCommonVector(data));
    }

    Pbmethod.PbChat chat(String msg) {
        Pbmethod.PbChat.Builder chat = Pbmethod.PbChat.newBuilder();
        chat.setReqTime(System.currentTimeMillis() / 1000);
        chat.setMessage(msg);
        chat.setType(ChatType.MSG.value);
        chat.setUser(user.toProto());
        chat.addAllPoint(user.getCachePoint().toProto());
        return chat.build();
    }
}
