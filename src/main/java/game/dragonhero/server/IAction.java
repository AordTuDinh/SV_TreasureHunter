package game.dragonhero.server;

import java.util.Arrays;
import java.util.List;

public class IAction {
    public static final int MSG_TOAST = 1;
    public static final int MSG_POPUP = 2;
    public static final int MSG_SLIDE = 3;
    public static final int GAME_CONFIG = 4;
    public static final int WARNING_AFK = 5;
    public static final int POPUP_INFO = 6;
    public static final int LOGIN_REQUIRE = 7;
    public static final int CONNECT_SOCKET_SUCCESS = 8; // for client
    public static final int CONNECT_SOCKET_FAIL = 9;// for client
    public static final int LOGIN_GAME = 10;
    public static final int LOGIN_GAME_FAIL = 11;
    public static final int BATTLE_CONFIG = 12;
    public static final int PING_GAME = 13;
    public static final int LOGOUT = 14;
    public static final int NOTIFY = 15;
    public static final int CHANGE_SERVER = 16;
    public static final int SERVICE_ERROR = 17;
    public static final int ADD_NOTIFY = 18;
    public static final int LOGIN_GAME_BLOCK = 19;


    //region resource
    public static final int SERVER_INFO = 20;
    public static final int INIT_MAP = 21;
    public static final int JOIN_MAP = 22;
    public static final int TABLE_STATE = 23;
    public static final int CLIENT_INPUT = 24;
    public static final int REVIVE_PLAYER = 25;
    public static final int CHANGE_AUTO_MODE = 26;
    public static final int CHANGE_ITEM_SLOT = 27;
    public static final int CHANGE_AUTO_SLOT = 28;
    public static final int CHANGE_CHANEL = 29;
    public static final int COUNTDOWN_MSG = 30;
    public static final int DEBUG_LOG = 31;
    public static final int DEBUG_ERROR = 32;
    public static final int BONUS_TOAST = 33;
    public static final int BONUS_TOAST_POSITIVE = 34;
    public static final int INIT_BOSS = 35;
    public static final int UPDATE_BONUS_PRIVATE = 36;
    public static final int INIT_BACK_HOME = 37;
    public static final int ROOM_INFO = 38;
    public static final int BONUS_LAND = 39;

    //region battle
    public static final int START_GAME = 40;
    public static final int END_GAME = 41;
    public static final int SMART_BOSS = 42;
    public static final int BOSS_GOD_DATA = 43;
    // disconect by message
    public static final int DISCONNECT_MSG = 44;
    public static final int CHANGE_LANG = 45;

    // other
    public static final int DAME_SKIN_EQUIP = 50;
    public static final int BUFF_INFO = 51;
    public static final int CHAT_FRAME_EQUIP = 52;
    public static final int TRIAL_EQUIP = 53;
    public static final int USE_GIFT_CODE = 54;

    //
    public static final int INIT_MAP_BY_TYPE_ID = 60;
    public static final int CAMPAIGN_DATA = 61;
    public static final int CAMPAIGN_REWARD = 62;
    public static final int CAMPAIGN_SMART = 63;
    // chat
    public static final int CHAT_SERVER = 70;
    public static final int CHAT_MAP = 71;
    public static final int CHAT_FRIEND_LIST = 72;
    public static final int CHAT_FRIEND = 73;
    public static final int CHAT_BLOCK = 74;
    public static final int CHAT_UN_BLOCK = 75;
    public static final int CHAT_SETTING = 76;
    public static final int CHAT_FRIEND_NOTIFY = 77;
    //panel notify
    public static final int POPUP_NOTIFY = 78;


    //  service user
    public static final int POINT_DATA = 99;
    public static final int USER_DATA_INFO = 100;
    public static final int GOLD_STAT_STATUS = 101;
    public static final int GOLD_STAT_UPGRADE = 102;
    public static final int LEVEL_STAT_STATUS = 103;
    public static final int LEVEL_STAT_UPGRADE = 104;
    public static final int CREATE_NAME = 105;
    public static final int CHANGE_INTRO = 106;
    public static final int UPDATE_NUM_POINT_LEVEL = 121;
    public static final int CHANGE_NAME = 180;
    public static final int HELP_VALUE = 181;
    public static final int USER_INFO = 182;
    public static final int UPDATE_NEXT_DAY = 183;
    //tower
    public static final int TOWER_STATUS = 184;
    public static final int TOWER_BUY_TURN = 185;
    public static final int TOWER_ATTACK = 186;
    public static final int TOWER_SMART = 187;

    // summon normal
    public static final int SUMMON_STATUS = 107;
    public static final int SUMMON_STONE = 108;
    public static final int SUMMON_REWARD_STONE = 109;
    // summon special
    public static final int SUMMON_PIECE = 110;
    public static final int SUMMON_REWARD_PIECE = 111;
    public static final int SUMMON_STONE_ADS = 138;
    public static final int SUMMON_PIECE_ADS = 139;

