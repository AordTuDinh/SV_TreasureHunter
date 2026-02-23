package game.dragonhero.mapping;

import game.config.CfgQuest;
import game.config.aEnum.QuestType;
import game.config.aEnum.StatusType;
import game.dragonhero.mapping.main.ResQuestEntity;
import game.dragonhero.service.resource.ResQuest;
import game.object.DataQuest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.StringHelper;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

@Entity
@NoArgsConstructor
@Table(name = "user_quest")
public class UserQuestEntity implements Serializable {
    @Getter
    @Id
    int userId;
    //quest D
    String dayInt; // type,status
    String dayQuest; //dailyQuest :  id - status
    String dayStatus; // status : Bar
    // quest C
    String weekInt; // type,status
    String weekQuest; //
    String weekStatus; // status  : Bar
    String monthInt; // type,status
    String monthQuest; // id - status
    @Getter
    int point;
    @Transient
    DataQuest dataQuestD;
    @Transient
    DataQuest dataQuestC;
    @Transient
    DataQuest dataQuestMonth;


    public UserQuestEntity(int userId, int userLevel) {
        this.userId = userId;
        genNewDataQuestD(userLevel, Calendar.getInstance().get(Calendar.DAY_OF_YEAR));
        genNewDataQuestC(userLevel);
        genNewDataQuestMonth(userLevel);
    }

    public void checkData(int userLevel) {
        int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        if (dataQuestD == null) dataQuestD = new DataQuest(QuestType.QUEST_D, dayInt, userId);
        if (day != dataQuestD.getTime()) {
            genNewDataQuestD(userLevel, day);
            update(List.of("day_status", dayStatus, "day_quest", dayQuest));
        }
        int week = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR);
        if (dataQuestC == null) dataQuestC = new DataQuest(QuestType.QUEST_C, weekInt, userId);
        if (week != dataQuestC.getTime()) {
            genNewDataQuestC(userLevel);
            update(List.of("week_status", weekStatus, "week_quest", weekQuest));
        }
        int month = Calendar.getInstance().get(Calendar.MONTH);
        if (dataQuestMonth == null) dataQuestMonth = new DataQuest(QuestType.QUEST_MONTH, monthInt, userId);
        if (month != dataQuestMonth.getTime()) {
            genNewDataQuestMonth(userLevel);
            update(List.of("month_quest", monthQuest));
        }
    }


    public void addPoint(int number) {
        point += number;
    }

    void genNewDataQuestD(int userLevel, int curDay) {
        List<Integer> data = new ArrayList<>();
        List<ResQuestEntity> questToDay = ResQuest.genQuest(userLevel);
        for (int i = 0; i < questToDay.size(); i++) {
            ResQuestEntity res = questToDay.get(i);
            data.add(res.getId());
            data.add(StatusType.PROCESSING.value);
        }
        this.dayQuest = StringHelper.toDBString(data);
        List<Integer> newDay = NumberUtil.genListInt(DataQuest.NUMBER_VALUE, 0);
        newDay.set(0, curDay);
        this.dayInt = StringHelper.toDBString(newDay);
        this.dayStatus = StringHelper.toDBString(NumberUtil.genListInt(CfgQuest.numberBonusDay, StatusType.PROCESSING.value));
        this.dataQuestD = new DataQuest(QuestType.QUEST_D, dayInt, userId);
    }

    void genNewDataQuestC(int userLevel) {
        List<Integer> data = new ArrayList<>();
        List<ResQuestEntity> quests = ResQuest.genQuest(userLevel);
        for (int i = 0; i < quests.size(); i++) {
            ResQuestEntity res = quests.get(i);
            data.add(res.getId());
            data.add(StatusType.PROCESSING.value);
        }
        this.weekQuest = StringHelper.toDBString(data);
        List<Integer> newWeek = NumberUtil.genListInt(DataQuest.NUMBER_VALUE, 0);
        newWeek.set(0, Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
        this.weekInt = StringHelper.toDBString(newWeek);
        this.weekStatus = StringHelper.toDBString(NumberUtil.genListInt(CfgQuest.numberBonusWeek, StatusType.PROCESSING.value));
        this.dataQuestC = new DataQuest(QuestType.QUEST_C, weekInt, userId);
    }

    void genNewDataQuestMonth(int userLevel) {
        List<Integer> data = new ArrayList<>();
        List<ResQuestEntity> quests = ResQuest.genQuest(userLevel);
        for (int i = 0; i < quests.size(); i++) {
            ResQuestEntity res = quests.get(i);
            data.add(res.getId());
            data.add(StatusType.PROCESSING.value);
        }
        this.monthQuest = StringHelper.toDBString(data);
        List<Integer> newData = NumberUtil.genListInt(DataQuest.NUMBER_VALUE, 0);
        newData.set(0, Calendar.getInstance().get(Calendar.MONTH));
        this.monthInt = StringHelper.toDBString(newData);
        this.dataQuestMonth = new DataQuest(QuestType.QUEST_MONTH, monthInt, userId);
    }

    public List<Integer> getQuest(QuestType type) {
        if (type == QuestType.QUEST_D) return GsonUtil.strToListInt(dayQuest);
        else if (type == QuestType.QUEST_C) return GsonUtil.strToListInt(weekQuest);
        else if (type == QuestType.QUEST_MONTH) return GsonUtil.strToListInt(monthQuest);
        else return null;
    }

    public List<Integer> getStatus(QuestType type) {
        if (type == QuestType.QUEST_D) return GsonUtil.strToListInt((dayStatus));
        else return GsonUtil.strToListInt((weekStatus));
    }

    public DataQuest getDataQuest(QuestType type) {
        if (type == QuestType.QUEST_D) return dataQuestD;
        else if (type == QuestType.QUEST_C) return dataQuestC;
        else if (type == QuestType.QUEST_MONTH) return dataQuestMonth;
        else return null;
    }

    public boolean receiveQuestBonus(QuestType type, String dataQuest) {
        if (type == QuestType.QUEST_D) {
            if (update(Arrays.asList("day_quest", dataQuest))) {
                this.dayQuest = dataQuest;
                return true;
            }
            return false;
        } else if (type == QuestType.QUEST_C) {
            if (update(Arrays.asList("week_quest", dataQuest))) {
                this.weekQuest = dataQuest;
                return true;
            }
            return false;
        } else if (type == QuestType.QUEST_MONTH) {
            if (update(Arrays.asList("month_quest", dataQuest))) {
                this.monthQuest = dataQuest;
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean updateStatus(QuestType type, String barStatus) {
        if (type == QuestType.QUEST_D) {
            if (update(Arrays.asList("day_status", barStatus))) {
                this.dayStatus = barStatus;
                return true;
            }
            return false;
        } else if (type == QuestType.QUEST_C) {
            if (update(Arrays.asList("week_status", barStatus))) {
                this.weekStatus = barStatus;
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean update(List<Object> updateData) {
        List<Object> obj = new ArrayList<>(updateData);
        if (dataQuestD != null) {
            obj.add("day_int");
            obj.add(StringHelper.toDBString(dataQuestD.aInt));
        }
        if (dataQuestC != null) {
            obj.add("week_int");
            obj.add(StringHelper.toDBString(dataQuestC.aInt));
        }
        if (dataQuestMonth != null) {
            obj.add("month_int");
            obj.add(StringHelper.toDBString(dataQuestMonth.aInt));
        }
        obj.add("point");
        obj.add(point);
        return DBJPA.update("user_quest", obj, Arrays.asList("user_id", userId));
    }

}
