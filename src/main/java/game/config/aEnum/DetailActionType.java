package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum DetailActionType {
    UPGRADE_WEAPON("upgrade_weapon_id_"), // Nâng cấp vũ khí id
    PIECE_GRAFT("piece_grap_type_"), // Ghép mảnh
    NHAN_THU("mail_"), // Nhận Mail theo id
    BONUS_KILL_ENEMY("bonus_kill_enemy"), // Quà fam trong bản đồ
    UPGRADE_WEAPON_ACCESSORY("upgrade_weapon_accessory_"), // Nâng cấp vũ khí phụ kiện id
    BUY_TREE_FARM("buy_tree_farm_id_"), // Mua cây id
    POINT_ACHIEVEMENT("bonus_point_achievement_tab_"),// Nhận quà thành tựu bên ngoài
    ACHIEVEMENT_RECEIVE("achievement_receive"),//Nhận quà thành tựu bên trong từng tab
    CASINO_REFRESH("casino_refresh"),// Làm mới spine
    RECEIVE_TUTORIAL_QUEST("receive_tutorial_quest_id_"),// Nhận quà tutorial quest
    REVIVE_PLAYER("revive_player"),// Hồi sinh
    CAMPAIGN_CONQUER("campaign_conquer_"),// Nhận bonus campaign conquer map id
    CAMPAIGN_SMART("campaign_smart_"),// Càn quét map id , number
    BUY_LOTTERY_MINI("buy_lottery_mini"),// Mua vé số nhỏ
    CHANG_NAME_CLAN("change_name_clan"),// Đổi tên clan
    DIEM_DANH_BANG_HOI("check_in_clan"),
    NANG_KI_NANG_BANG("clan_skill"),
    BONUS_CLAN_QUEST("bonus_clan_quest_"), // quest clan
    BONUS_CLAN_CONTRIBUTE("bonus_clan_contribute_x"), // cong hien
    BONUS_CLAN_DYNAMIC("bonus_clan_dynamic_"), // nhận năng động id
    BONUS_CLAN_DYNAMIC_ALL("bonus_clan_dynamic_all"), // nhận năng động all
    BONUS_CLAN_DYNAMIC_BOX("bonus_clan_dynamic_box_"), // nhận box năng động số lượng
    RESET_KI_NANG_BANG("reset_clan_skill"),
    NHIEM_VU_HANG_NGAY("daily_quest_"),
    NHIEM_VU_HANG_NGAY_2("quest_bonus_"),
    BONUS_QUEST_B("bonus_quest_c_"),
    BONUS_GIFT_CODE("bonus_gift_code_"),
    SU_DUNG_VE_SO_NHO("lottery_mini_use"),
    SU_DUNG_ITEM("used_item_"),
    MUA_CHIP_VONG_QUAY("buy_chip"),
    BUY_SHOP("buy_shop_"),
    REFRESH_SHOP("refresh_shop_"),
    REFRESH_SHOP_FAIL("refresh_shop_fail_"),
    CHE_TAO_TRANG_BI("create_item_equipment"),
    PHA_HUY_TRANG_BI("decay_item_equipment"),
    NANG_CAP_TRANG_BI("upgrade_item_equipment"),
    NANG_KI_NANG2("stat_2_upgrade"),
    NANG_KI_NANG1("stat_1_upgrade"),
    SUMMON_STONE("summon_stone_"),
    SUMMON_STONE_ADS("summon_stone_ads"),
    SUMMON_PIECE("summon_piece_"),
    SUMMON_PIECE_ADS("summon_piece_ads"),
    SUMMON_STONE_TICH_LUY("summon_stone_accum"),
    SUMMON_PIECE_TICH_LUY("summon_piece_accum"),
    BUY_SLOT_BAG("buy_slot_bag_"),
    BUY_SLOT_BAG_FAIL("buy_slot_bag_fail"),
    DIEM_DANH_HANG_NGAY("daily_check_in_"),
    PHAN_THUONG_BOSS_GOD("bonus_boss_god_"),
    PHAN_THUONG_LEO_THAP("bonus_tower_"),
    CREATE_WEAPON("create_weapon_"),  // Tạo mới vũ khí id
    MAKE_PIECE("make_piece_"),  // Hợp thành mảnh id
    STONE_COMBINE("stone_combine_"),  // Hợp thành đá id
    BUY_GOLD_SLOT("buy_gold_slot_"),  // Mua vàng slot (0,1,2) 0 free
    EVENT_1_HOUR("event_1_hour_slot_"),  // Nhận sk online 1h slot
    EVENT_FREE_100_SCROLL("event_free_100_scroll"),  // Nhận sk free 100 cuộn chiêu mộ
    EVENT_FREE_DAME_SKIN("event_free_dame_skin"),  // Nhận sk free dame skin
    EVENT_14_DAY("event_14_day_slot_"),  // Nhận sk online 14 ngày
    EVENT_7_DAY("event_7_day_"),  // Nhận sk 7 ngay
    EVENT_14_DAY_RE_TICK("event_14_day_re_tick_slot_"),  // Điểm danh bù sự kiện 14 ngày
    EAT_LUNCH("event_eat_lunch"),  // Ăn trưa
    EAT_DINNER("event_eat_dinner"),  // Ăn tối
    EVENT_MONTH_NORMAL("event_month_normal_slot_"),  // Sự kiện tháng thường
    EVENT_MONTH_VIP("event_month_vip_slot_"),  // Sự kiện tháng vip
    BUY_LAND("buy_lan_slot_"),  // Mua ô đất mới
    FARM_PLANT("farm_plant_"),  // Trồng cây ID
    HARVEST_TREE("harvest_tree_id_"),  // Thu hoạch cây ID
    QUICK_HARVEST_TREE("quick_harvest_tree"),  // Thu hoạch cây nhanh
    FARM_FERTILIZE("farm_fertilize_"), // tăng trưởng cây bằng id
    FARM_PLUCK("farm_pluck_"), //Nhổ cây id
    FARM_FER_TIME("farm_fer_time_"),// Giảm thời gian bằng id
    SELL_AGRI("sell_agri"), // Bán item nông sản
    SELL_FARM_SINGLE("sell_farm_single_"), // Bán farm id
    HARVEST_FARM("harvest_farm_"), // Thu hoạch id slot
    CREATE_FOOD("create_food_"), // Tạo món id
    QUICK_PLANT("quick_harvest"), // Gieo hạt nhanh
    QUICK_HARVEST("quick_harvest"), // Thu hoạch nhanh
    QUICK_VIP_PLUCK("quick_vip_pluck"), // Dọn vườn nhanh
    QUICK_FERTILIZE("quick_fertilize_"), // Kích tăng trưởng id
    QUICK_FER_TIME("quick_fer_time_"), // Giảm thời gian thu hoạch bằng id
    RESET_SPINE_NORMAL("reset_spin_normal"), // Reset vòng quay may mắn
    ROTATE_SPINE_NORMAL("rotate_spin_normal"), // Quay vòng quay normal\
    UPDATE_BONUS_NEXT_DAY("bonus-next-day"), //Xóa các item qua ngày hêt hạn
    BUY_PACK("buy_pack_id_"), // Mua gói id
    BUY_IAP("buy_iap_id_"), // Mua gói id
    RECEIVE_LOTTERY("receive_lottery_normal_event_"), // Nhận thưởng vé số event id
    REVIVE_FEE_10("revive_fee"), // Gỉam 10% exp
    SLIDER_7DAY_REWARD("slider_7_day_"), //  Nhận quà event 7 day id
    TOWER_BUY_KEY_("tower_buy_key_"), // Mua lượt đánh tháp
    UPDATE_FAIL("update_fail"), // Update db thất bại nên hoàn trả lại tiền
    TOWER_SMART("tower_smart_"), // Quét tháp
    GET_BONUS_AFK("get_bonus_afk"), // Nhận quà afk
    BONUS_FRIEND_GET("bonus_friend_get_"),//Nhận quà bạn bè tặng
    BONUS_FRIEND_SEND("bonus_friend_send_"),// Tặng quà bạn bè
    BONUS_QUICK_SEND("bonus_quick_friend_send"),//Nhận quà bạn bè tặng
    ARENA_ATTACK("arena_attack"),//Quà đấu trường
    UU_DAI_NGAY_FREE("uu_dai_ngay_free"),//ưu đãi hằng ngày free
    PHUC_LOI_FREE("phuc_loi_free"),//phúc lợi hằng ngày free
    GIOI_HAN_FREE("gioi_han_free"),//giới hạn hằng ngày free
    GET_QUA_NAP_TIEN("qua_nap_tien_"),// quà nạp tiền
    GET_QUY_TRUONG_THANH("quy_truong_thanh_"),// qũy trưởng thành
    GET_UU_DAI_NGAY("uu_dai_ngay_"),// mua gói ưu đãi ngày all
    VIP_BONUS("vip_bonus_"),// nhận quà lên cấp vip
    BUY_ARENA_TICKET("buy_arena_ticket_"),// Mua vé lượt đánh arena
    GET_CELL_MONTH_NORMAL("get_cell_event_panel_month_normal_"),// Nhận quà cell event panel month normal
    GET_CELL_MONTH_VIP("get_cell_event_panel_month_vip_"),// Nhận quà cell event panel month vip
    PIECE_GRAFT_MONSTER("piece_graft_monster_"),// Chế tạo pet id từ mảnh
    GET_REWARD_COLLECTION_MONSTER("reward_collection_monster"),// nhận quà bonus collection monster
    GET_REWARD_COLLECTION_PET("reward_collection_pet"),// nhận quà bonus collection pet
    MONSTER_CARE_ID("monster_care_id_"),// Chăm sóc thú id
    PET_CARE_ID("pet_care_id_"),// Chăm sóc thú id
    UP_STAR_MONSTER("up_star_monster"),  // nâng sao quái thú
    UP_STAR_PET("up_star_pet"),  // nâng sao thú cưng
    SUMMON_PET("summon_pet_x"), // summon pet x lần
    BUY_DECO_FARM("buy_deco_farm_id_"),// mua deco farm
    BONUS_ATTACK_ARENA("bonus_attack_arena_vs_id_"),// bonus khi danh vs user id
    USE_SCROLL_FARM_QUEST("scroll_farm_quest_"),// dùng cuộn nhiệm vụ farm
    Farm_QUEST_RESET("farm_quest_reset"),// reset đơn hàng nông trại
    FARM_QUEST_SPEED_UP("farm_quest_speed_up_"), // hoàn thành nhanh đơn hàng nông trại
    FARM_QUEST_RECEIVE_QUEST("farm_quest_receive_quest_"), // Nhận phần thưởng đơn hàng
    START_FARM_QUEST("start_farm_quest_id_"),// Gửi đơn hàng theo id
    ATTACK_ARENA("attack_arena"), // đánh đấu trường
    SMART_BOSS("smart_boss_num_"), // càn quét boss
    BONUS_COMMUNITY("bonus_community_"), // bonus cộng đồng
    BUY_EVENT_TIMER("buy_event_timer_"),// mua gói hạn giờ
    BONUS_UPGRADE_CLAN_QUEST("bonus_upgrade_clan_quest_"), // nâng cấp clan quest index
    BONUS_BOSS_PARTY("bonus_boss_party"), // quà đánh boss party
    RECEIVE_BONUS_HONOR("receive_bonus_honor_"),  // nhận quà bonus cống hiến bang theo ngày
    CLAN_HONOR("clan_honor"),  // donate clan
    ATTACK_BOSS_SOLO("attack_boss_solo"),  // donate clan
    CLEAR_ITEM_EVENT_DROP("clear_item_event_drop"),  // xoá item của event drop
    BONUS_ATTACK_BOSS_CLAN("bonus_attack_boss_clan"),  // qua danh boss clan
    ;
    private final String key;

    DetailActionType(String key) {
        this.key = key;
    }

    // lookup
    static Map<String, DetailActionType> lookup = new HashMap<>();

    static {
        for (DetailActionType detail : values()) {
            lookup.put(detail.key, detail);
        }
    }

    public String getKey(Object... params) {
        return params == null || params.length == 0 ? key : key + params[0].toString();
    }

    public static DetailActionType get(int type) {
        return lookup.get(type);
    }

}
