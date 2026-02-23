package game.dragonhero.mapping;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.StringHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EventInt {
    public static final int NUMBER_VALUE = 50;
    public static final int TIME_BUY_FIRST_PURCHASE = 0; // number day event
    public static final int EVENT_COMMUNITY_1 = 1; // nhận quà community 1
    public static final int EVENT_COMMUNITY_2 = 2; // nhận quà community 2

    public int userId;
    public List<Integer> aInt;

    public EventInt(String dataInt, int userId) {
        this.userId = userId;
        if (StringHelper.isEmpty(dataInt)) dataInt = "[]";
        aInt = new Gson().fromJson(dataInt, new TypeToken<ArrayList<Integer>>() {
        }.getType());
        while (aInt.size() < NUMBER_VALUE) {
            aInt.add(0);
        }
    }

    public void addValue(int index, int value) {
        aInt.set(index, aInt.get(index) + value);
    }

    public void addOneValue(int index) {
        aInt.set(index, aInt.get(index) + 1);
    }

    public void addOneAndUpdate(int index) {
        addOneValue(index);
        update();
    }

    public int getValue(int index) {
        return aInt.get(index);
    }

    public void setValue(int index, int value) {
        aInt.set(index, value);
    }

    public boolean setValueAndUpdate(int index, int value) {
        aInt.set(index, value);
        return update();
    }

    public String toString() {
        return new Gson().toJson(this);
    }

    public boolean update() {
        return DBJPA.update("user_event", Arrays.asList("event_int", StringHelper.toDBString(aInt)), Arrays.asList("user_id", String.valueOf(userId)));
    }
}
