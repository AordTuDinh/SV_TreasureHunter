package game.dragonhero.mapping;


import game.config.CfgEvent;
import game.config.CfgEventCommunity;
import game.config.CfgEventDrop;
import game.config.aEnum.DetailActionType;
import game.config.aEnum.StatusType;
import game.dragonhero.controller.AHandler;
import game.dragonhero.service.resource.ResEvent;
import game.dragonhero.service.user.Bonus;
import game.object.MyUser;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.StringHelper;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.*;

@Entity
@NoArgsConstructor
@Table(name = "user_event")
public class UserEventEntity implements Serializable {
    @Getter
    @Id
    int userId;
    @Getter
    @Setter
    int firstPurchase;
    @Getter
    Date start_14_day;
    String status_14_day, eventInt;
    @Setter
    String status_100_scroll, dameSkinFree;
    @Getter
    int levelChienLenh;
    @Getter
    @Setter
    int dayDone100Scroll, dayDoneFreeDameSkin;
    @Getter
    long packFarm; // time buy pack farm
    @Setter
    String quaNapTien, vipBonus, quyTruongThanh;
    @Getter
    @Setter
    int dayBuyIap, eventDrop; // ngày nạp tiền
    @Getter
    @Setter
    int numBuyIap;// số lần nạp tiền
    @Setter
    String quest, numReset; //quest : [id,curNum,status] , numReset : [day,num]

    @Setter
    @Transient
    EventInt eInt;


    public UserEventEntity(int userId) {
        this.userId = userId;
        this.start_14_day = Calendar.getInstance().getTime();
        this.status_14_day = NumberUtil.genListStringInt(CfgEvent.maxDayE14, 0);
        this.quaNapTien = NumberUtil.genListStringInt(CfgEvent.maxDayQuaNapTien, StatusType.PROCESSING.value);
        this.quest = "[]"; // [id,curNum,status] x5
        this.vipBonus = NumberUtil.genListStringInt(ResEvent.lengthVip, StatusType.PROCESSING.value); // 4 trạng thái của 4 vip, ban đầu được nhận luôn
        this.numReset = "[]"; // day,num
        this.quyTruongThanh = NumberUtil.genListStringInt(CfgEvent.maxQuyTruongThanh, StatusType.PROCESSING.value);
        this.firstPurchase = 0;
        this.levelChienLenh = 0;
        this.dayDone100Scroll = 0;
        this.dayBuyIap = 0;
        this.eventDrop = 0;
        this.numBuyIap = 0;
        this.status_100_scroll = NumberUtil.genListStringInt(CfgEventCommunity.SIZE_100_SCROLL, StatusType.PROCESSING.value);
        this.dameSkinFree = NumberUtil.genListStringInt(CfgEventCommunity.SIZE_DAME_SKIN_FREE, StatusType.PROCESSING.value);
    }


    public EventInt getEventInt() {
        if (eInt == null) {
            eInt = new EventInt(eventInt, userId);
        }
        return eInt;
    }


    public void checkEvent(MyUser mUser) {
        if (CfgEventDrop.inEvent() && CfgEventDrop.config.getEventId() != eventDrop) {
            this.eventDrop = CfgEventDrop.config.getEventId();
            UserItemEntity uItem = mUser.getResources().getItem(CfgEventDrop.config.getItemId());
            if (uItem != null && uItem.getNumber() > 0) {
                Bonus.receiveListItem(mUser, DetailActionType.CLEAR_ITEM_EVENT_DROP.getKey(), Bonus.viewItem(uItem.getItemId(), -uItem.getNumber()));
            }
            update(List.of("event_drop", eventDrop));
        }
    }

    public List<Integer> getEvent100Scroll() {
        List<Integer> info = GsonUtil.strToListInt(status_100_scroll);
        while (info.size() < CfgEventCommunity.SIZE_100_SCROLL) {
            info.add(StatusType.PROCESSING.value);
        }
        return info;
    }


