package game.object;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.StringHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataQuest {
    public static final int NUMBER_VALUE = 20;
    public static final int TIME = 0;// - time cache
    //region nv tuần
    //region quest D - Lưu ý id phải trùng với id của quest
    public static final int SEND_FRIEND_GIFT = 1;
    public static final int SPINE = 2;
    public static final int CHECKIN = 3;
    public static final int KILL_MONSTER = 4;
    public static final int KILL_1 = 5;
    public static final int KILL_2 = 6;
    public static final int KILL_3 = 7;
    public static final int KILL_4 = 8;
    public static final int CHANGE_GOLD = 9;
    public static final int HAVE_DIAMOND = 10;
    public static final int HAVE_GOLD = 11;
    public static final int GATHER = 12;
    public static final int KILL_PLAYER = 13;
    public static final int OPEN_BOX = 14;
    public static final int GET_FRIEND_GIFT = 15;


    public static final int CUR_POINT_D = 16;// - điểm nhiệm vụ
    //endregion

    //endregion
    public List<Integer> aInt;
    public List<Integer> aNotify; // k cần save
    private int userId;

    public DataQuest( String data, int userId) {
        if (data == null) data = "[]";
        aInt = new Gson().fromJson(data, new TypeToken<ArrayList<Integer>>() {
        }.getType());
        aNotify = NumberUtil.genListInt(NUMBER_VALUE, 0);
        while (aInt.size() < NUMBER_VALUE) {
            aInt.add(0);
        }
        this.userId = userId;
    }

    public void addValue(int index, int value) {
        int numAdd = aInt.get(index) + value;
        aInt.set(index, numAdd);
    }

    public int getValue(int index) {
        return aInt.get(index);
    }

    public int getTime() {
        return aInt.get(TIME);
    }

    public int setValue(int index, long value) {
        aInt.set(index, (int) value);
        return index;
    }

    public String toString() {
        return StringHelper.toDBString(aInt);
    }

    public boolean update() {
     return    DBJPA.update("user_quest", Arrays.asList("day_int", StringHelper.toDBString(aInt)), Arrays.asList("user_id", String.valueOf(userId)));
    }

}
