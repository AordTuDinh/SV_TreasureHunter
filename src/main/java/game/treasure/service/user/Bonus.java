package game.treasure.service.user;

import game.config.CfgAchievement;
import game.config.CfgItem;
import game.config.CfgLottery;
import game.config.CfgMaterial;
import game.config.CfgServer;
import game.config.aEnum.*;
import game.config.lang.Lang;
import protocol.Pbmethod;
import game.treasure.mapping.*;
import game.treasure.mapping.main.ResItemEntity;
import game.treasure.service.resource.ResAvatar;
import game.treasure.service.item.EquipmentStatRollService;
import game.treasure.service.resource.ResItem;
import game.treasure.service.resource.ResMount;
import game.object.MyUser;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.StringHelper;

import java.util.*;

public class Bonus {
    public static final int BONUS_GOLD = 1;
    public static final int BONUS_GEM = 2;
    public static final int BONUS_RUBY = 3;
    public static final int BONUS_ITEM = 4;
    public static final int BONUS_ARTIFACT = 5;
    public static final int BONUS_SKIN = 6;
    public static final int BONUS_EFFECT_SKIN = 7;
    public static final int BONUS_VIP_EXP = 8;
    public static final int BONUS_PET = 9;
    public static final int BONUS_MOUNT = 10;
    public static final int BONUS_MATERIAL = 11;
    public static final int BONUS_EQUIPMENT = 12;

    public static final Map<Integer, Integer> mTypeLength = new HashMap<>() {{
        put(BONUS_GOLD, 1);
        put(BONUS_GEM, 1);
        put(BONUS_RUBY, 1);
        put(BONUS_ITEM, 1);
        put(BONUS_ARTIFACT, 1);
        put(BONUS_SKIN, 1);
        put(BONUS_EFFECT_SKIN, 2);
        put(BONUS_VIP_EXP, 1);
        put(BONUS_PET, 2);
        put(BONUS_MOUNT, 2);
        put(BONUS_MATERIAL, 2);
        put(BONUS_EQUIPMENT, 2);
    }};

    public static List<Integer> bonusSinger = Arrays.asList(
            BONUS_ITEM, BONUS_EQUIPMENT, BONUS_ARTIFACT, BONUS_PET, BONUS_MOUNT,
            BONUS_SKIN, BONUS_EFFECT_SKIN, BONUS_MATERIAL);

    public static boolean isBonusSinger(int type) {
        return bonusSinger.contains(type);
    }

    public static List<Long> viewGold(long number) {
        return view(BONUS_GOLD, number);
    }

    /** Preview/receive item by config key (consum / currency / event). */
    public static List<Long> viewItem(int itemKey, long number) {
        if (number < 0) {
            List<Long> ret = new ArrayList<>();
            for (int i = 0; i < -number; i++)
                ret.addAll(view(BONUS_ITEM, -itemKey));
            return ret;
        }
        return viewXNumber(view(BONUS_ITEM, itemKey), (int) number);
    }

    /** Apply/cost input — [4, rowId, -itemKey]; output receive wire vẫn [4, rowId, -itemKey]. */
    public static List<Long> viewItemRemove(long userItemId, int itemKey, int count) {
        List<Long> ret = new ArrayList<>();
        List<Long> one = view(BONUS_ITEM, userItemId, -itemKey);
        for (int i = 0; i < count; i++)
            ret.addAll(one);
        return ret;
    }

    public static List<Long> viewItemMaterial(MaterialType type, long number) {
        return viewXNumber(view(BONUS_ITEM, type.id), (int) number);
    }

    public static List<Long> viewPet(int petId, int tier) {
        int t = tier > 0 ? Math.min(tier, 4) : 1;
        return view(BONUS_PET, petId, t);
    }

    public static List<Long> viewPet(int petId) {
        return viewPet(petId, 1);
    }

    public static List<Long> viewMount(int mountId, int tier) {
        int t = tier > 0 ? Math.min(tier, 4) : 1;
        return view(BONUS_MOUNT, mountId, t);
    }

    public static List<Long> viewMount(int mountId) {
        return viewMount(mountId, 1);
    }

    public static List<Long> viewMaterial(int materialId, int rank) {
        return view(BONUS_MATERIAL, materialId, rank);
    }

    public static List<Long> viewMaterial(int materialId) {
        return viewMaterial(materialId, 1);
    }

    public static List<Long> viewSkin(int skinId) {
        return view(BONUS_SKIN, skinId);
    }

    public static List<Long> viewItem(int itemType, Pbmethod.ItemKey itemKey, long number) {
        return viewItem(itemKey.getNumber(), number);
    }

    public static List<Long> viewDameSkin(int skinId) {
        return view(BONUS_EFFECT_SKIN, SkinType.DAMAGE_SKIN.value, skinId);
    }

    public static List<Long> viewItemEquipment(int itemId, int tier) {
        int t = tier > 0 ? Math.min(tier, 4) : 1;
        return view(BONUS_EQUIPMENT, itemId, t);
    }

    public static List<Long> viewItemEquipment(int itemId, int lock, long time) {
        return viewItemEquipment(itemId, 1);
    }

    public static List<Long> viewItemEquipmentRemove(long userEquipId, int itemKey, int tier, int count) {
        List<Long> ret = new ArrayList<>();
        List<Long> one = view(BONUS_EQUIPMENT, userEquipId, itemKey, tier);
        for (int i = 0; i < count; i++)
            ret.addAll(one);
        return ret;
    }

    public static List<Long> viewItemArtifact(int artifactId) {
        return view(BONUS_ARTIFACT, artifactId);
    }

    public static List<Long> viewGem(int number) {
        return view(BONUS_GEM, number);
    }