    // mail
    public static final int MAIL_LIST = 112;
    public static final int MAIL_RECEIVE = 113;
    public static final int MAIL_DELETE = 114;
    public static final int EQUIP_WEAPON = 117;
    public static final int AUTO_PROMOTE = 118;
    public static final int WEAPON_INFO = 454;
    // bag
    public static final int BAG_STATUS = 119;
    public static final int BAG_BUY_SLOT = 120;
    // tutorial
    public static final int TUTORIAL_STATUS = 122;
    // quest
    public static final int TUTORIAL_QUEST_STATUS = 123;
    public static final int TUTORIAL_QUEST_RECEIVE = 124;
    public static final int TUTORIAL_QUEST_UPDATE = 125;
    public static final int TUTORIAL_GO_TO = 126;
    // market
    public static final int MARKET_STATUS = 128;
    public static final int MARKET_BUY = 129;
    public static final int MARKET_REFRESH = 130;
    // piece
    public static final int PIECE_GRAFT = 131;
    // collection monster
    public static final int MONSTER_COLLECTION_STATUS = 132;
    public static final int MONSTER_COLLECTION_REWARD = 133;
    public static final int MONSTER_COLLECTION_CARE = 134;
    public static final int MONSTER_COLLECTION_UP_STAR = 135;
    public static final int MONSTER_COLLECTION_GET_STAR = 136;
    public static final int PET_INFO = 137;
    // collection pet
    public static final int PET_SUMMON = 142;
    public static final int PET_COLLECTION_STATUS = 143;
    public static final int PET_COLLECTION_REWARD = 144;
    public static final int PET_COLLECTION_CARE = 145;
    public static final int PET_COLLECTION_UP_STAR = 146;
    public static final int PET_COLLECTION_GET_STAR = 147;
    public static final int PET_SELECT = 148;
    // avatar
    public static final int AVATAR_LIST = 140;
    public static final int AVATAR_CHOOSE = 141;
    // quest hàng ngày
    public static final int QUEST_STATUS = 200;
    public static final int QUEST_RECEIVE = 201;
    public static final int QUEST_REWARD_BAR = 202;
    // quest C
    public static final int QUEST_B_STATUS = 205;
    public static final int QUEST_B_RECEIVE_QUEST = 207;
    //afk nhận quà
    public static final int AFK_STATUS = 208;
    public static final int AFK_GET_BONUS = 209;

    // clan
    public static final int CLAN_CREATE = 300; // tạo bang
    public static final int CLAN_APPLICATION_LIST = 301; //
    public static final int CLAN_REQ = 302; // xin gia nhập
    public static final int CLAN_ANSWER_REQ = 303; // trả lời request
    public static final int CLAN_MEMBER_LIST = 304; // danh sách thành viên
    public static final int CLAN_INFO = 305; // thông tin bang hội
    public static final int CLAN_KICK_MEMBER = 306; // kích thành viên
    public static final int CLAN_LEAVE = 307;
    public static final int CLAN_FINDING = 308; // tìm kiếm bang hội
    public static final int CLAN_CHECKIN = 309; // điểm danh bang
    public static final int CLAN_SET_POSITION = 310;
    public static final int CLAN_USER_UPDATE_STATE = 311;
    public static final int CLAN_MAIL_TO_MEMBER = 312;
    public static final int CLAN_CHANGE_NAME = 313;
    public static final int CLAN_BOSS_STATUS = 317;
    public static final int CLAN_BOSS_ACTIVE = 318;
    public static final int CLAN_CHANGE_AVATAR_INTRO = 319;
    public static final int CLAN_CHAT_LIST = 321;
    public static final int CLAN_CHAT = 322;
    public static final int CLAN_ACCEPT_MEMBER = 323;
    public static final int CLAN_SET_JOIN_RULE = 340;
    // clan contribute
    public static final int CLAN_LIST_QUEST = 324;
    public static final int CLAN_UPGRADE_QUEST = 325;
    public static final int CLAN_RECEIVE_QUEST = 326;
    public static final int CLAN_START_QUEST = 351;
    public static final int CLAN_CONTRIBUTE_INFO = 327;
    public static final int CLAN_CONTRIBUTE = 328;
    public static final int CLAN_CONTRIBUTE_TOP = 329;
    // clan dynamic
    public static final int CLAN_DYNAMIC_STATUS = 346;
    public static final int CLAN_DYNAMIC_DETAIL = 347;
    public static final int CLAN_DYNAMIC_REWARD = 348;
    public static final int CLAN_DYNAMIC_REWARD_BOX = 349;
    // clan up level
    public static final int CLAN_UP_LEVEL = 350;
    public static final int CLAN_HONOR_STATUS = 352;
    public static final int CLAN_HONOR_GET_BONUS = 353;
    public static final int CLAN_HONOR = 354;
    // clan skill
    public static final int CLAN_SKILL_STATUS = 314;
    public static final int CLAN_SKILL_UPGRADE = 315;
    public static final int CLAN_SKILL_RESET = 316;

