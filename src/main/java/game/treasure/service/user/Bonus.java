package game.treasure.service.user;

import com.google.gson.JsonArray;
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
        put(BONUS_PET, 1);
        put(BONUS_MOUNT, 1);
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

    public static List<Long> viewItem(int itemType, int itemId, long number) {
        if (ResItem.getItemEquipment(itemId) != null)
            return viewItemEquipment(itemId, 1);
        return viewItem(itemId, number);
    }

    public static List<Long> viewItemRemove(long userItemId, int itemKey, int count) {
        List<Long> ret = new ArrayList<>();
        List<Long> one = view(BONUS_ITEM, userItemId, itemKey);
        for (int i = 0; i < count; i++)
            ret.addAll(one);
        return ret;
    }

    public static List<Long> viewItemMaterial(MaterialType type, long number) {
        return viewXNumber(view(BONUS_ITEM, type.id), (int) number);
    }

    public static List<Long> viewPet(int itemId) {
        return view(BONUS_PET, itemId);
    }

    public static List<Long> viewMount(int mountId) {
        return view(BONUS_MOUNT, mountId);
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
                if (bonus.size() >= 4)
                    return Math.toIntExact(bonus.get(2));
                if (bonus.size() >= 3)
                    return Math.toIntExact(bonus.get(1));
                return Math.toIntExact(bonus.get(1));
            }
            case BONUS_EQUIPMENT -> {
                if (bonus.size() >= 4)
                    return Math.toIntExact(bonus.get(2));
                return Math.toIntExact(bonus.get(1));
            }
            case BONUS_ARTIFACT -> {
                return Math.toIntExact(bonus.get(2));
            }
            case BONUS_SKIN -> {
                return Math.toIntExact(bonus.get(1));
            }
            case BONUS_EFFECT_SKIN -> {
                return Math.toIntExact(bonus.get(2));
            }
        }
        return 0;
    }

    static boolean isBonusWireType(int v) {
        return v == BONUS_GOLD || v == BONUS_GEM || v == BONUS_RUBY || v == BONUS_ITEM
                || v == BONUS_EQUIPMENT || v == BONUS_ARTIFACT || v == BONUS_SKIN
                || v == BONUS_EFFECT_SKIN || v == BONUS_VIP_EXP
                || v == BONUS_PET || v == BONUS_MOUNT || v == BONUS_MATERIAL;
    }

    static int itemBonusPayloadLength(List<Long> bonus, int index) {
        if (index >= bonus.size())
            return 0;
        if (bonus.get(index) < 0)
            return 1;
        if (index + 2 >= bonus.size())
            return 1;
        if (isBonusWireType(bonus.get(index + 2).intValue()))
            return 1;
        return 2;
    }

    static int itemBonusPayloadLength(JsonArray aBonus, int index) {
        if (index >= aBonus.size())
            return 0;
        if (aBonus.get(index).getAsLong() < 0)
            return 1;
        if (index + 2 >= aBonus.size())
            return 1;
        if (isBonusWireType(aBonus.get(index + 2).getAsInt()))
            return 1;
        return 2;
    }

    static int equipmentBonusPayloadLength(List<Long> bonus, int index) {
        if (index >= bonus.size())
            return 0;
        if (index + 2 >= bonus.size())
            return Math.max(0, bonus.size() - index);
        if (index + 3 >= bonus.size())
            return 2;
        if (isBonusWireType(bonus.get(index + 3).intValue()))
            return 2;
        return 3;
    }

    static int equipmentBonusPayloadLength(JsonArray aBonus, int index) {
        if (index >= aBonus.size())
            return 0;
        if (index + 2 >= aBonus.size())
            return Math.max(0, aBonus.size() - index);
        if (index + 3 >= aBonus.size())
            return 2;
        if (isBonusWireType(aBonus.get(index + 3).getAsInt()))
            return 2;
        return 3;
    }

    static int bonusConfigLength(List<Long> bonus, int index, int type) {
        if (type == BONUS_ITEM)
            return itemBonusPayloadLength(bonus, index);
        if (type == BONUS_EQUIPMENT)
            return equipmentBonusPayloadLength(bonus, index);
        return mTypeLength.getOrDefault(type, 0);
    }

    static int bonusConfigLength(JsonArray aBonus, int index, int type) {
        if (type == BONUS_ITEM)
            return itemBonusPayloadLength(aBonus, index);
        if (type == BONUS_EQUIPMENT)
            return equipmentBonusPayloadLength(aBonus, index);
        return mTypeLength.getOrDefault(type, 0);
    }

    /** Preview chunk [4, itemKey] — không phải remove/receive [4, rowId, itemKey]. */
    static boolean isItemPreviewChunk(List<Long> chunk) {
        return chunk.size() == 2;
    }

    /** Preview chunk [12, itemKey, tier]. */
    static boolean isEquipmentPreviewChunk(List<Long> chunk) {
        return chunk.size() == 3;
    }

    public static ItemType resolveStorageType(int itemKey) {
        ResItemEntity res = ResItem.getItem(itemKey);
        if (res != null && res.getItemType() != null)
            return res.getItemType();
        return ItemType.CONSUMABLE;
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
        return receiveListItem(mUser, GsonUtil.parseJsonArray(aBonus.toString()), detailAction);
    }

    static List<Long> receiveListItem(MyUser mUser, JsonArray aBonus, String detailAction) {
        List<Long> aLong = new ArrayList<>();
        Integer index = 0;
        while (index < aBonus.size()) {
            int type = aBonus.get(index++).getAsInt();
            switch (type) {
                case BONUS_GOLD:
                    aLong.addAll(addGold(mUser, aBonus, index, detailAction));
                    break;
                case BONUS_GEM:
                    aLong.addAll(addGem(mUser, aBonus, index, detailAction));
                    break;
                case BONUS_RUBY:
                    aLong.addAll(addRuby(mUser, aBonus, index, detailAction));
                    break;
                case BONUS_ITEM:
                    aLong.addAll(addUserItem(mUser, aBonus, index, detailAction));
                    break;
                case BONUS_EQUIPMENT:
                    aLong.addAll(addUserEquipment(mUser, aBonus, index, detailAction));
                    break;
                case BONUS_ARTIFACT:
                    aLong.addAll(addItemArtifact(mUser, aBonus, index, detailAction));
                    break;
                case BONUS_VIP_EXP:
                    aLong.addAll(addVipExp(mUser, aBonus, index, detailAction));
                    break;
                case BONUS_SKIN:
                    aLong.addAll(addCharacterSkin(mUser, aBonus, index, detailAction));
                    break;
                case BONUS_EFFECT_SKIN:
                    aLong.addAll(addEffectSkin(mUser, aBonus, index, detailAction));
                    break;
                case BONUS_PET:
                    aLong.addAll(addPet(mUser, aBonus, index, detailAction));
                    break;
                case BONUS_MOUNT:
                    aLong.addAll(addMount(mUser, aBonus, index, detailAction));
                    break;
                case BONUS_MATERIAL:
                    aLong.addAll(addMaterial(mUser, aBonus, index, detailAction));
                    break;
            }
            if (type == BONUS_ITEM || type == BONUS_EQUIPMENT) {
                index += bonusConfigLength(aBonus, index, type);
            } else {
                index += mTypeLength.getOrDefault(type, 0);
            }
        }
        return aLong;
    }

    static List<Long> addCharacterSkin(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        int skinId = aBonus.get(index++).getAsInt();
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

    static List<Long> addEffectSkin(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        int type = aBonus.get(index++).getAsInt();
        int skinId = aBonus.get(index++).getAsInt();
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

    static List<Long> addUserItem(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        if (itemBonusPayloadLength(aBonus, index) == 2)
            return removeUserItem(mUser, aBonus, index, detailAction);
        int itemKey = aBonus.get(index++).getAsInt();
        if (itemKey < 0) {
            itemKey = -itemKey;
            if (!mUser.getResources().removeItemsByItemKey(itemKey, 1))
                return new ArrayList<>();
            UserItemEntity left = mUser.getResources().getItemByItemKey(itemKey);
            long rowId = left != null ? left.getId() : 0L;
            return Arrays.asList((long) BONUS_ITEM, rowId, (long) itemKey);
        }
        UserItemEntity uItem;
        if (itemKey == Pbmethod.ItemKey.TICKER_NORMAL.getNumber()) {
            uItem = checkGenItemData(mUser, 1);
            if (uItem == null) return new ArrayList<>();
            return Arrays.asList((long) BONUS_ITEM, uItem.getId(), (long) itemKey);
        }
        ItemType type = resolveStorageType(itemKey);
        uItem = new UserItemEntity(mUser.getUser().getId(), itemKey, type);
        if (!prepareNewItemSlot(mUser, BONUS_ITEM, 0, type))
            return new ArrayList<>();
        if (type == ItemType.CONSUMABLE)
            ResItem.initConsumableInstanceData(uItem);
        if (DBJPA.save(uItem)) {
            if (!prepareNewItemSlot(mUser, BONUS_ITEM, uItem.getId(), type)) {
                uItem.deleteFromDb();
                return new ArrayList<>();
            }
            mUser.getResources().addItem(uItem);
            if (type == ItemType.CONSUMABLE && CfgItem.isItemMedicine(itemKey))
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

    static List<Long> addUserEquipment(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        if (equipmentBonusPayloadLength(aBonus, index) == 3)
            return removeUserEquipment(mUser, aBonus, index, detailAction);
        int itemKey = aBonus.get(index++).getAsInt();
        int configTier = aBonus.get(index++).getAsInt();
        if (ResItem.getItemEquipment(itemKey) == null)
            return new ArrayList<>();
        UserEquipmentEntity uEquip = new UserEquipmentEntity(mUser.getUser().getId(), itemKey);
        uEquip.setTier(configTier);
        if (!prepareNewItemSlot(mUser, BONUS_EQUIPMENT, 0, ItemType.EQUIPMENT))
            return new ArrayList<>();
        EquipmentStatRollService.rollStatsIfNeeded(uEquip);
        if (DBJPA.save(uEquip)) {
            if (!prepareNewItemSlot(mUser, BONUS_EQUIPMENT, uEquip.getId(), ItemType.EQUIPMENT)) {
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

    static List<Long> removeUserItem(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        long userItemId = aBonus.get(index++).getAsLong();
        int itemKey = aBonus.get(index++).getAsInt();
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
        return Arrays.asList((long) BONUS_ITEM, userItemId, (long) itemKey);
    }

    static List<Long> removeUserEquipment(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        long userEquipId = aBonus.get(index++).getAsLong();
        int itemKey = aBonus.get(index++).getAsInt();
        int tier = aBonus.get(index++).getAsInt();
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
        return Arrays.asList((long) BONUS_EQUIPMENT, userEquipId, (long) itemKey, (long) uEquip.getTier());
    }

    static UserItemEntity checkGenItemData(MyUser mUser, int numItem) {
        UserItemEntity uItem = mUser.getResources().getItemByItemKey(Pbmethod.ItemKey.TICKER_NORMAL.getNumber());
        long eventDay = CfgLottery.getEventIdBuy();
        List<Long> nums = new ArrayList<>();
        for (int i = 0; i < numItem; i++)
            nums.add(NumberUtil.getRandomLong(100000, 999999));
        if (uItem == null) {
            uItem = new UserItemEntity(mUser.getUser().getId(), Pbmethod.ItemKey.TICKER_NORMAL.getNumber(), ItemType.EVENT);
            if (!prepareNewItemSlot(mUser, BONUS_ITEM, 0, ItemType.EVENT))
                return null;
            nums.add(0, eventDay);
            uItem.setData(StringHelper.toDBString(nums));
            if (!DBJPA.save(uItem))
                return null;
            mUser.getResources().addItem(uItem);
            prepareNewItemSlot(mUser, BONUS_ITEM, uItem.getId(), ItemType.EVENT);
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

    static List<Long> addItemArtifact(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        int artifactId = aBonus.get(index++).getAsInt();
        UserArtifactEntity uArtifact = new UserArtifactEntity(mUser.getUser().getId(), artifactId);
        if (DBJPA.save(uArtifact)) {
            mUser.getResources().addArtifact(uArtifact);
            if (CfgServer.isRealServer())
                Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction, "type", "artifact", "id", uArtifact.getId(), "artifactId", artifactId);
            return Arrays.asList((long) BONUS_ARTIFACT, uArtifact.getId(), (long) artifactId);
        }
        return new ArrayList<>();
    }

    static List<Long> addVipExp(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        int addExp = aBonus.get(index++).getAsInt();
        UserEntity user = mUser.getUser();
        mUser.getUser().addVipExp(addExp);
        if (DBJPA.update("user", Arrays.asList("vip_exp", user.getVipExp(), "vip", user.getVip()), Arrays.asList("id", user.getId()))) {
            Actions.save(user, Actions.GRECEIVE, detailAction, "type", "vip_exp", "vip", user.getVip(), "exp", user.getVipExp(), "addExp", addExp);
            return Arrays.asList((long) BONUS_VIP_EXP, (long) user.getVipExp(), (long) addExp, (long) user.getVip());
        }
        return new ArrayList<>();
    }

    static List<Long> addPet(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        int petId = aBonus.get(index++).getAsInt();
        UserPetEntity uPet = new UserPetEntity(mUser.getUser(), petId);
        if (DBJPA.save(uPet)) {
            mUser.getResources().addPet(uPet);
            if (CfgServer.isRealServer()) {
                Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction,
                        "type", "pet", "id", uPet.getId(), "petId", petId);
            }
            return Arrays.asList((long) BONUS_PET, uPet.getId(), (long) petId);
        }
        return new ArrayList<>();
    }

    static List<Long> addMount(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        int mountId = aBonus.get(index++).getAsInt();
        if (ResMount.get(mountId) == null) return new ArrayList<>();
        UserMountEntity uMount = new UserMountEntity(mUser.getUser(), mountId);
        if (DBJPA.save(uMount)) {
            mUser.getResources().addMount(uMount);
            if (CfgServer.isRealServer()) {
                Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction,
                        "type", "mount", "id", uMount.getId(), "mountId", mountId);
            }
            return Arrays.asList((long) BONUS_MOUNT, uMount.getId(), (long) mountId);
        }
        return new ArrayList<>();
    }

    static List<Long> addMaterial(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        int materialId = aBonus.get(index++).getAsInt();
        int rank = aBonus.get(index++).getAsInt();
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

    static List<Long> addGold(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        long value = aBonus.get(index++).getAsLong();
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

    static List<Long> addGem(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        long value = aBonus.get(index++).getAsLong();
        if (dbAddGem(mUser.getUser(), value)) {
            mUser.getUser().addGem(value);
            CfgAchievement.addListAchievement(mUser, 5, CfgAchievement.addGem, (int) value);
            if (CfgServer.isRealServer())
                Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction, "type", "gem", "value", mUser.getUser().getGem(), "addValue", value);
            return Arrays.asList((long) BONUS_GEM, mUser.getUser().getGem(), value);
        }
        return new ArrayList<>();
    }

    static List<Long> addRuby(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        long value = aBonus.get(index++).getAsLong();
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
        for (List<Long> chunk : parse(aBonus)) {
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
                    if (chunk.size() >= 3) {
                        long userItemId = chunk.get(1);
                        int itemKey = chunk.get(2).intValue();
                        UserItemEntity uItem = mUser.getResources().getItem(userItemId);
                        ResItemEntity resItem = ResItem.getItem(itemKey);
                        String name = resItem != null ? resItem.getName() : "?";
                        if (uItem == null || uItem.getItemId() != itemKey)
                            return String.format(Lang.instance(mUser).get(Lang.err_not_enough_item), name);
                    } else if (chunk.size() >= 2) {
                        int itemKey = chunk.get(1).intValue();
                        if (itemKey < 0)
                            itemDeduct.merge(-itemKey, 1, Integer::sum);
                    }
                    break;
                case BONUS_EQUIPMENT:
                    if (chunk.size() >= 4) {
                        long userEquipId = chunk.get(1);
                        int itemKey = chunk.get(2).intValue();
                        UserEquipmentEntity uEquip = mUser.getResources().getEquipment(userEquipId);
                        String name = ResItem.getItemEquipment(itemKey) != null
                                ? ResItem.getItemEquipment(itemKey).getName() : "?";
                        if (uEquip == null || uEquip.getItemId() != itemKey)
                            return String.format(Lang.instance(mUser).get(Lang.err_not_enough_item), name);
                    }
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

    public static boolean isBonusType(List<Long> bonus, int bonusType) {
        return bonus.get(0) == bonusType;
    }

    public static List<List<Long>> parse(List<Long> bonus) {
        List<List<Long>> result = new ArrayList<>();
        if (bonus != null && !bonus.isEmpty()) {
            int index = 0;
            while (index < bonus.size()) {
                List<Long> tmp = new ArrayList<>();
                int type = bonus.get(index++).intValue();
                int length = bonusConfigLength(bonus, index, type);
                tmp.add((long) type);
                for (int i = index; i < index + length; i++)
                    tmp.add(bonus.get(i));
                result.add(tmp);
                index += length;
            }
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

    /** Gán slot trong user_data.item_slot. {@code bonusType} = wire bonus (4, 12, …); {@code slotZoneType} chỉ chọn vùng bag vs event. */
    public static boolean prepareNewItemSlot(MyUser mUser, int bonusType, long rowId, ItemType slotZoneType) {
        List<Long> slots = mUser.getUData().getItemSlotList();
        int bagCount = mUser.getUData().getSlotBagUI();
        int eventCount = mUser.getUData().getSlotEvent();
        if (slotZoneType == ItemType.EVENT) {
            if (rowId == 0)
                return mUser.getResources().canAddEventItem(1);
            if (!mUser.getResources().canAddEventItem(1))
                return false;
            Integer slot = ItemSlotHelper.findFirstEmpty(slots, bagCount, eventCount);
            if (slot == null)
                return false;
            ItemSlotHelper.setPair(slots, slot, bonusType, rowId);
        } else {
            if (rowId == 0)
                return mUser.getResources().canAddBagItem(1);
            if (!mUser.getResources().canAddBagItem(1))
                return false;
            Integer slot = ItemSlotHelper.findFirstEmpty(slots, 0, bagCount);
            if (slot == null)
                return false;
            ItemSlotHelper.setPair(slots, slot, bonusType, rowId);
        }
        return mUser.getUData().saveItemSlot(slots);
    }

    public static void clearItemFromSlot(MyUser mUser, int bonusType, long rowId) {
        List<Long> slots = mUser.getUData().getItemSlotList();
        int bagCount = mUser.getUData().getSlotBagUI();
        int eventCount = mUser.getUData().getSlotEvent();
        Integer bagSlot = ItemSlotHelper.findSlotOf(slots, 0, bagCount, bonusType, rowId);
        if (bagSlot != null) {
            ItemSlotHelper.clearPair(slots, bagSlot);
            mUser.getUData().saveItemSlot(slots);
            return;
        }
        Integer eventSlot = ItemSlotHelper.findSlotOf(slots, bagCount, eventCount, bonusType, rowId);
        if (eventSlot != null) {
            ItemSlotHelper.clearPair(slots, eventSlot);
            mUser.getUData().saveItemSlot(slots);
        }
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
        if (!mUser.getUData().saveItemSlot(slots))
            return false;
        equip.setBagSlot(slot);
        return true;
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
}
