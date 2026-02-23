package game.config;

import com.google.gson.Gson;
import game.battle.calculate.IMath;
import game.battle.object.BattleTeam;
import game.battle.object.HeroBattle;
import game.battle.object.Point;
import game.config.aEnum.ItemFarmType;
import game.config.aEnum.ItemKey;
import game.config.aEnum.PetType;
import game.dragonhero.mapping.UserArenaEntity;
import game.dragonhero.mapping.UserPetEntity;
import game.dragonhero.service.Services;
import game.dragonhero.service.resource.ResFarm;
import game.dragonhero.service.user.Bonus;
import game.object.MyUser;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CfgArena {
    public static final int SLOT_T1_HERO_1 = 0;
    public static final int SLOT_T1_HERO_2 = 1;
    public static final int SLOT_T1_HERO_3 = 2;
    public static final int SLOT_T1_PET = 3;
    public static final int SLOT_T1_MONSTER = 4;
    public static final int SLOT_T2_HERO_1 = 6;
    public static final int SLOT_T2_HERO_2 = 7;
    public static final int SLOT_T2_HERO_3 = 8;
    public static final int SLOT_T2_PET = 9;
    public static final int SLOT_T2_MONSTER = 10;
    public static DataConfig config;
    public static final int maxTimeAttack = 60;
    private static final int[] rateWin1150 = new int[]{60, 85, 100};
    private static final int[] rateWin1450 = new int[]{50, 75, 95, 100};
    private static final int[] rateWin1750 = new int[]{20, 40, 75, 100};
    private static final int[] rateWinTop = new int[]{0, 20, 70, 100};

    private static final int[] rateLost1150 = new int[]{85, 100};
    private static final int[] rateLost1450 = new int[]{60, 95, 100};
    private static final int[] rateLost1750 = new int[]{40, 80, 90, 100};
    private static final int[] rateLostTop = new int[]{30, 50, 80, 100};
    private static final Map<Integer, BonusData> mBonusDay = new HashMap<>();
    private static final Map<Integer, BonusData> mBonusWeek = new HashMap<>();
    public static final long pointPerBonusStar = 3;


    public static void loadConfig(String strJson) {
        config = new Gson().fromJson(strJson, DataConfig.class);
        for (int i = 0; i < config.bonusDay.size(); i++) {
            int from = config.bonusDay.get(i).from;
            int to = config.bonusDay.get(i).to;
            if (from == to) mBonusDay.put(config.bonusDay.get(i).to, config.bonusDay.get(i));
            else {
                for (int j = from; j <= to; j++) {
                    mBonusDay.put(j, config.bonusDay.get(i));
                }
            }
        }
        for (int i = 0; i < config.bonusWeek.size(); i++) {
            int from = config.bonusWeek.get(i).from;
            int to = config.bonusWeek.get(i).to;
            if (from == to) mBonusWeek.put(config.bonusWeek.get(i).to, config.bonusWeek.get(i));
            else {
                for (int j = from; j <= to; j++) {
                    mBonusWeek.put(j, config.bonusWeek.get(i));
                }
            }
        }
    }

    public static String getBonusDayByRank(int rank) {
        return mBonusDay.get(rank).bonus.toString();
    }

    public static List<Long> getBonusWeekByRank(int rank) {
        for (var data : mBonusWeek.entrySet()) {
            if (rank <= data.getKey()) return new ArrayList<>(data.getValue().bonus);
        }
        return null;
    }

    public static List<Long> getBonusArena(int myElo, boolean isWin, int addElo, boolean isFirst) {
        List<Long> bonus = new ArrayList<>();
        int rank = 1;
        int number = NumberUtil.getRandom(5) + 1;
        if (isWin) {
            if (myElo < 1150) rank = getRandomRank(rateWin1150);
            else if (myElo < 1450) rank = getRandomRank(rateWin1450);
            else if (myElo < 1750) rank = getRandomRank(rateWin1750);
            else rank = getRandomRank(rateWinTop);
            int huyHieu = addElo / 10;
            bonus.addAll(Bonus.viewItem(ItemKey.XU_DAU_TRUONG, huyHieu <= 1 ? 1 : huyHieu));
        } else {
            if (myElo < 1150) rank = getRandomRank(rateLost1150);
            else if (myElo < 1450) rank = getRandomRank(rateLost1450);
            else if (myElo < 1750) rank = getRandomRank(rateLost1750);
            else rank = getRandomRank(rateLostTop);
        }
        bonus.addAll(Bonus.viewItemFarm(ItemFarmType.SEED, isFirst ? 1 : ResFarm.getRandomSeedByRank(rank), number));
        return bonus;
    }

    private static int getRandomRank(int[] rate) {
        int rand = NumberUtil.getRandom(100);
        for (int i = 0; i < rate.length; i++) {
            if (rand < rate[i]) return i + 1;
        }
        return 1;
    }

    public static BattleTeam reCalDefTeamArena(MyUser mUser, boolean canUpdate) {
        //canUpdate : có đc phép  lưu vào DB hay k
        // update cùng lúc với last session nên trả về string để update cùng cho đỡ tài nguyên
        UserArenaEntity uArena = Services.userDAO.getUserArena(mUser);
        BattleTeam team = uArena.getDefTeam();
        boolean changeData = false;
        Point basePoint = IMath.calculatePoint(mUser, false);
        if (uArena.isActive() && team != null) {
            HeroBattle hPet = team.getBattleHeroes()[CfgArena.SLOT_T1_PET];
            //pet
            UserPetEntity pet = null;
            if (hPet != null) pet = mUser.getResources().getPet(PetType.ANIMAL, hPet.getId());
            if (pet != null) team.getBattleHeroes()[CfgArena.SLOT_T1_PET] = pet.toHeroBattle(2, CfgArena.SLOT_T1_PET);
            //monster
            UserPetEntity monster = null;
            HeroBattle hMonster = team.getBattleHeroes()[CfgArena.SLOT_T1_MONSTER];
            if (hMonster != null) monster = mUser.getResources().getPet(PetType.MONSTER, hMonster.getId());
            if (monster != null)
                team.getBattleHeroes()[CfgArena.SLOT_T1_MONSTER] = monster.toHeroBattle(2, CfgArena.SLOT_T1_MONSTER);
            for (int i = 0; i < CfgArena.SLOT_T1_PET; i++) {
                HeroBattle hero = team.getBattleHeroes()[i];
                if (changeData == false && hero != null)
                    changeData = hero.calPoint(mUser, basePoint.cloneInstance(), monster);
            }
        }
        if (changeData) { // có thay đổi dữ liệu
            if (canUpdate) uArena.updateDefTeam(team);
            else {
                uArena.setDefTeam(team);
                uArena.setDefenseTeam(team.toString());
            }
            return team;
        } else return null;
    }

    private static int getK(int rating) {
        if (rating < 1150) {
            return 25;
        } else if (rating < 1450) {
            return 20;
        } else if (rating < 1750) {
            return 15;
        }
        return 10;
    }

    public static int[] getRating(int ratingA, int ratingB, boolean winA) {
        double qa = Math.pow(10, ratingA / 400.0);
        double qb = Math.pow(10, ratingB / 400.0);
        double ea = qa / (qa + qb);
        double eb = qb / (qa + qb);
        double k = getK(ratingA);

//        double newRatingA = ratingA;
//        double newRatingB = ratingB;
        if (winA) { // A thắng, A được cộng điểm, B trừ điểm
//            newRatingA = ratingA + k * (1 - ea);
//            double newRatingB = ratingB + k * (0 - eb);
            if (ratingB < 200) {
                return new int[]{(int) Math.round(k * (1 - ea)), 0};
            }
            int addValueA = (int) Math.round(k * (1 - ea)), addValueB = (int) Math.round(k * (0 - eb));
            return new int[]{addValueA == 0 ? 1 : addValueA, addValueB == 0 ? -1 : addValueB};
        }
        // B thắng, A bị trừ điểm, B được cộng điểm
//     double   newRatingA = ratingA + k * (0 - ea);
//        newRatingB = ratingB + k * (1 - eb);
        if (ratingA < 200) {
            return new int[]{0, (int) Math.round(k * (1 - eb))};
        }
        int addValueA = (int) Math.round(k * (0 - ea)), addValueB = (int) Math.round(k * (1 - eb));
        addValueA = addValueA == 0 ? -1 : addValueA;
        int perA = addValueA/Math.abs(addValueA);
        addValueA = Math.abs(addValueA) >100? 100 * perA: addValueA;

        addValueB = addValueB == 0 ? 1 : addValueB;
        int perB = addValueB/Math.abs(addValueB);
        addValueB = Math.abs(addValueB) >100? 100 * perB: addValueB;

        return new int[]{ addValueA, addValueB};
    }


    public static int getIdPetByTeam(int team) {
        if (team == 1) return 4;
        else return 9;
    }


    public class DataConfig {
        public int feeTicket;
        public int maxBuyTicket;
        public List<BonusData> bonusDay;
        public List<BonusData> bonusWeek;
    }

    public static List<Long> getFeeBuyTicket(int number) {
        return Bonus.viewGem(-config.feeTicket * number);
    }

    public class BonusData {
        public int from, to;
        List<Long> bonus;
    }

}
