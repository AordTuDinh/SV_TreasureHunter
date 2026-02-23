package game.config;

import com.google.gson.Gson;
import game.cache.CacheStoreBeans;
import game.config.aEnum.*;
import game.dragonhero.mapping.*;
import game.dragonhero.mapping.main.ResQuestEntity;
import game.dragonhero.mapping.main.ResTutorialQuestEntity;
import game.dragonhero.service.Services;
import game.dragonhero.service.resource.ResQuest;
import game.object.DataDaily;
import game.object.DataQuest;
import game.object.MyUser;
import ozudo.base.helper.StringHelper;

import java.util.*;

public class CfgQuest {
    public static DataConfig config;
    public static int numberBonusDay = 5;
    public static int numberBonusWeek = 5;
    public static int numberQuestD = 8;
    public static int numberQuestC = 8;
    public static int numberQuestB = 5;
    public static List<Integer> indexs = Arrays.asList(0, 1, 2, 3, 4);
    public static int packX2 = 2;
    public static final int INDEX_SUMMON_STONE = 0;
    public static final int INDEX_SUMMON_PIECE = 1;
    public static final int INDEX_SPINE = 2;
    public static final int INDEX_SUMMON_PET = 3;
    public static final int INDEX_SHIP = 4;


    public static void loadConfig(String strJson) {
        config = new Gson().fromJson(strJson, DataConfig.class);
    }

    public static List<Long> getQuestBonus(int index) {
        return config.aBonusQuest.get(index);
    }

    public static List<Long> getQuestCBonus(int index) {
        return config.aBonusQuestC.get(index);
    }

