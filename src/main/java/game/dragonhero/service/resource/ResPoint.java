package game.dragonhero.service.resource;

import game.config.CfgServer;
import game.dragonhero.mapping.main.ResPointInfoEntity;
import ozudo.base.database.DBResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResPoint {
    static Map<Integer, ResPointInfoEntity> mPoint = new HashMap<>();

    public static ResPointInfoEntity getPoint(int id) {
        return mPoint.get(id);
    }

    public static void init() {
        List<ResPointInfoEntity> aPoint = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_point_info", ResPointInfoEntity.class);
        mPoint.clear();
        aPoint.forEach(item -> mPoint.put(item.getId(), item));
    }
}
