package ozudo.base.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ListUtil {
    public static <T> String joinElement(List<T> values) {
        return values.stream().map(value -> String.valueOf(value)).collect(Collectors.joining(","));
    }

    public static void moveIntElement(List<Integer> aSource, List<Integer> aTarget, int index) {
        aTarget.add(aSource.get(index));
        aSource.remove(index);
    }

    public static void moveElement(List<Long> aSource, List<Long> aTarget, int index) {
        aTarget.add(aSource.get(index));
        aSource.remove(index);
    }

    public static boolean hasDuplicateValue(List<Object> values) {
        for (int i = 0; i < values.size() - 1; i++) {
            for (int j = i + 1; j < values.size(); j++) {
                if (values.get(i).equals(values.get(j))) return true;
            }
        }
        return false;
    }

    public static List<Integer> converLstLongToInt(List<Long> lst) {
        List<Integer> retList = new ArrayList<>();
        lst.stream().forEach(ln -> retList.add(Math.toIntExact(ln)));
        return retList;
    }


    public static boolean hasDuplicateValueInteger(List<Integer> values) {
        for (int i = 0; i < values.size() - 1; i++) {
            for (int j = i + 1; j < values.size(); j++) {
                if (values.get(i).equals(values.get(j))) return true;
            }
        }
        return false;
    }


    public static List<Long> toMutableList(List<Long> aLong) {
        return aLong.stream().collect(Collectors.toList());
    }

    public static List<Long> arrToListLong(int[] values) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            ids.add((long) values[i]);
        }
        return ids;
    }

    public static List<Long> arrToListLong(long[] values) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            ids.add(values[i]);
        }
        return ids;
    }

    public static List<Integer> arrToListInt(long[] values) {
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            ids.add((int) values[i]);
        }
        return ids;
    }

    public static List<Long> arrIntergerToListLong(Integer[] values) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            ids.add((long) values[i]);
        }
        return ids;
    }

    public static List<Long> arrIntToListLong(int[] values) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            ids.add((long) values[i]);
        }
        return ids;
    }

    public static List<Integer> arrIntergerToList(Integer[] values) {
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            ids.add(values[i]);
        }
        return ids;
    }

    public static String converIdQuery(int idfrom, int idTo) {
        String s = "(";
        for (int i = idfrom; i <= idTo; i++) {
            s += i + ",";
        }
        s = s.substring(0, s.length() - 1);
        return s + ")";
    }

    public static List<Integer> toListRate100(List<Integer> treasureRate) {
        List<Integer> ret = new ArrayList<>();
        ret.add(treasureRate.get(0));
        for (int i = 1; i < treasureRate.size(); i++) {
            ret.add(ret.get(i - 1) + treasureRate.get(i));
        }
        return ret;
    }
}
