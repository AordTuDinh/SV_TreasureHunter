package game.treasure.service.user;

import game.config.CfgAchievement;
import game.config.CfgArena;
import game.config.CfgCraft;
import game.config.CfgItem;
import game.config.CfgLottery;
import game.config.CfgMaterial;
import game.config.CfgServer;
import game.config.aEnum.*;
import game.config.lang.Lang;
import protocol.Pbmethod;
import game.treasure.mapping.*;
import game.treasure.mapping.main.ResItemEntity;
import game.treasure.mapping.main.ResItemPointEntity;
import game.treasure.service.resource.ResAvatar;
import game.treasure.service.item.EquipmentStatRollService;
import game.treasure.service.resource.ResItem;
import game.treasure.service.resource.ResItemPoint;
import game.treasure.service.resource.ResMount;
import game.treasure.service.resource.ResMob;
import game.object.MyUser;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.StringHelper;

import java.util.*;

public class Bonus {
    /** Shop display-only: [-1, imageId, craftExp] — cộng craft exp, không tạo vật phẩm. */
    public static final int BONUS_CUSTOM_IMAGE = -1;
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
    public static final int BONUS_ITEM_POINT = 13;
    public static final int BONUS_MOB = 14;
    public static final int BONUS_CHANGE_OWNER = 15;
    public static final int BONUS_CUP = 16;

    public static final Map<Integer, Integer> mTypeLength = new HashMap<>() {{
        put(BONUS_CUSTOM_IMAGE, 2);
        put(BONUS_GOLD, 1);
        put(BONUS_GEM, 1);
        put(BONUS_RUBY, 1);
        put(BONUS_CUP, 1);
        put(BONUS_ITEM, 1);
        put(BONUS_ARTIFACT, 2);
        put(BONUS_SKIN, 1);
        put(BONUS_EFFECT_SKIN, 2);
        put(BONUS_VIP_EXP, 1);
        put(BONUS_PET, 2);
        put(BONUS_MOUNT, 2);
        put(BONUS_MATERIAL, 2);
        put(BONUS_EQUIPMENT, 2);
        put(BONUS_ITEM_POINT, 2);
        put(BONUS_MOB, 2);
        put(BONUS_CHANGE_OWNER, 3);
    }};

    public static List<Integer> bonusSinger = Arrays.asList(
            BONUS_ITEM, BONUS_EQUIPMENT, BONUS_ARTIFACT, BONUS_PET, BONUS_MOUNT, BONUS_MOB,
            BONUS_SKIN, BONUS_EFFECT_SKIN, BONUS_MATERIAL, BONUS_ITEM_POINT, BONUS_CHANGE_OWNER);

    public static boolean isBonusSinger(int type) {
        return bonusSinger.contains(type);
    }

    public static List<Long> viewGold(long number) {
        return view(BONUS_GOLD, number);
    }

    /** Preview/grant consumable {@code res_item} by itemKey. Item point dùng {@link #viewItemPoint}. */
    public static List<Long> viewItem(int itemKey, long number) {
        if (number <= 0)
            return new ArrayList<>();
        return viewXNumber(view(BONUS_ITEM, itemKey), (int) number);
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

    public static List<Long> viewMob(int mobId, int tier) {
        int t = tier > 0 ? Math.min(tier, 4) : 1;
        return view(BONUS_MOB, mobId, t);
    }

    public static List<Long> viewMob(int mobId) {
        return viewMob(mobId, 1);
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

    public static List<Long> viewItemArtifact(int artifactId, int tier) {
        int t = tier > 0 ? Math.min(tier, 4) : 1;
        return view(BONUS_ARTIFACT, artifactId, t);
    }

    public static List<Long> viewItemArtifact(int artifactId) {
        return viewItemArtifact(artifactId, 1);
    }

    public static List<Long> viewGem(int number) {
        return view(BONUS_GEM, number);
    }

    public static List<Long> viewRuby(int number) {
        return view(BONUS_RUBY, number);
    }

    public static List<Long> viewCup(int number) {
        return view(BONUS_CUP, number);
    }

    /** Xu đấu trường = item point {@link ItemPointKey#ARENA_COIN}. */
    public static List<Long> viewArenaCoin(int number) {
        return viewItemPoint(CfgArena.arenaCoinPointId(), number);
    }

    public static List<Long> viewVipExp(long number) {
        return view(BONUS_VIP_EXP, number);
    }

    public static List<Long> viewItemPoint(int pointId, long number) {
        return view(BONUS_ITEM_POINT, pointId, number);
    }

    /** Preview/grant craft exp shop: [-1, imageId, exp]. */
    public static List<Long> viewCustomImageCraftExp(int imageId, long exp) {
        return view(BONUS_CUSTOM_IMAGE, imageId, exp);
    }

    public static boolean isCustomImageBonus(List<Long> items) {
        return items != null && !items.isEmpty() && items.get(0).intValue() == BONUS_CUSTOM_IMAGE;
    }

    public static int getCustomImageCraftExp(List<Long> items) {
        if (!isCustomImageBonus(items) || items.size() < 3)
            return 0;
        return Math.max(0, items.get(2).intValue());
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
                // preview [4, itemKey] | receive remove [4, rowId, -itemKey]
                return bonus.size() == 3
                        ? Math.toIntExact(Math.abs(bonus.get(2)))
                        : Math.toIntExact(bonus.get(1));
            }
            case BONUS_EQUIPMENT -> {
                // preview [12, itemKey, tier] | receive [12, rowId, itemKey, tier]
                return bonus.size() == 4
                        ? Math.toIntExact(bonus.get(2))
                        : Math.toIntExact(bonus.get(1));
            }
            case BONUS_ARTIFACT -> {
                // preview [5, itemKey, tier] | receive [5, rowId, itemKey, tier]
                return bonus.size() == 4
                        ? Math.toIntExact(bonus.get(2))
                        : Math.toIntExact(bonus.get(1));
            }
            case BONUS_SKIN -> Math.toIntExact(bonus.get(1));
            case BONUS_EFFECT_SKIN -> Math.toIntExact(bonus.get(2));
        }
        return 0;
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
        List<List<Long>> applied = new ArrayList<>();
        for (List<Long> chunk : parseForApply(aBonus))
            applied.add(applyBonusChunk(mUser, chunk, detailAction));
        return flattenReceiveItemPoint(applied);
    }

