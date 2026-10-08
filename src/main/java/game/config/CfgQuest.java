package game.config;

import com.google.gson.Gson;
import game.cache.CacheStoreBeans;
import game.config.aEnum.*;
import game.treasure.mapping.*;
import game.treasure.mapping.main.ResQuestEntity;
import game.treasure.mapping.main.ResTutorialQuestEntity;
import game.treasure.service.resource.ResQuest;
import game.object.DataQuest;
import game.object.MyUser;
import game.treasure.service.battle.PvpCupService;

import java.util.*;

public class CfgQuest {
    public static DataConfig config;
    public static int numberBonusDay = 5;
    public static int numberQuestD = 7;
    public static List<Integer> indexs = Arrays.asList(0, 1, 2, 4);

    public static void loadConfig(String strJson) {
        config = new Gson().fromJson(strJson, DataConfig.class);
    }

    public static List<Long> getQuestBonus(int index) {
        return config.aBonusQuest.get(index);
    }

    public static boolean isNotifyQuest(MyUser mUser) {
        UserQuestEntity uQuest = mUser.getUQuest();
        DataQuest quest = uQuest.getDataQuest();
        if (quest == null) return false;
        List<Integer> quests = uQuest.getQuest();
        for (int i = 0; i < quests.size(); i += 2) {
            ResQuestEntity qe = ResQuest.mQuest.get(quests.get(i));
            if (quests.get(i + 1) != StatusType.DONE.value) {
                StatusType status = CfgQuest.getStatus(quest.getValue(qe.getId()), qe.getNumber());
                if (status == StatusType.RECEIVE) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void addNumQuest(MyUser mUser, int type, int number) { // add value + check notify
        UserQuestEntity uQuest = mUser.getUQuest();
        if (uQuest.isDone()) {
            PvpCupService.tryGrantDailyQuestCup(mUser);
            return;
        }
        if (uQuest.isDoneGold() && type == DataQuest.HAVE_GOLD) return;
        if (uQuest.isDoneGem() && type == DataQuest.HAVE_GEM) return;

        ResQuestEntity quest = ResQuest.mQuest.get(type);
        if (uQuest.getDataQuest() != null) {
            DataQuest dataQuestD = uQuest.getDataQuest();
            dataQuestD.addValue(type, number);
            if (dataQuestD.aNotify.get(type) == 0 && dataQuestD.getValue(type) >= quest.getNumber()) {
                List<Integer> statusQuest = uQuest.getQuest();
                for (int i = 0; i < statusQuest.size(); i += 2) {
                    if (statusQuest.get(i) == type && statusQuest.get(i + 1) != StatusType.DONE.value) {
                        mUser.addNotify(NotifyType.QUEST_D);
                        dataQuestD.aNotify.set(type, 1);
                    }
                }
            }
        }
        //save db theo cache
        Integer cache = CacheStoreBeans.cache1Min.get(mUser.getUser().getId() + "_update_user_quest");
        uQuest.addPoint(number);
        if (cache == null) {
            CacheStoreBeans.cache1Min.add(mUser.getUser().getId() + "_update_user_quest", 1);
            uQuest.update(new ArrayList<>());
        }
        uQuest.checkDoneAllQuest();
        PvpCupService.tryGrantDailyQuestCup(mUser);
        // check notify

    }


    public static StatusType getStatus(int cur, int max) {
        return cur >= max ? StatusType.RECEIVE : StatusType.PROCESSING;
    }

    /** 0 = hết chuỗi. 1 = đang làm. 2 = đủ số, được nhận. Không tự đếm hành động. */
    public static int getQuestTutStatus(MyUser mUser, ResTutorialQuestEntity resQuest) {
        if (resQuest == null) return StatusType.LOCK.value;
        int cur = mUser.getUData().getQuestTutorialNumber();
        return cur >= resQuest.getNum() ? StatusType.RECEIVE.value : StatusType.PROCESSING.value;
    }

    // dùng cho nhiều chỗ, cẩn thận khi thay đổi
    public static boolean checkIndex(int index) {
        return indexs.contains(index);
    }

    public class DataConfig {
        public List<List<Long>> aBonusQuest;
        public List<Integer> pointState;
    }
}
