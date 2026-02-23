package game.pubsub;

import com.google.gson.JsonObject;
import game.config.CfgChat;
import game.config.CfgServer;
import game.config.aEnum.NotifyType;
import game.config.aEnum.DetailActionType;
import game.config.aEnum.StatusType;
import game.config.aEnum.ToastType;
import game.config.aEnum.YesNo;
import game.config.lang.Lang;
import game.dragonhero.controller.AHandler;
import game.dragonhero.mapping.*;
import game.dragonhero.mapping.main.ResIAPEntity;
import game.dragonhero.server.App;
import game.dragonhero.server.AppConfig;
import game.dragonhero.server.IAction;
import game.dragonhero.service.Services;
import game.dragonhero.service.resource.ResGift;
import game.dragonhero.service.resource.ResIAP;
import game.dragonhero.service.user.Bonus;
import game.monitor.Online;
import game.object.MyUser;
import game.protocol.CommonProto;
import io.netty.channel.Channel;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.*;
import ozudo.base.log.Logs;
import ozudo.base.log.slib_Logger;
import ozudo.net.HttpHelper;
import redis.clients.jedis.JedisPubSub;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.*;

@NoArgsConstructor
public class Subscriber extends JedisPubSub {

    @Override
    public void onMessage(String channel, String message) {
        int pos = message.indexOf("@");
        String result = "", serviceName = "name";

        int service = 0;
        try {
            if (pos >= 0) service = Integer.parseInt(message.substring(0, pos));
            if (pos >= 0) message = message.substring(pos + 1);
            System.out.println("message = " + message);
            serviceName = "none_" + service;

            PubSubService pubSubService = PubSubService.get(service);
            if (pubSubService != null) {
                serviceName = pubSubService.name();
                switch (pubSubService) {
                    case MAIL_NOTIFY -> mailNotify(message);
                    case MESSAGE_SLIDER -> messageNotify(message);
                    case TELEGRAM_NOTIFY -> telegramNotify(message);
                    case MESSAGE_TOAST -> messageToast(message);
                    case RELOAD_CONFIG_CHAT -> reloadConfigChat(message);
//                    case NAP_SMS -> buyPackSms(message);
                    case BUY_QR -> buyPackQr(message);
                    case RELOAD_CONFIG -> reloadConfig(message);
                    case RELOAD_GIFT_CODE -> reloadGiftCode(message);
                    case DELAY_RESTART_SERVER -> delayRestartServer(message);
                }
            }
        } catch (Exception ex) {
            getLogger().error(GUtil.exToString(ex));
        }
        if (StringHelper.isEmpty(result)) {
            getLogger().info(String.format("%s -> %s", serviceName, message));
        } else {
            getLogger().info(String.format("%s -> %s -> %s", serviceName, message, result));
        }

    }

    /**
     * @param userId
     */
    void mailNotify(String userId) {
        Channel chanel = Online.getChannel(Integer.parseInt(userId));
        if (chanel != null)
            Util.sendProtoData(chanel, CommonProto.getCommonVector(NotifyType.MAIL.value), IAction.ADD_NOTIFY);
    }

    void reloadConfigChat(String msg) {
        CfgChat.loadConfigChat();
    }

    void reloadGiftCode(String msg) {
        ResGift.init();
    }

