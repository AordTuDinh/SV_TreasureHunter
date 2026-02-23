package game.dragonhero.service.user;

import com.google.gson.JsonArray;
import game.config.CfgAchievement;
import game.config.CfgLottery;
import game.config.CfgServer;
import game.config.aEnum.PetType;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.dragonhero.mapping.*;
import game.dragonhero.mapping.main.ResItemEntity;
import game.dragonhero.service.resource.ResItem;
import game.object.MyUser;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.StringHelper;

import java.util.*;

public class Bonus {
    public static final int BONUS_GOLD = 1;
    public static final int BONUS_GEM = 2;
    public static final int BONUS_ITEM_EQUIPMENT = 3;
    public static final int BONUS_EXP = 4;
    public static final int BONUS_HERO = 5; // hero id
    public static final int BONUS_ITEM = 6;
    public static final int BONUS_WEAPON = 7;
    public static final int BONUS_AVATAR = 8;
    public static final int BONUS_VIP_EXP = 9;
    public static final int BONUS_PET = 10; // type - id
    public static final int BONUS_SKIN = 11; //type -id
    public static final int BONUS_ITEM_FARM = 12;
    //    public static final int BONUS_ITEM_POINT = 13;
    public static final int BONUS_PIECE = 14;
    public static final int BONUS_RUBY = 15;

    public static final Map<Integer, Integer> mTypeLength = new HashMap<>() {{
        put(BONUS_GOLD, 1);
        put(BONUS_GEM, 1);
        put(BONUS_ITEM_EQUIPMENT, 3);
        put(BONUS_EXP, 1);
        put(BONUS_HERO, 1);
        put(BONUS_ITEM, 2);
        put(BONUS_WEAPON, 1);
        put(BONUS_AVATAR, 2);
        put(BONUS_VIP_EXP, 1);
        put(BONUS_PET, 2);
        put(BONUS_SKIN, 2);
        put(BONUS_ITEM_FARM, 3);
//        put(BONUS_ITEM_POINT, 2);
        put(BONUS_PIECE, 3);
        put(BONUS_RUBY, 1);
    }};

    public static List<Integer> bonusSinger = Arrays.asList(BONUS_ITEM_EQUIPMENT, BONUS_HERO, BONUS_AVATAR, BONUS_WEAPON, BONUS_PET, BONUS_SKIN);


    public static boolean isBonusSinger(int type) {
        return bonusSinger.contains(type);
    }

    public static List<Long> viewHeroAvatar(int heroId) {
        return view(BONUS_AVATAR, 0, heroId);
    }

    public static List<Long> viewGold(long number) {
        return view(BONUS_GOLD, number);
    }

    public static List<Long> viewItem(int itemId, long number) {
        return view(BONUS_ITEM, itemId, number);
    }

    public static List<Long> viewItemMaterial(MaterialType type, long number) {
        return view(BONUS_ITEM, type.id, number);
    }

    public static List<Long> viewItem(ItemKey itemKey, long number) {
        return view(BONUS_ITEM, itemKey.id, number);
    }

    public static List<Long> viewWeapon(long id) {
        return view(BONUS_WEAPON, id);
    }

    public static List<Long> viewItemFarm(int itemFarmType, int itemId, long number) {
        return view(BONUS_ITEM_FARM, itemFarmType, itemId, number);
    }

    public static List<Long> viewItemFarm(ItemFarmType farmType, int itemId, long number) {
        return viewItemFarm(farmType.value, itemId, number);
    }

    public static List<Long> viewItemFarm(UserItemFarmEntity iFarm, long number) {
        return viewItemFarm(iFarm.getType(), iFarm.getId(), number);
    }

    public static List<Long> viewPiece(int pieceType, int itemId, long number) {
        return view(BONUS_PIECE, pieceType, itemId, number);
    }

    public static List<Long> viewPiece(PieceType type, int itemId, long number) {
        return view(BONUS_PIECE, type.value, itemId, number);
    }

    public static List<Long> viewPet(PetType petType, int itemId) {
        return viewPet(petType.value, itemId);
    }

    public static List<Long> viewPet(int petType, int itemId) {
        return view(BONUS_PET, petType, itemId);
    }

    public static List<Long> viewDameSkin(int skinId) {
        return view(BONUS_SKIN, SkinType.DAMAGE_SKIN.value, skinId);
    }

//    public static List<Long> viewItemPoint(int itemId, long number) {
//        return view(BONUS_ITEM_POINT, itemId, number);
//    }


