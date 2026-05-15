package game.treasure.service.resource;

import game.config.CfgServer;
import game.treasure.mapping.main.ResMobEntity;
import ozudo.base.database.DBResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResMob {
    public static Map<Integer, ResMobEntity> mMob = new HashMap<>();
    public static List<ResMobEntity> aMob = new ArrayList<>();

    public static ResMobEntity getMob(int id) {
        return mMob.get(id);
    }

    public static void init() {
        aMob = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_mob", ResMobEntity.class);
        mMob.clear();
        aMob.forEach(mob -> {
            mob.init();
            mMob.put(mob.getId(), mob);
        });
    }
}
