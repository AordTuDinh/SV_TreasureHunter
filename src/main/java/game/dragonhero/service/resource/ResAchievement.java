package game.dragonhero.service.resource;

import game.config.CfgServer;
import game.dragonhero.mapping.main.ResAchievementEntity;
import ozudo.base.database.DBResource;
import ozudo.base.helper.NumberUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResAchievement {
    static Map<Integer, ResAchievementEntity> mAchi1 = new HashMap<>();
    static Map<Integer, ResAchievementEntity> mAchi2 = new HashMap<>();
    static Map<Integer, ResAchievementEntity> mAchi3 = new HashMap<>();
    static Map<Integer, ResAchievementEntity> mAchi4 = new HashMap<>();
    static Map<Integer, ResAchievementEntity> mAchi5 = new HashMap<>();
    public static List<Integer> maxItemTab = new ArrayList<>();

    public static ResAchievementEntity getResAchievement(int type, int id) {
        switch (type) {
            case 1 -> {
                return mAchi1.get(id);
            }
            case 2 -> {
                return mAchi2.get(id);
            }
            case 3 -> {
                return mAchi3.get(id);
            }
            case 4 -> {
                return mAchi4.get(id);
            }
            case 5 -> {
                return mAchi5.get(id);
            }
        }
        return null;
    }

    public static void init() {
        maxItemTab = NumberUtil.genListInt(5, 0);
        List<ResAchievementEntity> aAchievement = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_achievement", ResAchievementEntity.class);
        aAchievement.forEach(achi -> {
            switch (achi.getType()) {
                case 1 -> mAchi1.put(achi.getId(), achi);
                case 2 -> mAchi2.put(achi.getId(), achi);
                case 3 -> mAchi3.put(achi.getId(), achi);
                case 4 -> mAchi4.put(achi.getId(), achi);
                case 5 -> mAchi5.put(achi.getId(), achi);
            }
            if (maxItemTab.get(achi.getType() - 1) < achi.getId()) maxItemTab.set(achi.getType() - 1, achi.getId());
        });
        maxItemTab.add(mAchi1.size() - 1);
        maxItemTab.add(mAchi2.size() - 1);
        maxItemTab.add(mAchi3.size() - 1);
        maxItemTab.add(mAchi4.size() - 1);
        maxItemTab.add(mAchi5.size() - 1);
    }
}
