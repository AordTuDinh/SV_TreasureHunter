package game.dragonhero.service.resource;

import game.config.CfgServer;
import game.dragonhero.mapping.main.ResGoldStatEntity;
import game.dragonhero.mapping.main.ResLevelStatEntity;
import ozudo.base.database.DBResource;

import java.util.*;

public class ResStat {
    public static Map<Integer, ResGoldStatEntity> mGoldStat = new HashMap<>();
    public static List<ResGoldStatEntity> aGoldStat = new ArrayList<>();
    public static Map<Integer, ResLevelStatEntity> mLevelStat = new HashMap<>();
    public static List<ResLevelStatEntity> aLevelStat = new ArrayList<>();

    public static ResGoldStatEntity getGoldItem(int itemId) {
        return mGoldStat.get(itemId);
    }

    public static ResLevelStatEntity getLevelItem(int itemId) {
        return mLevelStat.get(itemId);
    }

    public static int countGoldStat() {
        return aGoldStat.size();
    }

    public static int countLevelStat() {
        return aLevelStat.size();
    }

    public static void init() {
        aGoldStat = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_gold_stat", Arrays.asList("enable", 1), "", ResGoldStatEntity.class);
        mGoldStat.clear();
        aGoldStat.forEach(item -> {
            item.init();
            mGoldStat.put(item.getId(), item);
        });
        aLevelStat = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_level_stat", Arrays.asList("enable", 1), "", ResLevelStatEntity.class);
        mLevelStat.clear();
        aLevelStat.forEach(item -> {
            item.init();
            mLevelStat.put(item.getId(), item);
        });
    }

    public static long getFeeItemUpgrade(int point, int curLevel, int number) {
        ResGoldStatEntity goldStat = ResStat.getGoldItem(point);
        if (goldStat == null) return -1;
        List<Float> pt2 = goldStat.getAFormular();
        int maxLevel = goldStat.getLevelMax();
        if (number + curLevel > maxLevel) number = maxLevel - curLevel;
        long energy = 0;
        for (int i = 1; i <= number; i++) {
            energy += Math.ceil(pt2.get(0) * Math.pow(curLevel + i, 2) + pt2.get(1) * (curLevel + i) + pt2.get(2));
        }
        return energy;
    }

    public static long countEnergyUpgrade(int pointId, int curLevel) {
        ResGoldStatEntity goldStat = ResStat.getGoldItem(pointId);
        if (goldStat == null) return 0;
        List<Float> pt2 = goldStat.getAFormular();
        long energy = 0;
        for (int i = 0; i <= curLevel; i++) {
            energy += Math.ceil(pt2.get(0) * Math.pow(i, 2) + pt2.get(1) * (i) + pt2.get(2));
        }
        return energy;
    }
}
