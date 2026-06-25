package game.treasure.service.resource;

import game.config.CfgServer;
import game.treasure.mapping.main.ResItemPointEntity;
import ozudo.base.database.DBResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResItemPoint {
    static Map<Integer, ResItemPointEntity> mPoint = new HashMap<>();

    public static ResItemPointEntity get(int pointId) {
        return mPoint.get(pointId);
    }

    public static void init() {
        List<ResItemPointEntity> rows = DBResource.getInstance()
                .getList(CfgServer.DB_MAIN + "res_item_point", ResItemPointEntity.class);
        mPoint.clear();
        rows.forEach(row -> mPoint.put(row.getPointId(), row));
    }
}
