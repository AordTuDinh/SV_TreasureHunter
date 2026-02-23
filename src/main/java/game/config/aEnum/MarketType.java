package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum MarketType {
    TYPE_UNLIMITED(1), // ko save : không có thời gian và không giới hạn số lượt mua
    TYPE_REFRESH(2), //  save : không giới hạn lượt mua, có thể reset
    TYPE_STOCK(3), //save : chỉ reset bằng item. Có giới hạn lượt mua
    TYPE_STOCK_REFRESH(4), // save : có giới hạn lượt mua và có reset theo thời gian.

    ;
    //
    public static final int ITEM_AVAILABLE = 1;
    public static final int ITEM_OUT_OF_STOCK = 0;

    public final int id;

    MarketType(int id) {
        this.id = id;
    }

    // lookup
    static Map<Integer, MarketType> lookup = new HashMap<>();

    static {
        for (MarketType market : values())
            lookup.put(market.id, market);
    }

    public static MarketType get(int id) {
        return lookup.get(id);
    }
}