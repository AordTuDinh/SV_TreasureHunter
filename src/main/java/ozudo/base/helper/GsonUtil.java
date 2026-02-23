package ozudo.base.helper;

import com.google.common.collect.ImmutableList;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import game.object.BonusConfig;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class GsonUtil {
    public static JsonArray parseFromListLong(List<Long> aLong) {
        JsonArray arr = new JsonArray();
        aLong.forEach(value -> arr.add(new JsonPrimitive(value)));
        return arr;
    }

    public static JsonObject parseJsonObject(String value) {
        return new JsonParser().parse(value).getAsJsonObject();
    }

    public static JsonArray parseJsonArray(String value) {
        return new JsonParser().parse(value).getAsJsonArray();
    }

    public static List<Long> strToListLong(String value) {
        return new Gson().fromJson(value, new TypeToken<List<Long>>() {
        }.getType());
    }

    public static List<BonusConfig> strToListBonusConfig(String value) {
        return new Gson().fromJson(value, new TypeToken<List<BonusConfig>>() {
        }.getType());
    }

    public static List<String> strToListString(String value) {
        return new Gson().fromJson(value, new TypeToken<List<String>>() {
        }.getType());
    }

    public static List<Long> toListLong(List<Integer> value) {
        List<Long> aLong = new ArrayList<>();
        for (int i = 0; i < value.size(); i++) {
            aLong.add((long) value.get(i));
        }

        return aLong;
    }

    public static List<Integer> toListInt(List<Long> value) {
        List<Integer> aLong = new ArrayList<>();
        for (int i = 0; i < value.size(); i++) {
            aLong.add(Math.toIntExact(value.get(i)));
        }

        return aLong;
    }

    /**
     * Convert from [0,0,0] -> List<Integer>
     */
    public static List<Integer> strToListInt(String value) {
        return new Gson().fromJson(value, new TypeToken<List<Integer>>() {
        }.getType());
    }

    public static List<Float> strToListFloat(String value) {
        return new Gson().fromJson(value, new TypeToken<List<Float>>() {
        }.getType());
    }

    public static List<List<Integer>> strTo2ListInt(String value) {
        return new Gson().fromJson(value, new TypeToken<List<List<Integer>>>() {
        }.getType());
    }

    public static List<List<Long>> strTo2ListLong(String value) {
        return new Gson().fromJson(value, new TypeToken<List<List<Long>>>() {
        }.getType());
    }

    public static Type getTypeListInteger() {
        return new TypeToken<List<Integer>>() {
        }.getType();
    }

    public static Type getTypeListString() {
        return new TypeToken<List<String>>() {
        }.getType();
    }
//    public static <T> List<T> getList(T t, String value) {
//        return new Gson().fromJson(value, getItemId(t));
//    }
//
//    public static <T> ArrayList<T> getArrayList(T t, String value) {
//        return new Gson().fromJson(value, getItemId(t));
//    }

    public static <T> Type getType(Class<T> type) {
        return new TypeToken<T>() {
        }.getType();
    }

    public static String toJson(Object obj) {
        return new Gson().toJson(obj);
    }

    /**
     * @param aList
     * @param <T>
     * @return 1 duplicate list2 chứa các duplicate list để list2 vả list cũ đều không đổi
     */
    public static <T> ImmutableList<ImmutableList<T>> toImmutable(List<List<T>> aList) {
        List<ImmutableList<T>> tmp = new ArrayList<>();
        for (List<T> list : aList) {
            tmp.add(ImmutableList.copyOf(list));
        }

        return ImmutableList.copyOf(tmp);
    }
}
