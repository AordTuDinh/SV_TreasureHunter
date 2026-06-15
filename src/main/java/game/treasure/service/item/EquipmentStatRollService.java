package game.treasure.service.item;

import game.battle.object.Point;
import game.config.CfgItem;
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
        if (item == null || !item.isEquipment() || hasRolledData(item.getData()))
            return;

        ResItemEquipmentEntity meta = item.getResEquipment();
        if (meta == null)
            return;

        int set = meta.getSet();
        if (set < SET_ATK || set > SET_SPD)
            return;

        int type = meta.getType();
        if (type < 1 || type > 5)
            return;

        int tier = resolveTierRank(item, meta);
        CfgItem.EquipStatRollConfig cfg = CfgItem.getEquipStatRoll();
        CfgItem.Range anchor = cfg.getPrimaryAnchor(set, tier);
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
        CfgItem.SecondaryRollTierRates rates = cfg.getSecondaryRates(tier);
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

        item.setData(StringHelper.toDBString(data));
    }

    static boolean hasRolledData(String data) {
        return data != null && !data.isEmpty() && !"[]".equals(data);
    }

    static int resolveTierRank(UserItemEntity item, ResItemEquipmentEntity meta) {
        if (item.getTier() > 0)
            return Math.min(item.getTier(), 4);
        if (meta.getRank() > 0)
            return Math.min(meta.getRank(), 4);
        return 1;
    }

    static int primaryPointId(int set) {
        return switch (set) {
            case SET_ATK -> Point.ATTACK;
            case SET_HP -> Point.HP;
            case SET_SPD -> Point.MOVE_SPEED;
            default -> -1;
        };
    }

    /** Hai stat còn lại (không phải primary của set). */
    static int[] secondaryPointIds(int set) {
        int primary = primaryPointId(set);
        int[] out = new int[2];
        int j = 0;
        for (int id : STAT_POINT_IDS) {
            if (id != primary)
                out[j++] = id;
        }
        return out;
    }

    static float rollSecondaryValue(CfgItem.EquipStatRollConfig cfg, float budget, int pointId) {
        float min = budget * cfg.point2RatioMin;
        float max = budget * cfg.point2RatioMax;
        return formatSecondaryValue(pointId, NumberUtil.getRandom(min, max));
    }

    static float rollTertiaryValue(CfgItem.EquipStatRollConfig cfg, float budget, int pointId) {
        float min = budget * cfg.point3RatioMin;
        float max = budget * cfg.point3RatioMax;
        return formatSecondaryValue(pointId, NumberUtil.getRandom(min, max));
    }

    static float formatSecondaryValue(int pointId, float raw) {
        if (pointId == Point.MOVE_SPEED) {
            return round1(Math.max(raw, SPD_SECONDARY_MIN));
        }
        // ATK/HP phụ: budget set SPD nhỏ dễ round về 0 → mất dòng dù rate đã hit
        return Math.max(1, Math.round(raw));
    }

    static float formatValue(int pointId, float raw) {
        if (pointId == Point.MOVE_SPEED)
            return round1(raw);
        return Math.round(raw);
    }

    static float round1(float v) {
        return Math.round(v * 10f) / 10f;
    }

    static void appendStat(List<Float> data, int pointId, float value) {
        if (pointId < 0 || value <= 0f)
            return;
        data.add((float) pointId);
        data.add(value);
    }
}
