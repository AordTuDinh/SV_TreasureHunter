package game.config;

import com.google.gson.Gson;
import game.config.aEnum.ItemKey;
import game.config.aEnum.PieceType;
import game.dragonhero.service.user.Bonus;
import net.sf.json.JSONObject;
import ozudo.base.helper.NumberUtil;

import java.util.*;

public class CfgGacha {
    public static JSONObject json;
    public static DataConfig config;
    public static Map<Integer, SummonLevel> summonDataStone;
    public static Map<Integer, SummonLevel> summonDataPiece;
    public static final List<Long> firstSummonStoneX10 = List.of(6L, 41L, 1L, 6L, 43L, 1L, 6L, 42L, 1L, 6L, 43L, 1L, 6L, 40L, 1L, 6L, 41L, 1L, 6L, 42L, 1L, 6L, 43L, 1L, 6L, 44L, 1L, 6L, 43L, 1L);
    public static final int SUMMON_SCROLL = 0;
    public static final int SUMMON_GEM = 1;
    public static final Map<Integer, List<Integer>> stoneByRank = new HashMap<>() {{
        put(1, Arrays.asList(40, 41, 42, 43, 44));
        put(2, Arrays.asList(45, 46, 47, 48, 49));
        put(3, Arrays.asList(50, 51, 52, 53, 54));
        put(4, Arrays.asList(55, 56, 57, 58, 59));
        put(5, Arrays.asList(60, 61, 62, 63, 64));
        put(6, Arrays.asList(65, 66, 67, 68, 69));

    }};


    public static void loadConfig(String strJson) {
        summonDataStone = new HashMap<>();
        config = new Gson().fromJson(strJson, DataConfig.class);
        config.summonLevel.forEach(data -> {
            List<Long> rate = data.summonRate;
            for (int i = 1; i < rate.size(); i++) {
                rate.set(i, rate.get(i - 1) + rate.get(i));
            }
            summonDataStone.put(data.level, data);
        });

        summonDataPiece = new HashMap<>();
        config.summonPiece.forEach(data -> {
            List<Long> rate = data.summonRate;
            for (int i = 1; i < rate.size(); i++) {
                rate.set(i, rate.get(i - 1) + rate.get(i));
            }
            summonDataPiece.put(data.level, data);
        });
    }

    public static List<Long> bonusStone(int rank) {
        int ranStone = stoneByRank.get(rank).get(NumberUtil.getRandom(5));
        return Bonus.viewItem(ranStone, 1);
    }

    public static List<Long> bonusPiece(int id) {// id phải bằng rank
        return Bonus.viewPiece(PieceType.WEAPON, id, 1);
    }

    public static List<Long> getFeeSummonStone(int type, int number) {
        if (type == SUMMON_SCROLL) {
            return Bonus.viewItem(ItemKey.SCROLL_SUMMON, -number);
        } else {
            if (number == 1) {
                return Bonus.viewGem((int) -config.prices.get(0));
            } else if (number == 10) {
                return Bonus.viewGem((int) -config.prices.get(1));
            } else return Bonus.viewGem((int) -config.prices.get(2));
        }
    }


    public class DataConfig {
        public List<Long> prices;
        public int timeSummonAdsMinutes;
        public List<Integer> summonStone;
        public List<SummonLevel> summonLevel;
        public List<SummonLevel> summonPiece;

    }

    public class SummonLevel {
        public int level;
        public int number;
        public List<Long> bonus;
        public List<Long> summonRate; // chỉ số x10 => random 1000
    }
}
