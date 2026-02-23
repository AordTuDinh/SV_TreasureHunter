package ozudo.base.helper;

import java.util.*;
import java.util.stream.Collectors;

public class NumberUtil {


    /**
     * Kiểm tra trong mảng có phần tử nào lặp hay không
     *
     * @param values
     * @return
     */
    public static boolean hasDuplicate(List<Long> values, long... ignoreValues) {
        long ignore = ignoreValues.length == 0 ? -999999 : ignoreValues[0];
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) == ignore) continue;
            for (int j = i + 1; j < values.size(); j++) {
                if (values.get(i) == values.get(j)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<Integer> genListInt(int size, Integer value) {
        List<Integer> lst = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            lst.add(value);
        }
        return lst;
    }

    public static List<List<Integer>> gen2ListInt(int sizeI, int sizeJ, Integer value) {
        List<List<Integer>> ret = new ArrayList<>();
        for (int i = 0; i < sizeI; i++) {
            ret.add(genListInt(sizeJ, value));
        }
        return ret;
    }

    public static String genListStringInt(int size, Integer value) {
        return StringHelper.toDBString(genListInt(size, value));
    }

    public static List<Long> genListLong(int size, Long value) {
        List<Long> lst = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            lst.add(value);
        }
        return lst;
    }

    public static String genListStringLong(int size, Long value) {
        return StringHelper.toDBString(genListLong(size, value));
    }

    public static String joiningListInt(List<Integer> aInt) {
        return aInt.stream().map(Object::toString).collect(Collectors.joining(","));
    }

    public static String joiningListLong(List<Long> aLong) {
        return aLong.stream().map(Object::toString).collect(Collectors.joining(","));
    }

    public static boolean isNumber(String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (Exception ex) {
        }
        return false;
    }

    public static boolean isIntNumber(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (Exception ex) {
        }
        return false;
    }

    public static boolean isLongNumber(String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (Exception ex) {
        }
        return false;
    }

    public static float randomPercent() {
        return new Random().nextFloat() * 100;
    }

    /**
     * Ngẫu nhiên các số tự nhiên giữa 2 số bất kì
     *
     * @param n1
     * @param n2
     * @return n1<= number < = n2
     */
    public static int getRandom(int n1, int n2) {
        return new Random().nextInt(Math.abs(n1 - n2) + 1) + Math.min(n1, n2);
    }


    public static boolean rand100(int percent) {
        return new Random().nextInt(100) < percent;
    }

    public static int round(float num) {
        return Math.round(num);
    }

    public static float getRandom(float n1, float n2) {
        return Math.min(n1, n2) + new Random().nextFloat() * Math.abs(n1 - n2);
    }


    public static long getRandomLong(long n1, long n2) {
        return new Random().nextInt((int) (Math.abs(n1 - n2) + 1)) + Math.min(n1, n2);
    }

    /**
     * Ngẫu nhiên các số tự nhiên từ 0 tới n
     *
     * @param n
     */
    public static int getRandom(int n) { // return 0-> n-1
        return new Random().nextInt(n);
    }

    public static int getRandomDif(int n, int dif) {
        if (n == 1 && dif == 0) return 0;
        int rand = getRandom(n);
        while (rand == dif) {
            rand = getRandom(n);
        }
        return rand;
    }

    public static int min(int... number) {
        if (number.length == 0) return 0;
        int min = number[0];
        for (int i = 1; i < number.length; i++) {
            min = Math.min(min, number[i]);
        }
        return min;
    }

    // cho ra 1 khoang (-range ,range)
    public static float randomRank(float range) {
        return new Random().nextFloat() * (2 * range) - range;
    }

    public static float randomRange(float range) {
        return getRandom(-range, range);
    }

    public static List<Long> converListIntToLong(List<Integer> value) {
        List<Long> aLong = new ArrayList<>();
        for (int i = 0; i < value.size(); i++) {
            aLong.add((long) value.get(i));
        }

        return aLong;
    }

    public static int max(int... number) {
        if (number.length == 0) return 0;
        int max = number[0];
        for (int i = 1; i < number.length; i++) {
            max = Math.max(max, number[i]);
        }
        return max;
    }

    public static List<Long> randomDisticList(int size, int number) {
        List<Long> ret = new ArrayList<>();
        if (size <= number) {
            for (int i = 0; i < size; i++) {
                ret.add((long) i);
            }
        } else {
            while (ret.size() < number) {
                long num = new Random().nextInt(size);
                if (!ret.contains(num)) {
                    ret.add(num);
                }
            }
        }
        return ret;
    }


    // number : số lượng cần random, size là random từ 0 -> đến number
    public static List<Integer> randomDisticListInt(int size, int number) {
        List<Integer> ret = new ArrayList<>();
        if (size <= number) {
            for (int i = 0; i < size; i++) {
                ret.add(i);
            }
        } else {
            while (ret.size() < number) {
                int num = new Random().nextInt(size);
                if (!ret.contains(num)) {
                    ret.add(num);
                }
            }
        }
        return ret;
    }


    public static String FormatLongToString(long valueCast) {
        String ret = "";
        long n1k = 1000L;
        long n1m = n1k * n1k;
        long n1b = n1m * n1k;
        long n1t = n1b * n1k;
        long n1q = n1t * n1k;
        if (valueCast > n1q) {
            int n = (int) (valueCast / n1q);
            ret += n + "q";
            valueCast = (valueCast % n1q);
        }
        if (valueCast > n1t) {
            int n = (int) (valueCast / n1t);
            ret += n + "t";
            valueCast = valueCast % n1t;
        }
        if (valueCast > n1b) {
            int n = (int) (valueCast / n1b);
            ret += n + "b";
            valueCast = valueCast % n1b;
        }
        if (valueCast > n1m) {
            int n = (int) (valueCast / n1m);
            ret += n + "m";
            valueCast = valueCast % n1m;
        }
        if (valueCast > n1k) {
            int n = (int) (valueCast / n1k);
            ret += n + "k";
            valueCast = valueCast % n1k;
        }
        if (valueCast > 0) {
            ret += valueCast;
        }
        return ret.toUpperCase();
    }

    public static Long castStr2Long(String valueCast) {
        long ret = 0;
        long n1k = 1000L;
        long n1m = n1k * n1k;
        long n1b = n1m * n1k;
        long n1t = n1b * n1k;
        long n1q = n1t * n1k;
        if (NumberUtil.isLongNumber(valueCast)) {
            return Long.parseLong(valueCast);
        }
        String[] kNum = valueCast.toLowerCase().split("k");
        if (kNum.length == 2) {
            ret = Long.parseLong(kNum[1]);
        }
        if (NumberUtil.isLongNumber(kNum[0])) {
            ret += Long.parseLong(kNum[0]) * n1k;
        } else {
            String[] mNum = kNum[0].split("m");
            if (mNum.length == 2) {
                ret += Long.parseLong(mNum[1]) * n1k;
            }
            if (NumberUtil.isLongNumber(mNum[0])) {
                ret += Long.parseLong(mNum[0]) * n1m;
            } else {
                String[] bNum = mNum[0].split("b");
                if (bNum.length == 2) {
                    ret += Long.parseLong(bNum[1]) * n1m;
                }
                if (NumberUtil.isLongNumber(bNum[0])) {
                    ret += Long.parseLong(bNum[0]) * n1b;
                } else {
                    String[] tNum = bNum[0].split("t");
                    if (tNum.length == 2) {
                        ret += Long.parseLong(tNum[1]) * n1b;
                    }
                    if (NumberUtil.isLongNumber(tNum[0])) {
                        ret += Long.parseLong(tNum[0]) * n1t;
                    } else {
                        String[] qNum = tNum[0].split("q");
                        if (qNum.length == 2) {
                            ret += Long.parseLong(qNum[1]) * n1t;
                        }
                        if (NumberUtil.isLongNumber(qNum[0])) {
                            ret += Long.parseLong(qNum[0]) * n1q;
                        }
                    }
                }
            }
        }
        return ret;
    }
}
