package ozudo.base.helper;

import java.util.*;

public class LogicUtil {
    public static boolean isPositive(List<Long> aLong) {
        return !isNegative(aLong) && !isEqualZero(aLong);
    }

    public static boolean isEqualZero(List<Long> aLong) {
        for (Long num : aLong) {
            if (num != 0) return false;
        }
        return true;
    }

    public static boolean isNegative(List<Long> aLong) {
        for (Long num : aLong) {
            if (num > 0) return false;
        }
        return true;
    }

    /**
     * @param values List Objects
     * @param <T>    Object
     * @return Object at last index
     */
    public static <T> T getLast(List<T> values) {
        if (values == null || values.isEmpty()) return null;
        return values.get(values.size() - 1);
    }

    /**
     * @param values List Objects
     * @param <T>    Object
     * @return Object at random index
     */
    public static <T> T getRandom(List<T> values) {
        Random rd = new Random();
        int point = rd.nextInt(values.size());

        return values.get(point);
    }

    /**
     * @param values List Objects
     * @param aRate  rate
     * @param <T>    Object
     * @return Object at random index by rate
     */
    public static <T> T getRandomByRate(List<T> values, List<Integer> aRate) {
        if (values.size() == 1 || aRate .size() == 1) return values.get(0);
        Random rd = new Random();
        int totalRate = 0;
        for (int rate : aRate) {
            totalRate += rate;
        }
        int point = rd.nextInt(totalRate) + 1;
        int sum = aRate.get(0);
        T result = null;

        for (int i = 1; i < aRate.size(); i++) {
            if (point <= sum) {
                result = values.get(i - 1);
                break;
            }
            sum += aRate.get(i);
            if (sum >= totalRate) result = getLast(values);
        }

        return result;
    }

    /**
     * @param values            list giá trị để random trong đó
     * @param aRate             tỉ lệ
     * @param listCurrentNumber list số lượng hiện tại
     * @param listMaxNumber     list số lượng tối đa
     * @param <T>               result
     * @return nếu số lượng hiện tại đã đạt max thì bỏ qua ko random vào case đó nữa
     */
    public static <T> T getRandomByRateToMax(List<T> values, List<Integer> aRate, List<Integer> listCurrentNumber, List<Integer> listMaxNumber) {
        List<T> aRealValue = new ArrayList<>(values);
        List<Integer> aRealRate = new ArrayList<>(aRate);

        int count = 0;
        for (int i = 0; i < listCurrentNumber.size(); i++) {
            if (listCurrentNumber.get(i) < listMaxNumber.get(i)) {
                count++;
                continue;
            }


            if (aRealValue.isEmpty() || aRealRate.isEmpty()) return null;
            aRealValue.remove(count);
            aRealRate.remove(count);
        }

        return getRandomByRate(aRealValue, aRealRate);
    }

    /**
     * @param level List level
     * @param bonus List aBonus
     * @param point point to get bonus
     * @return aBonus
     */
    public static List<Long> getBonusByPointInt(List<Integer> level, List<List<Long>> bonus, int point) {
        if (level == null || bonus == null || level.isEmpty() || bonus.isEmpty()) return null;
        for (int i = level.size() - 1; i >= 0; i--) {
            if (point >= level.get(i)) return bonus.get(i);
        }

        return new ArrayList<>();
    }

    /**
     * @param level List level
     * @param bonus List aBonus
     * @param point point to get bonus
     * @return aBonus
     */
    public static List<Long> getBonusByPointLong(List<Long> level, List<List<Long>> bonus, long point) {
        if (level == null || bonus == null || level.isEmpty() || bonus.isEmpty()) return new ArrayList<>();
        for (int i = level.size() - 1; i >= 0; i--) {
            if (point >= level.get(i)) return bonus.get(i);
        }

        return new ArrayList<>();
    }