    // Friend
    public static final int FRIEND_STATUS = 330;
    public static final int FRIEND_LIST = 331;
    public static final int FRIEND_LIST_REQ = 332;
    public static final int FRIEND_RECOMMEND = 333;
    public static final int FRIEND_SEND_REQUEST = 334;
    public static final int SEND_MAIL = 335;
    public static final int FRIEND_RESPONSE_APPLY = 337;
    public static final int FRIEND_DELETE = 336;
    public static final int FRIEND_SEND_BONUS = 338;
    public static final int FRIEND_CHECK_ONLINE = 339;
    public static final int FRIEND_FIND = 342;
    public static final int FRIEND_NEW = 343;
    public static final int FRIEND_GET_BONUS = 344;
    public static final int FRIEND_QUICK_GIFT = 345;

    // Item equipment
    public static final int ITEM_EQUIPMENT_INFO = 400;
    public static final int ITEM_EQUIPMENT_EQUIP = 401;
    public static final int ITEM_EQUIPMENT_UN_EQUIP = 402;
    public static final int ITEM_EQUIPMENT_LOCK_STATUS = 403;
    public static final int ITEM_EQUIPMENT_LOCK_DESTROY = 404;
    public static final int ITEM_EQUIPMENT_SELECT_ACCESSORY = 405;
    public static final int ITEM_EQUIPMENT_UPGRADE_ACCESSORY = 406;
    public static final int ITEM_EQUIPMENT_VIEW_INFO = 407;
    // Smithy : Lò rèn
    public static final int SMITHY_STATUS = 410;
    public static final int SMITHY_CREATE = 411;
    public static final int SMITHY_DECAY = 412;
    public static final int SMITHY_UPGRADE = 413;
    // smithy 2 : npc 2
    public static final int SMITHY_PIECE_STATUS = 415;
    public static final int SMITHY_MAKE_WEAPON = 422;
    public static final int SMITHY_UP_LEVEL_WEAPON = 423;
    public static final int SMITHY_COMBINE = 424;
    // buy gold
    public static final int BUY_GOLD_STATUS = 425;
    public static final int BUY_GOLD_BUY = 426;
    // Achievement : thành tựu
    public static final int ACHIEVEMENT_STATUS = 416;
    public static final int ACHIEVEMENT_REWARD = 417;
    public static final int ACHIEVEMENT_INFO = 418;
    public static final int ACHIEVEMENT_RECEIVE = 419;
    // Ranking : Bảng xếp hạng
    public static final int RANKING_INFO = 420;
    public static final int RANKING_STATUS = 421;
    // Farm : Nông trại
    public static final int FARM_STATUS = 427;
    public static final int FARM_BUY_LAND = 428;
    public static final int FARM_CARE = 429;
    public static final int FARM_SELL_ITEM = 430;
    public static final int FARM_SELL_SINGLE = 431;
    public static final int FARM_CREATE_FOOD = 432;
    public static final int FARM_QUICK_CARE = 433;
    public static final int FARM_BUY_ITEM = 434;
    public static final int FARM_HARVEST_TREE = 435;
    public static final int FARM_PING = 436;
    public static final int FARM_TREE_STATUS = 437;

    // Tavern
    public static final int FARM_QUEST_STATUS = 460;
    public static final int FARM_QUEST_USE_ITEM = 461;
    public static final int FARM_QUEST_REFRESH = 462;
    public static final int FARM_QUEST_SPEED_UP = 463;
    public static final int FARM_QUEST_LOCK_UNLOCK = 464;
    public static final int FARM_QUEST_START = 465;
    public static final int FARM_QUEST_RECEIVE = 466;
    public static final int FARM_QUEST_CANCEL = 467;

    // Arena Don Dau
    public static final int ARENA_STATUS = 468;
    public static final int ARENA_ATTACK = 445;
    public static final int ARENA_HISTORY = 446;
    public static final int ARENA_REFRESH = 447;
    public static final int ARENA_BUY_TICKET = 448;
    public static final int ARENA_SET_DEF = 449;
    public static final int ARENA_VIEW_OPP = 450;
    public static final int ARENA_QUIT = 455;
    public static final int ARENA_START_BATTLE = 456;

    // Item
    public static final int ITEM_REMOVE = 451;
    public static final int ITEM_USED = 452;
    public static final int ITEM_INFO = 453;
    public static final int ITEM_USE_FOR_ITEM = 457;

