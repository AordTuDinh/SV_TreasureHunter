package game.dragonhero.mapping;

import game.config.CfgClan;
import game.config.aEnum.StatusType;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@Table(name = "user_clan")
public class UserClanEntity {
    @Id
    int userId;
    int clanId, server, firstReset, dayCheckin, honor;
    long contribute;
    String skills, skillsCount, dynamicReceive, boxDynamic, dynamicDetail;//dynamicReceive: week of yeah - boxDynamic: box đã nhận
    String quest; // [[star,timeDone (s) ,time,status, bonus], ...x]
    @Transient
    List<Integer> slotBoss;

    public UserClanEntity(int userId, int clanId, int server) {
        this.userId = userId;
        this.clanId = clanId;
        this.firstReset = 0;
        this.server = server;
        this.honor = 0;
        this.contribute = 0;
        this.boxDynamic = "[" + Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) + ",0]";
        this.skills = "[]";
        this.dynamicReceive = "[]";
        this.dynamicDetail = "[]";
        this.quest = "[]";
        this.skillsCount = NumberUtil.genListStringInt(3, 0);
    }


    public boolean isFirstReset() {
        return firstReset == 0;
    }

    public boolean canCheckin() {
        return DateTime.getNumberDay() != dayCheckin;
    }

    public List<Integer> getDynamicReceive() {
        return GsonUtil.strToListInt(dynamicReceive);
    }

    public List<Integer> getBoxDynamic() {
        List<Integer> data = GsonUtil.strToListInt(boxDynamic);
        if (data.get(0) != Calendar.getInstance().get(Calendar.WEEK_OF_YEAR))
            return Arrays.asList(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR), 0);
        return data;
    }

    public List<List<Long>> getQuest() {
        if (quest == null) quest = "[]";
        return GsonUtil.strTo2ListLong(quest);
    }





    public List<Integer> getSkillsCount() {
        return GsonUtil.strToListInt(skillsCount);
    }




    public boolean updateQuest(List<List<Long>> dataQuest) {
        if (update(List.of("quest", StringHelper.toDBString(dataQuest)))) {
            this.quest = dataQuest.toString();
            return true;
        }
        return false;
    }


    public boolean update(List<Object> data) {
        return DBJPA.update("user_clan", data, Arrays.asList("user_id", userId));
    }

    public void addContribute(int numBuff) {
        contribute += numBuff;
        update(List.of("contribute", contribute));
    }

    public boolean addHonor(int numHonor) {
        honor += numHonor;
        return update(List.of("honor", honor));
    }
}
