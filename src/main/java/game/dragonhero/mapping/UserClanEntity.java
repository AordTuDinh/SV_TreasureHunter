package game.dragonhero.mapping;

import game.config.CfgClan;
import game.config.aEnum.StatusType;
import game.dragonhero.mapping.main.ResClanSkillEntity;
import game.dragonhero.service.resource.ResClan;
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


    public List<Integer> getDynamicDetail() {
        if (dynamicDetail == null) dynamicDetail = "[]";
        List<Integer> data = GsonUtil.strToListInt(dynamicDetail);
        while (data.size() < ResClan.aDynamicType.size()) {
            data.add(StatusType.PROCESSING.value);
        }
        return data;
    }

    public List<Integer> getSkills() {
        List<Integer> allSkill = GsonUtil.strToListInt(skills);
        boolean hasUpdate = false;
        // veryfy trước số lượng skill- sau này có thêm skill cũng k bị lỗi.
        while (allSkill.size() < ResClan.maxClanSkill) {
            allSkill.add(-1);
            hasUpdate = true;
        }
        for (int i = 0; i < allSkill.size(); i++) {
            if (allSkill.get(i) == -1) { //check unlock
                ResClanSkillEntity skill = ResClan.getClanSkill(i + 1);
                if (skill.getParentId() == 0) { // main
                    allSkill.set(i, 0);
                    hasUpdate = true;
                } else {
                    int fatherLevel = Math.toIntExact(allSkill.get(skill.getParentId() - 1));
                    if (fatherLevel >= skill.getLevelUnlocked()) {
                        allSkill.set(i, 0);
                        hasUpdate = true;
                    }
                }
            }
        }
        // update db
        if (hasUpdate) updateSkill(allSkill);
        return allSkill;
    }

    public List<Integer> getSkillsCount() {
        return GsonUtil.strToListInt(skillsCount);
    }

    public boolean updateSkill(List<Integer> skills) {
        if (update(Arrays.asList("skills", skills.toString()))) {
            this.skills = skills.toString();
            return true;
        } else return false;
    }

    public boolean upgradeSkill(List<Integer> skills, int indexClass) {
        List<Integer> counts = getSkillsCount();
        int cur = counts.get(indexClass) + 1;
        counts.set(indexClass, cur);
        if (update(Arrays.asList("skills", skills.toString(), "skills_count", StringHelper.toDBString(counts)))) {
            this.skills = skills.toString();
            this.skillsCount = counts.toString();
            return true;
        } else return false;
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

    public boolean resetSkill() {
        this.skills = NumberUtil.genListStringInt(ResClan.maxClanSkill, -1);
        this.skillsCount = NumberUtil.genListStringInt(3, 0);
        if (update(Arrays.asList("skills", skills, "skills_count", skillsCount, "first_reset", 1))) {
            return true;
        } else return false;
    }


}
