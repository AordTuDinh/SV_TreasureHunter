package game.treasure.controller;

import game.config.CfgTrading;
import game.config.aEnum.DetailActionType;
import game.config.lang.Lang;
import game.monitor.Online;
import game.object.MyUser;
import game.protocol.CommonProto;
import game.treasure.mapping.UserEntity;
import game.treasure.mapping.UserMailEntity;
import game.treasure.mapping.UserTradingEntity;
import game.treasure.service.trading.TradingItemService;
import game.treasure.service.trading.TradingMarketCache;
import game.treasure.service.user.Actions;
import game.treasure.service.user.Bonus;
import game.treasure.task.dbcache.MailCreatorCache;
import io.netty.channel.Channel;
import ozudo.base.helper.StringHelper;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TradingHandler extends AHandler {
    static TradingHandler instance;

    public static TradingHandler getInstance() {
        if (instance == null)
            instance = new TradingHandler();
        return instance;
    }

    static {
        TradingMarketCache.init();
    }

    @Override
    public AHandler newInstance() {
        return new TradingHandler();
    }

    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(
                TRADING_MARKET_LIST, TRADING_WALLET_ADD, TRADING_WALLET_REMOVE,
                TRADING_UNLOCK_SLOT, TRADING_POST, TRADING_CANCEL, TRADING_BUY, TRADING_EDIT);
        actions.forEach(action -> mHandler.put(action, this));
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        try {
            switch (actionId) {
                case TRADING_MARKET_LIST -> marketList();
                case TRADING_WALLET_ADD -> walletAdd();
                case TRADING_WALLET_REMOVE -> walletRemove();
                case TRADING_UNLOCK_SLOT -> unlockSlot();
                case TRADING_POST -> postListing();
                case TRADING_CANCEL -> cancelListing();
                case TRADING_BUY -> buyListing();
                case TRADING_EDIT -> editListing();
            }
        } catch (Exception ex) {
            Logs.error(ex);
            addErrResponse();
        }
    }

    void marketList() {
        Pbmethod.CommonVector req = getInputCmv();
        if (req.getALongCount() < 2) {
            addErrParam();
            return;
        }
        int tab = (int) req.getALong(0);
        int page = (int) req.getALong(1);
        int myOnly = req.getALongCount() > 2 ? (int) req.getALong(2) : 0;
        int userFilter = myOnly == 1 ? user.getId() : 0;
        int server = user.getServer();
        int total = TradingMarketCache.count(server, tab, userFilter);
        List<UserTradingEntity> rows = TradingMarketCache.list(server, tab, userFilter, page);
        Pbmethod.ListCommonVector.Builder out = Pbmethod.ListCommonVector.newBuilder();
        out.addAVector(getCommonVector(tab, page, total, CfgTrading.PAGE_SIZE));
        for (UserTradingEntity row : rows) {
            UserEntity seller = Online.getDbUser(row.getUserId());
            String sellerName = seller != null ? seller.getName() : "";
            int waiting = row.isWaiting() ? 1 : 0;
            Pbmethod.CommonVector.Builder cv = Pbmethod.CommonVector.newBuilder();
            long postedAt = row.getDateCreated() != null ? row.getDateCreated().getTime() : 0L;
            cv.addAllALong(List.of(row.getId(), (long) row.getUserId(), (long) row.getPrice(),
                    row.getVerifyUntil(), (long) row.getItemType(), row.getItemId(), (long) waiting, postedAt));
            cv.addAString(sellerName);
            if (row.getItemInfo() != null)
                cv.addAString(row.getItemInfo());
            out.addAVector(cv.build());
        }
        addResponse(TRADING_MARKET_LIST, out.build());
    }

    void walletAdd() {
        Pbmethod.CommonVector req = getInputCmv();
        if (req.getALongCount() < 2) {
            addErrParam();
            return;
        }
        int bonusType = (int) req.getALong(0);
        long rowId = req.getALong(1);
        Object entity = TradingItemService.getOwned(mUser, bonusType, rowId);
        if (entity == null) {
            addErrResponse(getLang(Lang.item_not_own));
            return;
        }
        if (TradingItemService.getIsTrading(entity) == 1 || TradingItemService.getInMarket(entity) == 1) {
            addErrParam();
            return;
        }
        String errKey = CfgTrading.validateWalletAdd(bonusType, entity, mUser);
        if (errKey != null) {
            addErrResponse(getLang(errKey));
            return;
        }
        int tab = CfgTrading.resolveTab(bonusType);
        if (!TradingItemService.hasEmptyTradingSlot(mUser, tab)) {
            addErrResponse(getLang("err_trading_wallet_full"));
            return;
        }
        if (!TradingItemService.setTradingFlags(mUser, bonusType, rowId, 1, 0)) {
            addErrResponse();
            return;
        }
        Bonus.clearItemFromSlot(mUser, bonusType, rowId);
        pushTradingSync(bonusType, rowId, 1, 0);
        addResponseSuccess();
    }

    void walletRemove() {
        Pbmethod.CommonVector req = getInputCmv();
        if (req.getALongCount() < 2) {
            addErrParam();
            return;
        }
        int bonusType = (int) req.getALong(0);
        long rowId = req.getALong(1);
        Object entity = TradingItemService.getOwned(mUser, bonusType, rowId);
        if (entity == null) {
            addErrResponse(getLang(Lang.item_not_own));
            return;
        }
        if (TradingItemService.getIsTrading(entity) != 1 || TradingItemService.getInMarket(entity) == 1) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        if (!canReturnToMainBag(bonusType)) {
            addErrResponse(getLang("err_bag_full"));
            return;
        }
        if (!TradingItemService.setTradingFlags(mUser, bonusType, rowId, 0, 0)) {
            addErrResponse();
            return;
        }
        if (Bonus.usesItemSlotBonusType(bonusType))
            Bonus.prepareNewItemSlot(mUser, bonusType, rowId);
        pushTradingSync(bonusType, rowId, 0, 0);
        addResponseSuccess();
    }

    boolean canReturnToMainBag(int bonusType) {
        if (bonusType == Bonus.BONUS_MATERIAL)
            return mUser.getResources().canAddMaterial(1);
        if (Bonus.usesItemSlotBonusType(bonusType))
            return mUser.getResources().canAddBagItem(1);
        return true;
    }

    void unlockSlot() {
        Pbmethod.CommonVector req = getInputCmv();
        if (req.getALongCount() < 1) {
            addErrParam();
            return;
        }
        int tab = (int) req.getALong(0);
        int unlocked = tab == CfgTrading.TAB_ITEM ? mUser.getUData().getSlotTrading1() : mUser.getUData().getSlotTrading2();
        int cost = CfgTrading.getUnlockCost(tab, unlocked);
        List<Long> fee = Bonus.viewRuby(-cost);
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(getLang(err));
            return;
        }
        Bonus.receiveListItem(mUser, DetailActionType.BUY_SLOT_BAG.getKey("trading"), fee);
        if (!mUser.getUData().saveSlotTrading(tab, unlocked + 1)) {
            addErrResponse();
            return;
        }
        mUser.queueUserDataInfo();
        addResponse(getCommonVector(unlocked + 1, cost));
    }

    void postListing() {
        Pbmethod.CommonVector req = getInputCmv();
        if (req.getALongCount() < 3) {
            addErrParam();
            return;
        }
        int bonusType = (int) req.getALong(0);
        long rowId = req.getALong(1);
        int price = (int) req.getALong(2);
        if (!CfgTrading.isValidPrice(price)) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        Object entity = TradingItemService.getOwned(mUser, bonusType, rowId);
        if (entity == null) {
            addErrResponse(getLang(Lang.item_not_own));
            return;
        }
        if (TradingItemService.getIsTrading(entity) != 1 || TradingItemService.getInMarket(entity) == 1) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        String errKey = CfgTrading.validateListForSale(bonusType, entity, mUser);
        if (errKey != null) {
            addErrResponse(getLang(errKey));
            return;
        }
        int feeAmount = CfgTrading.calcListingFee(price);
        List<Long> fee = Bonus.viewRuby(-feeAmount);
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(getLang(err));
            return;
        }
        Bonus.receiveListItem(mUser, DetailActionType.SELL_ITEM.getKey("trading_post"), fee);
        int tab = CfgTrading.resolveTab(bonusType);
        UserTradingEntity row = new UserTradingEntity();
        row.setUserId(user.getId());
        row.setServer(user.getServer());
        row.setTab(tab);
        row.setItemType(bonusType);
        row.setItemId(rowId);
        row.setItemInfo(TradingItemService.serializeItemInfo(entity));
        row.setPrice(price);
        row.setVerifyUntil(CfgTrading.randomVerifyUntil());
        row = UserTradingEntity.insert(row);
        if (row == null || row.getId() <= 0) {
            addErrResponse();
            return;
        }
        if (!TradingItemService.setTradingFlags(mUser, bonusType, rowId, 1, 1)) {
            row.deleteFromDb();
            addErrResponse();
            return;
        }
        TradingMarketCache.add(row);
        pushTradingSync(bonusType, rowId, 1, 1);
        addResponse(getCommonVector(row.getId(), row.getVerifyUntil(), feeAmount));
    }

    void cancelListing() {
        Pbmethod.CommonVector req = getInputCmv();
        if (req.getALongCount() < 1) {
            addErrParam();
            return;
        }
        long tradingId = req.getALong(0);
        UserTradingEntity row = TradingMarketCache.get(tradingId);
        if (row == null || row.getUserId() != user.getId()) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        int bonusType = row.getItemType();
        long rowId = row.getItemId();
        if (!row.deleteFromDb()) {
            addErrResponse();
            return;
        }
        TradingMarketCache.remove(row);
        TradingItemService.setTradingFlags(mUser, bonusType, rowId, 1, 0);
        pushTradingSync(bonusType, rowId, 1, 0);
        addResponseSuccess();
    }

    /** Đổi giá listing: trừ phí mới, không hoàn phí cũ, reset verifyUntil. */
    void editListing() {
        Pbmethod.CommonVector req = getInputCmv();
        if (req.getALongCount() < 2) {
            addErrParam();
            return;
        }
        long tradingId = req.getALong(0);
        int price = (int) req.getALong(1);
        if (!CfgTrading.isValidPrice(price)) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        UserTradingEntity row = TradingMarketCache.get(tradingId);
        if (row == null || row.getUserId() != user.getId()) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        int feeAmount = CfgTrading.calcListingFee(price);
        List<Long> fee = Bonus.viewRuby(-feeAmount);
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(getLang(err));
            return;
        }
        Bonus.receiveListItem(mUser, DetailActionType.SELL_ITEM.getKey("trading_edit"), fee);
        long verifyUntil = CfgTrading.randomVerifyUntil();
        if (!row.update(List.of("price", price, "verify_until", verifyUntil))) {
            addErrResponse();
            return;
        }
        row.setPrice(price);
        row.setVerifyUntil(verifyUntil);
        addResponse(getCommonVector(row.getId(), (long) price, row.getVerifyUntil(), feeAmount));
    }

    void buyListing() {
        Pbmethod.CommonVector req = getInputCmv();
        if (req.getALongCount() < 1) {
            addErrParam();
            return;
        }
        long tradingId = req.getALong(0);
        UserTradingEntity row = TradingMarketCache.get(tradingId);
        if (row == null) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        if (row.getUserId() == user.getId()) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        if (row.isWaiting()) {
            addErrResponse(getLang("err_trading_waiting"));
            return;
        }
        int tab = row.getTab();
        if (!TradingItemService.hasEmptyTradingSlot(mUser, tab)) {
            addErrResponse(getLang("err_trading_wallet_full"));
            return;
        }
        int price = row.getPrice();
        List<Long> pay = Bonus.viewRuby(-price);
        String err = Bonus.checkMoney(mUser, pay);
        if (err != null) {
            addErrResponse(getLang(err));
            return;
        }
        int bonusType = row.getItemType();
        long rowId = row.getItemId();
        int sellerId = row.getUserId();
        if (!TradingItemService.transferToUser(bonusType, rowId, sellerId, user.getId(), 1, 0)) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        Bonus.receiveListItem(mUser, DetailActionType.BUY_SHOP.getKey("trading_" + tradingId), pay);
        if (!row.deleteFromDb()) {
            addErrResponse();
            return;
        }
        TradingMarketCache.remove(row);
        MyUser sellerUser = Online.getMUser(sellerId);
        if (sellerUser != null)
            TradingItemService.detachFromSellerResources(sellerUser, bonusType, rowId);
        TradingItemService.attachToBuyerResources(mUser, bonusType, rowId, sellerId);
        sendSellerMail(sellerId, row, price);
        pushTradingSync(bonusType, rowId, 1, 0);
        mUser.queueUserDataInfo();
        addResponse(getCommonVector(tradingId, price));
    }

    void sendSellerMail(int sellerId, UserTradingEntity row, int price) {
        String title = String.format("Bạn đã bán thành công item, với giá %d Ruby", price);
        MailCreatorCache.sendMail(UserMailEntity.builder()
                .userId(sellerId)
                .senderId(0)
                .senderName(Lang.getTitle(mUser, Lang.mail_sender_system))
                .title(title)
                .message(title)
                .bonus(StringHelper.toDBString(Bonus.viewRuby(price)))
                .build()
                .initDefault());
    }

    void pushTradingSync(int bonusType, long rowId, int isTrading, int inMarket) {
        addResponse(TRADING_SYNC, getCommonVector(bonusType, rowId, isTrading, inMarket));
    }
}
