package game.dragonhero.service.resource;

import game.config.CfgServer;
import game.dragonhero.mapping.main.*;
import ozudo.base.database.DBResource;
import ozudo.base.helper.NumberUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResFarm {
    static Map<Integer, ResItemFarmEntity> mFarmItem = new HashMap<>();
    static Map<Integer, ResSeedEntity> mFarmSeed = new HashMap<>();
    static Map<Integer, List<ResSeedEntity>> mSeedByRank = new HashMap<>();
    static Map<Integer, ResItemToolEntity> mFarmTool = new HashMap<>();
    static Map<Integer, ResItemFoodEntity> mFarmFood = new HashMap<>();
    static Map<Integer, ResFarmDecoEntity> mFarmDeco = new HashMap<>();
    static Map<Integer, ResFarmQuestRequireEntity> mFarmQuest = new HashMap<>();
    public static List<ResSeedEntity> farmTree = new ArrayList<>();
    static int maxSizeDeco;

    public static ResSeedEntity getSeed(int seedId) {
        return mFarmSeed.get(seedId);
    }
    public static ResSeedEntity getTree(int index) {
        if(index>=farmTree.size() ) return  null;
        return  farmTree.get(index);
    }


    public static ResFarmQuestRequireEntity getFarmQuest(int id) {
        return mFarmQuest.get(id);
    }

    public static List<ResSeedEntity> getSeedByRank(int rank) {
        return mSeedByRank.get(rank);
    }

    public static int getRandomSeedByRank(int rank) {
        List<ResSeedEntity> seeds = getSeedByRank(rank);
        return seeds.get(NumberUtil.getRandom(seeds.size())).getId();
    }

    public static ResItemToolEntity getTool(int toolId) {
        return mFarmTool.get(toolId);
    }


    public static ResFarmDecoEntity getDeco(int decoId) {
        return mFarmDeco.get(decoId);
    }

    public static ResItemFarmEntity getItemFarm(int itemId) {
        return mFarmItem.get(itemId);
    }

    public static ResItemFoodEntity getItemFood(int itemId) {
        return mFarmFood.get(itemId);
    }


    public static int getMaxSizeDeco() {
        return maxSizeDeco;
    }

    public static void init() {
        List<ResItemFarmEntity> aFarm = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_item_farm", ResItemFarmEntity.class);
        aFarm.forEach(farm -> mFarmItem.put(farm.getId(), farm));
        List<ResSeedEntity> aSeed = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_seed", ResSeedEntity.class);
        aSeed.forEach(seed -> {
            seed.init();
            mFarmSeed.put(seed.getId(), seed);
            if (seed.getIsTree() == 0) { // Hạt giống
                if (!mSeedByRank.containsKey(seed.getRank())) mSeedByRank.put(seed.getRank(), new ArrayList<>());
                mSeedByRank.get(seed.getRank()).add(seed);
            } else { // cây
                farmTree.add(seed);
            }

        });
        List<ResItemToolEntity> aTools = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_item_tool", ResItemToolEntity.class);
        aTools.forEach(tool -> mFarmTool.put(tool.getId(), tool));
        List<ResItemFoodEntity> aFoods = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_item_food", ResItemFoodEntity.class);
        aFoods.forEach(food -> mFarmFood.put(food.getId(), food));

        //deco map
        List<ResFarmDecoEntity> aDeco = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_farm_deco", ResFarmDecoEntity.class);
        aDeco.forEach(deco -> {
            if (deco.getId() > maxSizeDeco) maxSizeDeco = deco.getId();
            mFarmDeco.put(deco.getId(), deco);
        });

        //farm quest
        List<ResFarmQuestRequireEntity> aFarmQuest = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_farm_quest_require", ResFarmQuestRequireEntity.class);
        aFarmQuest.forEach(farm -> {
            farm.init();
            mFarmQuest.put(farm.getLevel(), farm);
        });
    }

}
