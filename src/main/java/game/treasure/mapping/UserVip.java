package game.treasure.mapping;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import game.config.aEnum.VipType;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.StringHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserVip {
    public int userId;
    public List<Integer> aInt;

    public UserVip(String vipData, int userId) {
        this.userId = userId;
        if (StringHelper.isEmpty(vipData)) vipData = "[]";
        aInt = new Gson().fromJson(vipData, new TypeToken<ArrayList<Integer>>() {
        }.getType());
        while (aInt.size() < VipType.COUNT) {
            aInt.add(0);
        }
    }

    public void addValue(int index, int value) {
        if (index < 0 || index >= VipType.COUNT) return;
        aInt.set(index, aInt.get(index) + value);
    }

    public int getValue(int index) {
        if (index < 0 || index >= aInt.size()) return 0;
        return aInt.get(index);
    }

    public List<Integer> getList() {
        return aInt;
    }

    public boolean update() {
        return DBJPA.update("user_settings", Arrays.asList("vip_data", StringHelper.toDBString(aInt)),
                Arrays.asList("user_id", String.valueOf(userId)));
    }
}
