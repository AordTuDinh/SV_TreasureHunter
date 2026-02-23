package game.dragonhero.service.resource;

import game.config.aEnum.EquipSlotType;
import game.config.CfgServer;
import game.config.CfgSmithy;
import game.dragonhero.mapping.main.ResItemEquipmentEntity;
import game.dragonhero.mapping.main.ResPointEquipmentEntity;
import ozudo.base.database.DBResource;
import ozudo.base.helper.NumberUtil;

import java.util.*;

public class ResPointEquipment {
    static Map<Integer, ResPointEquipmentEntity> mPointEquip = new HashMap<>();
    static List<ResPointEquipmentEntity> aPointEquip = new ArrayList<>();
    static Map<Integer, List<ResPointEquipmentEntity>> mainPoint = new HashMap<>(); // type(EquipSlotType 1,6) -> list point
    static Map<Integer, List<ResPointEquipmentEntity>> subPoint = new HashMap<>(); // rank(1-7) -> list point
    public static List<Integer> levelByRank = Arrays.asList(0, 3, 5, 7, 9, 11, 13, 15);

    public static ResPointEquipmentEntity getPointEquip(int id) {
        return mPointEquip.get(id);
    }

    public static ResPointEquipmentEntity genRandomSubPoint(int rank) {
        List<ResPointEquipmentEntity> subs = subPoint.get(rank);
        int per = 0;
        int rand = NumberUtil.getRandom(100);
        for (int j = 0; j < subs.size(); j++) {
            per += subs.get(j).getPer();
            if (rand < per) {
                return subs.get(j);
            }
        }
        return null;
    }


    public static List<Long> genItemEquipData(ResItemEquipmentEntity res) { // gen new
        List<Long> data = new ArrayList<>(); // [id,point,addx100]
        EquipSlotType slotType = res.getType();
        if (slotType == null || slotType == EquipSlotType.NULL) return data;
        if (slotType == EquipSlotType.WEAPON) {
            List<Long> acc = res.getDataAccessory();
            for (int i = 0; i < acc.size(); i += 2) {
                data.add(0L);
                data.add(acc.get(i));
                data.add(acc.get(i + 1));
            }
            return data;
        } else if (slotType == EquipSlotType.TREASURE) {
            return CfgSmithy.randomTreasure();
        } else {  // gen data main
            List<ResPointEquipmentEntity> genMain = mainPoint.get(slotType.value);
            int rand = NumberUtil.getRandom(100);
            int per = 0;
            for (int i = 0; i < genMain.size(); i++) {
                ResPointEquipmentEntity item = genMain.get(i);
                per += item.getPer();
                if (rand <= per) {
                    data.add((long) item.getId());
                    data.add((long) item.getPoint());
                    long randIndex = 0;
                    // point main =  rank số lần nâng cấp
                    for (int j = 0; j < res.getRank(); j++) {
                        randIndex += item.getRand();
                    }
                    data.add(randIndex);
                    break;
                }
            }
            // gen data sub
            float perUpgrade = CfgSmithy.getPerGen();
            for (int i = 1; i < res.getRank(); i++) {
                List<ResPointEquipmentEntity> subs = subPoint.get(i);
                int levelUp = levelByRank.get(i);
                rand = NumberUtil.getRandom(100);
                per = 0;
                for (int j = 0; j < subs.size(); j++) {
                    per += subs.get(j).getPer();
                    if (rand <= per) {
                        ResPointEquipmentEntity item = subs.get(j);
                        data.add((long) item.getId());
                        data.add((long) item.getPoint());
                        long randIndex = 0;
                        for (int k = 0; k < levelUp; k++) {
                            long add = (long) (100 * perUpgrade * NumberUtil.getRandom(item.getPerMin(), item.getPerMax()));
                            randIndex += add;
                        }
                        data.add(randIndex);
                        break;
                    }
                }
            }

        }
        return data;
    }

    public static void init() {
        // for item
        aPointEquip = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_point_equipment", ResPointEquipmentEntity.class);
        mPointEquip.clear();
        aPointEquip.forEach(item -> {
            mPointEquip.put(item.getId(), item);
            if (item.isMain() && item.getType() != EquipSlotType.NULL) {
                if (!mainPoint.containsKey(item.getType().value)) {
                    mainPoint.put(item.getType().value, new ArrayList<>());
                }
                mainPoint.get(item.getType().value).add(item);
            }
            if (!item.isMain()) {
                if (!subPoint.containsKey(item.getRank())) {
                    subPoint.put(item.getRank(), new ArrayList<>());
                }
                subPoint.get(item.getRank()).add(item);
            }
        });
    }
}
