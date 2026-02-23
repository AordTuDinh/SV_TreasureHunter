package ozudo.base.helper;

import net.sf.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class JsonUtil {
    public static float[] arrToFloat(String value) {
        JSONArray arr = JSONArray.fromObject(value);
        float[] arrFloat = new float[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            arrFloat[i] = (float) arr.getDouble(i);
        }
        return arrFloat;
    }

    public static int[] arrToInt(String value) {
        JSONArray arr = JSONArray.fromObject(value);
        int[] arrInt = new int[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            arrInt[i] = arr.getInt(i);
        }
        return arrInt;
    }

    public static List<Long> convertToListLong(JSONArray arr) {
        List<Long> aLong = new ArrayList<Long>();
        for (int i = 0; i < arr.size(); i++) {
            aLong.add(arr.getLong(i));
        }
        return aLong;
    }

    public static List<Integer> convertToListInt(JSONArray arr) {
        List<Integer> aInt = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            aInt.add(arr.getInt(i));
        }
        return aInt;
    }

    public static JSONArray clearJSONArray(JSONArray tmp) {
        JSONArray arr = new JSONArray();
        for (int i = 0; i < tmp.size(); i++) {
            if (tmp.getInt(i) > 0) {
                arr.add(tmp.getInt(i));
            }
        }
        return arr;
    }

    public static String arrayToString(JSONArray arr) {
        String tmp = "";
        for (int i = 0; i < arr.size(); i++) {
            int n = arr.getInt(i);
            if (n > 0) {
                tmp += "," + n;
            }
        }
        tmp = tmp.length() > 0 ? tmp.substring(1) : tmp;
        return tmp;
    }
}
