package game.object;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.StringHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataDaily {
    public static final int NUMBER_VALUE = 20;
    //endregion
    public static final int BUY_GOLD_0 = 0; // Mua vàng free
    public static final int BUY_GOLD_1 = 1; // Mua vàng 20
    public static final int BUY_GOLD_2 = 2; // Mua vàng 50
    public static final int EAT_LUNCH = 3; // Nhận hỗ trợ sáng
    public static final int EAT_DINNER = 4; // Nhận hỗ trợ tối
    //event
    public static final int UU_DAI_NGAY_FREE = 5; // Nhận ưu đãi free mỗi ngày
    public static final int THE_PHUC_LOI_FREE = 6; // Phúc lợi free mỗi ngày
    public static final int GIOI_HAN_FREE = 7; // Giới hạn free mỗi ngày
    public static final int NUMBER_BUY_TICKET_ARENA = 8; // Số lần mua vé đấu trường
    // other
    public static final int ATTACK_BOS_CLAN = 9; // Số lần đánh boss clan
    public static final int SEND_100_NANG_DONG = 10; // Số lần đánh boss clan
    //card
    public static final int GET_CARD_MONTH = 11; // Đã nhận quà thẻ tháng chưa
    public static final int GET_CARD_WEEK = 12; // Đã nhận quà thẻ tuần chưa
    public static final int GET_CARD_VINH_VIEN = 13; // Đã nhận quà thẻ vĩnh viễn chưa
    //
    public static final int BONUS_TOP_HONOR = 14; // Đã nhận quà cống hiến mỗi ngày chưa
    public static final int NUM_HONOR = 15; // Cống hiên bao nhiêu hôm nay rồi



    public List<Integer> aInt;
    private int userId;


    public DataDaily(String dataInt, int userId) {
        if (StringHelper.isEmpty(dataInt)) dataInt = "[]";
        aInt = new Gson().fromJson(dataInt, new TypeToken<ArrayList<Integer>>() {
        }.getType());
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

    public int setValue(int index, long value) {
        aInt.set(index, (int) value);
        return index;
    }

    public boolean setValueAndUpdate(int index, long value) {
        aInt.set(index, (int) value);
        return update();
    }

    public String toString() {
        return new Gson().toJson(this);
    }

    public boolean update() {
        return DBJPA.update("user_daily", Arrays.asList("data_int", StringHelper.toDBString(aInt)), Arrays.asList("user_id", String.valueOf(userId)));
    }
}