    public static boolean isNotifyQuest(MyUser mUser, QuestType type) {
        UserQuestEntity uQuest = mUser.getUQuest();
        DataQuest quest = uQuest.getDataQuest(type);
        if (quest == null) return false;
        List<Integer> quests = uQuest.getQuest(type);
        for (int i = 0; i < quests.size(); i += 2) {
            ResQuestEntity qe = ResQuest.mQuest.get(quests.get(i));
            if (quests.get(i + 1) != StatusType.DONE.value) {
                StatusType status = CfgQuest.getStatus(quest.getValue(qe.getId()), type == QuestType.QUEST_D ? qe.getNumber() : qe.getNumberC());
                if (status == StatusType.RECEIVE) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void addNumQuest(MyUser mUser, int type, int number) { // add value + check notify
        UserQuestEntity uQuest = mUser.getUQuest();
        ResQuestEntity quest = ResQuest.mQuest.get(type);
        if (uQuest.getDataQuest(QuestType.QUEST_D) != null) {
            DataQuest dataQuestD = uQuest.getDataQuest(QuestType.QUEST_D);
            dataQuestD.addValue(type, number);
            if (dataQuestD.aNotify.get(type) == 0 && dataQuestD.getValue(type) >= quest.getNumber()) {
                List<Integer> statusQuest = uQuest.getQuest(QuestType.QUEST_D);
                for (int i = 0; i < statusQuest.size(); i += 2) {
                    if (statusQuest.get(i) == type && statusQuest.get(i + 1) != StatusType.DONE.value) {
                        mUser.addNotify(NotifyType.QUEST_D);
                        dataQuestD.aNotify.set(type, 1);
                    }
                }
            }
        }

        if (uQuest.getDataQuest(QuestType.QUEST_C) != null) {
            DataQuest dataQuestC = uQuest.getDataQuest(QuestType.QUEST_C);
            dataQuestC.addValue(type, number);
            if (dataQuestC.aNotify.get(type) == 0 && dataQuestC.getValue(type) >= quest.getNumberC()) {
                List<Integer> statusQuestC = uQuest.getQuest(QuestType.QUEST_C);
                for (int i = 0; i < statusQuestC.size(); i += 2) {
                    if (statusQuestC.get(i) == type && statusQuestC.get(i + 1) != StatusType.DONE.value) {
                        mUser.addNotify(NotifyType.QUEST_C);
                        dataQuestC.aNotify.set(type, 1);
                    }
                }
            }
        }

        if (uQuest.getDataQuest(QuestType.QUEST_MONTH) != null)
            uQuest.getDataQuest(QuestType.QUEST_MONTH).addValue(type, number);
        //save db theo cache
        Integer cache = CacheStoreBeans.cache1Min.get(mUser.getUser().getId() + "_update_user_quest");
        uQuest.addPoint(number);
        if (cache == null) {
            CacheStoreBeans.cache1Min.add(mUser.getUser().getId() + "_update_user_quest", 1);
            uQuest.update(new ArrayList<>());
        }
        // check notify

    }

    public static void addNumQuestB(MyUser mUser, int index, int number) {
        UserItemEntity uItem = mUser.getResources().getItem(ItemKey.QUEST_B);
        if (uItem == null || uItem.getNumber() == 0 || uItem.expired()) return;
        List<Integer> data = uItem.getDataListInt();
        int indexState = index * 2 + 1;// 0: day, [state - number] nên phải +2
        if (data.get(indexState) == StatusType.PROCESSING.value) {
            data.set(indexState + 1, data.get(indexState + 1) + number);
            uItem.setData(data.toString());
            uItem.update(List.of("data", StringHelper.toDBString(data)));
        }
    }

    public static StatusType getStatus(int cur, int max) {
        return cur >= max ? StatusType.RECEIVE : StatusType.PROCESSING;
    }

    public static int getQuestTutStatus(MyUser mUser, ResTutorialQuestEntity resQuest) {
        if (resQuest == null) return 0;
        UserDataEntity uData = mUser.getUData();
        ResTutorialQuestEntity res = ResQuest.mTutQuest.get(uData.getQuestTutorial());
        if (res == null) return StatusType.PROCESSING.value;
        switch (res.getType()) {
            case BUY_LAND -> {
                List<UserLandEntity> uLand = new ArrayList<>(mUser.getResources().getMLand().values());
                UserLandEntity curLand = uLand.stream().max(Comparator.comparing(UserLandEntity::getId)).orElse(null);
                if (curLand != null && curLand.getId() == CfgFarm.config.maxLand) {
                    mUser.getUData().setQuestTutorialNumber(res.getNum());
                    return StatusType.RECEIVE.value;
                }
            }
            case HAS_LEVEL_TOWER -> {
                UserTowerEntity uTower = Services.userDAO.getUserTower(mUser);
                if (uTower == null) return StatusType.PROCESSING.value;
                mUser.getUData().setQuestTutorialNumber(uTower.getLevel() - 1);
                if (uTower.getLevel() > res.getNum()) {
                    return StatusType.RECEIVE.value;
                }
            }
            case HAS_LEVEL -> {
                mUser.getUData().setQuestTutorialNumber(mUser.getUser().getLevel());
                if (mUser.getUser().getLevel() >= res.getNum()) {
                    return StatusType.RECEIVE.value;
                }
            }
            case GET_BONUS_ONLINE -> {
                List<Integer> data = mUser.getUserDaily().getEvent1hStatus(mUser);
                int num = (int) data.stream().filter(i -> i.intValue() == StatusType.DONE.value).count();
                mUser.getUData().setQuestTutorialNumber(num);
            }
            case BUY_GOLD -> {
                int numBuy = 0;
                DataDaily uDaily = mUser.getUserDaily().getUDaily();
                if (uDaily.getValue(DataDaily.BUY_GOLD_0) != 0) numBuy++;
                if (uDaily.getValue(DataDaily.BUY_GOLD_1) != 0) numBuy++;
                if (uDaily.getValue(DataDaily.BUY_GOLD_2) != 0) numBuy++;
                mUser.getUData().setQuestTutorialNumber(numBuy);
            }
            case GET_SUPPORT -> {
                int num = 0;
                boolean lunch = mUser.getUserDaily().getUDaily().getValue(DataDaily.EAT_LUNCH) == StatusType.DONE.value;
                if (lunch) num++;
                boolean dinner = mUser.getUserDaily().getUDaily().getValue(DataDaily.EAT_DINNER) == StatusType.DONE.value;
                //System.out.println("mUser.getUserDaily().getUDaily().getValue(DataDaily.EAT_DINNER) = " + mUser.getUserDaily().getUDaily().getValue(DataDaily.EAT_DINNER));
                if (dinner) num++;
                mUser.getUData().setQuestTutorialNumber(num);
                //System.out.println("num = " + num);
            }
            case HAS_LAND -> {
                int landSize = mUser.getResources().getMLand().values().size();
                mUser.getUData().setQuestTutorialNumber(landSize);
                if (landSize >= res.getNum()) {
                    return StatusType.RECEIVE.value;
                }
            }
            case JOIN_CLAN -> {
                if (mUser.getUser().getClan() > 0) {
                    mUser.getUData().setQuestTutorialNumber(1);
                    return StatusType.RECEIVE.value;
                }
            }
            case HAS_COMBO_WEAPON -> {
                if (mUser.getComboWeapon().get(res.getIdInfo()) == 1) { // active combo
                    mUser.getUData().setQuestTutorialNumber(1);
                    return StatusType.RECEIVE.value;
                }
            }
            case HAS_WEAPON_ID -> {
                UserWeaponEntity uWea = mUser.getResources().getWeapon(res.getIdInfo());
                if (uWea != null) {
                    mUser.getUData().setQuestTutorialNumber(1);
                    return StatusType.RECEIVE.value;
                }
            }
            case HAS_TREE -> {
                List<Long> tree = mUser.getUData().getDataTree();
                int numTree = (int) tree.stream().filter(item -> item > 0).count();
                if (numTree >= res.getIdInfo()) {
                    mUser.getUData().setQuestTutorialNumber(numTree);
                    return StatusType.RECEIVE.value;
                }
            }
            case HAS_MONSTER -> {
                UserPetEntity userPet = mUser.getResources().getPet(PetType.MONSTER, res.getIdInfo());
                if (userPet != null) {
                    mUser.getUData().setQuestTutorialNumber(1);
                    return StatusType.RECEIVE.value;
                }
            }
            case HAS_ITEM_EQUIP_LEVEL -> {
                int max = 0;
                for (Map.Entry<Long, UserItemEquipmentEntity> itemEq : mUser.getResources().getMItemEquipment().entrySet()) {
                    if (itemEq.getValue().getItemId() == res.getIdInfo() && itemEq.getValue().getLevel() > max) {
                        max = itemEq.getValue().getLevel();
                    }
                }
                mUser.getUData().setQuestTutorialNumber(max);
            }
            case HAS_POINT_D -> {
                DataQuest dataQuest = mUser.getUQuest().getDataQuest(QuestType.QUEST_D);
                mUser.getUData().setQuestTutorialNumber(dataQuest.getValue(DataQuest.CUR_POINT_D));
            }
            case HAS_ITEM_EQUIP_ID -> {
                for (Map.Entry<Long, UserItemEquipmentEntity> itemEq : mUser.getResources().getMItemEquipment().entrySet()) {
                    if (itemEq.getValue().getItemId() == res.getIdInfo()) {
                        mUser.getUData().setQuestTutorialNumber(1);
                        break;
                    }
                }
            }
            case HAS_WEAPON_BY_RANK -> {
                mUser.getUData().setQuestTutorialNumber(mUser.getResources().getNumWeaponByRank(res.getIdInfo()));
            }
            case HAS_PET -> {
                UserPetEntity userPet = mUser.getResources().getPet(PetType.ANIMAL, res.getIdInfo());
                if (userPet != null) {
                    mUser.getUData().setQuestTutorialNumber(1);
                    return StatusType.RECEIVE.value;
                }
            }

        }
        return mUser.getUData().getQuestTutorialNumber() >= resQuest.getNum() ? StatusType.RECEIVE.value : StatusType.PROCESSING.value;
    }

    // dùng cho nhiều chỗ, cẩn thận khi thay đổi
    public static boolean checkIndex(int index) {
        return indexs.contains(index);
    }

    public class DataConfig {
        public List<List<Long>> aBonusQuest;
        public List<Integer> pointState;
        public List<List<Long>> aBonusQuestC;
        public List<Integer> pointStateC;
    }
}
