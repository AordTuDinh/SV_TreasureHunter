package game.dragonhero.mapping;

import game.config.CfgEvent;
import game.config.aEnum.StatusType;
import game.object.DataDaily;
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
import java.util.Arrays;
import java.util.List;

@Entity
@NoArgsConstructor
@Table(name = "user_daily")
public class UserDailyEntity implements Serializable {
    @Getter
    @Id
    int userId;
    int eventId;
    @Getter
    int loginTime; // time seconds: thời gian online hôm nay
    String dataInt; // type,status
    //    String dailyQuest, status; //dailyQuest : id - status
    @Setter
    String event_1h;
    String uuDaiHangNgay; // 5 số, bắt đầu = -1, nhận q =0, còn lại là đếm sô quái tiêu diệt
    @Setter
    String friendSend, giftReceive; // danh sách những thằng mình đã gửi quà của hôm đó - danh sách các quà đã nhận
    @Transient
    DataDaily dataDaily;

    public UserDailyEntity(int userId, int userLevel) {
        this.userId = userId;
        genNewData(DateTime.getNumberDay(), false);
    }

    public void checkData() {
        int dayOfYear = DateTime.getNumberDay();
        if (dayOfYear != eventId) {
            genNewData(dayOfYear, true);
        }
    }


    void genNewData(int dayOfYear, boolean update) {
        eventId = dayOfYear;
        List<Integer> newDaily = NumberUtil.genListInt(DataDaily.NUMBER_VALUE, 0);
        dataInt = StringHelper.toDBString(newDaily);
        dataDaily = new DataDaily(dataInt, userId);
        friendSend = "[]";
        giftReceive = "[]";
        uuDaiHangNgay = NumberUtil.genListStringInt(CfgEvent.uuDaiHangNgay, StatusType.PROCESSING.value);
        event_1h = StringHelper.toDBString(NumberUtil.genListInt(CfgEvent.config.bonus1hour.size(), StatusType.PROCESSING.value));
        loginTime = 0;
        if (update) DBJPA.update(this);
    }

    public List<Integer> getEvent1hStatus(MyUser mUser) {
        int timeAdd = (int) mUser.getUser().getTimeLastLogin();
        timeAdd += mUser.getUserDaily().getLoginTime();
        List<Integer> ret = GsonUtil.strToListInt(event_1h);
        for (int i = 0; i < ret.size(); i++) {
            if (ret.get(i) != StatusType.DONE.value && timeAdd > (i + 1) * 10 * 60) {
                ret.set(i, StatusType.RECEIVE.value);
            }
        }
        return ret;
    }

    public boolean isNotifyEvent1H(MyUser mUser) {
        List<Integer> data = getEvent1hStatus(mUser);
        return data.stream().filter(i -> i == StatusType.RECEIVE.value).count() > 0;
    }

    public List<Integer> getFriendSend() {
        return GsonUtil.strToListInt(friendSend);
    }

    public List<Integer> getGirtReceive() {
        return GsonUtil.strToListInt(giftReceive);
    }

    public DataDaily getUDaily() {
        checkData();
        if (dataDaily == null) {
            dataDaily = new DataDaily(dataInt, userId);
        }
        return dataDaily;
    }

    public List<Integer> getUuDaiHangNgay() {
        return GsonUtil.strToListInt(uuDaiHangNgay);
    }

    public boolean checkNotifyUuDai(DataDaily dataDaily) {
        List<Integer> data = getUuDaiHangNgay();
        boolean notify = false;
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i) == StatusType.RECEIVE.value) {
                notify = true;
                break;
            }
        }
        return notify || dataDaily.getValue(DataDaily.UU_DAI_NGAY_FREE) == 0;
    }

    public boolean updateSendFriend(String friendSend) {
        if (update(Arrays.asList("friend_send", friendSend, "data_int", StringHelper.toDBString(dataDaily.aInt)))) {
            this.friendSend = friendSend;
            return true;
        }
        return false;
    }

    public boolean updateGiftReceive(String giftReceive) {
        if (update(Arrays.asList("gift_receive", giftReceive, "data_int", StringHelper.toDBString(dataDaily.aInt)))) {
            this.giftReceive = giftReceive;
            return true;
        }
        return false;
    }

    public boolean updateUuDai(List<Integer> uuDai) {
        if (update(Arrays.asList("uu_dai_hang_ngay", StringHelper.toDBString(uuDai)))) {
            uuDaiHangNgay = uuDai.toString();
            return true;
        }
        return false;
    }


    public boolean update(List<Object> lst) {
        return DBJPA.update("user_daily", lst, Arrays.asList("user_id", userId));
    }
}
