package game.config;

import com.google.gson.Gson;
import game.treasure.service.user.Bonus;
import ozudo.base.helper.NumberUtil;

import java.util.ArrayList;
import java.util.List;

public class CfgLuckySpine {
    public static final int SLOT_GOLD = 0;
    public static final int SLOT_ITEM = 1;
    public static final int SLOT_ITEM_EQUIP = 2;
    public static final int SLOT_PET = 3;
    public static final int SLOT_MOUNT = 4;
    public static final int SLOT_MATERIAL = 5;
    public static final int SLOT_ITEM_POINT = 6;
    public static final int SLOT_MATERIAL_SPD = 7;
    public static final int SLOT_COUNT = 8;
    public static final int EQUIP_TIER = 4;

    public static DataConfig config;
    public static List<Integer> rateRotate;
    public static List<Integer> tierRates;

    public static void loadConfig(String value) {
        config = new Gson().fromJson(value, DataConfig.class);
        rateRotate = buildCumulativeRates(config.casinoRate);
        tierRates = buildCumulativeRates(config.tierRate);
    }

    private static List<Integer> buildCumulativeRates(List<Integer> rates) {
        List<Integer> cumulative = new ArrayList<>();
        if (rates == null) return cumulative;
        int rate = 0;
        for (int value : rates) {
            rate += value;
            cumulative.add(rate);
        }
        return cumulative;
    }

    public static int getRandomIndex() {
        return rollIndex(rateRotate);
    }

    public static int rollTier() {
        return rollIndex(tierRates) + 1;
    }

    private static int rollIndex(List<Integer> cumulative) {
        if (cumulative == null || cumulative.isEmpty()) return 0;
        int rand = NumberUtil.getRandom(1000);
        for (int i = 0; i < cumulative.size(); i++) {
            if (rand < cumulative.get(i)) return i;
        }
        return cumulative.size() - 1;
    }

    public static List<Long> rollBonusBySlot(int slot) {
        BonusNormal bonusNormal = config.bonusNormal;
        if (bonusNormal == null) return new ArrayList<>();
        return switch (slot) {
            case SLOT_GOLD -> Bonus.viewGold(rollGold(bonusNormal.gold));
            case SLOT_ITEM -> Bonus.viewItem(pickRandomId(bonusNormal.item), 1);
            case SLOT_ITEM_EQUIP -> Bonus.viewItemEquipment(pickRandomId(bonusNormal.itemEquip), EQUIP_TIER);
            case SLOT_PET -> Bonus.viewPet(pickRandomId(bonusNormal.pet), rollTier());
            case SLOT_MOUNT -> Bonus.viewMount(pickRandomId(bonusNormal.mount), rollTier());
            case SLOT_MATERIAL -> Bonus.viewMaterial(pickRandomId(bonusNormal.material), rollTier());
            case SLOT_ITEM_POINT -> Bonus.viewItemPoint(pickRandomId(bonusNormal.itemPoint), 1);
            case SLOT_MATERIAL_SPD -> Bonus.viewMaterial(pickRandomId(bonusNormal.materialSpd), rollTier());
            default -> new ArrayList<>();
        };
    }

    private static long rollGold(List<Integer> gold) {
        if (gold == null || gold.isEmpty()) return 0;
        if (gold.size() == 1) return gold.get(0);
        int min = gold.get(0);
        int max = gold.get(1);
        if (min > max) {
            int tmp = min;
            min = max;
            max = tmp;
        }
        return NumberUtil.getRandom(min, max);
    }

    private static int pickRandomId(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        return ids.get(NumberUtil.getRandom(ids.size()));
    }

    public static class DataConfig {
        public List<Integer> casinoRate;
        public int[] feeRotate;
        public List<Integer> tierRate;
        public BonusNormal bonusNormal;
        public int priceChip;
    }

    public static class BonusNormal {
        public List<Integer> gold;
        public List<Integer> item;
        public List<Integer> itemEquip;
        public List<Integer> pet;
        public List<Integer> mount;
        public List<Integer> material;
        public List<Integer> itemPoint;
        public List<Integer> materialSpd;
    }
}
