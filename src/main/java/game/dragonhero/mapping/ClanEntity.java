package game.dragonhero.mapping;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
import game.cache.JCache;
import game.config.CfgClan;
import game.config.aEnum.ClanPosition;
import game.config.aEnum.ItemKey;
import game.config.aEnum.NotifyType;
import game.config.aEnum.TopType;
import game.config.lang.Lang;
import game.dragonhero.controller.AHandler;
import game.dragonhero.mapping.main.ResContributeEntity;
import game.dragonhero.mapping.main.ResDynamicTypeEntity;
import game.dragonhero.service.Services;
import game.dragonhero.service.resource.ResClan;
import game.dragonhero.service.user.Actions;
import game.dragonhero.service.user.Bonus;
import game.dragonhero.table.BossClanRoom;
import game.monitor.ChatMonitor;
import game.monitor.ClanManager;
import game.monitor.Online;
import game.object.CellDynamic;
import game.object.ChatObject;
import game.object.DataDaily;
import game.object.MyUser;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sf.json.JSONArray;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.*;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Data
@Entity
@Table(name = "clan")
@NoArgsConstructor
public class ClanEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    int server, avatar, masterId, level, member, joinRule, bossLevel, rank, star, levelQuest, pointDynamic;
    String name, master, intro, activityLog, memberId, dynamic, dynamicId; //dynamicId : week - id
    String infoAttackBoss;// day-cur attack
    long exp, power, contribute, honor, timeOpenBoss;
    @Transient
    private Object lockObj = new Object();
    @Transient
    private List<ClanReqEntity> aReq;
    @Transient
    ChatMonitor chatMan;
    @Transient
    List<CellDynamic> dynamics;
    @Transient
    BossClanRoom curBossRoom;

    public ClanEntity(UserEntity master, String intro, int avatar, String name, int joinRule, int level) {
        this.server = master.getServer();
        this.avatar = avatar;
        this.masterId = master.id;
        this.level = level;
        this.member = 1;
        this.joinRule = joinRule;
        this.levelQuest = 1;
        this.bossLevel = 1;
        this.rank = 0;
        this.contribute = 0;
        this.honor = 0;
        this.name = name;
        this.master = master.getUsername();
        this.intro = intro;
        this.power = master.getPower();
        this.exp = 0;
        this.activityLog = "[]";
        this.memberId = "[]";
        this.dynamicId = "[]";
        this.infoAttackBoss = "[0,0]";
        addClanLog(Lang.clan_message_1, master.getName(), name);
    }

    public void initChat() {
        chatMan = new ChatMonitor("clanChat:" + id);
    }

    public List<Integer> getDynamicId() {
        if (dynamicId == null) dynamicId = "[]";
        List<Integer> ids = GsonUtil.strToListInt(dynamicId);
        if (ids.size() != 2) ids = Arrays.asList(0, 1);
        if (ids.get(0) != Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) && !dynamics.isEmpty()) {
            dynamics.clear();
            ids.set(0, Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
            ids.set(1, 1);
            this.dynamic = dynamics.toString();
            update(Arrays.asList("dynamic", StringHelper.toDBString(dynamic), "dynamic_id", StringHelper.toDBString(ids)));
        }
        return ids;
    }

    public void attackBoss(BossClanRoom curBossRoom) {
        this.curBossRoom = curBossRoom;
        List<Integer> info = getInfoAttackBoss();
        info.set(1, info.get(1) + 1);
        this.infoAttackBoss = info.toString();
        update(List.of("info_attack_boss", StringHelper.toDBString(info)));
    }

    public List<Integer> getInfoAttackBoss() {
        List<Integer> info = GsonUtil.strToListInt(infoAttackBoss);
        while (info.size() < 2) info.add(0);
        int day = DateTime.getNumberDay();
        if (info.get(0) != day) {
            info = Arrays.asList(day, 0);
        }
        return info;
    }

    public void checkEndBoss() {
        if(curBossRoom==null) return;
        List<Map.Entry<Integer, Long>> sortedList = new ArrayList<>(curBossRoom.getBoss().getBeDameInfo().entrySet());
        sortedList.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        for (int i = 0; i < sortedList.size(); i++) {
            if (sortedList.get(i).getValue() < 0) continue;
            int coin = CfgClan.config.coinTopDameBoss.get(Math.min(i, CfgClan.config.coinTopDameBoss.size() - 1)) + bossLevel * 2;
            int gem = CfgClan.config.gemTopDameBoss.get(Math.min(i, CfgClan.config.gemTopDameBoss.size() - 1)) + bossLevel * 2;
            List<Long> bonus = Bonus.viewGem(gem);
            bonus.addAll(Bonus.viewItem(ItemKey.HUY_HIEU_BANG, coin));
            int userId = sortedList.get(i).getKey();
            MyUser user = Online.getMUser(userId);
            String sql = DBHelper.sqlMail(userId, String.format(Lang.getTitle(user, Lang.mail_clan_boss_top), (i + 1)), StringHelper.toDBString(bonus));
            DBJPA.rawSQL(sql);
            if (user != null && user.getChannel() != null && user.getChannel().isActive())
                user.addNotify(NotifyType.MAIL);
        }

        setCurBossRoom(null);
    }

    public void upLevelBoss(){
        this.bossLevel++;
        this.timeOpenBoss = 0;
        update(List.of("boss_level",bossLevel,"time_open_boss",timeOpenBoss));
    }

    public synchronized boolean openBoss() {
        if(curBossRoom!=null && !DateTime.isAfterTime(curBossRoom.getTimeCreateRoom(), BossClanRoom.getTimeOut()) ) return false;
        honor -= CfgClan.config.feeOpenBoss;
        this.timeOpenBoss =System.currentTimeMillis();
        return update(Arrays.asList( "honor", honor,"time_open_boss",timeOpenBoss));
    }

    public List<ChatObject> getAChat() {
        return chatMan.getAChat();
    }

    public synchronized List<CellDynamic> getDynamics() {
        if (dynamic == null) dynamic = "[]";
        if (dynamics == null) dynamics = new Gson().fromJson(dynamic, new TypeToken<List<CellDynamic>>() {
        }.getType());
        return dynamics;
    }


    public void checkDynamic(MyUser mUser, int type, int curValue) {
        // todo check quest attack boss
        if (type == CfgClan.DYNAMIC_QUEST_D_100) {
            DataDaily dataDaily = mUser.getUserDaily().getUDaily();
            ResDynamicTypeEntity rType = ResClan.aDynamicType.get(2);
            if (curValue >= rType.getNumber() && dataDaily.getValue(DataDaily.SEND_100_NANG_DONG) == 0) {
                dataDaily.setValue(DataDaily.SEND_100_NANG_DONG, 1);
                if (dataDaily.update()) {
                    addDynamic(type, mUser.getUser().getName());
                }
            }
        } else {
            addDynamic(type, mUser.getUser().getName());
        }

    }

    public synchronized void addDynamic(int type, String name) {
        List<CellDynamic> lstDy = getDynamics();
        List<Integer> dynamicId = getDynamicId();
        dynamicId.set(1, dynamicId.get(1) + 1);
        pointDynamic++;
        lstDy.add(new CellDynamic(dynamicId.get(1), name, type));
        update(List.of("dynamic_id", StringHelper.toDBString(dynamicId), "dynamic", StringHelper.toDBString(lstDy), "point_dynamic", pointDynamic));
    }

    public List<ClanReqEntity> getAReq() {
        synchronized (lockObj) {
            if (aReq == null) {
                aReq = DBJPA.getList("clan_req", Arrays.asList("clan_id", id), "order by date_created desc", ClanReqEntity.class);
            }
            List<Integer> removeIds = new ArrayList<>();
            Calendar ca = Calendar.getInstance();
            ca.add(Calendar.DATE, -1);
            for (int i = aReq.size() - 1; i >= 0; i--) {
                if (aReq.get(i).getDateCreated().before(ca.getTime())) {
                    removeIds.add(aReq.get(i).getUserId());
                    aReq.remove(i);
                } else break;
            }
            if (!removeIds.isEmpty()) {
                String strIds = removeIds.stream().map(value -> String.valueOf(value)).collect(Collectors.joining(","));
                DBJPA.rawSQL(String.format("delete from clan_req where clan_id=%s and user_id in (%s)", id, strIds));
            }
        }
        return aReq;
    }

    public synchronized boolean addExp(int addExp) {
        exp += addExp;
        honor -= addExp;
        int maxExp = CfgClan.getMaxExp(level);
        while (level < CfgClan.config.exp.size() + 1 && exp >= maxExp) {
            exp -= maxExp;
            level++;
        }
        return update(Arrays.asList("level", level, "exp", exp, "honor", honor));
    }

    public synchronized boolean addHonor(int addHonor) {
        honor += addHonor;
        return update(Arrays.asList("honor", honor));
    }

    public synchronized void addContribute(int addContribute) {
        if (levelQuest >= ResClan.maxLevelContribute) return;
        ResContributeEntity res = ResClan.getClanContribute(levelQuest);
        contribute += addContribute;
        while (levelQuest < ResClan.maxLevelContribute && contribute >= res.getGold()) {
            contribute -= res.getGold();
            levelQuest++;
        }
        update(List.of("level_quest", levelQuest, "contribute", contribute));
    }

    public Pbmethod.PbClan toProto(int... rank) {
        Pbmethod.PbClan.Builder pbClan = Pbmethod.PbClan.newBuilder();
        pbClan.setId(id);
        pbClan.setServer(server);
        pbClan.setAvatar(avatar);
        pbClan.setName(name);
        pbClan.setMasterId(masterId);
        pbClan.setMasterName(master);
        pbClan.setRank(this.rank);
        ClanManager clanManager = ClanManager.getInstance(id);
        if (clanManager != null && clanManager.getClan() != null) clanManager.getClan().setRank(this.rank);
        pbClan.setPower(power);
        pbClan.setJoinRule(joinRule);
        pbClan.setIntro(intro);
        pbClan.setNumberMember(member);
        pbClan.setMaxMember(CfgClan.getMaxMember(level));
        pbClan.setLevel(level);
        pbClan.setExp(exp);
        List<Integer> info = getInfoAttackBoss();
        pbClan.setJoinTrophy(CfgClan.NUM_ATTACK_BOSS - info.get(1));
        // rank
        if (rank != null && rank.length > 0) {
            this.rank = rank[0];
            int typeRank = rank.length > 1 ? rank[1] : 0;
            TopType topType = TopType.get(typeRank);
            if (topType == TopType.CLAN_POWER) pbClan.setPointRank(power);
            if (topType == TopType.CLAN_STAR) pbClan.setPointRank(star);
            pbClan.setRank(this.rank);
        }
        return pbClan.build();
    }

    public List<Integer> getMemberId() {
        List<Integer> memberIds = GsonUtil.strToListInt(memberId);
        if (memberIds.isEmpty()) {
            List<UserEntity> aUser = Services.clanDAO.getListMember(id);
            if (aUser != null) aUser.forEach(u -> memberIds.add(u.getId()));
            if (update(List.of("member_id", StringHelper.toDBString(memberIds)))) {
                memberId = memberIds.toString();
            }
        }
        return memberIds;
    }

    public Pbmethod.PbClan.Builder protoClan(Lang lang, int... myClan) { //. my clan = 1
        Pbmethod.PbClan.Builder builder = Pbmethod.PbClan.newBuilder();
        if (myClan.length > 0 && myClan[0] == 1) {
            JsonArray arr = GsonUtil.parseJsonArray(activityLog);
            for (int i = arr.size() - 1; i >= 0; i--) {
                String value = arr.get(i).getAsString();
                if (value.contains("_")) {
                    String message = value.substring(value.indexOf("_") + 1);
                    builder.addActivityLog(value.substring(0, value.indexOf("_") + 1) + lang.formatMessage(message));
                } else builder.addActivityLog(value);
            }
        }

        List<UserEntity> aUser = Services.clanDAO.getListMember(id);
        {
            UserEntity uLeader = aUser.stream().filter(user -> user.getClanPosition() == ClanPosition.LEADER.value).findFirst().orElse(null);
            if (uLeader == null || System.currentTimeMillis() - uLeader.getLastAction() > DateTime.DAY_MILLI_SECOND * 5) { // Không hoạt động trên 5 ngày
                UserEntity uCoLeader = aUser.stream().filter(user -> user.getClanPosition() == ClanPosition.CO_LEADER.value && System.currentTimeMillis() - user.getLastAction() < DateTime.DAY_MILLI_SECOND * 5).findFirst().orElse(null);
                UserEntity uElder = aUser.stream().filter(user -> user.getClanPosition() == ClanPosition.ELDER.value && System.currentTimeMillis() - user.getLastAction() < DateTime.DAY_MILLI_SECOND * 5).findFirst().orElse(null);
                UserEntity uMember = aUser.stream().filter(user -> user.getClanPosition() == ClanPosition.MEMBER.value && System.currentTimeMillis() - user.getLastAction() < DateTime.DAY_MILLI_SECOND * 5).findFirst().orElse(null);

                UserEntity changePosition = uCoLeader;
                if (changePosition == null) changePosition = uElder;
                if (changePosition == null) changePosition = uMember;

                if (changePosition != null && Services.clanDAO.promote(uLeader == null ? 0 : uLeader.getId(), ClanPosition.CO_LEADER.value, changePosition, ClanPosition.LEADER.value)) {
                    if (uLeader != null) uLeader.setClanPosition(ClanPosition.CO_LEADER.value);
                    String leaderName = uLeader == null ? "" : uLeader.getName();
                    changePosition.setClanPosition(ClanPosition.LEADER.value);
                    addClanLog(Lang.clan_message_11, changePosition.getName(), Lang.toKey(ClanPosition.getKey(changePosition.getClanPosition())), leaderName);
                    ChatObject chat = new ChatObject(changePosition, String.format(lang.get(Lang.clan_message_11), changePosition.getName(), ClanPosition.getName(lang, changePosition.getClanPosition()), leaderName));
                    addChat(changePosition, chat);
                }
            }
        }
        for (UserEntity user : aUser) {
            builder.addMember(user.protoClanMember());
        }

        // Update average power top 10
        if (aUser.size() >= 10) {
            aUser.sort(Comparator.comparing(UserEntity::getPower).reversed());
            int totalPower = 0;
            for (int i = 0; i < 10; i++) totalPower += (int) aUser.get(i).getPower();
            update(Arrays.asList("power", totalPower));
        }

        if (member != aUser.size()) {
            Logs.warn(String.format("fixMember %s %s %s", id, member, aUser.size()));
            setMember(aUser.size());
            update(Arrays.asList("`member`", aUser.size()));
        }

        builder.setId(id).setName(name);
        builder.setMasterName(master);
        builder.setNumberMember(member);
        builder.setMaxMember(CfgClan.getMaxMember(level));
        builder.setJoinRule(joinRule);
        List<Integer> info = getInfoAttackBoss();
        builder.setJoinTrophy(CfgClan.NUM_ATTACK_BOSS - info.get(1));
        builder.setLevel(level).setExp(level == CfgClan.config.exp.size() + 1 ? 0 : exp).setMaxExp(CfgClan.getMaxExp(level));
        builder.setAvatar(avatar);
        builder.setIntro(intro);
        builder.setStar((int) honor);
        return builder;
    }

    public void addChat(UserEntity user, ChatObject chat) {
        if (chatMan != null) chatMan.addChat(chat);
    }

    public synchronized void kick(AHandler handler, String kickName, UserEntity user) {
        if (Services.clanDAO.removeMember(this, user, true) == 0) {
            addMember(-1, user.getId());
            handler.addResponse(null);
            try {
                MyUser _tmp = Online.getMUser(user.getId());
                if (_tmp != null) {
                    UserEntity _u = _tmp.getUser();
                    _u.setClan(0);
                    _u.setClanName("");
                    _u.setClanPosition(0);
                    _u.setClanAvatar(0);
                }
            } catch (Exception ex) {
            }
            addClanLog(Lang.clan_message_2, kickName, user.getName());
            Actions.save(user, Actions.GCLAN, Actions.DKICK, "id", id, "userId", user.getId(), "member", member);

            Integer numberKick = JCache.getInstance().getIntValue(DateTime.getDateyyyyMMdd(Calendar.getInstance().getTime()) + "clan" + id);
            if (numberKick == null) numberKick = 1;
            else numberKick++;
            JCache.getInstance().setValue(DateTime.getDateyyyyMMdd(Calendar.getInstance().getTime()) + "clan" + id, numberKick.toString());
        } else {
            handler.addErrResponse();
        }
    }

    public synchronized String setPosition(MyUser mUser, int promoteId, int newPosition) {
        UserEntity promoteUser = getUser(promoteId);
        if (promoteUser == null) {
            return Lang.instance(mUser).get(Lang.clan_message_5);
        }
        if (mUser.getUser().getClanPosition() <= promoteUser.getClanPosition()) {
            return Lang.instance(mUser).get(Lang.clan_message_6);
        }
        if (mUser.getUser().getClanPosition() < newPosition) {
            return Lang.instance(mUser).get(Lang.clan_message_7);
        }
        if (newPosition == ClanPosition.LEADER.value && System.currentTimeMillis() - promoteUser.getLastAction() >= DateTime.DAY_MILLI_SECOND * 5) {
            return Lang.instance(mUser).get(Lang.clan_message_8);
        }
        int myPosition = newPosition == ClanPosition.LEADER.value ? ClanPosition.CO_LEADER.value : mUser.getUser().getClanPosition();
        if (Services.clanDAO.promote(mUser.getUser().getId(), myPosition, promoteUser, newPosition)) {
            int oldPosition = promoteUser.getClanPosition();
            promoteUser.setClanPosition(newPosition);
            addPositionMsg(mUser.getUser().getName(), promoteUser, newPosition > oldPosition);
            if (myPosition > -1) {
                mUser.getUser().setClanPosition(myPosition);
                addPositionMsg(mUser.getUser().getName(), mUser.getUser(), false);
            }
            if (ClanPosition.isLeader(newPosition)) {
                masterId = promoteUser.getId();
                master = promoteUser.getName();
            }
            Actions.save(mUser.getUser(), Actions.GCLAN, "position", "clanId", String.valueOf(id), "oldPos", String.valueOf(oldPosition), "newPos", String.valueOf(newPosition), "userId", String.valueOf(promoteId));
            MyUser mPromoteUser = Online.getMUser(promoteUser.getId());
            if (mPromoteUser != null) mPromoteUser.getUser().setClanPosition(newPosition);
            return "";
        }
        return Lang.instance(mUser).get(Lang.err_system_down);
    }

    void addPositionMsg(String name, UserEntity user, boolean isPromote) {
        if (isPromote) {
            addClanLog(Lang.clan_message_9, user.getName(), Lang.toKey(ClanPosition.getKey(user.getClanPosition())), name);
        } else {
            addClanLog(Lang.clan_message_10, user.getName(), Lang.toKey(ClanPosition.getKey(user.getClanPosition())), name);
        }
    }

    UserEntity getUser(int userId) {
        return Online.getDbUser(userId);
    }

    public void deleteRequest(int userId) {
        removeRequest(userId);
        DBJPA.delete("clan_req", "clan_id", id, "user_id", userId);
    }

    //public void addChat(ChatObject chatObj) {
    //    if (chatMan != null) chatMan.addChat(chatObj);
    //}

    public void removeRequest(int userId) {
        List<ClanReqEntity> aReq = getAReq();
        synchronized (lockObj) {
            for (int i = aReq.size() - 1; i >= 0; i--) {
                if (aReq.get(i).getUserId() == userId) aReq.remove(i);
            }
        }
    }

    public synchronized void joinUser(UserEntity user) {
        // get session from username
        addMember(1, user.getId());
        removeRequest(user.getId());
        addClanLog(Lang.clan_message_3, user.getName());
        MyUser _tmp = Online.getMUser(user.getId());
        if (_tmp != null) {
            UserEntity _u = _tmp.getUser();
            if (_u.getServer() == server) {
                _u.setClanPosition(0);
                _u.setClan(id);
                _u.setClanName(name);
                _u.setClanAvatar(avatar);
            }
        }
        Actions.save(user, Actions.GCLAN, Actions.DAREQ, "id", id, "userId", user.getId(), "member", member);
    }

    public void addClanLog(String... params) {
        JSONArray arr = JSONArray.fromObject(activityLog);
        arr.add(String.format("%s_%s", System.currentTimeMillis(), GsonUtil.toJson(params)));
        if (arr.size() > 10) {
            arr.remove(0);
        }
        activityLog = StringHelper.toDBString(arr);
        if (id > 0) update(Arrays.asList("activity_log", activityLog));
    }

    synchronized void addMember(int value, int userId) {
        member += value;
        List<Integer> memberIds = getMemberId();
        if (!memberIds.contains(userId)) memberIds.add(userId);
        memberId = memberIds.toString();
        dbUpdateNumberMember(memberIds);
    }

    boolean update(List<Object> data) {
        List<Object> ret = new ArrayList<>(data);
        ret.add("rank");
        ret.add(rank);
        return DBJPA.update("clan", ret, Arrays.asList("id", String.valueOf(id)));
    }

    void dbUpdateNumberMember(List<Integer> members) {
        update(Arrays.asList("member", String.valueOf(member), "member_id", StringHelper.toDBString(members)));
    }

    public synchronized void leaveClan(AHandler handler, UserEntity user) {
        int result = Services.clanDAO.removeMember(this, user, false);
        if (result != 0) {
            handler.addErrResponse();
            return;
        }
        addMember(-1, user.getId());
        handler.addResponse(null);
        // addChat(user, ChatObject.MSG_RED, CfgMsgTemplate.tplClanLeave(user.getName()));
        addClanLog(Lang.clan_message_4, user.getName());
        Actions.save(user, Actions.GCLAN, Actions.DLEAVE, "id", id, "member", member);
        if (member == 0) {
            DBJPA.rawSQL("delete from clan where id=" + id, "update user set clan=0, clan_name='', clan_position='' where clan=" + id);
            Actions.save(user, Actions.GCLAN, Actions.DDESTROY, "id", id, "member", "0");
            ClanManager.getInstance(user.getClan()).destroyClan(user);
        }
    }
}
