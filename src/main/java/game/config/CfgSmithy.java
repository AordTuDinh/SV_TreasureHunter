package game.config;

import com.google.gson.Gson;
import game.battle.calculate.IMath;
import game.config.aEnum.EquipSlotType;
import game.config.aEnum.ItemFarmType;
import game.config.aEnum.ItemKey;
import game.config.aEnum.ToastType;
import game.dragonhero.controller.AHandler;
import game.dragonhero.mapping.UserInt;
import game.dragonhero.mapping.UserItemEquipmentEntity;
import game.dragonhero.mapping.UserWeaponEntity;
import game.dragonhero.mapping.main.ResPointEquipmentEntity;
import game.dragonhero.mapping.main.ResPointInfoEntity;
import game.dragonhero.mapping.main.ResWeaponEntity;
import game.dragonhero.service.resource.ResPoint;
import game.dragonhero.service.resource.ResWeapon;
import game.dragonhero.service.user.Bonus;
import game.object.EquipmentPoint;
import ozudo.base.helper.ListUtil;
import ozudo.base.helper.NumberUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CfgSmithy {
    public static final int FIRST_WEAPON_RANK_2 = 6;
    public static DataConfig config;
    public static int maxPerXu = 25;
    public static int maxXuInput = 100;
    public static float perReceiveBoss = 0.6f;// chỉ số nhận từ boss yeu hon nâng cấp.
    static final Map<Integer, Float> perXuCreate = new HashMap<>();
    public static final Map<Integer, Integer> numPhuocLanhCheTao = new HashMap<>(); // số lần cần đạt để nhận phước lành chế tạo vũ khí
    static final Map<Integer, List<Float>> perXuUpgrade = new HashMap<>();
    static List<Integer> treasureRate = new ArrayList<>();
    static final Map<Integer, List<Integer>> perUpgrade = new HashMap<>();
    public static Map<Integer, List<Long>> bonusDecay = new HashMap<>();
    static final Map<Integer, List<Long>> feeCreate = new HashMap<>();
    public static final Map<Integer, List<List<Long>>> feeUpgrade = new HashMap<>();
    public static final Map<Integer, List<List<Long>>> feeUpTreasure = new HashMap<>();
    public static final List<Integer> itemAccessoryUp = List.of(40, 47, 54);

    public static final int ID = 0;
    public static final int POINT_ID = 1;
    public static final int VALUE = 2;

    public static float getPerGen() {  // item từ ải sẽ cùi hơn nâng cấp
        return CfgSmithy.perReceiveBoss;
    }

    public static boolean isUpgrade(UserItemEquipmentEntity uItem, int rank, int curLevel, int xu) {
        int rand = NumberUtil.getRandom(1000);
        List<Integer> perUp = perUpgrade.get(rank);
        int numHasUp = IMath.getRoundUpNumber(100, perUp.get(curLevel));
        if (uItem.getBless() >= numHasUp) return true;
        List<Float> perXu = perXuUpgrade.get(rank);
        return rand - perXu.get(curLevel) * xu * 10 < perUp.get(curLevel) * 10;
    }

    public static List<Long> getFeeCreate(int mateRank) {
        return new ArrayList<>(feeCreate.get(mateRank));
    }

    public static boolean isCreateSuccess(UserInt uInt, int rank, int xu) {
        // đảm bảo
        if (getDataByRank(uInt, rank) >= numPhuocLanhCheTao.get(rank)) return resetDataByRank(uInt, rank);
        // ngẫu nhiên
        int rand = NumberUtil.getRandom(1000);
        boolean check = rand - config.dataMakeWeapon.perXu.get(rank - 1) * xu * 10 < config.dataMakeWeapon.per.get(rank - 1) * 10;
        if (check) {
            resetDataByRank(uInt, rank);
        } else {
            addDataByRank(uInt, rank);
            uInt.update();
        }
        return check;
    }

    public static int getDataByRank(UserInt uInt, int rank) {
        switch (rank) {
            case 2 -> {
                return uInt.getValue(UserInt.CREATE_WEAPON_RANK_2);
            }
            case 3 -> {
                return uInt.getValue(UserInt.CREATE_WEAPON_RANK_3);
            }
            case 4 -> {
                return uInt.getValue(UserInt.CREATE_WEAPON_RANK_4);
            }
            case 5 -> {
                return uInt.getValue(UserInt.CREATE_WEAPON_RANK_5);
            }
            case 6 -> {
                return uInt.getValue(UserInt.CREATE_WEAPON_RANK_6);
            }


        }
        return 0;
    }

    private static boolean resetDataByRank(UserInt uInt, int rank) {
        switch (rank) {
            case 2 -> uInt.setValueAndUpdate(UserInt.CREATE_WEAPON_RANK_2, 0);
            case 3 -> uInt.setValueAndUpdate(UserInt.CREATE_WEAPON_RANK_3, 0);
            case 4 -> uInt.setValueAndUpdate(UserInt.CREATE_WEAPON_RANK_4, 0);
            case 5 -> uInt.setValueAndUpdate(UserInt.CREATE_WEAPON_RANK_5, 0);
            case 6 -> uInt.setValueAndUpdate(UserInt.CREATE_WEAPON_RANK_6, 0);
        }
        return true;
    }

    private static boolean addDataByRank(UserInt uInt, int rank) {
        switch (rank) {
            case 2 -> uInt.addOneValue(UserInt.CREATE_WEAPON_RANK_2);
            case 3 -> uInt.addOneValue(UserInt.CREATE_WEAPON_RANK_3);
            case 4 -> uInt.addOneValue(UserInt.CREATE_WEAPON_RANK_4);
            case 5 -> uInt.addOneValue(UserInt.CREATE_WEAPON_RANK_5);
            case 6 -> uInt.addOneValue(UserInt.CREATE_WEAPON_RANK_6);
        }
        return true;
    }


    public static boolean isUpgradeSuccess(int rank, int xu, int number, int per) {
        int rand = NumberUtil.getRandom(1000);
        return rand - config.dataMakePiece.perXu.get(rank - 1) * xu / number * 10 < per * 10;
    }

    public static boolean isUpLevelSuccess(UserWeaponEntity weapon, int rank, int xu) {
        int perUp = config.dataUp2.per.get(weapon.getLevel());
        int max = IMath.getRoundUpNumber(100, perUp);
        if (weapon.getBless() >= max) return true;
        int rand = NumberUtil.getRandom(1000);
        return rand - config.dataUp2.perXu.get(rank - 1) * xu * 10 < perUp * 10;
    }

    public static List<Long> getFeeUpgrade(EquipSlotType type, int rank, int curLevel) {
        if (type == EquipSlotType.TREASURE) {
            List<List<Long>> fee = feeUpTreasure.get(rank);
            return new ArrayList<>(fee.get(curLevel));
        } else {
            List<List<Long>> fee = feeUpgrade.get(rank);
            return new ArrayList<>(fee.get(curLevel));
        }

    }

    public static List<Long> getStoneUpLevel(UserWeaponEntity weapon) {
        ResWeaponEntity res = weapon.getRes();
        int number = CfgGacha.config.summonStone.get(res.getRank() - 1) * 4 * weapon.getLevel();
        return Bonus.viewItem(ResWeapon.getWeaponMapStone(weapon.getWeaponId()), -number);
    }

    public static List<Long> getFeeCreate(int rank, int num) {
        return Bonus.viewGold(-config.dataMakeWeapon.gold.get(rank - 1) * num);
    }

    public static EquipmentPoint createItem(EquipmentPoint main, ResPointEquipmentEntity resPoint, int xu, int rank) {
        float perXu = perXuCreate.get(rank) * xu > maxPerXu ? maxPerXu : perXuCreate.get(rank) * xu;
        int bonusAdd = resPoint.getBonusAdd(perXu);
        main.addValue += bonusAdd;
//        ResPointInfoEntity point = ResPoint.getPoint(main.point);
//        handler.addToast(ToastType.RED, " +" + (bonusAdd / 100) + point.getUnit() + " " + point.getName());
        return main;
    }

    public static List<Long> randomTreasure() {
        // random 3 dòng, lấy index khác nhau để lấy trong list cfg ra point nào
        List<Integer> indexs = NumberUtil.randomDisticListInt(config.treasureRate.size(), 3);
        List<Long> ret = new ArrayList<>();
        for (int i = 0; i < indexs.size(); i++) {
            List<Long> cfg = config.dataTreasures.get(indexs.get(i));
            ret.add(cfg.get(2));// per id
            ret.add(cfg.get(0)); //point id
            ret.add(cfg.get(1)); // value
        }
        return ret;
    }

    public static void loadConfig(String strJson) {
        config = new Gson().fromJson(strJson, DataConfig.class);
        config.dataCreate.forEach(item -> {
            perXuCreate.put(item.rank, item.perXu);
            List<Long> fee = Bonus.viewGold(-item.gold);
            for (int i = 0; i < item.item.size(); i += 2) {
                fee.addAll(Bonus.viewItem(item.item.get(i), -item.item.get(i + 1)));
            }
            feeCreate.put(item.rank, fee);
        });
        config.dataUpLevel.forEach(item -> {
            if (!feeUpgrade.containsKey(item.rank)) {
                feeUpgrade.put(item.rank, new ArrayList<>());
            }
            if (!feeUpTreasure.containsKey(item.rank)) {
                feeUpTreasure.put(item.rank, new ArrayList<>());
            }
            for (int i = 0; i < item.perUp.size(); i++) {
                List<Long> fee = Bonus.viewGold(-item.gold.get(i));
                for (int j = 0; j < item.stone.get(i).size(); j += 2) {
                    fee.addAll(Bonus.viewItem(item.stone.get(i).get(j), -item.stone.get(i).get(j + 1)));
                }
                feeUpgrade.get(item.rank).add(fee);
                // treasure
                List<Long> feeTreasure = Bonus.viewGold(-item.gold.get(i));
                if (item.treasure != null && !item.treasure.isEmpty()) {
                    for (int j = 0; j < item.treasure.get(i).size(); j += 2) {
                        feeTreasure.addAll(Bonus.viewItem(item.treasure.get(i).get(j), -item.treasure.get(i).get(j + 1)));
                    }
                    feeUpTreasure.get(item.rank).add(feeTreasure);
                }
            }
            perXuUpgrade.put(item.rank, item.perXu);
            perUpgrade.put(item.rank, item.perUp);

        });
        config.dataDecay.forEach(item -> {
            List<Long> fee = Bonus.viewGold(item.gold);
            fee.addAll(Bonus.viewItem(ItemKey.DA_CUONG_HOA.id, item.stone));
            fee.addAll(Bonus.viewItem(ItemKey.DA_CUONG_HOA_VIP.id, item.stoneVip));
            bonusDecay.put(item.rank, fee);
        });
        List<Integer> per = config.dataMakeWeapon.per;
        for (int i = 1; i < per.size(); i++) {
            numPhuocLanhCheTao.put(i + 1, IMath.getRoundUpNumber(100, per.get(i)));
        }
        // treasure rate
        treasureRate = ListUtil.toListRate100(config.treasureRate);
    }


    public class DataConfig {
        public List<DataCreate> dataCreate;
        public List<DataUpLevel> dataUpLevel;
        public List<DataDecay> dataDecay;
        public DataMake dataMakeWeapon;
        public DataMake dataMakePiece;
        public DataUp2 dataUp2;
        public DataCombine dataCombine;
        public List<Integer> treasureRate;
        public List<List<Long>> dataTreasures; // point - base - rate
    }

    public class DataCreate {
        public int rank;
        public int gold;
        public List<Integer> item;//key-value
        public float perXu;
    }

    public class DataUpLevel {
        public int rank;
        public List<Integer> perUp;// đang x100
        public List<Float> perXu;
        public List<Integer> gold;
        List<List<Integer>> stone;
        List<List<Integer>> treasure;
    }

    public class DataDecay {
        public int rank;
        public int stone;
        public int stoneVip;
        public int gold;
    }

    public class DataMake {
        public List<Integer> gold;
        public List<Integer> item;
        public List<Float> perXu;
        public List<Integer> per;
    }

    public class DataUp2 {
        public List<Integer> per; // per theo level size 8
        public List<Integer> perGold; // gold theo level size 8
        public List<Float> perXu; // theo rank size 6
    }

    public class DataCombine {
        public List<Integer> per; // per thấp lên cao
        public List<Integer> gold;
    }
}
