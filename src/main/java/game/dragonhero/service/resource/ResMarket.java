package game.dragonhero.service.resource;

import game.config.CfgServer;
import game.dragonhero.mapping.main.ResMarketDetailEntity;
import game.dragonhero.mapping.main.ResMarketEntity;
import ozudo.base.database.DBJPA;

import java.util.*;
import java.util.stream.Collectors;

public class ResMarket {
    // market
    static Map<Integer, ResMarketEntity> mMarket = new HashMap<>();
    // market - item
    static Map<Integer, ResMarketDetailEntity> mMarketDetail = new HashMap<>();
    static Map<Integer, List<ResMarketDetailEntity>> mListMarketDetail = new HashMap<>();
    public static List<Integer> shopSaveData = new ArrayList<>();
    public static int maxIdShop;

    public static ResMarketDetailEntity getItem(int detailId) {
        return mMarketDetail.get(detailId);
    }

    public static List<ResMarketDetailEntity> getListShopItem(int marketId) {
        List<ResMarketDetailEntity> aDetail = mListMarketDetail.get(marketId);
        return aDetail.stream().filter(ResMarketDetailEntity::isShow).collect(Collectors.toList());
    }

    public static List<ResMarketDetailEntity> getListShopRandomItem(int marketId) {
        List<ResMarketDetailEntity> results = new ArrayList<>();
        ResMarketEntity market = mMarket.get(marketId);
        List<ResMarketDetailEntity> aMarketDetail = mListMarketDetail.get(marketId);
        Random rand = new Random();
        for (int i = 0; i < market.getShowNumber(); i++) {
            float randFloat = rand.nextFloat() * 100;
            for (int index = 0; index < aMarketDetail.size(); index++) {
                if (randFloat < aMarketDetail.get(index).getPercent()) {
                    results.add(aMarketDetail.get(index));
                    break;
                }
            }
        }
        return results;
    }

    public static ResMarketEntity getMarket(int id) {
        return mMarket.get(id);
    }

    public static void init() {
        mMarket.clear();
        mListMarketDetail.clear();
        mMarketDetail.clear();
        // market
        List<ResMarketEntity> aMarket = DBJPA.getList(CfgServer.DB_MAIN + "res_market", ResMarketEntity.class);
        aMarket.forEach(resMarketEntity -> {
            mMarket.put(resMarketEntity.getId(), resMarketEntity);
            mListMarketDetail.put(resMarketEntity.getId(), new ArrayList<>());
            if (resMarketEntity.getType() != 1) shopSaveData.add(resMarketEntity.getId());
            if (maxIdShop < resMarketEntity.getId()) maxIdShop = resMarketEntity.getId();
        });
        // market -item
        List<ResMarketDetailEntity> aMarketDetail = DBJPA.getList(CfgServer.DB_MAIN + "res_market_detail", ResMarketDetailEntity.class);
        aMarketDetail.forEach(item -> {
            item.init();
            mListMarketDetail.get(item.getMarketId()).add(item);
            mMarketDetail.put(item.getId(), item);
        });

        mListMarketDetail.forEach((marketId, aDetail) -> {
            mListMarketDetail.get(marketId).sort(Comparator.comparing(ResMarketDetailEntity::getItemOrder));
            for (int i = 1; i < aDetail.size(); i++) {
                aDetail.get(i).setPercent(aDetail.get(i).getPercent() + aDetail.get(i - 1).getPercent());
            }
        });
    }
}
