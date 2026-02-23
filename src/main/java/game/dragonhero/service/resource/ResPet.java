package game.dragonhero.service.resource;

import game.config.CfgServer;
import game.dragonhero.mapping.main.ResPetEntity;
import ozudo.base.database.DBResource;
import ozudo.base.helper.NumberUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResPet {
    static Map<Integer, ResPetEntity> mPet = new HashMap<>();
    static Map<Integer, List<ResPetEntity>> aPetByRank = new HashMap<>();

    public static ResPetEntity getPet(int petId) {
        return mPet.get(petId);
    }

    public static void init() {
        // for item
        List<ResPetEntity> aPet = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_pet", ResPetEntity.class);
             mPet.clear();
        aPet.forEach(pet -> {
            pet.init();
            mPet.put(pet.getId(), pet);
            if (!aPetByRank.containsKey(pet.getRank())) {
                aPetByRank.put(pet.getRank(), new ArrayList<>());
            }
            List<ResPetEntity> lst = aPetByRank.get(pet.getRank());
            lst.add(pet);
            aPetByRank.put(pet.getRank(), lst);
        });

    }

    public static List<Long> getDataEquipByLevel(List<List<Long>> points, int level) {
        if (!points.isEmpty()) {
            return points.get(level);
        }
        return new ArrayList<>();
    }
}
