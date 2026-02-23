package game.config;

import com.google.gson.Gson;
import game.config.aEnum.ItemKey;
import game.config.aEnum.PetType;
import game.config.aEnum.PieceType;
import game.dragonhero.mapping.UserPetEntity;
import game.dragonhero.mapping.main.ResPetEntity;
import game.dragonhero.service.resource.ResPet;
import game.dragonhero.service.user.Bonus;
import game.object.MyUser;
import ozudo.base.helper.NumberUtil;

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
    static List<Integer> rateStonePet = new ArrayList<>();
    static final List<Integer> MAX_HP_BY_STAR = List.of(100, 120, 150, 200);

    public static List<Long> summonPet(MyUser mUser, int number, boolean isVip, int petId) {
        List<Long> bonus = new ArrayList<>();
        ResPetEntity rPet = ResPet.getPet(petId);
        boolean hasPet = mUser.getResources().getPet(PetType.ANIMAL, petId) != null;
        if (petId == 0 || hasPet || rPet.getShowSummon() == 0) {  // bắt hụt
            bonus.addAll(getBonusStone(number));
        } else {
            for (int i = 0; i < number; i++) {
                if (hasPet || rPet.getShowSummon() == 0) {
                    bonus.addAll(getBonusStone(1));
                } else {
                    int rand = NumberUtil.getRandom(1000);
                    int rate = isVip ? config.ratePetVip.get(rPet.getRank() - 1) : config.ratePet.get(rPet.getRank() - 1);
                    if (rand <= rate) {
                        bonus.addAll(Bonus.viewPet(PetType.ANIMAL, petId));
                        hasPet = true;
                    } else {
                        bonus.addAll(getBonusStone(1));
                    }
                }
            }
        }
        return bonus;
    }

    public static List<Long> getBonusStone(int number) {
        List<Long> bonus = new ArrayList<>();
        for (int j = 0; j < number; j++) {
            int rand = NumberUtil.getRandom(1000);
            for (int i = 0; i < rateStonePet.size(); i++) {
                if (rand < rateStonePet.get(i)) {

                    bonus.addAll(Bonus.viewItem(getItemUpPetByRank(i ), 1));
                    break;
                }
            }
        }
        return bonus;
    }

    public static List<Long> getBonusMonsterPoint() {
        return new ArrayList<>(BONUS_MONSTER_POINT);
    }

    public static List<Long> getBonusPetPoint() {
        return new ArrayList<>(BONUS_PET_POINT);
    }

    public static List<Long> getFeeUpStarMonster(UserPetEntity uPet) {
        FeeUpStar fee = feeStarMonster.get(uPet.getResMonster().getRank()).get(uPet.getStar());
        List<Long> bonus = new ArrayList<>();
        bonus.addAll(Bonus.viewPiece(PieceType.MONSTER, uPet.getPetId(), -fee.piece));
        bonus.addAll(Bonus.viewGold(-fee.gold));
        return bonus;
    }

    public static List<Long> getFeeUpStarPet(UserPetEntity uPet) {
        FeeUpStar fee = feeStarPet.get(uPet.getResMonster().getRank()).get(uPet.getStar());
        List<Long> bonus = new ArrayList<>();
        bonus.addAll(Bonus.viewItem(getItemUpPetByRank(uPet.getStar()), -fee.piece));
        bonus.addAll(Bonus.viewGold(-fee.gold));
        return bonus;
    }

    public static ItemKey getItemUpPetByRank(int star) {
        switch (star) {
            case 0 -> {
                return ItemKey.DA_TIEN_HOA_CAP_1;
            }
            case 1 -> {
                return ItemKey.DA_TIEN_HOA_CAP_2;
            }
            default -> {
                return ItemKey.DA_TIEN_HOA_CAP_3;
            }
        }
    }

    public static void loadConfig(String value) {
        config = new Gson().fromJson(value, DataConfig.class);
        for (int i = 0; i < config.feeUpStarsMonster.size(); i++) {
            feeStarMonster.put(config.feeUpStarsMonster.get(i).rank, config.feeUpStarsMonster.get(i).fee);
        }
        for (int i = 0; i < config.feeUpStarsPet.size(); i++) {
            feeStarPet.put(config.feeUpStarsPet.get(i).rank, config.feeUpStarsPet.get(i).fee);
        }
        rateStonePet.add(config.rateStone.get(0));
        for (int i = 1; i < config.rateStone.size(); i++) {
            rateStonePet.add(i, rateStonePet.get(i - 1) + config.rateStone.get(i));
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
        List<Integer> ratePet;
        List<Integer> ratePetVip;
        List<Integer> rateStone;
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
