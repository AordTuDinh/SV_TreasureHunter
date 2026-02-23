package game.dragonhero.controller;

import io.netty.channel.Channel;
import ozudo.base.log.Logs;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BaseHandler extends AHandler {
    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList();
        actions.forEach(action -> mHandler.put(action, this));
    }

    static BaseHandler instance;

    public static BaseHandler getInstance() {
        if (instance == null) {
            instance = new BaseHandler();
        }
        return instance;
    }

    @Override
    public AHandler newInstance() {
        return new BaseHandler();
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        try {
            switch (actionId) {
                //case POINT_DATA -> pointData();
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }
}
