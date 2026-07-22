package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

/** res_item_point.point_id — đồng bộ pbdson.ItemPointKey proto. */
public enum ItemPointKey {
    CO_VAT(1),
    TICKER_MINI(2),
    TICKER_NORMAL(3),
    TICKER_SPECIAL(4),
    CHIP(5),
    BONG_LINH_THU(6),
    BONG_SIEU_THU(7),
    LOA_THE_GIOI(8),
    /** Vé x2 ruby khi nạp — res_item_point.point_id = 13 */
    RUBY_X2_VOUCHER(13),
    /** Xu đấu trường — res_item_point.point_id = 14 */
    ARENA_COIN(14),
    /** Ô khai thác — trừ khi phá cell; res_item_point.point_id = 15 */
    PLOT(15);

    public final int id;

    ItemPointKey(int id) {
        this.id = id;
    }

    static final Map<Integer, ItemPointKey> lookup = new HashMap<>();

    static {
        for (ItemPointKey key : values())
            lookup.put(key.id, key);
    }

    public static ItemPointKey get(int id) {
        return lookup.get(id);
    }

    public static boolean isPointKey(int id) {
        return lookup.containsKey(id);
    }

    public static boolean isLotteryTicket(int id) {
        return id == TICKER_NORMAL.id || id == TICKER_SPECIAL.id;
    }
}
