package game.object;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import game.config.aEnum.QuestType;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.StringHelper;

import java.util.ArrayList;
import java.util.List;

public class DataQuest {
    public static final int NUMBER_VALUE = 31;
    public static final int TIME = 0;// - time cache
    //region nv tuần
    //region quest D - Lưu ý id phải trùng với id của quest
    public static final int SEND_FRIEND_GIFT = 1;
    public static final int SUMMON_STONE = 2;
    public static final int SPINE = 3;
    public static final int KILL_MONSTER = 4;
    public static final int KILL_KIM_THAN = 5;
    public static final int KILL_HOA_THAN = 6;
    public static final int KILL_THO_THAN = 7;
    public static final int KILL_THUY_THAN = 8;
    public static final int CHANGE_GOLD = 9;
    public static final int CHECK_IN_CLAN = 10;
    public static final int CREATE_WEAPON = 11;
    public static final int BUY_ITEM_SHOP = 12;
    public static final int UPGRADE_WEAPON = 13;
    public static final int HARVEST_FARM = 14;
    public static final int PLANT_FARM = 15;
    public static final int SMASH_TOWER = 16;
    public static final int BUY_MEDICINE = 17;
    public static final int BUY_ENERGY = 18;
    public static final int KILL_BOSS_MAP = 19;
    public static final int GET_BONUS_AFK = 20;
    public static final int UPGRADE_ARMOR = 21;
    public static final int UPGRADE_HAT = 22;
    public static final int UPGRADE_SHOES = 23;
    public static final int UPGRADE_GLOVES = 24;
    public static final int GET_FRIEND_GIFT = 25;
    public static final int CREATE_PIECE = 26;
    public static final int SUMMON_PIECE = 27;

    public static final int CUR_POINT_D = 30;// - điểm nhiệm vụ
    //endregion

    //endregion
    public List<Integer> aInt;
    public List<Integer> aNotify; // k cần save
    private int userId;
    private QuestType questType;

    public DataQuest(QuestType type, String data, int userId) {
        if (data == null) data = "[]";
        aInt = new Gson().fromJson(data, new TypeToken<ArrayList<Integer>>() {
        }.getType());
        aNotify = NumberUtil.genListInt(NUMBER_VALUE, 0);
        while (aInt.size() < NUMBER_VALUE) {
            aInt.add(0);
        }
        this.userId = userId;
        this.questType = type;
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

//    public boolean update() {
//        if (questType == QuestType.QUEST_D)
//            return DBJPA.update("user_quest", Arrays.asList("day_int", StringHelper.toDBString(aInt)), Arrays.asList("user_id", String.valueOf(userId)));
//        else if (questType == QuestType.QUEST_C)
//            return DBJPA.update("user_quest", Arrays.asList("week_int", StringHelper.toDBString(aInt)), Arrays.asList("user_id", String.valueOf(userId)));
//        else if (questType == QuestType.QUEST_MONTH)
//            return DBJPA.update("user_quest", Arrays.asList("month_int", StringHelper.toDBString(aInt)), Arrays.asList("user_id", String.valueOf(userId)));
//        else return false;
//    }

}
