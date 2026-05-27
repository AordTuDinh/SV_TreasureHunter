package game.treasure.controller;

import game.battle.calculate.IMath;
import game.config.CfgCraft;
import game.config.aEnum.CraftTargetType;
import game.config.aEnum.DetailActionType;
import game.config.lang.Lang;
import game.treasure.mapping.UserDataEntity;
import game.treasure.mapping.UserItemEquipmentEntity;
import game.treasure.mapping.UserMaterialEntity;
import game.treasure.mapping.UserPetEntity;
import game.treasure.mapping.main.ResMaterialEntity;
import game.treasure.server.IAction;
import game.treasure.service.user.Bonus;
import io.netty.channel.Channel;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;
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

        if (targetType == CraftTargetType.MOUNT || targetType == CraftTargetType.SKIN) {
            addErrResponse(getLang(Lang.err_system_down));
            return;
        }

        UserDataEntity uData = mUser.getUData();
        int craftLevel = uData.getCraftLevel();
        int curSlot = CfgCraft.getCurSlot(targetType, craftLevel);
        if (gemRowIds.size() > curSlot) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }

        List<UserMaterialEntity> gems = new ArrayList<>();
        List<Integer> gemRanks = new ArrayList<>();
        int maxGemRank = 0;
        for (Long rowId : gemRowIds) {
            UserMaterialEntity gem = mUser.getResources().getMaterial(rowId);
            if (gem == null) {
                addErrResponse(getLang(Lang.err_item_equip_not_found));
                return;
            }
            if (gem.getRes() == null) {
                addErrParam();
                return;
            }
            gems.add(gem);
            gemRanks.add(gem.getMatRank());
            maxGemRank = Math.max(maxGemRank, gem.getMatRank());
        }

        int itemLevel = resolveItemLevel(targetType, targetId);
        if (itemLevel < 0) {
            addErrResponse(getLang(Lang.err_item_equip_not_found));
            return;
        }

        List<Long> fee = CfgCraft.sumCraftFees(targetType, gemRanks);
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

        int craftPercent = CfgCraft.getCraftSuccessPercent(targetType, maxGemRank, itemLevel, craftLevel);
        boolean craftOk = NumberUtil.getRandom(100) < craftPercent;

        List<Long> resp = new ArrayList<>();
        resp.add(craftOk ? 1L : 0L);
        resp.add((long) targetType.id);
        resp.add(targetId);

        if (!craftOk) {
            if (targetType.losesTargetOnCraftFail()) {
                destroyEquipment(targetId);
            }
            resp.add(-1L);
            resp.add(0L);
            appendCraftStatus(resp, targetType, uData);
            addResponse(getCommonVector(resp));
            return;
        }

        if (!resetTargetLevel(targetType, targetId)) {
            Bonus.receiveListItem(mUser, DetailActionType.UPDATE_FAIL.getKey(), Bonus.reverseBonus(fee));
            addErrSystem();
            return;
        }

        resp.add((long) resolveItemLevel(targetType, targetId));
        resp.add((long) gems.size());

        int totalExpGain = 0;
        for (UserMaterialEntity gem : gems) {
            boolean socketOk = NumberUtil.getRandom(100) < gem.getSocketSuccessPercent();
            boolean consumed = false;
            if (socketOk) {
                boolean statApplied = applySocketStat(targetType, targetId, gem);
                boolean canConsume = statApplied || targetType == CraftTargetType.PET;
                if (canConsume && removeMaterial(gem)) {
                    consumed = true;
                    if (CfgCraft.grantsCraftExp(craftLevel, gem.getMatRank())) {
                        totalExpGain += CfgCraft.getCraftExpByRank(gem.getMatRank());
                    }
                }
            }
            resp.add(gem.getId());
            resp.add(socketOk ? 1L : 0L);
            resp.add(consumed ? 1L : 0L);
        }

        boolean craftLeveled = false;
        if (totalExpGain > 0) {
            craftLeveled = CfgCraft.addCraftExp(uData, totalExpGain);
            uData.update(Arrays.asList("craft_level", uData.getCraftLevel(), "craft_exp", uData.getCraftExp()));
        }

        appendCraftStatus(resp, targetType, uData);
        addResponse(getCommonVector(resp));

        if (craftLeveled) {
            pushCraftUpdate(uData);
        }

        sendTargetProto(targetType, targetId);
    }

    private void sendTargetProto(CraftTargetType targetType, long targetId) {
        if (targetType == CraftTargetType.EQUIPMENT) {
            UserItemEquipmentEntity equip = mUser.getResources().getItemEquipment(targetId);
            if (equip != null) {
                addResponse(IAction.ITEM_INFO, equip.toProto().build());
            }
        } else if (targetType == CraftTargetType.PET) {
            UserPetEntity pet = mUser.getResources().getPetByConfigId(Math.toIntExact(targetId));
            if (pet != null) {
                addResponse(pet.toProto().build());
            }
        }
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

    private int resolveItemLevel(CraftTargetType type, long targetId) {
        if (type == CraftTargetType.EQUIPMENT) {
            UserItemEquipmentEntity equip = mUser.getResources().getItemEquipment(targetId);
            return equip == null ? -1 : equip.getLevel();
        }
        return -1;
    }

    private boolean resetTargetLevel(CraftTargetType type, long targetId) {
        if (type == CraftTargetType.EQUIPMENT) {
            UserItemEquipmentEntity equip = mUser.getResources().getItemEquipment(targetId);
            if (equip == null) {
                return false;
            }
            if (equip.update(List.of("level", 1))) {
                equip.setLevel(1);
                return true;
            }
            return false;
        }
        return false;
    }

    private void destroyEquipment(long equipId) {
        UserItemEquipmentEntity equip = mUser.getResources().getItemEquipment(equipId);
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

    private boolean applySocketStat(CraftTargetType type, long targetId, UserMaterialEntity gem) {
        ResMaterialEntity res = gem.getRes();
        if (res == null || res.getPointId() <= 0) {
            return false;
        }
        if (type != CraftTargetType.EQUIPMENT) {
            return false;
        }
        UserItemEquipmentEntity equip = mUser.getResources().getItemEquipment(targetId);
        if (equip == null) {
            return false;
        }
        long statValue = Math.round(gem.getValue() * 100);
        List<Long> add = Arrays.asList((long) res.getPointId(), statValue);
        List<Long> points = IMath.mergePointWeapon(equip.getPointList(), add);
        if (equip.update(Arrays.asList("point", GsonUtil.toJson(points)))) {
            equip.setPointList(points);
            return true;
        }
        return false;
    }

    private boolean removeMaterial(UserMaterialEntity gem) {
        if (!gem.deleteFromDb()) {
            return false;
        }
        mUser.getResources().removeMaterial(gem.getId());
        return true;
    }
}