    public boolean notifyFree100Scroll(UserEntity user) {
        List<Integer> status = getEvent100Scroll();
        if (status.get(0) == StatusType.LOCK.value) status.set(0, StatusType.PROCESSING.value);
        for (int i = 0; i < CfgEventCommunity.SIZE_100_SCROLL; i++) {
            CfgEventCommunity.EventCommunity cfg = CfgEventCommunity.config.free100Scroll.get(i);
            int curStatus = status.get(i);
            // check đã mở khóa event cũ chưa
            if (i > 0 && status.get(i - 1) == StatusType.PROCESSING.value) continue;
            // đang làm
            if (curStatus == StatusType.PROCESSING.value || curStatus == StatusType.LOCK.value) {
                List<Integer> require = cfg.type;
                if (require.get(0) == CfgEventCommunity.TYPE_LEVEL && user.getLevel() >= require.get(1) || require.get(0) == CfgEventCommunity.TYPE_DAY && user.getNumDayLogin() >= require.get(1) - 1) {
                    return true;
                }
            }
            if (status.get(i) == StatusType.RECEIVE.value) return true;
        }
        return false;
    }

    public List<Integer> getEventDameSkinFree() {
        List<Integer> info = GsonUtil.strToListInt(dameSkinFree);
        while (info.size() < CfgEventCommunity.SIZE_DAME_SKIN_FREE) {
            info.add(StatusType.PROCESSING.value);
        }
        return info;
    }

    public boolean notifyFreeDameSkin(UserEntity user) {
        List<Integer> status = getEventDameSkinFree();
        if (status.get(0) == StatusType.LOCK.value) status.set(0, StatusType.PROCESSING.value);
        for (int i = 0; i < CfgEventCommunity.SIZE_DAME_SKIN_FREE; i++) {
            CfgEventCommunity.EventCommunity cfg = CfgEventCommunity.config.freeDameSkin.get(i);
            if (status.get(i) == StatusType.PROCESSING.value || status.get(i) == StatusType.LOCK.value) {// đang làm
                List<Integer> require = cfg.type;
                if (require.get(0) == CfgEventCommunity.TYPE_LEVEL && user.getLevel() >= require.get(1) || require.get(0) == CfgEventCommunity.TYPE_DAY && user.getNumDayLogin() >= require.get(1) - 1) {
                    return true;
                }
            }
            if (status.get(i) == StatusType.RECEIVE.value) return true;
        }
        return false;
    }

    public List<Integer> getVipBonus() {
        List<Integer> curVip = GsonUtil.strToListInt(vipBonus);
        boolean update = false;
        while (curVip.size() < ResEvent.lengthVip) {
            curVip.add(StatusType.PROCESSING.value);
            update = true;
        }
        if (update) updateVipBonus(curVip);
        return curVip;
    }


    public List<Integer> getQuaNapTien() {
        return GsonUtil.strToListInt(quaNapTien);
    }

    public List<Integer> getQuyTruongThanh() {
        List<Integer> lst = GsonUtil.strToListInt(quyTruongThanh);
        while (lst.size() < CfgEvent.maxQuyTruongThanh) lst.add(StatusType.PROCESSING.value);
        return lst;
    }

    public boolean hasQuyTruongThanh() {
        List<Integer> quy = getQuyTruongThanh();
        return quy.stream().filter(i -> i.intValue() == StatusType.DONE.value).count() != CfgEvent.maxQuyTruongThanh;
    }

    public List<Integer> getQuest() {
        return GsonUtil.strToListInt(quest);
    }

    public boolean notifyEventMonth() {
        List<Integer> state = getQuest();
        for (int i = 0; i < state.size(); i += 3) {
            if (state.get(i + 2) == StatusType.RECEIVE.value) return true;
        }
        return false;
    }

    public List<Integer> getStatusE14() {
        List<Integer> status = GsonUtil.strToListInt(status_14_day);
        while (status.size() < CfgEvent.maxDayE14) { // cộng thêm thằng đặc biệt
            status.add(0);
        }
        long dayDif = CfgEvent.dayDifE14(this);
        if (dayDif < CfgEvent.maxDayE14 && status.get((int) dayDif) != StatusType.DONE.value) {
            status.set((int) dayDif, StatusType.RECEIVE.value);
        }
        return status;
    }