    public static List<Long> viewRuby(int number) {
        return view(BONUS_RUBY, number);
    }

    public static List<Long> viewVipExp(long number) {
        return view(BONUS_VIP_EXP, number);
    }

    public static List<Long> view(int bonusType, long... values) {
        List<Long> aLong = new ArrayList<>();
        aLong.add((long) bonusType);
        for (long value : values)
            aLong.add(value);
        return aLong;
    }

    public static int getIdItem(List<Long> bonus) {
        int type = Math.toIntExact(bonus.get(0));
        switch (type) {
            case BONUS_ITEM -> {
                // preview [4, itemKey] | receive [4, rowId, itemKey]
                return bonus.size() == 3
                        ? Math.toIntExact(bonus.get(2))
                        : Math.toIntExact(bonus.get(1));
            }
            case BONUS_EQUIPMENT -> {
                // preview [12, itemKey, tier] | receive [12, rowId, itemKey, tier]
                return bonus.size() == 4
                        ? Math.toIntExact(bonus.get(2))
                        : Math.toIntExact(bonus.get(1));
            }
            case BONUS_ARTIFACT -> Math.toIntExact(bonus.get(2));
            case BONUS_SKIN -> Math.toIntExact(bonus.get(1));
            case BONUS_EFFECT_SKIN -> Math.toIntExact(bonus.get(2));
        }
        return 0;
    }

    static boolean isBonusWireType(int v) {
        return v == BONUS_GOLD || v == BONUS_GEM || v == BONUS_RUBY || v == BONUS_ITEM
                || v == BONUS_EQUIPMENT || v == BONUS_ARTIFACT || v == BONUS_SKIN
                || v == BONUS_EFFECT_SKIN || v == BONUS_VIP_EXP
                || v == BONUS_PET || v == BONUS_MOUNT || v == BONUS_MATERIAL;
    }

  /** Apply wire payload sau type — ITEM preview [itemKey], deduct [-itemKey], remove [rowId, ±itemKey]. */
    static int itemApplyPayloadLength(List<Long> bonus, int index) {
        if (index >= bonus.size())
            return 0;
        if (bonus.get(index) < 0)
            return 1;
        if (index + 1 >= bonus.size())
            return 1;
        if (isBonusWireType(bonus.get(index + 1).intValue()))
            return 1;
        return 2;
    }

    /** Apply wire payload sau type — EQUIP preview [itemKey,tier], remove [rowId,itemKey,tier]. */
    static int equipmentApplyPayloadLength(List<Long> bonus, int index) {
        if (index >= bonus.size())
            return 0;
        if (index + 1 >= bonus.size())
            return Math.max(0, bonus.size() - index);
        if (index + 2 >= bonus.size())
            return Math.max(0, bonus.size() - index);
        if (isBonusWireType(bonus.get(index + 2).intValue()))
            return 2;
        return 3;
    }

    /** Apply wire payload sau type — PET/MOUNT preview [configId,tier], remove [rowId,configId,tier]. */
    static int petMountApplyPayloadLength(List<Long> bonus, int index) {
        if (index >= bonus.size())
            return 0;
        if (index + 1 >= bonus.size())
            return Math.max(0, bonus.size() - index);
        if (index + 2 >= bonus.size())
            return Math.max(0, bonus.size() - index);
        if (isBonusWireType(bonus.get(index + 2).intValue()))
            return 2;
        return 3;
    }

    static int applyPayloadLength(List<Long> bonus, int index, int type) {
        if (type == BONUS_ITEM)
            return itemApplyPayloadLength(bonus, index);
        if (type == BONUS_EQUIPMENT)
            return equipmentApplyPayloadLength(bonus, index);
        if (type == BONUS_PET || type == BONUS_MOUNT)
            return petMountApplyPayloadLength(bonus, index);
        return mTypeLength.getOrDefault(type, 0);
    }

    public static Pbmethod.ItemType resolveStorageType(int itemKey) {
        ResItemEntity res = ResItem.getItem(itemKey);
        if (res != null && res.getItemType() != null)
            return res.getItemType();
        return Pbmethod.ItemType.POSITION;
    }

    public static List<Long> viewXNumber(List<Long> bonus, int xNumber) {
        List<Long> ret = new ArrayList<>();
        if (isBonusSinger(Math.toIntExact(bonus.get(0)))) {
            int times = Math.abs(xNumber);
            for (int i = 0; i < times; i++)
                ret.addAll(bonus);
        } else {
            int last = bonus.size() - 1;
            bonus.set(last, bonus.get(last) * xNumber);
            ret.addAll(bonus);
        }
        return ret;
    }

    public static List<Long> receiveListItem(MyUser mUser, String detailAction, List<Long> aBonus) {
        List<Long> aLong = new ArrayList<>();
        for (List<Long> chunk : parseForApply(aBonus))
            aLong.addAll(applyBonusChunk(mUser, chunk, detailAction));
        return aLong;
    }

    /** Tách flat wire apply — preview grant (parse cố định) + deduct/remove (payload biến). */
    static List<List<Long>> parseForApply(List<Long> bonus) {
        return parseCost(bonus);
    }