    /** Gộp receive wire cùng pointId — preview vẫn tách từng chunk. */
    static List<Long> flattenReceiveItemPoint(List<List<Long>> applied) {
        List<Long> ret = new ArrayList<>();
        Map<Integer, Integer> indexByPoint = new HashMap<>();
        for (List<Long> chunk : applied) {
            if (chunk == null || chunk.isEmpty())
                continue;
            if (chunk.get(0).intValue() == BONUS_ITEM_POINT) {
                int pointId = chunk.get(1).intValue();
                long delta = chunk.get(2);
                long cur = chunk.get(3);
                if (indexByPoint.containsKey(pointId)) {
                    int idx = indexByPoint.get(pointId);
                    ret.set(idx + 2, ret.get(idx + 2) + delta);
                    ret.set(idx + 3, cur);
                } else {
                    indexByPoint.put(pointId, ret.size());
                    ret.addAll(chunk);
                }
            } else {
                ret.addAll(chunk);
            }
        }
        return ret;
    }

    /** Tách flat wire apply — cố định mTypeLength (giống parse preview). */
    static List<List<Long>> parseForApply(List<Long> bonus) {
        return parse(bonus);
    }

    static List<Long> applyBonusChunk(MyUser mUser, List<Long> chunk, String detailAction) {
        if (chunk == null || chunk.isEmpty())
            return new ArrayList<>();
        int type = chunk.get(0).intValue();
        return switch (type) {
            case BONUS_GOLD -> addGold(mUser, chunk.get(1), detailAction);
            case BONUS_GEM -> addGem(mUser, chunk.get(1), detailAction);
            case BONUS_RUBY -> addRuby(mUser, chunk.get(1), detailAction);
            case BONUS_CUP -> addCup(mUser, chunk.get(1), detailAction);
            case BONUS_ITEM -> grantUserItem(mUser, chunk.get(1).intValue(), detailAction);
            case BONUS_EQUIPMENT -> grantUserEquipment(mUser, chunk.get(1).intValue(), chunk.get(2).intValue(), detailAction);
            case BONUS_ARTIFACT -> addItemArtifact(mUser, chunk.get(1).intValue(), chunk.get(2).intValue(), detailAction);
            case BONUS_VIP_EXP -> addVipExp(mUser, chunk.get(1).intValue(), detailAction);
            case BONUS_SKIN -> addCharacterSkin(mUser, chunk.get(1).intValue(), detailAction);
            case BONUS_EFFECT_SKIN -> addEffectSkin(mUser, chunk.get(1).intValue(), chunk.get(2).intValue(), detailAction);
            case BONUS_PET -> addPet(mUser, chunk.get(1).intValue(), chunk.get(2).intValue(), detailAction);
            case BONUS_MOUNT -> addMount(mUser, chunk.get(1).intValue(), chunk.get(2).intValue(), detailAction);
            case BONUS_MOB -> addMob(mUser, chunk.get(1).intValue(), chunk.get(2).intValue(), detailAction);
            case BONUS_MATERIAL -> addMaterial(mUser, chunk.get(1).intValue(), chunk.get(2).intValue(), detailAction);
            case BONUS_ITEM_POINT -> addItemPoint(mUser, chunk.get(1).intValue(), chunk.get(2), detailAction);
            case BONUS_CHANGE_OWNER -> applyChangeOwnerChunk(mUser, chunk, detailAction);
            case BONUS_CUSTOM_IMAGE -> addCustomImageCraftExp(mUser, chunk, detailAction);
            default -> new ArrayList<>();
        };
    }

