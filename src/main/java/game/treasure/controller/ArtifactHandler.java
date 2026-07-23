package game.treasure.controller;



import game.config.ArtifactDataSlot;
import game.config.CfgArtifact;
import game.object.MyUser;

import game.config.aEnum.DetailActionType;

import game.config.lang.Lang;

import game.treasure.mapping.UserArtifactEntity;

import game.treasure.mapping.UserEntity;

import game.treasure.mapping.UserEquipmentEntity;

import game.treasure.mapping.main.ResArtifactEntity;

import game.treasure.server.IAction;

import game.treasure.controller.UserHandler;
import game.treasure.service.user.Bonus;
import game.treasure.service.user.ArtifactBuffTargets;
import game.treasure.service.user.UserBuff;

import game.treasure.service.user.ItemSlotHelper;

import io.netty.channel.Channel;

import ozudo.base.log.Logs;

import protocol.Pbmethod;



import java.util.ArrayList;

import java.util.Arrays;

import java.util.List;

import java.util.Map;



public class ArtifactHandler extends AHandler {



    static ArtifactHandler instance;



    public static ArtifactHandler getInstance() {

        if (instance == null)

            instance = new ArtifactHandler();

        return instance;

    }



    @Override

    public void initAction(Map<Integer, AHandler> mHandler) {

        mHandler.put(IAction.ARTIFACT_UPGRADE, this);

        mHandler.put(IAction.ARTIFACT_EQUIP, this);

        mHandler.put(IAction.ARTIFACT_UNEQUIP, this);

        mHandler.put(IAction.ARTIFACT_SELL, this);
        mHandler.put(IAction.ARTIFACT_USE, this);
        mHandler.put(IAction.ARTIFACT_INFO, this);

    }



    @Override

    public AHandler newInstance() {

        return new ArtifactHandler();

    }



    @Override

    public void handle(Channel channel, String session, int actionId, byte[] requestData) {

        super.handle(channel, session, actionId, requestData);

        try {

            switch (actionId) {

                case IAction.ARTIFACT_UPGRADE -> upgradeArtifact();

                case IAction.ARTIFACT_EQUIP -> equipArtifact();

                case IAction.ARTIFACT_UNEQUIP -> unequipArtifact();

                case IAction.ARTIFACT_SELL -> sellArtifact();

                case IAction.ARTIFACT_USE -> useArtifact();

                case IAction.ARTIFACT_INFO -> listArtifacts(getInputALong(), this, mUser);

            }

        } catch (Exception ex) {

            Logs.error(ex);

        }

    }



    public static void listArtifacts(List<Long> ids, AHandler handler, game.object.MyUser mUser) {
        Pbmethod.PbListArtifact.Builder lst = Pbmethod.PbListArtifact.newBuilder();
        for (int i = 0; i < ids.size(); i++) {
            UserArtifactEntity artifact = mUser.getResources().getArtifact(ids.get(i));
            if (artifact == null) {
                handler.addErrResponse(getLang(mUser, Lang.err_item_equip_not_found));
                return;
            }
            lst.addArtifacts(artifact.toProto());
        }
        handler.addResponse(IAction.ARTIFACT_INFO, lst.build());
    }



    private void upgradeArtifact() {

        List<Long> inputs = getInputALong();

        if (inputs.isEmpty()) {

            addErrParam();

            return;

        }

        long artifactRowId = inputs.get(0);

        UserArtifactEntity artifact = mUser.getResources().getArtifact(artifactRowId);

        if (artifact == null) {

            addErrResponse(getLang(Lang.err_item_equip_not_found));

            return;

        }

        ResArtifactEntity res = artifact.getRes();

        if (res == null) {

            addErrParam();

            return;

        }

        if (!CfgArtifact.canUpgrade(artifact.getLevel())) {

            addErrResponse(getLang(Lang.err_max_level));

            return;

        }

        List<Long> fee = CfgArtifact.getUpgradeFee(artifact, artifact.getLevel(), mUser);

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

                DetailActionType.UPGRADE_ARTIFACT.getKey(artifact.getId()), fee);

        if (aBonus.isEmpty()) {

            addErrSystem();

            return;

        }

        int newLevel = artifact.getLevel() + 1;

