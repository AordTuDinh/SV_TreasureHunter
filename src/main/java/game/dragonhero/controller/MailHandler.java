package game.dragonhero.controller;

import game.config.CfgFeature;
import game.config.aEnum.DetailActionType;
import game.config.aEnum.FeatureType;
import game.config.lang.Lang;
import game.dragonhero.mapping.UserMailEntity;
import game.dragonhero.service.user.Actions;
import game.dragonhero.service.user.Bonus;
import game.protocol.CommonProto;
import io.netty.channel.Channel;
import ozudo.base.database.DBJPA2;
import ozudo.base.log.Logs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MailHandler extends AHandler {
    private static final int STATUS_NORMAL = 0;
    private static final int STATUS_RECEIVED = 1;
    private static final int STATUS_REMOVE = 2;

    @Override
    public AHandler newInstance() {
        return new MailHandler();
    }

    static MailHandler instance;

    public static MailHandler getInstance() {
        if (instance == null) {
            instance = new MailHandler();
        }
        return instance;
    }

    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(MAIL_LIST, MAIL_RECEIVE, MAIL_DELETE);
        actions.forEach(action -> mHandler.put(action, this));
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        checkTimeMonitor("s");
        if (!CfgFeature.isOpenFeature(FeatureType.MAIL, mUser, this)) {
            return;
        }
        try {
            switch (actionId) {
                case MAIL_LIST:
                    listMail();
                    break;
                case MAIL_RECEIVE:
                    receive();
                    break;
                case MAIL_DELETE:
                    delete();
                    break;
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    void listMail() {
        List<UserMailEntity> uMail = dbGetList();
        protocol.Pbmethod.PbListMail.Builder builder = protocol.Pbmethod.PbListMail.newBuilder();
        uMail.forEach(mail -> {
            builder.addAMail(mail.toProto());
        });
        addResponse(builder.build());
    }

    void receive() {
        int mailId = (int) CommonProto.parseCommonVector(requestData).getALong(0);
        if (mailId == -1) { // view all
            List<UserMailEntity> uMail = dbGetListUnreceived();
            if (uMail.isEmpty()) {
                addResponse(protocol.Pbmethod.ListCommonVector.newBuilder().build());
            } else {
                List<Long> bonus = new ArrayList<>();
                boolean isFull = false;
                for (int i = 0; i < uMail.size(); i++) {
                    List<Long> tmp = uMail.get(i).getListBonus();
                    if (!tmp.isEmpty()) {
                        if (mUser.checkSlotAddBonus(tmp)) {
                            bonus.addAll(tmp);
                        } else {
                            uMail.remove(i);
                            isFull = true;
                        }
                    }
                }
                bonus = Bonus.merge(bonus);
                if (uMail.isEmpty() && isFull) {
                    addErrResponse(getLang(Lang.err_max_slot));
                    return;
                }
                String ids = uMail.stream().map(mail -> String.valueOf(mail.getId())).collect(Collectors.joining(","));
                if (DBJPA2.rawSQL("update user_mail set receive=" + STATUS_RECEIVED + " where user_id=" + user.getId() + " and id in (" + ids + ")")) {
                    Actions.save(user, "mail", "update_receive", "ids", ids);
                    protocol.Pbmethod.ListCommonVector.Builder builder = protocol.Pbmethod.ListCommonVector.newBuilder();
                    builder.addAVector(CommonProto.getCommonVectorProto(Bonus.receiveListItem(mUser, DetailActionType.NHAN_THU.getKey(ids), bonus)));
                    addResponse(builder.build());
                }
            }
        } else { // view one
            UserMailEntity uMail = dbGetMail(mailId);
            if (uMail == null) {
                addErrResponse(getLang(Lang.err_params));
                return;
            }
            List<Long> aBonus = Bonus.merge(uMail.getListBonus());
            if (!mUser.checkSlotAddBonus(aBonus)) {
                addErrResponse(getLang(Lang.err_max_slot));
                return;
            }
            if (uMail.getReceive() == STATUS_NORMAL && uMail.updateStatus(STATUS_RECEIVED)) {
                protocol.Pbmethod.ListCommonVector.Builder builder = protocol.Pbmethod.ListCommonVector.newBuilder();
                builder.addAVector(CommonProto.getCommonVectorProto(Bonus.receiveListItem(mUser, DetailActionType.NHAN_THU.getKey(uMail.getId()), aBonus)));
                addResponse(builder.build());
            }
        }
    }

    void delete() {
        int mailId = (int) CommonProto.parseCommonVector(requestData).getALong(0);
        if (mailId == -1) {
            if (dbDeleteAllMail()) addResponse(null);
            else addErrResponse();
        } else {
            UserMailEntity uMail = dbGetMail(mailId);
            if (uMail == null || uMail.getUserId() != user.getId()) {
                addErrResponse();
                return;
            }
            if (dbDeleteMail(mailId)) {
                addResponse(null);
            } else addErrResponse();
        }
    }

    //region Database
    List<UserMailEntity> dbGetList() {
        return DBJPA2.getList("user_mail", Arrays.asList("user_id", user.getId()), " and receive in (0,1) order by receive asc, date_created desc limit 50", UserMailEntity.class);
    }

    List<UserMailEntity> dbGetListUnreceived() {
        return DBJPA2.getList("user_mail", Arrays.asList("user_id", user.getId(), "receive", STATUS_NORMAL), " order by date_created desc limit 50", UserMailEntity.class);
    }

    private boolean dbDeleteMail(int mailId) {
        return DBJPA2.update("user_mail", Arrays.asList("receive", STATUS_REMOVE), Arrays.asList("user_id", user.getId(), "id", mailId));
    }

    private boolean dbDeleteAllMail() {
        return DBJPA2.rawSQL("update user_mail set receive=2 where user_id=" + user.getId() + " and receive=1");
    }

    private UserMailEntity dbGetMail(int mailId) {
        return (UserMailEntity) DBJPA2.getUnique("user_mail", UserMailEntity.class, "user_id", user.getId(), "id", mailId);
    }
    //endregion
}
