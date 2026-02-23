package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum PackType {
    CARD_MONTH(2),
    WPASS(3),
    FARM_MONTH(4),
    FARM_BO_NPC(5),
    QUEST_B(6),
    AFK_ADD_TIME(7),
    SIEU_CAP(8),
    SIEU_GIA_TRI(9),
    UU_DAI(10),
    CAO_CAP(11),
    THE_VINH_VIEN(12),
    THE_THANG(13),
    THE_TUAN(14),
    UU_DAI_CHIEU_MO(15),
    UU_DAI_MOI_NGAY(16),
    GOI_TAI_NGUYEN_TAN_THU(17),
    GOI_TAI_NGUYEN_BANG_HOI(18),
    GOI_TAI_NGUYEN_NONG_TRAI(19),
    GOI_MAY_MAN(20),
    CHIEU_MO_TUAN(21),
    XU_GIA_TRI(22),
    XU_MAY_MAN(23),
    LIEN_MINH_GIA_TRI(24),
    CUONG_HOA_CAP_1(25),
    CUONG_HOA_CAP_2(26),
    CUONG_HOA_CAP_3(27),
    CUONG_HOA_CAP_4(28),
    CUONG_HOA_CAP_5(36),
    // gói tháng
    GOI_THANG_SO_1(29),
    GOI_THANG_SO_2(30),
    GOI_THANG_SO_3(31),
    GOI_THANG_SO_4(32),
    QUY_TRUONG_THANH(33),
    GOI_KI_NANG_1(34),
    GOI_KI_NANG_2(35),
    ;

    public final int value;

    PackType(int value) {
        this.value = value;
    }

    // lookup
    static Map<Integer, PackType> lookup = new HashMap<>();

    static {
        for (PackType itemType : values()) {
            lookup.put(itemType.value, itemType);
        }
    }

    public static PackType get(int packId) {
        return lookup.get(packId);
    }

}
