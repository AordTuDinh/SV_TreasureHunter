package game.config;

import com.google.gson.Gson;
import game.object.DataDaily;

import java.util.Arrays;
import java.util.List;

public class CfgDaily {
    public static DataConfig config;
    static final List<Integer> slots = Arrays.asList(0, 1, 2);

    public static boolean checkSlotGold(int slot) {
        return slots.contains(slot);
    }

    public static boolean checkHasBuyGold(DataDaily uDaily, int slot) {
        switch (slot) {
            case 0 -> {
                return uDaily.getValue(DataDaily.BUY_GOLD_0) == 0;
            }
            case 1 -> {
                return uDaily.getValue(DataDaily.BUY_GOLD_1) == 0;
            }
            case 2 -> {
                return uDaily.getValue(DataDaily.BUY_GOLD_2) == 0;
            }
        }
        return false;
    }


    public static void loadConfig(String strJson) {
        config = new Gson().fromJson(strJson, DataConfig.class);
    }

    public class DataConfig {
        public List<Integer> gemFee;
        public List<Integer> goldBuy;
    }
}
