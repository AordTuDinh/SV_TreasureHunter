package game.treasure.service.user;

import com.google.gson.JsonArray;
import game.config.CfgAchievement;
import game.config.CfgLottery;
import game.config.CfgMaterial;
import game.config.CfgServer;
import game.config.aEnum.*;
import game.config.lang.Lang;
import protocol.Pbmethod;
import game.treasure.mapping.*;
import game.treasure.mapping.main.ResItemEntity;
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
    /** User item: preview [4, storageType, itemKey]; receive [4, storageType, userItemId, itemKey]. storageType 1-4 = user_item.type */
    public static final int BONUS_ITEM = 4;
    public static final int BONUS_ARTIFACT = 5;
    public static final int BONUS_SKIN = 7;
    public static final int BONUS_VIP_EXP = 8;
    public static final int BONUS_PET = 9;
    public static final int BONUS_MOUNT = 10;
    public static final int BONUS_MATERIAL = 11;

    public static final Map<Integer, Integer> mTypeLength = new HashMap<>() {{
        put(BONUS_GOLD, 1);
        put(BONUS_GEM, 1);
        put(BONUS_RUBY, 1);
        put(BONUS_ITEM, 2); // preview mặc định: itemType (user_item.type), itemKey
        put(BONUS_ARTIFACT, 1);
        put(BONUS_SKIN, 2);
        put(BONUS_VIP_EXP, 1);
        put(BONUS_PET, 1);
        put(BONUS_MOUNT, 1);
        put(BONUS_MATERIAL, 2);
    }};

    public static List<Integer> bonusSinger = Arrays.asList(
            BONUS_ITEM, BONUS_ARTIFACT, BONUS_PET, BONUS_MOUNT, BONUS_SKIN, BONUS_MATERIAL);

    public static boolean isBonusSinger(int type) {
        return bonusSinger.contains(type);
    }

    public static List<Long> viewGold(long number) {
        return view(BONUS_GOLD, number);
    }

    public static List<Long> viewItem(int itemType, int itemId, long number) {
        if (number < 0) {
            List<Long> ret = new ArrayList<>();
            for (int i = 0; i < -number; i++) {
                ret.addAll(view(BONUS_ITEM, itemType, -itemId));
            }
            return ret;
        }
        return viewXNumber(view(BONUS_ITEM, itemType, itemId), (int) number);
    }

    public static List<Long> viewItemRemove(int itemType, long userItemId, int itemKey, int count) {
        List<Long> ret = new ArrayList<>();
        List<Long> one = view(BONUS_ITEM, itemType, userItemId, itemKey);
        for (int i = 0; i < count; i++) ret.addAll(one);
        return ret;
    }

    public static List<Long> viewItemMaterial(MaterialType type, long number) {
        List<Long> one = view(BONUS_ITEM, type.id);
        return viewXNumber(one, (int) number);
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

    public static List<Long> viewItem(int itemType, Pbmethod.ItemKey itemKey, long number) {
        int itemId = itemKey.getNumber();
        return viewXNumber(view(BONUS_ITEM, itemType, itemId), (int) number);
    }

    public static List<Long> viewDameSkin(int skinId) {
        return view(BONUS_SKIN, SkinType.DAMAGE_SKIN.value, skinId);
    }

    public static List<Long> viewItemEquipment(int itemId, int lock, long time) {
        return view(BONUS_ITEM, ItemType.EQUIPMENT.value, itemId);
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
        for (long value : values) aLong.add(value);
        return aLong;
    }

    public static int getIdItem(List<Long> bonus) {
        int type = Math.toIntExact(bonus.get(0));
        switch (type) {
            case BONUS_ITEM -> {
                return Math.toIntExact(bonus.get(bonus.size() - 1));
            }
            case BONUS_ARTIFACT -> {
                return Math.toIntExact(bonus.get(2));
            }
            case BONUS_SKIN -> {
                return Math.toIntExact(bonus.get(2));
            }
        }
        return 0;
    }

    static boolean isUserItemStorageType(int v) {
        return v >= ItemType.CONSUMABLE.value && v <= ItemType.EVENT.value;
    }

    static boolean isBonusWireType(int v) {
        return v == BONUS_GOLD || v == BONUS_GEM || v == BONUS_RUBY || v == BONUS_ITEM
                || v == BONUS_ARTIFACT || v == BONUS_SKIN || v == BONUS_VIP_EXP
                || v == BONUS_PET || v == BONUS_MOUNT || v == BONUS_MATERIAL;
    }

    /** Preview: [4,itemType,itemKey]. Trừ theo row: [4,itemType,userItemId,itemKey]. */
    static int itemBonusConfigLength(List<Long> bonus, int index) {
        if (index + 1 >= bonus.size()) return 0;
        int itemType = bonus.get(index).intValue();
        if (!isUserItemStorageType(itemType)) return mTypeLength.get(BONUS_ITEM);
        if (index + 2 >= bonus.size()) return 2;
        long second = bonus.get(index + 1);
        if (second > 1000 && index + 2 < bonus.size() && !isBonusWireType(bonus.get(index + 2).intValue()))
            return 3;
        return 2;
    }

    static int itemBonusConfigLength(JsonArray aBonus, int index) {
        if (index >= aBonus.size()) return 0;
        int itemType = aBonus.get(index).getAsInt();
        if (!isUserItemStorageType(itemType)) return mTypeLength.get(BONUS_ITEM);
        if (index + 1 >= aBonus.size()) return 2;
        if (index + 2 < aBonus.size()) {
            long second = aBonus.get(index + 1).getAsLong();
            if (second > 1000 && index + 2 < aBonus.size() && !isBonusWireType(aBonus.get(index + 2).getAsInt()))
                return 3;
        }
        return 2;
    }

    public static List<Long> viewXNumber(List<Long> bonus, int xNumber) {
        List<Long> ret = new ArrayList<>();
        if (isBonusSinger(Math.toIntExact(bonus.get(0)))) {
            int times = Math.abs(xNumber);
            for (int i = 0; i < times; i++) {
                ret.addAll(bonus);
            }
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
                case BONUS_ARTIFACT:
                    aLong.addAll(addItemArtifact(mUser, aBonus, index, detailAction));
                    break;
                case BONUS_VIP_EXP:
                    aLong.addAll(addVipExp(mUser, aBonus, index, detailAction));
                    break;
                case BONUS_SKIN:
                    aLong.addAll(addSkin(mUser, aBonus, index, detailAction));
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
            if (type == BONUS_ITEM) {
                index += itemBonusConfigLength(aBonus, index);
            } else {
                index += mTypeLength.getOrDefault(type, 0);
            }
        }
        return aLong;
    }

    static List<Long> addSkin(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        int type = aBonus.get(index++).getAsInt();
        System.out.println("type = " + type);
        int skinId = aBonus.get(index++).getAsInt();
        System.out.println("skinId = " + skinId);
        if (type == SkinType.DAMAGE_SKIN.value) {
            if (mUser.getUData().addDameSkin(skinId) && mUser.getUData().update(List.of("dame_skin", mUser.getUData().getDameSkin()))) {
                if (CfgServer.isRealServer())
                    Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction, "dameSkin", "skinId", skinId);
                return Arrays.asList((long) BONUS_SKIN, (long) type, (long) skinId);
            }
            return Arrays.asList((long) BONUS_SKIN, (long) type, (long) skinId);
        } else if (type == SkinType.CHAT_FRAME.value) {
            if (mUser.getUData().addChatFrame(skinId) && mUser.getUData().update(List.of("chat_frame", mUser.getUData().getChatFrame()))) {
                if (CfgServer.isRealServer())
                    Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction, "chatFrame", "frameId", skinId);
                return Arrays.asList((long) BONUS_SKIN, (long) type, (long) skinId);
            }
            return Arrays.asList((long) BONUS_SKIN, (long) type, (long) skinId);
        } else if (type == SkinType.TRIAL.value) {
            if (mUser.getUData().addEffectTrial(skinId) && mUser.getUData().update(List.of("list_trial", mUser.getUData().getListTrial()))) {
                if (CfgServer.isRealServer())
                    Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction, "list_trial", "trialId", skinId);
                return Arrays.asList((long) BONUS_SKIN, (long) type, (long) skinId);
            }
            return Arrays.asList((long) BONUS_SKIN, (long) type, (long) skinId);
        }
        return new ArrayList<>();
    }

    static List<Long> addUserItem(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        int itemType = aBonus.get(index++).getAsInt();
        int itemKey = aBonus.get(index++).getAsInt();
        if (itemKey < 0) {
            itemKey = -itemKey;
            if (!mUser.getResources().removeItemsByItemKey(itemKey, 1)) return new ArrayList<>();
            UserItemEntity left = mUser.getResources().getItemByItemKey(itemKey);
            long rowId = left != null ? left.getId() : 0L;
            int st = left != null ? left.getType() : itemType;
            return Arrays.asList((long) BONUS_ITEM, (long) st, rowId, (long) itemKey);
        }
        UserItemEntity uItem;
        if (itemKey == Pbmethod.ItemKey.TICKER_NORMAL.getNumber()) {
            uItem = checkGenItemData(mUser, 1);
            return Arrays.asList((long) BONUS_ITEM, (long) uItem.getType(), uItem.getId(), (long) itemKey);
        }
        ItemType type = ItemType.get(itemType);
        if (type == null) type = ItemType.CONSUMABLE;
        uItem = new UserItemEntity(mUser.getUser().getId(), itemKey, type);
        if (DBJPA.save(uItem)) {
            mUser.getResources().addItem(uItem);
            if (CfgServer.isRealServer()) {
                Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction,
                        "type", "user_item",
                        "id", uItem.getId(),
                        "itemId", itemKey,
                        "storageType", uItem.getType(),
                        "addValue", 1);
            }
            return Arrays.asList((long) BONUS_ITEM, (long) uItem.getType(), uItem.getId(), (long) itemKey);
        }
        return new ArrayList<>();
    }

    static List<Long> removeUserItem(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        int storageType = aBonus.get(index++).getAsInt();
        long userItemId = aBonus.get(index++).getAsLong();
        int itemKey = aBonus.get(index++).getAsInt();
        UserItemEntity uItem = mUser.getResources().getItem(userItemId);
        if (uItem == null || uItem.getItemId() != itemKey) return new ArrayList<>();
        if (uItem.isAggregatedItem()) {
            if (!mUser.getResources().removeItemsByItemKey(itemKey, 1)) return new ArrayList<>();
        } else if (!uItem.deleteFromDb()) {
            return new ArrayList<>();
        } else {
            mUser.getResources().removeItem(userItemId);
        }
        if (CfgServer.isRealServer()) {
            Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction,
                    "type", "user_item_remove", "id", userItemId, "itemId", itemKey);
        }
        return Arrays.asList((long) BONUS_ITEM, (long) storageType, userItemId, (long) itemKey);
    }

    static UserItemEntity checkGenItemData(MyUser mUser, int numItem) {
        UserItemEntity uItem = mUser.getResources().getItemByItemKey(Pbmethod.ItemKey.TICKER_NORMAL.getNumber());
        long eventDay = CfgLottery.getEventIdBuy();
        List<Long> nums = new ArrayList<>();
        for (int i = 0; i < numItem; i++) {
            nums.add(NumberUtil.getRandomLong(100000, 999999));
        }
        if (uItem == null) {
            uItem = new UserItemEntity(mUser.getUser().getId(), Pbmethod.ItemKey.TICKER_NORMAL.getNumber(), ItemType.EVENT);
            nums.add(0, eventDay);
            uItem.setData(StringHelper.toDBString(nums));
            DBJPA.save(uItem);
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
        if (CfgMaterial.get(materialId) == null) {
            return new ArrayList<>();
        }
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
                    if (chunk.size() >= 4) {
                        long userItemId = chunk.get(2);
                        int itemKey = chunk.get(3).intValue();
                        UserItemEntity uItem = mUser.getResources().getItem(userItemId);
                        ResItemEntity resItem = ResItem.getItem(itemKey);
                        String name = resItem != null ? resItem.getName()
                                : (ResItem.getItemEquipment(itemKey) != null ? ResItem.getItemEquipment(itemKey).getName() : "?");
                        if (uItem == null || uItem.getItemId() != itemKey)
                            return String.format(Lang.instance(mUser).get(Lang.err_not_enough_item), name);
                    } else if (chunk.size() >= 3) {
                        int itemKey = chunk.get(2).intValue();
                        if (itemKey < 0) {
                            itemDeduct.merge(-itemKey, 1, Integer::sum);
                        }
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
                int length = type == BONUS_ITEM
                        ? itemBonusConfigLength(bonus, index)
                        : mTypeLength.getOrDefault(type, 0);
                tmp.add((long) type);
                for (int i = index; i < index + length; i++) {
                    tmp.add(bonus.get(i));
                }
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
                for (int i = 0; i < times; i++) {
                    result.addAll(bo);
                }
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
}
