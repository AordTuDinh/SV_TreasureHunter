package game.treasure.service.resource;

import game.config.CfgServer;
import game.treasure.mapping.main.ResMountEntity;
import ozudo.base.database.DBResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResMount {
    static Map<Integer, ResMountEntity> mMount = new HashMap<>();

    public static ResMountEntity get(int mountId) {
        return mMount.get(mountId);
    }

    public static void init() {
        List<ResMountEntity> list = DBResource.getInstance().getList(
                CfgServer.DB_MAIN + "res_mount", ResMountEntity.class);
        mMount.clear();
        list.forEach(mount -> mMount.put(mount.getId(), mount));
    }
}
