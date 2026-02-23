package game.dragonhero.controller;

import game.config.CfgAchievement;
import game.config.aEnum.DetailActionType;
import game.config.aEnum.StatusType;
import game.config.lang.Lang;
import game.dragonhero.mapping.UserAchievementEntity;
import game.dragonhero.mapping.main.ResAchievementEntity;
import game.dragonhero.server.IAction;
import game.dragonhero.service.Services;
import game.dragonhero.service.resource.ResAchievement;
import game.dragonhero.service.user.Bonus;
import game.protocol.CommonProto;
import io.netty.channel.Channel;
import ozudo.base.helper.StringHelper;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AchievementHandler extends AHandler {
    @Override
    public AHandler newInstance() {
        return new AchievementHandler();
    }

    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(ACHIEVEMENT_STATUS, ACHIEVEMENT_REWARD, ACHIEVEMENT_INFO, ACHIEVEMENT_RECEIVE);
        actions.forEach(action -> mHandler.put(action, this));
    }

    static AchievementHandler instance;
    UserAchievementEntity uAchi;

    public static AchievementHandler getInstance() {
        if (instance == null) {
            instance = new AchievementHandler();
        }
        return instance;
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        checkTimeMonitor("s");
        uAchi = Services.userDAO.getUserAchievement(mUser);
        try {
            switch (actionId) {
                case IAction.ACHIEVEMENT_STATUS -> status();
                case IAction.ACHIEVEMENT_REWARD -> reward();
                case IAction.ACHIEVEMENT_INFO -> info();
                case IAction.ACHIEVEMENT_RECEIVE -> receive();
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    void status() {
        addResponse(getCommonIntVector(uAchi.getPoint()));
    }

    void reward() {
        int type = (int) CommonProto.parseCommonVector(requestData).getALong(0);
        List<Integer> points = uAchi.getPoint();
        int maxPoint = type == 0 ? CfgAchievement.config.maxAllPoint : CfgAchievement.config.maxPoint;
        if (points.get(type) < maxPoint) {
            addErrResponse(getLang(Lang.err_not_enough_point));
            return;
        }
        int num = points.get(type) / maxPoint;
        points.set(type, points.get(type) - maxPoint * num);
        // add point all
        if (type != 0) points.set(0, points.get(0) + 1);
        if (uAchi.update(List.of("point", StringHelper.toDBString(points)))) {
            uAchi.setPoint(points.toString());
            List<Long> bonus = CfgAchievement.getBonusByType(type, num);
            Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
            List<Long> ret = Bonus.receiveListItem(mUser, DetailActionType.POINT_ACHIEVEMENT.getKey(type), Bonus.merge(bonus));
            pb.addAVector(getCommonVector(ret));
            pb.addAVector(getCommonIntVector(points));
            addResponse(pb.build());
        } else addErrSystem();
    }

    void info() {
        int type = (int) CommonProto.parseCommonVector(requestData).getALong(0);
        if (!CfgAchievement.checkType(type)) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        List<Integer> ret = uAchi.getAItem(type);
        addResponse(getCommonIntVector(ret));
    }

    void receive() {
        List<Long> cmm = CommonProto.parseCommonVector(requestData).getALongList();
        int type = Math.toIntExact(cmm.get(0));
        int id = Math.toIntExact(cmm.get(1));
        if (!CfgAchievement.checkType(type)) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        ResAchievementEntity resAchi = ResAchievement.getResAchievement(type, id);
        if (resAchi == null) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        List<Integer> ret = uAchi.getAItem(type);
        int index = id - 1;
        if (ret.get(index * 2 + 1) == StatusType.DONE.value) {
            addErrResponse(getLang(Lang.err_received_bonus));
            return;
        }
        ret.set(index * 2 + 1, StatusType.DONE.value);
        List<Integer> points = uAchi.getPoint();
        points.set(type, points.get(type) + resAchi.getBonus());
        if (uAchi.updateTab(type, ret, points)) {
            Pbmethod.ListCommonVector.Builder lsc = Pbmethod.ListCommonVector.newBuilder();
            lsc.addAVector(getCommonIntVector(points));
            lsc.addAVector(getCommonVector(type, id, ret.get(index * 2 + 1)));
            addResponse(lsc.build());
        }
    }
}
