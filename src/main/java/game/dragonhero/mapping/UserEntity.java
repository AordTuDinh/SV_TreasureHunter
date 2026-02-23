package game.dragonhero.mapping;

import game.battle.calculate.IMath;
import game.battle.object.Point;
import game.config.*;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.dragonhero.BattleConfig;
import game.dragonhero.mapping.main.ResTutorialQuestEntity;
import game.dragonhero.server.IAction;
import game.dragonhero.service.resource.ResEvent;
import game.dragonhero.service.resource.ResParty;
import game.dragonhero.service.resource.ResQuest;
import game.monitor.ClanManager;
import game.monitor.Online;
import game.object.MyUser;
import game.protocol.CommonProto;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.*;
import protocol.Pbmethod;

import javax.persistence.*;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

@Entity
@Data
@Table(name = "user")
@NoArgsConstructor
public class UserEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    String name, username, intro, gameChannel, version, packBuy, lang;
    int clan, clanAvatar, clanRank, mainId, clanPosition;
    String clanName, pointData;
    String weapon; // id - level
    String itemEquipment; // id - key - level
    int level, server, vip, vipExp, userRank;
    long gold, exp, gem, ruby, power;
    int numberFriend, rr, party;
    int blockType;
    int numDayLogin;
    long lastAction;
    Date lastLogin, dateCreated, lockChat;
    Date clanJoin;
    String avatar, pet; // type-  avatarId - heroId - frame - skin
    @Transient
    long lastChatMap, lastChatServer, lastUpdateDefTeam;
    @Transient
    String lastMsgChatMap, lastMsgChatServer;

    public UserEntity(String userName, String name, int server, String gameChannel, int mainId, String version) {
        this.server = server;
        this.username = userName;
        this.name = name;
        this.mainId = mainId;
        this.gameChannel = gameChannel;
        this.version = version;
        this.level = 1;
        this.gold = 0;
        this.avatar = "[0,1,0,0,0]";// type-  avatarId - heroId - frame - skin
        this.weapon = "[1,1,2,1,3,1,4,1,5,1]";
        this.clan = 0;
        this.clanName = "";
        this.power = 0;
        this.gem = 100;
        this.exp = 0;
        this.intro = "";
        this.vip = 0;
        this.userRank = 0;
        this.lastLogin = Calendar.getInstance().getTime();
        this.itemEquipment = NumberUtil.genListInt(24, 0).toString();
        this.dateCreated = Calendar.getInstance().getTime();
        this.pet = "[0,0]";
        this.packBuy = "[]";
        this.numDayLogin = 0;
    }

    public protocol.Pbmethod.PbUser toProto(MyUser mUser) {
        protocol.Pbmethod.PbUser.Builder builder = protocol.Pbmethod.PbUser.newBuilder();
        builder.setId(id);
        builder.setUsername(username);
        builder.setName(getName());
        builder.setGold(gold);
        builder.setRuby(ruby);
        builder.setGem(gem);
        builder.setLevel(level).setExp(exp);
        builder.addAllAvatar(getAvatar());
        builder.addAllVip(getVipInfo());
        builder.setDesc(intro);
        builder.setRank(userRank);
        builder.addAllChannel(Online.getUserChannelInfo(id));
        List<Integer> items = mUser.getUser().getAllInfoItemEquip();
        boolean update = false;
        for (int i = 0; i < items.size(); i += 3) {
            UserItemEquipmentEntity item = mUser.getResources().getItemEquipment(items.get(i));
            if (item == null || !item.hasExpire()) { // check hết hạn thì xóa khỏi equip
                items.set(i, 0);
                update = true;
            }
        }
        if (update) updateItemEquip(items);
        // point
        builder.addAllPoint(StringHelper.isEmpty(name) ? new Point().toProto() : mUser.getPlayer().getPoint().toProto());
        // list weapon
        mUser.getResources().getMWeapon().forEach((k, v) -> builder.addWeapons(v.toProto(this, mUser.getPlayer().getPoint())));
        // weapon equip
        builder.addAllWeaponEquip(GsonUtil.strToListInt(weapon));
        builder.addAllItemEquip(getAllInfoItemEquip());
        // caculator data
        builder.setHonor(0);
        builder.addAllPet(getPet(mUser));
        checkRank();
        builder.setClanInfo(protocol.Pbmethod.CommonVector.newBuilder().addAString(clanName).addALong(clan).addALong(clanPosition).addALong(clanRank).addALong(clanAvatar).build());
        return builder.build();
    }

    public List<Integer> getVipInfo() {
        return Arrays.asList(vip, vipExp);
    }

    public List<Integer> getListPackBuy() {
        if(packBuy==null || packBuy.isEmpty()) packBuy="[]";
        return GsonUtil.strToListInt(packBuy);
    }


    public List<Integer> getListIdEquipmentEquip() { // only id
        List<Integer> lst = GsonUtil.strToListInt(itemEquipment);
        List<Integer> ret = new ArrayList<>();
        for (int i = 0; i < lst.size(); i += 3) {
            ret.add(lst.get(i));
        }
        return ret;
    }

    public List<Integer> getAllInfoItemEquip() {
        return GsonUtil.strToListInt(itemEquipment);
    }

    public UserPartyEntity getParty() {
        return ResParty.getParty(party);
    }

    public List<Integer> getWeaponEquipId() {
        List<Integer> lst = GsonUtil.strToListInt(weapon);
        List<Integer> ret = new ArrayList<>();
        for (int i = 0; i < lst.size(); i += 2) {
            ret.add(lst.get(i));
        }
        return ret;
    }

    public Point reCalculatePoint(MyUser mUser) {
        // tính lại point thì set lại def team arena, nhưng sau 3p ms set db
        if (DateTime.isAfterTime(lastUpdateDefTeam, DateTime.MIN_SECOND * 3)) {
            lastUpdateDefTeam = Calendar.getInstance().getTime().getTime();
            CfgArena.reCalDefTeamArena(mUser, true);
        } else CfgArena.reCalDefTeamArena(mUser, false);
        return calculatePoint(mUser);
    }

    Point calculatePoint(MyUser mUser) {
        Point point = mUser.getPlayer().getPoint();
        long cacheHp = point.getCurHP();
        long cacheMp = point.getCurMP();
        point = IMath.calculatePoint(mUser, true);
        point.setCurHp(Math.min(cacheHp, point.getMaxHp()));
        point.setCurMp(Math.min(cacheMp, point.getMaxMp()));
        mUser.getPlayer().setPoint(point);
        //todo tính thêm chỉ số của thẻ monster
        return point;
    }

    public Point getInitPoint(MyUser mUser) { // chỉ lấy từ lúc init player
        Point point = getCachePoint();
        // lấy lại cache hp và mp
        long cacheHp = point.getCurHP();
        long cacheMp = point.getCurMP();
        // tính lại point
        point = IMath.calculatePoint(mUser, true);

        point.setWeight(BattleConfig.P_Weight);
        point.setCurHp(cacheHp <= 0 ? point.getMaxHp() : cacheHp);
        point.setCurMp(cacheMp <= 0 ? point.getMaxMp() : cacheMp);
        return point;
    }

    public Point getCachePoint() {
        if (pointData != null) {
            return new Point(GsonUtil.strToListLong(pointData));
        } else return new Point();
    }


    public List<Integer> getPet(MyUser mUser) {
        if (pet == null || pet.isEmpty()) pet = "[0,0]";
        List<Integer> ret = GsonUtil.strToListInt(pet);
        if (ret.get(0) != 0) {
            UserPetEntity uPet = mUser.getResources().getPet(PetType.ANIMAL, ret.get(0));
            if (uPet != null && uPet.getHp() > 0) return ret;
            else return NumberUtil.genListInt(2, 0);
        } else return ret;
    }

    public long getPower() {
        Point point = getCachePoint();
        if (point.getValues().length == 0) return 0;
        long newPower = point.getValues()[Point.POWER];
        if (newPower != power) {
            if (updatePower(newPower, point)) {
                return newPower;
            }
        }
        return power;
    }

    public protocol.Pbmethod.PbUser toProto() {
        return toProto(0);
    }

    public protocol.Pbmethod.PbUser toProto(int arenaPoint) {
        protocol.Pbmethod.PbUser.Builder pb = protocol.Pbmethod.PbUser.newBuilder();
        pb.setId(id);
        pb.setUsername(username);
        pb.setName(getName());
        pb.setGold(gold);
        pb.setGem(gem);
        pb.setLevel(level).setExp(exp);
        pb.addAllAvatar(getAvatar());
        pb.addAllVip(getVipInfo());
        pb.setDesc(intro);
        pb.setRank(userRank);
        pb.setPointRank(arenaPoint);
        pb.setPower(getPower());
        pb.addAllPoint(getCachePoint().toProto());
        pb.addAllPet(GsonUtil.strToListInt(pet));
        pb.setTimeLastAction(getTimeLastAction());
        pb.addAllWeaponEquip(GsonUtil.strToListInt(weapon));
        pb.addAllItemEquip(getAllInfoItemEquip());
        pb.addAllChannel(Online.getUserChannelInfo(id));
        // caculator data
        checkRank();
        pb.setClanInfo(protocol.Pbmethod.CommonVector.newBuilder().addAString(clanName).addALong(clan).addALong(clanPosition).addALong(clanRank).addALong(clanAvatar).build());
        return pb.build();
    }

    public protocol.Pbmethod.PbUser.Builder protoTinyUser(int... rank) {
        protocol.Pbmethod.PbUser.Builder builder = protocol.Pbmethod.PbUser.newBuilder();
        builder.setId(id);
        builder.setName(getName());
        builder.setLevel(level);
        builder.setPower(getPower());
        builder.setRank(rank.length > 0 ? rank[0] : 0);
        builder.addAllAvatar(getAvatar());
        builder.addAllItemEquip(getAllInfoItemEquip());
        builder.addAllPet(GsonUtil.strToListInt(pet));
        builder.addAllWeaponEquip(GsonUtil.strToListInt(weapon));
        checkRank();
        builder.setClanInfo(protocol.Pbmethod.CommonVector.newBuilder().addAString(clanName).addALong(clan).addALong(clanPosition).addALong(clanRank).addALong(clanAvatar).build());
        return builder;
    }

    public Pbmethod.PbBattleArenaUserInfo toProtoArenaInfo(int point) {
        Pbmethod.PbBattleArenaUserInfo.Builder pb = Pbmethod.PbBattleArenaUserInfo.newBuilder();
        pb.addAllAvatar(getAvatar());
        pb.setLevel(level);
        pb.setVip(vip);
        pb.setName(name);
        pb.setRankPoint(point);
        return pb.build();
    }


    public protocol.Pbmethod.ClanMember.Builder protoClanMember() {
        protocol.Pbmethod.ClanMember.Builder member = protocol.Pbmethod.ClanMember.newBuilder();
        member.setPosition(clanPosition);
        member.setId(id).setName(getName());
        member.addAllAvatar(getAvatar());
        member.setLevel(level);
        member.setClanDonated(0);
        member.setOnline(Online.isOnline(id));
        member.setIsNew(false);
        long seconds = getTimeLastAction();
        member.setLastAction(seconds > 60 * 5 ? seconds : -1);
        return member;
    }


    void checkRank() {
        if (clan > 0) {
            ClanManager clanManager = ClanManager.getInstance(clan);
            if (clanManager != null) {
                int rankClan = ClanManager.getInstance(clan).getClan().getRank();
                if (rankClan != clanRank && update(Arrays.asList("clan_rank", rankClan))) {
                    clanRank = rankClan;
                }
            }
        }
    }

    public void checkRankPower(int curRank) {
        if (userRank != curRank && update(Arrays.asList("user_rank", curRank))) {
            this.userRank = curRank;
        }
    }

    public long getTimeLastLogin() {
        return (System.currentTimeMillis() - lastLogin.getTime()) / 1000;
    }

    public long getTimeLastAction() {
        return lastAction / 1000;
    }

    public boolean isEquipWeapon(int weaponId) {
        return getWeaponEquipId().contains(weaponId);
    }

    public List<Integer> getAvatar() {
        List<Integer> avatars = GsonUtil.strToListInt(avatar);
        while (avatars.size() < 5) avatars.add(0);
        return avatars;
    }

    public int getHeroMain() {
        List<Integer> avatars = getAvatar();
        return avatars.get(AvatarIndex.HERO.value);
    }

    public String getAvatarString() {
        return avatar;
    }


    public String isLockChat() {
        if (lockChat != null) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            if (lockChat.after(Calendar.getInstance().getTime())) {
                return String.format(Lang.getTitle(lang, Lang.msg_chat_banned_until), df.format(lockChat));
            }
        }
        return null;
    }

    public synchronized void addGem(long value) {
        gem += value;
    }

    public synchronized void addRuby(long value) {
        ruby += value;
    }

    public synchronized void addGold(long value) {
        gold += value;
    }

    public synchronized void addExp(MyUser mUser, long value) {
        int maxLevel = CfgUser.config.exp.size();
        if (level >= maxLevel) return;
        exp += value;
        while (exp >= CfgUser.getExpByLevel(level)) {
            exp -= CfgUser.getExpByLevel(level);  // k phải bằng 0 vì có thể thừa exp
            level++;
            CfgEvent.processTriggerEventTimer(mUser, level, TriggerEventTimer.LEVEL);
            if (level >= maxLevel) {
                level = maxLevel; // set lại những thằng đã quá
                exp = 0;
                break;
            }
            mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.HAS_LEVEL, 1);
            ResTutorialQuestEntity res = ResQuest.mTutQuest.get(mUser.getUData().getQuestTutorial());
            if (res == null) return;
            // check đúng loại  type value k
            if (res.getType() == QuestTutType.HAS_LEVEL) {
                mUser.getUData().setQuestTutorialNumber(level);
                Util.sendProtoData(mUser.getChannel(), CommonProto.getCommonVector(mUser.getUData().getQuestTutorial(), level), IAction.TUTORIAL_QUEST_UPDATE);
            }

        }
    }

    public synchronized void addVipExp(long value) {
        if (vip >= ResEvent.lengthVip) return; // max vip k tăng exp nữa
        vipExp += value;
        int maxExp = ResEvent.getResVip(vip + 1).getExp();
        while (vipExp >= maxExp) {
            vipExp -= maxExp;
            vip++;
            maxExp = ResEvent.getResVip(vip + 1).getExp();
            if (vip >= ResEvent.lengthVip) break;
        }
        if (vip == ResEvent.lengthVip) vipExp = 0; // max level vip thì exp  vip = 0;
    }


    public BlockType getBlockType() {
        return BlockType.get(blockType);
    }

    // region db
    public boolean updateCreateUser(String name, int character) {
        if (update(Arrays.asList("name", name, "avatar", String.format("[0,%s,%s,0,0]", character, character)))) {
            this.name = name;
            this.avatar = "[0," + character + "," + character + ",0,0]";
            return true;
        }
        return false;
    }

    public boolean updateWeaponSlot(int slot, int id, int level) {
        List<Integer> lst = GsonUtil.strToListInt(weapon);
        lst.set((slot - 1) * 2, id);
        lst.set((slot - 1) * 2 + 1, level);
        if (update(Arrays.asList("weapon", StringHelper.toDBString(lst)))) {
            this.weapon = lst.toString();
            return true;
        }
        return false;
    }

    public boolean isOnline() {
        return Online.isOnline(id);
    }


    public boolean updatePower(long newPower, Point point) {
        if (update(Arrays.asList("power", newPower, "point_data", StringHelper.toDBString(point.getValues())))) {
            this.power = newPower;
            return true;
        }
        return false;
    }

    public boolean updatePet(int petId, int petStar) {
        String pets = StringHelper.toDBString(List.of(petId, petStar));
        if (update(Arrays.asList("pet", pets))) {
            this.pet = pets;
            return true;
        }
        return false;
    }

    public boolean updateItemEquip(List<Integer> items) {
        if (update(Arrays.asList("item_equipment", StringHelper.toDBString(items)))) {
            this.itemEquipment = items.toString();
            return true;
        }
        return false;
    }

    public boolean update(List<Object> updateData) {
        List<Object> obj = new ArrayList<>(updateData);
        obj.add("last_action");
        long time = System.currentTimeMillis();
        obj.add(time);
        // tiện thì lưu luôn bọn nhóc nhóc này
        obj.add("gem");
        obj.add(gem);
        obj.add("gold");
        obj.add(gold);
        obj.add("level");
        obj.add(level);
        obj.add("exp");
        obj.add(exp);
        this.setLastAction(time);
        return DBJPA.update("user", obj, Arrays.asList("id", id));
    }


    // endregion
}
