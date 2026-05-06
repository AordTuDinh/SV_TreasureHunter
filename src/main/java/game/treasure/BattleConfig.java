package game.treasure;

public class BattleConfig {
    // new config
    public static int CHUNK_SIZE = 10;
    public static float attackSpeed = 0.5f;
    public static float P_Height = 1.9f; //1 Chiều cao player
    public static float P_Width = 0.9f; //2 Chiều rộng player
    public static final float hSpeed = 0.015f; //3 - he so giam bot toc do move  - thong nhat giua client va server
    public static float P_timeStartAuto = 2f; // sau time này sẽ mới bắt đầu được auto
    public static float P_timeIdleToAuto = 0.2f; // sau time này sẽ chuyển sang chế độ auto mode
    public static final float P_delayReady = 2f; // join xong time này mới readly
    public static final float C_SCALE_SPEED = 20f; // x20 cho dễ hình dung
    public static final float P_TimeDelayMoveDone = 0.35f; // Move xong r mới cho action khac
    public static final float m_LerpSpeedBar = 0.05f; // tốc độ giảm hp, càng tăng thì tốc độ giảm càng nhanh theo hàm lerp
    public static final int m_LimitUseItem = 100;
    public static final float P_timeNoMove = 0.1f;
    public static final float P_RangerAttack = 1f;

    // ---------------------------------------------------------------------------------------------------------------------------------------------------------
    // server config - không gửi cho client config
    public static final float E_ReviveReady = 0.5f; //hồi sinh xong cho bât tử 1 lúc
    public static final float E_RangeYAttack = 0.4f; //  range y
    public static int M_timeRevive = 3; // seconds - thời gian enemy tự hồi sinh
    public static final float M_delayMove = 2f; //khoang thoi gian cach nhau giua 2 lan move random (move idle)
    public static float M_rangeMove = 1f; // range move random, move trong khoang nay
    public static final float M_speedMoveIdle = 20f; // move speed lúc idle
    public static float E_timeDelayAttackToMove = 0.6f; //Attack xong sau khoảng time này mới cho enemy move

    public static float C_haSReciveDamage = 1f; // sau khoảng time này thì mới nhận dame từ thằng đó tiếp (tránh đánh liên tục)
    public static float E_timeCheckDirection = 1f; // sau time này thi check lai direction 1 lan

    public static final int P_Weight = -1; // hệ số lực đẩy lùi (-1 = k bị đẩy lùi)
    public static final float P_timeRunHit = 0.6f; // time di chuyển trước khi chuyển sang attack (Hit and run)

    public static int timeReviveObject = 3;  // sau 3s thì revive object

}
