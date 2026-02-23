package game.dragonhero.controller;

import com.google.protobuf.AbstractMessage;
import game.config.CfgAchievement;
import game.config.CfgCheckin;
import game.config.CfgEvent;
import game.config.CfgFeature;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.dragonhero.mapping.UserEventEntity;
import game.dragonhero.mapping.UserPackEntity;
import game.dragonhero.mapping.main.ResPackEntity;
import game.dragonhero.mapping.main.ResVipEntity;
import game.dragonhero.server.IAction;
import game.dragonhero.service.resource.ResEvent;
import game.dragonhero.service.user.Bonus;
import game.object.DataDaily;
import io.netty.channel.Channel;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.StringHelper;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class WelfareHandler extends AHandler {
    @Override
    public AHandler newInstance() {
        return new WelfareHandler();
    }

    static WelfareHandler instance;

    UserEventEntity uEvent;

    public static WelfareHandler getInstance() {
        if (instance == null) {
            instance = new WelfareHandler();
        }
        return instance;
    }

    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(WELFARE_ACTIVE, WELFARE_STATUS, WELFARE_GET_FREE, WELFARE_GET_CELL);
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
                case IAction.WELFARE_ACTIVE -> welfareActive();
                case IAction.WELFARE_STATUS -> welfareStatus();
                case IAction.WELFARE_GET_FREE -> welfareGetFree();
                case IAction.WELFARE_GET_CELL -> welfareGetCell();
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }


    private void welfareActive() {
        addResponse(CfgEvent.getWelfare(mUser).build());
    }

    private void welfareStatus() {
        int eventId = getInputInt();
        EventType eventType = EventType.get(eventId);
        if (eventType == null) {
            addErrParam();
            return;
        }
        switch (eventType) {
            case GET_SUPPORT -> addResponse(getSupport());
            case QUY_TRUONG_THANH -> addResponse(quyTruongThanh());
            case ONLINE_1H -> addResponse(get1Hour());
            case UU_DAI_NGAY -> addResponse(UuDaiHangNgay());
            case DIEM_DANH -> addResponse(checkin());
            case DAC_QUYEN -> addResponse(dacQuyen());
            case QUA_NAP_TIEN -> addResponse(quaNapTien());
            case QUA_GIOI_HAN -> addResponse(quaGioiHan());
            case VIP_NONG_TRAI -> addResponse(quaNongTrai());
            case VIP -> addResponse(vip());
            default -> addErrParam();
        }
    }


    private Pbmethod.PbWelfare getSupport() {
        protocol.Pbmethod.PbWelfare.Builder event = protocol.Pbmethod.PbWelfare.newBuilder();
        event.setEventId(EventType.GET_SUPPORT.value);
        //todo check notify
        DataDaily uDaily = mUser.getUserDaily().getUDaily();
        int lunch = uDaily.getValue(DataDaily.EAT_LUNCH);
        boolean notify = false;
        if (lunch != StatusType.DONE.value) {
            if (CfgEvent.haveLunch()) {
                lunch = StatusType.RECEIVE.value;
                notify = true;
            } else lunch = 0;
        }
        int dinner = uDaily.getValue(DataDaily.EAT_DINNER);
        if (dinner != StatusType.DONE.value) {
            if (CfgEvent.haveDinner()) {
                notify = true;
                dinner = StatusType.RECEIVE.value;
            } else dinner = 0;
        }
        event.setNotify(notify);
        // Tab
        protocol.Pbmethod.PbTabWelfare.Builder tab = protocol.Pbmethod.PbTabWelfare.newBuilder();
        // lunch
        protocol.Pbmethod.PbCellEvent.Builder cell = protocol.Pbmethod.PbCellEvent.newBuilder();
        cell.setId(1);
        cell.addAllBonus(GsonUtil.toListInt(CfgEvent.config.bonusEatLunch));
        cell.setNameCell(getLang(Lang.eat_lunch));
        cell.setLimit(1);
        cell.setButtonStatus(lunch);
        tab.addCells(cell);
        // dinner
        protocol.Pbmethod.PbCellEvent.Builder cell2 = protocol.Pbmethod.PbCellEvent.newBuilder();
        cell2.setId(2);
        cell2.addAllBonus(GsonUtil.toListInt(CfgEvent.config.bonusEatDinner));
        cell2.setNameCell(getLang(Lang.eat_dinner));
        cell2.setLimit(1);
        cell2.setButtonStatus(dinner);
        tab.addCells(cell2);
        event.addTabEvent(tab);
        return event.build();
    }

    private Pbmethod.PbWelfare quyTruongThanh() {
        Pbmethod.PbWelfare.Builder event = Pbmethod.PbWelfare.newBuilder();
        UserEventEntity uEvent = mUser.getUEvent();
        event.setEventId(EventType.QUY_TRUONG_THANH.value);
        // notify
        boolean notify = false;
        List<Integer> quyTruongThanh = uEvent.getQuyTruongThanh();
        // Tab
        protocol.Pbmethod.PbTabWelfare.Builder tab = protocol.Pbmethod.PbTabWelfare.newBuilder();
        for (int i = 0; i < quyTruongThanh.size(); i++) {
            protocol.Pbmethod.PbCellEvent.Builder pb = protocol.Pbmethod.PbCellEvent.newBuilder();
            pb.setId(i);
            int requireLevel = CfgEvent.config.numQuyTruongThanh.get(i);
            pb.setNameCell(getLang(Lang.has_level) + " " + requireLevel);
            pb.addAllBonus(CfgEvent.config.bonusQuyTruongThanh.get(i));
            int cacheStatus = quyTruongThanh.get(i);
            if (cacheStatus == StatusType.PROCESSING.value && requireLevel <= user.getLevel()) {
                notify = true;
                pb.setButtonStatus(StatusType.RECEIVE.value);
            } else pb.setButtonStatus(cacheStatus);
            tab.addCells(pb);
        }
        event.setNotify(notify);
        // banner
        protocol.Pbmethod.PbBannerEvent.Builder banner = protocol.Pbmethod.PbBannerEvent.newBuilder();
        banner.setPathBanner("banner_mature_fund");
        banner.setPathTitle("banner_mature_title");
        banner.setText(getLang(Lang.quy_truong_thanh));
        banner.setDesc(getLang(Lang.quy_truong_thanh2));
        event.setBanner(banner);
        //tab
        event.addTabEvent(tab);
        return event.build();
    }

    private Pbmethod.PbWelfare get1Hour() {
        protocol.Pbmethod.PbWelfare.Builder event = protocol.Pbmethod.PbWelfare.newBuilder();
        event.setEventId(EventType.ONLINE_1H.value);
        // banner
        protocol.Pbmethod.PbBannerEvent.Builder banner = protocol.Pbmethod.PbBannerEvent.newBuilder();
        int timeAdd = (int) user.getTimeLastLogin();
        timeAdd += mUser.getUserDaily().getLoginTime();
        List<Integer> data = mUser.getUserDaily().getEvent1hStatus(mUser);
        data.add(0, timeAdd);
        banner.setInfo(Pbmethod.ListCommonVector.newBuilder().addAVector(getCommonIntVector(data)));
        event.setBanner(banner);
        return event.build();
    }

    private Pbmethod.PbWelfare UuDaiHangNgay() {
        protocol.Pbmethod.PbWelfare.Builder event = protocol.Pbmethod.PbWelfare.newBuilder();
        DataDaily dataDaily = mUser.getUserDaily().getUDaily();
        event.setEventId(EventType.UU_DAI_NGAY.value);
        //event.setNotify(uEvent.checkNotifyUuDai(dataDaily));
        // banner
        protocol.Pbmethod.PbBannerEvent.Builder banner = protocol.Pbmethod.PbBannerEvent.newBuilder();
        banner.setBoxStatus(dataDaily.getValue(DataDaily.UU_DAI_NGAY_FREE) == 0 ? StatusType.RECEIVE.value : StatusType.DONE.value);
        banner.addAllBonusBox(CfgEvent.config.bonusUuDaiNgayFree);
        event.setBanner(banner);
        // Tab
        protocol.Pbmethod.PbTabWelfare.Builder tab = protocol.Pbmethod.PbTabWelfare.newBuilder();
        //1 Gói Siêu Cấp
        List<Integer> statusPack = mUser.getUserDaily().getUuDaiHangNgay();
        tab.addCells(toProtoCell(PackType.SIEU_CAP));
        tab.addCells(toProtoCellUuDai(PackType.SIEU_GIA_TRI, statusPack.get(0)));
        tab.addCells(toProtoCellUuDai(PackType.UU_DAI, statusPack.get(1)));
        tab.addCells(toProtoCellUuDai(PackType.CAO_CAP, statusPack.get(2)));
        event.addTabEvent(tab);
        return event.build();
    }

    private protocol.Pbmethod.CommonVector checkin() {
        protocol.Pbmethod.CommonVector.Builder pb = protocol.Pbmethod.CommonVector.newBuilder();
        int numCheckin = mUser.getUData().getNumCheckin().get(CfgCheckin.NUM_CHECKIN);
        pb.addALong(numCheckin);
        pb.addALong(mUser.getUData().getStatusCheckIn());
        pb.addALong(CfgCheckin.config.bonusCheckin.size());
        pb.addAString(CfgCheckin.getBonusCheckin());
        return pb.build();
    }

    private protocol.Pbmethod.PbWelfare dacQuyen() {
        protocol.Pbmethod.PbWelfare.Builder event = protocol.Pbmethod.PbWelfare.newBuilder();
        DataDaily dataDaily = mUser.getUserDaily().getUDaily();
        event.setEventId(EventType.DAC_QUYEN.value);
        //event.setNotify(dataDaily.getValue(DataDaily.THE_PHUC_LOI_FREE) == 0);
        // banner
        protocol.Pbmethod.PbBannerEvent.Builder banner = protocol.Pbmethod.PbBannerEvent.newBuilder();
        banner.setText(getLang(Lang.moi_ngay));
        banner.setBoxStatus(dataDaily.getValue(DataDaily.THE_PHUC_LOI_FREE) == 0 ? StatusType.RECEIVE.value : StatusType.DONE.value);
        banner.addAllBonusBox(CfgEvent.config.bonusPhucLoi);
        event.setBanner(banner);
        // Tab
        protocol.Pbmethod.PbTabWelfare.Builder tab = protocol.Pbmethod.PbTabWelfare.newBuilder();
        tab.addCells(toProtoCell(PackType.THE_VINH_VIEN));
        tab.addCells(toProtoCell(PackType.THE_THANG));
        tab.addCells(toProtoCell(PackType.THE_TUAN));
        event.addTabEvent(tab);
        return event.build();
    }

    private protocol.Pbmethod.PbWelfare quaNapTien() {
        protocol.Pbmethod.PbWelfare.Builder event = protocol.Pbmethod.PbWelfare.newBuilder();
        UserEventEntity uEvent = mUser.getUEvent();
        event.setEventId(EventType.QUA_NAP_TIEN.value);
        // notify
        boolean notify = false;
        List<Integer> quaNapTien = uEvent.getQuaNapTien();
        // Tab
        protocol.Pbmethod.PbTabWelfare.Builder tab = protocol.Pbmethod.PbTabWelfare.newBuilder();
        for (int i = 0; i < quaNapTien.size(); i++) {
            protocol.Pbmethod.PbCellEvent.Builder pb = protocol.Pbmethod.PbCellEvent.newBuilder();
            pb.setId(i);
            pb.addAllBonus(CfgEvent.config.bonusQuaNapTien.get(i));
            if (quaNapTien.get(i) == StatusType.RECEIVE.value) {
                notify = true;
            }
            pb.setButtonStatus(quaNapTien.get(i));
            tab.addCells(pb);
        }
        event.setNotify(notify);
        // banner
        protocol.Pbmethod.PbBannerEvent.Builder banner = protocol.Pbmethod.PbBannerEvent.newBuilder();
        banner.setPathBanner("recharge_banner");
        banner.setPathTitle("recharge_title");
        banner.setText(getLang(Lang.qua_nap_tien));
        banner.setDesc(getLang(Lang.qua_nap_tien2));
        event.setBanner(banner);
        //tab
        event.addTabEvent(tab);
        return event.build();
    }

    private protocol.Pbmethod.PbWelfare quaGioiHan() {
        protocol.Pbmethod.PbWelfare.Builder event = protocol.Pbmethod.PbWelfare.newBuilder();
        event.setEventId(EventType.QUA_GIOI_HAN.value);
        //todo check notify
        event.setNotify(mUser.getUserDaily().checkNotifyUuDai(mUser.getDataDaily()));
        // banner
        protocol.Pbmethod.PbBannerEvent.Builder banner = protocol.Pbmethod.PbBannerEvent.newBuilder();
        banner.setPathBanner("limited_gift_banner");
        banner.setPathTitle("limited_gift_title");
        banner.setText(getLang(Lang.qua_nap_tien3));
        event.setBanner(banner);
        // Tab 1
        protocol.Pbmethod.PbTabWelfare.Builder tabNgay = protocol.Pbmethod.PbTabWelfare.newBuilder();
        // free
        protocol.Pbmethod.PbCellEvent.Builder pb = protocol.Pbmethod.PbCellEvent.newBuilder();
        DataDaily dataDaily = mUser.getDataDaily();
        int numBuy = 1;
        int status = StatusType.DONE.value;
        if (dataDaily.getValue(DataDaily.GIOI_HAN_FREE) == 0) {
            numBuy = 0;
            status = StatusType.RECEIVE.value;
        }
        pb.setId(0);
        pb.addAllBonus(CfgEvent.config.bonusGioiHanFree);
        pb.setNameCell(getLang(Lang.qua_nap_tien4));
        pb.setNumBuy(numBuy);
        pb.setLimit(1);
        pb.setButtonStatus(status);
        tabNgay.addCells(pb);
        // Tab 1
        tabNgay.setTabId(1);
        tabNgay.setNotify(false);
        tabNgay.setTabName(getLang(Lang.day));
        tabNgay.addCells(toProtoCell(PackType.UU_DAI_CHIEU_MO));
        tabNgay.addCells(toProtoCell(PackType.UU_DAI_MOI_NGAY));
        tabNgay.addCells(toProtoCell(PackType.GOI_TAI_NGUYEN_TAN_THU));
        tabNgay.addCells(toProtoCell(PackType.GOI_TAI_NGUYEN_BANG_HOI));
        tabNgay.addCells(toProtoCell(PackType.GOI_TAI_NGUYEN_NONG_TRAI));
        tabNgay.addCells(toProtoCell(PackType.GOI_MAY_MAN));
        event.addTabEvent(tabNgay);
        // Tab 2

        protocol.Pbmethod.PbTabWelfare.Builder tabTuan = protocol.Pbmethod.PbTabWelfare.newBuilder();
        tabTuan.setTabId(2);
        tabTuan.setNotify(false);
        tabTuan.setTabName(getLang(Lang.week) );
        tabTuan.addCells(toProtoCell(PackType.CHIEU_MO_TUAN));
        tabTuan.addCells(toProtoCell(PackType.XU_GIA_TRI));
        tabTuan.addCells(toProtoCell(PackType.XU_MAY_MAN));
        tabTuan.addCells(toProtoCell(PackType.LIEN_MINH_GIA_TRI));
        tabTuan.addCells(toProtoCell(PackType.CUONG_HOA_CAP_1));
        tabTuan.addCells(toProtoCell(PackType.CUONG_HOA_CAP_2));
        tabTuan.addCells(toProtoCell(PackType.CUONG_HOA_CAP_3));
        tabTuan.addCells(toProtoCell(PackType.CUONG_HOA_CAP_4));
        tabTuan.addCells(toProtoCell(PackType.CUONG_HOA_CAP_5));
        event.addTabEvent(tabTuan);
        // Tab 3
        protocol.Pbmethod.PbTabWelfare.Builder tabThang = protocol.Pbmethod.PbTabWelfare.newBuilder();
        tabThang.setTabId(3);
        tabThang.setNotify(false);
        tabThang.setTabName(getLang(Lang.month));
        tabThang.addCells(toProtoCell(PackType.GOI_THANG_SO_1));
        tabThang.addCells(toProtoCell(PackType.GOI_THANG_SO_2));
        tabThang.addCells(toProtoCell(PackType.GOI_THANG_SO_3));
        tabThang.addCells(toProtoCell(PackType.GOI_THANG_SO_4));
        tabThang.addCells(toProtoCell(PackType.GOI_KI_NANG_1));
        tabThang.addCells(toProtoCell(PackType.GOI_KI_NANG_2));
        event.addTabEvent(tabThang);
        return event.build();
    }

    private Pbmethod.PbCellEvent toProtoCell(PackType packType) {
        UserPackEntity uPack = mUser.getResources().getPack(packType.value);
        ResPackEntity rPack = ResEvent.getResPack(packType);
        int buttonStatus;
        int numBuy = 0;
        long timeRemain = -1;
        if (uPack == null || !uPack.hasHSD()) {
            buttonStatus = StatusType.PROCESSING.value;
        } else {
            numBuy = uPack.getNumber();
            buttonStatus = uPack.getNumber() < rPack.getLimit() ? StatusType.PROCESSING.value : StatusType.DONE.value;
            if (rPack.getTime() > 0)
                timeRemain = (rPack.getTime() + uPack.getTimeBuy() - Calendar.getInstance().getTimeInMillis()) / 1000;
        }
        return rPack.toPbCell(mUser, numBuy, buttonStatus, timeRemain);
    }

    private Pbmethod.PbCellEvent toProtoCellUuDai(PackType packType, int status) {
        UserPackEntity uPack = mUser.getResources().getPack(packType.value);
        ResPackEntity rPack = ResEvent.getResPack(packType);
        int numBuy = 1;
        int buttonStatus = StatusType.DONE.value;
        long timeRemain = -1;
        if (status == StatusType.PROCESSING.value && (uPack == null || !uPack.hasHSD())) {
            numBuy = 0;
            buttonStatus = StatusType.PROCESSING.value;
        }
        if (status == StatusType.RECEIVE.value) {
            buttonStatus = StatusType.RECEIVE.value;
        }
        if (uPack != null && uPack.hasHSD())
            timeRemain = rPack.getTime() + uPack.getTimeBuy() - Calendar.getInstance().getTimeInMillis();
        return rPack.toPbCell(mUser, numBuy, buttonStatus, timeRemain / 1000);
    }

    private protocol.Pbmethod.PbWelfare quaNongTrai() {
        protocol.Pbmethod.PbWelfare.Builder event = protocol.Pbmethod.PbWelfare.newBuilder();
        event.setEventId(EventType.VIP_NONG_TRAI.value);
        //todo check notify
        event.setNotify(mUser.getUserDaily().checkNotifyUuDai(mUser.getDataDaily()));
        // banner
        protocol.Pbmethod.PbBannerEvent.Builder banner = protocol.Pbmethod.PbBannerEvent.newBuilder();
//        banner.setPathBanner("support_farm_banner");
//        banner.setPathTitle("support_farm_title");
//        banner.setText("Hỗ trợ nông trại <color=yellow> 500%</color>");
        event.setBanner(banner);
        // Tab
        protocol.Pbmethod.PbTabWelfare.Builder tabNgay = protocol.Pbmethod.PbTabWelfare.newBuilder();
        tabNgay.addCells(toProtoCell(PackType.FARM_MONTH));
        tabNgay.addCells(toProtoCell(PackType.FARM_BO_NPC));
        event.addTabEvent(tabNgay);
        return event.build();
    }

    private protocol.Pbmethod.PbWelfare vip() {
        protocol.Pbmethod.PbWelfare.Builder event = protocol.Pbmethod.PbWelfare.newBuilder();
        event.setEventId(EventType.VIP.value);
        event.setNotify(false);
        protocol.Pbmethod.PbTabWelfare.Builder tab = protocol.Pbmethod.PbTabWelfare.newBuilder();

        protocol.Pbmethod.PbCellEvent.Builder pb = protocol.Pbmethod.PbCellEvent.newBuilder();
        List<Integer> vips = uEvent.getVipBonus();
        boolean updateVip = false;
        // cell free
        pb.setId(0);
        if (vips.get(0) == StatusType.PROCESSING.value && vips.get(0) != StatusType.DONE.value) {
            updateVip = true;
            pb.setButtonStatus(StatusType.RECEIVE.value);
            vips.set(0, StatusType.RECEIVE.value);
        } else {
            pb.setButtonStatus(vips.get(0));
        }
        // cell vip
        tab.addCells(pb);
        for (int i = 1; i < vips.size(); i++) {
            pb = protocol.Pbmethod.PbCellEvent.newBuilder();
            pb.setId(i);
            if (user.getVip() >= i && vips.get(i) == StatusType.PROCESSING.value) {
                pb.setButtonStatus(StatusType.RECEIVE.value);
                vips.set(i, StatusType.RECEIVE.value);
                updateVip = true;
            } else {
                pb.setButtonStatus(vips.get(i));
            }
            tab.addCells(pb);
        }
        if (updateVip) uEvent.updateVipBonus(vips);
        event.addTabEvent(tab);
        return event.build();
    }

    private void welfareGetFree() {
        int eventId = getInputInt();
        DataDaily dataDaily = mUser.getDataDaily();
        switch (EventType.get(eventId)) {
            case UU_DAI_NGAY -> {
                if (dataDaily.getValue(DataDaily.UU_DAI_NGAY_FREE) != 0) {
                    addErrResponse(getLang(Lang.err_no_bonus));
                    return;
                }
                dataDaily.setValue(DataDaily.UU_DAI_NGAY_FREE, 1);
                if (dataDaily.update())
                    addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.UU_DAI_NGAY_FREE.getKey(), GsonUtil.toListLong(CfgEvent.config.bonusUuDaiNgayFree))));
                else {
                    dataDaily.setValue(DataDaily.UU_DAI_NGAY_FREE, 0);
                    addErrSystem();
                }
            }
            case DAC_QUYEN -> {
                if (dataDaily.getValue(DataDaily.THE_PHUC_LOI_FREE) != 0) {
                    addErrResponse(getLang(Lang.err_no_bonus));
                    return;
                }
                dataDaily.setValue(DataDaily.THE_PHUC_LOI_FREE, 1);
                if (dataDaily.update())
                    addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.PHUC_LOI_FREE.getKey(), GsonUtil.toListLong(CfgEvent.config.bonusPhucLoi))));
                else {
                    dataDaily.setValue(DataDaily.THE_PHUC_LOI_FREE, 0);
                    addErrSystem();
                }
            }
            case QUA_GIOI_HAN -> {
                if (dataDaily.getValue(DataDaily.GIOI_HAN_FREE) != 0) {
                    addErrResponse(getLang(Lang.err_no_bonus));
                    return;
                }
                dataDaily.setValue(DataDaily.GIOI_HAN_FREE, 1);
                if (dataDaily.update())
                    addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.GIOI_HAN_FREE.getKey(), GsonUtil.toListLong(CfgEvent.config.bonusGioiHanFree))));
                else {
                    dataDaily.setValue(DataDaily.GIOI_HAN_FREE, 0);
                    addErrSystem();
                }
            }
            default -> addErrParam();
        }

    }

    private void welfareGetCell() {
        List<Long> inputs = getInputALong();
        EventType eventType = EventType.get(inputs.get(0).intValue());
        int cellId = inputs.get(1).intValue();
        switch (eventType) {
            case QUA_NAP_TIEN -> getQuaNapTien(cellId);
            case DIEM_DANH -> checkIn();
            case UU_DAI_NGAY -> getUuDaiNgay(cellId);
            case VIP -> getVipBonus(cellId);
            case GET_SUPPORT -> getSupportBonus(cellId);
            case ONLINE_1H -> getOnline1H(cellId);
            case QUY_TRUONG_THANH -> getQuyTruongThanh(cellId);
        }
    }


    void checkIn() {
        if (!CfgFeature.isOpenFeature(FeatureType.CHECK_IN, mUser, this)) {
            return;
        }
        if (mUser.getUData().getStatusCheckIn() == 1) {
            addErrResponse(Lang.getTitle(mUser.getUser().getLang(), Lang.err_has_checkin));
            return;
        }
        List<Integer> checkin = mUser.getUData().getNumCheckin();
        int numCheck = checkin.get(CfgCheckin.NUM_CHECKIN);
        List<Long> bonus = null;
        if(numCheck>=CfgCheckin.config.bonusCheckin.size()){
            bonus = Bonus.viewGem(100);
        }else bonus = CfgCheckin.config.bonusCheckin.get(numCheck);
        checkin.set(CfgCheckin.NUM_CHECKIN, checkin.get(CfgCheckin.NUM_CHECKIN) + 1);
        checkin.set(CfgCheckin.STATUS, 1);
        if (mUser.getUData().updateCheckIn(StringHelper.toDBString(checkin))) {
            List<Long> retBonus = Bonus.receiveListItem(mUser, DetailActionType.DIEM_DANH_HANG_NGAY.getKey(numCheck + 1), bonus);
            if (retBonus.isEmpty()) {
                addErrResponse();
                return;
            }
            addBonusToast(retBonus);
            addResponse(null);
            CfgAchievement.addListAchievement(mUser, 3, CfgAchievement.checkinAchi, 1);
        } else addErrResponse();
    }

    private void getUuDaiNgay(int cellId) {
        int index = CfgEvent.getIndexUuDai(cellId);
        if (index == -1) {
            addErrParam();
            return;
        }
        List<Integer> uuDai = mUser.getUserDaily().getUuDaiHangNgay();
        if (uuDai == null || uuDai.get(index) != StatusType.RECEIVE.value) {
            addErrParam();
            return;
        }
        uuDai.set(index, StatusType.DONE.value);
        if (mUser.getUserDaily().updateUuDai(uuDai)) {
            ResPackEntity rPack = ResEvent.getResPack(cellId);
            addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.GET_UU_DAI_NGAY.getKey(cellId), rPack.getBonus())));
        } else addErrSystem();
    }

    private void getVipBonus(int cellId) {
        if (cellId < 0 || cellId > ResEvent.lengthVip) {
            addErrParam();
            return;
        }
        List<Integer> vipBonus = uEvent.getVipBonus();
        if (vipBonus == null || vipBonus.get(cellId) != StatusType.RECEIVE.value) {
            addErrParam();
            return;
        }
        vipBonus.set(cellId, StatusType.DONE.value);
        if (uEvent.updateVipBonus(vipBonus)) {
            ResVipEntity vip = ResEvent.getResVip(cellId);
            addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.VIP_BONUS.getKey(cellId), vip.getABonus())));
        } else addErrSystem();
    }

    private void getSupportBonus(int cellId) {
        if (cellId != 1 && cellId != 2) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        DataDaily uDaily = mUser.getUserDaily().getUDaily();
        int lunch = uDaily.getValue(DataDaily.EAT_LUNCH);
        if (lunch != StatusType.DONE.value) {
            lunch = CfgEvent.haveLunch() ? 1 : 0;
        }
        int dinner = uDaily.getValue(DataDaily.EAT_DINNER);
        if (dinner != StatusType.DONE.value) {
            dinner = CfgEvent.haveDinner() ? 1 : 0;
        }
        if (cellId == 1) {
            if (lunch == StatusType.DONE.value || !CfgEvent.haveLunch()) {
                addErrResponse(getLang(Lang.err_no_bonus));
                return;
            }
            uDaily.setValue(DataDaily.EAT_LUNCH, StatusType.DONE.value);
            if (uDaily.update()) {
                addResponse(null);
                addBonusToastPlus(Bonus.receiveListItem(mUser, DetailActionType.EAT_LUNCH.getKey(), CfgEvent.config.bonusEatLunch));
                mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.GET_SUPPORT, 1);
            }
        } else {
            if (dinner == StatusType.DONE.value || !CfgEvent.haveDinner()) {
                addErrResponse(getLang(Lang.err_no_bonus));
                return;
            }
            uDaily.setValue(DataDaily.EAT_DINNER, StatusType.DONE.value);
            if (uDaily.update()) {
                addResponse(null);
                addBonusToastPlus(Bonus.receiveListItem(mUser, DetailActionType.EAT_DINNER.getKey(), CfgEvent.config.bonusEatDinner));
                mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.GET_SUPPORT, 1);
            }
        }

    }

    private void getOnline1H(int slot) {
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
            addBonusToastPlus(Bonus.receiveListItem(mUser, DetailActionType.EVENT_1_HOUR.getKey(slot), CfgEvent.config.bonus1hour.get(slot)));
            addResponse(null);
            mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.GET_BONUS_ONLINE, 1);
        } else addErrResponse();
    }

    private void getQuyTruongThanh(int cellId) {
        if (cellId < 0 || cellId >= CfgEvent.maxQuyTruongThanh) {
            addErrParam();
            return;
        }
        List<Integer> quyTruongThanh = uEvent.getQuyTruongThanh();
        if (quyTruongThanh.get(cellId) == StatusType.DONE.value) {
            addErrResponse(getLang(Lang.err_received_bonus));
            return;
        }
        boolean hasPack = mUser.getResources().getPack(PackType.QUY_TRUONG_THANH) != null;
        if (!hasPack) {
            addErrResponse(getLang(Lang.err_can_buy_pack));
            return;
        }
        if (CfgEvent.config.numQuyTruongThanh.get(cellId) > user.getLevel()) {
            addErrParam();
            return;
        }
        quyTruongThanh.set(cellId, StatusType.DONE.value);
        if (uEvent.update(Arrays.asList("quy_truong_thanh", StringHelper.toDBString(quyTruongThanh)))) {
            uEvent.setQuyTruongThanh(quyTruongThanh.toString());
            addBonusToastPlus(Bonus.receiveListItem(mUser, DetailActionType.GET_QUY_TRUONG_THANH.getKey(cellId), GsonUtil.toListLong(CfgEvent.config.bonusQuyTruongThanh.get(cellId))));
            addResponse(null);
        }
    }

    private void getQuaNapTien(int cellId) {
        if (cellId < 0 || cellId >= CfgEvent.maxDayQuaNapTien) {
            addErrParam();
            return;
        }
        List<Integer> quaNapTien = uEvent.getQuaNapTien();
        if (quaNapTien.get(cellId) == StatusType.DONE.value) {
            addErrResponse(getLang(Lang.err_received_bonus));
            return;
        } else if (quaNapTien.get(cellId) == StatusType.PROCESSING.value) {
            addErrResponse(getLang(Lang.err_no_bonus));
            return;
        }
        quaNapTien.set(cellId, StatusType.DONE.value);
        if (uEvent.update(Arrays.asList("qua_nap_tien", StringHelper.toDBString(quaNapTien)))) {
            uEvent.setQuaNapTien(quaNapTien.toString());
            addBonusToastPlus(Bonus.receiveListItem(mUser, DetailActionType.GET_QUA_NAP_TIEN.getKey(cellId), GsonUtil.toListLong(CfgEvent.config.bonusQuaNapTien.get(cellId))));
            addResponse(null);
        }
    }

}
