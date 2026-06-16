package game.treasure.controller;

import game.config.CfgMaterial;
import game.config.aEnum.DetailActionType;
import game.config.lang.Lang;
import game.treasure.mapping.UserMaterialEntity;
import game.treasure.mapping.main.ResMaterialEntity;
import game.treasure.server.IAction;
import game.treasure.service.user.Bonus;
import io.netty.channel.Channel;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.NumberUtil;
import ozudo.base.log.Logs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MaterialHandler extends AHandler {

    static MaterialHandler instance;

    public static MaterialHandler getInstance() {
        if (instance == null) {
            instance = new MaterialHandler();
        }
        return instance;
    }

    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        mHandler.put(IAction.MATERIAL_UPGRADE, this);
        mHandler.put(IAction.MATERIAL_MERGE, this);
    }

    @Override
    public AHandler newInstance() {
        return new MaterialHandler();
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        try {
            if (actionId == IAction.MATERIAL_UPGRADE) {
                upgradeMaterial();
            } else if (actionId == IAction.MATERIAL_MERGE) {
                mergeMaterial();
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    private void upgradeMaterial() {
        List<Long> inputs = getInputALong();
        if (inputs.isEmpty()) {
            addErrParam();
            return;
        }
        long materialRowId = inputs.get(0);
        UserMaterialEntity gem = mUser.getResources().getMaterial(materialRowId);
        if (gem == null) {
            addErrResponse(getLang(Lang.err_item_equip_not_found));
            return;
        }
        ResMaterialEntity res = gem.getRes();
        if (res == null) {
            addErrParam();
            return;
        }
        if (!CfgMaterial.canUpgrade(gem.getLevel())) {
            addErrResponse(getLang(Lang.err_max_level));
            return;
        }
        int tier = gem.getTier();
        List<Long> fee = CfgMaterial.getUpgradeFee(tier, gem.getTier(), gem.getLevel());
        if (fee.isEmpty()) {
            addErrParam();
            return;
        }
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        List<Long> aBonus = Bonus.receiveListItem(mUser,
                DetailActionType.UPGRADE_MATERIAL.getKey(gem.getId()), fee);
        if (aBonus.isEmpty()) {
            addErrSystem();
            return;
        }
        int newLevel = gem.getLevel() + 1;
        float newValue = gem.getValue() + CfgMaterial.rollValue(res, gem.getTier());
        float newSocketRate = CfgMaterial.nextSocketRate(gem.getSocketRate());
        if (gem.update(Arrays.asList("level", newLevel, "value", newValue, "socket_rate", newSocketRate))) {
            gem.setLevel(newLevel);
            gem.setValue(newValue);
            gem.setSocketRate(newSocketRate);
            addBonusToast(aBonus);
            addResponse(gem.toProto().build());
        } else {
            Bonus.receiveListItem(mUser, DetailActionType.UPDATE_FAIL.getKey(), Bonus.reverseBonus(fee));
            addErrSystem();
        }
    }

    /**
     * Request: [gemRowId1, gemRowId2, ...] (2–8 viên)
     * Response MATERIAL_MERGE: {@link protocol.Pbmethod.PbMaterial} (viên kết quả; fail = trả 1 viên rank cao nhất)
     */
    private void mergeMaterial() {
        List<Long> inputs = getInputALong();
        if (inputs.size() < CfgMaterial.getMergeMinMaterials()) {
            addErrParam();
            return;
        }
        if (inputs.size() > CfgMaterial.getMergeMaxMaterials()) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }

        List<UserMaterialEntity> gems = new ArrayList<>();
        java.util.HashSet<Long> seen = new java.util.HashSet<>();
        for (Long rowId : inputs) {
            if (!seen.add(rowId)) {
                addErrParam();
                return;
            }
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
        }

        CfgMaterial.MergePlan plan = CfgMaterial.buildMergePlan(gems);
        if (plan == null) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }

        long mergeCost = CfgMaterial.sumMergeSellPrice(gems);
        if (mergeCost <= 0) {
            addErrParam();
            return;
        }
        List<Long> fee = Bonus.viewGold(-mergeCost);
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        List<Long> feeBonus = Bonus.receiveListItem(mUser,
                DetailActionType.MATERIAL_MERGE.getKey(gems.get(0).getId()), fee);
        if (feeBonus.isEmpty()) {
            addErrSystem();
            return;
        }
        addBonusPrivate(feeBonus);

        int successRate = CfgMaterial.calcMergeSuccessPercent(plan);
        boolean success = NumberUtil.getRandom(100) < successRate;

        UserMaterialEntity output;
        if (success) {
            int materialId = CfgMaterial.pickMergeOutputMaterialId(plan.materialCount);
            if (CfgMaterial.get(materialId) == null) {
                Bonus.receiveListItem(mUser, DetailActionType.UPDATE_FAIL.getKey(), Bonus.reverseBonus(fee));
                addErrParam();
                return;
            }
            output = new UserMaterialEntity(mUser.getUser().getId(), materialId, plan.outputRank);
        } else {
            UserMaterialEntity refund = CfgMaterial.pickMergeFailReturnGem(gems);
            if (refund == null) {
                Bonus.receiveListItem(mUser, DetailActionType.UPDATE_FAIL.getKey(), Bonus.reverseBonus(fee));
                addErrSystem();
                return;
            }
            output = UserMaterialEntity.cloneFrom(refund);
        }

        if (!DBJPA.save(output)) {
            Bonus.receiveListItem(mUser, DetailActionType.UPDATE_FAIL.getKey(), Bonus.reverseBonus(fee));
            addErrSystem();
            return;
        }

        for (UserMaterialEntity gem : gems) {
            if (!gem.deleteFromDb()) {
                Bonus.receiveListItem(mUser, DetailActionType.UPDATE_FAIL.getKey(), Bonus.reverseBonus(fee));
                addErrSystem();
                return;
            }
            mUser.getResources().removeMaterial(gem.getId());
        }

        mUser.getResources().addMaterial(output);
        addResponse(output.toProto().build());
    }
}
