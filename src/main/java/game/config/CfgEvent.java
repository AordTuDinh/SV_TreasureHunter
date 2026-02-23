package game.config;

import com.google.gson.Gson;
import game.config.aEnum.PackType;
import game.config.aEnum.StatusType;
import game.config.aEnum.TriggerEventTimer;
import game.config.lang.Lang;
import game.dragonhero.mapping.*;
import game.dragonhero.mapping.main.ResEventClockEntity;
import game.dragonhero.server.IAction;
import game.dragonhero.service.Services;
import game.dragonhero.service.resource.ResEvent;
import game.dragonhero.service.user.Bonus;
import game.object.DataDaily;
import game.object.MyUser;
import game.protocol.CommonProto;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.Util;
import protocol.Pbmethod;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

public class CfgEvent {

    public static DataConfig config;
    public static final int maxDayE14 = 14;
    public static int maxDayQuaNapTien;
    public static int maxQuyTruongThanh;
    public static final int uuDaiHangNgay = 3;
    static final List<Integer> slots1H = Arrays.asList(0, 1, 2, 3, 4, 5);

    public static Pbmethod.PbListTab.Builder getWelfare(MyUser mUser) {
        DataDaily uIntDaily = mUser.getUserDaily().getUDaily();
        Pbmethod.PbListTab.Builder welfare = Pbmethod.PbListTab.newBuilder();
        // Điểm danh
        welfare.addTabs(toProtoWelfare(mUser, 0, CfgEvent.isNotifyCheckin(mUser.getUData())));
        //Nhận hỗ trợ
        welfare.addTabs(toProtoWelfare(mUser,1, CfgEvent.isNotifySupport(uIntDaily)));
        // Online 1h
        welfare.addTabs(toProtoWelfare(mUser,2, CfgEvent.isNotify1H(mUser)));
        // quỹ trưởng thành
        boolean hasPack = mUser.getResources().getPack(PackType.QUY_TRUONG_THANH) != null;
        if (!hasPack || mUser.getUEvent().hasQuyTruongThanh())
            welfare.addTabs(toProtoWelfare(mUser,3, CfgEvent.isNotifyQuyTruongThanh(mUser)));
        // Ưu đãi ngày
        welfare.addTabs(toProtoWelfare(mUser,4, CfgEvent.isNotifyUuDaiNgay(uIntDaily)));
        // Đặc quyền
        welfare.addTabs(toProtoWelfare(mUser,5, CfgEvent.isNotifyDacQuyen(uIntDaily)));
        // Quà nạp tiền
        welfare.addTabs(toProtoWelfare(mUser,6, CfgEvent.isNotifyQuyNapTien(mUser.getUEvent())));
        // Quà giới hạn
        welfare.addTabs(toProtoWelfare(mUser,7, CfgEvent.isNotifyGioiHan(uIntDaily)));
        // VIP
        welfare.addTabs(toProtoWelfare(mUser,8, CfgEvent.isNotifyBonusVip(mUser.getUEvent(), mUser.getUser())));
        // Gói nông trại
        welfare.addTabs(toProtoWelfare(mUser,9, false));
        // Gift code
        welfare.addTabs(toProtoWelfare(mUser,10, false));
        return welfare;
    }


    private static Pbmethod.PbTab.Builder toProtoWelfare(MyUser mUser, int index, boolean isNotify) {
        Pbmethod.PbTab.Builder pb = Pbmethod.PbTab.newBuilder();
        Welfare wel = config.welfare.get(index);
        pb.setTabId(wel.eventId);
        pb.setName(Lang.instance(mUser).get(wel.name));
        pb.setImage(wel.image);
        pb.setNotify(isNotify);
        return pb;
    }
    //

    public static List<Long> getFeeRetick() {
        List<Long> bonus = new ArrayList<>(config.feeReTick);
        return Bonus.reverseBonus(bonus);
    }