    static List<Long> applyBonusChunk(MyUser mUser, List<Long> chunk, String detailAction) {
        if (chunk == null || chunk.isEmpty())
            return new ArrayList<>();
        int type = chunk.get(0).intValue();
        return switch (type) {
            case BONUS_GOLD -> addGold(mUser, chunk.get(1), detailAction);
            case BONUS_GEM -> addGem(mUser, chunk.get(1), detailAction);
            case BONUS_RUBY -> addRuby(mUser, chunk.get(1), detailAction);
            case BONUS_ITEM -> applyUserItemChunk(mUser, chunk, detailAction);
            case BONUS_EQUIPMENT -> applyUserEquipmentChunk(mUser, chunk, detailAction);
            case BONUS_ARTIFACT -> addItemArtifact(mUser, chunk.get(1).intValue(), detailAction);
            case BONUS_VIP_EXP -> addVipExp(mUser, chunk.get(1).intValue(), detailAction);
            case BONUS_SKIN -> addCharacterSkin(mUser, chunk.get(1).intValue(), detailAction);
            case BONUS_EFFECT_SKIN -> addEffectSkin(mUser, chunk.get(1).intValue(), chunk.get(2).intValue(), detailAction);
            case BONUS_PET -> addPet(mUser, chunk.get(1).intValue(), chunk.get(2).intValue(), detailAction);
            case BONUS_MOUNT -> addMount(mUser, chunk.get(1).intValue(), chunk.get(2).intValue(), detailAction);
            case BONUS_MATERIAL -> addMaterial(mUser, chunk.get(1).intValue(), chunk.get(2).intValue(), detailAction);
            default -> new ArrayList<>();
        };
    }

    /** Preview grant [4,itemKey] | deduct [4,-itemKey] | remove [4,rowId,-itemKey]. */
    static List<Long> applyUserItemChunk(MyUser mUser, List<Long> chunk, String detailAction) {
        if (chunk.size() == 2) {
            int itemKey = chunk.get(1).intValue();
            if (itemKey < 0)
                return deductUserItemByKey(mUser, -itemKey, detailAction);
            return grantUserItem(mUser, itemKey, detailAction);
        }
        if (chunk.size() == 3)
            return removeUserItemRow(mUser, chunk.get(1), Math.abs(chunk.get(2).intValue()), detailAction);
        return new ArrayList<>();
    }

    /** Preview grant [12,itemKey,tier] | remove [12,rowId,itemKey,tier]. */
    static List<Long> applyUserEquipmentChunk(MyUser mUser, List<Long> chunk, String detailAction) {
        if (chunk.size() == 3)
            return grantUserEquipment(mUser, chunk.get(1).intValue(), chunk.get(2).intValue(), detailAction);
        if (chunk.size() == 4)
            return removeUserEquipmentRow(mUser, chunk.get(1), chunk.get(2).intValue(), chunk.get(3).intValue(), detailAction);
        return new ArrayList<>();
    }

