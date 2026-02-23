package game.dragonhero.mapping;


import game.config.CfgAchievement;
import game.config.aEnum.NotifyType;
import game.config.aEnum.StatusType;
import game.dragonhero.mapping.main.ResAchievementEntity;
import game.dragonhero.service.resource.ResAchievement;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@NoArgsConstructor
@Table(name = "user_achievement")
@Entity
public class UserAchievementEntity {
    @Getter
    @Id
    int userId;
    String tab1, tab2, tab3, tab4, tab5; // [curNum,status]
    @Setter
    String point;
    @Transient
    long timeUpdate;
    @Getter
    @Transient
    boolean canUpdate = false;


    public UserAchievementEntity(int userId) {
        this.userId = userId;
        this.tab1 = NumberUtil.genListStringInt(ResAchievement.maxItemTab.get(0) * 2, 0);
        // free 5 weapon nên cho nhận luôn
        List<Integer> data = NumberUtil.genListInt(ResAchievement.maxItemTab.get(1) * 2, 0);
        for (int i = 0; i < 5; i += 2) {
            data.set(i * 2, 1);
            data.set(i * 2 + 1, StatusType.RECEIVE.value);
        }
        this.tab2 = StringHelper.toDBString(data);
        this.tab3 = NumberUtil.genListStringInt(ResAchievement.maxItemTab.get(2) * 2, 0);
        this.tab4 = NumberUtil.genListStringInt(ResAchievement.maxItemTab.get(3) * 2, 0);
        this.tab5 = NumberUtil.genListStringInt(ResAchievement.maxItemTab.get(4) * 2, 0);
        this.point = NumberUtil.genListStringInt(6, 0);
    }

    public List<Integer> getAItem(int type) {
        List<Integer> ret = new ArrayList<>();
        switch (type) {
            case 1 -> ret = GsonUtil.strToListInt(tab1);
            case 2 -> ret = GsonUtil.strToListInt(tab2);
            case 3 -> ret = GsonUtil.strToListInt(tab3);
            case 4 -> ret = GsonUtil.strToListInt(tab4);
            case 5 -> ret = GsonUtil.strToListInt(tab5);
        }
        int length = ResAchievement.maxItemTab.get(type - 1) * 2;
        while (ret.size() < length) ret.add(0);
        return ret;
    }

    public List<Integer> getPoint() {
        return GsonUtil.strToListInt(point);
    }


