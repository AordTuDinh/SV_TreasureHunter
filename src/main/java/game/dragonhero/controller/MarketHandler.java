package game.dragonhero.controller;

import com.google.gson.Gson;
import game.config.CfgFeature;
import game.config.CfgItem;
import game.config.CfgLottery;
import game.config.CfgQuest;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.dragonhero.mapping.UserEventSevenDayEntity;
import game.dragonhero.mapping.UserItemEntity;
import game.dragonhero.mapping.UserMarketEntity;
import game.dragonhero.mapping.main.ResMarketDetailEntity;
import game.dragonhero.mapping.main.ResMarketEntity;
import game.dragonhero.service.Services;
import game.dragonhero.service.resource.ResMarket;
import game.dragonhero.service.user.Actions;
import game.dragonhero.service.user.Bonus;
import game.object.DataQuest;
import game.protocol.CommonProto;
import io.netty.channel.Channel;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.StringHelper;
import ozudo.base.log.Logs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MarketHandler extends AHandler {
    @Override
    public AHandler newInstance() {
        return new MarketHandler();
    }

    static MarketHandler instance;
    UserMarketEntity userMarket;

    public static MarketHandler getInstance() {
        if (instance == null) {
            instance = new MarketHandler();
        }
        return instance;
    }

    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(MARKET_STATUS, MARKET_BUY, MARKET_REFRESH);
        actions.forEach(action -> mHandler.put(action, this));
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        checkTimeMonitor("s");
        try {
            userMarket = Services.userDAO.getUserMarket(mUser);
            if (userMarket == null) {
                addErrResponse();
                return;
            }
            if (!CfgFeature.isOpenFeature(FeatureType.SHOP, mUser, this)) {
                return;
            }
            switch (actionId) {
                case MARKET_STATUS:
                    status((int) CommonProto.parseCommonVector(requestData).getALong(0));
                    break;
                case MARKET_BUY:
                    buy();
                    break;
                case MARKET_REFRESH:
                    refresh();
                    break;
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    private void status(int marketId) {
        ResMarketEntity resMarket = ResMarket.getMarket(marketId);
        if (resMarket == null) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        if (resMarket.getMarketType() == MarketType.TYPE_UNLIMITED) {
            List<ResMarketDetailEntity> aItem = ResMarket.getListShopItem(marketId);
            protocol.Pbmethod.ListCommonVector.Builder builder = protocol.Pbmethod.ListCommonVector.newBuilder();
            builder.addAVector(getCommonVector(marketId, -1));
            protocol.Pbmethod.CommonVector.Builder cmm = protocol.Pbmethod.CommonVector.newBuilder();
            for (int i = 0; i < aItem.size(); i++) {
                cmm.addALong(aItem.get(i).getId());
                cmm.addALong(-1);
            }
            builder.addAVector(cmm);
            addResponse(MARKET_STATUS, builder.build());
        } else if (resMarket.getMarketType() == MarketType.TYPE_REFRESH || resMarket.getMarketType() == MarketType.TYPE_STOCK_REFRESH || resMarket.getMarketType() == MarketType.TYPE_STOCK) {
            if (userMarket.needRefresh(resMarket)) {
                userMarket.refreshShop(resMarket);
            }
            List<ResMarketDetailEntity> aItem = userMarket.getShopItem(resMarket);
            protocol.Pbmethod.ListCommonVector.Builder builder = protocol.Pbmethod.ListCommonVector.newBuilder();
            builder.addAVector(protocol.Pbmethod.CommonVector.newBuilder().addALong(marketId).addALong(userMarket.getCountdown(resMarket)).build());
            protocol.Pbmethod.CommonVector.Builder cmm = protocol.Pbmethod.CommonVector.newBuilder();
            for (int i = 0; i < aItem.size(); i++) {
                if (!aItem.get(i).hasBuy()) {
                    continue;
                }
                cmm.addALong(aItem.get(i).getId());
                if (resMarket.getMarketType() == MarketType.TYPE_REFRESH) { // refresh không giới hạn lượt mua
                    cmm.addALong(-1);
                } else {
                    // chặn hero đang sở hữu
                    if (aItem.get(i).isHasHero(mUser)) {
                        cmm.addALong(0);
                    } else cmm.addALong(aItem.get(i).getStock());
                }
            }
            builder.addAVector(cmm);
            addResponse(MARKET_STATUS, builder.build());
        }
    }

    private void buy() {
        List<Long> aLong = CommonProto.parseCommonVector(requestData).getALongList();
        int marketId = aLong.get(0).intValue();
        int itemId = aLong.get(1).intValue();
        int number = 1;
        if (aLong.size() > 2) {
            number = aLong.get(2).intValue();
            if (number <= 0) number = 1;
            if (number >= 1000) number = 1000;
        }
        List<Long> other = new ArrayList<>();
        if (aLong.size() > 3) {
            other = aLong.subList(3, aLong.size());
        }
        ResMarketEntity resMarket = ResMarket.getMarket(marketId);
        MarketType type = MarketType.get(resMarket.getType());
        ResMarketDetailEntity rMarketDetail = ResMarket.getItem(itemId);
        // check require
        List<Integer> requires = rMarketDetail.getRequires();
        if (requires.get(0) == 1 && user.getLevel() < requires.get(1)) {
            addErrResponse(String.format(getLang(Lang.err_level_buy), requires.get(1)));
            return;
        }
        if (type == MarketType.TYPE_UNLIMITED) {
            buyShopUnlimited(type, rMarketDetail, number);
        } else if (type == MarketType.TYPE_STOCK || type == MarketType.TYPE_STOCK_REFRESH) {
            buyShopStock(resMarket, rMarketDetail, other);
        } else if (type == MarketType.TYPE_REFRESH) {
            buyShopRefresh(resMarket, rMarketDetail, number);
        }
    }

    private void buyShopUnlimited(MarketType market, ResMarketDetailEntity resMarketItem, int number) {
        List<Long> aPrice = new ArrayList<>();
        List<Long> aItem = new ArrayList<>();
        List<Long> price = resMarketItem.getPriceItem();
        List<Long> item = resMarketItem.getItems();
        String errBuy = CfgItem.canBuyItem(mUser, item, number);
        if (errBuy != null) {
            addErrResponse(errBuy);
            return;
        }

        for (int i = 0; i < number; i++) {
            aPrice.addAll(price);
            aItem.addAll(item);
        }
        String err = Bonus.checkMoney(mUser, Bonus.merge(aPrice));
        if (err != null) {
            addErrResponse(err);
            return;
        }

        aPrice.addAll(aItem);
        List<Long> aBonus = Bonus.receiveListItem(mUser, DetailActionType.BUY_SHOP.getKey(market.id), Bonus.merge(aPrice));
        if (aBonus.isEmpty()) {
            addErrResponse();
            return;
        }
        addBonusToastPlus(aBonus);
        mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.BUY_SHOP, number);
        checkQuest(Bonus.getIdItem(item), number);
        // event 7 day attack boss day 2
        UserEventSevenDayEntity uEvent = Services.userDAO.getUserSevenDay(mUser);
        if (uEvent.hasEvent() && uEvent.hasActive(3) && uEvent.update(List.of("buy_shop", uEvent.getBuyShop() + number))) {
            uEvent.setBuyShop(uEvent.getBuyShop() + number);
        }
        Actions.save(user, Actions.GRECEIVE, DetailActionType.BUY_SHOP.getKey(market.id), "bonus", price);
    }

    private void checkQuest(int itemId, int number) {
        if (ItemKey.isItemMedicine(itemId)) CfgQuest.addNumQuest(mUser, DataQuest.BUY_MEDICINE, number);
        if (itemId==ItemKey.NANG_LUONG.id) CfgQuest.addNumQuest(mUser, DataQuest.BUY_ENERGY, number);
        CfgQuest.addNumQuest(mUser, DataQuest.BUY_ITEM_SHOP, number);
        mUser.getDataDaily().update();
    }


    private void buyShopStock(ResMarketEntity market, ResMarketDetailEntity rMarketDetail, List<Long> nums) {
        List<ResMarketDetailEntity> aItem = userMarket.getShopItem(market);
        List<ResMarketDetailEntity> detailEntity = aItem.stream().filter(item -> item.getId() == rMarketDetail.getId()).collect(Collectors.toList());
        if (detailEntity.isEmpty() || detailEntity.get(0).getStock() <= 0)
            addErrResponse(getLang(Lang.err_params));
        else {
            ResMarketDetailEntity resItem = detailEntity.get(0);
            if (!resItem.hasBuy()) {
                addErrResponse(getLang(Lang.err_params));
                return;
            }
            int idItem = Bonus.getIdItem(rMarketDetail.getItems());
            if (idItem == ItemKey.TICKER_NORMAL.id) buyTicketNormal(nums, resItem, market, aItem);
            else if (idItem == ItemKey.TICKER_SPECIAL.id) buyTicketSpecial(nums, resItem, market, aItem);
            else {
                // mua vật phẩm bình thường
                String errBuy = CfgItem.canBuyItem(mUser, resItem.getItems(), 1);
                if (errBuy != null) {
                    addErrResponse(errBuy);
                    return;
                }

                List<Long> price = resItem.getPriceItem();
                String err = Bonus.checkMoney(mUser, price);
                if (err == null) {
                    resItem.setStock(resItem.getStock() - 1);
                    String strValue = new Gson().toJson(aItem);
                    if (userMarket.updateShop(market, strValue)) {
                        price.addAll(resItem.getItems());
                        List<Long> aBonus = Bonus.receiveListItem(mUser, DetailActionType.BUY_SHOP.getKey(market.getId()), price);
                        if (aBonus.isEmpty()) {
                            resItem.setStock(resItem.getStock() + 1);
                            userMarket.updateShop(market, new Gson().toJson(aItem));
                            addErrResponse();
                        } else {
                            addBonusToastPlus(aBonus);
//                            addResponse(getCommonVector(aBonus));
                            mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.BUY_SHOP, 1);
                            status(market.getId());
                            checkQuest(idItem, 1);
                        }
                        // event 7 day attack boss day 2
                        UserEventSevenDayEntity uEvent = Services.userDAO.getUserSevenDay(mUser);
                        if (uEvent.hasEvent() && uEvent.hasActive(3) && uEvent.update(List.of("buy_shop", uEvent.getBuyShop() + 1))) {
                            uEvent.setBuyShop(uEvent.getBuyShop() + 1);
                        }
                        Actions.save(user, Actions.GRECEIVE, DetailActionType.BUY_SHOP.getKey(market.getId()), "bonus", price);
                    } else addErrResponse();
                } else {
                    addErrResponse(err);
                }
            }
        }

    }

    private void buyTicketSpecial(List<Long> nums, ResMarketDetailEntity resItem, ResMarketEntity market, List<ResMarketDetailEntity> aItem) {
        if (!CfgLottery.hasBuySpecial()) {
            addErrResponse(getLang(Lang.err_buy_ticker_special));
            return;
        }
        for (int i = 0; i < nums.size(); i++) {
            if (nums.get(i) < 0) {
                addErrResponse(String.format(getLang(Lang.err_number_greater_than_equal), 0));
                return;
            }
        }
        List<Long> bonus = CfgLottery.getFeeBuySpecial(nums.size());
        String err = Bonus.checkMoney(mUser, bonus);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        List<Long> aBonus = Bonus.receiveListItem(mUser, "buy_lottery_special", bonus);
        if (aBonus == null) {
            addErrResponse();
            return;
        }
        int numAdd = 0;
        long eventDay = CfgLottery.getEventIdBuy();
        UserItemEntity uItem = mUser.getResources().getItem(ItemKey.TICKER_SPECIAL.id);
        if (uItem == null) {
            uItem = new UserItemEntity(user.getId(), ItemKey.TICKER_SPECIAL, nums.size());
            List<Long> numNew = new ArrayList<>(nums);
            numNew.add(0, eventDay);
            uItem.setData(StringHelper.toDBString(numNew));
        } else {
            // check vé cũ cần xóa dữ liệu đi
            List<Long> dataSticker = GsonUtil.strToListLong(uItem.getData());
            if (dataSticker.size() <= 0 || dataSticker.get(0) != eventDay) {
                dataSticker = new ArrayList<>();
                dataSticker.add(eventDay);
                uItem.setNumber(0);
            }
            uItem.add(nums.size());
            numAdd = uItem.getNumber();
            dataSticker.addAll(nums);
            uItem.setData(dataSticker.toString());
        }
        if (DBJPA.saveOrUpdate(uItem)) {
            mUser.getResources().addItem(uItem);
        } else {
            Bonus.receiveListItem(mUser, "buy_lottery_special", Bonus.reverseBonus(CfgLottery.getFeeBuySpecial(nums.size())));
            addErrSystem();
            return;
        }
        resItem.setStock(resItem.getStock() - nums.size());
        aBonus.add((long) Bonus.BONUS_ITEM);
        aBonus.add((long) uItem.getItemId());
        aBonus.add((long) numAdd);
        aBonus.add((long) nums.size());

        if (userMarket.updateShop(market, new Gson().toJson(aItem))) {
            addBonusToastPlus(aBonus);
            status(market.getId());
            mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.BUY_SHOP, numAdd);
            // event 7 day attack boss day 2
            UserEventSevenDayEntity uEvent = Services.userDAO.getUserSevenDay(mUser);
            if (uEvent.hasEvent() && uEvent.hasActive(3) && uEvent.update(List.of("buy_shop", uEvent.getBuyShop() + numAdd))) {
                uEvent.setBuyShop(uEvent.getBuyShop() + numAdd);
            }
        } else addErrResponse();
    }

    private void buyTicketNormal(List<Long> nums, ResMarketDetailEntity resItem, ResMarketEntity market, List<ResMarketDetailEntity> aItem) {
        if (!CfgLottery.hasBuyNormal()) {
            addErrResponse(getLang(Lang.err_buy_ticker_normal));
            return;
        }
        if (nums.isEmpty()) {
            addErrResponse(getLang(Lang.err_number_input));
            return;
        }
        for (int i = 0; i < nums.size(); i++) {
            if (nums.get(i) < 100000) {
                addErrResponse(String.format(getLang(Lang.err_number_greater_than_equal), 100000));
                return;
            }
        }
        UserItemEntity uItem = mUser.getResources().getItem(ItemKey.TICKER_NORMAL.id);
        List<Long> bonus = CfgLottery.getFeeBuyNormal(nums.size());
        String err = Bonus.checkMoney(mUser, bonus);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        List<Long> aBonus = Bonus.receiveListItem(mUser, "buy_lottery_normal", bonus);
        if (aBonus.isEmpty()) {
            addErrResponse();
            return;
        }
        int numAdd = 0;
        long eventDay = CfgLottery.getEventIdBuy();
        if (uItem == null) {
            uItem = new UserItemEntity(user.getId(), ItemKey.TICKER_NORMAL, nums.size());
            List<Long> aNum = new ArrayList(nums);
            aNum.add(0, eventDay);
            uItem.setData(StringHelper.toDBString(aNum));
        } else {
            // check vé cũ cần xóa dữ liệu đi
            String data =uItem.getData();
            List<Long> dataSticker = GsonUtil.strToListLong(data==null?"[]":data);
            if (dataSticker.size() <= 0 || dataSticker.get(0) != eventDay) {
                dataSticker = new ArrayList<>();
                dataSticker.add(eventDay);
                uItem.setNumber(0);
            }
            uItem.add(nums.size());
            numAdd = uItem.getNumber();
            dataSticker.addAll(nums);
            uItem.setData(dataSticker.toString());
        }
        if (DBJPA.saveOrUpdate(uItem)) {
            mUser.getResources().addItem(uItem);
        } else {
            Bonus.receiveListItem(mUser, "buy_lottery_normal", Bonus.reverseBonus(CfgLottery.getFeeBuyNormal(nums.size())));
            addErrSystem();
            return;
        }
        resItem.setStock(resItem.getStock() - nums.size());
        aBonus.add((long) Bonus.BONUS_ITEM);
        aBonus.add((long) uItem.getItemId());
        aBonus.add((long) numAdd);
        aBonus.add((long) nums.size());
        if (userMarket.updateShop(market, new Gson().toJson(aItem))) {
            addBonusToastPlus(aBonus);
            mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.BUY_SHOP, numAdd);
            status(market.getId());
            // event 7 day attack boss day 2
            UserEventSevenDayEntity uEvent = Services.userDAO.getUserSevenDay(mUser);
            if (uEvent.hasEvent() && uEvent.hasActive(3) && uEvent.update(List.of("buy_shop", uEvent.getBuyShop() + numAdd))) {
                uEvent.setBuyShop(uEvent.getBuyShop() + numAdd);
            }
        } else addErrResponse();
    }

    private void buyShopRefresh(ResMarketEntity market, ResMarketDetailEntity rMarketDetail, int number) {
        List<ResMarketDetailEntity> aItem = userMarket.getShopItem(market);
        List<ResMarketDetailEntity> detailEntity = aItem.stream().filter(item -> item.getId() == rMarketDetail.getId()).collect(Collectors.toList());
        if (detailEntity.isEmpty()) addErrResponse(getLang(Lang.err_params));
        else {
            ResMarketDetailEntity item = detailEntity.get(0);
            if (!item.hasBuy()) {
                addErrResponse(getLang(Lang.err_params));
                return;
            }
            String errBuy = CfgItem.canBuyItem(mUser, item.getItems(), number);
            if (errBuy != null) {
                addErrResponse(errBuy);
                return;
            }

            List<Long> aPrice = new ArrayList<>(Bonus.xBonus(item.getPriceItem(), number));
            List<Long> itemBuy = new ArrayList<>(Bonus.xBonus(item.getItems(), number));
            String err = Bonus.checkMoney(mUser, aPrice);
            if (err == null) {
                aPrice.addAll(itemBuy);
                List<Long> aBonus = Bonus.receiveListItem(mUser, DetailActionType.BUY_SHOP.getKey(market.getId()), Bonus.merge(aPrice));
                if (aBonus.isEmpty()) {
                    addErrResponse();
                    return;
                } else {
                    addBonusToastPlus(aBonus);
                    status(market.getId());
                    checkQuest(Bonus.getIdItem(item.getItems()), number);
                    mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.BUY_SHOP, 1);
                }
                // event 7 day attack boss day 2
                UserEventSevenDayEntity uEvent = Services.userDAO.getUserSevenDay(mUser);
                if (uEvent.hasEvent() && uEvent.hasActive(3) && uEvent.update(List.of("buy_shop", uEvent.getBuyShop() + number))) {
                    uEvent.setBuyShop(uEvent.getBuyShop() + number);
                }
                Actions.save(user, Actions.GRECEIVE, DetailActionType.BUY_SHOP.getKey(market.getId()), "bonus", aPrice);

            } else {
                addErrResponse(err);
            }
        }
    }

    private void refresh() {
        List<Long> aLong = CommonProto.parseCommonVector(requestData).getALongList();
        int marketId = aLong.get(0).intValue();
        ResMarketEntity resMarketEntity = ResMarket.getMarket(marketId);
        List<Long> price = resMarketEntity.getListPriceReset();
        String err = Bonus.checkMoney(mUser, price);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        List<Long> aBonus = Bonus.receiveListItem(mUser, DetailActionType.REFRESH_SHOP.getKey(marketId), price);
        if (aBonus.isEmpty()) {
            addErrResponse(getLang(Lang.err_system_down));
            return;
        }

        if (userMarket.refreshShop(resMarketEntity)) {
            addResponse(getCommonVector(aBonus));
            status(marketId);
        } else {
            price.set(price.size() - 1, -price.get(price.size() - 1));
            Bonus.receiveListItem(mUser, DetailActionType.REFRESH_SHOP_FAIL.getKey(marketId), price);
            addErrResponse(getLang(Lang.err_system_down));
        }
    }
}


