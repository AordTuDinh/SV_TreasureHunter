package game.treasure.service.resource;

import game.config.CfgServer;
import game.treasure.mapping.main.ResPetEntity;
import ozudo.base.database.DBResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResPet {
    static Map<Integer, ResPetEntity> mPet = new HashMap<>();

    public static ResPetEntity getPet(int petId) {
        return mPet.get(petId);
    }

    public static void init() {
        List<ResPetEntity> aPet = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_pet", ResPetEntity.class);
        mPet.clear();
        aPet.forEach(pet -> mPet.put(pet.getId(), pet));
    }
}
