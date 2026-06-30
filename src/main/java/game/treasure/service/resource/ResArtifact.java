package game.treasure.service.resource;

import game.config.CfgServer;
import game.treasure.mapping.main.ResArtifactEntity;
import ozudo.base.database.DBResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ResArtifact {
    static Map<Integer, ResArtifactEntity> mArtifact = new HashMap<>();
    static boolean loaded;

    public static ResArtifactEntity get(int artifactId) {
        if (!loaded)
            init();
        return mArtifact.get(artifactId);
    }

    public static List<ResArtifactEntity> getAll() {
        if (!loaded)
            init();
        return new ArrayList<>(mArtifact.values());
    }

    public static List<ResArtifactEntity> getByRank(int rank) {
        if (!loaded)
            init();
        if (rank < 1)
            return List.of();
        return mArtifact.values().stream()
                .filter(a -> a != null && a.getRank() == rank)
                .collect(Collectors.toList());
    }

    public static ResArtifactEntity pickRandomByRank(int rank) {
        List<ResArtifactEntity> list = getByRank(rank);
        if (list.isEmpty())
            return null;
        return list.get(ozudo.base.helper.NumberUtil.getRandom(list.size()));
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
