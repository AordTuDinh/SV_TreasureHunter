package game.treasure.service.resource;

import game.config.CfgServer;
import game.treasure.mapping.main.ResWingsEntity;
import ozudo.base.database.DBResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResWings {
    static Map<Integer, ResWingsEntity> mWings = new HashMap<>();

    public static ResWingsEntity get(int wingsId) {
        return mWings.get(wingsId);
    }

    public static void init() {
        List<ResWingsEntity> list = DBResource.getInstance().getList(
                CfgServer.DB_MAIN + "res_wings", ResWingsEntity.class);
        mWings.clear();
        list.forEach(wings -> {
            wings.init();
            mWings.put(wings.getId(), wings);
        });
    }
}