    public List<Long> listNotify() {
        List<Long> noti = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            List<Integer> tab = getAItem(i);
            for (int j = 0; j < tab.size(); j += 2) {
                if (tab.get(j + 1) == StatusType.RECEIVE.value) {
                    if (i == 1) noti.add((long) NotifyType.ACHIEVEMENT_TAB_1.value);
                    if (i == 2) noti.add((long) NotifyType.ACHIEVEMENT_TAB_2.value);
                    if (i == 3) noti.add((long) NotifyType.ACHIEVEMENT_TAB_3.value);
                    if (i == 4) noti.add((long) NotifyType.ACHIEVEMENT_TAB_4.value);
                    if (i == 5) noti.add((long) NotifyType.ACHIEVEMENT_TAB_5.value);
                    break;
                }
            }
        }
        // full notify thi khoi check
        if (noti.size() == 5) {
            noti.add((long) NotifyType.ACHIEVEMENT.value);
            return noti;
        }
        List<Integer> points = getPoint();
        //slider 5 tab con
        int maxPoint = CfgAchievement.config.maxPoint;
        for (int i = 1; i < points.size(); i++) {
            if (points.get(i).intValue() >= maxPoint) {
                if (!noti.contains(NotifyType.ACHIEVEMENT_TAB_1.value) && i == 1)
                    noti.add((long) NotifyType.ACHIEVEMENT_TAB_1.value);
                if (!noti.contains(NotifyType.ACHIEVEMENT_TAB_2.value) && i == 2)
                    noti.add((long) NotifyType.ACHIEVEMENT_TAB_2.value);
                if (!noti.contains(NotifyType.ACHIEVEMENT_TAB_3.value) && i == 3)
                    noti.add((long) NotifyType.ACHIEVEMENT_TAB_3.value);
                if (!noti.contains(NotifyType.ACHIEVEMENT_TAB_4.value) && i == 4)
                    noti.add((long) NotifyType.ACHIEVEMENT_TAB_4.value);
                if (!noti.contains(NotifyType.ACHIEVEMENT_TAB_5.value) && i == 5)
                    noti.add((long) NotifyType.ACHIEVEMENT_TAB_5.value);
            }
        }
        if (noti.size() > 0) noti.add((long) NotifyType.ACHIEVEMENT.value);
        return noti;
    }

    public void addAchievement(int type, int id, int addNum) {
        List<Integer> tab = getAItem(type);
        int index = (id - 1) * 2;
        if (index + 1 > tab.size()) return;
        int status = tab.get(index + 1);
        ResAchievementEntity resAchievement = ResAchievement.getResAchievement(type, id);
        if (status != StatusType.DONE.value && status != StatusType.RECEIVE.value) {
            tab.set(index, tab.get(index) + addNum);
            if (tab.get(index) >= resAchievement.getNumber()) {
                tab.set(index + 1, StatusType.RECEIVE.value);
            }
            switch (type) {
                case 1 -> tab1 = tab.toString();
                case 2 -> tab2 = tab.toString();
                case 3 -> tab3 = tab.toString();
                case 4 -> tab4 = tab.toString();
                case 5 -> tab5 = tab.toString();
            }
            updateData();
        }
    }

    public void addListAchievement(int type, List<Integer> ids, int addNum) {
        List<Integer> tab = getAItem(type);
        boolean update = false;
        for (int i = 0; i < ids.size(); i++) {
            int index = (ids.get(i) - 1) * 2;
            if (index + 1 > tab.size()) return;
            int status = tab.get(index + 1);
            ResAchievementEntity resAchievement = ResAchievement.getResAchievement(type, ids.get(i));
            if (status != StatusType.DONE.value && status != StatusType.RECEIVE.value) {
                tab.set(index, tab.get(index) + addNum);
                if (tab.get(index) >= resAchievement.getNumber()) {
                    tab.set(index + 1, StatusType.RECEIVE.value);
                }
                update = true;
            }
        }
        if (update) {
            switch (type) {
                case 1 -> tab1 = tab.toString();
                case 2 -> tab2 = tab.toString();
                case 3 -> tab3 = tab.toString();
                case 4 -> tab4 = tab.toString();
                case 5 -> tab5 = tab.toString();
            }
            updateData();
        }
    }

    private void updateData() {
        canUpdate = true;
        if (DateTime.isAfterTime(timeUpdate, CfgAchievement.TIME_UPDATE)) {
            timeUpdate = System.currentTimeMillis();
            canUpdate = false;
            updateAll();
        }
    }

    public void checkData() {
        boolean update = false;
        List<Integer> aItem1 = GsonUtil.strToListInt(tab1);
        if (aItem1.size() < ResAchievement.maxItemTab.get(0) * 2) {
            aItem1.add(0);
            update = true;
        }
        List<Integer> aItem2 = GsonUtil.strToListInt(tab2);
        if (aItem2.size() < ResAchievement.maxItemTab.get(1) * 2) {
            aItem2.add(0);
            update = true;
        }
        List<Integer> aItem3 = GsonUtil.strToListInt(tab3);
        if (aItem3.size() < ResAchievement.maxItemTab.get(2) * 2) {
            aItem3.add(0);
            update = true;
        }
        List<Integer> aItem4 = GsonUtil.strToListInt(tab4);
        if (aItem4.size() < ResAchievement.maxItemTab.get(3) * 2) {
            aItem4.add(0);
            update = true;
        }
        List<Integer> aItem5 = GsonUtil.strToListInt(tab5);
        if (aItem5.size() < ResAchievement.maxItemTab.get(4) * 2) {
            aItem5.add(0);
            update = true;
        }
        if (update) {
            updateAll();
        }
        timeUpdate = System.currentTimeMillis();
    }


    public boolean update(List<Object> updateData) {
        return DBJPA.update("user_achievement", updateData, Arrays.asList("user_id", userId));
    }

    public boolean updateTab(int type, List<Integer> data, List<Integer> points) {
        String column = "";
        switch (type) {
            case 1 -> column = "tab1";
            case 2 -> column = "tab2";
            case 3 -> column = "tab3";
            case 4 -> column = "tab4";
            case 5 -> column = "tab5";
        }
        if (DBJPA.update("user_achievement", Arrays.asList(column, StringHelper.toDBString(data), "point", StringHelper.toDBString(points)), Arrays.asList("user_id", userId))) {
            setDataTab(type, data.toString());
            this.point = points.toString();
            return true;
        }
        return false;
    }

    private void setDataTab(int type, String data) {
        switch (type) {
            case 1 -> tab1 = data;
            case 2 -> tab2 = data;
            case 3 -> tab3 = data;
            case 4 -> tab4 = data;
            case 5 -> tab5 = data;
        }
    }

    public boolean updateAll() {
        return DBJPA.update(this);
    }
}
