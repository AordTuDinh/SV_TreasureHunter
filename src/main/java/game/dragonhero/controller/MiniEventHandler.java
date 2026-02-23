package game.dragonhero.controller;

import game.battle.object.Coroutine;
import game.config.CfgEvent;
import game.config.aEnum.DetailActionType;
import game.config.aEnum.StatusType;
import game.config.lang.Lang;
import game.dragonhero.server.IAction;
import game.dragonhero.service.user.Bonus;
import game.monitor.Online;
import game.monitor.UserMiniGame;
import game.object.KeoBuaBao;
import game.protocol.CommonProto;
import io.netty.channel.Channel;
import org.quartz.SchedulerException;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.StringHelper;
import ozudo.base.helper.Util;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MiniEventHandler extends AHandler {
    @Override
    public AHandler newInstance() {
        return new MiniEventHandler();
    }

    static MiniEventHandler instance;
    static int ACCEPT = 1;
    static int DISAGREE = 0;
    List<Integer> kbb = Arrays.asList(1, 2, 3);


    public static MiniEventHandler getInstance() {
        if (instance == null) {
            instance = new MiniEventHandler();
        }
        return instance;
    }

    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(RPS_SEND_RQ, RPS_RECEIVE_RQ, RPS_SELECT_RQ, RPS_SELECT_RESULT, RPS_RESULT);
        actions.forEach(action -> mHandler.put(action, this));
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        try {
            switch (actionId) {
                case IAction.RPS_SEND_RQ -> request();
                case IAction.RPS_SELECT_RQ -> selectRequest();
                case IAction.RPS_SELECT_RESULT -> selectResult();
                //case IAction.EVENT_1H_STATUS -> e1HourStatus();
                //case IAction.EVENT_1H_REWARD -> e1HourReward();
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    void request() {
        int userId = (int) CommonProto.parseCommonVector(requestData).getALong(0);
        // check người chơi có đang chơi k
        KeoBuaBao kb = UserMiniGame.keoBuaBao.get(userId);
        if (kb != null && System.currentTimeMillis() < kb.timeEnd) {
            addErrResponse(getLang(Lang.err_user_in_battle));
            return;
        }
        // check mình có đang trong trận k
        KeoBuaBao myKb = UserMiniGame.keoBuaBao.get(user.getId());
        if (myKb != null && System.currentTimeMillis() < myKb.timeEnd) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        Channel channel = Online.getChannel(userId);
        if (channel == null) {
            addErrResponse(getLang(Lang.err_user_not_online));
            return;
        }
        Pbmethod.CommonVector.Builder cmm = Pbmethod.CommonVector.newBuilder();
        cmm.addALong(user.getId());
        cmm.addAString(user.getName());
        // send req
        Util.sendProtoData(getChannel(), cmm.build(), IAction.RPS_RECEIVE_RQ);
        addResponse(getCommonVector(userId));
    }

    void selectRequest() throws SchedulerException {
        Pbmethod.CommonVector cmm = CommonProto.parseCommonVector(requestData);
        int userId = (int) cmm.getALong(0);
        KeoBuaBao kb = UserMiniGame.keoBuaBao.get(userId);
        if (kb != null && System.currentTimeMillis() < kb.timeEnd) {
            addErrResponse(getLang(Lang.err_user_in_battle));
            return;
        }
        // check mình có đang trong trận k
        KeoBuaBao myKb = UserMiniGame.keoBuaBao.get(user.getId());
        if (myKb != null && System.currentTimeMillis() < myKb.timeEnd) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        int status = (int) cmm.getALong(1);
        String name = cmm.getAString(0);
        Channel channelTarget = Online.getChannel(userId);
        if (channelTarget == null) {
            addErrResponse(getLang(Lang.err_user_not_online));
            return;
        }
        if (status == ACCEPT) {
            Pbmethod.CommonVector.Builder cm = Pbmethod.CommonVector.newBuilder();
            cm.addALong(user.getId());
            cm.addALong(status);
            cm.addAString(user.getName());
            Util.sendProtoData(channelTarget, cm.build(), IAction.RPS_SELECT_RQ);
            cm.clear();
            cm.addALong(userId);
            cm.addALong(status);
            cm.addAString(name);
            addResponse(getCommonVector(cm));

            if (kb == null) {
                kb = new KeoBuaBao(userId);
            }
            if (myKb == null) {
                myKb = new KeoBuaBao(user.getId());
            }

            UserMiniGame.addKb(kb);
            UserMiniGame.addKb(myKb);
            new Coroutine(() -> {
                int myId = user.getId();
                int targetId = userId;
                int result = UserMiniGame.keoBuaBao.get(myId).cal(UserMiniGame.keoBuaBao.get(targetId));
                Channel myChan = Online.getChannel(myId);
                Channel tgChan = Online.getChannel(targetId);
                Pbmethod.CommonVector.Builder res = Pbmethod.CommonVector.newBuilder();
                long idWin = result == KeoBuaBao.EQUAL ? 0 : result == KeoBuaBao.WIN ? myId : targetId;
                res.addALong(idWin);
                Util.sendProtoData(myChan, res.build(), IAction.RPS_RESULT);
                Util.sendProtoData(tgChan, res.build(), IAction.RPS_RESULT);
            }, user.getId(), "keobuabao", 6 * DateTime.SECOND2_MILLI_SECOND);
        } else {
            Pbmethod.CommonVector.Builder cm = Pbmethod.CommonVector.newBuilder();
            cm.addALong(user.getId());
            cm.addALong(status);
            cm.addAString(String.format(getLang(Lang.disagree_request), user.getName()));
            Util.sendProtoData(getChannel(), cm.build(), IAction.RPS_SELECT_RQ);
        }
    }

    void selectResult() {
        int req = (int) CommonProto.parseCommonVector(requestData).getALong(0);
        if (!kbb.contains(req)) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        KeoBuaBao myKb = UserMiniGame.keoBuaBao.get(user.getId());
        if (myKb == null) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        myKb.result = req;
        addResponse(getCommonVector(myKb.result));
    }


    void e1HourStatus() {
        int timeAdd = (int) user.getTimeLastLogin();
        timeAdd += mUser.getUserDaily().getLoginTime();
        List<Integer> data = mUser.getUserDaily().getEvent1hStatus(mUser);
        data.add(0, timeAdd);
        addResponse(getCommonIntVector(data));
    }

    void e1HourReward() {
        int slot = getInputInt();
        if (!CfgEvent.checkSlot1H(slot)) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        List<Integer> data = mUser.getUserDaily().getEvent1hStatus(mUser);
        int state = data.get(slot);
        if (state == StatusType.DONE.value) {
            addErrResponse(getLang(Lang.err_received_bonus));
            return;
        }
        if (state == StatusType.PROCESSING.value) {
            addErrResponse(getLang(Lang.err_condition_bonus));
            return;
        }

        data.set(slot, StatusType.DONE.value);
        if (mUser.getUserDaily().update(Arrays.asList("event_1h", StringHelper.toDBString(data)))) {
            mUser.getUserDaily().setEvent_1h(data.toString());
            List<Long> bonus = Bonus.receiveListItem(mUser, DetailActionType.EVENT_1_HOUR.getKey(slot), CfgEvent.config.bonus1hour.get(slot));
            if (bonus.isEmpty()) {
                addErrResponse();
                return;
            }
            Pbmethod.ListCommonVector.Builder builder = Pbmethod.ListCommonVector.newBuilder();
            builder.addAVector(Pbmethod.CommonVector.newBuilder());
            builder.addAVector(getCommonIntVector(data));
            addBonusToastPlus(bonus);
            addResponse(builder.build());
        } else addErrResponse();
    }
}




























