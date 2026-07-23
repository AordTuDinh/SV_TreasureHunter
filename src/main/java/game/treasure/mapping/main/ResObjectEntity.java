package game.treasure.mapping.main;

import game.treasure.service.user.Bonus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.StringHelper;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@NoArgsConstructor
@Entity
public class ResObjectEntity extends BaseEntity implements Serializable {
    private static final int RATE_DROP_TOTAL = 1000;
    private static final int RATE_OPTION_TOTAL = 1000;
    private static final int RATE_INNER_TOTAL = 100;
    private static final int EVENT_MATERIAL_TIER = 1;

    private static final int CAT_HP = 0;
    private static final int CAT_EQUIP = 1;
    private static final int CAT_MOB = 2;
    private static final int CAT_GOLD = 3;
    private static final int CAT_MATERIAL = 4;
    private static final int CAT_ITEM_EVENT = 5;

    @Getter
    @Id
    int id;
    @Getter
    String name;
    @Getter
    int hp;
    @Getter
    int rateDrop;
    String rateOption;
    String itemHp;
    String equipment;
    String equipmentTierRate;
    String mobs;
    String gold;
    String materialRate;

    @Transient
    List<Integer> rateOptionParsed;
    @Transient
    List<Integer> itemHpParsed;
    @Transient
    List<Integer> equipmentParsed;
    @Transient
    List<Integer> equipmentTierRateParsed;
    @Transient
    List<Integer> mobsParsed;
    @Transient
    List<Integer> goldParsed;
    @Transient
    List<Integer> materialRateParsed;

    public void init() {
        rateOptionParsed = parseIntList(rateOption);
        itemHpParsed = parseIntList(itemHp);
        equipmentParsed = parseIntList(equipment);
        equipmentTierRateParsed = parseIntList(equipmentTierRate);
        mobsParsed = parseIntList(mobs);
        goldParsed = parseIntList(gold);
        materialRateParsed = parseIntList(materialRate);
    }

    /**
     * Gate {@code rateDrop}/1000 trước (+ {@code rateDropBonus} VIP); pass mới roll category theo {@code rate_option}/1000.
     * Hụt gate → roll miss bonus; vẫn trống → {@code fullMiss=true} (caller roll treasure).
     */
    public ObjectDropResult randomBonus(int materialId, int itemEventId,
                                        int rateDropGold, int rateDropGem, int rateDropItem) {
        return randomBonus(materialId, itemEventId, rateDropGold, rateDropGem, rateDropItem, 0);
    }

    public ObjectDropResult randomBonus(int materialId, int itemEventId,
                                        int rateDropGold, int rateDropGem, int rateDropItem, int rateDropBonus) {
        int effectiveRate = Math.min(RATE_DROP_TOTAL, Math.max(0, rateDrop) + Math.max(0, rateDropBonus));
        if (effectiveRate <= 0 || NumberUtil.getRandom(RATE_DROP_TOTAL) >= effectiveRate) {
            if (rateDropGold <= 0 && rateDropGem <= 0 && rateDropItem <= 0) {
                return ObjectDropResult.fullMiss();
            }
            List<Long> miss = randomMissBonus(materialId, rateDropGold, rateDropGem, rateDropItem);
            return miss.isEmpty() ? ObjectDropResult.fullMiss() : ObjectDropResult.of(miss);
        }
        if (rateOptionParsed == null || rateOptionParsed.isEmpty()) return ObjectDropResult.of(new ArrayList<>());
        int cat = pickIndexByRate(rateOptionParsed, RATE_OPTION_TOTAL);
        List<Long> bonus = switch (cat) {
            case CAT_HP -> randomHpBonus();
            case CAT_EQUIP -> randomEquipBonus();
            case CAT_MOB -> randomMobBonus();
            case CAT_GOLD -> randomGoldBonus();
            case CAT_MATERIAL -> randomMaterialBonus(materialId);
            case CAT_ITEM_EVENT -> itemEventBonus(itemEventId);
            default -> new ArrayList<>();
        };
        return ObjectDropResult.of(bonus);
    }

    public static final class ObjectDropResult {
        public final List<Long> bonus;
        public final boolean fullMiss;