    public static List<Long> viewItemEquipment(int itemId, int lock, long time) {
        return view(BONUS_ITEM_EQUIPMENT, itemId, lock, time);
    }


    public static List<Long> viewGem(int number) {
        return view(BONUS_GEM, number);
    }

    public static List<Long> viewRuby(int number) {
        return view(BONUS_RUBY, number);
    }

    public static List<Long> viewExp(long number) {
        return view(BONUS_EXP, number);
    }

    public static List<Long> viewVipExp(long number) {
        return view(BONUS_VIP_EXP, number);
    }


    public static List<Long> view(int bonusType, long... values) {
        List<Long> aLong = new ArrayList<>();
        aLong.add((long) bonusType);
        switch (bonusType) {
            case BONUS_GOLD:
            case BONUS_GEM:
            case BONUS_ITEM_EQUIPMENT:
            case BONUS_HERO:
            case BONUS_EXP:
            case BONUS_ITEM:
            case BONUS_WEAPON:
            case BONUS_VIP_EXP:
            case BONUS_PET:
            case BONUS_SKIN:
            case BONUS_ITEM_FARM:
            case BONUS_PIECE:
            case BONUS_RUBY:
                for (int i = 0; i < values.length; i++) aLong.add((long) values[i]);
                break;
            default:
                for (int i = 0; i < values.length; i++) aLong.add((long) values[i]);
                break;
        }
        return aLong;
    }

    // read id item
    public static int getIdItem(List<Long> bonus) {
        int type = Math.toIntExact(bonus.get(0));
        switch (type) {
            case BONUS_ITEM, BONUS_ITEM_EQUIPMENT, BONUS_HERO, BONUS_WEAPON, BONUS_PET -> {
                return Math.toIntExact(bonus.get(1));
            }
            case BONUS_AVATAR, BONUS_SKIN, BONUS_ITEM_FARM, BONUS_PIECE -> {
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
                case BONUS_ITEM_EQUIPMENT:
                    aLong.addAll(addItemEquipment(mUser, aBonus, index, detailAction));
                    break;
                case BONUS_HERO:
                    aLong.addAll(addHero(mUser, aBonus, index, detailAction));
                    break;
                case BONUS_EXP:
                    aLong.addAll(addUserExp(mUser, aBonus, index, detailAction));
                    break;
                case BONUS_VIP_EXP:
                    aLong.addAll(addVipExp(mUser, aBonus, index, detailAction));
                    break;
                case BONUS_ITEM:
                    aLong.addAll(addItem(mUser, aBonus, index, detailAction));
                    break;
                case BONUS_WEAPON:
                    aLong.addAll(addWeapon(mUser, aBonus, index, detailAction));
                    break;
                case BONUS_AVATAR:
                    aLong.addAll(addAvatar(mUser, aBonus, index, detailAction));
                    break;
                case BONUS_PET:
                    aLong.addAll(addPet(mUser, aBonus, index, detailAction));
                    break;
                case BONUS_SKIN:
                    aLong.addAll(addSkin(mUser, aBonus, index, detailAction));
                    break;
                case BONUS_ITEM_FARM:
                    aLong.addAll(addItemFarm(mUser, aBonus, index, detailAction));
                    break;
//                case BONUS_ITEM_POINT:
//                    aLong.addAll(addItemPoint(mUser, aBonus, index, detailAction));
//                    break;
                case BONUS_PIECE:
                    aLong.addAll(addPiece(mUser, aBonus, index, detailAction));
                    break;
                case BONUS_RUBY:
                    aLong.addAll(addRuby(mUser, aBonus, index, detailAction));
                    break;
            }
            index += mTypeLength.containsKey(type) ? mTypeLength.get(type) : 0;
        }
        return aLong;
    }
    //endregion

    //region Logic