    /**
     * @param aLevel           list level [1,2]
     * @param listARate        list các list tỉ lệ tương ứng với level [[20,30,50],[20,30,50]]
     * @param listARandomBonus list các list aBonus tương ứng với tỉ lệ [[[2,200],[2,100],[2,50]],[[2,200],[2,100]]]
     * @return
     */
    public static List<Long> getRandomBonusByPoint(List<Integer> aLevel, List<List<Integer>> listARate, List<List<List<Long>>> listARandomBonus, int point) {
        if (aLevel == null) return new ArrayList<>();
        List<Integer> aRate = null;
        List<List<Long>> aRandomBonus = null;

        for (int i = aLevel.size() - 1; i >= 0; i--) {
            if (point == -1) {
                aRate = listARate.get(aLevel.size() - 1);
                aRandomBonus = listARandomBonus.get(aLevel.size() - 1);
                break;
            }

            if (point >= aLevel.get(i)) {
                aRate = listARate.get(i);
                aRandomBonus = listARandomBonus.get(i);
                break;
            }
        }

        if (aRate == null || aRandomBonus == null) return new ArrayList<>();

        return getRandomByRate(aRandomBonus, aRate);
    }

    /**
     * @param list  list
     * @param value value to set
     * @param <T>   type
     * @return set list
     */
    public static <T> List<T> setLast(List<T> list, T value) {
        if (list == null || value == null || list.isEmpty()) return new ArrayList<>();

        List<T> tmp = new ArrayList<>(list);
        tmp.set(list.size() - 1, value);
        return tmp;
    }

    public static int getMinInt(List<Integer> list) {
        List<Integer> aInt = new ArrayList<>(list);
        Collections.sort(aInt);

        return aInt.get(0);
    }

    public static int getMinInt(Integer... values) {
        List<Integer> aInt = Arrays.asList(values);
        Collections.sort(aInt);

        return aInt.get(0);
    }

    public static int getMaxInt(List<Integer> list) {
        List<Integer> aInt = new ArrayList<>(list);
        Collections.sort(aInt);

        return getLast(aInt);
    }

    public static int getMaxInt(Integer... values) {
        List<Integer> aInt = Arrays.asList(values);
        Collections.sort(aInt);

        return getLast(aInt);
    }

    public static <T> List<T> getOverWrite(List<T> baseList, int startOverWriteIndex, List<T> overWriteList) {
        List<T> resultList = new ArrayList<>();
        for (int i = 0; i < startOverWriteIndex; i++) {
            resultList.add(baseList.get(i));
        }

        for (int i = startOverWriteIndex; i < baseList.size(); i++) {
            resultList.add(overWriteList.get(i));
        }

        return resultList;
    }

    /**
     * @param baseList   list
     * @param biggerList list lớn hơn
     * @return list phần tử không nằm trong base list
     */
    public static <T> List<T> getAllValueNotExist(List<T> baseList, List<T> biggerList) {
        if (baseList == null || biggerList == null) return null;
        if (biggerList.isEmpty()) return new ArrayList<>();
        if (baseList.isEmpty()) return new ArrayList<>(biggerList);
        List<T> resultList = new ArrayList<>();

        for (T valueInBiggerList : biggerList) {
            boolean isExist = false;
            for (T valueInBaseList : baseList) {
                if (valueInBaseList.equals(valueInBiggerList)) isExist = true;
            }

            if (!isExist) resultList.add(valueInBiggerList);
        }

        return resultList;
    }

    public static int getSumInt(List<Integer> list) {
        if (list == null || list.isEmpty()) return 0;
        int result = list.get(0);
        for (int i = 1; i < list.size(); i++) {
            result += list.get(i);
        }

        return result;
    }

    public static long getSumLong(List<Long> list) {
        if (list == null || list.isEmpty()) return 0;
        long result = list.get(0);
        for (int i = 1; i < list.size(); i++) {
            result += list.get(i);
        }

        return result;
    }
}