    void reloadConfig(String msg) throws Exception {
        App.reloadConfig();
    }

//    void buyPackSms(String message) {
//        JsonObject obj = GsonUtil.parseJsonObject(message);
//        int userId = obj.get("userId").getAsInt();
//        String productId = obj.get("productId").getAsString();
////        String msg = obj.get("message").getAsString();
//        YesNo status = obj.get("success").getAsBoolean() ? YesNo.yes : YesNo.no;
//        MyUser mUser = Online.getMUser(userId);
//        if (mUser == null) return;
//
//        UserEntity user = Services.userDAO.getUser(mUser.getUser().getId());
//        int os = obj.get("os").getAsInt();
//        ResIAPEntity rPack = ResIAP.getIAPProduct(os, productId);
//        String checkProductId = os == 0 ? rPack.getProductIdAndroid() : rPack.getProductIdIos();
//        if (rPack == null || (!checkProductId.equals(productId))) {
//            Util.sendProtoData(mUser.getChannel(), CommonProto.getCommonVector(AHandler.getLang(mUser, Lang.err_params)), IAction.MSG_TOAST);
//            return;
//        }
//
//        UserEventEntity uEvent = mUser.getUEvent();
//        EventInt uInt = uEvent.getEventInt();
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
//            if (rPack.getId() == ResIAP.IAP_ID_FIRST_PURCHASE)
//                uInt.setValueAndUpdate(EventInt.TIME_BUY_FIRST_PURCHASE, DateTime.getNumberDay());
//            List<Long> bonus = rPack.getABonus();
//            // add vip exp
//            bonus.addAll(Bonus.viewVipExp(rPack.getVipExp()));
//            Util.sendProtoData(mUser.getChannel(), CommonProto.getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.BUY_IAP.getKey(rPack.getId()), bonus)), IAction.IAP_BUY);
//
//        } else {
//            Util.sendProtoData(mUser.getChannel(), CommonProto.getCommonVector(AHandler.getLang(mUser, Lang.err_buy_iap_fail)), IAction.IAP_BUY);
//            Logs.error("EVENT_BUY_PACK:" + rPack.getId() + ":" + userId);
//        }
//        // save vào log để thống kê
//        LogBuyIAPEntity pack = LogBuyIAPEntity.builder().userId(user.getId()).packId(rPack.getId()).serverId(user.getServer()).price(rPack.getPrice()).descc(message).status(status.value).build();
//        pack.save();
//    }
//

