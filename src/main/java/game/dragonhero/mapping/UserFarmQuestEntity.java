package game.dragonhero.mapping;


import game.config.CfgFarmQuest;
import game.dragonhero.controller.FarmHandler;
import game.dragonhero.service.resource.ResFarm;
import game.dragonhero.service.user.Bonus;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.StringHelper;

import javax.persistence.*;
import java.util.Arrays;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "user_farm_quest")
public class UserFarmQuestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    int userId, status, questLock, star;
    String bonus, fee;
    long timeStart;

    public UserFarmQuestEntity(int userId, String bonus, int numQuest, int star, boolean isFirst) {
        this.questLock = 0;
        this.star = star;
        this.status = 0;
        this.timeStart = 0;
        this.userId = userId;
        this.bonus = bonus;
        if (isFirst) this.fee = StringHelper.toDBString(CfgFarmQuest.firstFarmQuest);
        else this.fee = StringHelper.toDBString(ResFarm.getFarmQuest(star).getRequire(numQuest));
    }

    public boolean shouldComplete() {
        return status == 1 && getTavernCoundown() == 0;
    }

    public boolean isComplete() {
        return status == 2;
    }

    public List<Long> getFee() {
        return GsonUtil.strToListLong(fee);
    }

    public List<Long> getTavernBonus() {
        return GsonUtil.strToListLong(bonus);
    }

    public long getTavernCoundown() {
        long timeRemain = 0;
        if (status != 0) {
            timeRemain = CfgFarmQuest.config.timeComplete[star - 1] - (System.currentTimeMillis() - timeStart) / 1000;
        }

        return timeRemain <= 0 ? 0 : timeRemain;
    }

    public boolean update(List<Object> data) {
        return DBJPA.update("user_farm_quest", data, Arrays.asList("id", id));
    }

    public boolean updateTavernLock() {
        int newLock = questLock == FarmHandler.QUEST_LOCK ? FarmHandler.QUEST_UNLOCK : FarmHandler.QUEST_LOCK;
        if (DBJPA.update("user_farm_quest", Arrays.asList("quest_lock", newLock), Arrays.asList("id", id))) {
            questLock = newLock;
            return true;
        }

        return false;
    }

    public boolean deleteTavern() {
        return DBJPA.delete("user_farm_quest", "id", id);
    }

}
