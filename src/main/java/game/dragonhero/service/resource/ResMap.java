package game.dragonhero.service.resource;

import game.config.CfgServer;
import game.config.aEnum.RoomType;
import game.dragonhero.mapping.main.*;
import ozudo.base.database.DBResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResMap {
    // big map
    static Map<Integer, ResMapEntity> mMap = new HashMap<>();
    // boss map
    public static int maxMapCampaign;

    public static BaseMap getMap(ResTeleportEntity teleport) {
        if (teleport.getMapId() == 0) {
            return mMap.get(teleport.getMap().value);
        } else {
            switch (teleport.getMap()) {
                default -> {
                    return mMap.get(teleport.getMap().value);
                }
            }
        }
    }

    public static BaseMap getMap(RoomType roomType, int subId) {
        return getMap(roomType.value, subId);
    }

    public static BaseMap getMap(int mapType, int subId) {
        RoomType roomType = RoomType.get(mapType);
        switch (roomType) {
            default -> {
                return mMap.get(mapType);
            }
        }
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