        private ObjectDropResult(List<Long> bonus, boolean fullMiss) {
            this.bonus = bonus != null ? bonus : new ArrayList<>();
            this.fullMiss = fullMiss;
        }

        public static ObjectDropResult of(List<Long> bonus) {
            return new ObjectDropResult(bonus, false);
        }

        public static ObjectDropResult fullMiss() {
            return new ObjectDropResult(new ArrayList<>(), true);
        }
    }

    /**
     * Drop hụt: {@code x = random(1000)}; bucket cộng dồn gold → gem(1) → material(tier {@code materialRate}).
     */
    private List<Long> randomMissBonus(int materialId, int rateDropGold, int rateDropGem, int rateDropItem) {
        int goldRate = Math.max(0, rateDropGold);
        int gemRate = Math.max(0, rateDropGem);
        int itemRate = Math.max(0, rateDropItem);
        int x = NumberUtil.getRandom(RATE_DROP_TOTAL);
        if (x < goldRate) return randomGoldBonus();
        if (x < goldRate + gemRate) return Bonus.viewGem(1);
        if (x < goldRate + gemRate + itemRate) return randomMaterialBonus(materialId);
        return new ArrayList<>();
    }

    private List<Long> randomHpBonus() {
        int itemId = pickIdByRatePairs(itemHpParsed);
        if (itemId <= 0) return new ArrayList<>();
        return Bonus.viewItem(itemId, 1);
    }

    private List<Long> randomEquipBonus() {
        if (equipmentParsed == null || equipmentParsed.isEmpty()) return new ArrayList<>();
        int itemId = equipmentParsed.get(NumberUtil.getRandom(equipmentParsed.size()));
        int tier = pickTier(equipmentTierRateParsed);
        return Bonus.viewItemEquipment(itemId, tier);
    }

    private List<Long> randomMobBonus() {
        int mobId = pickIdByRatePairs(mobsParsed);
        if (mobId <= 0) return new ArrayList<>();
        return Arrays.asList(-1L, (long) mobId);
    }

    private List<Long> randomGoldBonus() {
        if (goldParsed == null || goldParsed.size() < 2) return new ArrayList<>();
        int min = goldParsed.get(0);
        int max = goldParsed.get(1);
        return Bonus.viewGold(NumberUtil.getRandom(min, max));
    }

    private List<Long> randomMaterialBonus(int materialId) {
        if (materialId <= 0) return new ArrayList<>();
        int tier = pickTier(materialRateParsed);
        return Bonus.viewMaterial(materialId, tier);
    }

    private static List<Long> itemEventBonus(int itemEventId) {
        if (itemEventId <= 0) return new ArrayList<>();
        return Bonus.viewMaterial(itemEventId, EVENT_MATERIAL_TIER);
    }

    private static int pickTier(List<Integer> tierRates) {
        if (tierRates == null || tierRates.isEmpty()) return 1;
        return pickIndexByRate(tierRates, RATE_INNER_TOTAL) + 1;
    }

    /**
     * {@code pairs}: [id, rate, id, rate, ...]} — rate cộng dồn theo {@code totalRate}.
     */
    public static int pickIdByRatePairs(List<Integer> pairs) {
        if (pairs == null || pairs.size() < 2) return 0;
        int total = 0;
        for (int i = 1; i < pairs.size(); i += 2) total += pairs.get(i);
        if (total <= 0) return pairs.get(0);
        int roll = NumberUtil.getRandom(total);
        int sum = 0;
        for (int i = 0; i + 1 < pairs.size(); i += 2) {
            sum += pairs.get(i + 1);
            if (roll < sum) return pairs.get(i);
        }
        return pairs.get(pairs.size() - 2);
    }

    static int pickIndexByRate(List<Integer> rates, int totalRate) {
        if (rates == null || rates.isEmpty()) return 0;
        if (rates.size() == 1) return 0;
        int roll = NumberUtil.getRandom(totalRate);
        for (int i = 0; i < rates.size(); i++) {
            if (roll < rates.get(i)) return i;
            roll -= rates.get(i);
        }
        return rates.size() - 1;
    }

    private static List<Integer> parseIntList(String json) {
        if (StringHelper.isEmpty(json)) return new ArrayList<>();
        try {
            List<Integer> list = GsonUtil.strToListInt(json);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
