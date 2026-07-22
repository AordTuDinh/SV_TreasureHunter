package game.treasure.controller;

import game.config.*;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.treasure.mapping.*;
import game.treasure.mapping.main.*;
import game.treasure.server.IAction;
import game.treasure.service.Services;
import game.treasure.service.resource.ResEvent;
import game.treasure.service.resource.ResEventPanel;
import game.treasure.service.resource.ResIAP;
import game.treasure.service.resource.ResQuest;
import game.treasure.service.user.Bonus;
import game.object.DataQuest;
import io.netty.channel.Channel;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.StringHelper;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import java.util.*;
import java.util.stream.Collectors;

public class EventHandler extends AHandler {
    @Override
    public AHandler newInstance() {
        return new EventHandler();
    }

    static EventHandler instance;
    UserEventEntity uEvent;

    public static EventHandler getInstance() {
        if (instance == null) {
            instance = new EventHandler();
        }
        return instance;
    }

    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(EVENT_ACTIVE, EVENT_14D_STATUS, EVENT_14D_REWARD, EVENT_14D_RE_TICK, EVENT_TIMER_INFO, EVENT_TIMER_LIST, EVENT_TIMER_BUY, EVENT_FREE_100_STATUS, EVENT_FREE_100_REWARD, EVENT_FREE_DAME_SKIN_STATUS, EVENT_FREE_DAME_SKIN_REWARD, EVENT_COMMUNITY_STATUS, EVENT_COMMUNITY_REWARD, EVENT_7_SLIDER_REWARD, EVENT_7_STATUS, EVENT_7_REWARD, EVENT_BUY_PACK, EVENT_LIST_PACK, EVENT_FI_PU_STATUS, EVENT_GROUP_ACTIVE, EVENT_GROUP_STATUS, EVENT_GROUP_GET_CELL_MONTH);
        actions.forEach(action -> mHandler.put(action, this));
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        uEvent = mUser.getUEvent();
        if (uEvent == null) {
            addErrResponse(getLang(Lang.err_system_down));
            return;
        }
        try {
            switch (actionId) {
               // case IAction.EVENT_ACTIVE -> active();
                case IAction.EVENT_14D_STATUS -> e14DayStatus();
                case IAction.EVENT_14D_REWARD -> e14DayReward();
                case IAction.EVENT_14D_RE_TICK -> e14DayReTick();
                case IAction.EVENT_BUY_PACK -> buyPackStatus();
                case IAction.EVENT_LIST_PACK -> listPack();
                case IAction.EVENT_FI_PU_STATUS -> fipuStatus();
                case IAction.EVENT_GROUP_ACTIVE -> groupActive();
                case IAction.EVENT_GROUP_STATUS -> groupStatus();
                case IAction.EVENT_COMMUNITY_STATUS -> communityStatus();
                case IAction.EVENT_COMMUNITY_REWARD -> communityReward();
                case IAction.EVENT_FREE_100_STATUS -> free100Status();
                case IAction.EVENT_FREE_100_REWARD -> free100Reward();
                case IAction.EVENT_FREE_DAME_SKIN_STATUS -> freeDameSKin();
                case IAction.EVENT_FREE_DAME_SKIN_REWARD -> freeDameSKinReward();
                case IAction.EVENT_TIMER_LIST -> timerList();
                case IAction.EVENT_TIMER_INFO -> timerInfo();
                case IAction.EVENT_TIMER_BUY -> timerBuy();
                case IAction.EVENT_7_STATUS -> e7Status();
                case IAction.EVENT_7_REWARD -> e7Reward();
                case IAction.EVENT_7_SLIDER_REWARD -> e7SliderReward();
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    void communityStatus() {
        EventInt eInt = mUser.getUEvent().getEventInt();
        int curDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        int e1 = eInt.getValue(EventInt.EVENT_COMMUNITY_1);
        int e2 = eInt.getValue(EventInt.EVENT_COMMUNITY_2);
        if (e1 != 0 && e1 == curDay && e2 != 0 && e2 != curDay) {
            addErrResponse(getLang(Lang.err_event_done));
            return;
        }
        Pbmethod.ListCommonVector.Builder lscm = Pbmethod.ListCommonVector.newBuilder();
        for (int i = 0; i < CfgEventCommunity.config.events.size(); i++) {
            Pbmethod.CommonVector.Builder cm = Pbmethod.CommonVector.newBuilder();
            CfgEventCommunity.EventCommunity ev1 = CfgEventCommunity.config.events.get(i);
            cm.addALong(ev1.id);
            int index = i == 0 ? EventInt.EVENT_COMMUNITY_1 : EventInt.EVENT_COMMUNITY_2;
            cm.addALong(eInt.getValue(index) == 0 ? StatusType.PROCESSING.value : StatusType.DONE.value);
            cm.addAllALong(ev1.getBonus());
            cm.addAString(getLang(ev1.name));
            cm.addAString(ev1.link);
            lscm.addAVector(cm);
        }
        addResponse(lscm.build());
    }


    void communityReward() {
        EventInt eInt = mUser.getUEvent().getEventInt();
        int eventId = getInputInt();
        if (eventId != 1 && eventId != 2) {
            addErrParam();
            return;
        }
        int curDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        if ((eventId == 1 && eInt.getValue(EventInt.EVENT_COMMUNITY_1) != 0) || (eventId == 2 && eInt.getValue(EventInt.EVENT_COMMUNITY_2) != 0)) {
            addErrResponse(getLang(Lang.err_received_bonus));
            return;
        }
        int idx = eventId == 1 ? EventInt.EVENT_COMMUNITY_1 : EventInt.EVENT_COMMUNITY_2;
        if (eInt.setValueAndUpdate(idx, curDay)) {
            List<Long> bonus = Bonus.receiveListItem(mUser, DetailActionType.BONUS_COMMUNITY.getKey(eventId), CfgEventCommunity.getBonusCommunity(eventId));
            addResponse(getCommonVector(bonus));
        } else addErrSystem();
    }

    void free100Status() {
        List<Integer> status = uEvent.getEvent100Scroll();
        boolean doneEvent100 = status.stream().filter(i -> i.intValue() == StatusType.DONE.value).count() == CfgEventCommunity.SIZE_100_SCROLL && uEvent.getDayDone100Scroll() != Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        if (doneEvent100) {
            addErrResponse(getLang(Lang.err_event_done));
            return;
        }
        Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
        // check data
        if (!uEvent.checkEventFree100Scroll(status, user)) {
            addErrSystem();
            return;
        }
        // result data
        for (int i = 0; i < status.size(); i++) {
            CfgEventCommunity.EventCommunity cfg = CfgEventCommunity.config.free100Scroll.get(i);
            Pbmethod.CommonVector.Builder cm = Pbmethod.CommonVector.newBuilder();
            cm.addALong(cfg.id);
            cm.addALong(status.get(i));
            cm.addAllALong(cfg.getBonus());
            cm.addAString(getLang( cfg.name));
            pb.addAVector(cm);
        }
        addResponse(pb.build());
    }

    void free100Reward() {
        int id = getInputInt();
        if (id < 1 || id > CfgEventCommunity.SIZE_100_SCROLL) {
            addErrParam();
            return;
        }
        List<Integer> status = uEvent.getEvent100Scroll();
        if (status.get(id - 1) != StatusType.RECEIVE.value) {
            addErrParam();
            return;
        }
        status.set(id - 1, StatusType.DONE.value);
        CfgEventCommunity.EventCommunity cfg = CfgEventCommunity.config.free100Scroll.get(id - 1);
        boolean doneEvent100 = status.stream().filter(i -> i.intValue() == StatusType.DONE.value).count() == CfgEventCommunity.SIZE_100_SCROLL;
        int curDay = doneEvent100 ? Calendar.getInstance().get(Calendar.DAY_OF_YEAR) : 0;
        if (uEvent.update(List.of("status_100_scroll", StringHelper.toDBString(status), "day_done100scroll", curDay))) {
            uEvent.setStatus_100_scroll(status.toString());
            uEvent.setDayDone100Scroll(curDay);
            addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.EVENT_FREE_100_SCROLL.getKey(id), cfg.getBonus())));
        } else addErrSystem();
    }

    void timerList() {
        List<UserEventCloEntity> lstEvent = Services.userDAO.getListEventTimer(mUser);
        Pbmethod.CommonVector.Builder pb = Pbmethod.CommonVector.newBuilder();
        if (lstEvent == null || lstEvent.isEmpty()) {
            addResponse(pb.build());
            return;
        }
        for (int i = 0; i < lstEvent.size(); i++) {
            if (lstEvent.get(i).getRes() != null && lstEvent.get(i).isAlive())
                pb.addALong(lstEvent.get(i).getRes().getId());
        }
        addResponse(pb.build());
    }

    void timerInfo() {
        int packId = getInputInt();
        List<UserEventCloEntity> lstEvent = Services.userDAO.getListEventTimer(mUser);
        List<UserEventCloEntity> aEvent = lstEvent.stream().filter(e -> e.getEventId() == packId && e.isAlive()).collect(Collectors.toList());
        if (aEvent.size() == 0) {
            addErrResponse(getLang(Lang.err_event_end));
            return;
        }
        addResponse(aEvent.get(0).toProto().build());
    }


    void e7Status() {
        UserEventSevenDayEntity uEvent = Services.userDAO.getUserSevenDay(mUser);
        if (!uEvent.hasEvent()) {
            addErrResponse(getLang(Lang.err_event_end));
            return;
        }
        addResponse(CfgEventSevenDay.toProto(uEvent, mUser).build());
    }

    void e7Reward() {
        List<Long> inputs = getInputALong();
        UserEventSevenDayEntity uEvent = Services.userDAO.getUserSevenDay(mUser);
        if (!uEvent.hasEvent()) {
            addErrResponse(getLang(Lang.err_event_end));
            return;
        }
        int day = inputs.get(0).intValue();
        int tab = inputs.get(1).intValue();
        int cell = inputs.get(2).intValue();
        CfgEventSevenDay.CellItemDay cellDay = CfgEventSevenDay.panelDays.get(day).getTab(tab).cells.get(cell);
        // check status
        List<List<Integer>> status = uEvent.getStatus();
        if (status == null || status.size() < day * 4 || status.get(day * 4 + tab).size() < cell) {
            addErrParam();
            return;
        }
        int curStatus = status.get(day * 4 + tab).get(cell);
        List<Long> bonus = new ArrayList<>();
        if (tab == 3) { // buy item
            if (curStatus == StatusType.DONE.value) {
                addErrResponse(getLang(Lang.err_max_buy));
                return;
            }
            List<Long> newPrice = cellDay.getNewPrice();
            if (!newPrice.isEmpty()) {
                bonus = Bonus.reverseBonus(newPrice);
                String err = Bonus.checkMoney(mUser, bonus);
                if (err != null) {
                    addErrResponse(err);
                    return;
                }
            }
        }
        bonus.addAll(cellDay.getBonus());
        int curNum;
        if (tab == 0) {
            if (cell == 0) curNum = mUser.getUEvent().getNumBuyIap();
            else curNum = (int) DateTime.getDayDiff(uEvent.getStart(), Calendar.getInstance().getTime()) + 1;
        } else curNum = uEvent.getCurValue(day, tab, mUser);
        if (curStatus != StatusType.DONE.value && curNum >= cellDay.maxValue) {
            if (uEvent.updateStatus(day, tab, cell, cellDay.xu)) {
                addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.EVENT_7_DAY.getKey(day + "_" + tab + "_" + cell), bonus)));
            } else addErrSystem();
        } else addErrParam();
    }

    void e7SliderReward() {
        UserEventSevenDayEntity uEvent = Services.userDAO.getUserSevenDay(mUser);
        if (!uEvent.hasEvent()) {
            addErrResponse(getLang(Lang.err_event_end));
            return;
        }

        int id = getInputInt();
        CfgEventSevenDay.PosReward posReward = CfgEventSevenDay.config.posRewards.get(id);
        if (posReward == null) {
            addErrParam();
            return;
        }
        List<Integer> status = uEvent.getSlider();
        // check đã nhận chưa
        if (status.get(id) == StatusType.DONE.value) {
            addErrResponse(getLang(Lang.err_received_bonus));
            return;
        }
        // check đủ dk nhận
        if (uEvent.getPoint() >= posReward.point) {
            List<Long> bonus = posReward.getBonus();
            status.set(id, StatusType.DONE.value);
            if (uEvent.update(List.of("slider", StringHelper.toDBString(status)))) {
                uEvent.setSlider(status.toString());
                Pbmethod.ListCommonVector.Builder lsc = Pbmethod.ListCommonVector.newBuilder();
                lsc.addAVector(getCommonIntVector(status));
                lsc.addAVector(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.SLIDER_7DAY_REWARD.getKey(id), bonus)));
                addResponse(lsc.build());
            } else addErrSystem();
        } else addErrParam();
    }

    void timerBuy() {
        int packId = getInputInt();
        ResEventClockEntity rEvent = ResEvent.getResEventTimer(packId);
        if (rEvent == null) {
            addErrParam();
            return;
        }
        List<Long> fee = Bonus.reverseBonus(rEvent.getPriceLong());
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }

        List<UserEventCloEntity> lstEvent = Services.userDAO.getListEventTimer(mUser);
        List<UserEventCloEntity> aEvent = lstEvent.stream().filter(e -> e.getEventId() == packId && e.isAlive()).collect(Collectors.toList());
        if (aEvent.size() == 0) {
            addErrResponse(getLang(Lang.err_event_end));
            return;
        }
        fee.addAll(rEvent.getBonus());
        if (aEvent.get(0).update(List.of("status", StatusType.DONE.value))) {
            aEvent.get(0).setStatus(StatusType.DONE.value);
            addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.BUY_EVENT_TIMER.getKey(packId), fee)));
        } else addErrResponse();
    }

    void freeDameSKin() {
        List<Integer> status = uEvent.getEventDameSkinFree();
        boolean doneEventDmS = status.stream().filter(i -> i.intValue() == StatusType.DONE.value).count() == CfgEventCommunity.SIZE_DAME_SKIN_FREE && uEvent.getDayDoneFreeDameSkin() != Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        if (doneEventDmS) {
            addErrResponse(getLang(Lang.err_event_done));
            return;
        }
        Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
        // check data
        if (!uEvent.checkEventFreeDameSkin(status, user)) {
            addErrSystem();
            return;
        }
        // result data
        for (int i = 0; i < status.size(); i++) {
            CfgEventCommunity.EventCommunity cfg = CfgEventCommunity.config.freeDameSkin.get(i);
            Pbmethod.CommonVector.Builder cm = Pbmethod.CommonVector.newBuilder();
            cm.addALong(cfg.id);
            cm.addALong(status.get(i));
            cm.addAllALong(cfg.getBonus());
            cm.addAString(getLang(cfg.name));
            pb.addAVector(cm);
        }
        addResponse(pb.build());
    }

    void freeDameSKinReward() {
        int id = getInputInt();
        if (id < 1 || id > CfgEventCommunity.SIZE_DAME_SKIN_FREE) {
            addErrParam();
            return;
        }
        List<Integer> status = uEvent.getEventDameSkinFree();
        if (status.get(id - 1) != StatusType.RECEIVE.value) {
            addErrParam();
            return;
        }
        status.set(id - 1, StatusType.DONE.value);
        CfgEventCommunity.EventCommunity cfg = CfgEventCommunity.config.freeDameSkin.get(id - 1);
        boolean doneEvent = status.stream().filter(i -> i.intValue() == StatusType.DONE.value).count() == CfgEventCommunity.SIZE_DAME_SKIN_FREE;
        int curDay = doneEvent ? Calendar.getInstance().get(Calendar.DAY_OF_YEAR) : 0;
        if (uEvent.update(List.of("dame_skin_free", StringHelper.toDBString(status), "day_done_free_dame_skin", curDay))) {
            uEvent.setDameSkinFree(status.toString());
            uEvent.setDayDoneFreeDameSkin(curDay);
            addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.EVENT_FREE_DAME_SKIN.getKey(id), cfg.getBonus())));
        } else addErrSystem();
    }


    void active() {
        Pbmethod.CommonVector.Builder builder = Pbmethod.CommonVector.newBuilder();
        if (CfgEvent.hasEventE14(uEvent)) {
            builder.addALong(EventType.LOGIN_14.value);
        }
        EventInt eInt = uEvent.getEventInt();
        // tat tam event fipu
        if (eInt.getValue(EventInt.TIME_BUY_FIRST_PURCHASE) == 0 || DateTime.getDayToNumberDay(eInt.getValue(EventInt.TIME_BUY_FIRST_PURCHASE)) < 3)
            builder.addALong(EventType.FIRST_PURCHASE.value);
        // event 7 day
        builder.addALong(EventType.OPEN_SV_7_DAY.value);
        // event cong dong
        boolean inEventCommunity = eInt.getValue(EventInt.EVENT_COMMUNITY_1) == 0 || eInt.getValue(EventInt.EVENT_COMMUNITY_2) == 0;
        if (inEventCommunity) builder.addALong(EventType.COMMUNITY.value);

        // event 100 scroll
        List<Integer> status = uEvent.getEvent100Scroll();
        boolean doneEvent100 = status.stream().filter(i -> i.intValue() == StatusType.DONE.value).count() == CfgEventCommunity.SIZE_100_SCROLL && uEvent.getDayDone100Scroll() != Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        if (!doneEvent100) builder.addALong(EventType.FREE_100_SCROLL.value);

        // event free dame skin
        List<Integer> infoEventFreeDameSkin = uEvent.getEventDameSkinFree();
        boolean doneEDms = infoEventFreeDameSkin.stream().filter(i -> i.intValue() == StatusType.DONE.value).count() == CfgEventCommunity.SIZE_DAME_SKIN_FREE && uEvent.getDayDoneFreeDameSkin() != Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        if (!doneEDms) builder.addALong(EventType.FREE_DAME_SKIN.value);
        // event 7 ngày mở sv
        UserEventSevenDayEntity u7Day = Services.userDAO.getUserSevenDay(mUser);
        if (u7Day.hasEvent()) {
            builder.addALong(EventType.EVENT_SEVEN_DAY.value);
        }
        // result
        addResponse(builder.build());
    }


    void e14DayStatus() { // 0: khóa, 1 chưa nhận, 2 đã nhận
        Pbmethod.CommonVector.Builder builder = Pbmethod.CommonVector.newBuilder();
        boolean hasEvent = CfgEvent.hasEventE14(uEvent);
        builder.addALong(hasEvent ? CfgEvent.timeRemainE14(uEvent) : 0);
        builder.addALong(DateTime.getDayDiff(uEvent.getStart_14_day(), Calendar.getInstance().getTime()));
        builder.addAllALong(GsonUtil.toListLong(uEvent.getStatusE14()));
        addResponse(builder.build());
    }


    void e14DayReward() {
        int slot = getInputInt();
        if (slot < 0 && slot > 14) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        List<Integer> status = uEvent.getStatusE14();
        int curCheck = (int) CfgEvent.dayDifE14(uEvent);
        if (status.get(slot) != StatusType.RECEIVE.value || curCheck > slot) {
            addErrResponse(getLang(Lang.err_no_bonus));
            return;
        }
        List<Long> bonus = Bonus.receiveListItem(mUser, DetailActionType.EVENT_14_DAY.getKey(slot), CfgEvent.config.bonus14day.get(slot));
        if (bonus.isEmpty()) {
            addErrResponse();
            return;
        }
        status.set(slot, StatusType.DONE.value);
        if (uEvent.updateStatus14(status)) {
            protocol.Pbmethod.ListCommonVector.Builder pb = protocol.Pbmethod.ListCommonVector.newBuilder();
            pb.addAVector(Pbmethod.CommonVector.newBuilder());
            addBonusToastPlus(bonus);
            pb.addAVector(getCommonIntVector(status));
            addResponse(pb.build());
        } else addErrResponse();
    }

    void e14DayReTick() {
        int slot = getInputInt();
        if (slot < 0 && slot > 13) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        List<Integer> status = uEvent.getStatusE14();
        int curCheck = (int) CfgEvent.dayDifE14(uEvent);

        if (status.get(slot) != StatusType.DONE.value && curCheck > slot) {
            List<Long> bonus = CfgEvent.getFeeRetick();
            String err = Bonus.checkMoney(mUser, bonus);
            if (err != null) {
                addErrResponse(err);
                return;
            }
            bonus.addAll(CfgEvent.config.bonus14day.get(slot));
            bonus = Bonus.receiveListItem(mUser, DetailActionType.EVENT_14_DAY_RE_TICK.getKey(slot), bonus);
            status.set(slot, StatusType.DONE.value);
            if (uEvent.updateStatus14(status)) {
                protocol.Pbmethod.ListCommonVector.Builder pb = protocol.Pbmethod.ListCommonVector.newBuilder();
                pb.addAVector(Pbmethod.CommonVector.newBuilder());
                addBonusToastPlus(bonus);
                pb.addAVector(getCommonIntVector(status));
                addResponse(pb.build());
            } else addErrResponse(getLang(Lang.err_system_down));
        } else addErrResponse(getLang(Lang.err_params));
    }


    private void buyPackStatus() {
        Pbmethod.CommonVector cmm = parseCommonVector(requestData);
        int id = (int) cmm.getALong(0);
        ResPackEntity rPack = ResEvent.getResPack(id);
        if (rPack == null) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        UserPackEntity curPack = mUser.getResources().getPack(id);
        if (curPack != null && curPack.hasHSD() && curPack.getNumber() >= rPack.getLimit()) {
            addErrResponse(getLang(Lang.err_limit_pack_buy));
            return;
        }

        // check có được mua hay k
        switch (PackType.get(id)) {
            case SIEU_GIA_TRI, UU_DAI, CAO_CAP -> {
                List<Integer> uuDai = mUser.getUserDaily().getUuDaiHangNgay();
                int index = CfgEvent.getIndexUuDai(id);
                if (uuDai.get(index) != StatusType.PROCESSING.value) {
                    addErrParam();
                    return;
                }
            }
            case AFK_ADD_TIME -> {

            }
        }
        // mua bằng ruby
        List<Long> aBonus = Bonus.reverseBonus(rPack.getPrice());
        String err = Bonus.checkMoney(mUser, Objects.requireNonNull(aBonus));
        if (err != null) {
            addErrResponse(err);
            return;
        }
        //System.out.println("curPack = " + curPack);
        if (curPack == null)
            curPack = UserPackEntity.builder().userId(user.getId()).packId(id).serverId(user.getServer()).number(0).build();

        // Hết hạn / chưa có HSD → mua lại bắt đầu từ 1, không cộng number cũ
        boolean stillActive = curPack.hasHSD();
        int nextNumber = stillActive ? curPack.getNumber() + 1 : 1;

        if (curPack.buyPack(mUser, nextNumber)) {
            switch (PackType.get(id)) {
//                case AFK_ADD_TIME, THE_THANG -> {
//                    UserAfkEntity uAfk = Services.userDAO.getUserAfk(mUser);
//                    List<Integer> data = GsonUtil.strToListInt(rPack.getStringData());
//                    uAfk.updateTimeFullBonus(data.get(0));
//                }
//                case THE_VINH_VIEN, THE_TUAN -> {
//                    UserAfkEntity uAfk = Services.userDAO.getUserAfk(mUser);
//                    List<Integer> data = GsonUtil.strToListInt(rPack.getStringData());
//                    uAfk.updatePerBonus(data.get(0));
//                }
                case SIEU_CAP -> {
                    UserEventEntity uEvent = mUser.getUEvent();
                    List<Integer> uuDai = mUser.getUserDaily().getUuDaiHangNgay();
                    for (int i = 0; i < uuDai.size(); i++) {
                        if (uuDai.get(i) == StatusType.PROCESSING.value) uuDai.set(i, StatusType.RECEIVE.value);
                    }
                    mUser.getUserDaily().updateUuDai(uuDai);
                }
                case SIEU_GIA_TRI, UU_DAI, CAO_CAP -> {
                    UserEventEntity uEvent = mUser.getUEvent();
                    List<Integer> uuDai = mUser.getUserDaily().getUuDaiHangNgay();
                    int index = CfgEvent.getIndexUuDai(id);
                    uuDai.set(index, StatusType.DONE.value);
                    mUser.getUserDaily().updateUuDai(uuDai);
                }
            }
            List<Long> bonus = rPack.getBonus();
            bonus.addAll(checkBonusOld(curPack));
            aBonus.addAll(bonus);
            addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.BUY_PACK.getKey(id), aBonus)));
            // save vào log để thống kê
            LogBuyPackEntity pack = LogBuyPackEntity.builder().userId(user.getId()).packId(id).serverId(user.getServer()).price(rPack.getPriceString()).build();
            pack.saveLog();
        } else {
            addResponse(getCommonVector(0));
            Logs.error("EVENT_BUY_PACK:" + cmm.getALongList() + ":" + cmm.getAString(0));
        }
    }

    private List<Long> checkBonusOld(UserPackEntity pack) {
        List<Long> bonus = new ArrayList<>();
        // check nhận thêm bonus ví dụ quest b đc nhận x2 bonus thì phải bù bonus đã nhận cho nó
        if (!pack.hasHSD()) return bonus;
        return bonus;
    }

    private void listPack() {
        List<UserPackEntity> packs = mUser.getResources().getListPack();
        List<Long> status = new ArrayList<>();
        for (int i = 0; i < packs.size(); i++) {
            UserPackEntity pack = packs.get(i);
            if (!pack.hasHSD()) {
                mUser.getResources().removePack(pack.getPackId());
                continue;
            }
            status.add((long) pack.getPackId());
            ResPackEntity resPack = pack.getRes();
            if (resPack.getTime() > 0) {
                long timeRemain = (resPack.getTime() + pack.getTimeBuy()) - Calendar.getInstance().getTimeInMillis();
                status.add(timeRemain > 0 ? timeRemain / 1000 : 0L);
            } else {
                // time <= 0 (-1/-2/-3/0): còn hạn theo hasHSD, gửi 1 để client giữ pack active
                status.add(1L);
            }
        }
        addResponse(getCommonVector(status));
    }

    private void fipuStatus() {
        Pbmethod.ListCommonVector.Builder cmm = Pbmethod.ListCommonVector.newBuilder();
        List<List<Long>> bonusNextDay = ResIAP.bonusDayFirstPurchase;
        ResIAPEntity iap = ResIAP.getIAP(ResIAP.IAP_ID_FIRST_PURCHASE);
        cmm.addAVector(getCommonVector(iap.getABonus()));
        cmm.addAVector(getCommonVector(bonusNextDay.get(0)));
        cmm.addAVector(getCommonVector(bonusNextDay.get(1)));
        int timeBuy = mUser.getUEvent().getEventInt().getValue(EventInt.TIME_BUY_FIRST_PURCHASE);
        long dayGet = timeBuy == 0 ? 0 : DateTime.getDayToNumberDay(timeBuy) + 1;
        Pbmethod.CommonVector.Builder ret = Pbmethod.CommonVector.newBuilder();
        if (dayGet > 3) return;
        ret.addALong(dayGet).addALong(iap.getId()).addALong(iap.getPrice()).addALong(iap.getPriceQr()).addAString(iap.getProductIdAndroid()).addAString(iap.getProductIdIos());
        cmm.addAVector(ret);
        addResponse(cmm.build());
    }


    private void groupActive() {
        Pbmethod.PbListTab.Builder pb = Pbmethod.PbListTab.newBuilder();
        Pbmethod.PbTab.Builder packMonth = Pbmethod.PbTab.newBuilder();
        // List event chiến tập
//        List<ResEventPanelMonthEntity> panelMonth = ResEventPanel.aPanelMonth;
//        for (int i = 0; i < panelMonth.size(); i++) {
//            ResEventPanelMonthEntity panel = panelMonth.get(i);
//            if (panel != null && panel.isActive()) {
//                packMonth.setTabId(panel.getId());
//                packMonth.setEventTemplate(EventPanel.PACK_MONTH.id);
//                packMonth.setImage(panel.getTabImage());
//                packMonth.setName(panel.getTabName());
//                //todo check notify
//                packMonth.setNotify(false);
//                pb.addTabs(packMonth);
//            }
//        }
        //Tab Mục Tiêu Tháng
        Pbmethod.PbTab.Builder tabCell = Pbmethod.PbTab.newBuilder();
        tabCell.setEventTemplate(EventPanel.TAB_CELL.id);
        tabCell.setTabId(EventType.EVENT_MONTH.value);
        tabCell.setImage("/tabcell.png");
        tabCell.setName(getLang(Lang.event_monthly_goal));
        tabCell.setNotify(false);
        pb.addTabs(tabCell);
        addResponse(pb.build());
    }

    private void groupStatus() {
        List<Long> inputs = getInputALong();
        int eventId = inputs.get(0).intValue();
        EventPanel eventPanel = EventPanel.get(inputs.get(1).intValue());
        switch (eventPanel) {
            case TAB_CELL -> tabCell(eventId);
            default -> addErrParam();
        }
    }


    void tabCell(int eventId) {
        UserQuestEntity uQuest = mUser.getUQuest();
        protocol.Pbmethod.PbPanelEventTabCell.Builder event = protocol.Pbmethod.PbPanelEventTabCell.newBuilder();
        // Cell
        if (eventId == EventType.EVENT_MONTH.value) {
            // banner
            event.setEventName(getLang(Lang.event_monthly_goal_name));
            event.setImageBanner("Link");
            event.setTimeCD(DateTime.getSecondsToNextMonth());
            QuestType questType = QuestType.QUEST_MONTH;
            List<Integer> quests = uQuest.getQuest(questType);
            DataQuest dataQuest = mUser.getUQuest().getDataQuest(questType);
            for (int i = 0; i < quests.size(); i += 2) {
                protocol.Pbmethod.PbCellPanelEventTabCell.Builder cell = protocol.Pbmethod.PbCellPanelEventTabCell.newBuilder();
                cell.setId(i / 2);
                ResQuestEntity rQuest = ResQuest.mQuest.get(quests.get(i));
                cell.setCellName(rQuest.getDesc());
                cell.addAllBonus(rQuest.getBonusMonth());
                cell.setPer(dataQuest.getValue(questType.value) + "/" + rQuest.getNumberMonth());
                if (quests.get(i + 1) != StatusType.DONE.value) {
                    cell.setButtonStatus(CfgQuest.getStatus(dataQuest.getValue(rQuest.getId()), rQuest.getNumberMonth()).value);
                } else {
                    cell.setButtonStatus(StatusType.DONE.value);
                }
                event.addCells(cell);
            }
        }
        addResponse(event.build());
    }
}
