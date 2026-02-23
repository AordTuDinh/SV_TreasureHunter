package game.config;

import com.google.gson.Gson;

import java.util.List;

public class CfgBag {
    public static DataConfig config;

    public static void loadConfig(String strJson) {
        config = new Gson().fromJson(strJson, DataConfig.class);
    }

    public static int maxSlotItem() {
        return config.numSlotItem + config.priceSlot.size();
    }

    public static int maxSlotEquipment() {
        return config.numSlotEquipment + config.priceSlot.size();
    }

    public static int maxSlotPiece() {
        return config.numSlotPiece + config.priceSlot.size();
    }

    public static String genBaseSlot() {
        return "[" + config.numSlotItem + "," + config.numSlotEquipment + "," + config.numSlotPiece + "]";
    }

    public static int getPriceSot(int curSlot, int number, int type) {
        int price = 0;
        int slotBase = config.numSlotItem;
        if (type == 2) {
            slotBase = config.numSlotEquipment;
        }
        for (int i = 0; i < number; i++) {
            int slot = curSlot - slotBase + i;
            price += config.priceSlot.get(slot);
        }
        return price;
    }

    public class DataConfig {
        public int numSlotItem;
        public int numSlotEquipment;
        public int numSlotPiece;
        public List<Integer> priceSlot;
    }
}