    static List<Long> addCharacterSkin(MyUser mUser, int skinId, String detailAction) {
        if (ResAvatar.getSkin(skinId) == null) return new ArrayList<>();
        UserSkinEntity existing = mUser.getResources().getSkinByConfigId(skinId);
        if (existing != null) {
            return Arrays.asList((long) BONUS_SKIN, existing.getId(), (long) skinId);
        }
        UserSkinEntity uSkin = new UserSkinEntity(mUser.getUser(), skinId, ResAvatar.getSkin(skinId).getType());
        if (DBJPA.save(uSkin)) {
            mUser.getResources().addSkin(uSkin);
            if (CfgServer.isRealServer()) {
                Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction,
                        "type", "character_skin", "id", uSkin.getId(), "skinId", skinId);
            }
            return Arrays.asList((long) BONUS_SKIN, uSkin.getId(), (long) skinId);
        }
        return new ArrayList<>();
    }

    static List<Long> addEffectSkin(MyUser mUser, int type, int skinId, String detailAction) {
        if (type == SkinType.DAMAGE_SKIN.value) {
            if (mUser.getUData().addDameSkin(skinId) && mUser.getUData().update(List.of("dame_skin", mUser.getUData().getDameSkin()))) {
                if (CfgServer.isRealServer())
                    Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction, "dameSkin", "skinId", skinId);
                return Arrays.asList((long) BONUS_EFFECT_SKIN, (long) type, (long) skinId);
            }
            return Arrays.asList((long) BONUS_EFFECT_SKIN, (long) type, (long) skinId);
        } else if (type == SkinType.CHAT_FRAME.value) {
            if (mUser.getUData().addChatFrame(skinId) && mUser.getUData().update(List.of("chat_frame", mUser.getUData().getChatFrame()))) {
                if (CfgServer.isRealServer())
                    Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction, "chatFrame", "frameId", skinId);
                return Arrays.asList((long) BONUS_EFFECT_SKIN, (long) type, (long) skinId);
            }
            return Arrays.asList((long) BONUS_EFFECT_SKIN, (long) type, (long) skinId);
        } else if (type == SkinType.TRIAL.value) {
            if (mUser.getUData().addEffectTrial(skinId) && mUser.getUData().update(List.of("list_trial", mUser.getUData().getListTrial()))) {
                if (CfgServer.isRealServer())
                    Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction, "list_trial", "trialId", skinId);
                return Arrays.asList((long) BONUS_EFFECT_SKIN, (long) type, (long) skinId);
            }
            return Arrays.asList((long) BONUS_EFFECT_SKIN, (long) type, (long) skinId);
        }
        return new ArrayList<>();
    }

    static List<Long> deductUserItemByKey(MyUser mUser, int itemKey, String detailAction) {
        List<UserItemEntity> rows = mUser.getResources().listByItemKey(itemKey);
        if (rows.isEmpty())
            return new ArrayList<>();
        long rowId = rows.get(0).getId();
        if (!mUser.getResources().removeItemsByItemKey(itemKey, 1))
            return new ArrayList<>();
        return Arrays.asList((long) BONUS_ITEM, rowId, (long) -itemKey);
    }

    static List<Long> grantUserItem(MyUser mUser, int itemKey, String detailAction) {
        UserItemEntity uItem;
        if (itemKey == Pbmethod.ItemKey.TICKER_NORMAL.getNumber()) {
            uItem = checkGenItemData(mUser, 1);
            if (uItem == null) return new ArrayList<>();
            return Arrays.asList((long) BONUS_ITEM, uItem.getId(), (long) itemKey);
        }
        Pbmethod.ItemType type = resolveStorageType(itemKey);
        uItem = new UserItemEntity(mUser.getUser().getId(), itemKey, type);
        boolean needSlot = usesItemSlotForUserItem(type);
        if (needSlot && !mUser.getResources().prepareNewItemSlot(BONUS_ITEM, 0))
            return new ArrayList<>();
        if (type == Pbmethod.ItemType.POSITION)
            ResItem.initConsumableInstanceData(uItem);
        if (DBJPA.save(uItem)) {
            if (needSlot && !mUser.getResources().prepareNewItemSlot(BONUS_ITEM, uItem.getId())) {
                uItem.deleteFromDb();
                return new ArrayList<>();
            }
            mUser.getResources().addItem(uItem);
            if (type == Pbmethod.ItemType.POSITION && CfgItem.isItemMedicine(itemKey))
                mUser.queueItemPointUpdate(uItem);
            if (CfgServer.isRealServer()) {
                Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction,
                        "type", "user_item",
                        "id", uItem.getId(),
                        "itemId", itemKey,
                        "storageType", uItem.getType(),
                        "tier", uItem.getTier(),
                        "addValue", 1);
            }
            return Arrays.asList((long) BONUS_ITEM, uItem.getId(), (long) itemKey);
        }
        return new ArrayList<>();
    }

    static List<Long> grantUserEquipment(MyUser mUser, int itemKey, int configTier, String detailAction) {
        if (ResItem.getItemEquipment(itemKey) == null)
            return new ArrayList<>();
        UserEquipmentEntity uEquip = new UserEquipmentEntity(mUser.getUser().getId(), itemKey);
        uEquip.setTier(configTier);
        if (!mUser.getResources().prepareNewItemSlot(BONUS_EQUIPMENT, 0))
            return new ArrayList<>();
        EquipmentStatRollService.rollStatsIfNeeded(uEquip);
        if (DBJPA.save(uEquip)) {
            if (!mUser.getResources().prepareNewItemSlot(BONUS_EQUIPMENT, uEquip.getId())) {
                uEquip.deleteFromDb();
                return new ArrayList<>();
            }
            mUser.getResources().addEquipment(uEquip);
            if (CfgServer.isRealServer()) {
                Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction,
                        "type", "user_equipment",
                        "id", uEquip.getId(),
                        "itemId", itemKey,
                        "tier", uEquip.getTier(),
                        "addValue", 1);
            }
            return Arrays.asList((long) BONUS_EQUIPMENT, uEquip.getId(), (long) itemKey, (long) uEquip.getTier());
        }
        return new ArrayList<>();
    }

    static List<Long> removeUserItemRow(MyUser mUser, long userItemId, int itemKey, String detailAction) {
        UserItemEntity uItem = mUser.getResources().getItem(userItemId);
        if (uItem == null || uItem.getItemId() != itemKey)
            return new ArrayList<>();
        clearItemFromSlot(mUser, BONUS_ITEM, userItemId);
        if (uItem.isAggregatedItem()) {
            if (!mUser.getResources().removeItemsByItemKey(itemKey, 1))
                return new ArrayList<>();
        } else if (!uItem.deleteFromDb()) {
            return new ArrayList<>();
        } else {
            mUser.getResources().removeItem(userItemId);
        }
        if (CfgServer.isRealServer()) {
            Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction,
                    "type", "user_item_remove", "id", userItemId, "itemId", itemKey);
        }
        return Arrays.asList((long) BONUS_ITEM, userItemId, (long) -itemKey);
    }

    static List<Long> removeUserEquipmentRow(MyUser mUser, long userEquipId, int itemKey, int tier, String detailAction) {
        UserEquipmentEntity uEquip = mUser.getResources().getEquipment(userEquipId);
        if (uEquip == null || uEquip.getItemId() != itemKey)
            return new ArrayList<>();
        clearItemFromSlot(mUser, BONUS_EQUIPMENT, userEquipId);
        if (!uEquip.deleteFromDb())
            return new ArrayList<>();
        mUser.getResources().removeEquipment(userEquipId);
        if (CfgServer.isRealServer()) {
            Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction,
                    "type", "user_equipment_remove", "id", userEquipId, "itemId", itemKey, "tier", tier);
        }
        return Arrays.asList((long) BONUS_EQUIPMENT, userEquipId, (long) -itemKey, (long) uEquip.getTier());
    }

    static UserItemEntity checkGenItemData(MyUser mUser, int numItem) {
        UserItemEntity uItem = mUser.getResources().getItemByItemKey(Pbmethod.ItemKey.TICKER_NORMAL.getNumber());
        long eventDay = CfgLottery.getEventIdBuy();
        List<Long> nums = new ArrayList<>();
        for (int i = 0; i < numItem; i++)
            nums.add(NumberUtil.getRandomLong(100000, 999999));
        if (uItem == null) {
            uItem = new UserItemEntity(mUser.getUser().getId(), Pbmethod.ItemKey.TICKER_NORMAL.getNumber(), Pbmethod.ItemType.EVENT);
            nums.add(0, eventDay);
            uItem.setData(StringHelper.toDBString(nums));
            if (!DBJPA.save(uItem))
                return null;
            mUser.getResources().addItem(uItem);
        } else {
            List<Long> dataSticker = new ArrayList<>(GsonUtil.strToListLong(uItem.getData() == null ? "[]" : uItem.getData()));
            if (dataSticker.isEmpty() || dataSticker.get(0) != eventDay) {
                dataSticker = new ArrayList<>();
                dataSticker.add(eventDay);
            }
            dataSticker.addAll(nums);
            uItem.setData(StringHelper.toDBString(dataSticker));
            uItem.update(List.of("data", uItem.getData()));
        }
        return uItem;
    }

    static List<Long> addItemArtifact(MyUser mUser, int artifactId, String detailAction) {
        if (mUser.getResources().getArtifactByConfigId(artifactId) != null)
            return new ArrayList<>();
        if (!mUser.getResources().prepareNewItemSlot(BONUS_ARTIFACT, 0))
            return new ArrayList<>();
        UserArtifactEntity uArtifact = new UserArtifactEntity(mUser.getUser().getId(), artifactId);
        if (DBJPA.save(uArtifact)) {
            if (!mUser.getResources().prepareNewItemSlot(BONUS_ARTIFACT, uArtifact.getId())) {
                DBJPA.delete("user_artifact", "id", uArtifact.getId(), "user_id", uArtifact.getUserId());
                return new ArrayList<>();
            }
            mUser.getResources().addArtifact(uArtifact);
            if (CfgServer.isRealServer())
                Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction, "type", "artifact", "id", uArtifact.getId(), "artifactId", artifactId);
            return Arrays.asList((long) BONUS_ARTIFACT, uArtifact.getId(), (long) artifactId);
        }
        return new ArrayList<>();
    }

    static List<Long> addVipExp(MyUser mUser, int addExp, String detailAction) {
        UserEntity user = mUser.getUser();
        mUser.getUser().addVipExp(addExp);
        if (DBJPA.update("user", Arrays.asList("vip_exp", user.getVipExp(), "vip", user.getVip()), Arrays.asList("id", user.getId()))) {
            Actions.save(user, Actions.GRECEIVE, detailAction, "type", "vip_exp", "vip", user.getVip(), "exp", user.getVipExp(), "addExp", addExp);
            return Arrays.asList((long) BONUS_VIP_EXP, (long) user.getVipExp(), (long) addExp, (long) user.getVip());
        }
        return new ArrayList<>();
    }

    static List<Long> addPet(MyUser mUser, int petId, int tier, String detailAction) {
        if (tier <= 0)
            tier = 1;
        if (!mUser.getResources().prepareNewItemSlot(BONUS_PET, 0))
            return new ArrayList<>();
        UserPetEntity uPet = new UserPetEntity(mUser.getUser(), petId);
        uPet.setTier(tier);
        if (DBJPA.save(uPet)) {
            if (!mUser.getResources().prepareNewItemSlot(BONUS_PET, uPet.getId())) {
                DBJPA.delete("user_pet", "id", uPet.getId(), "user_id", uPet.getUserId());
                return new ArrayList<>();
            }
            mUser.getResources().addPet(uPet);
            if (CfgServer.isRealServer()) {
                Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction,
                        "type", "pet", "id", uPet.getId(), "petId", petId, "tier", tier);
            }
            return Arrays.asList((long) BONUS_PET, uPet.getId(), (long) petId, (long) tier);
        }
        return new ArrayList<>();
    }

    static List<Long> addMount(MyUser mUser, int mountId, int tier, String detailAction) {
        if (tier <= 0)
            tier = 1;
        if (ResMount.get(mountId) == null) return new ArrayList<>();
        if (!mUser.getResources().prepareNewItemSlot(BONUS_MOUNT, 0))
            return new ArrayList<>();
        UserMountEntity uMount = new UserMountEntity(mUser.getUser(), mountId);
        uMount.setTier(tier);
        if (DBJPA.save(uMount)) {
            if (!mUser.getResources().prepareNewItemSlot(BONUS_MOUNT, uMount.getId())) {
                DBJPA.delete("user_mount", "id", uMount.getId(), "user_id", uMount.getUserId());
                return new ArrayList<>();
            }
            mUser.getResources().addMount(uMount);
            if (CfgServer.isRealServer()) {
                Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction,
                        "type", "mount", "id", uMount.getId(), "mountId", mountId, "tier", tier);
            }
            return Arrays.asList((long) BONUS_MOUNT, uMount.getId(), (long) mountId, (long) tier);
        }
        return new ArrayList<>();
    }

    static List<Long> addMaterial(MyUser mUser, int materialId, int rank, String detailAction) {
        if (CfgMaterial.get(materialId) == null)
            return new ArrayList<>();
        if (!mUser.getResources().canAddMaterial(1)) return new ArrayList<>();
        UserMaterialEntity uMaterial = new UserMaterialEntity(mUser.getUser().getId(), materialId, rank);
        if (DBJPA.save(uMaterial)) {
            mUser.getResources().addMaterial(uMaterial);
            if (CfgServer.isRealServer()) {
                Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction,
                        "type", "material",
                        "id", uMaterial.getId(),
                        "materialId", materialId,
                        "mat_rank", rank,
                        "level", uMaterial.getLevel(),
                        "value", uMaterial.getValue());
            }
            return Arrays.asList((long) BONUS_MATERIAL, uMaterial.getId(), (long) materialId, (long) rank);
        }
        return new ArrayList<>();
    }

    static List<Long> addGold(MyUser mUser, long value, String detailAction) {
        if (detailAction.equals(DetailActionType.BONUS_KILL_ENEMY)) {
            mUser.getUser().addGold(value);
            CfgAchievement.addListAchievement(mUser, 5, CfgAchievement.addGold, (int) value);
            return Arrays.asList((long) BONUS_GOLD, mUser.getUser().getGold(), value);
        } else {
            if (dbAddGold(mUser.getUser(), value)) {
                mUser.getUser().addGold(value);
                CfgAchievement.addListAchievement(mUser, 5, CfgAchievement.addGold, (int) value);
                if (CfgServer.isRealServer()) Actions.logGold(mUser.getUser(), detailAction, value);
                return Arrays.asList((long) BONUS_GOLD, mUser.getUser().getGold(), value);
            }
            return new ArrayList<>();
        }
    }

    static List<Long> addGem(MyUser mUser, long value, String detailAction) {
        if (dbAddGem(mUser.getUser(), value)) {
            mUser.getUser().addGem(value);
            CfgAchievement.addListAchievement(mUser, 5, CfgAchievement.addGem, (int) value);
            if (CfgServer.isRealServer())
                Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction, "type", "gem", "value", mUser.getUser().getGem(), "addValue", value);
            return Arrays.asList((long) BONUS_GEM, mUser.getUser().getGem(), value);
        }
        return new ArrayList<>();
    }

    static List<Long> addRuby(MyUser mUser, long value, String detailAction) {
        if (dbAddRuby(mUser.getUser(), value)) {
            mUser.getUser().addRuby(value);
            CfgAchievement.addListAchievement(mUser, 5, CfgAchievement.addRuby, (int) value);
            if (CfgServer.isRealServer())
                Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction, "type", "ruby", "value", mUser.getUser().getRuby(), "addValue", value);
            return Arrays.asList((long) BONUS_RUBY, mUser.getUser().getRuby(), value);
        }
        return new ArrayList<>();
    }

    static boolean dbAddGold(UserEntity user, long addGold) {
        return DBJPA.update("user", Arrays.asList("gem", user.getGem(), "gold", user.getGold() + addGold), Arrays.asList("id", user.getId()));
    }

    static boolean dbAddGem(UserEntity user, long addGem) {
        return DBJPA.update("user", Arrays.asList("gem", user.getGem() + addGem, "gold", user.getGold()), Arrays.asList("id", user.getId()));
    }

    static boolean dbAddRuby(UserEntity user, long addRuby) {
        return DBJPA.update("user", Arrays.asList("ruby", user.getRuby() + addRuby), Arrays.asList("id", user.getId()));
    }

    public static String checkMoney(MyUser mUser, List<Long> aBonus) {
        Map<Integer, Integer> itemDeduct = new HashMap<>();
        for (List<Long> chunk : parseCost(aBonus)) {
            int type = chunk.get(0).intValue();
            switch (type) {
                case BONUS_GOLD:
                    if (mUser.getUser().getGold() + chunk.get(1) < 0)
                        return Lang.instance(mUser).get(Lang.err_not_enough_gold);
                    break;
                case BONUS_GEM:
                    if (mUser.getUser().getGem() + chunk.get(1) < 0)
                        return Lang.instance(mUser).get(Lang.err_not_enough_gem);
                    break;
                case BONUS_RUBY:
                    if (mUser.getUser().getRuby() + chunk.get(1) < 0)
                        return Lang.instance(mUser).get(Lang.err_not_enough_ruby);
                    break;
                case BONUS_ITEM:
                    if (chunk.size() == 2 && chunk.get(1) < 0) {
                        itemDeduct.merge((int) (-chunk.get(1)), 1, Integer::sum);
                    } else if (chunk.size() == 3) {
                        long userItemId = chunk.get(1);
                        int itemKey = Math.abs(chunk.get(2).intValue());
                        UserItemEntity uItem = mUser.getResources().getItem(userItemId);
                        ResItemEntity resItem = ResItem.getItem(itemKey);
                        String name = resItem != null ? resItem.getName() : "?";
                        if (uItem == null || uItem.getItemId() != itemKey)
                            return String.format(Lang.instance(mUser).get(Lang.err_not_enough_item), name);
                    } else {
                        return Lang.instance(mUser).get(Lang.err_params);
                    }
                    break;
                case BONUS_EQUIPMENT:
                    if (chunk.size() != 4)
                        return Lang.instance(mUser).get(Lang.err_params);
                    long userEquipId = chunk.get(1);
                    int itemKey = chunk.get(2).intValue();
                    UserEquipmentEntity uEquip = mUser.getResources().getEquipment(userEquipId);
                    String name = ResItem.getItemEquipment(itemKey) != null
                            ? ResItem.getItemEquipment(itemKey).getName() : "?";
                    if (uEquip == null || uEquip.getItemId() != itemKey)
                        return String.format(Lang.instance(mUser).get(Lang.err_not_enough_item), name);
                    break;
            }
        }
        for (Map.Entry<Integer, Integer> entry : itemDeduct.entrySet()) {
            ResItemEntity resItem = ResItem.getItem(entry.getKey());
            String name = resItem != null ? resItem.getName() : "?";
            if (mUser.getResources().countByItemKey(entry.getKey()) < entry.getValue())
                return String.format(Lang.instance(mUser).get(Lang.err_not_enough_item), name);
        }
        return null;
    }

    public static List<Long> merge(List<Long> lstBonus) {
        List<List<Long>> ret = new ArrayList<>();
        List<List<Long>> aBonus = parse(lstBonus);
        aBonus.forEach(bonus -> {
            if (bonusSinger.contains(bonus.get(0).intValue())) {
                ret.add(bonus);
            } else {
                boolean include = false;
                for (List<Long> childBonus : ret) {
                    if (childBonus.size() == bonus.size()) {
                        boolean isOk = true;
                        for (int index = 0; index < bonus.size() - 1; index++) {
                            if (!bonus.get(index).equals(childBonus.get(index))) isOk = false;
                        }
                        if (isOk) {
                            childBonus.set(childBonus.size() - 1, childBonus.get(childBonus.size() - 1) + bonus.get(bonus.size() - 1));
                            include = true;
                            break;
                        }
                    }
                }
                if (!include) ret.add(bonus);
            }
        });
        List<Long> results = new ArrayList<>();
        for (List<Long> bonus : ret) results.addAll(bonus);
        return results;
    }

    /** Parse preview/reward config — ITEM [4,itemKey], EQUIP [12,itemKey,tier], PET/MOUNT [9|10,configId,tier]. */
    public static List<List<Long>> parse(List<Long> bonus) {
        List<List<Long>> result = new ArrayList<>();
        if (bonus != null && !bonus.isEmpty()) {
            int index = 0;
            while (index < bonus.size()) {
                List<Long> tmp = new ArrayList<>();
                int type = bonus.get(index++).intValue();
                int length = mTypeLength.getOrDefault(type, 0);
                tmp.add((long) type);
                for (int i = index; i < index + length; i++)
                    tmp.add(bonus.get(i));
                result.add(tmp);
                index += length;
            }
        }
        return result;
    }

    /** Parse apply wire — preview grant (len cố định) + deduct/remove (payload biến). */
    public static List<List<Long>> parseCost(List<Long> bonus) {
        List<List<Long>> result = new ArrayList<>();
        if (bonus == null || bonus.isEmpty())
            return result;
        int index = 0;
        while (index < bonus.size()) {
            int type = bonus.get(index++).intValue();
            int payloadIndex = index;
            int length = applyPayloadLength(bonus, payloadIndex, type);
            List<Long> tmp = new ArrayList<>();
            tmp.add((long) type);
            for (int i = 0; i < length; i++)
                tmp.add(bonus.get(payloadIndex + i));
            result.add(tmp);
            index = payloadIndex + length;
        }
        return result;
    }

    public static List<Long> reverseBonus(List<Long> bonus) {
        List<Long> ret = new ArrayList<>();
        List<List<Long>> aBonus = parse(bonus);
        for (List<Long> bm : aBonus) {
            if (!bonusSinger.contains(bm.get(0).intValue())) {
                int last = bm.size() - 1;
                bm.set(last, -bm.get(last));
                ret.addAll(bm);
            }
        }
        return ret.isEmpty() ? null : ret;
    }

    public static List<Long> xBonus(List<Long> bonus, int times) {
        List<List<Long>> aBonus = parse(bonus);
        List<Long> result = new ArrayList<>();
        aBonus.forEach(bo -> {
            if (bonusSinger.contains(bo.get(0).intValue())) {
                for (int i = 0; i < times; i++)
                    result.addAll(bo);
            } else {
                result.addAll(bo);
                result.set(result.size() - 1, result.get(result.size() - 1) * times);
            }
        });
        return result;
    }

    public static List<Long> xPerBonus(List<Long> bonus, int per100) {
        List<List<Long>> aBonus = parse(bonus);
        List<Long> result = new ArrayList<>();
        aBonus.forEach(bo -> {
            if (!bonusSinger.contains(bo.get(0).intValue())) {
                result.addAll(bo);
                result.set(result.size() - 1, (long) (result.get(result.size() - 1) * per100 / 100f));
            }
        });
        return result;
    }

    public static boolean moveEquipmentToBag(MyUser mUser, UserEquipmentEntity equip) {
        if (!mUser.getResources().canAddBagItem(1))
            return false;
        List<Long> slots = mUser.getUData().getItemSlotList();
        int bagCount = mUser.getUData().getSlotBagUI();
        Integer slot = ItemSlotHelper.findFirstEmpty(slots, 0, bagCount);
        if (slot == null)
            return false;
        ItemSlotHelper.setPair(slots, slot, BONUS_EQUIPMENT, equip.getId());
        if (!mUser.getResources().saveItemSlot(slots))
            return false;
        equip.setBagSlot(slot);
        return true;
    }

    public static boolean moveArtifactOutOfBag(MyUser mUser, UserArtifactEntity artifact) {
        if (artifact == null)
            return false;
        clearItemFromSlot(mUser, BONUS_ARTIFACT, artifact.getId());
        artifact.setBagSlot(-1);
        return true;
    }

    public static boolean moveArtifactToBag(MyUser mUser, UserArtifactEntity artifact) {
        if (artifact == null || !mUser.getResources().canAddBagItem(1))
            return false;
        List<Long> slots = mUser.getUData().getItemSlotList();
        int bagCount = mUser.getUData().getSlotBagUI();
        Integer slot = ItemSlotHelper.findFirstEmpty(slots, 0, bagCount);
        if (slot == null)
            return false;
        ItemSlotHelper.setPair(slots, slot, BONUS_ARTIFACT, artifact.getId());
        if (!mUser.getResources().saveItemSlot(slots))
            return false;
        artifact.setBagSlot(slot);
        return true;
    }

    public static boolean moveArtifactToBagSlot(MyUser mUser, UserArtifactEntity artifact, int slotIndex) {
        if (artifact == null || slotIndex < 0)
            return false;
        List<Long> slots = mUser.getUData().getItemSlotList();
        int bagCount = mUser.getUData().getSlotBagUI();
        if (slotIndex >= bagCount)
            return false;
        ItemSlotHelper.setPair(slots, slotIndex, BONUS_ARTIFACT, artifact.getId());
        if (!mUser.getResources().saveItemSlot(slots))
            return false;
        artifact.setBagSlot(slotIndex);
        return true;
    }

    public static int getEquippedArtifactRowId(MyUser mUser) {
        int treasureIdx = game.treasure.mapping.UserEntity.equipSlotIndex(
                protocol.Pbmethod.EquipSlotType.TREASURE.getNumber());
        List<Integer> lst = mUser.getUser().normalizeItemEquipList();
        if (treasureIdx < 0 || treasureIdx >= lst.size())
            return 0;
        return lst.get(treasureIdx);
    }

    public static boolean isArtifactEquipped(MyUser mUser, long rowId) {
        return rowId > 0 && getEquippedArtifactRowId(mUser) == (int) rowId;
    }

    public static boolean clearTreasureEquipSlot(MyUser mUser) {
        int treasureIdx = game.treasure.mapping.UserEntity.equipSlotIndex(
                protocol.Pbmethod.EquipSlotType.TREASURE.getNumber());
        List<Integer> lst = mUser.getUser().normalizeItemEquipList();
        if (treasureIdx < 0)
            return false;
        lst.set(treasureIdx, 0);
        lst.set(treasureIdx + 1, 0);
        lst.set(treasureIdx + 2, 0);
        return mUser.getUser().updateItemEquip(lst);
    }

    public static boolean moveEquipmentOutOfBag(MyUser mUser, UserEquipmentEntity equip) {
        clearItemFromSlot(mUser, BONUS_EQUIPMENT, equip.getId());
        equip.setBagSlot(-1);
        return true;
    }

    public static boolean isAggregatedEventItemKey(int itemKey) {
        return itemKey == Pbmethod.ItemKey.TICKER_NORMAL.getNumber()
                || itemKey == Pbmethod.ItemKey.TICKER_SPECIAL.getNumber();
    }

    /** item_slot: consum (BONUS_ITEM), equip, pet, mount, artifact — không gồm event/currency. */
    public static boolean usesItemSlotBonusType(int bonusType) {
        return bonusType == BONUS_ITEM || bonusType == BONUS_EQUIPMENT
                || bonusType == BONUS_PET || bonusType == BONUS_MOUNT
                || bonusType == BONUS_ARTIFACT;
    }

    public static boolean usesItemSlotForUserItem(Pbmethod.ItemType storageType) {
        return storageType == Pbmethod.ItemType.POSITION;
    }

    /** Item hiển thị tab Túi (type >= EVENT), không dùng item_slot home. */
    public static boolean usesEventBagStorage(Pbmethod.ItemType storageType) {
        return storageType != null
                && storageType != Pbmethod.ItemType.CURRENCY
                && storageType.getNumber() >= Pbmethod.ItemType.EVENT.getNumber();
    }

    /** Xóa ô item_slot trỏ tới row không còn tồn tại hoặc không thuộc túi UI. */
    public static void reconcileItemSlots(MyUser mUser) {
        List<Long> slots = mUser.getUData().getItemSlotList();
        int bagCount = mUser.getUData().getSlotBagUI();
        boolean changed = false;
        for (int s = 0; s < bagCount; s++) {
            if (ItemSlotHelper.isEmpty(slots, s))
                continue;
            int bt = ItemSlotHelper.getBonusType(slots, s);
            long rowId = ItemSlotHelper.getRowId(slots, s);
            if (!slotRowExists(mUser, bt, rowId)) {
                ItemSlotHelper.clearPair(slots, s);
                changed = true;
            }
        }
        if (changed)
            mUser.getResources().saveItemSlot(slots);
    }

    static boolean slotRowExists(MyUser mUser, int bonusType, long rowId) {
        if (rowId <= 0)
            return false;
        switch (bonusType) {
            case BONUS_ITEM: {
                UserItemEntity u = mUser.getResources().getItem(rowId);
                return u != null && usesItemSlotForUserItem(Pbmethod.ItemType.valueOf(u.getType()));
            }
            case BONUS_EQUIPMENT: {
                UserEquipmentEntity e = mUser.getResources().getEquipment(rowId);
                return e != null && !e.isEquip();
            }
            case BONUS_PET:
                return mUser.getResources().getPet(rowId) != null;
            case BONUS_MOUNT:
                return mUser.getResources().getMount(rowId) != null;
            case BONUS_ARTIFACT:
                return mUser.getResources().getArtifact(rowId) != null;
            default:
                return false;
        }
    }

    /** Gán ô túi UI — bonusType ∈ {4 consum, 12 equip, 9 pet, 10 mount, 5 artifact}. rowId=0 chỉ check còn chỗ. */
    public static boolean prepareNewItemSlot(MyUser mUser, int bonusType, long rowId) {
        if (!usesItemSlotBonusType(bonusType))
            return true;
        reconcileItemSlots(mUser);
        List<Long> slots = mUser.getUData().getItemSlotList();
        int bagCount = mUser.getUData().getSlotBagUI();
        if (rowId == 0)
            return mUser.getResources().canAddBagItem(1);
        if (!mUser.getResources().canAddBagItem(1))
            return false;
        Integer slot = ItemSlotHelper.findFirstEmpty(slots, 0, bagCount);
        if (slot == null)
            return false;
        ItemSlotHelper.setPair(slots, slot, bonusType, rowId);
        return mUser.getResources().saveItemSlot(slots);
    }

    public static void clearItemFromSlot(MyUser mUser, int bonusType, long rowId) {
        if (!usesItemSlotBonusType(bonusType))
            return;
        List<Long> slots = mUser.getUData().getItemSlotList();
        int bagCount = mUser.getUData().getSlotBagUI();
        Integer slot = ItemSlotHelper.findSlotOf(slots, 0, bagCount, bonusType, rowId);
        if (slot != null) {
            ItemSlotHelper.clearPair(slots, slot);
            mUser.getResources().saveItemSlot(slots);
        }
    }
}
