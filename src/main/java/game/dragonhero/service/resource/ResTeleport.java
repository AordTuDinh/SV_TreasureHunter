package game.dragonhero.service.resource;


import game.config.CfgServer;
import game.dragonhero.mapping.main.ResTeleportEntity;
import ozudo.base.database.DBResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResTeleport {
    static Map<Integer, ResTeleportEntity> mTeleport = new HashMap<>();
    public static int HomeRoomTeleportId = 0;

    public static ResTeleportEntity getTeleport(int id) {
      return  mTeleport.get(id);
    }

    public static ResTeleportEntity getHomeTeleport() {
        return getTeleport(HomeRoomTeleportId);
    }

    public static void init() {
        List<ResTeleportEntity> teleports = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_teleport", ResTeleportEntity.class);
        mTeleport.clear();
        teleports.forEach(teleport -> {
            teleport.init();
            mTeleport.put(teleport.getId(), teleport);
        });
    }
}