    public static void loadConfig(String strJson) {
        config = new Gson().fromJson(strJson, DataConfig.class);
        maxQuyTruongThanh = config.bonusQuyTruongThanh.size();
        maxDayQuaNapTien = config.bonusQuaNapTien.size();
    }

    public static boolean checkSlot1H(int slot) {
        return slots1H.contains(slot);
    }

    public static boolean hasEventE14(UserEventEntity uEvent) {
        return DateTime.getDayDiff(uEvent.getStart_14_day(), Calendar.getInstance().getTime()) < config.timeExpire14Day;
    }

    public static void processTriggerEventTimer(MyUser mUser, int numCheck, TriggerEventTimer trigger) {
        List<UserEventCloEntity> lst = Services.userDAO.getListEventTimer(mUser);
        if (lst == null) lst = new ArrayList<>();
        for (var event : ResEvent.mEventTimer.entrySet()) {
            ResEventClockEntity res = event.getValue();
            UserEventCloEntity uEvent = null;
            if (res.getTriggerType() == TriggerEventTimer.TIME && res.inEvent() && res.getTriggerType() == trigger && numCheck >= res.getTriggerData().get(1)) {
                List<UserEventCloEntity> uEvents = lst.stream().filter(e -> e.getEventId() == res.getId()).toList();
                if (!uEvents.isEmpty()) uEvent = uEvents.get(0);
                if (uEvent != null) {
                    if (uEvent.getStatus() != StatusType.DONE.value) {
                        Util.sendProtoData(mUser.getChannel(), CommonProto.getCommonVector(uEvent.getEventId()), IAction.EVENT_TIMER_ACTIVE);
                        return;
                    }

                }else uEvent = new UserEventCloEntity(mUser.getUser().getId(), res);

            if (!lst.contains(uEvent)) {
                lst.add(uEvent);
                mUser.getCache().set("list_event_timer", lst);
            } else continue;
            if (DBJPA.saveOrUpdate(uEvent)) {
                Util.sendProtoData(mUser.getChannel(), CommonProto.getCommonVector(uEvent.getEventId()), IAction.EVENT_TIMER_ACTIVE);
            }
        } else{
            if (res.getTriggerType() == trigger && res.getTriggerData().get(1) == numCheck) {
                uEvent = new UserEventCloEntity(mUser.getUser().getId(), res);
            }
            if (uEvent == null) continue;
            if (!lst.contains(uEvent)) {
                lst.add(uEvent);
                mUser.getCache().set("list_event_timer", lst);
            } else continue;

            if (DBJPA.saveOrUpdate(uEvent)) {
                Util.sendProtoData(mUser.getChannel(), CommonProto.getCommonVector(uEvent.getEventId()), IAction.EVENT_TIMER_ACTIVE);
            }

        }


    }
}

public static void checkEventTimer(MyUser mUser) {

}

public static long getCDPackFarm(long timeBuy) {
    if (DateTime.numberDayPassed(timeBuy) < CfgEvent.config.timeExpirePackFarm) {
        return DateTime.numberSecondsPassed(timeBuy);
    } else return 0L;
}

public static int getIndexUuDai(int cellId) {
    int index = -1;
    if (cellId == PackType.SIEU_GIA_TRI.value) {
        index = 0;
    } else if (cellId == PackType.UU_DAI.value) {
        index = 1;
    }
    if (cellId == PackType.CAO_CAP.value) {
        index = 2;
    }
    return index;
}

public static boolean isNotifySupport(DataDaily uIntDaily) {
    // Nhận hỗ trợ
    int lunch = uIntDaily.getValue(DataDaily.EAT_LUNCH);
    if (lunch != StatusType.DONE.value && CfgEvent.haveLunch()) {
        return true;
    }
    int dinner = uIntDaily.getValue(DataDaily.EAT_DINNER);
    if (dinner != StatusType.DONE.value && CfgEvent.haveDinner()) {
        return true;

    }
    return false;
}

public static boolean isNotify1H(MyUser mUser) {
    List<Integer> data = mUser.getUserDaily().getEvent1hStatus(mUser);
    for (int i = 0; i < data.size(); i++) {
        if (data.get(i) == StatusType.RECEIVE.value) {
            return true;
        }
    }
    return false;
}

public static boolean isNotifyUuDaiNgay(DataDaily uIntDaily) {
    return uIntDaily.getValue(DataDaily.UU_DAI_NGAY_FREE) == 0;
}

public static boolean isNotifyDacQuyen(DataDaily uIntDaily) {
    return uIntDaily.getValue(DataDaily.THE_PHUC_LOI_FREE) == 0;
}

public static boolean isNotifyQuyNapTien(UserEventEntity uEvent) {
    List<Integer> quaNapTien = uEvent.getQuaNapTien();
    for (int i = 0; i < quaNapTien.size(); i++) {
        if (quaNapTien.get(i) == StatusType.RECEIVE.value) {
            return true;
        }
    }
    return false;
}

public static boolean isNotifyCheckin(UserDataEntity uData) {
    return uData.getStatusCheckIn() == 0;
}

public static boolean isNotifyGioiHan(DataDaily uIntDaily) {
    return uIntDaily.getValue(DataDaily.GIOI_HAN_FREE) == 0;
}

public static boolean isNotifyBonusVip(UserEventEntity uEvent, UserEntity user) {
    List<Integer> vips = uEvent.getVipBonus();
    for (int i = 1; i < vips.size(); i++) {
        if (user.getVip() >= i && vips.get(i) == StatusType.PROCESSING.value) {
            return true;
        }
    }
    return false;
}

public boolean hasPackFarm(long timeBuy) {
    return DateTime.numberDayPassed(timeBuy) < CfgEvent.config.timeExpirePackFarm;
}

public static long dayDifE14(UserEventEntity uEvent) {
    return DateTime.getDayDiff(uEvent.getStart_14_day(), Calendar.getInstance().getTime());
}


public static long timeRemainE14(UserEventEntity uEvent) {
    return config.timeExpire14Day * DateTime.DAY_SECOND - (Calendar.getInstance().getTime().getTime() - uEvent.getStart_14_day().getTime()) / 1000;
}

public static boolean haveLunch() {
    return LocalTime.now().isAfter(LocalTime.parse("09:00:00")) && LocalTime.now().isBefore(LocalTime.parse("13:00:00"));
}

private static boolean isNotifyQuyTruongThanh(MyUser mUser) {
    List<Integer> quyTruongThanh = mUser.getUEvent().getQuyTruongThanh();
    for (int i = 0; i < quyTruongThanh.size(); i++) {
        if (quyTruongThanh.get(i) == StatusType.PROCESSING.value && mUser.getUser().getLevel() >= CfgEvent.config.numQuyTruongThanh.get(i))
            return true;
    }
    return false;
}

public static boolean haveDinner() {
    return LocalTime.now().isAfter(LocalTime.parse("17:00:00")) && LocalTime.now().isBefore(LocalTime.parse("23:00:00"));
}

public class DataConfig {
    public List<List<Long>> bonus1hour;
    public List<List<Long>> bonus14day;
    public List<Long> feeReTick;
    public int timeExpire14Day;
    public int timeExpirePackFarm;
    public List<Long> bonusEatLunch;
    public List<Long> bonusEatDinner;
    public List<Integer> bonusUuDaiNgayFree;
    public List<Integer> bonusPhucLoi;
    public List<Integer> bonusGioiHanFree;
    public List<Integer> numQuyTruongThanh; // ngày (max)
    public List<List<Integer>> bonusQuaNapTien;
    public List<List<Integer>> bonusQuyTruongThanh;
    List<Welfare> welfare;
}

public class Welfare {
    public int eventId;
    public String name;
    public String image;
}
}
