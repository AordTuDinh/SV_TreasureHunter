package ozudo.math;

import java.util.Arrays;
import java.util.List;

public class DaThucNoiSuy {
    static List<Float> x = Arrays.asList(1f, 2f, 20f);
    static List<Float> y = Arrays.asList(1080f, 1110f, 1635f);
    static float value = 2;

    public static void main(String[] args) {
        float a, b, c, ax, bx, cx, ax2, bx2, cx2, n1, n2, n3;
        ax = y.get(0) / ((x.get(0) - x.get(1)) * (x.get(0) - x.get(2)));
        bx = y.get(1) / ((x.get(1) - x.get(0)) * (x.get(1) - x.get(2)));
        cx = y.get(2) / ((x.get(2) - x.get(0)) * (x.get(2) - x.get(1)));
        a = ax + bx + cx;
        ax2 = ax * ((-x.get(1) - x.get(2)));
        bx2 = bx * ((-x.get(0) - x.get(2)));
        cx2 = cx * ((-x.get(0) - x.get(1)));
        b = ax2 + bx2 + cx2;
        n1 = ax * ((x.get(1) * x.get(2)));
        n2 = bx * ((x.get(0) * x.get(2)));
        n3 = cx * ((x.get(0) * x.get(1)));
        c = n1 + n2 + n3;
        System.out.println(a + "x2 + " + b + "x + " + c);
        float kq = (float) (a * Math.pow(value, 2) + b * value + c);
        System.out.println("value = " + value + " -- result = " + kq);
        //Ktra();
        //giaiPTBac2(10, 20, 30);
    }

    public static void Ktra() {
        float y = (float) (0.8f * Math.pow(value, 2) - 1.8f * value + 1);
        System.out.println("y = " + y);
    }

    public static void giaiPTBac2(float a, float b, float c) {
        // kiểm tra các hệ số
        if (a == 0) {
            if (b == 0) {
                System.out.println("Phương trình vô nghiệm!");
            } else {
                System.out.println("Phương trình có một nghiệm: "
                        + "x = " + (-c / b));
            }
            return;
        }
        // tính delta
        float delta = b * b - 4 * a * c;
        float x1;
        float x2;
        // tính nghiệm
        if (delta > 0) {
            x1 = (float) ((-b + Math.sqrt(delta)) / (2 * a));
            x2 = (float) ((-b - Math.sqrt(delta)) / (2 * a));
            System.out.println("Phương trình có 2 nghiệm là: "
                    + "x1 = " + x1 + " và x2 = " + x2);
        } else if (delta == 0) {
            x1 = (-b / (2 * a));
            System.out.println("Phương trình có nghiệm kép: "
                    + "x1 = x2 = " + x1);
        } else {
            System.out.println("Phương trình vô nghiệm!");
        }
    }
}