        if (artifact.update(Arrays.asList("level", newLevel))) {

            artifact.setLevel(newLevel);

            if (Bonus.isArtifactEquipped(mUser, artifact.getId())) {

                syncTreasureEquipFields(artifact);

                finishArtifactEquipChange(null);

            }

            addBonusToast(aBonus);

            addResponse(artifact.toProto().build());

        } else {

            Bonus.receiveListItem(mUser, DetailActionType.UPDATE_FAIL.getKey(), Bonus.reverseBonus(fee));

            addErrSystem();

        }

    }



    private void equipArtifact() {

        if (CfgArtifact.isArtifactOnCooldown(mUser)) {
            addErrResponse(getLang(Lang.err_artifact_cooldown));
            return;
        }

        List<Long> inputs = getInputALong();

        if (inputs.isEmpty()) {

            addErrParam();

            return;

        }

        long rowId = inputs.get(0);

        UserArtifactEntity artifact = mUser.getResources().getArtifact(rowId);

        if (artifact == null) {

            addErrResponse(getLang(Lang.err_item_equip_not_found));

            return;

        }

        if (Bonus.isArtifactEquipped(mUser, rowId)) {

            addErrResponse(getLang(Lang.err_params));

            return;

        }



        int treasureIdx = UserEntity.equipSlotIndex(Pbmethod.EquipSlotType.TREASURE.getNumber());

        List<Integer> lst = mUser.getUser().normalizeItemEquipList();

        int oldRowId = lst.get(treasureIdx);



        List<Long> slots = mUser.getUData().getItemSlotList();

        int bagCount = mUser.getUData().getSlotBagUI();

        Integer newBagSlot = ItemSlotHelper.findSlotOf(slots, 0, bagCount, Bonus.BONUS_ARTIFACT, rowId);



        Bonus.moveArtifactOutOfBag(mUser, artifact);



        if (oldRowId > 0 && oldRowId != rowId) {

            UserArtifactEntity oldArtifact = mUser.getResources().getArtifact(oldRowId);

            if (oldArtifact != null) {

                if (newBagSlot != null) {

                    if (!Bonus.moveArtifactToBagSlot(mUser, oldArtifact, newBagSlot)) {

                        rollbackEquipArtifact(artifact, newBagSlot, oldRowId, lst);

                        addErrResponse(getLang(Lang.err_max_slot));

                        return;

                    }

                } else if (!Bonus.moveArtifactToBag(mUser, oldArtifact)) {

                    rollbackEquipArtifact(artifact, newBagSlot, oldRowId, lst);

                    addErrResponse(getLang(Lang.err_max_slot));

                    return;

                }

            } else {

                UserEquipmentEntity oldEquip = mUser.getResources().getEquipment(oldRowId);

                if (oldEquip != null) {

                    if (!Bonus.moveEquipmentToBag(mUser, oldEquip)) {

                        rollbackEquipArtifact(artifact, newBagSlot, oldRowId, lst);

                        addErrResponse(getLang(Lang.err_max_slot));

                        return;

                    }

                    oldEquip.unEquip();

                }

            }

        }



        lst.set(treasureIdx, (int) artifact.getId());

        lst.set(treasureIdx + 1, artifact.getArtifactId());

        lst.set(treasureIdx + 2, artifact.getLevel());

        if (!mUser.getUser().updateItemEquip(lst)) {

            addErrSystem();

            return;

        }

        finishArtifactEquipChange(buildSlotUpdatePayload(artifact, newBagSlot));

    }



    void rollbackEquipArtifact(UserArtifactEntity artifact, Integer bagSlot, int oldRowId, List<Integer> lst) {

        if (bagSlot != null)

            Bonus.moveArtifactToBagSlot(mUser, artifact, bagSlot);

        else

            Bonus.moveArtifactToBag(mUser, artifact);

        int treasureIdx = UserEntity.equipSlotIndex(Pbmethod.EquipSlotType.TREASURE.getNumber());

        if (oldRowId > 0) {

            UserArtifactEntity old = mUser.getResources().getArtifact(oldRowId);

            if (old != null) {

                lst.set(treasureIdx, oldRowId);

                lst.set(treasureIdx + 1, old.getArtifactId());

                lst.set(treasureIdx + 2, old.getLevel());

                mUser.getUser().updateItemEquip(lst);

                Bonus.moveArtifactOutOfBag(mUser, old);

            }

        }

    }



    private void unequipArtifact() {

        if (CfgArtifact.isArtifactOnCooldown(mUser)) {
            addErrResponse(getLang(Lang.err_artifact_cooldown));
            return;
        }

        List<Long> inputs = getInputALong();

        if (inputs.isEmpty()) {

            addErrParam();

            return;

        }

        long rowId = inputs.get(0);

        if (!Bonus.isArtifactEquipped(mUser, rowId)) {

            addErrResponse(getLang(Lang.err_params));

            return;

        }

        UserArtifactEntity artifact = mUser.getResources().getArtifact(rowId);

        if (artifact == null) {

            addErrResponse(getLang(Lang.err_item_equip_not_found));

            return;

        }

        if (!mUser.getResources().canAddBagItem(1)) {

            addErrResponse(getLang(Lang.err_max_slot));

            return;

        }

        if (!Bonus.clearTreasureEquipSlot(mUser)) {

            addErrSystem();

            return;

        }

        if (!Bonus.moveArtifactToBag(mUser, artifact)) {

            addErrResponse(getLang(Lang.err_max_slot));

            return;

        }

        finishArtifactEquipChange(buildSlotUpdatePayload(artifact, artifact.getBagSlot()));

    }



    private void sellArtifact() {

        if (CfgArtifact.isArtifactOnCooldown(mUser)) {
            addErrResponse(getLang(Lang.err_artifact_cooldown));
            return;
        }

        List<Long> inputs = getInputALong();

        if (inputs.isEmpty()) {

            addErrParam();

            return;

        }

        long rowId = inputs.get(0);

        UserArtifactEntity artifact = mUser.getResources().getArtifact(rowId);

        if (artifact == null) {

            addErrResponse(getLang(Lang.item_not_own));

            return;

        }

        boolean wasEquipped = Bonus.isArtifactEquipped(mUser, rowId);

        if (wasEquipped && !Bonus.clearTreasureEquipSlot(mUser)) {

            addErrSystem();

            return;

        }

        Bonus.clearItemFromSlot(mUser, Bonus.BONUS_ARTIFACT, rowId);

        if (!artifact.deleteFromDb()) {

            addErrSystem();

            return;

        }

        mUser.getResources().removeArtifact(rowId);

        List<Long> sellBonus = CfgArtifact.getPriceSellArtifact(artifact);

        addBonusToast(Bonus.receiveListItem(mUser,

                DetailActionType.SELL_ARTIFACT.getKey(artifact.getArtifactId()), sellBonus));



        Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();

        pb.addAVector(getCommonVector(rowId, 1L));

        if (wasEquipped) {

            pb.addAVector(user.reCalculatePoint(mUser).toCommonVector());

            pb.addAVector(getCommonIntVector(mUser.getUser().normalizeItemEquipList()));

        }

        addResponse(pb.build());

    }

    private void useArtifact() {
        List<Long> inputs = getInputALong();
        if (inputs.isEmpty()) {
            addErrParam();
            return;
        }
        long rowId = inputs.get(0);
        if (!Bonus.isArtifactEquipped(mUser, rowId)) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        UserArtifactEntity artifact = mUser.getResources().getArtifact(rowId);
        if (artifact == null) {
            addErrResponse(getLang(Lang.err_item_equip_not_found));
            return;
        }
        if (CfgArtifact.isArtifactOnCooldown(mUser)) {
            addErrResponse(getLang(Lang.err_artifact_cooldown));
            return;
        }
        long cdSec = Math.round(artifact.getEffectiveSlot(game.config.ArtifactDataSlot.IDX_CD));
        if (cdSec <= 0) {
            addErrParam();
            return;
        }
        long now = System.currentTimeMillis();
        if (!mUser.getUData().update(Arrays.asList("time_active_artifact", now))) {
            addErrSystem();
            return;
        }
        mUser.getUData().setTimeActiveArtifact(now);
        mUser.queueUserDataInfo();
        ResArtifactEntity res = artifact.getRes();
        if (res != null && res.getArtifactType() != null) {
            int pointId = res.getPointMain();
            float effValue = artifact.getEffectiveSlot(ArtifactDataSlot.IDX_VALUE);
            long durationSec = Math.round(artifact.getEffectiveSlot(ArtifactDataSlot.IDX_TIME));
            if (pointId != 0 && effValue != 0 && durationSec > 0) {
                long valueScaled = Math.round(effValue * 1000);
                List<MyUser> targets = ArtifactBuffTargets.resolve(mUser, res.getArtifactType(), artifact);
                for (MyUser target : targets) {
                    if (target != null)
                        UserBuff.grantBuff(target, pointId, valueScaled, durationSec);
                }
            }
        }
        UserHandler.buffInfo(mUser);
        addResponse(getCommonVector(now, cdSec));
    }



    void syncTreasureEquipFields(UserArtifactEntity artifact) {

        int treasureIdx = UserEntity.equipSlotIndex(Pbmethod.EquipSlotType.TREASURE.getNumber());

        List<Integer> lst = mUser.getUser().normalizeItemEquipList();

        if (lst.get(treasureIdx) == (int) artifact.getId()) {

            lst.set(treasureIdx + 2, artifact.getLevel());

            mUser.getUser().updateItemEquip(lst);

        }

    }



    List<Long> buildSlotUpdatePayload(UserArtifactEntity artifact, Integer freedSlot) {

        List<Long> data = new ArrayList<>();

        if (artifact != null && artifact.getBagSlot() >= 0) {

            data.add(artifact.getId());

            data.add((long) artifact.getBagSlot());

        }

        if (freedSlot != null && freedSlot >= 0) {

            data.add(0L);

            data.add((long) freedSlot);

        }

        return data;

    }



    private void finishArtifactEquipChange(List<Long> slotPairUpdates) {

        Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();

        pb.addAVector(user.reCalculatePoint(mUser).toCommonVector());

        pb.addAVector(getCommonIntVector(mUser.getUser().normalizeItemEquipList()));

        if (slotPairUpdates != null && !slotPairUpdates.isEmpty())

            pb.addAVector(getCommonVector(slotPairUpdates));

        addResponse(pb.build());

        mUser.reCalculatePoint();

        addResponse(IAction.UPDATE_BAG, mUser.getResources().buildUpdateBagPayload());

    }

}


