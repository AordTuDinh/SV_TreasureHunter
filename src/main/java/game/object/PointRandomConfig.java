package game.object;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import game.treasure.service.user.Bonus;
import ozudo.base.helper.NumberUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PointRandomConfig implements Serializable {
    public  int pointId;
    public  float min;
    public  float max;
    public int rate;


    public static List<PointRandomConfig> parsePoint(String pointConfig) {
        return new Gson().fromJson(pointConfig, new TypeToken<List<PointRandomConfig>>() {
        }.getType());
    }



    // lấy bonus của 1 config theo rate (hoặc luôn có nếu -1)
    List<Float> getPointWithPer() {
        float num = max == 1f ? 1 : NumberUtil.getRandom(min, max);
        if (rate == -1) { // mặc định là có
            return Arrays.asList((float)pointId,num);
        } else {
            int rand = NumberUtil.getRandom(1000);
            if (rand < rate) return Arrays.asList((float)pointId, num);
            else return new ArrayList<>();
        }
    }

    //random từng cái theo rate riêng, cái nào random trúng thì add cái đó
    // cộng dồn bonus của từng config (mỗi config tự roll)
    public static List<Float> getRandomBonusMulti(List<PointRandomConfig> aBonus) {
        List<Float> ret = new ArrayList<>();
        for (PointRandomConfig bm : aBonus) {
            ret.addAll(bm.getPointWithPer());
        }
        return ret;
    }

    // chỉ nhận được 1 bonus, cái nào random trúng thì trả về cái đó
    public static List<Float> getRandomOneBonus(List<PointRandomConfig> aBonus) {
        int per = NumberUtil.getRandom(1000);
        for (PointRandomConfig bm : aBonus) {
            float num = bm.max == 1 ? 1 : NumberUtil.getRandom(bm.min, bm.max);
            if (bm.rate == -1) {
                return Arrays.asList((float) bm.pointId, num);
            } else {
                if (per < bm.rate) {
                    return Arrays.asList((float) bm.pointId, num);
                } else per -= bm.rate;
            }
        }
        return new ArrayList<>();
    }

}
