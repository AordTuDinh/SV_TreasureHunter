package ozudo.net;

import game.config.lang.Lang;
import game.dragonhero.controller.*;
import game.dragonhero.server.Constans;
import game.dragonhero.server.IAction;
import game.dragonhero.table.BaseRoom;
import game.dragonhero.table.StandaloneMoveRoom;
import game.protocol.CommonProto;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import io.netty.channel.Channel;
import lombok.Getter;
import ozudo.base.helper.ChUtil;
import protocol.Pbmethod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResponseMessage {
    private RequestMessage request;
    @Getter
    private int service = 0;
    static Map<Integer, game.dragonhero.controller.AHandler> mHandler;
    Pbmethod.ResponseData.Builder responseData = Pbmethod.ResponseData.newBuilder();

    static {
        mHandler = new HashMap<>();
        LoginHandler.getInstance().initAction(mHandler);
        BattleHandler.getInstance().initAction(mHandler);
        ArenaHandler.getInstance().initAction(mHandler);
        StatHandler.getInstance().initAction(mHandler);
        TowerHandler.getInstance().initAction(mHandler);
        UserHandler.getInstance().initAction(mHandler);
        PetHandler.getInstance().initAction(mHandler);
        ItemHandler.getInstance().initAction(mHandler);
        SmithyHandler.getInstance().initAction(mHandler);
        AchievementHandler.getInstance().initAction(mHandler);
        QuestHandler.getInstance().initAction(mHandler);
        MiniEventHandler.getInstance().initAction(mHandler);
        EventHandler.getInstance().initAction(mHandler);
        EventLuaHandler.getInstance().initAction(mHandler);
        PartyHandler.getInstance().initAction(mHandler);
        WorldBossHandler.getInstance().initAction(mHandler);
        LotteryHandler.getInstance().initAction(mHandler);
        LuckySpineHandler.getInstance().initAction(mHandler);
        HeroHandler.getInstance().initAction(mHandler);
        IAPHandler.getInstance().initAction(mHandler);
        WelfareHandler.getInstance().initAction(mHandler);
        ClanSkillHandler.getInstance().initAction(mHandler);
        ClanHandler.getInstance().initAction(mHandler);
        ChatHandler.getInstance().initAction(mHandler);
        SummonHandler.getInstance().initAction(mHandler);
        MailHandler.getInstance().initAction(mHandler);
        FriendHandler.getInstance().initAction(mHandler);
        MarketHandler.getInstance().initAction(mHandler);
        FarmHandler.getInstance().initAction(mHandler);
    }

    public ResponseMessage(RequestMessage request, Channel channel) {
        this.request = request;
        Object roomObj = ChUtil.get(channel, ChUtil.KEY_ROOM);
        if (request.getMagic().equals(Constans.MAGIC_IN_PUT)) {
            if (roomObj instanceof BaseRoom) {
                ((BaseRoom) roomObj).doSyncAction(channel, IAction.CLIENT_INPUT, request.getBody());
            } else if (roomObj instanceof StandaloneMoveRoom) {
                ((StandaloneMoveRoom) roomObj).doSyncAction(channel, IAction.CLIENT_INPUT, request.getBody());
            } else {
                // channel chưa có room (vd. chạy không DB/Redis) → dùng StandaloneMoveRoom
                StandaloneMoveRoom moveRoom = StandaloneMoveRoom.getInstance();
                moveRoom.registerChannel(channel);
                ChUtil.set(channel, ChUtil.KEY_ROOM, moveRoom);
                System.out.println("[SV] Client joined move room: " + channel.remoteAddress());
                moveRoom.doSyncAction(channel, IAction.CLIENT_INPUT, request.getBody());
            }
        } else if (request.getMagic().equals(Constans.MAGIC_OUT_GAME)) {
            Pbmethod.RequestData requestData = CommonProto.parseRequest(request.getBody());
            if (requestData != null) {
                String session = requestData.getSession();
                List<Pbmethod.PbAction> actions = requestData.getActionsList();
                for (int i = 0; i < actions.size(); i++) {
                    int sv = actions.get(i).getActionId();
                    if (sv == IAction.PING_IDLE) {
                        return;
                    }
                    ChUtil.remove(channel, ChUtil.KEY_IDLE);
                    if (mHandler.get(sv) == null) {
                        LoginHandler.getInstance().addErrResponse(Lang.getTitle(LoginHandler.getInstance().getMUser(), Lang.err_params));
                        return;
                    }

                    AHandler handler = mHandler.get(sv).newInstance();
                    handler.handle(channel, session, sv, actions.get(i).getData().toByteArray());
                    responseData.addAllAAction(handler.getResponse().getAActionList());
                }
            }
        }
    }

    static Map<String, Integer> handlerReq = new HashMap<String, Integer>();

    public static synchronized boolean validateSequenceRequest(String session) {
        if (session == null || session.length() == 0) {
            return true;
        }
        if (handlerReq.containsKey(session)) {
            return false;
        }
        handlerReq.put(session, 1);
        return true;
    }

    public static void endRequest(String session) {
        if (session != null && session.length() > 0) {
            handlerReq.remove(session);
        }
    }


    public Object getResponse() {
        if (responseData != null) {
            try {
                byte[] body = responseData.build().toByteArray();
                ByteBuf buffer = Unpooled.buffer();
                buffer.writeBytes(Constans.MAGIC_OUT_GAME.getBytes()); //T1
                buffer.writeInt(body.length);
                buffer.writeBytes(body);
                return buffer;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }
}
