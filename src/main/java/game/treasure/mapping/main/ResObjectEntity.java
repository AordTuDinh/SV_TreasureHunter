package game.treasure.mapping.main;

import game.config.aEnum.ItemType;
import game.object.BonusConfig;
import game.treasure.service.resource.ResItem;
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
    private static final int RATE_OPTION_TOTAL = 1000;
    private static final int RATE_INNER_TOTAL = 100;
    private static final int MATERIAL_ID_MIN = 1;
    private static final int MATERIAL_ID_MAX = 23;

    private static final int CAT_HP = 0;
    private static final int CAT_EQUIP = 1;
    private static final int CAT_MOB = 2;
    private static final int CAT_GOLD = 3;
    private static final int CAT_MATERIAL = 4;

    @Getter
    @Id
    int id;
    @Getter
    String name;
    @Getter
    int hp;
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
     * Roll 2 lần: (1) loại drop theo {@code rate_option} /1000, (2) id/tier theo rate /100 trong từng loại.
     */
    public List<Long> randomBonus() {
        if (rateOptionParsed == null || rateOptionParsed.isEmpty()) return new ArrayList<>();
        int cat = pickIndexByRate(rateOptionParsed, RATE_OPTION_TOTAL);
        return switch (cat) {
            case CAT_HP -> randomHpBonus();
            case CAT_EQUIP -> randomEquipBonus();
            case CAT_MOB -> randomMobBonus();
            case CAT_GOLD -> randomGoldBonus();
            case CAT_MATERIAL -> randomMaterialBonus();
            default -> new ArrayList<>();
        };
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

    private List<Long> randomMaterialBonus() {
        int materialId = NumberUtil.getRandom(MATERIAL_ID_MIN, MATERIAL_ID_MAX);
        int tier = pickTier(materialRateParsed);
        return Bonus.viewMaterial(materialId, tier);
    }

    private static int pickTier(List<Integer> tierRates) {
        if (tierRates == null || tierRates.isEmpty()) return 1;
        return pickIndexByRate(tierRates, RATE_INNER_TOTAL) + 1;
    }

    /** {@code pairs}: [id, rate, id, rate, ...]} — rate cộng dồn theo {@code totalRate}. */
    static int pickIdByRatePairs(List<Integer> pairs) {
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
