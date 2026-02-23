package game.object;

import game.dragonhero.service.user.Bonus;
import lombok.Getter;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BonusConfig implements Serializable {
    @Getter
    List<Long> bonus;
    @Getter
    int min;
    @Getter
    int max;
    @Getter
    int rate; // 0 -1000


    List<Long> getBonusWithPer() {
        if (rate == -1) { // mặc định là có
            int num = max == 1 ? 1 : NumberUtil.getRandom(min, max);
            num += num;
            return Bonus.viewXNumber(new ArrayList<>(bonus), num);
        } else {
            int rand = NumberUtil.getRandom(1000);
            if (rand < rate) return Bonus.viewXNumber(new ArrayList<>(bonus), NumberUtil.getRandom(min, max));
            else return new ArrayList<>();
        }
    }

    //random từng cái theo rate riêng, cái nào random trúng thì add cái đó
    public static List<Long> getRandomBonusMulti(List<BonusConfig> aBonus) {
        List<Long> ret = new ArrayList<>();
        for (int i = 0; i < aBonus.size(); i++) {
            BonusConfig bm = aBonus.get(i);
            ret.addAll(bm.getBonusWithPer());
        }
        return ret;
    }

    // chỉ nhận được 1 bonus, cái nào random trúng thì trả về cái đó
    public static List<Long> getRandomOneBonus(List<BonusConfig> aBonus) {
        int per = NumberUtil.getRandom(1000);

        for (int i = 0; i < aBonus.size(); i++) {
            BonusConfig bm = aBonus.get(i);
            if (bm.rate == -1) {
                int num = bm.max == 1 ? 1 : NumberUtil.getRandom(bm.min, bm.max);
                return Bonus.viewXNumber(new ArrayList<>(bm.bonus), num);
            } else {
                if (per <= bm.rate) {
                    int num = NumberUtil.getRandom(bm.min, bm.max);
                    return Bonus.viewXNumber(new ArrayList<>(bm.bonus), num);
                } else per -= bm.rate;
            }
        }
        return new ArrayList<>();
    }

    // check cả bonus -1 , còn lại thì trả về 1 bonus theo per
    public static List<Long> getRandomBonus(List<BonusConfig> aBonus) {
        int per = NumberUtil.getRandom(1000);
        List<Long> ret = new ArrayList<>();
        boolean hasBonusOne = false;
        for (int i = 0; i < aBonus.size(); i++) {
            BonusConfig bm = aBonus.get(i);
            if (bm.rate == -1) {
                int num = bm.max == 1 ? 1 : NumberUtil.getRandom(bm.min, bm.max);
                ret.addAll(Bonus.viewXNumber(new ArrayList<>(bm.bonus), num));
            } else if (!hasBonusOne) {
                if (per < bm.rate) {
                    ret.addAll(Bonus.viewXNumber(new ArrayList<>(bm.bonus), NumberUtil.getRandom(bm.min, bm.max)));
                    hasBonusOne = true;
                } else per -= bm.rate;
            }
        }
        return ret;
    }

    // only boss
    public static List<Long> getRandomBonusBoss(List<BonusConfig> aBonus, List<Integer> perBonusAdd) {
        int per = NumberUtil.getRandom(1000);
        List<Long> ret = new ArrayList<>();
        boolean hasBonusOne = false;
        for (int i = 0; i < aBonus.size(); i++) {
            BonusConfig bm = aBonus.get(i);
            if (bm.rate == -1) {
                int num = bm.max == 1 ? 1 : NumberUtil.getRandom(bm.min, bm.max);
                num += (int) (num * perBonusAdd.get(0) / 100f);
                ret.addAll(Bonus.viewXNumber(new ArrayList<>(bm.bonus), num));
            } else if (!hasBonusOne) {
                if (per < bm.rate + perBonusAdd.get(1) * 10) {
                    int num =NumberUtil.getRandom(bm.min, bm.max);
                    num += (int) (num * perBonusAdd.get(0) / 100f);
                    ret.addAll(Bonus.viewXNumber(new ArrayList<>(bm.bonus), num));
                    hasBonusOne = true;
                } else per -= bm.rate;
            }
        }
        return ret;
    }
}