    static List<Long> addAvatar(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        int typeId = aBonus.get(index++).getAsInt();
        int avatarId = aBonus.get(index++).getAsInt();

        if (DBJPA.saveOrUpdate(new UserAvatarEntity(mUser.getUser().getId(), avatarId, typeId))) {
            Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction, "type", "avatar", "typeId", typeId, "id", avatarId);
            return Arrays.asList((long) BONUS_AVATAR, (long) typeId, (long) avatarId);
        }
        return new ArrayList<>();
    }

    static List<Long> addSkin(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        int type = aBonus.get(index++).getAsInt();
        int skinId = aBonus.get(index++).getAsInt();
        if (type == SkinType.CHARACTER.value) {
//            if (mUser.getResources().getHero(heroKey) == null) return new ArrayList<>();
//            UserHeroEntity uHero = new UserHeroEntity(mUser.getUser().getId(), heroKey);
//            // yêu cầu sở hữu hero trước
//            if (uHero == null) return new ArrayList<>();
//            uHero.addSkin(skinId);
//            if (DBJPA.save(uHero)) {
//                mUser.getResources().addHero(uHero);
//                if (CfgServer.isRealServer())
//                    Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction, "heroKey", "skinId", heroKey, skinId);
//                return Arrays.asList((long) BONUS_SKIN, (long) heroKey, (long) skinId);
//            }
//            return new ArrayList<>();
        } else if (type == SkinType.DAMAGE_SKIN.value) {
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

    static List<Long> addItem(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        int itemId = aBonus.get(index++).getAsInt();
        int number = aBonus.get(index++).getAsInt();
        UserItemEntity uItem =null;
        if(itemId==ItemKey.TICKER_NORMAL.id) uItem =  checkGenItemData(mUser, number);
        else  {
            uItem = mUser.getResources().getItem(itemId);
            if (uItem == null) uItem = new UserItemEntity(mUser.getUser().getId(), itemId, number);
            else uItem.add(number);
            if (uItem.getNumber() < 0) return new ArrayList<>();

        }
        boolean isOk = DBJPA.saveOrUpdate(uItem);
        if (isOk) {
            if (!mUser.getResources().hasItem(uItem.getItemId())) mUser.getResources().addItem(uItem);
            if (CfgServer.isRealServer())
                Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction, "type", "item", "itemId", itemId, "value", uItem.getNumber(), "addValue", number);
            return Arrays.asList((long) BONUS_ITEM, (long) uItem.getItemId(), (long) uItem.getNumber(), (long) number);
        }
        return new ArrayList<>();
    }
    static UserItemEntity checkGenItemData(MyUser mUser, int numItem) {
                UserItemEntity uItem = mUser.getResources().getItem(ItemKey.TICKER_NORMAL.id);
                long eventDay = CfgLottery.getEventIdBuy();
                List<Long> nums = new ArrayList<>();
                for (int i = 0; i < numItem; i++) {
                    nums.add(NumberUtil.getRandomLong(100000, 999999));
                }
                if (uItem == null) {
                    uItem = new UserItemEntity(mUser.getUser().getId(), ItemKey.TICKER_NORMAL, numItem);
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


    static List<Long> addItemFarm(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        int type = aBonus.get(index++).getAsInt();
        int itemId = aBonus.get(index++).getAsInt();
        int number = aBonus.get(index++).getAsInt();
        UserItemFarmEntity uItem = mUser.getResources().getItemFarm(type, itemId);
        if (uItem == null) uItem = new UserItemFarmEntity(mUser.getUser().getId(), type, itemId, number);
        else uItem.add(number);
        if (uItem.getNumber() < 0) return new ArrayList<>();
        boolean isOk = (mUser.getResources().hasItemFarm(type, uItem.getId()) && DBJPA.update(uItem)) || (!mUser.getResources().hasItemFarm(type, uItem.getId()) && DBJPA.save(uItem));
        if (isOk) {
            if (!mUser.getResources().hasItemFarm(type, uItem.getId())) mUser.getResources().addItemFarm(type, uItem);
            if (CfgServer.isRealServer())
                Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction, "type", "item_farm", "id", uItem.getId(), "itemId", itemId, "value", uItem.getNumber(), "addValue", number);
            return Arrays.asList((long) BONUS_ITEM_FARM, (long) uItem.getType(), (long) uItem.getId(), (long) uItem.getNumber(), (long) number);
        }
        return new ArrayList<>();
    }

    static List<Long> addPiece(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        int type = aBonus.get(index++).getAsInt();
        int pieceId = aBonus.get(index++).getAsInt();
        int number = aBonus.get(index++).getAsInt();
        UserPieceEntity uPiece = mUser.getResources().getPiece(type, pieceId);
        if (uPiece == null) uPiece = new UserPieceEntity(mUser.getUser().getId(), type, pieceId, number);
        else uPiece.add(number);
        if (uPiece.getNumber() < 0) return new ArrayList<>();

        boolean isOk = (mUser.getResources().hasPiece(type, uPiece.getId()) && DBJPA.update(uPiece)) || (!mUser.getResources().hasPiece(type, uPiece.getId()) && DBJPA.save(uPiece));

        if (isOk) {
            if (!mUser.getResources().hasPiece(type, uPiece.getId())) mUser.getResources().addPiece(type, uPiece);
            if (CfgServer.isRealServer())
                Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction, "type", "piece", "id", uPiece.getId(), "pieceId", pieceId, "value", uPiece.getNumber(), "addValue", number);
            return Arrays.asList((long) BONUS_PIECE, (long) uPiece.getType(), (long) uPiece.getId(), (long) uPiece.getNumber(), (long) number);
        }
        return new ArrayList<>();
    }

//    static List<Long> addItemPoint(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
//        int itemId = aBonus.get(index++).getAsInt();
//        int number = aBonus.get(index++).getAsInt();
//        UserItemPointEntity uItem = mUser.getResources().getMItemPoint().get(itemId);
//        if (uItem == null) uItem = new UserItemPointEntity(mUser.getUser().getId(), itemId, number);
//        else uItem.add(number);
//        if (uItem.getNumber() < 0) return new ArrayList<>();
//        boolean isOk = DBJPA.saveOrUpdate(uItem);
//        if (isOk) {
//            if (!mUser.getResources().hasItemPoint(uItem.getPointId())) mUser.getResources().addItemPoint(uItem);
//            if (CfgServer.isRealServer())
//                Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction, "type", "item_point", "point_id", uItem.getPointId(), "itemId", itemId, "value", uItem.getNumber(), "addValue", number);
//            return Arrays.asList((long) BONUS_ITEM_POINT, (long) uItem.getPointId(), uItem.getNumber(), (long) number);
//        }
//        return new ArrayList<>();
//    }

    static List<Long> addItemEquipment(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        int itemId = aBonus.get(index++).getAsInt();
        long expire = aBonus.get(index++).getAsLong();
        long isLock = aBonus.get(index++).getAsLong();
        UserItemEquipmentEntity uItemEquip = new UserItemEquipmentEntity(mUser.getUser().getId(), itemId, expire, (int) isLock);
        if (DBJPA.save(uItemEquip)) {
            mUser.getResources().addItemEquip(uItemEquip);
            if (CfgServer.isRealServer())
                Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction, "type", "item_equipment", "id", uItemEquip.getId());
            return Arrays.asList((long) BONUS_ITEM_EQUIPMENT, uItemEquip.getId(), (long) itemId, expire, isLock);
        }
        return new ArrayList<>();
    }


    static List<Long> addWeapon(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        int itemId = aBonus.get(index++).getAsInt();
        boolean hasWeapon = mUser.getResources().hasWeapon(itemId);
        if (!hasWeapon) {
            UserWeaponEntity uWeapon = new UserWeaponEntity(mUser.getUser().getId(), itemId);
            if (DBJPA.save(uWeapon)) {
                mUser.getResources().addWeapon(uWeapon);
                Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction, "type", "weapon", "id", itemId);
                return Arrays.asList((long) BONUS_WEAPON, (long) itemId);
            }
        }
        return new ArrayList<>();
    }

    static List<Long> addPet(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        int type = aBonus.get(index++).getAsInt();
        int id = aBonus.get(index++).getAsInt();
        UserPetEntity uPet = mUser.getResources().getPet(type, id);
        if (uPet == null) uPet = new UserPetEntity(mUser.getUser(), type, id);
        else return new ArrayList<>();
        if (DBJPA.save(uPet)) {
            mUser.getResources().addPet(uPet);
            Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction, "type", "pet", "type", type, "id", id);
            return Arrays.asList((long) BONUS_PET, (long) type, (long) id);
        }
        return new ArrayList<>();
    }

    static List<Long> addHero(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        int heroKey = aBonus.get(index++).getAsInt();
        if (mUser.getResources().getHero(heroKey) != null) return new ArrayList<>();
        UserHeroEntity uHero = new UserHeroEntity(mUser.getUser().getId(), heroKey);
        if (DBJPA.save(uHero)) {
            mUser.getResources().addHero(uHero);
            if (CfgServer.isRealServer())
                Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction, "type", "hero", "id", heroKey);
            return Arrays.asList((long) BONUS_HERO, (long) heroKey);
        }
        return new ArrayList<>();
    }

    static List<Long> addVipExp(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        int addExp = aBonus.get(index++).getAsInt();
        UserEntity user = mUser.getUser();
        mUser.getUser().addVipExp(addExp);
        if (DBJPA.update("user", Arrays.asList("vip_exp", user.getVipExp(), "vip", user.getVip()), Arrays.asList("id", user.getId()))) {
            Actions.save(user, Actions.GRECEIVE, detailAction, "type", "vip_exp", "vip", user.getVip(), "exp", user.getVipExp(), "addExp", addExp);
            return Arrays.asList((long) BONUS_VIP_EXP, (long) user.getVip(), (long) user.getVipExp(), (long) addExp);
        }
        return new ArrayList<>();
    }

    static List<Long> addUserExp(MyUser mUser, JsonArray aBonus, Integer index, String detailAction) {
        int addExp = aBonus.get(index++).getAsInt();
        int oldLevel = mUser.getUser().getLevel();
        mUser.getUser().addExp(mUser, addExp);
        int numUp = mUser.getUser().getLevel() - oldLevel;
        if (detailAction.equals(DetailActionType.BONUS_KILL_ENEMY)) {
            if (numUp > 0) {
                mUser.getUData().increNumPointUpLV(mUser, numUp);
                mUser.getUser().update(new ArrayList<>());
            }
            return Arrays.asList((long) BONUS_EXP, (long) mUser.getUser().getLevel(), mUser.getUser().getExp(), (long) addExp);
        } else {
            if (mUser.getUser().update(new ArrayList<>())) {
                if (CfgServer.isRealServer())
                    Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction, "type", "user_exp", "level", mUser.getUser().getLevel(), "exp", mUser.getUser().getExp(), "addExp", addExp);
                if (numUp > 0) {
                    mUser.getUData().increNumPointUpLV(mUser, numUp);
                }
                return Arrays.asList((long) BONUS_EXP, (long) mUser.getUser().getLevel(), mUser.getUser().getExp(), (long) addExp);
            }
            return new ArrayList<>();
        }
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
    static boolean dbAddMaterial(int userId, int typeId, int inventoryId, long value) {
        return DBJPA.updateNumber("user_material", Arrays.asList("number", value), Arrays.asList("user_id", userId, "type_id", typeId, "material_id", inventoryId));
    }

    static boolean dbAddGold(UserEntity user, long addGold) {
        return DBJPA.update("user", Arrays.asList("gem", user.getGem(), "gold", user.getGold() + addGold, "exp", user.getExp(), "level", user.getLevel()), Arrays.asList("id", user.getId()));
    }

    static boolean dbAddGem(UserEntity user, long addGem) {
        return DBJPA.update("user", Arrays.asList("gem", user.getGem() + addGem, "gold", user.getGold(), "exp", user.getExp(), "level", user.getLevel()), Arrays.asList("id", user.getId()));

    }

    static boolean dbAddRuby(UserEntity user, long addRuby) {
        return DBJPA.update("user", Arrays.asList("ruby", user.getRuby() + addRuby, "exp", user.getExp(), "level", user.getLevel()), Arrays.asList("id", user.getId()));

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
                case BONUS_ITEM_FARM:
                    int farmType = aBonus.get(index++).intValue();
                    int id = aBonus.get(index++).intValue();
                    int farmNum = aBonus.get(index++).intValue();
                    UserItemFarmEntity uItemFarm = mUser.getResources().getItemFarm(farmType, id);
                    if (uItemFarm == null || uItemFarm.getNumber() + farmNum < 0) {
                        if (farmType == ItemFarmType.AGRI.value || farmType == ItemFarmType.SEED.value)
                            return Lang.instance(mUser).get(Lang.err_not_enough_item_farm);
                        if (farmType == ItemFarmType.TOOL.value)
                            return Lang.instance(mUser).get(Lang.err_not_enough_item_tool);
                        if (farmType == ItemFarmType.FOOD.value)
                            return Lang.instance(mUser).get(Lang.err_not_enough_item_food);
                    }
                    break;
//                case BONUS_ITEM_POINT:
//                    int pointId = aBonus.get(index++).intValue();
//                    int pointNum = aBonus.get(index++).intValue();
//                    UserItemPointEntity uItemPoint = mUser.getResources().getItemPoint(pointId);
//                    if (uItemPoint == null || uItemPoint.getNumber() + pointNum < 0)
//                        return mUser.getLang().get(Lang.err_not_enough_point);
//                    break;
                case BONUS_PIECE:
                    int pieceType = aBonus.get(index++).intValue();
                    int pieceId = aBonus.get(index++).intValue();
                    int pieceNum = aBonus.get(index++).intValue();
                    UserPieceEntity uPiece = mUser.getResources().getPiece(pieceType, pieceId);
                    if (uPiece == null || uPiece.getNumber() + pieceNum < 0)
                        return Lang.instance(mUser).get(Lang.err_not_enough_piece);
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
                int length = mTypeLength.containsKey(type) ? mTypeLength.get(type) : 0;
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