    public boolean notifyEvent14() {
        List<Integer> data = getStatusE14();
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i) == StatusType.RECEIVE.value) return true;
        }
        return false;
    }

    public boolean hasQuaNapTien() {
        List<Integer> data = getQuaNapTien();
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i) != StatusType.DONE.value) return true;
        }
        return false;
    }

    public boolean update(List<Object> data) {
        return DBJPA.update("user_event", data, Arrays.asList("user_id", userId));
    }

    public boolean updateVipBonus(List<Integer> vipBonus) {
        if (update(Arrays.asList("vip_bonus", StringHelper.toDBString(vipBonus)))) {
            this.vipBonus = vipBonus.toString();
            return true;
        }
        return false;
    }


    public boolean updateQuestReset(String quest, String numReset) {
        if (update(Arrays.asList("quest", quest, "num_reset", numReset))) {
            this.quest = quest;
            this.numReset = numReset;
            return true;
        }
        return false;
    }

    public boolean updateQuest(List<Integer> quests) {
        if (update(Arrays.asList("quest", StringHelper.toDBString(quests)))) {
            this.quest = quests.toString();
            return true;
        }
        return false;
    }

    public boolean updateStatus14(List<Integer> status) {
        if (DBJPA.update("user_event", Arrays.asList("status_14_day", StringHelper.toDBString(status)), Arrays.asList("user_id", userId))) {
            status_14_day = status.toString();
            return true;
        }
        return false;
    }


    public boolean checkEventFree100Scroll(List<Integer> status, UserEntity user) {
        boolean update = false;
        if (status.get(0) == StatusType.LOCK.value) status.set(0, StatusType.PROCESSING.value);
        for (int i = 0; i < CfgEventCommunity.SIZE_100_SCROLL; i++) {
            CfgEventCommunity.EventCommunity cfg = CfgEventCommunity.config.free100Scroll.get(i);
            int curStatus = status.get(i);
            // check đã mở khóa event cũ chưa
            if (i > 0 && status.get(i - 1) == StatusType.PROCESSING.value) continue;
            // đang làm
            if (curStatus == StatusType.PROCESSING.value || curStatus == StatusType.LOCK.value) {
                List<Integer> require = cfg.type;
                if (require.get(0) == CfgEventCommunity.TYPE_LEVEL && user.getLevel() >= require.get(1) || require.get(0) == CfgEventCommunity.TYPE_DAY && user.getNumDayLogin() >= require.get(1) - 1) {
                    status.set(i, StatusType.RECEIVE.value);
                    update = true;
                }
            }
        }
        if (update) {
            if (update(List.of("status_100_scroll", StringHelper.toDBString(status)))) {
                setStatus_100_scroll(status.toString());
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    public boolean checkEventFreeDameSkin(List<Integer> status, UserEntity user) {
        boolean update = false;
        if (status.get(0) == StatusType.LOCK.value) status.set(0, StatusType.PROCESSING.value);
        for (int i = 0; i < CfgEventCommunity.SIZE_DAME_SKIN_FREE; i++) {
            CfgEventCommunity.EventCommunity cfg = CfgEventCommunity.config.freeDameSkin.get(i);
            if (status.get(i) == StatusType.PROCESSING.value || status.get(i) == StatusType.LOCK.value) {// đang làm
                List<Integer> require = cfg.type;
                if (require.get(0) == CfgEventCommunity.TYPE_LEVEL && user.getLevel() >= require.get(1) || require.get(0) == CfgEventCommunity.TYPE_DAY && user.getNumDayLogin() >= require.get(1) - 1) {
                    status.set(i, StatusType.RECEIVE.value);
                    update = true;
                }
            }
        }
        if (update) {
            if (update(List.of("dame_skin_free", StringHelper.toDBString(status)))) {
                setDameSkinFree(status.toString());
                return true;
            } else {
                return false;
            }
        }
        return true;
    }
}
