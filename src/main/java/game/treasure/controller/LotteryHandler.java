package game.treasure.controller;

import game.config.CfgLottery;
import game.config.aEnum.DetailActionType;
import game.config.aEnum.ItemPointKey;
import game.config.aEnum.StatusType;
import game.config.lang.Lang;
import game.treasure.mapping.UserItemPointEntity;
import game.treasure.mapping.UserLotteryHistoryEntity;
import game.treasure.server.IAction;
import game.treasure.service.user.Bonus;
import game.protocol.CommonProto;
import io.netty.channel.Channel;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.DateTime;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import java.util.*;
import java.util.stream.Collectors;

public class LotteryHandler extends AHandler {

    @Override
    public AHandler newInstance() {
        return new LotteryHandler();
    }

    static LotteryHandler instance;

    public static LotteryHandler getInstance() {
        if (instance == null) {
            instance = new LotteryHandler();
        }
        return instance;
    }

    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(LOTTERY_MINI_USE, LOTTERY_VIEW, LOTTERY_MINI_BUY, LOTTERY_HISTORY, LOTTERY_RECEIVE);
        actions.forEach(action -> mHandler.put(action, this));
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        try {
            switch (actionId) {
                case IAction.LOTTERY_MINI_BUY -> miniBuy();
                case IAction.LOTTERY_MINI_USE -> miniUse();
                case IAction.LOTTERY_VIEW -> lotteView();
                case IAction.LOTTERY_HISTORY -> history(getInputInt());
                case IAction.LOTTERY_RECEIVE -> receive();
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }


    void miniBuy() {
        int numTicker = (int) CommonProto.parseCommonVector(requestData).getALong(0);
        if (numTicker == 0 || numTicker > CfgLottery.config.maxBuyMini) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        List<Long> fee = CfgLottery.getFeeBuyMini(numTicker);
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        fee.addAll(CfgLottery.getTickerMini(numTicker));
        List<Long> bonus = Bonus.receiveListItem(mUser, DetailActionType.BUY_LOTTERY_MINI.getKey(), fee);
        if (bonus == null) {
            addErrResponse();
            return;
        }
        addResponse(getCommonVector(bonus));
    }

    void miniUse() {
        protocol.Pbmethod.ListCommonVector lscm = CommonProto.parseListCommonVector(requestData);
        int numTicker = lscm.getAVectorCount();
        if (numTicker == 0) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        for (int i = 0; i < lscm.getAVectorCount(); i++) {
            List<Long> lst = lscm.getAVector(i).getALongList();
            if (lst.size() < 1 || lst.size() > 5 || lst.stream().filter(index -> Collections.frequency(lst, index) > 1).collect(Collectors.toList()).size() > 0) {
                addErrParam();
                return;
            }
        }

        List<Long> bonus = Bonus.viewItemPoint(ItemPointKey.TICKER_MINI.id, -numTicker);
        String err = Bonus.checkMoney(mUser, bonus);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        protocol.Pbmethod.PbListMiniLotte.Builder lResult = CfgLottery.checkLuckyMini(numTicker, lscm, bonus);
        lResult.addAllAllBonus(Bonus.receiveListItem(mUser, DetailActionType.SU_DUNG_VE_SO_NHO.getKey(), bonus));
        addResponse(lResult.build());
    }

    void lotteView() {
        int pointId = getInputInt();
        if (!ItemPointKey.isLotteryTicket(pointId)) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        UserItemPointEntity row = mUser.getResources().getItemPoint(pointId);
        if (row == null) {
            addResponse(getCommonVector(new ArrayList<>()));
            return;
        }
        List<Long> lst = row.getTicketNumbersForEvent(DateTime.getNumberDay());
        addResponse(getCommonVector(lst));
    }

    void history(int inputType) {
        // type = ItemPointKey pointId: TICKER_NORMAL(3) / TICKER_SPECIAL(4)
        if (!ItemPointKey.isLotteryTicket(inputType)) {
            addErrParam();
            return;
        }
        List<UserLotteryHistoryEntity> history = DBJPA.getList("user_lottery_history",
                Arrays.asList("type", inputType, "user_id", user.getId()), "", UserLotteryHistoryEntity.class);
        Pbmethod.PbListLotteryHistory.Builder pb = Pbmethod.PbListLotteryHistory.newBuilder();
        for (UserLotteryHistoryEntity u : history) {
            pb.addALottery(u.toProto());
        }
        addResponse(IAction.LOTTERY_HISTORY, pb.build());
    }

    void receive() {
        List<Long> input = getInputALong();
        if (input == null || input.size() < 2) {
            addErrParam();
            return;
        }
        int pointId = Math.toIntExact(input.get(0));
        if (!ItemPointKey.isLotteryTicket(pointId)) {
            addErrParam();
            return;
        }
        int eventId = Math.toIntExact(input.get(1));
        List<UserLotteryHistoryEntity> history = DBJPA.getList("user_lottery_history",
                Arrays.asList("type", pointId, "event_id", eventId, "user_id", user.getId()), "",
                UserLotteryHistoryEntity.class);
        if (history == null || history.isEmpty()) {
            addErrParam();
            return;
        }
        UserLotteryHistoryEntity curItem = history.get(0);
        if (curItem.getStatus() == StatusType.LOCK.value) {
            addErrResponse(getLang(Lang.err_not_win_lottery));
            return;
        } else if (curItem.getStatus() == StatusType.DONE.value) {
            addErrResponse(getLang(Lang.err_received_bonus));
            return;
        }
        if (curItem.update(Arrays.asList("status", StatusType.DONE.value))) {
            curItem.setStatus(StatusType.DONE.value);
            List<Long> result = Bonus.receiveListItem(mUser, DetailActionType.RECEIVE_LOTTERY.getKey(eventId),
                    Bonus.merge(curItem.getBonus()));
            addBonusToast(result);
            addResponse(IAction.LOTTERY_RECEIVE, getCommonVector(result));
            flushItemPointUpdates();
        } else addErrSystem();
    }
}
