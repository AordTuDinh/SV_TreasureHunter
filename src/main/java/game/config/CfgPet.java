package game.config;

import com.google.gson.Gson;

import java.util.*;

public class CfgPet {
    public static DataConfig config;
    public static final int PIECE_TO_ITEM = 50;
    public static final int MAX_POINT_BONUS_MONSTER = 10;
    public static final int MAX_POINT_BONUS_PET = 10;
    public static final int HP_1_DAY = 10;
    public static final int START_PET_OPEN = 3;
    static final Map<Integer, List<FeeUpStar>> feeStarMonster = new HashMap<>();
    static final Map<Integer, List<FeeUpStar>> feeStarPet = new HashMap<>();
    static final List<Long> BONUS_MONSTER_POINT = List.of(6L, 87L, 10L);
    static final List<Long> BONUS_PET_POINT = List.of(6L, 87L, 10L);
    static final List<Integer> MAX_HP_BY_STAR = List.of(100, 120, 150, 200);

    public static void loadConfig(String value) {
        config = new Gson().fromJson(value, DataConfig.class);
        for (int i = 0; i < config.feeUpStarsMonster.size(); i++) {
            feeStarMonster.put(config.feeUpStarsMonster.get(i).rank, config.feeUpStarsMonster.get(i).fee);
        }
        for (int i = 0; i < config.feeUpStarsPet.size(); i++) {
            feeStarPet.put(config.feeUpStarsPet.get(i).rank, config.feeUpStarsPet.get(i).fee);
        }
    }

    public static boolean isMaxStar(int star) {
        return star >= START_PET_OPEN;
    }

    public static int getMaxHpByStar(int star) {
        return MAX_HP_BY_STAR.get(star);
    }

    public static class DataConfig {
        public List<FeeStarByRank> feeUpStarsMonster;
        public List<FeeStarByRank> feeUpStarsPet;
        public List<Integer> bonusStar;
    }

    public static class FeeStarByRank {
        public int rank;
        public List<FeeUpStar> fee;

    }

    public static class FeeUpStar {
        public int piece;
        public int gold;
    }
}
