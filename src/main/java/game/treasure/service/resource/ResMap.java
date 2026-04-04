package game.treasure.service.resource;

import game.config.CfgServer;
import game.config.aEnum.MapType;
import game.treasure.mapping.main.*;
import ozudo.base.database.DBResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResMap {
    static Map<Integer, ResMapEntity> mMap = new HashMap<>();


    public static ResMapEntity getMap(MapType type) {
        return mMap.getOrDefault(type.value, null);
    }


    public static void init() {
        // map
        List<ResMapEntity> aMap = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_map", ResMapEntity.class);
        mMap.clear();

        aMap.forEach(item -> {
            item.init();
            mMap.put(item.getId(), item);
        });
    }
}
