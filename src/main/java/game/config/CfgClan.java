package game.config;

import com.google.gson.Gson;
import game.config.aEnum.ClanPosition;
import game.dragonhero.service.user.Bonus;
import game.monitor.Online;
import game.object.BonusConfig;
import ozudo.base.helper.DateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CfgClan {
    public static final int CHECK_IN_CLAN = 1;
      public static final int ATTACK_BOSS = 2;
    public static final int NUM_ATTACK_BOSS = 3;
    public static final int DYNAMIC_QUEST_D_100 = 3;
    public static DataConfig config;
    public static long timeWaitLeave = 12 * DateTime.HOUR_MILLI_SECOND;
    public static List<Integer> CLAN_RULE = Arrays.asList(ClanPosition.LEADER.value, ClanPosition.CO_LEADER.value);
    public static float perGoldReset = 0.8f;
    public static float perCointReset = 1f;
    public static int HUY_HIEU_BANG = 38;
    public static List<Integer> slotClanBoss = Arrays.asList(0, 1, 2, 3, 4, 5);

    public static void loadConfig(String strJson) {
        config = new Gson().fromJson(strJson, DataConfig.class);
    }

    public static ClanWelfare getClanWelfare(int indexLevel) {
        return config.clanWelfare.get(indexLevel);
    }

    public static int getMaxExp(int level) {
        return config.exp.get(level - 1);
    }

    public static List<Long> getBonusDailyHonor(int curHonor) {
        for (int i = 0; i < config.bonusHonor.size(); i++) {
            if (curHonor < config.bonusHonor.get(i).point) {
                return new ArrayList<>(config.bonusHonor.get(i - 1).bonus);
            }
        }
        return config.bonusHonor.get(config.bonusHonor.size() - 1).bonus;
    }


    public static int getIndexBonusHonor(int curHonor) {
        for (int i = 0; i < config.bonusHonor.size(); i++) {
            if (curHonor < config.bonusHonor.get(i).point) return i - 1;
        }
        return config.bonusHonor.size();
    }

    public static boolean checkSlotInput(int slot) {
        return slotClanBoss.contains(slot);
    }


    public static List<Long> getBonusBoxDynamic(int numberBox) {
        List<Long> bonus = new ArrayList<>();
        for (int i = 0; i < numberBox; i++) {
            bonus.addAll(BonusConfig.getRandomBonusMulti(config.bonusBoxDynamic));
        }
        return bonus;
    }

    public static List<Long> getBonusDynamic() {
        return new ArrayList<>(config.bonusDynamic);
    }

    public static List<Long> getFeeCreate(int type) {
        if (type == 1) return Bonus.viewRuby(-config.feeCreateRuby);
        return Bonus.viewGem(-config.feeCreate);
    }

    //seconds
    public static long getTimeRemainBoss(long timeAttack) {
        long time = timeAttack + config.timeAttackBoss * DateTime.MIN_MILLI_SECOND - System.currentTimeMillis();
        return time > 0 ? time / 1000 : 0;
    }

    public static List<Long> getFeeChangeName() {
        return Bonus.viewGem(-config.feeChangeName);
    }

    public static int getMaxMember(int level) {
        return config.maxMember + level - 1;
    }


    public static boolean hasUpdateSkill(int group, List<Integer> skillCount, int nunber) { // k cho nâng skill 2 và 3 lệch quá point check
        if (group == 1 && skillCount.get(0) + nunber - skillCount.get(1) > config.pointCheck - 1) return false;
        if (group == 2 && skillCount.get(1) + nunber - skillCount.get(0) > config.pointCheck - 1) return false;
        return true;
    }

    public class DataConfig {
        public int maxMember;
        public int clanNameLength;
        public int maxNumHonor;
        public int introLength;
        public int levelCreateClan;
        public int feeCreate;
        public int feeCreateRuby;
        public int feeReset;
        public int feeOpenBoss;
        public int timeAttackBoss; //minutes
        public int feeChangeName;
        public List<Integer> exp;
        public List<Integer> gemTopDameBoss;
        public List<Integer> coinTopDameBoss;
        public List<DataHonor> bonusHonor;
        public int checkInGuildExp, checkInGuildCoin;
        public int masterSkill;
        public int pointCheck;
        public int timeQuest;
        public int contributeX1;
        public int contributeX10;
        public List<Integer> upgradeQuest;
        public int maxPointDynamic;
        public int maxBoxDynamic;
        public List<Long> bonusDynamic;
        public List<BonusConfig> bonusBoxDynamic;
        public List<ClanWelfare> clanWelfare;
    }

    public class ClanWelfare {
        public int level;
        public int num;
        public int point;
    }

    public class DataHonor {
        public int point;
        public List<Long> bonus;
    }
}
