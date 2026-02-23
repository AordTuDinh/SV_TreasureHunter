package game.config;

import com.google.gson.Gson;
import game.config.aEnum.DecoPointType;
import game.config.aEnum.ItemFarmType;
import game.config.aEnum.PackType;
import game.config.aEnum.ToolFarmType;
import game.dragonhero.mapping.UserDataEntity;
import game.dragonhero.mapping.UserLandEntity;
import game.dragonhero.mapping.UserPackEntity;
import game.dragonhero.mapping.main.ResItemToolEntity;
import game.dragonhero.mapping.main.ResSeedEntity;
import game.dragonhero.service.resource.ResFarm;
import game.dragonhero.service.user.Bonus;
import game.object.MyUser;
import ozudo.base.helper.DateTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CfgFarm {

    public static DataConfig config;
    public static final int ID_TREE = 0;
    public static final int TIME_PLANT = 1;
    public static final int TIME_HARVEST = 2;
    public static final int WATER = 3;
    public static final int FERTILIZE = 4;
    public static final int FER_TIME = 5;
    public static final int MaxSizeInfoFarm = 6;
    // data tool type fer time
    public static final int ADD_NUMBER = 1;
    public static final int ADD_PERCENT = 2;
    // max per dec time harvest
    public static final int MAX_TIME_DEC_HARVEST = 80; //giảm tối đa 80% thời gian


    public static void loadConfig(String str) {
        config = new Gson().fromJson(str, DataConfig.class);
    }

    public static boolean treeAlive(List<Integer> info) {
        return info.get(ID_TREE) != 0 && info.get(TIME_HARVEST) + DateTime.DAY_SECOND * config.dayAliveTree > DateTime.getSeconds();
    }

    public static boolean hasNPC(MyUser mUser) {
        UserPackEntity pack = mUser.getResources().getPack(PackType.FARM_MONTH);
        if (pack == null) return false;
        return pack.hasHSD();
    }

//    public static int maxSecondWater(ResSeedEntity seed) {
//        if (seed == null) return 0;
//        return (int) (config.maxPerSecondWater / 100f * seed.getTimeHarvest());
//    }

    public static int hasLevelBuyLand(int curLevel, int indexLand) {
        int requireLevel = config.requireLevel.get(indexLand);
        return curLevel >= requireLevel ? 0 : requireLevel;
    }

    public static boolean hasBoNPC(MyUser mUser) {
        UserPackEntity pack = mUser.getResources().getPack(PackType.FARM_BO_NPC);
        if (pack == null || !hasNPC(mUser)) return false;
        return pack.getTimeBuy() + pack.getRes().getTime() < Calendar.getInstance().getTimeInMillis();
    }

    public static boolean beforeHarvest(List<Integer> info) {
        return DateTime.getSeconds() < info.get(TIME_HARVEST);
    }

    public static List<Long> getBonusHarvest(UserDataEntity uData, UserLandEntity uLand, int level) {
        List<Long> ret = new ArrayList<>();
        List<Integer> info = uLand.getInfo();
        if (info.get(0) == 0) return ret;
        ResSeedEntity resSeed = ResFarm.getSeed(info.get(0));
        int numReceive = CfgFarm.checkHarvestInSeasonBuff(uData, info.get(CfgFarm.WATER) == 0, resSeed);
        int buff = info.get(CfgFarm.FERTILIZE);
        if (buff > 0) {
            ResItemToolEntity tool = ResFarm.getTool(buff);
            if (tool.getType() == ToolFarmType.FERTILIZE) {
                List<Integer> toolData = tool.getData();
                if (toolData.get(0) == CfgFarm.ADD_NUMBER) {
                    numReceive += toolData.get(1);
                } else if (toolData.get(0) == CfgFarm.ADD_PERCENT) {
                    numReceive += Math.round(toolData.get(1) * numReceive / 100);
                }
            }
        }
        ret.addAll(Bonus.viewItemFarm(ItemFarmType.AGRI, resSeed.getId(), numReceive));
        ret.addAll(bonusExpHarvest(uData, resSeed, level));
        return ret;
    }

    public static List<Long> bonusExpHarvest(UserDataEntity uData, ResSeedEntity seed, int level) {
        List<Integer> buff = uData.getFarmPoint();
        return Bonus.viewExp((long) (seed.getExp() + seed.getExp() * buff.get(DecoPointType.INC_EXP.value) / 10000f + CfgUser.getExpByLevel(level) * 0.000028f));
    }

    public static int checkHarvestInSeasonBuff(UserDataEntity uData, boolean noWater, ResSeedEntity seed) { // buff + khác season
        List<Integer> buff = uData.getFarmPoint();
        // Không tưới nước giảm 20%
        int max = noWater ? (int) (seed.getNumber() * CfgFarm.config.noWater) : seed.getNumber();
        max += buff.get(DecoPointType.INC_QUANTITY.value) / 10000f * max;
        if (!inSeason(seed)) {
            max -= max * CfgFarm.config.noSeason;
        }

        return max;
    }

    private static boolean inSeason(ResSeedEntity resSeed) {
        List<Integer> seasons = resSeed.getSeason();
        return seasons.contains(CfgServer.getSeason());
    }

    public static List<Long> getFeeBuyLand(int idLand) {
        return new ArrayList<>(config.feeBuyLand.get(idLand - 1));
    }

    public class DataConfig {
        public int maxLand;
        public int maxPerSecondWater;
        public int dayAliveTree;
        public float noWater;
        public float noSeason;
        public List<Integer> requireLevel;
        public List<List<Long>> feeBuyLand;
    }
}
