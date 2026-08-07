package game.treasure.controller;

import game.config.CfgAchievement;
import game.config.aEnum.DetailActionType;
import game.config.aEnum.StatusType;
import game.config.lang.Lang;
import game.treasure.mapping.UserAchievementEntity;
import game.treasure.mapping.main.ResAchievementEntity;
import game.treasure.server.IAction;
import game.treasure.service.Services;
import game.treasure.service.resource.ResAchievement;
import game.treasure.service.user.Bonus;
import game.protocol.CommonProto;
import io.netty.channel.Channel;
import ozudo.base.helper.StringHelper;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

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
        List<Integer> points = uAchi.getPoint();
        uAchi.syncMilestoneStatus(points);
        uAchi.flushMilestoneSync();
        Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
        pb.addAVector(getCommonIntVector(points));
        pb.addAVector(getCommonIntVector(uAchi.getMilestoneStatus()));
        addResponse(pb.build());
    }

    void reward() {
        List<Long> cmm = CommonProto.parseCommonVector(requestData).getALongList();
        int type = Math.toIntExact(cmm.get(0));
        if (type == 0) {
            rewardMainMilestone(cmm);
            return;
        }
        rewardTabSlider(type);
    }

    /** Nhận quà mốc thanh tổng (20/40/60/80/100); request [0, milestoneIndex]. */
    void rewardMainMilestone(List<Long> cmm) {
        if (cmm.size() < 2) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        int index = Math.toIntExact(cmm.get(1));
        if (!CfgAchievement.checkMilestoneIndex(index)) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        List<Integer> points = uAchi.getPoint();
        uAchi.syncMilestoneStatus(points);
        List<Integer> status = uAchi.getMilestoneStatus();
        if (index >= status.size()) {
            addErrResponse(getLang(Lang.err_not_enough_point));
            return;
        }
        if (status.get(index) == StatusType.DONE.value) {
            addErrResponse(getLang(Lang.err_received_bonus));
            return;
        }
        if (status.get(index) != StatusType.RECEIVE.value) {
            addErrResponse(getLang(Lang.err_not_enough_point));
            return;
        }
        List<Long> bonusWire = CfgAchievement.getMilestoneBonus(index);
        if (!bonusWire.isEmpty() && !mUser.checkSlotAddBonus(bonusWire)) {
            addErrResponse(getLang(Lang.err_max_slot));
            return;
        }
        List<Long> ret = Bonus.receiveListItem(mUser,
                DetailActionType.POINT_ACHIEVEMENT.getKey(index), bonusWire);
        if (ret.isEmpty()) {
            addErrSystem();
            return;
        }
        status.set(index, StatusType.DONE.value);
        if (!uAchi.updateMilestoneStatus(status)) {
            addErrSystem();
            return;
        }
        Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
        pb.addAVector(getCommonVector(ret));
        pb.addAVector(getCommonIntVector(points));
        pb.addAVector(getCommonIntVector(status));
        addResponse(pb.build());
    }

    /** Rương slider từng tab (type 1–5): giữ logic cũ, trừ điểm tab và +1 điểm tổng. */
    void rewardTabSlider(int type) {
        List<Integer> points = uAchi.getPoint();
        int maxPoint = CfgAchievement.getMaxPoint();
        if (points.get(type) < maxPoint) {
            addErrResponse(getLang(Lang.err_not_enough_point));
            return;
        }
        int num = points.get(type) / maxPoint;
        points.set(type, points.get(type) - maxPoint * num);
        points.set(0, points.get(0) + num);
        uAchi.syncMilestoneStatus(points);
        List<Object> updateFields = new java.util.ArrayList<>(List.of("point", StringHelper.toDBString(points)));
        if (uAchi.isCanUpdate()) {
            updateFields.add("milestone_status");
            updateFields.add(StringHelper.toDBString(uAchi.getMilestoneStatus()));
            uAchi.setCanUpdate(false);
        }
        if (uAchi.update(updateFields)) {
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
            lsc.addAVector(getCommonIntVector(uAchi.getMilestoneStatus()));
            addResponse(lsc.build());
        }
    }
}
