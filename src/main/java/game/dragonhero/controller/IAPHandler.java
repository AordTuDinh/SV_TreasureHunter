package game.dragonhero.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.model.ProductPurchase;
import com.google.api.services.androidpublisher.model.ProductPurchasesAcknowledgeRequest;
import com.google.gson.JsonObject;
import game.config.aEnum.DetailActionType;
import game.config.aEnum.StatusType;
import game.config.aEnum.YesNo;
import game.config.lang.Lang;
import game.dragonhero.mapping.*;
import game.dragonhero.mapping.main.ResIAPEntity;
import game.dragonhero.server.AppConfig;
import game.dragonhero.service.resource.ResIAP;
import game.dragonhero.service.user.Bonus;
import io.netty.channel.Channel;
import org.json.JSONArray;
import org.json.JSONObject;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.StringHelper;
import ozudo.base.log.Logs;
import ozudo.net.HttpHelper;
import protocol.Pbmethod;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

import static game.pubsub.Subscriber.getJavaNetPostContent;

public class IAPHandler extends AHandler {
    @Override
    public AHandler newInstance() {
        return new IAPHandler();
    }

    //    String urlGG = "http://3k-gmtool.metgame.net/api/v1/generate-qr-code";
//    String urlCb = "https://app.aordgame.com:7102/api/gg/buyPack";
    String formatSendGG = "kdw__%s__%s__%s__" + StringHelper.getRandomString(20);
    private static IAPHandler instance;
    private static final String APPLICATION_NAME = "Ninja Legends";
    // SỬ DỤNG ĐƯỜNG DẪN TUYỆT ĐỐI CHÍNH XÁC VÀ MỚI NHẤT
    static final String SERVICE_ACCOUNT_FILE = "/etc/google-service/ninjabattle-b27cf-4c11ee83c1e4.json";

    // Thêm các hằng số cho cơ chế thử lại
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_DELAY_MS = 2000;