    // Hero
    public static final int HERO_LIST = 500;
    public static final int CHANGE_HERO = 501;

    // Event
    public static final int RPS_SEND_RQ = 600;
    public static final int RPS_RECEIVE_RQ = 601;
    public static final int RPS_SELECT_RQ = 602;
    public static final int RPS_SELECT_RESULT = 603;
    public static final int RPS_RESULT = 604;
    // VÉ SỐ NHỎ
    public static final int LOTTERY_MINI_BUY = 607;
    public static final int LOTTERY_MINI_USE = 608;
    // VÉ SỐ THƯỜNG
    public static final int LOTTERY_VIEW = 610;
    public static final int LOTTERY_HISTORY = 611;
    public static final int LOTTERY_RECEIVE = 612;
    // LUCKY SPINE NORMAL
    public static final int LUCKY_SPINE_STATUS = 616;
    public static final int LUCKY_SPINE_REFRESH = 617;
    public static final int LUCKY_SPINE_ROTATE = 618;
    public static final int LUCKY_SPINE_BUY_CHIP = 619;

    // ONLINE 1 HOUR
    public static final int EVENT_ACTIVE = 624;
    // ONLINE 14 DAY
    public static final int EVENT_14D_STATUS = 627;
    public static final int EVENT_14D_REWARD = 628;
    public static final int EVENT_14D_RE_TICK = 629;

    // buy pack
    public static final int EVENT_BUY_PACK = 638;
    public static final int EVENT_LIST_PACK = 639;
    // event first purchase
    public static final int EVENT_FI_PU_STATUS = 640;
    // event welfare
    public static final int WELFARE_ACTIVE = 641;
    public static final int WELFARE_STATUS = 642;
    public static final int WELFARE_GET_FREE = 643;
    public static final int WELFARE_GET_CELL = 644;
    // event community
    public static final int EVENT_COMMUNITY_STATUS = 645;
    public static final int EVENT_COMMUNITY_REWARD = 646;
    // event community
    public static final int EVENT_FREE_100_STATUS = 647;
    public static final int EVENT_FREE_100_REWARD = 648;
    // event free dame skin
    public static final int EVENT_FREE_DAME_SKIN_STATUS = 649;
    public static final int EVENT_FREE_DAME_SKIN_REWARD = 652;

    //IAP
    public static final int IAP_STATUS = 650;
    public static final int IAP_BUY = 665;
    public static final int IAP_BUY_QR = 664;
    public static final int IAP_GG = 663;
    // event timer
    public static final int EVENT_TIMER_LIST = 653;
    public static final int EVENT_TIMER_INFO = 654;
    public static final int EVENT_TIMER_BUY = 658;
    public static final int EVENT_TIMER_ACTIVE = 661;


    // event button group
    public static final int EVENT_GROUP_ACTIVE = 655;
    public static final int EVENT_GROUP_STATUS = 656;
    public static final int EVENT_GROUP_GET_CELL_MONTH = 657;

    // event 7 ngày gia nhâp server
    public static final int EVENT_7_STATUS = 659;
    public static final int EVENT_7_REWARD = 660;
    public static final int EVENT_7_SLIDER_REWARD = 662;
    // event lua
    public static final int EVENT_LUA_LIST = 700;
    public static final int EVENT_LUA_STATUS = 701;
    // party
    public static final int PARTY_INVITE_MEMBER = 751;
    public static final int PARTY_REMOVE_MEMBER = 752;
    public static final int PARTY_LEAVE = 753;
    public static final int PARTY_INFO = 754;
    public static final int PARTY_CHANGE_LEADER = 755;
    public static final int PARTY_NEW_INVITE = 756;
    public static final int PARTY_ACCEPT = 757;
    public static final int PARTY_UPDATE_INFO = 758;
    public static final int PARTY_UPDATE_AUTO= 759;
    // world boss
    public static final int WORLD_BOSS_STATUS = 770;
    public static final int WORLD_BOSS_JOIN = 771;
    public static final int WORLD_BOSS_INVITE = 772;
    public static final int WORLD_BOSS_ATTACK = 773;
    public static final int WORLD_BOSS_LEAVE = 774;
    public static final int WORLD_BOSS_INFO = 775;
    public static final int WORLD_BOSS_NEW_INVITE = 776;
    public static final int WORLD_BOSS_SOLO_ATTACK = 777;
    public static final int WORLD_BOSS_SOLO_INFO = 778;


    public static final int PING_IDLE = 10003;
    //endregion
    public static List<Integer> loginServices = Arrays.asList(LOGIN_GAME, LOGOUT);
    public static List<Integer> notDebug = Arrays.asList(PING_GAME, TUTORIAL_QUEST_UPDATE);
}

