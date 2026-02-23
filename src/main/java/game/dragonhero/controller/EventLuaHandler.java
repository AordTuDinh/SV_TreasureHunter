package game.dragonhero.controller;

import game.config.lang.Lang;
import game.dragonhero.mapping.UserEventEntity;
import game.dragonhero.mapping.main.ResEventTopEntity;
import game.dragonhero.server.IAction;
import game.dragonhero.service.resource.ResEventTop;
import io.netty.channel.Channel;
import ozudo.base.helper.DateTime;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import java.util.*;

public class EventLuaHandler extends AHandler {
    @Override
    public AHandler newInstance() {
        return new EventLuaHandler();
    }

    static EventLuaHandler instance;

    public static EventLuaHandler getInstance() {
        if (instance == null) {
            instance = new EventLuaHandler();
        }
        return instance;
    }

    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(EVENT_LUA_LIST, EVENT_LUA_STATUS);
        actions.forEach(action -> mHandler.put(action, this));
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);

        try {
            switch (actionId) {
                case IAction.EVENT_LUA_LIST -> eventList();
                case IAction.EVENT_LUA_STATUS -> eventStatus();
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    private void eventList() {
        Pbmethod.ListCommonVector.Builder lstCm = Pbmethod.ListCommonVector.newBuilder();
        List<ResEventTopEntity> resEventTopEntities = ResEventTop.aEvent;
        Date now = new Date();
        for (int i = 0; i < resEventTopEntities.size(); i++) {
            ResEventTopEntity res = resEventTopEntities.get(i);
            if (res.inEvent(user) && now.after(res.getDateStart()) && now.before(res.getDateEnd())) {
                lstCm.addAVector(Pbmethod.CommonVector.newBuilder().addALong(res.getEventType()).addALong(1).addAString(res.getButtonPart()).addAString(getLang(res.getButtonName())));
            }
        }
        addResponse(lstCm.build());
    }


    void eventStatus() {
        int eventId = getInputInt();
        ResEventTopEntity res = ResEventTop.getResEventTop(eventId);
        if (res == null) {
            addErrParam();
            return;
        }
        Pbmethod.ListCommonVector.Builder lstCm = Pbmethod.ListCommonVector.newBuilder();
        lstCm.addAVector(Pbmethod.CommonVector.newBuilder().addALong(eventId).addAString(res.getPartBanner()).addAString(getLang( res.getTitlePanel()))
                .addAString(getLang( res.getDescPanel())));

        List<List<Long>> dataBonus = res.getDataBonus();
        for (int i = 0; i < dataBonus.size(); i++) {
            lstCm.addAVector(getCommonVector(dataBonus.get(i)));
        }
        addResponse(lstCm.build());

    }

}