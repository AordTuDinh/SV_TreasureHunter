package game.dragonhero.service.resource;

import game.config.CfgServer;
import game.dragonhero.mapping.main.ResTowerEntity;
import ozudo.base.database.DBResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResTower {
    public static Map<Integer, ResTowerEntity> mTower = new HashMap<>();
    public static int maxLevelTower;

    public static void init() {
        List<ResTowerEntity> aMap = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_tower", ResTowerEntity.class);
        maxLevelTower = aMap.size();
        mTower.clear();
        aMap.forEach(tower -> {
            tower.init();
            mTower.put(tower.getId(), tower);
        });
    }
}
