package game.treasure.mapping;

import game.battle.calculate.IMath;
import game.battle.object.Point;
import game.config.CfgQuest;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.object.DataQuest;
import game.treasure.mapping.main.ResVipEntity;
import game.treasure.service.resource.ResEvent;
import game.monitor.ClanManager;
import game.monitor.Online;
import game.object.MyUser;
import game.object.UserResources;
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
    int numberFriend, rr, cup;
    int mobKill;
    int blockType;
    @Transient
    boolean mobKillDirty;
    int numDayLogin;
    long lastAction;
    Date lastLogin, dateCreated, lockChat;
    Date clanJoin;
    String pet;
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
        this.clan = 0;
        this.clanName = "";
        this.power = 0;
        this.gem = 0;
        this.vip = 0;
        this.userRank = 0;
        this.lastLogin = Calendar.getInstance().getTime();
        this.itemEquipment = NumberUtil.genListInt(EQUIP_LIST_SIZE, 0).toString();
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
        builder.setCup(cup);
        builder.setBlockType(blockType);
        builder.addAllVip(getVipInfo());
        builder.setRank(userRank);
        builder.addAllChannel(Online.getUserChannelInfo(id));
        List<Integer> items = new ArrayList<>(mUser.getUser().getAllInfoItemEquip());
        while (items.size() < EQUIP_LIST_SIZE)
            items.add(0);
        int treasureIdx = equipSlotIndex(protocol.Pbmethod.EquipSlotType.TREASURE.getNumber());
        int petIdx = equipSlotIndex(protocol.Pbmethod.EquipSlotType.PET.getNumber());
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
            if (i == petIdx) {
                if (mUser.getResources().getPet(rowId) == null) {
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
        List<Integer> lst = GsonUtil.strToListInt(itemEquipment);
        if (lst.size() == OLD_EQUIP_LIST_SIZE) return migrateOldEquip(lst);
        while (lst.size() < EQUIP_LIST_SIZE) lst.add(0);
        if (lst.size() > EQUIP_LIST_SIZE) return new ArrayList<>(lst.subList(0, EQUIP_LIST_SIZE));
        return lst;
    }

    public static final int EQUIP_FIELDS_PER_SLOT = 3;
    /** 16 slot theo EquipSlotType: vũ khí → vòng tay. Mount không nằm trong list. */
    public static final int[] EQUIP_SLOT_ORDER = {
            protocol.Pbmethod.EquipSlotType.WEAPON.getNumber(),
            protocol.Pbmethod.EquipSlotType.HAT.getNumber(),
            protocol.Pbmethod.EquipSlotType.ARMOR.getNumber(),
            protocol.Pbmethod.EquipSlotType.PANTS.getNumber(),
            protocol.Pbmethod.EquipSlotType.SHOES.getNumber(),
            protocol.Pbmethod.EquipSlotType.CLOAK.getNumber(),
            protocol.Pbmethod.EquipSlotType.GLOVES.getNumber(),
            protocol.Pbmethod.EquipSlotType.PET.getNumber(),
            protocol.Pbmethod.EquipSlotType.TREASURE.getNumber(),
            protocol.Pbmethod.EquipSlotType.BODY.getNumber(),
            protocol.Pbmethod.EquipSlotType.HEAD.getNumber(),
            protocol.Pbmethod.EquipSlotType.HAIR.getNumber(),
            protocol.Pbmethod.EquipSlotType.FACE.getNumber(),
            protocol.Pbmethod.EquipSlotType.ACCESSORY.getNumber(),
            protocol.Pbmethod.EquipSlotType.GLASSES.getNumber(),
            protocol.Pbmethod.EquipSlotType.BRACELET.getNumber()
    };
    public static final int EQUIP_SLOT_COUNT = EQUIP_SLOT_ORDER.length;
    public static final int EQUIP_LIST_SIZE = EQUIP_SLOT_COUNT * EQUIP_FIELDS_PER_SLOT;
    /** Format cũ 8 slot: weapon, hat, armor, cloak, shoes, treasure, pet, mount. */
    private static final int OLD_EQUIP_LIST_SIZE = 24;

    public static int equipSlotIndex(int equipSlotType) {
        for (int i = 0; i < EQUIP_SLOT_ORDER.length; i++) {
            if (EQUIP_SLOT_ORDER[i] == equipSlotType) return i * EQUIP_FIELDS_PER_SLOT;
        }
        return -1;
    }

    /** Cũ: weapon, hat, armor, cloak, shoes, treasure, pet, mount. */
    private static List<Integer> migrateOldEquip(List<Integer> old) {
        List<Integer> neu = new ArrayList<>();
        for (int i = 0; i < EQUIP_LIST_SIZE; i++) neu.add(0);
        copyEquipSlot(old, neu, 0, 0);
        copyEquipSlot(old, neu, 1, 1);
        copyEquipSlot(old, neu, 2, 2);
        copyEquipSlot(old, neu, 4, 4);
        copyEquipSlot(old, neu, 3, 5);
        copyEquipSlot(old, neu, 5, 7);
        copyEquipSlot(old, neu, 6, 8);
        copyEquipSlot(old, neu, 7, 9);
        return neu;
    }

    private static void copyEquipSlot(List<Integer> oldList, List<Integer> newList, int oldSlot, int newSlot) {
        int from = oldSlot * EQUIP_FIELDS_PER_SLOT;
        int to = newSlot * EQUIP_FIELDS_PER_SLOT;
        if (from + 2 >= oldList.size()) return;
        newList.set(to, oldList.get(from));
        newList.set(to + 1, oldList.get(from + 1));
        newList.set(to + 2, oldList.get(from + 2));
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

    /**
     * Wire effect trang bị cho player khác trên map:
     * 8 slot × (itemKey, level, hh) theo EquipSlotType WEAPON..MOUNT.
     * hh lấy từ entity đang trang bị (equipment / artifact / pet / mount).
     */
    public List<Integer> getListItemEquipView(MyUser mUser) {
        List<Integer> lst = normalizeItemEquipList();
        List<Integer> ret = new ArrayList<>(EQUIP_SLOT_COUNT * EQUIP_FIELDS_PER_SLOT);
        UserResources res = mUser != null ? mUser.getResources() : null;
        for (int i = 0; i < EQUIP_SLOT_COUNT; i++) {
            int base = i * EQUIP_FIELDS_PER_SLOT;
            int rowId = lst.get(base);
            int key = lst.get(base + 1);
            int level = lst.get(base + 2);
            int hh = 0;
            int slotType = EQUIP_SLOT_ORDER[i];
            if (rowId > 0 && res != null) {
                if (slotType == protocol.Pbmethod.EquipSlotType.TREASURE.getNumber()) {
                    UserArtifactEntity art = res.getArtifact(rowId);
                    if (art != null) {
                        key = art.getArtifactId();
                        level = art.getLevel();
                        hh = art.getHh();
                    }
                } else if (slotType == protocol.Pbmethod.EquipSlotType.PET.getNumber()) {
                    UserPetEntity pet = res.getPet(rowId);
                    if (pet != null) {
                        key = pet.getPetId();
                        level = pet.getLevel();
                        hh = pet.getHh();
                    }
                } else if (slotType >= protocol.Pbmethod.EquipSlotType.BODY.getNumber()
                        && slotType <= protocol.Pbmethod.EquipSlotType.BRACELET.getNumber()) {
                    UserSkinEntity skin = res.getSkin(rowId);
                    if (skin != null) {
                        key = skin.getSkinId();
                        level = skin.getTier();
                    }
                } else {
                    UserEquipmentEntity equip = res.getEquipment(rowId);
                    if (equip != null) {
                        key = equip.getItemId();
                        level = equip.getLevel();
                        hh = equip.getHh();
                    }
                }
            }
            if (rowId <= 0) {
                level = 0;
                hh = 0;
            }
            ret.add(key);
            ret.add(level);
            ret.add(hh);
        }
        return ret;
    }

    public List<Long> getListItemEquipViewLong(MyUser mUser) {
        List<Integer> view = getListItemEquipView(mUser);
        List<Long> ret = new ArrayList<>(view.size());
        for (int v : view) ret.add((long) v);
        return ret;
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
        mUser.syncDropRates(point);
        game.treasure.service.battle.ZoneAttackService.refresh(mUser);
        //todo tính thêm chỉ số của thẻ monster
        return point;
    }

    public Point getInitPoint(MyUser mUser) { // chỉ lấy từ lúc init player
        Point point = getCachePoint();
        long cacheHp = point.getCurHP();
        point = IMath.calculatePoint(mUser, true);
        game.treasure.service.user.UserBuff.applyActiveToPoint(mUser, point);
        if (mUser.isLastHomeDead()) {
            point.setCurHp(0);
        } else {
            point.setCurHp(cacheHp <= 0 ? point.getMaxHp() : Math.min(cacheHp, point.getMaxHp()));
        }
        mUser.syncDropRates(point);
        game.treasure.service.battle.ZoneAttackService.refresh(mUser);
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
        pb.setCup(cup);
        pb.setBlockType(blockType);
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
        member.addAllItemEquip(getAllInfoItemEquip());
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
                int rankClan = ClanManager.getInstance(clan).getClan().getClanRank();
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

    public int getBodySkinId() {
        int idx = equipSlotIndex(protocol.Pbmethod.EquipSlotType.BODY.getNumber());
        List<Integer> lst = getAllInfoItemEquip();
        if (idx < 0 || idx + 1 >= lst.size()) return 0;
        return lst.get(idx + 1);
    }

    public boolean updateSkin(protocol.Pbmethod.EquipSlotType part, long userSkinId, int resSkinId) {
        int idx = equipSlotIndex(part.getNumber());
        if (idx < 0) return false;
        List<Integer> lst = normalizeItemEquipList();
        lst.set(idx, (int) userSkinId);
        lst.set(idx + 1, resSkinId);
        lst.set(idx + 2, 0);
        return updateItemEquip(lst);
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

    public synchronized void addCup(long value) {
        cup += (int) value;
    }

    public synchronized void addGold(long value) {
        gold += value;
    }

    public synchronized void addMobKill(int delta) {
        if (delta <= 0) return;
        mobKill += delta;
        mobKillDirty = true;
    }

    public synchronized void addVipExp(long value) {
        if (vip >= ResEvent.lengthVip - 1) return;
        vipExp += value;
        ResVipEntity nextRes = ResEvent.getResVip(vip + 1);
        if (nextRes == null) return;
        int maxExp = nextRes.getExp();
        while (vipExp >= maxExp) {
            vipExp -= maxExp;
            vip++;
            if (vip >= ResEvent.lengthVip - 1) break;
            nextRes = ResEvent.getResVip(vip + 1);
            if (nextRes == null) break;
            maxExp = nextRes.getExp();
        }
        if (vip >= ResEvent.lengthVip - 1) vipExp = 0;
    }


    public BlockType getBlockType() {
        return BlockType.get(blockType);
    }

    // region db
    public boolean updateCreateUser(String name, List<Integer> equip) {
        String dbValue = StringHelper.toDBString(equip);
        if (update(Arrays.asList("name", name, "item_equipment", dbValue))) {
            this.name = name;
            this.itemEquipment = dbValue;
            return true;
        }
        return false;
    }

    /** Tạo nhân vật: 4 lựa chọn + body 2000 + head 12000. Ô khác = 0. */
    public List<Integer> buildCreateLook(int hair, int face, int armor, int pants) {
        List<Integer> equip = new ArrayList<>();
        for (int i = 0; i < EQUIP_LIST_SIZE; i++) equip.add(0);
        putLookKey(equip, protocol.Pbmethod.EquipSlotType.BODY.getNumber(), 2000);
        putLookKey(equip, protocol.Pbmethod.EquipSlotType.HEAD.getNumber(), 12000);
        putLookKey(equip, protocol.Pbmethod.EquipSlotType.HAIR.getNumber(), hair);
        putLookKey(equip, protocol.Pbmethod.EquipSlotType.FACE.getNumber(), face);
        putLookKey(equip, protocol.Pbmethod.EquipSlotType.ARMOR.getNumber(), armor);
        putLookKey(equip, protocol.Pbmethod.EquipSlotType.PANTS.getNumber(), pants);
        return equip;
    }

    private static void putLookKey(List<Integer> equip, int slotType, int itemKey) {
        int idx = equipSlotIndex(slotType);
        if (idx < 0) return;
        equip.set(idx + 1, itemKey);
    }

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
        if (mobKillDirty) {
            obj.add("mob_kill");
            obj.add(mobKill);
            mobKillDirty = false;
        }
        this.setLastAction(time);
        return DBJPA.update("user", obj, Arrays.asList("id", id));
    }

    /** Ghi mob_kill khi logout — luôn flush dù chưa có update user khác trong session. */
    public boolean flushMobKill() {
        if (!mobKillDirty) return true;
        mobKillDirty = false;
        return update(Arrays.asList("mob_kill", mobKill));
    }


    // endregion
}
