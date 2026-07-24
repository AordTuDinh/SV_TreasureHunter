package game.treasure.controller;

import game.battle.calculate.IMath;
import game.battle.object.Point;
import game.config.CfgArtifact;
import game.config.CfgCraft;
import game.config.aEnum.CraftTargetType;
import game.config.aEnum.DetailActionType;
import game.config.aEnum.ToastType;
import game.config.lang.Lang;
import game.treasure.mapping.UserArtifactEntity;
import game.treasure.mapping.UserDataEntity;
import game.treasure.mapping.UserEquipmentEntity;
import game.treasure.mapping.UserItemEntity;
import game.treasure.mapping.UserMaterialEntity;
import game.treasure.mapping.UserMountEntity;
import game.treasure.mapping.UserPetEntity;
import game.treasure.mapping.UserSkinEntity;
import game.treasure.mapping.main.ResArtifactEntity;
import game.treasure.mapping.main.ResItemEquipmentEntity;
import game.treasure.mapping.main.ResMaterialEntity;
import game.treasure.mapping.main.ResMountEntity;
import game.treasure.mapping.main.ResPetEntity;
import game.treasure.server.IAction;
import game.treasure.service.item.CraftPointDataUtil;
import game.treasure.service.resource.ResItem;
import game.treasure.service.trading.TradingItemService;
import game.treasure.service.user.Bonus;
import io.netty.channel.Channel;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.StringHelper;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CraftHandler extends AHandler {

    private static CraftHandler instance;

    public static CraftHandler getInstance() {
        if (instance == null) {
            instance = new CraftHandler();
        }
        return instance;
    }

    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        mHandler.put(IAction.CRAFT_EXECUTE, this);
    }

    @Override
    public AHandler newInstance() {
        return new CraftHandler();
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        try {
            if (actionId == IAction.CRAFT_EXECUTE) {
                executeCraft();
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    /**
     * Request: [targetType, targetId, gemRowId...]
     * Response CRAFT_EXECUTE: [craftOk, targetType, targetId, targetLevel, gemCount,
     *   per gem: rowId, socketOk, consumed,
     *   craftLevel, craftExp, expToNext, maxSlot, curSlot, bonusPct]
     */
    private void executeCraft() {
        List<Long> inputs = getInputALong();
        if (inputs.size() < 3) {
            addErrParam();
            return;
        }
        CraftTargetType targetType = CraftTargetType.fromId(inputs.get(0).intValue());
        if (targetType == null) {
            addErrParam();
            return;
        }
        long targetId = inputs.get(1);
        List<Long> gemRowIds = new ArrayList<>(inputs.subList(2, inputs.size()));
        if (gemRowIds.isEmpty()) {
            addErrParam();
            return;
        }

        if (targetType == CraftTargetType.SKIN) {
            addErrResponse(getLang(Lang.err_system_down));
            return;
        }

        if (targetType == CraftTargetType.EQUIPMENT) {
            UserEquipmentEntity equip = mUser.getResources().getItemEquipment(targetId);
            if (equip == null) {
                addErrResponse(getLang(Lang.err_item_equip_not_found));
                return;
            }
            if (equip.getIsCraft() == 1) {
                addErrResponse(getLang(Lang.err_params));
                return;
            }
        }

        if (targetType == CraftTargetType.CONSUMABLE) {
            UserItemEntity item = mUser.getResources().getItem(targetId);
            if (item == null) {
                addErrResponse(getLang(Lang.err_item_equip_not_found));
                return;
            }
            if (item.getIsCraft() == 1) {
                addErrResponse(getLang(Lang.err_params));
                return;
            }
            if (item.getType() != Pbmethod.ItemType.POSITION.getNumber()) {
                addErrResponse(getLang(Lang.err_params));
                return;
            }
        }

        if (targetType == CraftTargetType.PET) {
            UserPetEntity pet = mUser.getResources().getPet(targetId);
            if (pet == null) {
                addErrResponse(getLang(Lang.err_item_equip_not_found));
                return;
            }
            if (pet.getIsCraft() == 1) {
                addErrResponse(getLang(Lang.err_params));
                return;
            }
        }
        if (targetType == CraftTargetType.MOUNT) {
            UserMountEntity mount = mUser.getResources().getMount(targetId);
            if (mount == null) {
                addErrResponse(getLang(Lang.err_item_equip_not_found));
                return;
            }
            if (mount.getIsCraft() == 1) {
                addErrResponse(getLang(Lang.err_params));
                return;
            }
        }

        UserDataEntity uData = mUser.getUData();
        int craftLevel = uData.getCraftLevel();
        int curSlot = CfgCraft.getCurSlot(targetType, craftLevel);
        if (gemRowIds.size() > curSlot) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }

        UserArtifactEntity craftArtifact = null;
        if (targetType == CraftTargetType.ARTIFACT) {
            craftArtifact = mUser.getResources().getArtifact(targetId);
            if (craftArtifact == null) {
                addErrResponse(getLang(Lang.err_item_equip_not_found));
                return;
            }
            if (craftArtifact.getIsCraft() == 1) {
                addErrResponse(getLang(Lang.err_params));
                return;
            }
        }

        Object craftTarget = resolveCraftTargetEntity(targetType, targetId, craftArtifact);
        if (craftTarget == null) {
            addErrResponse(getLang(Lang.err_item_equip_not_found));
            return;
        }
        if (TradingItemService.isBlockedFromCraft(craftTarget)) {
            addErrResponse(getLang(Lang.err_item_in_trading));
            return;
        }

        List<UserMaterialEntity> gems = new ArrayList<>();
        List<Integer> gemRanks = new ArrayList<>();
        int maxGemRank = 0;
        int requiredPointId = resolveRequiredMaterialPointId(targetType, targetId, craftArtifact);
        for (Long rowId : gemRowIds) {
            UserMaterialEntity gem = mUser.getResources().getMaterial(rowId);
            if (gem == null) {
                addErrResponse(getLang(Lang.err_item_equip_not_found));
                return;
            }
            ResMaterialEntity gemRes = gem.getRes();
            if (gemRes == null) {
                addErrParam();
                return;
            }
            if (TradingItemService.isBlockedFromCraft(gem)) {
                addErrResponse(getLang(Lang.err_item_in_trading));
                return;
            }
            if (targetType == CraftTargetType.ARTIFACT) {
                if (requiredPointId <= 0 || gemRes.getPointId() != requiredPointId) {
                    addErrResponse(getLang(Lang.err_params));
                    return;
                }
            }
            gems.add(gem);
            gemRanks.add(gem.getTier());
            maxGemRank = Math.max(maxGemRank, gem.getTier());
        }

        int itemLevel = resolveItemLevel(targetType, targetId);
        if (itemLevel < 0) {
            addErrResponse(getLang(Lang.err_item_equip_not_found));
            return;
        }

        List<Long> fee = resolveCraftFee(targetType, craftArtifact, gemRanks);
        if (fee.isEmpty()) {
            addErrParam();
            return;
        }
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        List<Long> feeBonus = Bonus.receiveListItem(mUser,
                DetailActionType.CRAFT_EXECUTE.getKey(targetId), fee);
        if (feeBonus.isEmpty()) {
            addErrSystem();
            return;
        }
        addBonusPrivate(feeBonus);

        int craftPercent;
        if (targetType == CraftTargetType.CONSUMABLE) {
            UserItemEntity consumable = mUser.getResources().getItem(targetId);
            int resTier = consumable != null ? consumable.getTier() : 1;
            int userLevel = consumable != null && consumable.getLevel() > 0 ? consumable.getLevel() : 1;
            craftPercent = CfgCraft.getCraftSuccessPercent(targetType, resTier, userLevel, craftLevel);
        } else {
            craftPercent = CfgCraft.getCraftSuccessPercent(targetType, maxGemRank, itemLevel, craftLevel);
        }
        boolean craftOk = NumberUtil.getRandom(100) < craftPercent;

        List<Long> resp = new ArrayList<>();
        resp.add(craftOk ? 1L : 0L);
        resp.add((long) targetType.id);
        resp.add(targetId);

        if (!craftOk) {
            if (targetType.losesTargetOnCraftFail()) {
                destroyCraftTarget(targetType, targetId);
            }
            resp.add(-1L);
            resp.add(0L);
            appendCraftStatus(resp, targetType, uData);
            addResponse(getCommonVector(resp));
            addToast(ToastType.FAIL, "");
            return;
        }

        float consumableHpOriginal = 0f;
        if (targetType == CraftTargetType.CONSUMABLE) {
            UserItemEntity item = mUser.getResources().getItem(targetId);
            if (item == null) {
                Bonus.receiveListItem(mUser, DetailActionType.UPDATE_FAIL.getKey(), Bonus.reverseBonus(fee));
                addErrSystem();
                return;
            }
            consumableHpOriginal = CraftPointDataUtil.readHpBaseFromData(item.getData());
        }

        if (!resetTargetLevel(targetType, targetId)) {
            Bonus.receiveListItem(mUser, DetailActionType.UPDATE_FAIL.getKey(), Bonus.reverseBonus(fee));
            addErrSystem();
            return;
        }

        if (!markTargetCrafted(targetType, targetId)) {
            Bonus.receiveListItem(mUser, DetailActionType.UPDATE_FAIL.getKey(), Bonus.reverseBonus(fee));
            addErrSystem();
            return;
        }

        resp.add((long) resolveItemLevel(targetType, targetId));
        resp.add((long) gems.size());

        int totalExpGain = 0;
        int totalPriceTreasureGain = 0;
        for (UserMaterialEntity gem : gems) {
            boolean socketOk;
            boolean consumed = false;
            if (targetType == CraftTargetType.ARTIFACT) {
                socketOk = true;
                consumed = true;
                if (CfgCraft.grantsCraftExp(craftLevel, gem.getTier())) {
                    totalExpGain += CfgCraft.getCraftExpByRank(gem.getTier());
                }
            } else {
                socketOk = NumberUtil.getRandom(100) < gem.getSocketSuccessPercent();
                if (socketOk) {
                    boolean statApplied = applySocketStat(targetType, targetId, gem);
                    if (statApplied) {
                        consumed = true;
                        if (CfgCraft.grantsCraftExp(craftLevel, gem.getTier())) {
                            totalExpGain += CfgCraft.getCraftExpByRank(gem.getTier());
                        }
                        ResMaterialEntity gemRes = gem.getRes();
                        if (gemRes != null) {
                            totalPriceTreasureGain += CfgCraft.getPriceTreasurePoints(
                                    gemRes.getTier(), gem.getTier());
                        }
                    }
                }
            }
            removeMaterial(gem);
            resp.add(gem.getId());
            resp.add(socketOk ? 1L : 0L);
            resp.add(consumed ? 1L : 0L);
        }

        if (totalPriceTreasureGain > 0) {
            addPriceTreasure(targetType, targetId, totalPriceTreasureGain);
        }

        if (targetType == CraftTargetType.EQUIPMENT) {
            applyEquipmentTransform(targetId);
        } else if (targetType == CraftTargetType.CONSUMABLE) {
            applyConsumableTransform(targetId, consumableHpOriginal);
        } else if (targetType == CraftTargetType.PET) {
            applyPetTransform(targetId);
        } else if (targetType == CraftTargetType.MOUNT) {
            applyMountTransform(targetId);
        } else if (targetType == CraftTargetType.ARTIFACT) {
            applyArtifactTransform(targetId);
        }

        boolean craftLeveled = false;
        if (totalExpGain > 0) {
            craftLeveled = CfgCraft.addCraftExp(uData, totalExpGain);
            uData.update(Arrays.asList("craft_level", uData.getCraftLevel(), "craft_exp", uData.getCraftExp()));
        }

        appendCraftStatus(resp, targetType, uData);
        addResponse(getCommonVector(resp));
        addToast(ToastType.SUCCESS, getLang(Lang.craft_success));

        if (craftLeveled) {
            pushCraftUpdate(uData);
        }

        sendTargetProto(targetType, targetId);
        broadcastEquipViewIfTargetEquipped(targetType, targetId);
    }

    private void broadcastEquipViewIfTargetEquipped(CraftTargetType targetType, long targetId) {
        if (mUser.getPlayer() == null)
            return;
        List<Integer> ids = mUser.getUser().getListIdEquipmentEquip();
        boolean equipped = false;
        if (targetType == CraftTargetType.EQUIPMENT
                || targetType == CraftTargetType.PET
                || targetType == CraftTargetType.MOUNT
                || targetType == CraftTargetType.ARTIFACT) {
            for (int id : ids) {
                if (id == (int) targetId) {
                    equipped = true;
                    break;
                }
            }
        }
        if (equipped)
            mUser.getPlayer().broadcastEquipViewEffect();
    }

    private void sendTargetProto(CraftTargetType targetType, long targetId) {
        if (targetType == CraftTargetType.EQUIPMENT) {
            UserEquipmentEntity equip = mUser.getResources().getItemEquipment(targetId);
            if (equip != null) {
                addResponse(IAction.ITEM_INFO, equip.toProto().build());
            }
        } else if (targetType == CraftTargetType.PET) {
            UserPetEntity pet = mUser.getResources().getPet(targetId);
            if (pet != null) {
                addResponse(IAction.PET_INFO, Pbmethod.PbListPet.newBuilder()
                        .addPets(pet.toProto()).build());
            }
        } else if (targetType == CraftTargetType.MOUNT) {
            UserMountEntity mount = mUser.getResources().getMount(targetId);
            if (mount != null) {
                addResponse(IAction.MOUNT_INFO, Pbmethod.PbListMount.newBuilder()
                        .addMounts(mount.toProto()).build());
            }
        } else if (targetType == CraftTargetType.ARTIFACT) {
            UserArtifactEntity artifact = mUser.getResources().getArtifact(targetId);
            if (artifact != null) {
                addResponse(artifact.toProto().build());
            }
        } else if (targetType == CraftTargetType.CONSUMABLE) {
            UserItemEntity item = mUser.getResources().getItem(targetId);
            if (item != null) {
                addResponse(IAction.CRAFT_CONSUMABLE_INFO, item.toProto().build());
            }
        }
    }

    private List<Long> resolveCraftFee(CraftTargetType targetType, UserArtifactEntity artifact,
            List<Integer> gemRanks) {
        if (targetType == CraftTargetType.ARTIFACT) {
            if (artifact == null)
                return List.of();
            int tier = artifact.getTier() > 0 ? artifact.getTier() : 1;
            return CfgArtifact.getCraftFee(tier, mUser);
        }
        return CfgCraft.sumCraftFees(targetType, gemRanks, mUser);
    }

    private int resolveRequiredMaterialPointId(CraftTargetType targetType, long targetId,
            UserArtifactEntity artifact) {
        if (targetType != CraftTargetType.ARTIFACT)
            return 0;
        if (artifact == null)
            artifact = mUser.getResources().getArtifact(targetId);
        if (artifact == null)
            return -1;
        ResArtifactEntity res = artifact.getRes();
        return res == null ? -1 : res.getPointMain();
    }

    private void appendCraftStatus(List<Long> resp, CraftTargetType targetType, UserDataEntity uData) {
        int craftLevel = uData.getCraftLevel();
        resp.add((long) craftLevel);
        resp.add((long) uData.getCraftExp());
        resp.add((long) CfgCraft.getExpToNext(craftLevel));
        resp.add((long) CfgCraft.getMaxSocket(targetType));
        resp.add((long) CfgCraft.getCurSlot(targetType, craftLevel));
        resp.add((long) CfgCraft.getCraftLevelBonusPercent(craftLevel));
    }

    /** CRAFT_UPDATE (460): [craftLevel, craftExp] — client tự tính expToNext, maxSlot, curSlot, bonus% */
    private void pushCraftUpdate(UserDataEntity uData) {
        addResponse(IAction.CRAFT_UPDATE, getCommonVector(
                (long) uData.getCraftLevel(),
                (long) uData.getCraftExp()
        ));
    }

    private Object resolveCraftTargetEntity(CraftTargetType type, long targetId, UserArtifactEntity craftArtifact) {
        if (type == CraftTargetType.EQUIPMENT)
            return mUser.getResources().getItemEquipment(targetId);
        if (type == CraftTargetType.CONSUMABLE)
            return mUser.getResources().getItem(targetId);
        if (type == CraftTargetType.PET)
            return mUser.getResources().getPet(targetId);
        if (type == CraftTargetType.MOUNT)
            return mUser.getResources().getMount(targetId);
        if (type == CraftTargetType.ARTIFACT)
            return craftArtifact != null ? craftArtifact : mUser.getResources().getArtifact(targetId);
        return null;
    }

    private int resolveItemLevel(CraftTargetType type, long targetId) {
        if (type == CraftTargetType.EQUIPMENT) {
            UserEquipmentEntity equip = mUser.getResources().getItemEquipment(targetId);
            return equip == null ? -1 : equip.getLevel();
        }
        if (type == CraftTargetType.ARTIFACT) {
            UserArtifactEntity artifact = mUser.getResources().getArtifact(targetId);
            if (artifact == null)
                return -1;
            return artifact.getTier() > 0 ? artifact.getTier() : 1;
        }
        if (type == CraftTargetType.PET) {
            UserPetEntity pet = mUser.getResources().getPet(targetId);
            if (pet == null)
                return -1;
            return pet.getTier() > 0 ? pet.getTier() : 1;
        }
        if (type == CraftTargetType.MOUNT) {
            UserMountEntity mount = mUser.getResources().getMount(targetId);
            if (mount == null)
                return -1;
            return mount.getTier() > 0 ? mount.getTier() : 1;
        }
        if (type == CraftTargetType.CONSUMABLE) {
            UserItemEntity item = mUser.getResources().getItem(targetId);
            if (item == null)
                return -1;
            return item.getLevel() > 0 ? item.getLevel() : 1;
        }
        return -1;
    }

    private boolean resetTargetLevel(CraftTargetType type, long targetId) {
        if (type == CraftTargetType.EQUIPMENT) {
            UserEquipmentEntity equip = mUser.getResources().getItemEquipment(targetId);
            if (equip == null) {
                return false;
            }
            if (equip.update(List.of("level", 1))) {
                equip.setLevel(1);
                return true;
            }
            return false;
        }
        return true;
    }

    private String resolveCrafterName() {
        if (mUser == null || mUser.getUser() == null)
            return "";
        String name = mUser.getUser().getName();
        return name != null ? name : "";
    }

    private boolean markTargetCrafted(CraftTargetType type, long targetId) {
        String crafter = resolveCrafterName();
        if (type == CraftTargetType.EQUIPMENT) {
            UserEquipmentEntity equip = mUser.getResources().getItemEquipment(targetId);
            if (equip == null)
                return false;
            if (equip.getIsCraft() == 1)
                return true;
            if (equip.update(List.of("is_craft", 1, "hh", 1))) {
                equip.setIsCraft(1);
                equip.setHh(1);
                return true;
            }
            return false;
        }
        if (type == CraftTargetType.PET) {
            UserPetEntity pet = mUser.getResources().getPet(targetId);
            if (pet == null)
                return false;
            if (pet.getIsCraft() == 1)
                return true;
            if (pet.update(List.of("is_craft", 1, "craft_by", crafter, "hh", 1))) {
                pet.setIsCraft(1);
                pet.setCraftBy(crafter);
                pet.setHh(1);
                return true;
            }
            return false;
        }
        if (type == CraftTargetType.MOUNT) {
            UserMountEntity mount = mUser.getResources().getMount(targetId);
            if (mount == null)
                return false;
            if (mount.getIsCraft() == 1)
                return true;
            if (mount.update(List.of("is_craft", 1, "craft_by", crafter, "hh", 1))) {
                mount.setIsCraft(1);
                mount.setCraftBy(crafter);
                mount.setHh(1);
                return true;
            }
            return false;
        }
        if (type == CraftTargetType.ARTIFACT) {
            UserArtifactEntity artifact = mUser.getResources().getArtifact(targetId);
            if (artifact == null)
                return false;
            if (artifact.getIsCraft() == 1)
                return true;
            if (artifact.update(List.of("is_craft", 1, "craft_by", crafter, "hh", 1))) {
                artifact.setIsCraft(1);
                artifact.setCraftBy(crafter);
                artifact.setHh(1);
                return true;
            }
            return false;
        }
        if (type == CraftTargetType.CONSUMABLE) {
            UserItemEntity item = mUser.getResources().getItem(targetId);
            if (item == null)
                return false;
            if (item.getIsCraft() == 1)
                return true;
            if (item.update(List.of("is_craft", 1, "craft_by", crafter, "hh", 1))) {
                item.setIsCraft(1);
                item.setCraftBy(crafter);
                item.setHh(1);
                return true;
            }
            return false;
        }
        return true;
    }

    private void destroyCraftTarget(CraftTargetType type, long targetId) {
        if (type == CraftTargetType.EQUIPMENT) {
            destroyEquipment(targetId);
        } else if (type == CraftTargetType.CONSUMABLE) {
            destroyConsumable(targetId);
        } else if (type == CraftTargetType.PET) {
            destroyPet(targetId);
        } else if (type == CraftTargetType.MOUNT) {
            destroyMount(targetId);
        } else if (type == CraftTargetType.ARTIFACT) {
            destroyArtifact(targetId);
        } else if (type == CraftTargetType.SKIN) {
            destroySkin(targetId);
        }
    }

    private void destroyConsumable(long itemRowId) {
        UserItemEntity item = mUser.getResources().getItem(itemRowId);
        if (item == null) {
            return;
        }
        if (item.getLockDestroy() == 1) {
            return;
        }
        ResItem.removeUserItemRow(mUser, item, DetailActionType.CRAFT_EXECUTE.getKey(itemRowId));
    }

    private void destroyEquipment(long equipId) {
        UserEquipmentEntity equip = mUser.getResources().getItemEquipment(equipId);
        if (equip == null) {
            return;
        }
        if (equip.getLockDestroy() == 1 || equip.isEquip()) {
            return;
        }
        if (equip.deleteFromDb()) {
            mUser.getResources().removeItemEquip(List.of(equip));
        }
    }

    private void destroyPet(long petId) {
        UserPetEntity pet = mUser.getResources().getPet(petId);
        if (pet == null) {
            return;
        }
        pet.syncEquipFlag(mUser);
        if (pet.isEquip()) {
            return;
        }
        Bonus.clearItemFromSlot(mUser, Bonus.BONUS_PET, petId);
        if (pet.deleteFromDb()) {
            mUser.getResources().removePet(petId);
        }
    }

    private void destroyMount(long mountId) {
        UserMountEntity mount = mUser.getResources().getMount(mountId);
        if (mount == null) {
            return;
        }
        mount.syncEquipFlag(mUser);
        if (mount.isEquip()) {
            return;
        }
        Bonus.clearItemFromSlot(mUser, Bonus.BONUS_MOUNT, mountId);
        if (mount.deleteFromDb()) {
            mUser.getResources().removeMount(mountId);
        }
    }

    private void destroyArtifact(long rowId) {
        UserArtifactEntity artifact = mUser.getResources().getArtifact(rowId);
        if (artifact == null) {
            return;
        }
        if (Bonus.isArtifactEquipped(mUser, rowId) && !Bonus.clearTreasureEquipSlot(mUser)) {
            return;
        }
        Bonus.clearItemFromSlot(mUser, Bonus.BONUS_ARTIFACT, rowId);
        if (artifact.deleteFromDb()) {
            mUser.getResources().removeArtifact(rowId);
        }
    }

    private void destroySkin(long skinId) {
        UserSkinEntity skin = mUser.getResources().getSkin(skinId);
        if (skin == null) {
            return;
        }
        if (DBJPA.delete("user_skin", "id", skinId, "user_id", skin.getUserId())) {
            mUser.getResources().removeSkin(skinId);
        }
    }

    private boolean applySocketStat(CraftTargetType type, long targetId, UserMaterialEntity gem) {
        ResMaterialEntity res = gem.getRes();
        if (res == null || res.getPointId() <= 0) {
            return false;
        }
        int pointId = res.getPointId();
        float addValue = gem.getValue();
        if (type == CraftTargetType.CONSUMABLE) {
            addValue = addValue / 4f;
        }

        if (type == CraftTargetType.EQUIPMENT) {
            UserEquipmentEntity equip = mUser.getResources().getItemEquipment(targetId);
            if (equip == null) {
                return false;
            }
            long statValue = Math.round(addValue * 100);
            List<Long> add = Arrays.asList((long) pointId, statValue);
            List<Long> points = IMath.mergePointWeapon(equip.getPoint(), add);
            if (equip.update(Arrays.asList("data", GsonUtil.toJson(points)))) {
                equip.setData(points.toString());
                return true;
            }
            return false;
        }
        if (type == CraftTargetType.PET) {
            UserPetEntity pet = mUser.getResources().getPet(targetId);
            if (pet == null)
                return false;
            List<Float> merged = CraftPointDataUtil.mergePointPair(
                    CraftPointDataUtil.parseDataFloats(pet.getData()), pointId, addValue);
            String dataJson = StringHelper.toDBString(merged);
            if (pet.update(List.of("data", dataJson))) {
                pet.setData(dataJson);
                return true;
            }
            return false;
        }
        if (type == CraftTargetType.MOUNT) {
            UserMountEntity mount = mUser.getResources().getMount(targetId);
            if (mount == null)
                return false;
            List<Float> merged = CraftPointDataUtil.mergePointPair(
                    CraftPointDataUtil.parseDataFloats(mount.getData()), pointId, addValue);
            String dataJson = StringHelper.toDBString(merged);
            if (mount.update(List.of("data", dataJson))) {
                mount.setData(dataJson);
                return true;
            }
            return false;
        }
        if (type == CraftTargetType.CONSUMABLE) {
            UserItemEntity item = mUser.getResources().getItem(targetId);
            if (item == null)
                return false;
            List<Float> merged = CraftPointDataUtil.mergePointPair(
                    CraftPointDataUtil.parseDataFloats(item.getData()), pointId, addValue);
            String dataJson = StringHelper.toDBString(merged);
            if (item.update(List.of("data", dataJson))) {
                item.setData(dataJson);
                return true;
            }
            return false;
        }
        return false;
    }

    private void applyConsumableTransform(long targetId, float hpOriginal) {
        UserItemEntity item = mUser.getResources().getItem(targetId);
        if (item == null)
            return;
        int tier = CfgCraft.rollConsumableTransformTier();
        int hh = CfgCraft.hhFromTransformTier(tier);
        if (tier <= 0) {
            if (item.update(List.of("hh", hh))) {
                item.setHh(hh);
            }
            return;
        }
        int iconId = CfgCraft.getConsumableTransformIcon(tier);
        if (iconId <= 0)
            return;
        float mul = CfgCraft.getConsumableHpMul(tier);
        String dataJson = CraftPointDataUtil.applyHpTransform(item.getData(), hpOriginal, mul);
        if (item.update(Arrays.asList("data", dataJson, "icon", iconId, "hh", hh))) {
            item.setData(dataJson);
            item.setIcon(iconId);
            item.setHh(hh);
        }
    }

    private void applyEquipmentTransform(long targetId) {
        applyTransform(targetId, CfgCraft.rollTransformTier(mUser.getTransmuteRateBonus()),
                mUser.getResources().getItemEquipment(targetId),
                equip -> equip == null ? null : equip.getResEquipment(),
                ResItemEquipmentEntity::getTransformIcon,
                (equip, dataJson, iconId, hh) -> {
                    if (equip.update(Arrays.asList("data", dataJson, "icon", iconId, "hh", hh))) {
                        equip.setData(dataJson);
                        equip.setIcon(iconId);
                        equip.setHh(hh);
                        return true;
                    }
                    return false;
                },
                UserEquipmentEntity::getData);
    }

    private void applyPetTransform(long targetId) {
        applyTransform(targetId, CfgCraft.rollTransformTier(mUser.getTransmuteRateBonus()),
                mUser.getResources().getPet(targetId),
                pet -> pet == null ? null : pet.getResPet(),
                ResPetEntity::getTransformIcon,
                (pet, dataJson, iconId, hh) -> {
                    if (pet.update(Arrays.asList("data", dataJson, "icon", iconId, "hh", hh))) {
                        pet.setData(dataJson);
                        pet.setIcon(iconId);
                        pet.setHh(hh);
                        return true;
                    }
                    return false;
                },
                UserPetEntity::getData);
    }

    private void applyMountTransform(long targetId) {
        applyTransform(targetId, CfgCraft.rollTransformTier(mUser.getTransmuteRateBonus()),
                mUser.getResources().getMount(targetId),
                mount -> mount == null ? null : mount.getRes(),
                ResMountEntity::getTransformIcon,
                (mount, dataJson, iconId, hh) -> {
                    if (mount.update(Arrays.asList("data", dataJson, "icon", iconId, "hh", hh))) {
                        mount.setData(dataJson);
                        mount.setIcon(iconId);
                        mount.setHh(hh);
                        return true;
                    }
                    return false;
                },
                UserMountEntity::getData);
    }

    /** Artifact: chỉ cập nhật hh (không icon/stat), rate hóa hình giống equip. */
    private void applyArtifactTransform(long targetId) {
        int tier = CfgCraft.rollTransformTier(mUser.getTransmuteRateBonus());
        if (tier <= 0)
            return;
        UserArtifactEntity artifact = mUser.getResources().getArtifact(targetId);
        if (artifact == null)
            return;
        int hh = CfgCraft.hhFromTransformTier(tier);
        if (artifact.update(List.of("hh", hh))) {
            artifact.setHh(hh);
        }
    }

    @FunctionalInterface
    private interface TransformIconResolver<R> {
        int resolve(R res, int tier);
    }

    @FunctionalInterface
    private interface TransformUpdater<T> {
        boolean update(T target, String dataJson, int iconId, int hh);
    }

    @FunctionalInterface
    private interface DataGetter<T> {
        String get(T target);
    }

    @FunctionalInterface
    private interface ResGetter<T, R> {
        R get(T target);
    }

    private <T, R> void applyTransform(long targetId, int tier, T target, ResGetter<T, R> resGetter,
            TransformIconResolver<R> iconResolver, TransformUpdater<T> updater, DataGetter<T> dataGetter) {
        if (tier <= 0 || target == null)
            return;
        R res = resGetter.get(target);
        if (res == null)
            return;
        int iconId = iconResolver.resolve(res, tier);
        if (iconId <= 0)
            return;
        float mul = CfgCraft.getTransformStatMul(tier);
        int hh = CfgCraft.hhFromTransformTier(tier);
        String dataJson = CraftPointDataUtil.scaleData(dataGetter.get(target), mul);
        updater.update(target, dataJson, iconId, hh);
    }

    private boolean removeMaterial(UserMaterialEntity gem) {
        if (!gem.deleteFromDb()) {
            return false;
        }
        mUser.getResources().removeMaterial(gem.getId());
        return true;
    }

    private void addPriceTreasure(CraftTargetType type, long targetId, int gain) {
        if (gain <= 0) {
            return;
        }
        if (type == CraftTargetType.EQUIPMENT) {
            UserEquipmentEntity equip = mUser.getResources().getItemEquipment(targetId);
            if (equip == null) {
                return;
            }
            int newValue = equip.getPriceTreasure() + gain;
            if (equip.update(List.of("price_treasure", newValue))) {
                equip.setPriceTreasure(newValue);
            }
        } else if (type == CraftTargetType.PET) {
            UserPetEntity pet = mUser.getResources().getPet(targetId);
            if (pet == null) {
                return;
            }
            int newValue = pet.getPriceTreasure() + gain;
            if (pet.update(List.of("price_treasure", newValue))) {
                pet.setPriceTreasure(newValue);
            }
        } else if (type == CraftTargetType.MOUNT) {
            UserMountEntity mount = mUser.getResources().getMount(targetId);
            if (mount == null) {
                return;
            }
            int newValue = mount.getPriceTreasure() + gain;
            if (mount.update(List.of("price_treasure", newValue))) {
                mount.setPriceTreasure(newValue);
            }
        }
    }
}
