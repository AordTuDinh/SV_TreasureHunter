package game.dragonhero.mapping;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.StringHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserInt {
    public static final int NUMBER_VALUE = 50;
//    public static final int TIME_BUY_FIRST_PURCHASE = 0; // number day event
    public static final int CREATE_WEAPON_RANK_2 = 1; // chế tạo vũ khí từ mảnh
    public static final int CREATE_WEAPON_RANK_3 = 2; // chế tạo vũ khí từ mảnh
    public static final int CREATE_WEAPON_RANK_4 = 3; // chế tạo vũ khí từ mảnh
    public static final int CREATE_WEAPON_RANK_5 = 4; // chế tạo vũ khí từ mảnh
    public static final int CREATE_WEAPON_RANK_6 = 5; // chế tạo vũ khí từ mảnh
    public static final int MONSTER_COLLECTION_POINT = 6; // điểm collection monster
    public static final int PET_COLLECTION_POINT = 7; // điểm collection pet
    public static final int REFRESH_TAVERN = 8; // điểm collection pet
    public static final int FIRST_SUMMON_STONE_X10 = 9; // lần đầu summon x10 dá
    public static final int FIRST_UPGRADE_ITEM_EQUIP = 10; // lần đầu nâng item
    public static final int FIRST_CREATE_WEAPON_RANK_2 = 11; // lần đầu chế tạo vũ khí
    public static final int FIRST_UPGRADE_WEAPON = 12; // lần đầu chế tạo vũ khí
    public static final int FIRST_FARM_QUEST = 13; // nhiệm vụ farm đầu tiên
    public static final int FIRST_ATTACK_ARENA = 14;

    public int userId;
    public List<Integer> aInt;

    public UserInt(String dataInt, int userId) {
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
        return DBJPA.update("user_data", Arrays.asList("data_int", StringHelper.toDBString(aInt)), Arrays.asList("user_id", String.valueOf(userId)));
    }
}
