package game.treasure.mapping;

import game.config.CfgQuest;
import game.config.aEnum.StatusType;
import game.treasure.mapping.main.ResQuestEntity;
import game.treasure.service.resource.ResQuest;
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
    @Getter
    int point;
    @Transient
    @Getter
    DataQuest dataQuestD;


    public UserQuestEntity(int userId, int userLevel) {
        this.userId = userId;
        genNewDataQuestD(userLevel, Calendar.getInstance().get(Calendar.DAY_OF_YEAR));
    }

    public void checkData(int userLevel) {
        int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        if (dataQuestD == null) dataQuestD = new DataQuest( dayInt, userId);
        if (day != dataQuestD.getTime()) {
            genNewDataQuestD(userLevel, day);
            update(List.of("day_status", dayStatus, "day_quest", dayQuest));
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
        this.dataQuestD = new DataQuest( dayInt, userId);
    }


    public List<Integer> getQuest() {
        return GsonUtil.strToListInt(dayQuest);
    }

    public List<Integer> getStatus() {
      return   GsonUtil.strToListInt((dayStatus));
    }


    public boolean receiveQuestBonus( String dataQuest) {
        if (update(Arrays.asList("day_quest", dataQuest))) {
            this.dayQuest = dataQuest;
            return true;
        }
        return false;
    }

    public boolean updateStatus( String barStatus) {
        if (update(Arrays.asList("day_status", barStatus))) {
            this.dayStatus = barStatus;
            return true;
        }
        return false;
    }

    public boolean update(List<Object> updateData) {
        List<Object> obj = new ArrayList<>(updateData);
        if (dataQuestD != null) {
            obj.add("day_int");
            obj.add(StringHelper.toDBString(dataQuestD.aInt));
        }
        obj.add("point");
        obj.add(point);
        return DBJPA.update("user_quest", obj, Arrays.asList("user_id", userId));
    }

}
