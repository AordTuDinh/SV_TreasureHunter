package game.config.aEnum;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum ItemKey {
    BOX_NORMAL_SHURIKEN(1, "box_normal_shuriken", 0),
    BOX_ADVANCED_SHURIKEN(2, "box_advanced_shuriken", 0),
    BOX_RARE_SHURIKEN(3, "box_rare_shuriken", 0),
    BOX_HERO_SHURIKEN(4, "box_hero_shuriken", 0),
    BOX_LEGENDARY_SHURIKEN(5, "box_legendary_shuriken", 0),
    BOX_DIVINE_SHURIKEN(6, "box_divine_shuriken", 0),
    BOX_UTIMATE_SHURIKEN(7, "box_utimate_shuriken", 0),
    EGG_PETS(8, "title_egg_pet", 0),
    GOLD(9, "title_gold", 0),
    GEM(10, "title_gem", 0),
    EXP(11, "title_exp", 0),
    DROP_25(12, "title_drop_25", 0),
    EXP_25(13, "title_exp_25", 0),
    GOLD_25(14, "title_gold_25", 0),
    BINH_MAU_DO(15, "title_binh_mau", 0),
    BINH_MAU_CAM(16, "title_binh_mau", 0),
    BINH_MAU_TRANG(17, "title_binh_mau", 0),
    BINH_MANA_XANH(18, "title_binh_mana", 0),
    BINH_MANA_TIM(19, "title_binh_mana", 0),
    BINH_MANA_VANG(20, "title_binh_mana", 0),
    BANH_QUY_VANG(21, "title_banh_quy", 0),
    BANH_QUY_XANH(22, "title_banh_quy", 0),
    BANH_QUY_DO(23, "title_banh_quy", 0),
    THE_CAN_QUET(24, "title_the_can_quet", 0),
    THE_HOAN_TRA_1(25, "title_the_hoan_tra_1", 0),
    VE_HOI_SINH(26, "title_hoi_sinh", 0),
    THAN_THIEN(28, "title_than_thien", 0),
    DA_CUONG_HOA(29, "da_cuong_hoa", 0),
    TICKER_MINI(30, "ve_so_nho", 0),
    TICKER_NORMAL(31, "ve_so", 0),
    TICKER_SPECIAL(32, "ve_so_dac_biet", 0),
    CHIP(33, "chip", 0),//Dùng để quay vòng quay may mắn
    SCROLL_SUMMON_SPECIAL(34, "scroll_summon_special", 0),
    LUCKY_COIN(35, "lucky_coin", 0),

    DA_CUONG_HOA_VIP(36, "da_cuong_hoa_vip", 0),

    DA_CHE_TAO(37, "da_che_tao", 0),

    HUY_HIEU_BANG(38, "huy_hieu_bang", 0),

    SCROLL_SUMMON(39, "cuon_chieu_mo", 0),

    DA_KIM1(40, "DA_KIM1", 45),

    DA_THUY1(41, "DA_THUY1", 46),

    DA_HOA1(42, "DA_HOA1", 47),

    DA_MOC1(43, "DA_MOC1", 48),

    DA_THO1(44, "DA_THO1", 49),

    DA_KIM2(45, "DA_KIM2", 50),

    DA_THUY2(46, "DA_THUY2", 51),

    DA_HOA2(47, "DA_HOA2", 52),

    DA_MOC2(48, "DA_MOC2", 53),

    DA_THO2(49, "DA_THO2", 54),

    DA_KIM3(50, "DA_KIM3", 55),

    DA_THUY3(51, "DA_THUY3", 56),

    DA_HOA3(52, "DA_HOA3", 57),

    DA_MOC3(53, "DA_MOC3", 58),

    DA_THO3(54, "DA_THO3", 59),

    DA_KIM4(55, "DA_KIM4", 60),

    DA_THUY4(56, "DA_THUY4", 61),

    DA_HOA4(57, "DA_HOA4", 62),

    DA_MOC4(58, "DA_MOC4", 63),

    DA_THO4(59, "DA_THO4", 64),

    DA_KIM5(60, "DA_KIM5", 65),

    DA_THUY5(61, "DA_THUY5", 66),

    DA_HOA5(62, "DA_HOA5", 67),

    DA_MOC5(63, "DA_MOC5", 68),

    DA_THO5(64, "DA_THO5", 69),

    DA_KIM6(65, "DA_KIM6", 0),

    DA_THUY6(66, "DA_THUY6", 0),

    DA_HOA6(67, "DA_HOA6", 0),

    DA_MOC6(68, "DA_MOC6", 0),

    DA_THO6(69, "DA_THO6", 0),
    NANG_LUONG(70, "NANG_LUONG", 0),
    QUEST_B(71, "QUEST_B", 0),
    XU_DAU_TRUONG(72, "", 0),
    ARENA_TICKET(73, "", 0),
    HUY_HIEU_VANG(74, "", 0),
    XU_NONG_TRAI(79, "", 0),
    BONG_LINH_THU(87, "", 0), // dùng làm nguyên liệu bắt pet
    RUBY(88, "", 0),
    THE_HOAN_TRA_2(89, "title_the_hoan_tra_2", 0),
    FARM_QUEST_SCROLL(91, "", 0),// Dùng để nhận đơn hàng mới trong nông trại
    FARM_QUEST_SCROLL_VIP(92, "", 0),// Dùng để nhận đơn hàng đặc biệt trong nông trại
    BOSS_TICKET(93, "", 0),// Dùng để chiến đấu trong Hầm Ngục
    FARM_TOOL(94, "", 0),// Mở ra nhận được các công cụ hỗ trợ nông trại
    BAG_ORE_1(95, "", 0),//Mở ra nhận được các loại quặng
    BAG_ORE_2(96, "", 0),//Mở ra nhận được các loại quặng
    BAG_ORE_3(97, "", 0),//Mở ra nhận được các loại quặng
    BAG_ORE_4(98, "", 0),//Mở ra nhận được các loại quặng
    BAG_ORE_5(99, "", 0),//Mở ra nhận được các loại quặng
    QUANG_CAP_1(100, "", 101),//Quặng cấp 1
    QUANG_CAP_2(101, "", 102),//Quặng cấp 2
    QUANG_CAP_3(102, "", 103),//Quặng cấp 3
    QUANG_CAP_4(103, "", 104),//Quặng cấp 4
    QUANG_CAP_5(104, "", 105),//Quặng cấp 5
    QUANG_CAP_6(105, "", 106),//Quặng cấp 6
    BONG_SIEU_THU(106, "", 0),//Sử dụng để thu thập Linh Thú, có tỉ lệ thu thập linh thú tốt hơn.
    DA_TIEN_HOA_CAP_1(107, "", 108),// Sử dụng để nâng cấp Linh Thú Cấp 1
    DA_TIEN_HOA_CAP_2(108, "", 109),// Sử dụng để nâng cấp Linh Thú Cấp 2
    DA_TIEN_HOA_CAP_3(109, "", 0),// Sử dụng để nâng cấp Linh Thú Cấp 3
    DA_TIEN_HOA_VU_KHI(111, "", 0),// Sử dụng để tiến hóa vũ khí
    EXP_50(112, "", 0),// Sử dụng để nâng cấp Linh Thú Cấp 3
    GOLD_50(113, "", 0),// Sử dụng để nâng cấp Linh Thú Cấp 3
    DROP_50(114, "", 0),// Sử dụng để nâng cấp Linh Thú Cấp 3
    EXP_X2(115, "", 0),// Sử dụng để nâng cấp Linh Thú Cấp 3
    GOLD_X2(116, "", 0),// Sử dụng để nâng cấp Linh Thú Cấp 3
    DROP_X2(117, "", 0),// Sử dụng để nâng cấp Linh Thú Cấp 3
    STONE_TREASURE(118, "", 0),// Sử dụng để nâng cấp bảo vật
    BUA_HOAN_TRA_VU_KHI(119, "", 0),// Sử dụng để hoàn trả vũ khí
    BOSS_SOLO_TICKER(120, "", 0),// Sử dụng để danh boss solo
    ;

    public final int id;
    public final String title;  // for lang
    public final int nextId;

    ItemKey(int id, String title, int nextId) {
        this.id = id;
        this.title = title;
        this.nextId = nextId;
    }

    static final List<Integer> itemMedicine = List.of(BINH_MAU_DO.id, BINH_MAU_CAM.id, BINH_MAU_TRANG.id, BINH_MANA_XANH.id,
            BINH_MANA_TIM.id, BINH_MANA_VANG.id, BANH_QUY_VANG.id, BANH_QUY_XANH.id, BANH_QUY_DO.id
    );


    // lookup
    static Map<Integer, ItemKey> lookup = new HashMap<>();

    static {
        for (ItemKey key : values()) {
            lookup.put(key.id, key);
        }
    }

    public static boolean isItemMedicine(int itemId) {
        return itemMedicine.contains(itemId);
    }

//    public static boolean isItemUpgradeWeapon(int itemId) {
//        return itemStoneUpgrade.contains(itemId);
//    }

    public static ItemKey get(int key) {
        return lookup.get(key);
    }
}
