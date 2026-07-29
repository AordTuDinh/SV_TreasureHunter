package game.treasure.service.resource;

import game.config.CfgServer;
import game.treasure.mapping.main.ResBonusImageEntity;
import ozudo.base.database.DBResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cache cho bảng {@code res_bonus_image}.
 * <p>
 * ResImage = mapping "entity" + parse data để server roll nhanh.
 */
public class ResImage {
    static final Map<Integer, ResBonusImageEntity> mImage = new HashMap<>();

    public static ResBonusImageEntity get(int bonusImageId) {
        return mImage.get(bonusImageId);
    }

    public static void init() {
        List<ResBonusImageEntity> rows = DBResource.getInstance()
                .getList(CfgServer.DB_MAIN + "res_bonus_image", ResBonusImageEntity.class);
        mImage.clear();
        rows.forEach(row -> {
            row.init();
            mImage.put(row.getId(), row);
        });
    }
}