    void buyPackQr(String message) {
        JsonObject obj = GsonUtil.parseJsonObject(message);
        int userId = obj.get("userId").getAsInt();
        int packId = obj.get("packId").getAsInt();
        int code = obj.get("code").getAsInt();

        MyUser mUser = Online.getMUser(userId);
        if (mUser == null) return;
        UserEntity user = Services.userDAO.getUser(mUser.getUser().getId());
        ResIAPEntity rPack = ResIAP.getIAP(packId);
        if (rPack == null) {
            Util.sendProtoData(mUser.getChannel(), CommonProto.getCommonVector(AHandler.getLang(mUser, Lang.err_params)), IAction.MSG_TOAST);
            return;
        }
        // check DB
        List<UserBuyQrEntity> uBuyQr = DBJPA.getList("user_buy_qr", Arrays.asList("user_id", user.getId(), "pack_id", packId, "code", code, "status", StatusType.PROCESSING.value), "", UserBuyQrEntity.class);
        if (uBuyQr.size() == 0) return;
        UserEventEntity uEvent = mUser.getUEvent();
        EventInt uInt = uEvent.getEventInt();
        UserBuyQrEntity uBuy = uBuyQr.get(0);
        if (uBuy.getStatus() != StatusType.PROCESSING.value) return;
        boolean update = false;
        // check qua nap tien
        int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        List<Integer> nap = uEvent.getQuaNapTien();
        if (uEvent.hasQuaNapTien()) {
            if (uEvent.getDayBuyIap() != day) {
                for (int j = 0; j < nap.size(); j++) {
                    if (nap.get(j) == StatusType.PROCESSING.value) {
                        nap.set(j, StatusType.RECEIVE.value);
                        update = true;
                        break;
                    }
                }
            }
        }
        // check evnt buy
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

        if (rPack.getId() == ResIAP.IAP_ID_FIRST_PURCHASE)
            uInt.setValueAndUpdate(EventInt.TIME_BUY_FIRST_PURCHASE, DateTime.getNumberDay());
        List<Long> bonus = rPack.getABonus();
        // add vip exp

        uBuy.setStatus(StatusType.DONE.value);
        List<Integer> lstPackBuy = user.getListPackBuy();
        boolean x2 = !lstPackBuy.contains(packId);
        if (x2) {
            lstPackBuy.add(packId);
            if(user.update(Arrays.asList("pack_buy", StringHelper.toDBString(lstPackBuy)))){
                bonus = Bonus.xBonus(bonus,2);
                mUser.getUser().setPackBuy(StringHelper.toDBString(lstPackBuy));
            }
        }
        bonus.addAll(Bonus.viewVipExp(rPack.getVipExp()));
        if (DBJPA.update(uBuy)) {
            Util.sendProtoData(mUser.getChannel(), CommonProto.getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.BUY_IAP.getKey(rPack.getId()), bonus)), IAction.IAP_BUY_QR);
            // save vào log để thống kê
            LogBuyIAPEntity pack = LogBuyIAPEntity.builder().userId(user.getId()).packId(rPack.getId()).serverId(user.getServer()).price(rPack.getPrice()).descc(message).status(YesNo.yes.value).build();
            pack.save();
        }
    }

    void messageNotify(String msg) {
        String[] tmp = msg.split("_");
        if (tmp.length < 2) return;
        int server = Integer.parseInt(tmp[0]);
        List<Channel> channels = new ArrayList<>();
        if (server == 0) { // all server
            channels = Online.getAllChanel();
        } else channels = Online.getUserInServer(server);

        Util.sendSliderChat(channels, tmp[1]);
    }

    void messageToast(String msg) {
        String[] tmp = msg.split("_");
        if (tmp.length < 3) return;
        int server = Integer.parseInt(tmp[0]);
        ToastType type = ToastType.get(Integer.parseInt(tmp[1]));
        if (type == null) return;
        List<Channel> channels = new ArrayList<>();
        if (server == 0) { // send all server
            channels = Online.getAllChanel();
        } else channels = Online.getUserInServer(server);
        Util.sendToast(channels, type, tmp[2]);
    }

    void delayRestartServer(String msg){
        String[] tmp = msg.split("_");
        if (tmp.length < 2) return;
        int server = Integer.parseInt(tmp[0]);
        int delayTime = Integer.parseInt(tmp[1])*60; // minutes
        List<Channel> channels = new ArrayList<>();
        if (server == 0) { // send all server
            channels = Online.getAllChanel();
        } else channels = Online.getUserInServer(server);
        if(channels.isEmpty()) return;
        Util.sendProtoDataToListChanel(channels, protocol.Pbmethod.CommonVector.newBuilder().addALong(delayTime).addAString(Lang.getTitle(CfgServer.config.mainLanguage, Lang.countdown_server_update)).build(), IAction.COUNTDOWN_MSG);
    }

    void telegramNotify(String message) {
        String url = String.format("https://api.telegram.org/%s/sendMessage?parse_mode=html", AppConfig.cfg.telegram.botId);
        Map<String, String> data = new HashMap<>();
        data.put("chat_id", String.valueOf(AppConfig.cfg.telegram.chatId));
        data.put("text", message);
        HttpHelper.getPostContent(url, data);
//        String response = getJavaNetPostContent(url, data);
//        warningDAO.save(GameApiLog.builder().timestamp(new Date()).apiType("telegram").message(message).response(response).info(info).build());
    }

    public static String getJavaNetPostContent(String url, Map<String, String> data) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder().uri(URI.create(url)).header("Content-Type", "application/json").POST(java.net.http.HttpRequest.BodyPublishers.ofString(GsonUtil.toJson(data))).build();
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception ex) {
            Logs.error(ex);
        }
        return null;
    }


    @Override
    public void onPMessage(String pattern, String channel, String message) {

    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {

    }

    @Override
    public void onUnsubscribe(String channel, int subscribedChannels) {

    }

    @Override
    public void onPUnsubscribe(String pattern, int subscribedChannels) {

    }

    @Override
    public void onPSubscribe(String pattern, int subscribedChannels) {

    }

    public Logger getLogger() {
        return slib_Logger.redis();
    }

}
