package game.config;

import com.google.gson.Gson;
import game.config.aEnum.ClanPosition;
import game.treasure.service.user.Bonus;
import game.object.BonusConfig;
import ozudo.base.helper.DateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CfgClan {
    public static final int NUM_ATTACK_BOSS = 3;
    public static final int ASSASSIN_CLAN_ID = -1;
    public static final String ASSASSIN_CLAN_NAME = "Sát thủ";
    public static final int WARRIOR_CLAN_ID = -2;
    public static final String WARRIOR_CLAN_NAME = "Chiến binh";
    public static final int assassinMoveSpeedBonus = 10;
    public static final int warriorHpBonus = 10;
    public static DataConfig config;
    /** Cooldown sau khi rời bang thường: chặn gia nhập + tạo bang. */
    public static long timeWaitLeave = 6 * DateTime.HOUR_MILLI_SECOND;
    /** Rời bang hệ thống không khóa; giữ field để tương thích cũ. */
    public static long timeWaitLeaveSystemClan = 0;
    public static final int MAX_PENDING_REQ = 10;
    public static final int MAX_CLAN_LEVEL = 10;
    public static final int MAX_MEMBER_CAP = 13;
    public static final int MAX_PERSONAL_CONTRIBUTE = 2000;

    public static boolean isSystemClan(int clanId) {
        return clanId == ASSASSIN_CLAN_ID || clanId == WARRIOR_CLAN_ID;
    }

    public static String getSystemClanName(int clanId) {
        if (clanId == ASSASSIN_CLAN_ID) return ASSASSIN_CLAN_NAME;
        if (clanId == WARRIOR_CLAN_ID) return WARRIOR_CLAN_NAME;
        return "";
    }
    public static List<Integer> CLAN_RULE = Arrays.asList(ClanPosition.LEADER.value, ClanPosition.CO_LEADER.value);
    public static List<Integer> slotClanBoss = Arrays.asList(0, 1, 2, 3, 4, 5);

    public static void loadConfig(String strJson) {
        config = new Gson().fromJson(strJson, DataConfig.class);
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

    /** Level 1 = maxMember (3), mỗi level +1, tối đa 13. */
    public static int getMaxMember(int level) {
        int lv = Math.max(1, Math.min(level, MAX_CLAN_LEVEL));
        return Math.min(MAX_MEMBER_CAP, config.maxMember + lv - 1);
    }

    /** Phó bang: mở thêm 1 slot ở cấp 3, 6, 9 (tối đa 3). */
    public static int getMaxCoLeader(int level) {
        if (level >= 9) return 3;
        if (level >= 6) return 2;
        if (level >= 3) return 1;
        return 0;
    }


    public static boolean hasUpdateSkill(int group, List<Integer> skillCount, int nunber) { // k cho nâng skill 2 và 3 lệch quá point check
        if (group == 1 && skillCount.get(0) + nunber - skillCount.get(1) > config.pointCheck - 1) return false;
        if (group == 2 && skillCount.get(1) + nunber - skillCount.get(0) > config.pointCheck - 1) return false;
        return true;
    }

    public class DataConfig {
        public int maxMember;
        public int clanNameLength;
        public int introLength;
        public int levelCreateClan;
        public int feeCreate;
        public int feeCreateRuby;
        public int timeAttackBoss; //minutes
        public int feeChangeName;
        public List<Integer> exp;
        public List<DataHonor> bonusHonor;
        public int checkInGuildExp, checkInGuildCoin;
        public int pointCheck;
        public List<Integer> upgradeQuest;
        public List<Long> bonusDynamic;
        public List<BonusConfig> bonusBoxDynamic;
    }

    public class DataHonor {
        public int point;
        public List<Long> bonus;
    }
}
