package game.treasure.service.item;

import game.battle.object.Point;
import game.config.CfgItem;
import game.treasure.mapping.UserEquipmentEntity;
import game.treasure.mapping.UserItemEntity;
import game.treasure.mapping.main.ResItemEquipmentEntity;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.StringHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Roll stat trang bị khi nhận item mới. Xem {@code docs/equip.md}.
 */
public class EquipmentStatRollService {

    private static final int SET_ATK = 1;
    private static final int SET_HP = 2;
    private static final int SET_SPD = 3;

    private static final int[] STAT_POINT_IDS = {Point.ATTACK, Point.HP, Point.MOVE_SPEED};
    private static final float SPD_SECONDARY_MIN = 0.5f;

    private EquipmentStatRollService() {
    }

    public static void rollStatsIfNeeded(UserItemEntity item) {
        if (item == null || item.isEquipment() || hasRolledData(item.getData()))
            return;
        rollStats(item.getResEquipment(), item.getTier(), item::setData);
    }

    public static void rollStatsIfNeeded(UserEquipmentEntity item) {
        if (item == null || hasRolledData(item.getData()))
            return;
        rollStats(item.getResEquipment(), item.getTier(), item::setData);
    }

    @FunctionalInterface
    interface DataSetter {
        void set(String data);
    }

    private static void rollStats(ResItemEquipmentEntity meta, int tier, DataSetter setter) {
        if (meta == null)
            return;

        int set = meta.getSet();
        if (set < SET_ATK || set > SET_SPD)
            return;

        int type = meta.getType();
        if (type < 1 || type > 5)
            return;

        int tierRank = tier > 0 ? tier : 1;
        CfgItem.EquipStatRollConfig cfg = CfgItem.getEquipStatRoll();
        CfgItem.Range anchor = cfg.getPrimaryAnchor(set, tierRank);
        if (anchor == null)
            return;

        float slotMul = cfg.getSlotPrimaryMultiplier(type);
        float primaryMin = anchor.min * slotMul;
        float primaryMax = anchor.max * slotMul;
        float primaryValue = NumberUtil.getRandom(primaryMin, primaryMax);

        int primaryPointId = primaryPointId(set);
        List<Float> data = new ArrayList<>();
        appendStat(data, primaryPointId, formatValue(primaryPointId, primaryValue));

        float budget = (primaryMin + primaryMax) / 2f;
        CfgItem.SecondaryRollTierRates rates = cfg.getSecondaryRates(tierRank);
        int point2Rate = rates != null ? rates.point2Rate : 0;
        int point3Rate = rates != null ? rates.point3Rate : 0;

        int[] secondaryIds = secondaryPointIds(set);
        boolean point2Hit = NumberUtil.rand100(point2Rate);
        int point2Id = secondaryIds[NumberUtil.getRandom(secondaryIds.length)];
        if (point2Hit) {
            appendStat(data, point2Id, rollSecondaryValue(cfg, budget, point2Id));
        }

        int point3Id = point2Hit
                ? (point2Id == secondaryIds[0] ? secondaryIds[1] : secondaryIds[0])
                : secondaryIds[NumberUtil.getRandom(secondaryIds.length)];
        if (NumberUtil.rand100(point3Rate)) {
            appendStat(data, point3Id, rollTertiaryValue(cfg, budget, point3Id));
        }

        setter.set(StringHelper.toDBString(data));
    }

    private static boolean hasRolledData(String data) {
        return data != null && data.length() > 2 && !"[]".equals(data);
    }

    private static int primaryPointId(int set) {
        return switch (set) {
            case SET_ATK -> Point.ATTACK;
            case SET_HP -> Point.HP;
            default -> Point.MOVE_SPEED;
        };
    }

    private static int[] secondaryPointIds(int set) {
        return switch (set) {
            case SET_ATK -> new int[]{Point.HP, Point.MOVE_SPEED};
            case SET_HP -> new int[]{Point.ATTACK, Point.MOVE_SPEED};
            default -> new int[]{Point.ATTACK, Point.HP};
        };
    }

    private static void appendStat(List<Float> data, int pointId, float value) {
        data.add((float) pointId);
        data.add(value);
    }

    private static float formatValue(int pointId, float value) {
        return CfgItem.formatPointStat(pointId, value);
    }

    private static float rollSecondaryValue(CfgItem.EquipStatRollConfig cfg, float budget, int pointId) {
        CfgItem.Range range = cfg.getSecondaryRange(budget, pointId);
        if (range == null)
            return 0f;
        return formatValue(pointId, NumberUtil.getRandom(range.min, range.max));
    }

    private static float rollTertiaryValue(CfgItem.EquipStatRollConfig cfg, float budget, int pointId) {
        CfgItem.Range range = cfg.getTertiaryRange(budget, pointId);
        if (range == null)
            return 0f;
        float v = formatValue(pointId, NumberUtil.getRandom(range.min, range.max));
        if (pointId == Point.MOVE_SPEED && v > 0 && v < SPD_SECONDARY_MIN)
            return SPD_SECONDARY_MIN;
        return v;
    }
}
