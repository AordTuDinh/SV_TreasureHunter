package game.dragonhero.mapping;

import game.config.CfgEventSevenDay;
import game.config.aEnum.QuestType;
import game.config.aEnum.StatusType;
import game.dragonhero.controller.FriendHandler;
import game.dragonhero.service.Services;
import game.object.DataQuest;
import game.object.MyUser;
import lombok.Data;
import lombok.NoArgsConstructor;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


@Data
@Entity
@Table(name = "user_event_seven_day")
@NoArgsConstructor
public class UserEventSevenDayEntity implements Serializable {
    @Id
    int userId;
    Date start;
    Date end;
    int point;
    int attackBoss;
    int summonStone;
    int attackArena;
    int summonPiece;
    int buyShop;
    int summonPet;
    int buyLand;
    int ship;
    int upWeapon;
    int monster;
    String status; // List<List<Int>> :  [[status] x 4 tab] x day
    String slider; // [status x4] : các mốc nhận quà
    @Transient
    boolean genStatus;

    public UserEventSevenDayEntity(MyUser mUser) {
        this.userId = mUser.getUser().getId();
        this.start = Calendar.getInstance().getTime();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, CfgEventSevenDay.config.timeAlive);
        this.end = cal.getTime();
        this.point = 0;
        this.attackBoss = 0;
        this.summonStone = 0;
        this.attackArena = 0;
        this.summonPiece = 0;
        this.buyShop = 0;
        this.summonPet = 0;
        this.buyLand = 0;
        this.ship = 0;
        this.upWeapon = 0;
        this.monster = 0;
        this.slider = NumberUtil.genListStringInt(CfgEventSevenDay.config.posRewards.size(), StatusType.PROCESSING.value);
        List<List<Integer>> dataStatus = new ArrayList<>();
        for (int i = 0; i < CfgEventSevenDay.panelDays.size(); i++) {
            CfgEventSevenDay.PanelDay panel = CfgEventSevenDay.panelDays.get(i);
            dataStatus.add(NumberUtil.genListInt(panel.tab1.cells.size(), StatusType.PROCESSING.value));
            dataStatus.add(NumberUtil.genListInt(panel.tab2.cells.size(), StatusType.PROCESSING.value));
            dataStatus.add(NumberUtil.genListInt(panel.tab3.cells.size(), StatusType.PROCESSING.value));
            dataStatus.add(NumberUtil.genListInt(panel.tab4.cells.size(), StatusType.PROCESSING.value));
        }
        this.status = StringHelper.toDBString(dataStatus);
    }

    public List<List<Integer>> getStatus() {
        List<List<Integer>> dataStatus = GsonUtil.strTo2ListInt(status);
        if (!genStatus) {
            genStatus = true;
            boolean update = false;
            for (int i = 0; i < CfgEventSevenDay.panelDays.size(); i++) {
                CfgEventSevenDay.PanelDay panel = CfgEventSevenDay.panelDays.get(i);
                while (dataStatus.get(i * 4).size() < panel.tab1.cells.size()) {
                    dataStatus.get(i * 4).add(StatusType.PROCESSING.value);
                    update = true;
                }
                while (dataStatus.get(i * 4 + 1).size() < panel.tab2.cells.size()) {
                    dataStatus.get(i * 4 + 1).add(StatusType.PROCESSING.value);
                    update = true;
                }
                while (dataStatus.get(i * 4 + 2).size() < panel.tab3.cells.size()) {
                    dataStatus.get(i * 4 + 2).add(StatusType.PROCESSING.value);
                    update = true;
                }
                while (dataStatus.get(i * 4 + 3).size() < panel.tab4.cells.size()) {
                    dataStatus.get(i * 4 + 3).add(StatusType.PROCESSING.value);
                    update = true;
                }
            }
            if (update && update(List.of("status", StringHelper.toDBString(dataStatus)))) {
                this.status = dataStatus.toString();
            }
        }

        return dataStatus;
    }

    public boolean hasEvent() {
        return Calendar.getInstance().getTime().getTime() < end.getTime();
    }

    public boolean hasNotify(MyUser mUser) {
        if (!hasEvent()) return false;
        List<List<Integer>> aStatus = getStatus();
        int curDay = (int) DateTime.getDayDiff(start, Calendar.getInstance().getTime());
        for (int i = 0; i < CfgEventSevenDay.panelDays.size(); i++) {
            if (i > 0 && i > curDay) return false;
            for (int j = 0; j < 4; j++) {
                int curValue = getCurValue(i, j, mUser);
                List<Integer> status = aStatus.get(i * 4 + j);
                CfgEventSevenDay.TabDay tab = CfgEventSevenDay.panelDays.get(i).tab1;
                if (j == 1) tab = CfgEventSevenDay.panelDays.get(i).tab2;
                if (j == 2) tab = CfgEventSevenDay.panelDays.get(i).tab3;
                if (j == 3) tab = CfgEventSevenDay.panelDays.get(i).tab4;
                for (int k = 0; k < tab.cells.size(); k++) {
                    if (status.get(k) != StatusType.DONE.value && curValue >= tab.cells.get(k).maxValue && tab.cells.get(k).getNewPrice().isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean hasActive(int indexDay) {
        return DateTime.getDayDiff(start, Calendar.getInstance().getTime()) >= indexDay;
    }

    public List<Integer> getSlider() {
        List<Integer> values = GsonUtil.strToListInt(slider);
        while (values.size() < CfgEventSevenDay.config.posRewards.size()) values.add(StatusType.PROCESSING.value);
        return values;
    }

    public int getCurValue(int day, int panel, MyUser mUser) {
        switch (day) {
            case 0 -> {
                if (panel == 1) { // bạn bè
                    List<UserFriendRelationshipEntity> aFriends = FriendHandler.dbGetListFriends(mUser);
                    return aFriends.size();
                } else if (panel == 2) { // Đánh ải
                    return mUser.getUQuest().getDataQuest(QuestType.QUEST_MONTH).getValue(DataQuest.KILL_MONSTER);
                }
            }
            case 1 -> {
                if (panel == 1) { // Leo tháp
                    UserTowerEntity tower = Services.userDAO.getUserTower(mUser);
                    return tower.getLevel() - 1;
                } else if (panel == 2) { // Đánh boss hầm ngục
                    return attackBoss;
                }
            }
            case 2 -> {
                if (panel == 1) { // summon stone
                    return summonStone;
                } else if (panel == 2) { // attack win arena
                    return attackArena;
                }
            }
            case 3 -> {
                if (panel == 1) { // summon mảnh
                    return summonPiece;
                } else if (panel == 2) { // mua cửa hàng
                    return buyShop;
                }
            }
            case 4 -> {
                if (panel == 1) { // level
                    return mUser.getUser().getLevel();
                } else if (panel == 2) { // pet
                    return summonPet;
                }
            }
            case 5 -> {
                if (panel == 1) { // buy land
                    return buyLand;
                } else if (panel == 2) { // ship đơn hàng
                    return ship;
                }
            }
            case 6 -> {
                if (panel == 1) {  // bắt quái
                    return monster;
                } else if (panel == 2) { // Cường hóa vũ khí
                    return upWeapon;
                }
            }
        }
        return 0;
    }

    public boolean updateStatus(int day, int tab, int cellId, int pointAdd) {
        List<List<Integer>> data = getStatus();
        List<Integer> ret = data.get(day * 4 + tab);
        ret.set(cellId, StatusType.DONE.value);
        if (update(List.of("status", StringHelper.toDBString(data), "point", point + pointAdd))) {
            this.status = data.toString();
            this.point += pointAdd;
            return true;
        }
        return false;
    }

    public boolean update(List<Object> obj) {
        return DBJPA.update("user_event_seven_day", obj, List.of("user_id", userId));
    }


}