    public static IAPHandler getInstance() {
        if (instance == null) {
            instance = new IAPHandler();
        }
        return instance;
    }

    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(IAP_STATUS, IAP_BUY, IAP_GG);
        actions.forEach(action -> mHandler.put(action, this));
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        try {
            switch (actionId) {
                case IAP_STATUS -> status();
                case IAP_BUY -> buyIAP();
                //case IAP_GG -> buyIapGG(); //not working
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    private void status() {
        Pbmethod.PbListIAP.Builder pb = Pbmethod.PbListIAP.newBuilder();
        List<Integer> lstBuy = user.getListPackBuy();
        for (int i = 0; i < ResIAP.aIAP.size(); i++) {
            // trừ gói nạp đầu id =1
            ResIAPEntity pack = ResIAP.aIAP.get(i);
            if (pack.getId() != ResIAP.IAP_ID_FIRST_PURCHASE) {
                Pbmethod.PpIAP.Builder pbItem = pack.getPbIap();
                if (!lstBuy.contains(pack.getId())) pbItem.addAllAddBonus(pack.getABonus());
                pb.addIap(pbItem);
            }
        }
        addResponse(pb.build());
    }

    private void buyIAP() throws GeneralSecurityException, IOException {
        Pbmethod.CommonVector cmm = parseCommonVector(requestData);
        int id = (int) cmm.getALong(0);
        YesNo status = YesNo.get((int) cmm.getALong(1));
        int os = (int) cmm.getALong(2); // 0 : android , 1 ios
        String productId = cmm.getAString(0);
        String desc = cmm.getAString(1);
        String payload = cmm.getAStringList().size() > 2 ? cmm.getAString(2) : "";
        ResIAPEntity rPack = ResIAP.getIAP(id);
        // verify google
        byte[] decodedBytes = Base64.getDecoder().decode(payload);
        String decodedJson = new String(decodedBytes);
        JSONObject root = new JSONObject(decodedJson);
        String innerJsonStr = root.getString("json");
        innerJsonStr = innerJsonStr
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
        JSONObject inner = new JSONObject(innerJsonStr);
        // In ra thông tin
        String orderId = inner.getString("orderId");
        if (StringHelper.isEmpty(orderId)) {
            addErrParam();
            return;
        }
        String productGG = inner.getString("productId");
        if (!productId.equals(productGG)) {
            addErrParam();
            LogBuyIAPEntity pack = LogBuyIAPEntity.builder().userId(user.getId()).packId(id).
                    serverId(user.getServer()).price(rPack.getPrice()).descc(desc).orderId(orderId).status(StatusType.LOCK.value).build();
            pack.save();
            return;
        }
        // check đã xử lí chưa

        List<LogBuyIAPEntity> hasLog = DBJPA.getList("cms.log_buy_iap", List.of("order_id", orderId), "", LogBuyIAPEntity.class);
        if (hasLog != null && !hasLog.isEmpty()) {
            addErrParam();
            return;
        }

        String purchaseToken = inner.getString("purchaseToken");
        String pakageName = inner.getString("packageName");
        // --- 3. GỌI API VỚI CƠ CHẾ THỬ LẠI (RETRY LOGIC) ---
        AndroidPublisher publisher = getPublisherService();
        ProductPurchase purchase = null;
        long delay = INITIAL_DELAY_MS;

        for (int retry = 0; retry < MAX_RETRIES; retry++) {
            try {
                // SỬ DỤNG TOKEN THUẦN TÚY (ĐÃ TRÍCH XUẤT)
                AndroidPublisher.Purchases.Products.Get request = publisher.purchases().products()
                        .get(pakageName, productGG, purchaseToken);

                purchase = request.execute();

                // Thành công, thoát vòng lặp
                break;

            } catch (GoogleJsonResponseException e) {
                // Kiểm tra lỗi 5xx (Service Unavailable)
                if (e.getStatusCode() >= 500 && e.getStatusCode() < 600 && retry < MAX_RETRIES - 1) {
                    System.err.printf("LỖI TẠM THỜI (%d): %s. Thử lại sau %d ms...\n",
                            e.getStatusCode(), e.getStatusMessage(), delay);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ignore) {
                        Thread.currentThread().interrupt();
                    }
                    delay *= 2; // Exponential backoff
                } else {
                    // Lỗi 4xx (ví dụ: token sai, quyền sai) hoặc đã hết lần thử lại
                    System.err.println("LỖI KHÔNG THỂ KHẮC PHỤC HOẶC HẾT LẦN THỬ LẠI:");
                    throw e;
                }
            }
        }

        // --- 4. HIỂN THỊ KẾT QUẢ ---
        if (purchase != null) {
            System.out.println("--- KẾT QUẢ XÁC MINH ---");
            if (purchase.getPurchaseState() == 1) {
                addErrResponse(getLang(Lang.err_transaction_cancelled_refund));
                return;
            } else if (purchase.getPurchaseState() == 2) {
                addErrResponse(getLang(Lang.err_payment_processing));
                return;
            }
        } else {
            addErrResponse(getLang(Lang.err_verify_failed));
            return;
        }
        // Nếu giao dịch thành công (state = 0)
        if (purchase.getPurchaseState() == 0) {
            // Theo tài liệu: 0 = chưa acknowledge, 1 = đã acknowledge
            boolean notAcknowledged = (purchase.getAcknowledgementState() == 0);
            if (notAcknowledged) {
                // Tạo request acknowledge
                ProductPurchasesAcknowledgeRequest ackRequest = new ProductPurchasesAcknowledgeRequest();
                ackRequest.setDeveloperPayload("confirm_" + orderId);

                try {
                    publisher.purchases().products()
                            .acknowledge(pakageName, productGG, purchaseToken, ackRequest) // ⚠️ Biến packageName đúng chính tả
                            .execute();
                } catch (Exception e) {
                    Logs.error("❌ Lỗi khi acknowledge giao dịch: " + e.getMessage());
                }
            }
        }


        // end
        String productCheck = os == 0 ? rPack.getProductIdAndroid() : rPack.getProductIdIos();
        if (!productCheck.equals(productId)) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        EventInt uInt = mUser.getUEvent().getEventInt();
        if (id == ResIAP.IAP_ID_FIRST_PURCHASE && uInt.getValue(EventInt.TIME_BUY_FIRST_PURCHASE) > 0) {
            addErrResponse(getLang(Lang.err_limit_pack_buy));
            return;
        }
        // check quà nạp tiền
        UserEventEntity uEvent = mUser.getUEvent();
        // mua thành công thì cộng vật phẩm và chức năng
        if (status == YesNo.yes) {
            boolean update = false;
            int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
            List<Integer> nap = uEvent.getQuaNapTien();
            if (uEvent.hasQuaNapTien()) {
                if (uEvent.getDayBuyIap() != day) {
                    for (int i = 0; i < nap.size(); i++) {
                        if (nap.get(i) == StatusType.PROCESSING.value) {
                            nap.set(i, StatusType.RECEIVE.value);
                            update = true;
                            break;
                        }
                    }
                }
            }
            int numBuy = uEvent.getNumBuyIap() + 1;
            if (update && uEvent.update(Arrays.asList("qua_nap_tien", StringHelper.toDBString(nap), "day_buy_iap", day, "num_buy_iap", numBuy))) {
                uEvent.setQuaNapTien(nap.toString());
                uEvent.setNumBuyIap(numBuy);
                uEvent.setDayBuyIap(day);
            } else {
                if (uEvent.update(Arrays.asList("num_buy_iap", numBuy))) {
                    uEvent.setNumBuyIap(numBuy);
                }
            }
            if (id == ResIAP.IAP_ID_FIRST_PURCHASE)
                uInt.setValueAndUpdate(EventInt.TIME_BUY_FIRST_PURCHASE, DateTime.getNumberDay());
            List<Long> bonus = rPack.getABonus();
            // check x2
            List<Integer> lstPackBuy = user.getListPackBuy();
            boolean x2 = !lstPackBuy.contains(rPack.getId());
            if (x2) {
                lstPackBuy.add(rPack.getId());
                if (user.update(Arrays.asList("pack_buy", StringHelper.toDBString(lstPackBuy)))) {
                    bonus = Bonus.xBonus(bonus, 2);
                    mUser.getUser().setPackBuy(StringHelper.toDBString(lstPackBuy));
                }
            }
            // add vip exp
            bonus.addAll(Bonus.viewVipExp(rPack.getVipExp()));
            addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.BUY_IAP.getKey(id), bonus)));
        } else {
            addErrResponse(getLang(Lang.err_buy_iap_fail));
            Logs.error("EVENT_BUY_PACK:" + cmm.getALongList() + ":" + cmm.getAString(0));
        }
        // save vào log để thống kê
        LogBuyIAPEntity pack = LogBuyIAPEntity.builder().userId(user.getId()).packId(id).
                serverId(user.getServer()).price(rPack.getPrice()).descc(desc).orderId(orderId).status(status.value).build();
        pack.save();
    }

    private AndroidPublisher getPublisherService() throws IOException, GeneralSecurityException {
        File keyFile = new File(SERVICE_ACCOUNT_FILE);

        GoogleCredential credential = GoogleCredential
                .fromStream(new FileInputStream(keyFile))
                .createScoped(Collections.singleton("https://www.googleapis.com/auth/androidpublisher"));

        // THÀNH CÔNG: Đã đọc được File Key JSON.
        System.out.println("THÀNH CÔNG: Đã đọc được File Key JSON.");

        return new AndroidPublisher.Builder(
                credential.getTransport(),
                credential.getJsonFactory(),
                credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
//
//    private void buyIAPQr() {
//        Pbmethod.CommonVector cmm = parseCommonVector(requestData);
//        int id = (int) cmm.getALong(0);
//        YesNo status = YesNo.get((int) cmm.getALong(1));
//        int os = (int) cmm.getALong(2); // 0 : android , 1 ios
//        String productId = cmm.getAString(0);
//        String desc = cmm.getAString(1);
//        ResIAPEntity rPack = ResIAP.getIAP(id);
//        String productCheck = os == 0 ? rPack.getProductIdAndroid() : rPack.getProductIdIos();
//        if (rPack == null || !productCheck.equals(productId)) {
//            addErrResponse(getLang(Lang.err_params));
//            return;
//        }
//        EventInt uInt = mUser.getUEvent().getEventInt();
//        if (id == ResIAP.IAP_ID_FIRST_PURCHASE && uInt.getValue(EventInt.TIME_BUY_FIRST_PURCHASE) > 0) {
//            addErrResponse(getLang(Lang.err_limit_pack_buy));
//            return;
//        }
//        // check quà nạp tiền
//        UserEventEntity uEvent = mUser.getUEvent();
//        // mua thành công thì cộng vật phẩm và chức năng
//        if (status == YesNo.yes) {
//            boolean update = false;
//            int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
//            List<Integer> nap = uEvent.getQuaNapTien();
//            if (uEvent.hasQuaNapTien()) {
//                if (uEvent.getDayBuyIap() != day) {
//                    for (int i = 0; i < nap.size(); i++) {
//                        if (nap.get(i) == StatusType.PROCESSING.value) {
//                            nap.set(i, StatusType.RECEIVE.value);
//                            update = true;
//                            break;
//                        }
//                    }
//                }
//            }
//            int numBuy = uEvent.getNumBuyIap() + 1;
//            if (update && uEvent.update(Arrays.asList("qua_nap_tien", StringHelper.toDBString(nap), "day_buy_iap", day, "num_buy_iap", numBuy))) {
//                uEvent.setQuaNapTien(nap.toString());
//                uEvent.setNumBuyIap(numBuy);
//                uEvent.setDayBuyIap(day);
//            } else {
//                if (uEvent.update(Arrays.asList("num_buy_iap", numBuy))) {
//                    uEvent.setNumBuyIap(numBuy);
//                }
//            }
//            if (id == ResIAP.IAP_ID_FIRST_PURCHASE)
//                uInt.setValueAndUpdate(EventInt.TIME_BUY_FIRST_PURCHASE, DateTime.getNumberDay());
//            List<Long> bonus = rPack.getABonus();
//            // check x2
//            List<Integer> lstPackBuy = user.getListPackBuy();
//            boolean x2 = !lstPackBuy.contains(rPack.getId());
//            if (x2) {
//                lstPackBuy.add(rPack.getId());
//                if(user.update(Arrays.asList("pack_buy", StringHelper.toDBString(lstPackBuy)))){
//                    bonus = Bonus.xBonus(bonus,2);
//                    mUser.getUser().setPackBuy(StringHelper.toDBString(lstPackBuy));
//                }
//            }
//            // add vip exp
//            bonus.addAll(Bonus.viewVipExp(rPack.getVipExp()));
//            addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.BUY_IAP.getKey(id), bonus)));
//        } else {
//            addErrResponse(getLang(Lang.err_buy_iap_fail));
//            Logs.error("EVENT_BUY_PACK:" + cmm.getALongList() + ":" + cmm.getAString(0));
//        }
//        // save vào log để thống kê
//        LogBuyIAPEntity pack = LogBuyIAPEntity.builder().userId(user.getId()).packId(id).serverId(user.getServer()).price(rPack.getPrice()).descc(desc).status(status.value).build();
//        pack.save();
//    }


//    private void buyIapGG() {
//        String productId = getInputCmv().getAString(0);
//        Map<String, String> data = new HashMap<>();
//        data.put("orderId", String.format(formatSendGG, 1, user.getId(), productId));
//        data.put("amount", "1");
//        data.put("currency", "KHR");
//        data.put("urlCallback", urlCb);
//        System.out.println("data.get(\"orderId\") = " + data.get("orderId"));
//        String response = HttpHelper.graphQL(urlGG, GsonUtil.toJson(data));
//        if (response == null) {
//            addErrSystem();
//            return;
//        }
//        JsonObject jArr = GsonUtil.parseJsonObject(response);
//        if (jArr.get("success").getAsBoolean()) {
//            JsonObject jData = GsonUtil.parseJsonObject(GsonUtil.toJson(jArr.get("data")));
//            Pbmethod.CommonVector.Builder cmm = Pbmethod.CommonVector.newBuilder();
//            cmm.addALong(jData.get("expiredTime").getAsLong());
//            cmm.addAString(jData.get("qrCode").getAsString());
//            addResponse(cmm.build());
//        } else addErrSystem();
//    }
}
