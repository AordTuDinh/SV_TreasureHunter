package game.dragonhero;

import game.battle.object.Pos;
import game.config.CfgAfk;
import ozudo.base.helper.NumberUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BattleConfig {
    public static List<Integer> itemUseSlot = Arrays.asList(0, 1, 2);
    // player config
    public static float P_Height = 1.9f; //1 Chiều cao player
    public static float P_Width = 0.9f; //2 Chiều rộng player
    public static final float hSpeed = 0.015f; //3 - he so giam bot toc do move  - thong nhat giua client va server
    public static final float P_timeQuestRevive = 5f; //4 -thời gian cho phép hồi sinh
    public static final float P_timeImmortal = 3f; //5 - seconds - hồi sinh xong cho bât tử 1 lúc
    public static float B_timeDelayAnim = 0.2f; // 6 - chờ cho animation hoạt động rồi mới move bullet
    public static int B_acceleration = 100; //7 = 30
    public static float C_Collider = 0.3f; //8  C : character collider
    public static float C_timeDelayAttackToMove = 0.2f; //9 - attack xong sau khoảng time này mới cho move
    public static final float M_timeBeHit = 0.2f; //10 khoảng time bị đánh k cho di chuyen
    public static final float CL_timeAliveTextHit = 0.4f; //11 time tồn tại text damage
    public static final float CL_timeAliveComboHit = 0.15f; //12 time cho phép add thêm text damage
    public static float P_timeStartAuto = 2f; // sau time này sẽ mới bắt đầu được auto
    public static float P_timeIdleToAuto = 0.2f; // sau time này sẽ chuyển sang chế độ auto mode
    public static float P_RangeAttack = 3.5f;  // tầm đánh
    public static final float P_delayReady = 2f; // join xong time này mới readly
    public static final float P_attackRun2 = 0.2f; // ngoài tầm đánh thì move thêm time này nữa rồi mới đánh tiếp
    public static final float C_SCALE_SPEED = 20f; // x20 cho dễ hình dung
    public static final float P_attackBlockMove = 0.3f; // khi attack thì block move
    public static final float P_TimeDelayActiveItem = 1f; // Gửi active item cách nhau 1s
    public static final float P_TimeDelayMoveDone = 0.35f; // Move xong r mới cho action khac
    public static final float M_timeBeHitClient = M_timeBeHit - 0.05f; // Cái này cho client chặn di chuyển
    public static final float m_LerpSpeedBar = 0.05f; // tốc độ giảm hp, càng tăng thì tốc độ giảm càng nhanh theo hàm lerp
    public static final int timeSendBonusAfk = CfgAfk.config.secondUpdate;
    public static final int maxNumberOpenItem = 100;
    public static float P_timeNoMove = 0.02f; // sau khoảng time này thì từ move -> k move - dùng để check nhân vật có đang move hay k
    // ---------------------------------------------------------------------------------------------------------------------------------------------------------
    // server config - không gửi cho client config
    public static final int CL_FPS = 60; //  client  target fps
    public static final float E_ReviveReady = 0.5f; //hồi sinh xong cho bât tử 1 lúc
    public static final float P_ReviveReady = 2f;
    public static final float E_RangeYAttack = 0.4f; //  range y
    public static final float CHECK = ((float) 1 / BattleConfig.CL_FPS) * 1000 - 5; //  client  target fps
    public static float B_rangeForMonster = 4f; //7 - tầm bay xa của bullet - dành cho enemy
    public static float M_timeDelayMoveToAttack = 0.3f; // move xong mới cho attack
    public static int M_timeRevive = 3; // seconds - thời gian enemy tự hồi sinh
    public static final float M_delayMove = 2f; //khoang thoi gian cach nhau giua 2 lan move random (move idle)
    public static float M_rangeMove = 1f; // range move random, move trong khoang nay
    public static final float M_speedMoveIdle = 20f; // move speed lúc idle
    public static float P_offsetYColTop = 0.2f; // pos y shuriken top
    public static float M_PerDameCollider = 0.5f; // per dame hit collider
    public static float P_offsetYColDown = -0.15f; //pos y shuriken down
    public static float P_offsetXRow2 = 0.3f; //pos x cộng thêm cho shuriken hàng 2
    public static float M_rangeRandomMoveAttack = 0.3f;// khoảng random điểm move khi attack
    public static float E_timeDelayAttackToMove = 0.6f; //Attack xong sau khoảng time này mới cho enemy move
    public static float M_perRangeAttack = 1.5f; // hệ số dame lúc tấn công từ xa
    public static float M_rangeMoveAttack = 0.6f; // đi quá từng này thì đổi hướng quay lại, dùng cho attack melee
    public static float M_rangCheckDirection = 2f; // đi quá từng này thì check lại direction đến player

    public static float C_haSReciveDamage = 1f; // sau khoảng time này thì mới nhận dame từ thằng đó tiếp (tránh đánh liên tục)
    public static float E_timeCheckDirection = 1f; // sau time này thi check lai direction 1 lan
    public static float E_distance_attack = 0.5f; // move cách nhau 1 khoảng để k bị dính

    public static final int rateDrop = 5;// 10% tỉ lệ rơi đồ khi đánh ải
    public static final float P_distionHitRun = 2f;// move đến cách tường khoảng này thì đổi hướng
    public static final float M_rangePushHit = 1f; // hệ số lực đẩy lùi
    public static final long P_Weight = -1L; // hệ số lực đẩy lùi (-1 = k bị đẩy lùi)
    public static final float P_timeRunHit = 0.6f; // time di chuyển trước khi chuyển sang attack (Hit and run)
    public static final float P_perMoveHit = 1f; // hệ số tốc độ lúc move hit


    public static final float P_delayUseItemSlot = 0.5f; // time delay auto buff (k cho buff liên tục)
    public static final float P_delayBePush = 0.5f; // cùng 1 attacker thì sau khoảng time này target mới nhận push tiếp


    // Skill -------------------------------
    public static final float S_celiDameMaxHp = 10f; // sát thương quy đổi(1000% attack dame) tối đa khi dùng skill sát thương theo máu tối đa
    public static final int S_addForcePush = 5000; // lực cộng thêm cho skill push
    public static final float S_celiDameToxic = 15f; // sát thương quy đổi(1500% magic attack) tối đa khi dùng skill sát thương theo máu tối đa

    public static final float S_timePoinson = 4f; // thời gian gây sát thương độc
    public static final int S_maxReduce90 = 90;// 90% // speed -
    public static final int S_maxReduce80 = 80;// 80% = def - agility - magic resist
    public static final float S_maxReHp50 = 50f;// 50% hồi máu tối đa áp dụng cho skill heal
    public static final float S_maxReHp75 = 75f;// 50% hồi máu tối đa áp dụng cho skill heal
    public static final long S_maxReHPBurned = 3000;// hồi máu tối đa attack dame - skill 23
    public static final float S_timeDecBlizzard = 3f;// thời gian giảm chỉ số - skill 27
    public static final float S_radiusSpin = 1.5f;// // bán kính vòng quay spin - skill 23
    public static final float S_speedSpin = 50f;// // tốc độ spin - skill 23

    // for boss
    public static final float BOSS_DELAY_ATTACK = 2f;// sau time này boss mới bắt đầu hoạt động
    public static final float BOSS_RANGE_VIEW_TARGET = 15f;// bán kính tầm nhìn thấy mục tiêu của boss
    public static final float S_timeDot = 2f; // thời gian gây sát thương đốt boss kagu
    public static final float S_timePoisonOgama = 2f; // thời gian gây sát thương đốt boss ogama
    /*
     * maps demo
     */
    //
    public static int maxEnemyInMap = 30;
    public static int maxSizeInputArena = 14;
    public static int sizeHeroArena = 12; //[heroId - shu1 - shu2 - shu3]
    public static int petIndex = 12;
    public static int monsterIndex = 13;
    public static int maxSlotArena = 5;

    public static final int perDropBoxReward = 10;
    public static final int perDropGoldOni = 100;


    // gen data link : https://docs.google.com/spreadsheets/d/1wTIzY8YwcABzMi_uU_GHFcIeyoiu-85ZEFm6ZHT3VqY/edit#gid=1086795262
    public static final Map<Integer, Pos> mapEnemy = new HashMap<Integer, Pos>() {{
        put(36, new Pos(4, 2));
        put(20, new Pos(13, 2));
        put(35, new Pos(16, 2));
        put(34, new Pos(24, 2));
        put(44, new Pos(27, 2));
        put(21, new Pos(6, 3));
        put(11, new Pos(10, 3));
        put(10, new Pos(19, 3));
        put(6, new Pos(14, 4));
        put(19, new Pos(22, 4));
        put(22, new Pos(4, 5));
        put(12, new Pos(9, 5));
        put(7, new Pos(17, 5));
        put(33, new Pos(25, 5));
        put(45, new Pos(6, 6));
        put(5, new Pos(12, 6));
        put(9, new Pos(20, 6));
        put(1, new Pos(14, 7));
        put(2, new Pos(16, 7));
        put(32, new Pos(23, 7));
        put(43, new Pos(27, 7));
        put(23, new Pos(4, 8));
        put(13, new Pos(9, 8));
        put(4, new Pos(12, 8));
        put(8, new Pos(18, 8));
        put(39, new Pos(7, 9));
        put(3, new Pos(15, 9));
        put(18, new Pos(21, 9));
        put(42, new Pos(27, 9));
        put(31, new Pos(24, 10));
        put(24, new Pos(5, 11));
        put(14, new Pos(10, 11));
        put(17, new Pos(20, 11));
        put(15, new Pos(13, 12));
        put(16, new Pos(17, 12));
        put(30, new Pos(22, 12));
        put(41, new Pos(26, 12));
        put(37, new Pos(2, 13));
        put(38, new Pos(4, 14));
        put(25, new Pos(7, 14));
        put(26, new Pos(10, 14));
        put(27, new Pos(13, 14));
        put(28, new Pos(16, 14));
        put(29, new Pos(19, 14));
        put(40, new Pos(24, 14));
    }};

    public static Pos getPosRandomEnemy() {
        return BattleConfig.mapEnemy.get(NumberUtil.getRandom(1, BattleConfig.mapEnemy.size())).clone();
    }
}
