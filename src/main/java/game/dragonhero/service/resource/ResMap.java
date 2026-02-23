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
    static Map<Integer, ResMapBossEntity> mMapBoss = new HashMap<>();
    // campaign normal
    static Map<Integer, ResCampaignEntity> mCampaign = new HashMap<>();
    public static int maxMapCampaign;

    public static BaseMap getMap(ResTeleportEntity teleport) {
        if (teleport.getMapId() == 0) {
            return mMap.get(teleport.getMap().value);
        } else {
            switch (teleport.getMap()) {
                case CAMPAIGN -> {
                    return mCampaign.get(teleport.getMapId());
                }
                default -> {
                    return mMap.get(teleport.getMap().value);
                }
            }
        }
    }

    public static BaseMap getBossMap(RoomType type) {
        return mMapBoss.get(type.value); // bắt đầu từ 8
    }


    public static BaseMap getMap(RoomType roomType, int subId) {
        return getMap(roomType.value, subId);
    }

    public static BaseMap getMap(int mapType, int subId) {
        RoomType roomType = RoomType.get(mapType);
        switch (roomType) {
            case CAMPAIGN -> {
                return mCampaign.get(subId);
            }
            case CLAN_BOSS -> {
                return mMapBoss.get(mapType);
            }
            default -> {
                return mMap.get(mapType);
            }
        }
    }


    public static ResCampaignEntity getMapCampaign(int mapId) {
        return mCampaign.get(mapId);
    }


    public static void init() {
        // map
        List<ResMapEntity> aMap = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_map", ResMapEntity.class);
        mMap.clear();
        aMap.forEach(item -> {
            item.init();
            mMap.put(item.getId(), item);
        });

        // map boss
        List<ResMapBossEntity> aMapBoss = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_map_boss", ResMapBossEntity.class);
        mMapBoss.clear();
        aMapBoss.forEach(item -> {
            item.init();
            mMapBoss.put(item.getId(), item);
        });
        // campaign normal
        List<ResCampaignEntity> aCamNormal = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_campaign", ResCampaignEntity.class);
        mCampaign.clear();
        aCamNormal.forEach(item -> {
            item.init();
            if (item.getId() > maxMapCampaign) maxMapCampaign = item.getId();
            mCampaign.put(item.getId(), item);
        });
    }
}
