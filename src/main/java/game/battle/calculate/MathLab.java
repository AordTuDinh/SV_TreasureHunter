package game.battle.calculate;

import game.battle.object.Pos;

import java.util.List;

public class MathLab {


    public static boolean pointBetween2Circle(Pos center, float radiusIn, float radiusOut, Pos point) {
        double distance = center.distance(point);
        return distance < radiusOut && distance > radiusIn;
    }

    // 0 - 360
    public static float getAngle(Pos direction) {
        float angle = (float) Math.toDegrees(Math.atan2(direction.y, direction.x));
        return angle < 0 ? 360 + angle : angle;
    }

    public static boolean inSizeElip(Pos point, Pos center, float xRadius, float yRadius) {
        return Math.pow(point.x - center.x, 2) / Math.pow(xRadius, 2) +
                Math.pow(point.y - center.y, 2) / Math.pow(yRadius, 2) <= 1;
    }

    public static float pos2Anger(Pos a, Pos b) {
        Pos direction = a.getDirectionTo(b);
        return (float) Math.toDegrees(Math.atan2(direction.y, direction.x));
    }

    // Goc(degree) -> Pos direction
    public static Pos angle2Direction(float angleDegree, Pos direction) {
        float angle = MathLab.getAngle(direction) + angleDegree;
        return new Pos((float) (Math.cos(Math.toRadians(angle)) + direction.x), (float) (Math.sin(Math.toRadians(angle)) + direction.y)).normalized();
    }

    // tinh dien tich tam giac
    public static float triAngleArea(Pos A, Pos B, Pos C) {
        return Math.round(0.5f * Math.abs(A.x * (B.y - C.y) + B.x * (C.y - A.y) + C.x * (A.y - B.y)) * 100f) / 100f;
    }

    // nam tren canh tam giac
    public static boolean pointInTriangle(Pos curPos, Pos A, Pos B, Pos C) {
        float s1 = triAngleArea(curPos, A, B);
        float s2 = triAngleArea(curPos, A, C);
        float s3 = triAngleArea(curPos, B, C);
        float sum3 = s1 + s2 + s3;
        float sABC = triAngleArea(A, B, C);
        if (sum3 > sABC) {
            return false;
        } else return true;
    }

    public static boolean pointInTriangle(Pos curPos, List<Pos> point) {
        if (point.size() != 3) return false;
        float s1 = triAngleArea(curPos, point.get(0), point.get(1));
        float s2 = triAngleArea(curPos, point.get(0), point.get(2));
        float s3 = triAngleArea(curPos, point.get(1), point.get(2));
        float sum3 = Math.round((s1 + s2 + s3) * 100f) / 100f;
        float sABC = triAngleArea(point.get(0), point.get(1), point.get(2));
        float saiso = 0.01f;
        if (sum3 - saiso > sABC) {
            return false;
        } else return true;
    }


    // nam tren tam giac
    public static boolean checkInLineTriAngle(Pos M, Pos A, Pos B, Pos C) {
        float s1 = triAngleArea(M, A, B);
        float s2 = triAngleArea(M, A, C);
        float s3 = triAngleArea(M, B, C);
        if (s1 == 0 || s2 == 0 || s3 == 0) {
            return true;
        } else return false;
    }

    public static Pos getDirection(Pos from, Pos to) {
        return new Pos(to.x - from.x, to.y - from.y).normalized();
    }



    public static boolean pointInCircle(Pos center, float radius, Pos point) {
        return center.distance(point) < radius;
    }

    // nằm giữa khoảng 2 hình chữ nhật  : 1 -> ngoài -- 2-> trong
    public static boolean checkIn2Square(Pos M, Pos A1, Pos B1, Pos C1, Pos D1, Pos A2, Pos B2, Pos C2, Pos D2) {
        return checkInSquare(M, A1, B1, C1, D1) && !checkInSquare(M, A2, B2, C2, D2);
    }


    // nằm trên canh hoặc trong hình chữ nhật
    public static boolean checkInSquare(Pos M, Pos A, Pos B, Pos C, Pos D) {
        float s1 = triAngleArea(M, A, B);
        float s3 = triAngleArea(M, B, C);
        float s2 = triAngleArea(M, D, C);
        float s4 = triAngleArea(M, A, D);
        float sum = s1 + s2 + s3 + s4;
        float sABCD = (float) (A.distance(B) * B.distance(C));
        return sum <= sABCD;
    }

    // nằm trên cạnh hình chữ nhật
    public static boolean checkInLineSquare(Pos M, Pos A, Pos B, Pos C, Pos D) {
        float s1 = triAngleArea(M, A, B);
        float s3 = triAngleArea(M, B, C);
        float s2 = triAngleArea(M, D, C);
        float s4 = triAngleArea(M, A, D);
        if (s1 == 0 || s2 == 0 || s3 == 0 || s4 == 0) {
            return true;
        }
        return false;
    }

    // tu 2 diem tinh ra vector
    public static Pos toPos2(Pos a, Pos b) {
        return new Pos(b.x - a.x, b.y - a.y);
    }

    // check 1 diem nam trong 1 da giac
    public static boolean checkInPolyGon(Pos M, List<Pos> lstPoint) {
        // can tinh goc lan luot giua cac diem
        float sum = 0;
        for (int i = 0; i < lstPoint.size() - 1; i++) {
            Pos a = toPos2(lstPoint.get(i), M);
            Pos b = toPos2(lstPoint.get(i + 1), M);
            sum += pos2Anger(a, b);
        }

        Pos ax = toPos2(lstPoint.get(lstPoint.size() - 1), M);
        Pos bx = toPos2(lstPoint.get(0), M);
        sum += pos2Anger(ax, bx);
        return Math.abs(sum - 2 * Math.PI) <= 0.01;
    }
}