    /**
     * Shop bonus [-1, imageId, exp] — cộng craft exp (imageId chỉ để client hiển thị).
     * Trả [-1, imageId, exp] để toast client hiện icon ItemImage + số EXP.
     */
    static List<Long> addCustomImageCraftExp(MyUser mUser, List<Long> chunk, String detailAction) {
        if (chunk == null || chunk.size() < 3)
            return new ArrayList<>();
        int imageId = chunk.get(1).intValue();
        int expGain = chunk.get(2).intValue();
        if (expGain <= 0)
            return new ArrayList<>();
        UserDataEntity uData = mUser.getUData();
        if (uData == null)
            return new ArrayList<>();
        CfgCraft.addCraftExp(uData, expGain);
        uData.update(Arrays.asList("craft_level", uData.getCraftLevel(), "craft_exp", uData.getCraftExp()));
        if (CfgServer.isRealServer()) {
            Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction,
                    "type", "craft_exp", "imageId", imageId, "exp", expGain,
                    "craftLevel", uData.getCraftLevel(), "craftExp", uData.getCraftExp());
        }
        return Arrays.asList((long) BONUS_CUSTOM_IMAGE, (long) imageId, (long) expGain);
    }

    /** Apply [15, typeBonus, rowId, itemKey] — đổi chủ row escrow → receive [12|11, rowId, itemKey, tier]. */
    static List<Long> applyChangeOwnerChunk(MyUser mUser, List<Long> chunk, String detailAction) {
        int typeBonus = chunk.get(1).intValue();
        long rowId = chunk.get(2);
        int itemKey = chunk.get(3).intValue();
        if (typeBonus == BONUS_EQUIPMENT)
            return claimEscrowEquipment(mUser, rowId, itemKey, detailAction);
        if (typeBonus == BONUS_MATERIAL)
            return claimEscrowMaterial(mUser, rowId, itemKey, detailAction);
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

    static List<Long> grantUserItem(MyUser mUser, int itemKey, String detailAction) {
        UserItemEntity uItem;
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

    static List<Long> claimEscrowEquipment(MyUser mUser, long rowId, int itemKey, String detailAction) {
        int escrowId = game.treasure.BattleConfig.P_escrowUserId;
        if (mUser.getResources().getEquipment(rowId) != null)
            return new ArrayList<>();
        UserEquipmentEntity uEquip = (UserEquipmentEntity) DBJPA.getUnique(
                "user_equipment", UserEquipmentEntity.class, "id", rowId, "user_id", escrowId);
        if (uEquip == null || uEquip.getItemId() != itemKey)
            return new ArrayList<>();
        if (!mUser.getResources().prepareNewItemSlot(BONUS_EQUIPMENT, 0))
            return new ArrayList<>();
        int killerId = mUser.getUser().getId();
        uEquip.setUserId(killerId);
        if (!uEquip.update(List.of("user_id", killerId)))
            return new ArrayList<>();
        if (!mUser.getResources().prepareNewItemSlot(BONUS_EQUIPMENT, rowId))
            return new ArrayList<>();
        mUser.getResources().addEquipment(uEquip);
        if (CfgServer.isRealServer()) {
            Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction,
                    "type", "user_equipment_claim", "id", rowId, "itemId", itemKey, "tier", uEquip.getTier());
        }
        return Arrays.asList((long) BONUS_EQUIPMENT, rowId, (long) itemKey, (long) uEquip.getTier());
    }

    static List<Long> claimEscrowMaterial(MyUser mUser, long rowId, int materialId, String detailAction) {
        int escrowId = game.treasure.BattleConfig.P_escrowUserId;
        if (mUser.getResources().getMaterial(rowId) != null)
            return new ArrayList<>();
        UserMaterialEntity uMaterial = (UserMaterialEntity) DBJPA.getUnique(
                "user_material", UserMaterialEntity.class, "id", rowId, "user_id", escrowId);
        if (uMaterial == null || uMaterial.getMaterialId() != materialId)
            return new ArrayList<>();
        if (!mUser.getResources().canAddMaterial(1))
            return new ArrayList<>();
        int killerId = mUser.getUser().getId();
        uMaterial.setUserId(killerId);
        if (!uMaterial.update(List.of("user_id", killerId)))
            return new ArrayList<>();
        mUser.getResources().addMaterial(uMaterial);
        if (CfgServer.isRealServer()) {
            Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction,
                    "type", "user_material_claim", "id", rowId, "materialId", materialId, "tier", uMaterial.getTier());
        }
        return Arrays.asList((long) BONUS_MATERIAL, rowId, (long) materialId, (long) uMaterial.getTier());
    }

    static List<Long> addItemArtifact(MyUser mUser, int artifactId, int configTier, String detailAction) {
        if (!mUser.getResources().prepareNewItemSlot(BONUS_ARTIFACT, 0))
            return new ArrayList<>();
        int tier = configTier > 0 ? Math.min(configTier, 4) : 1;
        UserArtifactEntity uArtifact = new UserArtifactEntity(mUser.getUser().getId(), artifactId, tier);
        if (DBJPA.save(uArtifact)) {
            if (!mUser.getResources().prepareNewItemSlot(BONUS_ARTIFACT, uArtifact.getId())) {
                DBJPA.delete("user_artifact", "id", uArtifact.getId(), "user_id", uArtifact.getUserId());
                return new ArrayList<>();
            }
            mUser.getResources().addArtifact(uArtifact);
            if (CfgServer.isRealServer())
                Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction,
                        "type", "artifact", "id", uArtifact.getId(), "artifactId", artifactId, "tier", tier);
            return Arrays.asList((long) BONUS_ARTIFACT, uArtifact.getId(), (long) artifactId, (long) uArtifact.getTier());
        }
        return new ArrayList<>();
    }

    static List<Long> addItemPoint(MyUser mUser, int pointId, long delta, String detailAction) {
        if (ResItemPoint.get(pointId) == null && !ItemPointKey.isPointKey(pointId))
            return new ArrayList<>();
        int server = mUser.getUser().getServer();
        boolean deferDb = pointId == ItemPointKey.PLOT.id;
        UserItemPointEntity row = mUser.getResources().getItemPoint(pointId);
        if (row == null) {
            if (delta < 0)
                return new ArrayList<>();
            if (usesEventBagPoint(pointId) && !mUser.getResources().canAddEventItem(1))
                return new ArrayList<>();
            row = new UserItemPointEntity(mUser.getUser().getId(), pointId, server);
            row.setNumber((int) delta);
            if (!row.saveOrUpdate())
                return new ArrayList<>();
            mUser.getResources().addItemPoint(row);
            if (CfgServer.isRealServer())
                Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction,
                        "type", "item_point", "pointId", pointId, "add", delta, "cur", row.getNumber());
            return Arrays.asList((long) BONUS_ITEM_POINT, (long) pointId, delta, (long) row.getNumber());
        }
        long newNum = (long) row.getNumber() + delta;
        if (newNum < 0)
            return new ArrayList<>();
        row.setServer(server);
        if (deferDb) {
            if (!row.setNumberDeferred((int) newNum))
                return new ArrayList<>();
        } else {
            row.setNumber((int) newNum);
            if (!row.updateNumber((int) newNum))
                return new ArrayList<>();
        }
        if (CfgServer.isRealServer())
            Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction,
                    "type", "item_point", "pointId", pointId, "add", delta, "cur", newNum);
        return Arrays.asList((long) BONUS_ITEM_POINT, (long) pointId, delta, newNum);
    }

    static List<Long> addVipExp(MyUser mUser, int addExp, String detailAction) {
        UserEntity user = mUser.getUser();
        int oldVip = user.getVip();
        user.addVipExp(addExp);
        int newVip = user.getVip();
        if (newVip > oldVip) {
            VipService.applyLevelUpBonuses(mUser, oldVip + 1, newVip);
        }
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
        UserPetEntity uPet = new UserPetEntity(mUser.getUser(), petId,tier);
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
        UserMountEntity uMount = new UserMountEntity(mUser.getUser(), mountId,tier);
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

    static List<Long> addMob(MyUser mUser, int mobId, int tier, String detailAction) {
        if (tier <= 0)
            tier = 1;
        if (ResMob.getMob(mobId) == null)
            return new ArrayList<>();
        if (!mUser.getResources().prepareNewItemSlot(BONUS_MOB, 0))
            return new ArrayList<>();
        UserMobEntity uMob = new UserMobEntity(mUser.getUser(), mobId, tier);
        if (DBJPA.save(uMob)) {
            if (!mUser.getResources().prepareNewItemSlot(BONUS_MOB, uMob.getId())) {
                DBJPA.delete("user_mob", "id", uMob.getId(), "user_id", uMob.getUserId());
                return new ArrayList<>();
            }
            mUser.getResources().addMob(uMob);
            if (CfgServer.isRealServer()) {
                Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction,
                        "type", "mob", "id", uMob.getId(), "mobId", mobId, "tier", tier);
            }
            return Arrays.asList((long) BONUS_MOB, uMob.getId(), (long) mobId, (long) tier);
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
        if (value > 0)
            value = scaleGoldByIncrease(mUser, value);
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

    /** Point 17 — tăng vàng nhận thêm theo % (chỉ khi value > 0). */
    static long scaleGoldByIncrease(MyUser mUser, long baseGold) {
        if (baseGold <= 0 || mUser == null)
            return baseGold;
        int percent = resolveGoldIncreasePercent(mUser);
        if (percent <= 0)
            return baseGold;
        return baseGold + (long) Math.floor(baseGold * percent / 100f);
    }

    static int resolveGoldIncreasePercent(MyUser mUser) {
        if (mUser.getPlayer() == null || mUser.getPlayer().getPoint() == null)
            return 0;
        return mUser.getPlayer().getPoint().getBuffGold();
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

    static List<Long> addCup(MyUser mUser, long value, String detailAction) {
        if (dbAddCup(mUser.getUser(), value)) {
            mUser.getUser().addCup(value);
            if (CfgServer.isRealServer())
                Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction, "type", "cup", "value", mUser.getUser().getCup(), "addValue", value);
            return Arrays.asList((long) BONUS_CUP, (long) mUser.getUser().getCup(), value);
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

    static boolean dbAddCup(UserEntity user, long addCup) {
        return DBJPA.update("user", Arrays.asList("cup", user.getCup() + addCup), Arrays.asList("id", user.getId()));
    }

    public static String checkMoney(MyUser mUser, List<Long> aBonus) {
        Map<Integer, Long> pointDeduct = new HashMap<>();
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
                case BONUS_CUP:
                    if (mUser.getUser().getCup() + chunk.get(1) < 0)
                        return Lang.instance(mUser).get(Lang.err_not_enough_cup);
                    break;
                case BONUS_ITEM_POINT:
                    if (chunk.get(2) < 0)
                        pointDeduct.merge(chunk.get(1).intValue(), chunk.get(2), Long::sum);
                    break;
            }
        }
        for (Map.Entry<Integer, Long> entry : pointDeduct.entrySet()) {
            game.treasure.mapping.main.ResItemPointEntity res = ResItemPoint.get(entry.getKey());
            String name = res != null ? res.getName() : "?";
            if (mUser.getResources().getItemPointNumber(entry.getKey()) + entry.getValue() < 0)
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

    /** Parse preview/reward config — ITEM [4,itemKey], ARTIFACT [5,itemKey,tier], EQUIP [12,itemKey,tier], PET/MOUNT [9|10,configId,tier]. */
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

    /** Parse apply wire — cố định mTypeLength (alias parse). */
    public static List<List<Long>> parseCost(List<Long> bonus) {
        return parse(bonus);
    }

    public static List<Long> reverseBonus(List<Long> bonus) {
        List<Long> ret = new ArrayList<>();
        List<List<Long>> aBonus = parse(bonus);
        for (List<Long> bm : aBonus) {
            if (bm.get(0).intValue() == BONUS_ITEM_POINT) {
                List<Long> copy = new ArrayList<>(bm);
                copy.set(2, -copy.get(2));
                ret.addAll(copy);
                continue;
            }
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

    /** Chỉ nhân đôi (hoặc times) các chunk BONUS_RUBY trong bonus. */
    public static List<Long> xRubyBonus(List<Long> bonus, int times) {
        List<List<Long>> aBonus = parse(bonus);
        List<Long> result = new ArrayList<>();
        for (List<Long> bo : aBonus) {
            result.addAll(bo);
            if (bo.get(0).intValue() == BONUS_RUBY)
                result.set(result.size() - 1, result.get(result.size() - 1) * times);
        }
        return result;
    }

    static boolean hasPositiveRuby(List<Long> bonus) {
        for (List<Long> chunk : parse(bonus)) {
            if (chunk.get(0).intValue() == BONUS_RUBY && chunk.size() > 1 && chunk.get(1) > 0)
                return true;
        }
        return false;
    }

    /**
     * Nếu túi có Vé x2 ruby nạp (point 13) và bonus có ruby: x2 ruby.
     * {@code attachFee=true}: gắn trừ 1 vé vào bonus (cùng receiveListItem).
     * {@code attachFee=false}: trừ vé ngay (dùng khi gửi bonus qua mail).
     */
    public static List<Long> withRubyX2Voucher(MyUser mUser, List<Long> bonus, boolean attachFee) {
        int pointId = ItemPointKey.RUBY_X2_VOUCHER.id;
        if (mUser.getResources().getItemPointNumber(pointId) < 1)
            return bonus;
        if (!hasPositiveRuby(bonus))
            return bonus;
        List<Long> fee = viewItemPoint(pointId, -1);
        if (checkMoney(mUser, fee) != null)
            return bonus;
        List<Long> result = xRubyBonus(bonus, 2);
        if (attachFee) {
            List<Long> withFee = new ArrayList<>(fee);
            withFee.addAll(result);
            return withFee;
        }
        if (receiveListItem(mUser, DetailActionType.SU_DUNG_VE_X2_RUBY.getKey(), fee).isEmpty())
            return bonus;
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

    public static boolean movePetOutOfBag(MyUser mUser, UserPetEntity pet) {
        if (pet == null)
            return false;
        clearItemFromSlot(mUser, BONUS_PET, pet.getId());
        return true;
    }

    public static boolean movePetToBag(MyUser mUser, UserPetEntity pet) {
        if (pet == null || !mUser.getResources().canAddBagItem(1))
            return false;
        List<Long> slots = mUser.getUData().getItemSlotList();
        int bagCount = mUser.getUData().getSlotBagUI();
        Integer slot = ItemSlotHelper.findFirstEmpty(slots, 0, bagCount);
        if (slot == null)
            return false;
        ItemSlotHelper.setPair(slots, slot, BONUS_PET, pet.getId());
        return mUser.getResources().saveItemSlot(slots);
    }

    public static boolean movePetToBagSlot(MyUser mUser, UserPetEntity pet, int slotIndex) {
        if (pet == null || slotIndex < 0)
            return false;
        List<Long> slots = mUser.getUData().getItemSlotList();
        int bagCount = mUser.getUData().getSlotBagUI();
        if (slotIndex >= bagCount)
            return false;
        ItemSlotHelper.setPair(slots, slotIndex, BONUS_PET, pet.getId());
        return mUser.getResources().saveItemSlot(slots);
    }

    public static boolean moveMountOutOfBag(MyUser mUser, UserMountEntity mount) {
        if (mount == null)
            return false;
        clearItemFromSlot(mUser, BONUS_MOUNT, mount.getId());
        return true;
    }

    public static boolean moveMountToBag(MyUser mUser, UserMountEntity mount) {
        if (mount == null || !mUser.getResources().canAddBagItem(1))
            return false;
        List<Long> slots = mUser.getUData().getItemSlotList();
        int bagCount = mUser.getUData().getSlotBagUI();
        Integer slot = ItemSlotHelper.findFirstEmpty(slots, 0, bagCount);
        if (slot == null)
            return false;
        ItemSlotHelper.setPair(slots, slot, BONUS_MOUNT, mount.getId());
        return mUser.getResources().saveItemSlot(slots);
    }

    public static boolean moveMountToBagSlot(MyUser mUser, UserMountEntity mount, int slotIndex) {
        if (mount == null || slotIndex < 0)
            return false;
        List<Long> slots = mUser.getUData().getItemSlotList();
        int bagCount = mUser.getUData().getSlotBagUI();
        if (slotIndex >= bagCount)
            return false;
        ItemSlotHelper.setPair(slots, slotIndex, BONUS_MOUNT, mount.getId());
        return mUser.getResources().saveItemSlot(slots);
    }

    public static boolean clearPetEquipSlot(MyUser mUser) {
        int idx = game.treasure.mapping.UserEntity.equipSlotIndex(
                protocol.Pbmethod.EquipSlotType.PET.getNumber());
        List<Integer> lst = mUser.getUser().normalizeItemEquipList();
        if (idx < 0)
            return false;
        lst.set(idx, 0);
        lst.set(idx + 1, 0);
        lst.set(idx + 2, 0);
        return mUser.getUser().updateItemEquip(lst);
    }

    public static boolean clearMountEquipSlot(MyUser mUser) {
        int idx = game.treasure.mapping.UserEntity.equipSlotIndex(
                protocol.Pbmethod.EquipSlotType.MOUNT.getNumber());
        List<Integer> lst = mUser.getUser().normalizeItemEquipList();
        if (idx < 0)
            return false;
        lst.set(idx, 0);
        lst.set(idx + 1, 0);
        lst.set(idx + 2, 0);
        return mUser.getUser().updateItemEquip(lst);
    }

    public static Integer findPetBagSlot(MyUser mUser, long rowId) {
        List<Long> slots = mUser.getUData().getItemSlotList();
        int bagCount = mUser.getUData().getSlotBagUI();
        return ItemSlotHelper.findSlotOf(slots, 0, bagCount, BONUS_PET, rowId);
    }

    public static Integer findMountBagSlot(MyUser mUser, long rowId) {
        List<Long> slots = mUser.getUData().getItemSlotList();
        int bagCount = mUser.getUData().getSlotBagUI();
        return ItemSlotHelper.findSlotOf(slots, 0, bagCount, BONUS_MOUNT, rowId);
    }

    public static boolean moveEquipmentOutOfBag(MyUser mUser, UserEquipmentEntity equip) {
        clearItemFromSlot(mUser, BONUS_EQUIPMENT, equip.getId());
        equip.setBagSlot(-1);
        return true;
    }

    public static boolean isLotteryTicketPoint(int pointId) {
        return ItemPointKey.isLotteryTicket(pointId);
    }

    /** Tab túi event — res_item_point.type ∈ {EVENT, USE, SPEAKER, OPEN_BOX, OPEN_BOX_TIER}. */
    public static boolean usesEventBagStorage(Pbmethod.ItemPointType storageType) {
        return storageType == Pbmethod.ItemPointType.EVENT
                || storageType == Pbmethod.ItemPointType.USE
                || storageType == Pbmethod.ItemPointType.SPEAKER
                || storageType == Pbmethod.ItemPointType.OPEN_BOX
                || storageType == Pbmethod.ItemPointType.OPEN_BOX_TIER;
    }

    public static boolean usesEventBagPoint(int pointId) {
        ResItemPointEntity res = ResItemPoint.get(pointId);
        return res != null && res.getItemPointType() != null && usesEventBagStorage(res.getItemPointType());
    }

    /** Mua vé số — lưu user_item_point.data [eventDay, các số vé]. */
    public static List<Long> grantLotteryTickets(MyUser mUser, int pointId, long eventDay, List<Long> nums, String detailAction) {
        if (!ItemPointKey.isLotteryTicket(pointId) || nums == null || nums.isEmpty())
            return new ArrayList<>();
        UserItemPointEntity row = mUser.getResources().getItemPoint(pointId);
        if ((row == null || row.getNumber() <= 0) && !mUser.getResources().canAddEventItem(1))
            return new ArrayList<>();
        int server = mUser.getUser().getServer();
        if (row == null) {
            row = new UserItemPointEntity(mUser.getUser().getId(), pointId, server);
            row.appendTicketNumbers(eventDay, nums);
            if (!row.saveOrUpdate())
                return new ArrayList<>();
            mUser.getResources().addItemPoint(row);
        } else {
            row.appendTicketNumbers(eventDay, nums);
            if (!row.persist())
                return new ArrayList<>();
        }
        if (CfgServer.isRealServer()) {
            Actions.save(mUser.getUser(), Actions.GRECEIVE, detailAction,
                    "type", "item_point_ticket", "pointId", pointId, "add", nums.size(), "cur", row.getNumber());
        }
        return Arrays.asList((long) BONUS_ITEM_POINT, (long) pointId, (long) nums.size(), (long) row.getNumber());
    }

    /** item_slot: consum (BONUS_ITEM), equip, pet, mount, artifact — không gồm event/currency. */
    public static boolean usesItemSlotBonusType(int bonusType) {
        return bonusType == BONUS_ITEM || bonusType == BONUS_EQUIPMENT
                || bonusType == BONUS_PET || bonusType == BONUS_MOUNT || bonusType == BONUS_MOB
                || bonusType == BONUS_ARTIFACT;
    }

    public static boolean usesItemSlotForUserItem(Pbmethod.ItemType storageType) {
        return storageType == Pbmethod.ItemType.POSITION;
    }

    public static boolean isBlockedFromBagSlot(int isTrading, int inMarket) {
        return isTrading == 1 || inMarket == 1;
    }

    static String slotOccupancyKey(int bonusType, long rowId) {
        return bonusType + ":" + rowId;
    }

    static boolean isAlreadySlotted(List<Long> slots, int bagCount, int bonusType, long rowId) {
        return ItemSlotHelper.findSlotOf(slots, 0, bagCount, bonusType, rowId) != null;
    }

    /** Ô item_slot còn trỏ tới row hợp lệ cho túi UI (tồn tại, chưa mặc, chưa kẹt ví/chợ). */
    static boolean slotRowValid(MyUser mUser, int bonusType, long rowId) {
        if (rowId <= 0 || !usesItemSlotBonusType(bonusType))
            return false;
        switch (bonusType) {
            case BONUS_ITEM: {
                UserItemEntity u = mUser.getResources().getItem(rowId);
                return u != null
                        && usesItemSlotForUserItem(Pbmethod.ItemType.valueOf(u.getType()))
                        && !isBlockedFromBagSlot(u.getIsTrading(), u.getInMarket());
            }
            case BONUS_EQUIPMENT: {
                UserEquipmentEntity e = mUser.getResources().getEquipment(rowId);
                return e != null && !e.isEquip();
            }
            case BONUS_PET: {
                UserPetEntity pet = mUser.getResources().getPet(rowId);
                return pet != null && !pet.isEquip()
                        && !isBlockedFromBagSlot(pet.getIsTrading(), pet.getInMarket());
            }
            case BONUS_MOUNT: {
                UserMountEntity mount = mUser.getResources().getMount(rowId);
                return mount != null && !mount.isEquip()
                        && !isBlockedFromBagSlot(mount.getIsTrading(), mount.getInMarket());
            }
            case BONUS_MOB: {
                UserMobEntity mob = mUser.getResources().getMob(rowId);
                return mob != null && !isBlockedFromBagSlot(mob.getIsTrading(), mob.getInMarket());
            }
            case BONUS_ARTIFACT: {
                UserArtifactEntity artifact = mUser.getResources().getArtifact(rowId);
                return artifact != null && !isArtifactEquipped(mUser, rowId)
                        && !isBlockedFromBagSlot(artifact.getIsTrading(), artifact.getInMarket());
            }
            default:
                return false;
        }
    }

    static boolean slotRowExists(MyUser mUser, int bonusType, long rowId) {
        return slotRowValid(mUser, bonusType, rowId);
    }

    /** Xóa ô invalid/trùng; tùy chọn gán item hợp lệ chưa có ô vào chỗ trống. */
    public static void verifyItemSlots(MyUser mUser, boolean backfillOrphans) {
        List<Long> slots = mUser.getUData().getItemSlotList();
        int bagCount = mUser.getUData().getSlotBagUI();
        boolean changed = reconcileItemSlotPairs(mUser, slots, bagCount);
        if (backfillOrphans)
            changed |= backfillOrphanedItemSlots(mUser, slots, bagCount);
        if (changed)
            mUser.getResources().saveItemSlot(slots);
    }

    static boolean reconcileItemSlotPairs(MyUser mUser, List<Long> slots, int bagCount) {
        boolean changed = false;
        Set<String> seen = new HashSet<>();
        for (int s = 0; s < bagCount; s++) {
            if (ItemSlotHelper.isEmpty(slots, s))
                continue;
            int bt = ItemSlotHelper.getBonusType(slots, s);
            long rowId = ItemSlotHelper.getRowId(slots, s);
            String key = slotOccupancyKey(bt, rowId);
            if (!usesItemSlotBonusType(bt) || !slotRowValid(mUser, bt, rowId) || seen.contains(key)) {
                ItemSlotHelper.clearPair(slots, s);
                changed = true;
                continue;
            }
            seen.add(key);
        }
        return changed;
    }

    static boolean backfillOrphanedItemSlots(MyUser mUser, List<Long> slots, int bagCount) {
        boolean changed = false;
        for (UserItemEntity item : mUser.getResources().getMItem().values()) {
            if (!usesItemSlotForUserItem(Pbmethod.ItemType.valueOf(item.getType())))
                continue;
            if (isBlockedFromBagSlot(item.getIsTrading(), item.getInMarket()))
                continue;
            if (isAlreadySlotted(slots, bagCount, BONUS_ITEM, item.getId()))
                continue;
            Integer s = ItemSlotHelper.findFirstEmpty(slots, 0, bagCount);
            if (s == null)
                return changed;
            ItemSlotHelper.setPair(slots, s, BONUS_ITEM, item.getId());
            changed = true;
        }
        for (UserEquipmentEntity equip : mUser.getResources().getMEquipment().values()) {
            if (equip.isEquip() || equip.isExpired())
                continue;
            if (isAlreadySlotted(slots, bagCount, BONUS_EQUIPMENT, equip.getId()))
                continue;
            Integer s = ItemSlotHelper.findFirstEmpty(slots, 0, bagCount);
            if (s == null)
                return changed;
            ItemSlotHelper.setPair(slots, s, BONUS_EQUIPMENT, equip.getId());
            changed = true;
        }
        for (UserPetEntity pet : mUser.getResources().getMPet().values()) {
            if (pet.isEquip() || isBlockedFromBagSlot(pet.getIsTrading(), pet.getInMarket()))
                continue;
            if (isAlreadySlotted(slots, bagCount, BONUS_PET, pet.getId()))
                continue;
            Integer s = ItemSlotHelper.findFirstEmpty(slots, 0, bagCount);
            if (s == null)
                return changed;
            ItemSlotHelper.setPair(slots, s, BONUS_PET, pet.getId());
            changed = true;
        }
        for (UserMountEntity mount : mUser.getResources().getMMount().values()) {
            if (mount.isEquip() || isBlockedFromBagSlot(mount.getIsTrading(), mount.getInMarket()))
                continue;
            if (isAlreadySlotted(slots, bagCount, BONUS_MOUNT, mount.getId()))
                continue;
            Integer s = ItemSlotHelper.findFirstEmpty(slots, 0, bagCount);
            if (s == null)
                return changed;
            ItemSlotHelper.setPair(slots, s, BONUS_MOUNT, mount.getId());
            changed = true;
        }
        for (UserMobEntity mob : mUser.getResources().getMMob().values()) {
            if (isBlockedFromBagSlot(mob.getIsTrading(), mob.getInMarket()))
                continue;
            if (isAlreadySlotted(slots, bagCount, BONUS_MOB, mob.getId()))
                continue;
            Integer s = ItemSlotHelper.findFirstEmpty(slots, 0, bagCount);
            if (s == null)
                return changed;
            ItemSlotHelper.setPair(slots, s, BONUS_MOB, mob.getId());
            changed = true;
        }
        for (UserArtifactEntity artifact : mUser.getResources().getMArtifact().values()) {
            if (isArtifactEquipped(mUser, artifact.getId())
                    || isBlockedFromBagSlot(artifact.getIsTrading(), artifact.getInMarket()))
                continue;
            if (isAlreadySlotted(slots, bagCount, BONUS_ARTIFACT, artifact.getId()))
                continue;
            Integer s = ItemSlotHelper.findFirstEmpty(slots, 0, bagCount);
            if (s == null)
                return changed;
            ItemSlotHelper.setPair(slots, s, BONUS_ARTIFACT, artifact.getId());
            changed = true;
        }
        return changed;
    }

    /** Xóa ô item_slot trỏ tới row không còn tồn tại hoặc không thuộc túi UI. */
    public static void reconcileItemSlots(MyUser mUser) {
        verifyItemSlots(mUser, false);
    }

    /** Gán ô túi UI — bonusType ∈ {4 consum, 12 equip, 9 pet, 10 mount, 14 mob, 5 artifact}. rowId=0 chỉ check còn chỗ. */
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
