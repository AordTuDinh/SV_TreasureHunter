package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum EventType {
    ONLINE_1H(101),
    LOGIN_14(102),
    FIRST_PURCHASE(103),
    OPEN_SV_7_DAY(104),
    EVENT_MONTH(105),
    UU_DAI_NGAY(107),
    DAC_QUYEN(108),
    QUA_NAP_TIEN(109),
    DIEM_DANH(110),
    QUA_GIOI_HAN(111),
    VIP_NONG_TRAI(112),
    VIP(113),
    TARGET_MONTH(114),
    GET_SUPPORT(115),
    COMMUNITY(116),
    FREE_100_SCROLL(117),
    FREE_DAME_SKIN(118),
    EVENT_SEVEN_DAY(119),
    QUY_TRUONG_THANH(120),
    GIFT_CODE(121),
    ;

    public final int value;

    EventType(int value) {
        this.value = value;
    }

    // lookup
    static Map<Integer, EventType> lookup = new HashMap<>();

    static {
        for (EventType itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public static EventType get(int type) {
        return lookup.get(type);
    }

}
