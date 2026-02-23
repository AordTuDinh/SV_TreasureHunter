package game.config.lang;

import com.google.gson.JsonArray;
import game.config.CfgServer;
import game.dragonhero.mapping.UserEntity;
import game.dragonhero.mapping.main.ConfigLanguage;
import game.dragonhero.mapping.main.ConfigResLanguage;
import game.dragonhero.mapping.main.ResTitleEntity;
import game.object.MyUser;
import ozudo.base.database.DBResource;
import ozudo.base.helper.GsonUtil;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Lang implements Serializable {
    public static String err_not_enough_gold = "err_not_enough_gold"; // Không đủ vàng
    public static String err_not_enough_gem = "err_not_enough_gem"; // Không đủ kim cương
    public static String no_need_refresh = "no_need_refresh"; // Không cần làm mới
    public static String err_not_enough_ruby = "err_not_enough_ruby"; // Không đủ Ruby
    public static String err_not_enough_weapon = "err_not_enough_weapon"; // Không đủ vũ khí
    public static String err_not_enough_piece = "err_not_enough_piece"; // Không đủ mảnh
    public static String user_name_exist = "user_name_exist"; //Tên đã tồn tại
    public static String err_name_sake = "err_name_sake"; //Tên mới phải khác tên hiện tại
    public static String name_err_1 = "name_err_1"; // Tên không được chưa ký tự '<', '>' ,'[',']'
    public static String name_not_found = "name_not_found"; // Tên không hợp lệ
    public static String name_err_length = "name_err_length"; // Tên quá dài
    public static String name_err_min_length = "name_err_min_length"; // Tên tối thiểu 6 kí tự
    public static String content_err_length = "content_err_length"; // Nội dung quá dài
    public static String err_intro_max_length = "err_intro_max_length"; // Giới thiệu quá dài
    public static String err_intro = "err_intro"; //Intro không hợp lệ
    public static String err_max_character = "err_max_character"; //Vượt quá số lượng kí tự cho phép
    public static String err_no_clan = "err_no_clan"; // Bạn chưa gia nhập clan nào
    public static String err_max_num_honor = "err_max_num_honor"; // Đã cống hiến tôi đa của ngày hôm nay
    public static String err_not_gift_code = "err_not_gift_code"; // Gift Code không tồn tại
    public static String err_not_gift_code_expire = "err_not_gift_code_expire"; // Gift Code đã hết hạn sử dụng
    public static String err_gift_code_use = "err_gift_code_use"; // Gift code đã được sử dụng
    public static String err_gift_code_use_type = "err_gift_code_use_type"; // Bạn đã nhận giftcode sự kiện này rồi
    public static String err_gift_code_use_not_allow = "err_gift_code_use_not_allow"; // Giftcode không hợp lệ cho tài khoản này
    public static String clan_edit_info = "clan_edit_info"; // %s sửa thông tin bang hội
    public static String locked_skill_in_clan = "locked_skill_in_clan"; // Kĩ năng đang bị khóa
    public static String max_level = "max_level"; // Cấp tối đa!
    public static String err_number_input = "err_number_input"; // Số lượng không hợp lệ
    public static String err_number_error = "err_number_error"; // Số không hợp lệ
    public static String err_number_greater_than_equal = "err_number_greater_than_equal"; // Số phải lớn hơn hoặc bằng %s
    public static String need_join_clan_to_use = "need_join_clan_to_use"; //Cần gia nhập bang hội để sử dụng
    public static String clan_no_clan = "clan_no_clan";
    public static String err_no_bonus = "err_no_bonus"; // Không có gì để nhận
    public static String err_unauthorized = "err_unauthorized"; // Không được phép
    public static String err_in_room_already = "err_in_room_already"; // Bạn đang ở trong bản đồ này
    public static String err_limit_pack_buy = "err_limit_pack_buy"; // Đã hết lượt mua gói này
    public static String err_not_win_lottery = "err_not_win_lottery"; // Bạn không trúng thưởng
    public static String err_not_enough_point = "err_not_enough_point"; // Không đủ điểm
    public static String err_received_bonus = "err_received_bonus"; // Bạn đã nhận phần thưởng này rồi!
    public static String err_received_gift = "err_received_gift"; // Bạn đã nhận phần quà này rồi!
    public static String err_condition_bonus = "err_condition_bonus"; // Chưa đủ điều kiện để nhận
    public static String err_params = "err_params"; // Dữ liệu không hợp lệ
    public static String err_max_xu = "err_max_xu"; // Xu không được vượt quá %s
    public static String err_send_data = "err_send_data"; // Lỗi gửi dữ liệu!
    public static String err_item_number = "err_item_number"; // Số lượng vật phẩm không hợp lệ
    public static String err_use_item_equip = "err_use_item_equip"; // Trang bị đang được sử dụng!
    public static String user_not_found = "user_not_found"; // Người chơi không tồn tại
    public static String user_in_clan = "user_in_clan"; // Người chơi đã tham gia tổ chức
    public static String err_slot = "err_slot"; // Vị trí không hợp lệ
    public static String err_has_item = "err_has_item";// Bạn không sở hữu vật phẩm
    public static String err_has_monster = "err_has_monster";// Bạn đã sở hữu quái thú này rồi
    public static String err_no_forgot = "err_no_forgot";// Không có gì để hoàn trả
    public static String err_max_level = "err_max_level";// Bạn đã đạt cấp độ tối đa
    public static String err_max_land = "err_max_land";// Bạn đã mở tối đa ô đất
    public static String err_not_enough_item = "err_not_enough_item";// Không đủ %s
    public static String err_item_has_been_used = "err_item_has_been_used";// Vật phẩm đã được sử dụng
    public static String err_max_number = "err_max_number";// Bạn đã đạt số lượng tối đa!
    public static String err_has_checkin = "err_has_checkin";// Bạn đã điểm danh rồi!
    public static String err_summon_free = "err_summon_free";// Chưa đến giờ mở hộp miễn phí!
    public static String err_feature_lock = "err_feature_lock";// Chưa đủ cấp độ để sử dụng tính năng này!
    public static String err_string_prefix = "err_string_prefix";// Không được chưa ký tự '<', '>','[',']'
    public static String err_change_chanel = "err_change_chanel";// Không được đổi kênh ở bản đồ này!
    public static String err_open_chanel = "err_open_chanel";// Kênh phải nằm trong khoảng từ 1 đến %s
    public static String success = "success";// Thành công!
    public static String fail = "fail";// Thất bại!

    // hero
    public static String hero_not_own = "hero_not_own";
    public static String err_hero_use = "err_hero_use"; // Nhân vật đang được sử dụng!
    // item
    public static String item_not_own = "item_not_own";
    public static String item_not_found = "item_not_found"; // Không tìm thấy vật phẩm!
    public static String item_out_of_date = "item_out_of_date"; // Vật phẩm đã hết hạn sử dụng!
    public static String err_item_equip_max_level = "err_item_equip_max_level"; // Trang bị đã đạt cấp độ tối đa!
    public static String err_item_equip_has_max_level = "err_item_equip_has_max_level"; // Trang bị cần đạt cấp độ tối đa!
    public static String err_item_equip_expire = "err_item_equip_expire"; // Trang bị đã hết hạn sử dụng
    public static String err_item_level = "err_item_level"; // Không đủ cấp độ để sử dụng
    public static String err_item_expire = "err_item_expire"; // Vật phẩm đã hết hạn sử dụng
    public static String err_item_equip = "err_item_equip"; // Trang bị đang được sử dụng
    public static String err_item_equip_cant_up = "err_item_equip_cant_up"; // Trang bị đang được sử dụng, cần tháo trang bị trước!
    public static String err_item_equip_slot = "err_item_equip_slot"; // Trang bị phải cùng loại!
    public static String err_max_slot = "err_max_slot"; // Túi không đủ chỗ chứa!
    public static String err_full_player = "err_full_player"; // Kênh đầy
    public static String err_item_equip_not_found = "err_item_equip_not_found"; // Trang bị không tồn tại
    public static String err_item_lock_in_bag = "err_item_lock_in_bag"; // Trang bị đang khóa, cần mở khóa trước!
    public static String err_max_buy_lottery_normal = "err_max_buy_lottery_normal"; // Chỉ được mua tối đa %s vé số
    public static String err_number_item_key = "err_number_item_key"; // Không đủ chìa khóa
    // pet
    public static String err_monster_no_care_require = "err_monster_no_care_require"; // Quái thú không cần chăm sóc
    public static String err_pet_no_care_require = "err_pet_no_care_require"; // Thú cưng không cần chăm sóc
    public static String err_pet_can_care = "err_pet_can_care"; // Thú cưng cần chăm sóc
    public static String err_monster_can_care = "err_monster_can_care"; // Quái thú cần chăm sóc
    public static String err_pet_can_not_buy = "err_pet_can_not_buy"; // Bạn đã sở hữu thú cưng này rồi!
    public static String err_monster_can_not_buy = "err_monster_can_not_buy"; //Bạn đã sở hữu quái thú này rồi!
    public static String err_can_buy_one = "err_can_buy_one"; //Chỉ có thể mua 1 vật phẩm
    public static String err_monster_max_star = "err_monster_max_star"; // Quái thú đã đạt sao tối đa
    public static String err_pet_max_star = "err_pet_max_star"; // Thú cưng đã đạt sao tối đa
    // arena
    public static String err_arena_has_defense = "err_arena_has_defense"; // Cần thiết lập đội hình phòng thủ trước
    public static String err_arena_has_select_weapon = "err_arena_has_select_weapon"; // Cần trang bị vũ khí cho anh hùng
    //friend
    public static String err_max_buy = "err_max_buy"; // Hết lượt mua
    public static String err_max_can_buy = "err_max_can_buy"; // Vượt quá số lượng mua cho phép
    public static String err_level_buy = "err_level_buy"; // Cần đạt cấp độ %s để mở khóa
    public static String err_friend_not_same_server = "err_friend_not_same_server";// Người chơi không cùng server
    public static String err_max_number_friend = "err_max_number_friend";// Đã đạt tối đa số lượng bạn bè!
    public static String err_friend_already_be = "err_friend_already_be";// Người này đã là bạn bè!
    public static String request_add_friend_success = "request_add_friend_success"; // Gửi yêu cầu kết bạn thành công!
    public static String player_was_applied = "player_was_applied"; //Người chơi này đã gửi lời kết bạn tới bạn.
    public static String no_friend_in_list = "no_friend_in_list"; //Người chơi không phải là bạn bè!
    public static String send_mail_successful = "send_mail_successful"; // Gửi thư thành công!
    public static String err_friend_send_bonus = "err_friend_send_bonus"; // Bạn đã gửi quà cho người này rồi!
    public static String err_friend_send_bonus_all = "err_friend_send_bonus_all"; // Bạn đã gửi quà cho tất cả bạn bè rồi!
    // upgrade
    public static String upgrade_done = "upgrade_done";// Nâng cấp thành công !
    public static String no_upgrade_now = "no_upgrade_now";// Không thể nâng cấp thêm !
    public static String invalid_location = "invalid_location";// Vị trí không hợp lệ !
    public static String equip_done = "equip_done";// Trang bị thành công!
    public static String err_has_weapon = "err_has_weapon";// Bạn đã sở hữu tất cả vũ khi cấp bậc này!

    // Clan
    public static String clan_skill_upgrade_error = "clan_skill_upgrade_error"; // Cường công và đa dụng không được lệnh quá 50 điểm
    public static String clan_not_enough_position = "clan_not_enough_position"; // Bạn không có quyền thực hiện chức năng này
    public static String err_reset_skill = "err_reset_skill"; // Không có điểm kỹ năng, không thể reset
    public static String user_function_level_required = "user_function_level_required";// Lên cấp %s để mở chức năng này!
    public static String clan_name_exist = "clan_name_exist";// Tên bang hội đã tồn tại
    public static String clan_quest_err = "clan_quest_err";// Cần hoàn thành tất cả nhiệm vụ đang có
    public static String clan_name_change = "clan_name_change";// Phí đổi tên tổ chức là %s Kim Cương
    public static String clan_leader_required = "clan_leader_required";// Chỉ đội trưởng mới thực hiện được chứng năng này
    public static String clan_leave_first = "clan_leave_first";//  Bạn phải rời bang trước đã!
    public static String clan_name_min_length = "clan_name_min_length";// Tên bang tối thiểu 4 ký tự
    public static String clan_name_max_length = "clan_name_max_length";// Tên bang tối đa 100 ký tự
    public static String clan_wait_leave1 = "clan_wait_leave1";// Người chơi phải chờ %s trước khi gia nhập bang hội mới
    public static String clan_not_found = "clan_not_found";// Bang hội không tồn tại
    public static String clan_name_character = "clan_name_character";// Tên bang phải có ít nhất 1 ký tự chữ cái
    public static String clan_leader_coleader_required = "clan_leader_coleader_required";// Chỉ chủ bang hoặc phó bang mới thực hiện được chứng năng này
    public static String clan_mail_length = "clan_mail_length";// Độ dài tối đa là 255 ký tự
    public static String clan_new_position_error = "clan_new_position_error";// Vị trí mới không phù hợp
    public static String clan_message_1 = "clan_message_1"; // %s thành lập Bang hội %s
    public static String clan_message_2 = "clan_message_2"; // %s đuổi %s khỏi Hạm Đội
    public static String clan_message_3 = "clan_message_3"; // %s vừa tham gia vào Hạm Đội
    public static String clan_message_4 = "clan_message_4"; // %s rời Hạm Đội
    public static String clan_message_5 = "clan_message_5"; // Thành viên không tồn tại
    public static String clan_message_6 = "clan_message_6"; // Không đủ quyền để đặt chức vụ cho thành viên này
    public static String clan_message_7 = "clan_message_7"; // Không đặt được chức vụ lớn hơn bản thân
    public static String clan_message_8 = "clan_message_8"; // Người chơi offline quá 5 ngày không được làm Thuyền trưởng
    public static String clan_message_9 = "clan_message_9"; // %s vừa được bổ nhiệm làm %s bởi %s
    public static String clan_message_10 = "clan_message_10"; // %s vừa giáng chức làm %s bởi %s
    public static String clan_message_11 = "clan_message_11"; // %s vừa được bổ nhiệm làm %s, vì %s không đăng nhập trên 5 ngày
    public static String clan_message_12 = "clan_message_12"; // Chúc mừng bạn đã được duyệt vào bang %s
    public static String clan_message_13 = "clan_message_13"; // %s đã dùng %s điểm cống hiến để tăng kinh nghiệm bang
    public static String clan_message_14 = "clan_message_14"; // %s đã cống hiến %s kim cương cho bang
    public static String clan_message_15 = "clan_message_15"; // %s đã cống hiến %s vàng cho bang
    public static String clan_message_16 = "clan_message_16"; // %s sử dụng %s cống hiến để mở Boss
    public static String clan_max_member = "clan_max_member";
    public static String clan_application_processing = "clan_application_processing";
    public static String clan_application_rejected = "clan_application_rejected";
    public static String user_not_allow_function = "user_not_allow_function";
    public static String clan_kick_error = "clan_kick_error";
    public static String clan_kick_too_many = "clan_kick_too_many";
    public static String clan_leader_leave_error = "clan_leader_leave_error"; //Bang chủ không được thoát
    public static String clan_wait_leave = "clan_wait_leave"; //Hãy chờ %s trước khi thoát khỏi bang
    // Auth
    public static String err_user_not_exist = "err_user_not_exist"; // Người chơi không tồn tại.
    public static String err_user_not_online = "err_user_not_online"; // Người chơi không online!
    public static String auth_invalid = "err_user_not_exist"; //Đăng nhập không hợp lệ
    public static String err_login = "err_login"; //Đăng nhập thất bại
    public static String err_system_down = "err_system_down"; //Hệ thống đang bận
    public static String err_use_chanel = "err_use_chanel"; // Bạn đang ở kênh này!
    public static String err_has_username = "err_login"; //Tên đăng nhập đã được sử dụng
    public static String err_login_orther = "err_login_orther"; //Tài khoản đang được đăng nhập ở nơi khác

    public static String err_user_block = "err_user_block"; //Tài khoản của bạn hiện đang bị khóa
    // chat
    public static String chat_too_quick = "chat_too_quick"; // Chat quá nhanh!
    public static String chat_msg_invalid = "chat_msg_invalid";
    public static String err_connect_network = "err_connect_network"; // Lỗi kết nối!
    public static String err_room_not_found = "err_room_not_found"; // Map không tồn tại!
    public static String err_chat_block = "err_chat_block"; // Tài khoản của bạn đang bị cấm chat
    public static String err_user_friend_not_online = "err_user_friend_not_online"; // Người chơi này không online, hãy gửi thư cho họ!
    // campaign
    public static String err_lock_map_level = "err_lock_map_level"; //Bạn chưa đủ điều kiện để mở khóa bản đồ!
    public static String err_lock_map_power = "err_lock_map_power"; //Bạn chưa đủ lực chiến để mở khóa bản đồ!
    // quest
    public static String err_quest_done = "err_quest_done"; //Bạn chưa hoàn thành nhiệm vụ!
    public static String err_quest_not_found = "err_quest_not_found"; // Không tìm thấy nhiệm vụ!
    //tower
    public static String err_max_level_tower = "err_max_level_tower"; // Bạn đã đánh đến tháp cuối cùng
    public static String need_to_pass_first = "need_to_pass_first"; // Cần phải chiến thắng tầng này trước
    // farm
    public static String err_farm_has_tree = "err_farm_has_tree"; // Ô đất đã có cây rồi
    public static String err_farm_no_has_tree = "err_farm_no_has_tree"; // Bạn chưa sở hữu cây này
    public static String err_farm_has_buy_tree = "err_farm_has_buy_tree"; // Bạn đã mua cây này rồi
    public static String err_farm_not_tree = "err_farm_not_tree"; // Ô đất đang không trồng cây
    public static String err_farm_not_use = "err_farm_not_use"; // Không thể sử dụng ở giai đoạn này
    public static String err_farm_has_fertilize = "err_farm_has_fertilize"; // Bạn đã sử dụng ở ô đất này rồi
    public static String err_farm_has_fer_time = "err_farm_has_fer_time"; // Chỉ có thể sử dụng thuốc tăng trưởng một lần
    public static String err_farm_harvest_time = "err_farm_harvest_time"; // Chưa đến giờ thu hoạch
    public static String err_npc_farm = "err_npc_farm"; // Bạn cần mua gói hỗ trợ nông trại để sử dụng tính năng này.
    public static String err_npc_refuse_perform = "err_npc_refuse_perform"; // NPC từ chối thực hiện yêu cầu
    public static String err_not_enough_item_farm = "err_not_enough_item_farm"; // Không đủ nông phẩm
    public static String err_not_enough_item_tool = "err_not_enough_item_tool"; // Không đủ vật dụng
    public static String err_not_enough_item_food = "err_not_enough_item_food"; // Không đủ thức ăn
    public static String err_no_plant = "err_no_plant"; // Đã gieo hết tất cả ô đất
    public static String err_no_farm_slot = "err_no_farm_slot"; // Bạn đang không có ô đất nào!
    public static String err_no_pluck = "err_no_pluck"; // Không có gì để dọn
    public static String err_no_harvest = "err_no_harvest"; // Không có gì để thu hoạch
    public static String err_farm_action = "err_farm_action"; // Không thể thực hiện
    public static String err_farm_not_fer_time = "err_farm_not_fer_time"; // Bạn đã sử dụng ở tất cả ô đất!
    public static String err_not_enough_item_seed = "err_not_enough_item_seed"; // Không đủ hạt giống
    public static String farm_quest_body_auto_delete = "farm_quest_body_auto_delete"; // Hệ thống tự động nhận %s đơn hàng hoàn thành và gửi quà vào thư
    public static String farm_quest_title_auto_delete = "farm_quest_title_auto_delete"; // Đơn hàng nông trại
    public static String farm_quest_not_exist_quest = "farm_quest_not_exist_quest"; // Đơn hàng không tồn tại
    public static String quest_unfinished = "quest_unfinished"; // Đơn hàng không thành công
    public static String err_farm_quest_not_lock_quest = "err_farm_quest_not_lock_quest"; // Không thể khóa đơn hàng


    // event
    public static String err_require_level_to_receive = "err_require_level_to_receive"; // Phải dạt cấp độ %s để nhận
    public static String err_has_buy_pack_event = "err_has_buy_pack_event"; // Cần mua gói sự kiện để nhận thưởng
    public static String err_event_done = "err_event_done"; // Bạn đã hoàn thành sự kiện!


    public static String disagree_request = "disagree_request"; //Người chơi %s đã từ chối lời mời của bạn!
    public static String err_user_in_battle = "err_user_in_battle"; //Người chơi đang trong mini game!
    public static String err_sold_out = "err_sold_out"; //Hết lượt mua
    public static String err_event_end = "err_event_end"; //Sự kiện đã kết thúc
    public static String err_event_not_active = "err_event_not_active"; //Sự kiện đang không diễn ra
    public static String err_num_reset = "err_num_reset"; //Đã hết lượt thay đổi nhiệm vụ trong ngày!


    public static String err_buy_ticker_normal = "err_buy_ticker_normal"; // Hết thời gian mua, vui lòng mua sau 18 giờ 30 phút từ thứ 2 đến thứ 7.
    public static String err_buy_ticker_special = "err_buy_ticker_special"; // Hết thời gian mua, vui lòng mua sau 18 giờ 30 phút từ thứ 7 đến chủ nhật
    // spine
    public static String label_spin = "label_spin";
    public static String not_enough_chip = "not_enough_chip";
    // arena
    public static String need_set_def_arena_first = "need_set_def_arena_first"; // cần phải bố trí đội hình phòng thủ trước
    public static String def_team_empty = "def_team_empty"; // đội hình phòng thủ không được để trống
    public static String attack_team_empty = "attack_team_empty"; // đội hình tấn công không được để trống
    public static String need_set_attack_arena_first = "need_set_attack_arena_first"; // cần phải bố trí đội hình tấn công trước


    public static String LOCALE_VI = "vi", LOCALE_EN = "en", LOCALE_KM = "km",LOCALE_RU = "ru", LOCALE_ZH = "zh", LOCALE_JP = "jp" ;
    public static String err_buy_iap_fail = "err_buy_iap_fail"; // Mua thất bại
    public static String err_can_buy_pack = "err_can_buy_pack"; // Cần mua gói
    public static String err_max_quest_farm = "err_max_quest_farm";  //"Tối đa %s nhiệm vụ"
    public static String err_null_quest = "err_null_quest";  //"Nhiệm vụ không tồn tại"
    public static String err_no_has_dame_skin = "err_no_has_dame_skin"; // Bạn đang không sở hữu hiệu ứng này!
    public static String err_no_has_chat_frame = "err_no_has_chat_frame"; // Bạn đang không sở hữu khung chat này!
    public static String err_no_has_trial = "err_no_has_trial"; // Bạn đang không sở hữu bóng mờ này!
    public static String err_vip_to_use = "err_vip_to_use"; // Cần đạt vip %s để sử dụng tính năng này
    public static String err_has_level_for_buy = "err_has_level_for_buy"; // Cần đạt cấp %s để mua
    public static String eat_lunch = "eat_lunch"; //Bữa trưa
    public static String eat_dinner = "eat_dinner"; //Bữa tối
    public static String has_level = "has_level"; //Đạt cấp độ
    public static String quy_truong_thanh = "quy_truong_thanh"; //Quỹ trưởng thành
    public static String quy_truong_thanh2 = "quy_truong_thanh2"; //Đạt cấp độ để nhận quà
    public static String moi_ngay = "moi_ngay"; //"Mỗi ngày:"
    public static String qua_nap_tien = "qua_nap_tien"; //"Quà nạp tiền"
    public static String qua_nap_tien2 = "qua_nap_tien2"; // Nạp số tiền bất kỳ mỗi ngày để tích lũy nhận thưởng.
    public static String qua_nap_tien3 = "qua_nap_tien3"; // Quà giới hạn <color=yellow> 800%</color>
    public static String qua_nap_tien4 = "qua_nap_tien4"; // Phúc lợi miễn phí
    public static String day = "day"; // Ngày
    public static String week = "week"; // "Tuần"
    public static String month = "month"; // Tháng

    // World Boss / Party
    public static String err_need_party_to_boss = "err_need_party_to_boss"; // Bạn cần tham gia tổ đội để đánh boss
    public static String err_only_leader_can_start = "err_only_leader_can_start"; // Chỉ trưởng nhóm mới có quyền bắt đầu
    public static String err_not_time_yet = "err_not_time_yet"; // Chưa đến thời gian đánh
    public static String err_need_at_least_2_members = "err_need_at_least_2_members"; // Cần ít nhất 2 thành viên để tấn công
    public static String err_need_3_heroes = "err_need_3_heroes"; // Cần đủ 3 tướng để tham gia
    public static String msg_invite_sent_to_party = "msg_invite_sent_to_party"; // Đã gửi lời mời đến nhóm
    public static String msg_no_member_online = "msg_no_member_online"; // Không có thành viên online
    public static String msg_x_invite_you_boss = "msg_x_invite_you_boss"; // %s mời bạn tham gia đánh boss
    public static String err_only_leader_can_invite = "err_only_leader_can_invite"; // Chỉ trưởng nhóm mới có quyền gửi lời mời
    public static String err_player_not_online = "err_player_not_online"; // Người chơi không online!
    public static String err_player_has_party = "err_player_has_party"; // Người chơi đã có nhóm rồi!
    public static String err_party_full = "err_party_full"; // Nhóm đã đủ thành viên!
    public static String err_waiting_player_response = "err_waiting_player_response"; // Đang chờ người chơi phản hồi!
    public static String err_too_many_invites = "err_too_many_invites"; // Bạn đã gửi quá nhiều lời mời đến người chơi này
    public static String msg_x_invite_you_party = "msg_x_invite_you_party"; // Người chơi %s gửi lời mời bạn vào nhóm!
    public static String msg_invite_sent = "msg_invite_sent"; // Đã gửi lời mời vào nhóm
    public static String msg_x_reject_party_invite = "msg_x_reject_party_invite"; // Người chơi %s đã từ chối lời mời bạn vào nhóm!
    public static String err_leave_current_party_first = "err_leave_current_party_first"; // Bạn cần phải rời nhóm hiện tại trước
    public static String err_party_not_exist = "err_party_not_exist"; // Nhóm không tồn tại!
    public static String err_only_leader_can_kick = "err_only_leader_can_kick"; // Chỉ trưởng nhóm mới được phép xóa
    public static String msg_kicked_from_party = "msg_kicked_from_party"; // Bạn đã bị kick khỏi nhóm!
    public static String msg_party_disbanded = "msg_party_disbanded"; // Nhóm không đủ thành viên nên đã giải tán
    public static String msg_kicked_success = "msg_kicked_success"; // Đã kích khỏi nhóm
    public static String msg_party_disbanded_done = "msg_party_disbanded_done"; // Đã giải tán nhóm!
    public static String msg_left_party = "msg_left_party"; // Đã rời nhóm!
    public static String msg_x_joined_party = "msg_x_joined_party"; // %s đã tham gia nhóm
    public static String msg_x_left_party = "msg_x_left_party"; // %s đã rời nhóm
    public static String msg_x_left_party_disbanded = "msg_x_left_party_disbanded"; // %s đã rời nhóm, nhóm đã bị giải tán!
    public static String msg_leader_changed = "msg_leader_changed"; // Trưởng nhóm đã thay đổi
    public static String err_not_in_party = "err_not_in_party"; // Bạn đang không tham gia nhóm nào!
    public static String err_not_leader = "err_not_leader"; // Bạn không phải là trưởng nhóm
    public static String err_can_only_disband = "err_can_only_disband"; // Chỉ có thể giải tán nhóm
    // Clan
    public static String err_not_enough_clan_honor = "err_not_enough_clan_honor"; // Không đủ cống hiến bang!
    public static String err_min_10_gem = "err_min_10_gem"; // Tối thiểu 10 kim cương
    public static String err_not_enough_honor = "err_not_enough_honor"; // Không đủ cống hiến
    public static String err_boss_daily_limit = "err_boss_daily_limit"; // Đã hết lượt đánh boss hôm nay
    public static String err_clan_boss_open = "err_clan_boss_open"; // Boss bang đang mở
    public static String countdown_teleport_boss = "countdown_teleport_boss"; // Dịch chuyển đến boss sau {0}
    // Farm
    public static String err_not_enough_items = "err_not_enough_items"; // Không đủ vật phẩm
    public static String farm_quest_order = "farm_quest_order"; // Đơn hàng số %s
    // Login / Server
    public static String msg_server_not_open = "msg_server_not_open"; // Server chưa mở, server Kiến Lập mở lúc 11h ngày
    public static String msg_server_maintenance = "msg_server_maintenance"; // Server đang bảo trì, bạn quay lại sau
    public static String countdown_server_update = "countdown_server_update"; // Server update dữ liệu sau {0}
    public static String msg_disconnect_idle = "msg_disconnect_idle"; // Ngắt kết nối do không hoạt động!
    // IAP
    public static String err_transaction_cancelled_refund = "err_transaction_cancelled_refund"; // Giao dịch đã bị hủy hoặc hoàn tiền.
    public static String err_payment_processing = "err_payment_processing"; // Thanh toán đang được xử lý. Vui lòng chờ vài phút.
    public static String err_verify_failed = "err_verify_failed"; // XÁC MINH THẤT BẠI
    // Pet
    public static String msg_pet_full_energy = "msg_pet_full_energy"; // Tràn đầy năng lượng
    public static String msg_pet_healthier = "msg_pet_healthier"; // Khỏe hơn rồi
    // Battle
    public static String err_already_hit_this_boss = "err_already_hit_this_boss"; // Bạn đã đánh boss này rồi, hãy tham gia boss khác!
    // Arena
    public static String err_save_fail_need_hero_weapon = "err_save_fail_need_hero_weapon"; // Lưu thất bại, cần lưu đầy đủ hero và vũ khí!
    // Event
    public static String event_monthly_goal = "event_monthly_goal"; // Mục tiêu tháng
    public static String event_monthly_goal_name = "event_monthly_goal_name"; // Mục tiêu hàng tháng
    // Login / Facebook
    public static String err_wrong_login = "err_wrong_login"; // Sai thông tin đăng nhập
    public static String err_not_login_facebook = "err_not_login_facebook"; // Bạn chưa đăng nhập facebook
    public static String err_facebook_login_fail = "err_facebook_login_fail"; // Đăng nhập Facebook thất bại. Hãy thử lại sau
    // Mail
    public static String mail_card_week_daily = "mail_card_week_daily"; // Quà thẻ Tuần mỗi ngày
    public static String mail_card_month_daily = "mail_card_month_daily"; // Quà thẻ Tháng mỗi ngày
    public static String mail_card_forever_daily = "mail_card_forever_daily"; // Quà thẻ Vĩnh Viễn mỗi ngày
    public static String mail_refund_close_beta = "mail_refund_close_beta"; // Hoàn trả gói nạp Close Beta
    public static String mail_pack_bonus = "mail_pack_bonus"; // Phần quà gói: %s
    public static String mail_clan_boss_top = "mail_clan_boss_top"; // Chúc mừng bạn đạt top %s sát thương boss bang hội
    public static String mail_event_top_level = "mail_event_top_level"; // Chúc mừng bạn đạt top %s trong sự kiện đua top Cấp độ
    public static String mail_reset_hero_bonus = "mail_reset_hero_bonus"; // Bù quà reset nhân vật
    public static String mail_world_boss_week = "mail_world_boss_week"; // Phần thưởng tổng kết Boss Thế Giới
    public static String mail_world_boss_top = "mail_world_boss_top"; // Chúc mừng bạn đã đạt top %s. Hãy cố gắng phát huy để đạt thành tích tốt hơn.
    public static String mail_arena_week = "mail_arena_week"; // Phần thưởng tổng kết tuần ĐẤU TRƯỜNG
    public static String mail_arena_day = "mail_arena_day"; // Phần thưởng tổng kết ngày ĐẤU TRƯỜNG
    public static String mail_arena_top = "mail_arena_top"; // Chúc mừng bạn đã đạt top %s đấu trường. Hãy cố gắng phát huy để đạt thành tích tốt hơn.
    public static String mail_gift_loan_tin = "mail_gift_loan_tin"; // Quà Loan Tin
    public static String mail_sender_system = "mail_sender_system"; // Hệ Thống
    // User / Chat ban
    public static String msg_chat_banned_until = "msg_chat_banned_until"; // Tài khoản bị cấm chat đến %s
    // Tutorial / God names
    public static String god_fire = "god_fire"; // Kim Thần
    public static String god_water = "god_water"; // Thủy Thần
    public static String god_flame = "god_flame"; // Hỏa Thần
    public static String god_earth = "god_earth"; // Thổ Thần

    static Map<String, Integer> keyIndex = new HashMap<String, Integer>();
    static Map<String, String> keyMap = new HashMap<String, String>();

    private static Map<String, Lang> mlang = new HashMap<>() {{
        put(LOCALE_VI, LangVi.instance());
        put(LOCALE_EN, LangEn.instance());
        put(LOCALE_KM, LangKm.instance());
        put(LOCALE_JP, LangJp.instance());
        put(LOCALE_RU, LangRu.instance());
        put(LOCALE_ZH, LangZh.instance());
    }};

    static {
        List<ConfigLanguage> tmp = DBResource.getInstance().getList(CfgServer.DB_MAIN + "config_language", ConfigLanguage.class);
        for (int i = 0; i < tmp.size(); i++) {
            ConfigLanguage cLang = tmp.get(i);
            keyIndex.put(cLang.getK(), i);
            keyMap.put(LOCALE_VI + "_" + cLang.getK(), cLang.getVi());
            keyMap.put(LOCALE_EN + "_" + cLang.getK(), cLang.getEn());
            keyMap.put(LOCALE_KM + "_" + cLang.getK(), cLang.getKm());
            keyMap.put(LOCALE_JP + "_" + cLang.getK(), cLang.getJp());
            keyMap.put(LOCALE_RU + "_" + cLang.getK(), cLang.getRu());
            keyMap.put(LOCALE_ZH + "_" + cLang.getK(), cLang.getZh());
        }
        List<ResTitleEntity> listTitle = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_title", ResTitleEntity.class);
        for (int i = 0; i < listTitle.size(); i++) {
            keyMap.put(LOCALE_VI + "_" + listTitle.get(i).getK(), listTitle.get(i).getVi());
            keyMap.put(LOCALE_EN + "_" + listTitle.get(i).getK(), listTitle.get(i).getEn());
            keyMap.put(LOCALE_KM + "_" + listTitle.get(i).getK(), listTitle.get(i).getKm());
            keyMap.put(LOCALE_JP + "_" + listTitle.get(i).getK(), listTitle.get(i).getJp());
            keyMap.put(LOCALE_RU + "_" + listTitle.get(i).getK(), listTitle.get(i).getRu());
            keyMap.put(LOCALE_ZH + "_" + listTitle.get(i).getK(), listTitle.get(i).getZh());
        }
        List<ConfigResLanguage> resLanguages = DBResource.getInstance().getList(CfgServer.DB_MAIN + "config_res_language", ConfigResLanguage.class);
        for (int i = 0; i < resLanguages.size(); i++) {
            ConfigResLanguage resLanguage = resLanguages.get(i);
            keyMap.put(LOCALE_VI + "_" + resLanguage.getK(), resLanguage.getVi());
            keyMap.put(LOCALE_EN + "_" + resLanguage.getK(), resLanguage.getEn());
            keyMap.put(LOCALE_KM + "_" + resLanguage.getK(), resLanguage.getKm());
            keyMap.put(LOCALE_JP + "_" + resLanguage.getK(), resLanguage.getJp());
            keyMap.put(LOCALE_RU + "_" + resLanguage.getK(), resLanguage.getRu());
            keyMap.put(LOCALE_ZH + "_" + resLanguage.getK(), resLanguage.getZh());
        }
    }


    public static String getTitle(String locale, String key) {
        return instance(locale).get(key);
    }

    public static String getTitle(MyUser myUser, String key) {
        return instance(myUser.getUser().getLang()).get(key);
    }

    public static Lang instance(String locale) {
        locale = locale == null ? "en" : locale;
        locale = getValidLang(locale.toLowerCase());
        Lang lang = mlang.get(locale);
        return lang == null ? instance(CfgServer.config.mainLanguage) : lang;
    }
    public static Lang instance(MyUser mUser) {
        return instance(mUser.getUser().getLang());
    }

    String locale = "";

    public String getLocale() {
        return locale;
    }

    public boolean isEn() {
        return locale.equalsIgnoreCase(LOCALE_EN);
    }

    public boolean isVi() {
        return locale.equalsIgnoreCase(LOCALE_VI);
    }

    public abstract String get(String key);

    public String get(String key, String languageCode) {
        String value = keyMap.get(languageCode + "_" + key);
        return value == null ? "NA" : value;
    }

    public static String format(String input, String[] replace) {
        return String.format(input, replace);
    }

    static List<String> validLanguage = Arrays.asList(LOCALE_VI, LOCALE_EN, LOCALE_KM,LOCALE_RU,LOCALE_JP,LOCALE_ZH);

    public static String getValidLang(String value) {
        if (validLanguage.contains(value)) {
            return value;
        }
        return validLanguage.get(1);
    }


    public String formatMessage(String message) {
        if (message.startsWith("[") && message.endsWith("]")) {
            JsonArray arr = GsonUtil.parseJsonArray(message);
            String key = arr.get(0).getAsString();
            String[] params = new String[arr.size() - 1];
            for (int i = 0; i < params.length; i++) {
                String param = arr.get(i + 1).getAsString();
                params[i] = param.startsWith("[") ? formatMessage(param) : param;

            }
            return String.format(get(key), params);
        }
        return message;
    }

    public static String toKey(String key) {
        return Arrays.asList(key).toString();
    }
}
