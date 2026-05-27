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
    public static final int BONUS_EQUIPMENT = 4;
    public static final int BONUS_ARTIFACT = 5;
    public static final int BONUS_ITEM = 6;
    public static final int BONUS_SKIN = 7; //type -id
    public static final int BONUS_VIP_EXP = 8;
    public static final int BONUS_PET = 9;
    public static final int BONUS_MOUNT = 10;
    public static final int BONUS_MATERIAL = 11;



    // type length for parsing VIEW-format bonus input (type + values...)
    public static final Map<Integer, Integer> mTypeLength = new HashMap<>() {{
        put(BONUS_GOLD, 1);      // addGold
        put(BONUS_GEM, 1);       // addGem
        put(BONUS_RUBY, 1);      // addRuby
        put(BONUS_EQUIPMENT, 1);
        put(BONUS_ARTIFACT, 1);
        put(BONUS_ITEM, 1);
        put(BONUS_SKIN, 2);      // type, skinId
        put(BONUS_VIP_EXP, 1);   // addExp
        put(BONUS_PET, 1);       // petId
        put(BONUS_MOUNT, 1);     // mountId
        put(BONUS_MATERIAL, 2);  // materialId, rank (1=Common..4=Legend)
    }};

    // Receive-format: [BONUS_PET, id, petId] / [BONUS_MOUNT, id, mountId]

    public static List<Integer> bonusSinger = Arrays.asList(BONUS_EQUIPMENT, BONUS_ARTIFACT, BONUS_ITEM, BONUS_PET, BONUS_MOUNT, BONUS_SKIN,BONUS_MATERIAL);

    public static boolean isBonusSinger(int type) {
        return bonusSinger.contains(type);
    }


    public static List<Long> viewGold(long number) {
        return view(BONUS_GOLD, number);
    }

    public static List<Long> viewItem(int itemId, long number) {
        // new format: material is "singer" by itemKey; quantity is represented by repeating entries
        List<Long> one = view(BONUS_ITEM, itemId);
        return viewXNumber(one, (int) number);
    }

    public static List<Long> viewItemMaterial(MaterialType type, long number) {
        List<Long> one = view(BONUS_ITEM, type.id);
        return viewXNumber(one, (int) number);
    }

    public static List<Long> viewPet( int itemId) {
        return view(BONUS_PET,  itemId);
    }

    public static List<Long> viewMount(int mountId) {
        return view(BONUS_MOUNT, mountId);
    }

    // Preview / send: [BONUS_PET, petId] / [BONUS_MOUNT, mountId]
    // Receive: [BONUS_PET, id, petId] / [BONUS_MOUNT, id, mountId]
    // Preview / send: [BONUS_MATERIAL, materialId, rank].
    // Receive: [BONUS_MATERIAL, rowId, materialId, rank].
    public static List<Long> viewMaterial(int materialId, int rank) {
        return view(BONUS_MATERIAL, materialId, rank);
    }

    public static List<Long> viewMaterial(int materialId) {
        return viewMaterial(materialId, 1);
    }

    public static List<Long> viewItem(Pbmethod.ItemKey itemKey, long number) {
        List<Long> one = view(BONUS_ITEM, itemKey.getNumber());
        return viewXNumber(one, (int) number);
    }


    public static List<Long> viewDameSkin(int skinId) {
        return view(BONUS_SKIN, SkinType.DAMAGE_SKIN.value, skinId);
    }


    public static List<Long> viewItemEquipment(int itemId, int lock, long time) {
        // new format: only itemKey; lock/time are ignored
        return view(BONUS_EQUIPMENT, itemId);
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
        for (int i = 0; i < values.length; i++) aLong.add(values[i]);
        return aLong;
    }

    // read id item
    public static int getIdItem(List<Long> bonus) {
        int type = Math.toIntExact(bonus.get(0));
        switch (type) {
            case BONUS_ITEM, BONUS_EQUIPMENT, BONUS_ARTIFACT -> {
                return Math.toIntExact(bonus.get(1));
            }
            case BONUS_SKIN -> {
                return Math.toIntExact(bonus.get(2));
            }
        }
        return 0;
    }

    public static List<Long> viewXNumber(List<Long> bonus, int xNumber) {
        List<Long> ret = new ArrayList<>();
        if (isBonusSinger(Math.toIntExact(bonus.get(0)))) {
            for (int i = 0; i < xNumber; i++) {
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
                case BONUS_EQUIPMENT:
                    aLong.addAll(addItemEquipment(mUser, aBonus, index, detailAction));
                    break;
                case BONUS_ARTIFACT:
                    aLong.addAll(addItemArtifact(mUser, aBonus, index, detailAction));
                    break;
                case BONUS_VIP_EXP:
                    aLong.addAll(addVipExp(mUser, aBonus, index, detailAction));
                    break;
                case BONUS_ITEM:
                    aLong.addAll(addItemMaterial(mUser, aBonus, index, detailAction));
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
            index += mTypeLength.getOrDefault(type, 0);
        }
        return aLong;
    }
    //endregion

    //region Logic

    static List<Long> addSkin(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        int type = aBonus.get(index++).getAsInt();
        int skinId = aBonus.get(index++).getAsInt();
        if (type == SkinType.DAMAGE_SKIN.value) {
            if (mUser.getUData().addDameSkin(skinId) && mUser.getUData().update(List.of("dame_skin", mUser.getUData().getDameSkin()))) {
                if (CfgServer.isRealServer())
                    Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction, "dameSkin", "skinId", skinId);
                return Arrays.asList((long) BONUS_SKIN, (long) type, (long) skinId);
            }
            return new ArrayList<>();
        } else if (type == SkinType.CHAT_FRAME.value) {
            if (mUser.getUData().addChatFrame(skinId) && mUser.getUData().update(List.of("chat_frame", mUser.getUData().getChatFrame()))) {
                if (CfgServer.isRealServer())
                    Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction, "chatFrame", "frameId", skinId);
                return Arrays.asList((long) BONUS_SKIN, (long) type, (long) skinId);
            }
            return new ArrayList<>();
        } else if (type == SkinType.TRIAL.value) {
            if (mUser.getUData().addEffectTrial(skinId) && mUser.getUData().update(List.of("list_trial", mUser.getUData().getListTrial()))) {
                if (CfgServer.isRealServer())
                    Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction, "list_trial", "trialId", skinId);
                return Arrays.asList((long) BONUS_SKIN, (long) type, (long) skinId);
            }
            return new ArrayList<>();
        }
        return new ArrayList<>();
    }

    static List<Long> addItemMaterial(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        int itemKey = aBonus.get(index++).getAsInt();
        int addNumber = 1;
        UserItemEntity uItem;
        if (itemKey == Pbmethod.ItemKey.TICKER_NORMAL.getNumber()) uItem = checkGenItemData(mUser, addNumber);
        else {
            uItem = mUser.getResources().getItem(itemKey);
            if (uItem == null) uItem = new UserItemEntity(mUser.getUser().getId(), itemKey, addNumber);
            else uItem.add(addNumber);
            if (uItem.getNumber() < 0) return new ArrayList<>();
        }
        boolean isOk = DBJPA.saveOrUpdate(uItem);
        if (isOk) {
            if (!mUser.getResources().hasItem(uItem.getItemId())) mUser.getResources().addItem(uItem);
            if (CfgServer.isRealServer()) {
                Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction,
                        "type", "item",
                        "itemId", itemKey,
                        "value", uItem.getNumber(),
                        "addValue", addNumber);
            }
            // receive-format: [type, itemKey]
            return Arrays.asList((long) BONUS_ITEM, (long) itemKey);
        }
        return new ArrayList<>();
    }

    static UserItemEntity checkGenItemData(MyUser mUser, int numItem) {
        UserItemEntity uItem = mUser.getResources().getItem(Pbmethod.ItemKey.TICKER_NORMAL.getNumber());
        long eventDay = CfgLottery.getEventIdBuy();
        List<Long> nums = new ArrayList<>();
        for (int i = 0; i < numItem; i++) {
            nums.add(NumberUtil.getRandomLong(100000, 999999));
        }
        if (uItem == null) {
            uItem = new UserItemEntity(mUser.getUser().getId(), Pbmethod.ItemKey.TICKER_NORMAL, numItem);
            nums.add(0, eventDay);
            uItem.setData(StringHelper.toDBString(nums));
        } else {
            // check vé cũ cần xóa dữ liệu đi
            String data = uItem.getData();
            List<Long> dataSticker = GsonUtil.strToListLong(data == null ? "[]" : data);
            if (dataSticker.isEmpty() || dataSticker.get(0) != eventDay) {
                dataSticker = new ArrayList<>();
                dataSticker.add(eventDay);
                uItem.setNumber(0);
            }
            uItem.add(numItem);
            dataSticker.addAll(nums);
            uItem.setData(StringHelper.toDBString(dataSticker));
        }
        return uItem;
    }


    static List<Long> addItemEquipment(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        int itemKey = aBonus.get(index++).getAsInt();
        UserItemEquipmentEntity uItemEquip = new UserItemEquipmentEntity(mUser.getUser().getId(), itemKey);
        if (DBJPA.save(uItemEquip)) {
            mUser.getResources().addItemEquip(uItemEquip);
            if (CfgServer.isRealServer())
                Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction, "type", "item_equipment", "id", uItemEquip.getId(), "itemKey", itemKey);
            // receive-format: [type, itemKey]
            return Arrays.asList((long) BONUS_EQUIPMENT,uItemEquip.getId(), (long) itemKey);
        }
        return new ArrayList<>();
    }

    static List<Long> addItemArtifact(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        int artifactId = aBonus.get(index++).getAsInt();
        UserArtifactEntity uArtifact = new UserArtifactEntity(mUser.getUser().getId(), artifactId);
        if (DBJPA.save(uArtifact)) {
            mUser.getResources().addArtifact(uArtifact);
            if (CfgServer.isRealServer())
                Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction, "type", "artifact", "id", uArtifact.getId(), "artifactId", artifactId);
            // receive-format: [type, userArtifactId, artifactId]
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
            // receive-format: [type, curExp, addExp, vip]
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

    // Send-format: [BONUS_MATERIAL, materialId, rank]
    // Receive-format: [BONUS_MATERIAL, id, materialId, rank]
    static List<Long> addMaterial(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        int materialId = aBonus.get(index++).getAsInt();
        int rank = aBonus.get(index++).getAsInt();
        game.treasure.mapping.main.ResMaterialEntity res = CfgMaterial.get(materialId);
        if (res == null) {
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

    //endregion

    //region Database
    static boolean dbAddGold(UserEntity user, long addGold) {
        return DBJPA.update("user", Arrays.asList("gem", user.getGem(), "gold", user.getGold() + addGold), Arrays.asList("id", user.getId()));
    }

    static boolean dbAddGem(UserEntity user, long addGem) {
        return DBJPA.update("user", Arrays.asList("gem", user.getGem() + addGem, "gold", user.getGold()), Arrays.asList("id", user.getId()));

    }

    static boolean dbAddRuby(UserEntity user, long addRuby) {
        return DBJPA.update("user", Arrays.asList("ruby", user.getRuby() + addRuby), Arrays.asList("id", user.getId()));

    }

    //endregion

    public static String checkMoney(MyUser mUser, List<Long> aBonus) {
        int index = 0;
        while (index < aBonus.size()) {
            int type = aBonus.get(index++).intValue();
            switch (type) {
                case BONUS_GOLD:
                    if (mUser.getUser().getGold() + aBonus.get(index++) < 0)
                        return Lang.instance(mUser).get(Lang.err_not_enough_gold);
                    break;
                case BONUS_GEM:
                    if (mUser.getUser().getGem() + aBonus.get(index++) < 0)
                        return Lang.instance(mUser).get(Lang.err_not_enough_gem);
                    break;
                case BONUS_RUBY:
                    if (mUser.getUser().getRuby() + aBonus.get(index++) < 0)
                        return Lang.instance(mUser).get(Lang.err_not_enough_ruby);
                    break;
                case BONUS_ITEM:
                    int itemId = aBonus.get(index++).intValue();
                    UserItemEntity uItem = mUser.getResources().getItem(itemId);
                    ResItemEntity resItem = ResItem.getItem(itemId);
                    if (uItem == null)
                        return String.format(Lang.instance(mUser).get(Lang.err_not_enough_item), resItem.getName());
                    if (uItem.getNumber() + aBonus.get(index++) < 0)
                        return String.format(Lang.instance(mUser).get(Lang.err_not_enough_item), resItem.getName());
                    break;
            }
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
                            if (bonus.get(index).longValue() != childBonus.get(index).longValue()) isOk = false;
                        }
                        if (isOk) {
                            childBonus.set(childBonus.size() - 1, childBonus.get(childBonus.size() - 1) + bonus.get(bonus.size() - 1));
                            include = true;
                            break;
                        }
                    }
                }
                if (!include) {
                    ret.add(bonus);
                }
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
                int length = mTypeLength.getOrDefault(type, 0);
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
        for (int i = 0; i < aBonus.size(); i++) {
            List<Long> bm = aBonus.get(i);
            if (!bonusSinger.contains(bm.get(0).intValue())) { // chỉ đảo ngược bonus có số lượng
                int last = bm.size() - 1;
                bm.set(last, -bm.get(last));
                ret.addAll(bm);
            }
        }
        if (ret.isEmpty()) return null;
        else return ret;
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

    // per100 vd : 120% = 120
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
