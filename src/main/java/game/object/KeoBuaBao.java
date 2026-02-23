package game.object;


import ozudo.base.helper.DateTime;
import ozudo.base.helper.NumberUtil;

public class KeoBuaBao {
    public int userId;
    public int result = 0;
    public long timeEnd;
    public static final int WIN = 1;
    public static final int LOST = 2;
    public static final int EQUAL = 0;

    public KeoBuaBao(int userId) {
        this.userId = userId;
        this.timeEnd = System.currentTimeMillis() * 10 * DateTime.SECOND2_MILLI_SECOND; // 10s
    }

    public void reset() {
        this.timeEnd = System.currentTimeMillis() * 10 * DateTime.SECOND2_MILLI_SECOND; // 10s
    }

    public int cal(KeoBuaBao target) {
        target.result = target.result == 0 ? NumberUtil.getRandom(1, 3) : target.result;
        this.result = this.result == 0 ? NumberUtil.getRandom(1, 3) : this.result;
        if (result == target.result) return EQUAL;
        else {
            if (Math.abs(result - target.result) == 1) {
                return result < target.result ? LOST : WIN;
            } else {
                return result == 1 ? WIN : LOST;
            }
        }
    }

}
