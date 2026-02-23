package game.config;

import com.google.gson.Gson;
import game.config.aEnum.ItemFarmType;
import game.config.aEnum.ItemKey;
import game.dragonhero.service.user.Actions;
import game.dragonhero.service.user.Bonus;
import game.object.MyUser;
import lombok.Data;
import net.sf.json.JSONArray;
import ozudo.base.helper.NumberUtil;

import java.util.*;

public class CfgFarmQuest {
    public static final int MAX_QUEST = 50;
    public static DataConfig config;
    public static final List<Long> firstFarmQuest = Bonus.viewItemFarm(ItemFarmType.AGRI, 1, -3);


    public static int getQuestFree(int curLevel) {
        for (int i = 0; i < config.questByLevel.size(); i += 2) {
            if (curLevel < config.questByLevel.get(i)) return config.questByLevel.get(i + 1);
        }
        return config.questByLevel.get(1); // min
    }

    public static int getRandomIdFarmQuest(MyUser mUser) {
        float[] rate = getRateFarmQuest(mUser.getUser().getLevel());
        float range = 0;
        for (int i = 0; i < rate.length; i++) {
            range += rate[i];
        }

        float random = new Random().nextFloat() * range;
        float top = 0;
        for (int i = 0; i < rate.length; i++) {
            top += rate[i];
            if (random < top) {
                Actions.save(mUser.getUser(), "farmQuest", "random", "randomValue", random, "star", i + 1);
                return i;
            }
        }
        return 0;
    }

    public static int getRandomIdTavernNormal(MyUser mUser) {
        float[] rate = getRateFarmQuest(mUser.getUser().getLevel());
        float range = 0;
        for (int i = 0; i < 3; i++) {
            range += rate[i];
        }

        float random = new Random().nextFloat() * range;
        float top = 0;
        for (int i = 0; i < rate.length; i++) {
            top += rate[i];
            if (random < top) {
                return i;
            }
        }
        return 0;
    }

    public static int getRandomIdTavernSenior(MyUser mUser) {
        float[] rate = getRateFarmQuest(mUser.getUser().getLevel());
        float range = 0;
        for (int i = 3; i < 7; i++) {
            range += rate[i];
        }
        float random = new Random().nextFloat() * range;
        float top = 0;
        for (int i = 3; i < rate.length; i++) {
            top += rate[i];
            if (random < top) {
                return i;
            }
        }
        return 0;
    }

    private static float[] getRateFarmQuest(int level) {
        return level < 10 ? config.farmQuestRateLvBelow10 : config.farmQuestRate;
    }

    public static JSONArray getFarmQuestBonusShow(int currIndex) {
        JSONArray arrBonus = new JSONArray();
        FarmQuestBonusObject bonusObject = null;
        if (currIndex < config.bonusFarmQuest.length) bonusObject = config.bonusFarmQuest[currIndex];
        if (bonusObject != null) {
            while (arrBonus.size() == 0) {
                int rand = NumberUtil.getRandom(12);
                switch (rand) {
                    case 0:
                        if (bonusObject.diamond.length > 0 && bonusObject.diamond[0] > 0) {
                            arrBonus.addAll(Bonus.view(Bonus.BONUS_GEM, new Random().nextInt(bonusObject.diamond[1] - bonusObject.diamond[0] + 1) + bonusObject.diamond[0]));
                        }
                        break;
                    case 1:
                        if (bonusObject.arenaCard > 0) {
                            arrBonus.addAll(Bonus.viewItem(ItemKey.ARENA_TICKET, bonusObject.arenaCard));
                        }
                        break;
                    case 2:
                        if (bonusObject.bossTicker > 0) {
                            arrBonus.addAll(Bonus.viewItem(ItemKey.BOSS_TICKET, bonusObject.bossTicker));
                        }
                        break;
                    case 3:
                        if (bonusObject.casinoChip > 0) {
                            arrBonus.addAll(Bonus.viewItem(ItemKey.CHIP, bonusObject.casinoChip));
                        }
                        break;
                    case 4:
                        if (bonusObject.toolBag > 0) {
                            arrBonus.addAll(Bonus.viewItem(ItemKey.FARM_TOOL, bonusObject.toolBag));
                        }
                        break;
                    case 5:
                        if (bonusObject.basicScroll > 0) {
                            arrBonus.addAll(Bonus.viewItem(ItemKey.SCROLL_SUMMON, bonusObject.basicScroll));
                        }
                        break;
                    case 6:
                        if (bonusObject.superScroll > 0) {
                            arrBonus.addAll(Bonus.viewItem(ItemKey.SCROLL_SUMMON_SPECIAL, bonusObject.superScroll));
                        }
                        break;
                    case 7:
                        if (bonusObject.bagOre1 > 0) {
                            arrBonus.addAll(Bonus.viewItem(ItemKey.BAG_ORE_1, bonusObject.bagOre1));
                        }
                        break;
                    case 8:
                        if (bonusObject.bagOre2 > 0) {
                            arrBonus.addAll(Bonus.viewItem(ItemKey.BAG_ORE_2, bonusObject.bagOre2));
                        }
                        break;
                    case 9:
                        if (bonusObject.bagOre3 > 0) {
                            arrBonus.addAll(Bonus.viewItem(ItemKey.BAG_ORE_3, bonusObject.bagOre3));
                        }
                        break;
                    case 10:
                        if (bonusObject.bagOre4 > 0) {
                            arrBonus.addAll(Bonus.viewItem(ItemKey.BAG_ORE_4, bonusObject.bagOre4));
                        }
                        break;
                    case 11:
                        if (bonusObject.bagOre5 > 0) {
                            arrBonus.addAll(Bonus.viewItem(ItemKey.BAG_ORE_5, bonusObject.bagOre5));
                        }
                        break;
                }
            }
        }
        return arrBonus;
    }

    public static void loadConfig(String value) {
        config = new Gson().fromJson(value, DataConfig.class);
        config.init();
    }

    @Data
    public class DataConfig {
        float[] farmQuestRate;
        float[] farmQuestRateLvBelow10;
        public int[] feeSpeedup;
        public long[] timeComplete;
        public int feeRefresh;
        public int[] requireNumberItem;
        public int[] requireStar;
        public List<Integer> questByLevel;
        public int maxStore;
        FarmQuestBonusObject[] bonusFarmQuest;

        public void init() {

        }
    }

    public class FarmQuestBonusObject {
        public int[] diamond;
        public int arenaCard;
        public int bossTicker;
        public int casinoChip;
        public int toolBag;
        public int basicScroll;
        public int superScroll;
        public int bagOre1;
        public int bagOre2;
        public int bagOre3;
        public int bagOre4;
        public int bagOre5;

    }
}
