package game.dragonhero.service.resource;

import game.config.CfgServer;
import game.dragonhero.mapping.main.CacheUserBuyRubyEntity;
import game.dragonhero.mapping.main.ResIAPEntity;
import ozudo.base.database.DBResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResIAP {
    public static List<ResIAPEntity> aIAP = new ArrayList<>();
    static Map<Integer, ResIAPEntity> mIAP = new HashMap<>();
    static Map<Integer, CacheUserBuyRubyEntity> mCacheBuyIAPOld = new HashMap<>(); // cache bon nap r
    static Map<String, ResIAPEntity> mIAPProductAndroid = new HashMap<>();
    static Map<String, ResIAPEntity> mIAPProductIos = new HashMap<>();
    public static final int IAP_ID_FIRST_PURCHASE = 1;
    public static final List<List<Long>> bonusDayFirstPurchase = List.of(List.of(6L, 70L, 1000l, 6L, 26L, 10L), List.of(6L, 70L, 1000l, 6L, 35L, 10L));

    public static ResIAPEntity getIAP(int iapId) {
        return mIAP.get(iapId);
    }

    public static ResIAPEntity getIAPProduct(int os, String productId) {
        if (os == 0) return mIAPProductAndroid.get(productId);
        return mIAPProductIos.get(productId);
    }

    public static CacheUserBuyRubyEntity getCacheUserBuyRubyEntity(int userId) {
        return mCacheBuyIAPOld.get(userId);
    }

    public static void init() {
        // purchase list
        mIAP.clear();
        mIAPProductAndroid.clear();
        mIAPProductIos.clear();
        aIAP = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_IAP", ResIAPEntity.class);
        aIAP.forEach(item -> {
            item.init();
            mIAP.put(item.getId(), item);
            mIAPProductAndroid.put(item.getProductIdAndroid(), item);
            mIAPProductIos.put(item.getProductIdIos(), item);
        });

        List<CacheUserBuyRubyEntity> aUCahe = DBResource.getInstance().getList(CfgServer.DB_MAIN + "cache_user_buy_ruby", CacheUserBuyRubyEntity.class);
        aUCahe.forEach(aCache -> mCacheBuyIAPOld.put(aCache.getUserId(), aCache));
    }
}
