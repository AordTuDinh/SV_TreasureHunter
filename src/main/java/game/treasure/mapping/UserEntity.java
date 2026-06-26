package game.treasure.mapping;

import game.battle.calculate.IMath;
import game.battle.object.Point;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.treasure.service.resource.ResEvent;
import game.treasure.service.resource.ResParty;
import game.treasure.mapping.UserSkinEntity;
import game.monitor.ClanManager;
import game.monitor.Online;
import game.object.MyUser;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.*;

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
    String name, username, gameChannel, version, packBuy, lang;
    int clan, clanAvatar, clanRank, mainId, clanPosition;
    String clanName, pointData;
    String itemEquipment; // id - key - level
    int server, vip, vipExp, userRank;
    long gold, gem, ruby, power;
    int numberFriend, rr, party;
    int blockType;
    int numDayLogin;
    long lastAction;
    Date lastLogin, dateCreated, lockChat;
    Date clanJoin;
    String skins, pet; // HAIR, FACE, EYE, BODY by SkinType index
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
        this.gold = 0;
        this.skins = "[0,0,0,0,0,0,0,0]";
        this.clan = 0;
        this.clanName = "";
        this.power = 0;
        this.gem = 100;
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
        builder.addAllSkins(getSkins());
        builder.addAllVip(getVipInfo());
        builder.setRank(userRank);
        builder.addAllChannel(Online.getUserChannelInfo(id));
        List<Integer> items = new ArrayList<>(mUser.getUser().getAllInfoItemEquip());
        while (items.size() < EQUIP_LIST_SIZE)
            items.add(0);
        int treasureIdx = equipSlotIndex(protocol.Pbmethod.EquipSlotType.TREASURE.getNumber());
        boolean update = false;
        for (int i = 0; i < items.size(); i += EQUIP_FIELDS_PER_SLOT) {
            int rowId = items.get(i);
            if (rowId <= 0)
                continue;
            if (i == treasureIdx) {
                if (mUser.getResources().getArtifact(rowId) == null) {
                    items.set(i, 0);
                    items.set(i + 1, 0);
                    items.set(i + 2, 0);
                    update = true;
                }
                continue;
            }
            UserEquipmentEntity item = mUser.getResources().getItemEquipment(rowId);
            if (item == null) {
                items.set(i, 0);
                items.set(i + 1, 0);
                items.set(i + 2, 0);
                update = true;
            }
        }
        if (update)
            updateItemEquip(items);
        // point
        builder.addAllPoint(StringHelper.isEmpty(name) ? new Point().toProto() : mUser.getPlayer().getPoint().toProto());
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
        if (packBuy == null || packBuy.isEmpty()) packBuy = "[]";
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

    public static final int EQUIP_SLOT_COUNT = 8;
    public static final int EQUIP_FIELDS_PER_SLOT = 3;
    public static final int EQUIP_LIST_SIZE = EQUIP_SLOT_COUNT * EQUIP_FIELDS_PER_SLOT;

    public static int equipSlotIndex(int equipSlotType) {
        if (equipSlotType < 1 || equipSlotType > EQUIP_SLOT_COUNT) return -1;
        return (equipSlotType - 1) * EQUIP_FIELDS_PER_SLOT;
    }

    public static int findEquipSlotByItemId(List<Integer> lst, int itemId) {
        if (lst == null || itemId <= 0) return -1;
        for (int i = 0; i < EQUIP_LIST_SIZE; i += EQUIP_FIELDS_PER_SLOT) {
            if (i < lst.size() && lst.get(i) == itemId) return i;
        }
        return -1;
    }

    public List<Integer> normalizeItemEquipList() {
        List<Integer> lst = new ArrayList<>(getAllInfoItemEquip());
        while (lst.size() < EQUIP_LIST_SIZE) lst.add(0);
        return lst;
    }

    /** 8 itemKey theo thứ tự EquipSlotType (WEAPON..MOUNT). */
    public List<Integer> getListItemKeyEquip() {
        List<Integer> lst = normalizeItemEquipList();
        List<Integer> ret = new ArrayList<>(EQUIP_SLOT_COUNT);
        for (int i = 0; i < EQUIP_SLOT_COUNT; i++) {
            ret.add(lst.get(i * EQUIP_FIELDS_PER_SLOT + 1));
        }
        return ret;
    }

    public List<Long> getListItemKeyEquipLong() {
        List<Integer> keys = getListItemKeyEquip();
        List<Long> ret = new ArrayList<>(keys.size());
        for (int key : keys) ret.add((long) key);
        return ret;
    }

    public UserPartyEntity getParty() {
        return ResParty.getParty(party);
    }

    public Point reCalculatePoint(MyUser mUser) {
        // tính lại point thì set lại def team arena, nhưng sau 3p ms set db
        if (DateTime.isAfterTime(lastUpdateDefTeam, DateTime.MIN_SECOND * 3)) {
            lastUpdateDefTeam = Calendar.getInstance().getTime().getTime();
        }
        return calculatePoint(mUser);
    }

    Point calculatePoint(MyUser mUser) {
        Point point = mUser.getPlayer().getPoint();
        long cacheHp = point.getCurHP();
        point = IMath.calculatePoint(mUser, true);
        game.treasure.service.user.UserBuff.applyActiveToPoint(mUser, point);
        point.setCurHp(Math.min(cacheHp, point.getMaxHp()));
        mUser.getPlayer().setPoint(point);
        //todo tính thêm chỉ số của thẻ monster
        return point;
    }

    public Point getInitPoint(MyUser mUser) { // chỉ lấy từ lúc init player
        Point point = getCachePoint();
        // lấy lại cache hp và mp   
        long cacheHp = point.getCurHP();
        // tính lại point
        point = IMath.calculatePoint(mUser, true);
        game.treasure.service.user.UserBuff.applyActiveToPoint(mUser, point);
        point.setCurHp(cacheHp <= 0 ? point.getMaxHp() : cacheHp);
        return point;
    }

    public Point getCachePoint() {
        if (pointData != null) {
            return new Point(GsonUtil.strToListInt(pointData));
        } else return new Point();
    }


    public List<Integer> getPet(MyUser mUser) {
        if (pet == null || pet.isEmpty()) pet = "[0,0]";
        List<Integer> ret = GsonUtil.strToListInt(pet);
        if (ret.get(0) != 0) {
            return NumberUtil.genListInt(2, 0);
        } else return ret;
    }

    public long getPower() {
        Point point = getCachePoint();
        if (point.getValues().length == 0) return 0;
        long newPower = point.getPower();
        if (newPower != power) {
            if (updatePower(newPower, point)) {
                return newPower;
            }
        }
        return power;
    }


    public protocol.Pbmethod.PbUser toProto() {
        protocol.Pbmethod.PbUser.Builder pb = protocol.Pbmethod.PbUser.newBuilder();
        pb.setId(id);
        pb.setUsername(username);
        pb.setName(getName());
        pb.setGold(gold);
        pb.setGem(gem);
        pb.addAllSkins(getSkins());
        pb.addAllVip(getVipInfo());
        pb.setRank(userRank);
        pb.setPower(getPower());
        pb.addAllPoint(getCachePoint().toProto());
        pb.addAllPet(GsonUtil.strToListInt(pet));
        pb.setTimeLastAction(getTimeLastAction());
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
        builder.setPower(getPower());
        builder.setRank(rank.length > 0 ? rank[0] : 0);
        builder.addAllSkins(getSkins());
        builder.addAllItemEquip(getAllInfoItemEquip());
        builder.addAllPet(GsonUtil.strToListInt(pet));
        checkRank();
        builder.setClanInfo(protocol.Pbmethod.CommonVector.newBuilder().addAString(clanName).addALong(clan).addALong(clanPosition).addALong(clanRank).addALong(clanAvatar).build());
        return builder;
    }


    public protocol.Pbmethod.ClanMember.Builder protoClanMember() {
        protocol.Pbmethod.ClanMember.Builder member = protocol.Pbmethod.ClanMember.newBuilder();
        member.setPosition(clanPosition);
        member.setId(id).setName(getName());
        member.addAllSkins(getSkins());
        member.setLevel(1);
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

    public List<Integer> getSkins() {
        return UserSkinEntity.normalize(GsonUtil.strToListInt(this.skins));
    }

    public int getBodySkinId() {
        return UserSkinEntity.getBodyId(getSkins());
    }

    public boolean updateSkins(List<Integer> skinList) {
        List<Integer> normalized = UserSkinEntity.normalize(skinList);
        String dbValue = StringHelper.toDBString(normalized.subList(0, UserSkinEntity.EQUIPPED_SIZE));
        if (update(Arrays.asList("skins", dbValue))) {
            this.skins = dbValue;
            return true;
        }
        return false;
    }

    public boolean updateSkin(protocol.Pbmethod.SkinType part, long userSkinId, int resSkinId) {
        List<Integer> list = getSkins();
        UserSkinEntity.setEquipped(list, part, userSkinId, resSkinId);
        return updateSkins(list);
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
    public boolean updateCreateUser(String name) {
        if (update(Arrays.asList("name", name))) {
            this.name = name;
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
        String dbValue = StringHelper.toDBString(items);
        if (update(Arrays.asList("item_equipment", dbValue))) {
            this.itemEquipment = dbValue;
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
        this.setLastAction(time);
        return DBJPA.update("user", obj, Arrays.asList("id", id));
    }


    // endregion
}
