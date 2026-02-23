package game.dragonhero.service.resource;

import game.config.CfgServer;
import game.dragonhero.mapping.main.ResGiftCodeEntity;
import ozudo.base.database.DBResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResGift {
    static Map<String, ResGiftCodeEntity> mGift = new HashMap<>();


    public static ResGiftCodeEntity getGiftCode(String giftId) {
        return mGift.get(giftId);
    }

    public static void init() {
        List<ResGiftCodeEntity> aGift = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_gift_code", new ArrayList<>(), "where time_end > Now()", ResGiftCodeEntity.class);
        mGift.clear();
        aGift.forEach(item -> mGift.put(item.getId(), item));
    }
}
