package game.treasure;

public class BattleConfig {
    // new config
    public static int CHUNK_SIZE = 10;


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
    public static final int maxNumberOpenItem = 100;
    public static float P_timeNoMove = 0.1f; // sau khoảng time này thì từ move -> k move - dùng để check nhân vật có đang move hay k
    // ---------------------------------------------------------------------------------------------------------------------------------------------------------
    // server config - không gửi cho client config
    public static final float E_ReviveReady = 0.5f; //hồi sinh xong cho bât tử 1 lúc
    public static final float E_RangeYAttack = 0.4f; //  range y
    public static int M_timeRevive = 3; // seconds - thời gian enemy tự hồi sinh
    public static final float M_delayMove = 2f; //khoang thoi gian cach nhau giua 2 lan move random (move idle)
    public static float M_rangeMove = 1f; // range move random, move trong khoang nay
    public static final float M_speedMoveIdle = 20f; // move speed lúc idle
    public static float M_PerDameCollider = 0.5f; // per dame hit collider

    public static float E_timeDelayAttackToMove = 0.6f; //Attack xong sau khoảng time này mới cho enemy move

    public static float C_haSReciveDamage = 1f; // sau khoảng time này thì mới nhận dame từ thằng đó tiếp (tránh đánh liên tục)
    public static float E_timeCheckDirection = 1f; // sau time này thi check lai direction 1 lan

    public static final float P_distionHitRun = 2f;// move đến cách tường khoảng này thì đổi hướng
    public static final float M_rangePushHit = 1f; // hệ số lực đẩy lùi
    public static final int P_Weight = -1; // hệ số lực đẩy lùi (-1 = k bị đẩy lùi)
    public static final float P_timeRunHit = 0.6f; // time di chuyển trước khi chuyển sang attack (Hit and run)


    public static final float P_delayUseItemSlot = 0.5f; // time delay auto buff (k cho buff liên tục)

}
