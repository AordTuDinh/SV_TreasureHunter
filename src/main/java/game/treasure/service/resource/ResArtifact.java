package game.treasure.service.resource;

import game.config.CfgServer;
import game.treasure.mapping.main.ResArtifactEntity;
import ozudo.base.database.DBResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResArtifact {
    static Map<Integer, ResArtifactEntity> mArtifact = new HashMap<>();
    static boolean loaded;

    public static ResArtifactEntity get(int artifactId) {
        if (!loaded)
            init();
        return mArtifact.get(artifactId);
    }

    public static void init() {
        List<ResArtifactEntity> list = DBResource.getInstance().getList(
                CfgServer.DB_MAIN + "res_artifact", ResArtifactEntity.class);
        mArtifact.clear();
        list.forEach(row -> {
            row.init();
            mArtifact.put(row.getId(), row);
        });
        loaded = true;
    }
}
