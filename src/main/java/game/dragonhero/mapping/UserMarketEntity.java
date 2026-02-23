package game.dragonhero.mapping;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import game.config.aEnum.MarketType;
import game.dragonhero.mapping.main.ResMarketDetailEntity;
import game.dragonhero.mapping.main.ResMarketEntity;
import game.dragonhero.service.resource.ResMarket;
import game.monitor.FileData;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.StringHelper;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "user_market")
@Data
@NoArgsConstructor
public class UserMarketEntity {
    @Id
    int userId;
    String countdown;
    @Transient
    private Map<Integer, String> shops = new HashMap<>();
    @Transient
    private List<Long> aCountdown;

    public UserMarketEntity(int userId) {
        this.userId = userId;
        aCountdown = NumberUtil.genListLong(ResMarket.maxIdShop + 1, 0L);
        for (int i = 0; i < ResMarket.shopSaveData.size(); i++) {
            ResMarketEntity rMarket = ResMarket.getMarket(ResMarket.shopSaveData.get(i));
            if (rMarket.getType() == MarketType.TYPE_REFRESH.id || rMarket.getType() == MarketType.TYPE_STOCK_REFRESH.id) {
                // add countdown
                if (rMarket.getTimeRefresh() == DateTime.DAY_SECOND) {
                    aCountdown.set(rMarket.getId(), System.currentTimeMillis() + DateTime.msUntilEndDay());
                } else aCountdown.set(rMarket.getId(), System.currentTimeMillis());
            }
            // gen new data
            String dataShop = new Gson().toJson(ResMarket.getListShopItem(rMarket.getId()));
            updateShop(rMarket, dataShop);
        }
        countdown = StringHelper.toDBString(aCountdown);
    }

    public List<ResMarketDetailEntity> getShopItem(ResMarketEntity resMarket) {
        List<ResMarketDetailEntity> aItem = serializeMarketDetailEntity(shops.get(resMarket.getId()));
        List<ResMarketDetailEntity> itemConfig = ResMarket.getListShopItem(resMarket.getId());
        if (aItem.size() != itemConfig.size()) {
            List<Integer> lst1 = aItem.stream().map(ResMarketDetailEntity::getId).collect(Collectors.toList());
            List<Integer> lst2 = itemConfig.stream().map(ResMarketDetailEntity::getId).collect(Collectors.toList());
            // xóa phần tử k có trong lst mới
            for (int i = 0; i < lst1.size(); i++) {
                // k có trong danh sách mới thì xóa đi
                if (!lst2.contains(lst1.get(i))) {
                    lst1.remove(i);
                    aItem.remove(i);
                }
            }
            for (int i = 0; i < lst2.size(); i++) {
                if (!lst1.contains(lst2.get(i))) {
                    lst1.add(lst2.get(i));
                    aItem.add(itemConfig.get(i));
                }
            }
            String value = new Gson().toJson(aItem);
            updateShop(resMarket, value);
        }
        return aItem;
    }

    private void checkCountdown() {
        if (aCountdown == null || aCountdown.isEmpty()) aCountdown = GsonUtil.strToListLong(countdown);
        while (aCountdown.size() < ResMarket.maxIdShop + 1) aCountdown.add(0L);
    }

    public long getCountdown(ResMarketEntity rMarket) {
        checkCountdown();
        if (rMarket.getTimeRefresh() == 86400l) {
            return DateTime.secondsUntilEndDay();
        } else if (rMarket.getTimeRefresh() > 86400l) {
            return calculateCountdown(aCountdown.get(rMarket.getId()), rMarket.getTimeRefresh());
        }
        return 0;
    }

    private long calculateCountdown(long lastRefresh, long timeWait) {
        long timePass = (System.currentTimeMillis() - lastRefresh) / 1000;
        if (timePass > timeWait) return 0;
        return timeWait - timePass;
    }

    public boolean needRefresh(ResMarketEntity resMarket) {
        checkCountdown();
        // check nó chưa logout
        if (resMarket.getTimeRefresh() == DateTime.DAY_SECOND) {
            return System.currentTimeMillis() > aCountdown.get(resMarket.getId());
        } else {
            long timeSave = aCountdown.get(resMarket.getId()); // thời gian nó lưu
            return DateTime.isAfterTime(timeSave,resMarket.getTimeRefresh());
        }
    }

    public boolean refreshShop(ResMarketEntity resMarket) {
        switch (MarketType.get(resMarket.getType())) {
            case TYPE_REFRESH, TYPE_STOCK -> {
                String value = new Gson().toJson(ResMarket.getListShopItem(resMarket.getId()));
                return updateShop(resMarket, value);
            }
            case TYPE_STOCK_REFRESH -> {
                String value = new Gson().toJson(ResMarket.getListShopItem(resMarket.getId()));
                if (resMarket.getTimeRefresh() == DateTime.DAY_SECOND) {
                    return updateShopRefresh(resMarket, value, System.currentTimeMillis() + DateTime.msUntilEndDay());
                } else return updateShopRefresh(resMarket, value, System.currentTimeMillis());
            }
            default -> {
                return false;
            }
        }
    }


    public void setShopValue(ResMarketEntity res, String value) {
        shops.put(res.getId(), value);
    }

    public boolean updateShop(ResMarketEntity res, String value) {
        if (FileData.writeFile(userId, "market", res.getId() + "", value)) {
            shops.put(res.getId(), value);
            return true;
        }
        return false;
    }


    public boolean update(List<Object> updateData) {
        return DBJPA.update("user_market", updateData, Arrays.asList("user_id", userId));
    }

    public boolean updateShopRefresh(ResMarketEntity rMarket, String value, long refreshTime) {
        long cache = aCountdown.get(rMarket.getId());
        aCountdown.set(rMarket.getId(), refreshTime);
        if (DBJPA.update("user_market", Arrays.asList("countdown", StringHelper.toDBString(aCountdown)), Arrays.asList("user_id", userId)) && FileData.writeFile(userId, "market", rMarket.getId() + "", value)) {
            setShopValue(rMarket, value);
            return true;
        } else {
            aCountdown.set(rMarket.getId(), cache);
            return false;
        }
    }


    private List<ResMarketDetailEntity> serializeMarketDetailEntity(String value) {
        return new Gson().fromJson(value, new TypeToken<List<ResMarketDetailEntity>>() {
        }.getType());
    }


    public void checkData() {
        for (int i = 0; i < ResMarket.shopSaveData.size(); i++) {
            ResMarketEntity rShop = ResMarket.getMarket(ResMarket.shopSaveData.get(i));
            if (!shops.containsKey(rShop.getId()) || shops.get(rShop.getId()).isEmpty()) {
                updateShop(rShop, new Gson().toJson(ResMarket.getListShopItem(rShop.getId())));
            }
        }
    }

    public boolean getFileData() {
        for (int i = 0; i < ResMarket.shopSaveData.size(); i++) {
            shops.put(ResMarket.shopSaveData.get(i), FileData.readFile(userId, "market", ResMarket.shopSaveData.get(i) + ""));
        }
        return true;
    }
    //endregion
}
