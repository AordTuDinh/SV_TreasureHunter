package game.config;

import com.google.gson.Gson;
import game.config.aEnum.ItemKey;
import game.dragonhero.service.user.Bonus;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;
import protocol.Pbmethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class CfgLottery {
    public static DataConfig config;
    public static List<Integer> indexMini = Arrays.asList(1, 2, 3, 4, 5);
    // time quay normal
    public static int HOUR_SPINE = 18;


    public static List<Long> getFeeBuyMini(int num) {
        return Bonus.viewGem(-config.feeNormal * num);
    }

    public static List<Long> getFeeBuySpecial(int num) {
        return Bonus.viewGem(-config.feeSpecial * num);
    }

    public static boolean hasBuyNormal() {
        int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int min = Calendar.getInstance().get(Calendar.MINUTE);
        if (day == 0 || (day == 6 && hour > HOUR_SPINE)) return false;
        return !(hour == HOUR_SPINE && min < 30); // sau 6h30 mới cho mua cho lần tiếp theo
    }

    public static boolean hasBuySpecial() {
        return !hasBuyNormal();
    }

    public static int getEventIdBuy() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int min = Calendar.getInstance().get(Calendar.MINUTE);
        if (hour < HOUR_SPINE || (hour == HOUR_SPINE && min <= 30)) {
            return DateTime.getNumberDay();
        } else return DateTime.getNumberDay() + 1;
    }

    public static List<Long> getFeeBuyNormal(int num) {
        return Bonus.viewGem(-config.feeMini * num);
    }

    public static List<Long> getTickerMini(int num) {
        return Bonus.viewItem(ItemKey.TICKER_MINI, num);
    }


    public static List<Long> getBonusMini(int type, int index) {
        switch (type) {
            case 1:
                return config.bonusMini1.get(index);
            case 2:
                return config.bonusMini2.get(index);
            case 3:
                return config.bonusMini3.get(index);
            case 4:
                return config.bonusMini4.get(index);
            case 5:
                return config.bonusMini5.get(index);
        }
        return new ArrayList<>();
    }

    public static protocol.Pbmethod.PbListMiniLotte.Builder checkLuckyMini(int numTicker, protocol.Pbmethod.ListCommonVector lscm, List<Long> bonus) {
        protocol.Pbmethod.PbListMiniLotte.Builder builder = protocol.Pbmethod.PbListMiniLotte.newBuilder();
        // gen num lucky
        List<Long> lucky = new ArrayList<>();
        while (lucky.size() < 20) {
            long numLucky = NumberUtil.getRandom(1, 80);
            if (!lucky.contains(numLucky)) lucky.add(numLucky);
        }
        builder.addAllLuckyNum(GsonUtil.toListInt(lucky));
        for (int i = 0; i < numTicker; i++) {
            Pbmethod.PbMiniLotte.Builder pb = Pbmethod.PbMiniLotte.newBuilder();
            // check trung giai
            int type = lscm.getAVector(i).getALongCount();
            protocol.Pbmethod.CommonVector numberSelect = lscm.getAVector(i);
            pb.addAllNumChoose(GsonUtil.toListInt(numberSelect.getALongList()));
            switch (type) {
                case 1 -> {
                    boolean hasValue = numberSelect.getALongList().stream().anyMatch(element -> lucky.contains(element));
                    if (hasValue) {
                        List<Long> reward = CfgLottery.getBonusMini(type, 0);
                        pb.addAllBonus(GsonUtil.toListInt(reward));
                        pb.setPrizeIndex(1);
                        bonus.addAll(reward);
                    } else pb.setPrizeIndex(0);
                }
                case 2 -> {
                    int num = 0;
                    for (int j = 0; j < type; j++) {
                        if (lucky.contains(lscm.getAVector(i).getALong(j))) {
                            num++;
                        }
                    }
                    if (num == 2) {
                        List<Long> reward = CfgLottery.getBonusMini(type, 0);
                        pb.addAllBonus(GsonUtil.toListInt(reward));
                        bonus.addAll(reward);
                        pb.setPrizeIndex(1);
                    } else pb.setPrizeIndex(0);

                }
                case 3 -> {
                    int num = 0;
                    for (int j = 0; j < type; j++) {
                        if (lucky.contains(lscm.getAVector(i).getALong(j))) {
                            num++;
                        }
                    }
                    if (num == 2) {
                        List<Long> reward = CfgLottery.getBonusMini(type, 0);
                        pb.addAllBonus(GsonUtil.toListInt(reward));
                        bonus.addAll(reward);
                        pb.setPrizeIndex(2);
                    } else if (num == 3) {
                        List<Long> reward = CfgLottery.getBonusMini(type, 1);
                        pb.addAllBonus(GsonUtil.toListInt(reward));
                        bonus.addAll(reward);
                        pb.setPrizeIndex(1);
                    } else pb.setPrizeIndex(0);
                }
                case 4 -> {
                    int num = 0;
                    for (int j = 0; j < type; j++) {
                        if (lucky.contains(lscm.getAVector(i).getALong(j))) {
                            num++;
                        }
                    }
                    if (num == 2) {
                        List<Long> reward = CfgLottery.getBonusMini(type, 0);
                        pb.addAllBonus(GsonUtil.toListInt(reward));
                        bonus.addAll(reward);
                        pb.setPrizeIndex(3);
                    } else if (num == 3) {
                        List<Long> reward = CfgLottery.getBonusMini(type, 1);
                        pb.addAllBonus(GsonUtil.toListInt(reward));
                        bonus.addAll(reward);
                        pb.setPrizeIndex(2);
                    } else if (num == 4) {
                        List<Long> reward = CfgLottery.getBonusMini(type, 2);
                        pb.addAllBonus(GsonUtil.toListInt(reward));
                        bonus.addAll(reward);
                        pb.setPrizeIndex(1);
                    } else pb.setPrizeIndex(0);
                }
                case 5 -> {
                    int num = 0;
                    for (int j = 0; j < type; j++) {
                        if (lucky.contains(lscm.getAVector(i).getALong(j))) {
                            num++;
                        }
                    }
                    if (num == 3) {
                        List<Long> reward = CfgLottery.getBonusMini(type, 0);
                        pb.addAllBonus(GsonUtil.toListInt(reward));
                        bonus.addAll(reward);
                        pb.setPrizeIndex(3);
                    } else if (num == 4) {
                        List<Long> reward = CfgLottery.getBonusMini(type, 1);
                        pb.addAllBonus(GsonUtil.toListInt(reward));
                        bonus.addAll(reward);
                        pb.setPrizeIndex(2);
                    } else if (num == 5) {
                        List<Long> reward = CfgLottery.getBonusMini(type, 2);
                        pb.addAllBonus(GsonUtil.toListInt(reward));
                        bonus.addAll(reward);
                        pb.setPrizeIndex(1);
                    } else pb.setPrizeIndex(0);
                }
            }
            builder.addALotte(pb);
        }
        return builder;
    }

    public static boolean checkIndexMini(int index) {
        return indexMini.contains(index);
    }

    public static long getSpecial() {
//        special = DBJPA.getNumber("select special from dson_main.lottery");
//        return special == 0 ? 10 : special;
        return 0L;
    }

    public static void loadConfig(String strJson) {
        config = new Gson().fromJson(strJson, DataConfig.class);
    }

    public class DataConfig {
        public int feeMini;
        public int feeNormal;
        public int feeSpecial;
        public int maxBuyMini;
        public int maxBuyNormal;
        public List<List<Long>> bonusMini1;
        public List<List<Long>> bonusMini2;
        public List<List<Long>> bonusMini3;
        public List<List<Long>> bonusMini4;
        public List<List<Long>> bonusMini5;
        public List<Integer> bonusNormal;
    }
}
