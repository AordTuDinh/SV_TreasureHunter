package ozudo.math;

public class DuongCongBezier {

    public static void main(String[] args) {
//        float f = NoiSuyTuyenTinh(0, 10, 0.4f);
//        System.out.println("f = " + f);

        for (int i = 0; i < 100; i++) {
            float p = BezierBac2(0, 6, 10, i);
            System.out.println("p = " + p / 100);
        }
    }

    public static float NoiSuyTuyenTinh(float P0, float P1, float P) {
        P = P > P1 ? P1 : P;
        P = P < P0 ? P0 : P;
        return P0 + P * (P1 - P0);
    }

    public static float BezierTuyenTinh(float P0, float P1, float t) {
        float P = P0 + t * (P1 - P0);
        return P;
    }

    /**
     * lí thuyết : P = (1-t).Bp0,p1 + t.Bp1,p2
     * cthuc rút gọn : P = (1-t)^2.P0 +2.(1-t).t.P1 + t^2.P2
     *
     * @param P0 : số bắt đầu
     * @param P1 : đỉnh ở giữa 2 số P1 và P2
     * @param P2 : số kết thúc
     * @param t  : thời gian
     * @return
     */
    public static float BezierBac2(float P0, float P1, float P2, float t) {
        return (float) (Math.pow((1 - t), 2) * P0 + 2 * (1 - t) * t * P1 + Math.pow(t, 2) * P2);
    }


}
