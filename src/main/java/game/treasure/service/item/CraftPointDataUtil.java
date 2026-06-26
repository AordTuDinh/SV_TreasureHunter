package game.treasure.service.item;

import game.config.CfgItem;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.StringHelper;

import java.util.ArrayList;
import java.util.List;

public final class CraftPointDataUtil {

    private CraftPointDataUtil() {
    }

    public static List<Float> parseDataFloats(String data) {
        if (data == null || data.isEmpty() || "[]".equals(data))
            return new ArrayList<>();
        return GsonUtil.strToListFloat(data);
    }

    public static List<Float> mergePointPair(List<Float> raw, int pointId, float value) {
        List<Float> merged = new ArrayList<>(raw);
        for (int i = 0; i + 1 < merged.size(); i += 2) {
            if (Math.round(merged.get(i)) == pointId) {
                merged.set(i + 1, merged.get(i + 1) + value);
                return merged;
            }
        }
        merged.add((float) pointId);
        merged.add(value);
        return merged;
    }

    public static String scaleData(String data, float mul) {
        List<Float> raw = parseDataFloats(data);
        if (raw.isEmpty())
            return data == null ? "[]" : data;
        List<Float> scaled = new ArrayList<>();
        for (int i = 0; i + 1 < raw.size(); i += 2) {
            int pointId = Math.round(raw.get(i));
            float value = raw.get(i + 1) * mul;
            scaled.add((float) pointId);
            scaled.add(CfgItem.formatPointStat(pointId, value));
        }
        return StringHelper.toDBString(scaled);
    }
}
