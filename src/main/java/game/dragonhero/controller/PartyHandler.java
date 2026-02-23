package game.dragonhero.controller;

import game.config.CfgServer;
import game.config.aEnum.YesNo;
import game.config.lang.Lang;
import game.dragonhero.mapping.UserEntity;
import game.dragonhero.mapping.UserPartyEntity;
import game.dragonhero.server.IAction;
import game.dragonhero.service.resource.ResParty;
import game.monitor.Online;
import game.object.MyUser;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.ListUtil;
import ozudo.base.helper.StringHelper;
import ozudo.base.helper.Util;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import java.util.*;

@Slf4j
public class PartyHandler extends AHandler {
    @Override
    public AHandler newInstance() {
        return new PartyHandler();
    }

    static PartyHandler instance;

    public static PartyHandler getInstance() {
        if (instance == null) {
            instance = new PartyHandler();
        }
        return instance;
    }

    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(PARTY_INVITE_MEMBER, PARTY_REMOVE_MEMBER, PARTY_UPDATE_AUTO, PARTY_LEAVE, PARTY_INFO, PARTY_CHANGE_LEADER, PARTY_ACCEPT);
        actions.forEach(action -> mHandler.put(action, this));
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);

        try {
            switch (actionId) {
                case IAction.PARTY_INVITE_MEMBER -> invite(); // done
                case IAction.PARTY_ACCEPT -> accept(); //done
                case IAction.PARTY_INFO -> info();
                case IAction.PARTY_UPDATE_AUTO -> autoInfo();
                case IAction.PARTY_REMOVE_MEMBER -> remove(); // done
                case IAction.PARTY_LEAVE -> leave();
                case IAction.PARTY_CHANGE_LEADER -> changeLeader();
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    private void invite() {
        UserPartyEntity myParty = user.getParty();
        // đã có nhóm nhung k phai leader
        if (myParty != null && !myParty.isLeader(user.getId())) {
            addErrResponse(getLang(Lang.err_only_leader_can_invite));
            return;
        }

        int targetId = getInputInt();
        MyUser uTarget = Online.getMUser(targetId);
        if (uTarget == null) {
            addErrResponse(getLang(Lang.err_player_not_online));
            return;
        }
        // check nguoi choi co party chua
        if (uTarget.getUser().getParty() != null && !uTarget.getUser().getParty().emptyMember()) {
            addErrResponse(getLang(Lang.err_player_has_party));
            return;
        }

        if (myParty != null && myParty.getMembers().size() >= ResParty.MAX_MEMBER) {
            addErrResponse(getLang(Lang.err_party_full));
            return;
        }

        // ban than chua co nhom thi tao nhom
        if (myParty == null) {
            myParty = new UserPartyEntity(user.getId());
            myParty.create();
            if (user.update(List.of("party", user.getId()))) {
                user.setParty(user.getId());
            }
        }
        // check k cho gui spam
        List<Integer> dataSend = mUser.getCacheSendParty();
        boolean hasCache = false;
        for (int i = 0; i < dataSend.size(); i += 3) {
            int userId = dataSend.get(i);
            if (userId == targetId) {
                hasCache = true;
                // chan 5s sau moi cho gui
                int timeSend = dataSend.get(i + 1);
                if (timeSend > Calendar.getInstance().getTime().getTime() / 1000) {
                    addErrResponse(getLang(Lang.err_waiting_player_response));
                    return;
                }
                // gui toi da 20 loi moi
                int numberSend = dataSend.get(i + 2);
                if (numberSend > 20) {
                    addErrResponse(getLang(Lang.err_too_many_invites));
                    return;
                }
                // chan 5s send 1 lan
                dataSend.set(i + 1, (int) (Calendar.getInstance().getTime().getTime() / 1000 + 5));
                dataSend.set(i + 2, numberSend + 1);
                break;
            }
        }
        // luu lai cach neu tim k co
        if (!hasCache) {
            dataSend.add(targetId);
            dataSend.add((int) (Calendar.getInstance().getTime().getTime() / 1000));
            dataSend.add(1);
        }
        // send data cho nguoi nhan
        Pbmethod.CommonVector.Builder pb = getCommonVector(user.getId()).toBuilder();
        pb.addAString(getLang(uTarget, Lang.msg_x_invite_you_party, user.getName()));
        Util.sendProtoData(uTarget.getChannel(), pb.build(), PARTY_NEW_INVITE);
        // tra ve cho nguoi gui
        addResponse(IAction.PARTY_UPDATE_INFO, getCommonVector(getLang(Lang.msg_invite_sent)));
    }

    private void accept() {
        List<Long> inputs = getInputALong();
        YesNo yesNo = YesNo.get(inputs.get(0).intValue());
        int leaderId = inputs.get(1).intValue();
        MyUser senderUser = Online.getMUser(leaderId);
        if (yesNo == null || yesNo == YesNo.no) {
            if (senderUser != null) {
                Util.sendProtoData(senderUser.getChannel(), getCommonVector(getLang(senderUser, Lang.msg_x_reject_party_invite, user.getName())), IAction.PARTY_UPDATE_INFO);
            }
            addResponseSuccess();
            return;
        }
        // kiem tra ban than co dang co nhom k
        UserPartyEntity myParty = ResParty.getParty(user.getId());
        if (myParty != null) { // dang co party thi check so luong thanh vien
            if (myParty.emptyMember()) { // xoa party
                if (!myParty.delete()) {
                    addErrSystem();
                    return;
                }
                user.setParty(0);
            } else {
                addErrResponse(getLang(Lang.err_leave_current_party_first));
                return;
            }
        }

        // kiểm tra nhom co ton tai k
        UserPartyEntity newParty = ResParty.getParty(leaderId);
        if (newParty == null) {
            addErrResponse(getLang(Lang.err_party_not_exist));
            return;
        }
        // check so luong member
        List<Integer> members = newParty.getMembers();
        if (members.size() >= ResParty.MAX_MEMBER) {
            addErrResponse(getLang(Lang.err_party_full));
            return;
        }
        // tham gia
        members.add(user.getId());
        if (newParty.updateMembers(members)) {
            if (user.update(List.of("party", leaderId))) {
                user.setParty(leaderId);
            }
            sendToastAll(newParty, Lang.msg_x_joined_party, user.getName());
        } else addErrResponse();
    }

    private void remove() {
        int targetId = getInputInt();
        if (targetId == user.getId()) {
            addErrParam();
            return;
        }
        UserPartyEntity uParty = ResParty.getParty(user.getId());
        if (uParty == null) {
            addErrResponse(getLang(Lang.err_only_leader_can_kick));
            return;
        }
        List<Integer> members = uParty.getMembers();
        if (members.contains(targetId) && DBJPA.rawSQL("UPDATE  user  SET party = 0 WHERE id =" + targetId)) {
            members.remove((Object) targetId);
            MyUser iUser = Online.getMUser(targetId);
            if (iUser != null) {
                iUser.getUser().setParty(0);
                Util.sendProtoData(iUser.getChannel(), getCommonVector(getLang(iUser, Lang.msg_kicked_from_party)), PARTY_LEAVE);
            }
            // save party
            uParty.updateMembers(members);
            if (uParty.emptyMember()) {
                addResponse(IAction.PARTY_UPDATE_INFO, getCommonVector(getLang(Lang.msg_party_disbanded)));
            } else addResponse(IAction.PARTY_UPDATE_INFO, getCommonVector(getLang(Lang.msg_kicked_success)));
        } else addErrSystem();
    }

    private void sendToastAll(UserPartyEntity uParty, String msg) {
        // send tất cả trừ bản thân (dùng msg đã dịch - một ngôn ngữ)
        List<Channel> lstChanel = new ArrayList<>();
        List<Integer> fullMember = uParty.getMembersAndLeader();
        for (Integer i : fullMember) {
            Channel chanel = Online.getChannel(i);
            if (chanel != null) lstChanel.add(chanel);
        }
        Util.sendProtoDataToListChanel(lstChanel, getCommonVector(msg), IAction.PARTY_UPDATE_INFO);
    }

    /** Gửi toast cho tất cả thành viên nhóm, mỗi user nhận theo ngôn ngữ của mình. */
    private void sendToastAll(UserPartyEntity uParty, String langKey, Object... params) {
        List<Integer> fullMember = uParty.getMembersAndLeader();
        for (Integer userId : fullMember) {
            Channel chanel = Online.getChannel(userId);
            if (chanel == null) continue;
            MyUser targetUser = Online.getMUser(userId);
            String msg = (params == null || params.length == 0)
                    ? getLang(targetUser, langKey)
                    : getLang(targetUser, langKey, params);
            Util.sendProtoData(chanel, getCommonVector(msg), IAction.PARTY_UPDATE_INFO);
        }
    }

    private void leave() {
        UserPartyEntity uParty = user.getParty();
        if (uParty == null) {
            addErrResponse(getLang(Lang.err_not_in_party));
            return;
        }
        // là leader thì xóa nhóm luôn
        List<Integer> members = uParty.getMembers();
        // có 2 thành viên hoặc là leader thì xóa nhóm luôn
        if (uParty.isLeader(user.getId()) || members.size() <= 1) {
            uParty.delete();
            members.add(uParty.getUserId());
            DBJPA.rawSQL("UPDATE " + CfgServer.DB_DSON + "user as u SET u.party = 0 WHERE id in " + StringHelper.toDBList(members));
            user.setParty(0);
            // tra thong tin cho user khac (mỗi user nhận theo ngôn ngữ của mình)
            members.remove((Object) user.getId());
            for (Integer memberId : members) {
                MyUser iUser = Online.getMUser(memberId);
                if (iUser != null) {
                    iUser.getUser().setParty(0);
                    Util.sendProtoData(iUser.getChannel(), getCommonVector(getLang(iUser, Lang.msg_x_left_party_disbanded, user.getName())), PARTY_UPDATE_INFO);
                }
            }
            addResponse(getCommonVector(getLang(Lang.msg_party_disbanded_done)));
        } else { // tu minh roi nhom
            members.remove((Object) user.getId());
            if (uParty.updateMembers(members) && user.update(List.of("party", 0))) {
                user.setParty(0);
                addResponse(getCommonVector(getLang(Lang.msg_left_party)));
                sendToastAll(uParty, Lang.msg_x_left_party, user.getName());
            } else addErrSystem();
        }

    }


    private void info() {
        UserPartyEntity uParty = user.getParty();
        Pbmethod.PbListUser.Builder builder = Pbmethod.PbListUser.newBuilder();
        if (uParty == null || uParty.emptyMember()) {
            addErrResponse(getLang(Lang.err_not_in_party));
            return;
        }
        List<Integer> fullMember = uParty.getMembers();
        for (int i = 0; i < fullMember.size(); i++) {
            Pbmethod.PbUser.Builder pb = checkUser(fullMember.get(i), uParty);
            if (pb != null) builder.addAUser(pb);
        }
        builder.addAUser(0, checkUser(uParty.getUserId(), uParty));
        addResponse(IAction.PARTY_INFO, builder.build());
    }

    private void autoInfo() {
        UserPartyEntity uParty = user.getParty();
        Pbmethod.PbListUser.Builder builder = Pbmethod.PbListUser.newBuilder();
        if (uParty == null || uParty.emptyMember()) {
            addResponse(IAction.PARTY_UPDATE_AUTO, builder.build());
            return;
        }
        List<Integer> fullMember = uParty.getMembers();
        for (int i = 0; i < fullMember.size(); i++) {
            Pbmethod.PbUser.Builder pb = checkUser(fullMember.get(i), uParty);
            if (pb != null) builder.addAUser(pb);
        }
        builder.addAUser(0, checkUser(uParty.getUserId(), uParty));
        addResponse(IAction.PARTY_UPDATE_AUTO, builder.build());
    }

    private Pbmethod.PbUser.Builder checkUser(int userId, UserPartyEntity uParty) {
        Pbmethod.PbUser.Builder pb;
        MyUser iUser = Online.getMUser(userId);
        if (iUser == null) { // offline
            UserEntity uMem = Online.getDbUser(userId);
            if (uMem == null) return null;
            pb = uMem.toProto(0).toBuilder();
            pb.setInfo(getCommonVector(0, uParty.isLeader(userId) ? 1 : 0, 0)); //is online - isLeader - cùng map
        } else { // online
            pb = iUser.getUser().toProto().toBuilder();
            boolean inMap = iUser.getPlayer().getRoom()!=null&& iUser.getPlayer().getRoom().getKeyRoom().equals(mUser.getPlayer().getRoom().getKeyRoom());
            pb.setInfo(getCommonVector(1, uParty.isLeader(userId) ? 1 : 0, inMap ? 1 : 0)); //is online - isLeader - cùng map
        }
        return pb;
    }


    private void changeLeader() {
        int leaderNew = getInputInt();
        UserPartyEntity uParty = user.getParty();
        if (!uParty.isLeader(user.getId())) {
            addErrResponse(getLang(Lang.err_not_leader));
            return;
        }
        List<Integer> members = uParty.getMembers();
        if (members.size() <= 0) {
            addErrResponse(getLang(Lang.err_can_only_disband));
            return;
        }
        if (!members.contains(leaderNew)) {
            addErrResponse(getLang(Lang.user_not_found));
            return;
        }
        members.remove((Object) leaderNew);
        members.add(user.getId());
        // đổi chủ trong db
        List<Integer> lstIdUpdate = uParty.getMembersAndLeader();
        if (DBJPA.rawSQL("UPDATE " + CfgServer.DB_DSON + "user_party as u SET u.user_id = " + leaderNew + ", members='" + StringHelper.toDBString(members) + "' WHERE u.user_id= " + user.getId())
            &&  DBJPA.rawSQL("UPDATE  user  SET party = "+leaderNew+" WHERE id in(" + ListUtil.joinElement(lstIdUpdate) +")")){
            // đổi chử ở cache
            ResParty.mPartyMap.remove(user.getId());
            uParty.setUserId(leaderNew);
            uParty.setMembers(members.toString());
            ResParty.mPartyMap.put(leaderNew, uParty);
            // set lai party cho toàn bộ user
            for (int i = 0; i < lstIdUpdate.size(); i++) {
                MyUser iUser = Online.getMUser(lstIdUpdate.get(i));
                if (iUser != null) {
                    iUser.getUser().setParty(leaderNew);
                }
            }
            // gửi về lại update toàn bộ trong team
            sendToastAll(uParty, Lang.msg_leader_changed);
        } else addErrSystem();

    }


}
