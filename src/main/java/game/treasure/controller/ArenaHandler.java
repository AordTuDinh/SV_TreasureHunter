package game.treasure.controller;

import game.config.CfgFeature;
import game.config.aEnum.FeatureType;
import game.protocol.CommonProto;
import game.treasure.service.arena.ArenaService;
import io.netty.channel.Channel;
import ozudo.base.log.Logs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ArenaHandler extends AHandler {
    static ArenaHandler instance;

    public static ArenaHandler getInstance() {
        if (instance == null)
            instance = new ArenaHandler();
        return instance;
    }

    @Override
    public AHandler newInstance() {
        return new ArenaHandler();
    }

    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(ARENA_STATUS, ARENA_REGISTER, ARENA_CANCEL);
        actions.forEach(action -> mHandler.put(action, this));
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        try {
            if (mUser == null)
                return;
            if (!CfgFeature.isOpenFeature(FeatureType.ARENA, mUser, this))
                return;
            switch (actionId) {
                case ARENA_STATUS -> status();
                case ARENA_REGISTER -> register();
                case ARENA_CANCEL -> cancel();
            }
        } catch (Exception ex) {
            Logs.error(ex);
            addErrSystem();
        }
    }

    void status() {
        List<Long> wire = ArenaService.getInstance().buildStatus(mUser);
        addResponse(CommonProto.getCommonVector(wire));
    }

    void register() {
        List<Long> bonus = new ArrayList<>();
        String err = ArenaService.getInstance().register(mUser, bonus);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        addResponse(getCommonVector(bonus.isEmpty() ? Arrays.asList(1L) : bonus));
        addResponse(ARENA_STATUS, CommonProto.getCommonVector(ArenaService.getInstance().buildStatus(mUser)));
    }

    void cancel() {
        List<Long> bonus = new ArrayList<>();
        String err = ArenaService.getInstance().cancel(mUser, bonus);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        addResponse(getCommonVector(bonus.isEmpty() ? Arrays.asList(1L) : bonus));
        addResponse(ARENA_STATUS, CommonProto.getCommonVector(ArenaService.getInstance().buildStatus(mUser)));
    }
}
