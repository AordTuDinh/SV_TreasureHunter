package game.dragonhero.service.resource;

import game.config.CfgQuest;
import game.config.CfgServer;
import game.dragonhero.mapping.main.ResQuestEntity;
import game.dragonhero.mapping.main.ResQuestBEntity;
import game.dragonhero.mapping.main.ResTutorialQuestEntity;
import ozudo.base.database.DBResource;
import ozudo.base.helper.NumberUtil;

import java.util.*;
import java.util.stream.Collectors;

public class ResQuest {
    public static Map<Integer, ResQuestEntity> mQuest = new HashMap<>();
    static List<ResQuestEntity> aQuest = new ArrayList<>();
    public static Map<Integer, List<ResQuestEntity>> mQuestType = new HashMap<>();
    // quest c
    public static Map<Integer, ResQuestBEntity> mQuestB = new HashMap<>();
    public static Map<Integer, ResTutorialQuestEntity> mTutQuest = new HashMap<>();
    static List<ResQuestBEntity> aQuestB = new ArrayList<>();

    public static void init() {
        // quest D + C
        aQuest = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_quest", ResQuestEntity.class);
        mQuest.clear();
        aQuest.forEach(quest -> {
            mQuest.put(quest.getId(), quest);
            if (!mQuestType.containsKey(quest.getType())) mQuestType.put(quest.getType(), new ArrayList<>());
            mQuestType.get(quest.getType()).add(quest);
        });
        // quest B
        aQuestB = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_quest_b", ResQuestBEntity.class);
        mQuestB.clear();
        aQuestB.forEach(quest -> {
            quest.init();
            mQuestB.put(quest.getId(), quest);
        });
        // tut quest
        List<ResTutorialQuestEntity> aTutQuest = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_tutorial_quest", ResTutorialQuestEntity.class);
        aTutQuest.forEach(tut -> {
            tut.init();
            mTutQuest.put(tut.getId(), tut);
        });
    }


    // Lấy ngẫu nhiên 10 quest thỏa mãn điều kiện lớn hơn lv nhân vật.
    public static List<ResQuestEntity> genQuest(int userLevel) {
        List<ResQuestEntity> ret = new ArrayList<>();
        // quest 0 sẽ luôn hiện
        ret.addAll(mQuestType.get(0));
        for (int i = 1; i <= 6; i++) {
            // kiểm tra level yêu cầu của nhiệm vụ
            List<ResQuestEntity> questType = mQuestType.get(i).stream().filter(quest -> userLevel >= quest.getLevel()).collect(Collectors.toList());
            ret.add(questType.get(NumberUtil.getRandom(questType.size())));
        }
//        Collections.shuffle(ret);
        // Đảm bảo luôn chỉ có 8 quest
        return ret.subList(0, CfgQuest.numberQuestD);
    }
}
