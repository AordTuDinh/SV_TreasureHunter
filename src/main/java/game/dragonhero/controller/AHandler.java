package game.dragonhero.controller;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.ByteString;
import game.config.CfgServer;
import game.config.aEnum.ToastType;
import game.config.lang.Lang;
import game.dragonhero.mapping.UserEntity;
import game.dragonhero.server.IAction;
import game.monitor.Online;
import game.object.MyUser;
import game.protocol.CommonProto;
import io.netty.channel.Channel;
import lombok.Data;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.StringHelper;
import protocol.Pbmethod;

import javax.persistence.EntityManager;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Data
public abstract class AHandler extends IAction {
    protected Pbmethod.ResponseData.Builder response = Pbmethod.ResponseData.newBuilder();
    protected int actionId = 0;
    protected String session = null, ip;
    protected byte[] requestData;
    protected Channel channel;
    protected MyUser mUser;
    protected UserEntity user;
    protected String debug = "";
    protected long time = System.currentTimeMillis();


    public abstract AHandler newInstance();

    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        this.actionId = actionId;
        this.channel = channel;
        this.session = session;
        this.ip = ((InetSocketAddress) channel.remoteAddress()).getHostString();
        this.requestData = requestData;
        if (!StringHelper.isEmpty(session)) {
            mUser = Online.getMUser(channel);
            if (mUser == null && !loginServices.contains(actionId)) {
                addResponse(LOGIN_REQUIRE, null);
                return;
            }
            if (mUser != null) {
                Channel oldChannel = Online.getChannel(mUser.getUser().getId());
                if (oldChannel == null) Online.addChannel(mUser.getUser(), channel);
                user = mUser.getUser();
                if (mUser.getChannel() == null) mUser.setChannel(channel);
            }
        }
        debug("-----------> Time: " + DateTime.getFullDate() + " -- Receive Service: " + actionId + " -- Length: " + requestData.length);
        if (requestData.length > 0) {
            Pbmethod.CommonVector cmm = getInputCmv();
            addDebugLog("Client send aLong: " + cmm.getALongList() + " ----- aString:" + cmm.getAStringList());
        }
    }

    public abstract void initAction(Map<Integer, AHandler> mHandler);

    public void addResponse(AbstractMessage msg) {
        addResponse(actionId, msg);
    }

    public void addResponseSuccess() {
        addResponse(getCommonVector(1));
    }

    public void addResponseError() {
        addResponse(getCommonVector(0));
    }

    public void addErrResponse(String msg) {
        addResponse(MSG_TOAST, ToastType.NORMAL.retToast(msg));
    }

    public void addErrParam() {
        addErrResponse(getLang(Lang.err_params));
    }

    public void addErrSystem() {
        addErrResponse(getLang(Lang.err_system_down));
    }

    public void addDebugLog(String msg) {
        if (CfgServer.isTestServer()) addResponse(DEBUG_LOG, CommonProto.getCommonVector(msg));
    }

    public void addDebugLogError(String msg) {
        if (CfgServer.isTestServer()) addResponse(DEBUG_ERROR, CommonProto.getCommonVector(msg));
    }

    public void addBonusToast(List<Long> bonus) {
        addResponse(BONUS_TOAST, CommonProto.getCommonVector(bonus));
    }


    public void addBonusToastPlus(List<Long> bonus) {
        addResponse(BONUS_TOAST_POSITIVE, CommonProto.getCommonVector(bonus));
    }

    public void addBonusPrivate(List<Long> bonus) {
        addResponse(UPDATE_BONUS_PRIVATE, CommonProto.getCommonVector(bonus));
    }

    public void addErrResponse() {
        addResponse(MSG_TOAST, ToastType.NORMAL.retToast(getLang(Lang.err_send_data)));
    }

    public void addToast(ToastType type, String msg) {
        if (type != null) {
            addResponse(MSG_TOAST, type.retToast(msg));
        } else {
            debug("Sai cấu trúc toast " + msg);
        }
    }

    public void addCountDown(long time, String msg) { // String format : "Test xem sao {0}"
        addResponse(COUNTDOWN_MSG, CommonProto.getCommonVectorProto(Arrays.asList(time), Arrays.asList(msg)));
    }


    public void addServiceErrResponse() {
        addResponse(SERVICE_ERROR, null);
    }

    public void addPopupResponse(String msg) {
        addResponse(MSG_POPUP, CommonProto.getCommonVectorProto(null, Arrays.asList(getLang(msg))));
    }

    public void addResponse(int action, AbstractMessage msg) {
        String username = user == null ? "none" : user.getUsername();
        debug(username + " <-- " + action + " <-- " + (msg == null ? "none" : msg.toString()));
        Pbmethod.PbAction.Builder builder = Pbmethod.PbAction.newBuilder();
        builder.setActionId(action);
        if (msg != null) {
            builder.setData(msg.toByteString());
        }
        response.addAAction(builder.build());
    }

    public void addRawResponse(int action, byte[] data) {
        Pbmethod.PbAction.Builder builder = Pbmethod.PbAction.newBuilder();
        builder.setActionId(action);
        builder.setData(ByteString.copyFrom(data));
        response.addAAction(builder.build());
    }

    public Pbmethod.ResponseData.Builder getResponse() {
        return response;
    }

    public Pbmethod.CommonVector getCommonVector(Object... values) {
        Pbmethod.CommonVector.Builder builder = Pbmethod.CommonVector.newBuilder();
        for (Object value : values) {
            builder.addALong(Long.parseLong(value.toString()));
        }
        return builder.build();
    }


    public Pbmethod.CommonVector getCommonVector(Long... values) {
        return CommonProto.getCommonVectorProto(values);
    }

    public Pbmethod.CommonVector getCommonVector(Integer... values) {
        return CommonProto.getCommonIntVectorProto(values);
    }

    public Pbmethod.CommonVector getCommonVector(List<Long> values) {
        return CommonProto.getCommonVectorProto(values);
    }

    public Pbmethod.CommonVector getCommonIntVector(List<Integer> values) {
        return CommonProto.getCommonIntVectorProto(values);
    }

    public Pbmethod.CommonVector getCommonVector(String... values) {
        return CommonProto.getCommonLongVectorProto(null, Arrays.stream(values).toList());
    }

    public void checkTimeMonitor(String... k) {
        if (k.length == 0) this.time = System.currentTimeMillis();
        else {
            this.debug += "|" + k[0] + (System.currentTimeMillis() - this.time);
            this.time = System.currentTimeMillis();
        }
    }

    void debug(String msg) {
        if (CfgServer.isTestServer()) {
//            System.out.println(msg.length() < 500 ? msg : msg.substring(0, 500) + " ...");
//            System.out.println("");
        }
    }

    //public void checkNotify() {
    //    if (mUser != null) {
    //        checkTimeMonitor();
    //        // TODO Popup msg
    //        if (!mUser.getMsgNotify().isEmpty()) {
    //            int size = mUser.getMsgNotify().size();
    //            for (int i = 0; i < size; i++)
    //                response.addAAction(mUser.getMsgNotify().get(i)); //addPopupResponse(mUser.getMsgNotify().get(i));
    //            mUser.getMsgNotify().clear();
    //        }
    //
    //        // TODO Bonus notify
    //        if (!mUser.getABonus().isEmpty()) {
    //            addResponse(IAction.UPDATE_BONUS, getCommonVector(mUser.getABonus()));
    //            mUser.getABonus().clear();
    //        }
    //        checkTimeMonitor("n");
    //    }
    //}

    protected Pbmethod.CommonVector parseCommonVector(byte[] data) {
        return CommonProto.parseCommonVector(data);
    }

    protected List<Long> getInputALong() {
        return parseCommonVector(requestData).getALongList();
    }

    protected Pbmethod.CommonVector getInputCmv() {
        return parseCommonVector(requestData);
    }

    protected Long getInputLong() {
        return parseCommonVector(requestData).getALong(0);
    }

    protected Integer getInputInt() {
        return Math.toIntExact(parseCommonVector(requestData).getALong(0));
    }

    protected String getInputString() {
        return parseCommonVector(requestData).getAString(0);
    }


    protected String getLang(Object... keys) {
        if (mUser != null) return getLang(mUser, keys);
        Lang lang = Lang.instance(CfgServer.config.mainLanguage);
        if (keys.length == 1) return lang.get((String) keys[0]);
        String format = lang.get((String) keys[0]);
        Object[] formatArgs = Arrays.copyOfRange(keys, 1, keys.length);
        return formatArgs.length == 0 ? format : String.format(format, formatArgs);
    }


    /**
     * Lấy text theo ngôn ngữ của user đích (dùng khi gửi tin cho user khác).
     */
    public static String getLang(MyUser forUser, Object... keys) {
        String locale = (forUser != null && forUser.getUser() != null && !StringHelper.isEmpty(forUser.getUser().getLang()))
                ? forUser.getUser().getLang() : null;
        Lang lang = Lang.instance(locale);
        if (keys.length == 1) return lang.get((String) keys[0]);
        String format = lang.get((String) keys[0]);
        Object[] formatArgs = Arrays.copyOfRange(keys, 1, keys.length);
        return formatArgs.length == 0 ? format : String.format(format, formatArgs);
    }

    /**
     * Lấy text theo ngôn ngữ của user đích với key và mảng tham số (cho sendToastAll).
     */
    public static String getLang(MyUser forUser, String key, Object[] formatArgs) {
        String locale = (forUser != null && forUser.getUser() != null && !StringHelper.isEmpty(forUser.getUser().getLang()))
                ? forUser.getUser().getLang() : null;
        Lang lang = Lang.instance(locale);
        if (formatArgs == null || formatArgs.length == 0) return lang.get(key);
        return String.format(lang.get(key), formatArgs);
    }

    protected void closeSession(EntityManager session) {
        DBJPA.closeSession(session);
    }
}
