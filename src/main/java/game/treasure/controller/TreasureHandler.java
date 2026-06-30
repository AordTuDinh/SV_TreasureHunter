package game.treasure.controller;

import game.config.aEnum.DetailActionType;
import game.config.CfgArtifact;
import game.config.CfgCraft;
import game.config.lang.Lang;
import game.treasure.mapping.UserEquipmentEntity;
import game.treasure.mapping.UserMountEntity;
import game.treasure.mapping.UserPetEntity;
import game.treasure.mapping.main.ResArtifactEntity;
import game.treasure.server.IAction;
import game.treasure.service.resource.ResArtifact;
import game.treasure.service.user.Bonus;
import io.netty.channel.Channel;
import ozudo.base.database.DBJPA;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import java.util.List;
import java.util.Map;

public class TreasureHandler extends AHandler {

    private static TreasureHandler instance;

    public static TreasureHandler getInstance() {
        if (instance == null) {
            instance = new TreasureHandler();
        }
        return instance;
    }

    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        mHandler.put(IAction.TREASURE_TRANS, this);
        mHandler.put(IAction.TREASURE_BUY, this);
    }

    @Override
    public AHandler newInstance() {
        return new TreasureHandler();
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        try {
            if (actionId == IAction.TREASURE_TRANS) {
                treasureTrans();
            } else if (actionId == IAction.TREASURE_BUY) {
                treasureBuy();
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    /** Request: [bonusType, rowId] */
    private void treasureTrans() {
        List<Long> inputs = getInputALong();
        if (inputs.size() < 2) {
            addErrParam();
            return;
        }
        int bonusType = inputs.get(0).intValue();
        long rowId = inputs.get(1);

        int priceTreasure = 0;
        if (bonusType == Bonus.BONUS_EQUIPMENT) {
            UserEquipmentEntity equip = mUser.getResources().getEquipment(rowId);
            if (equip == null) {
                addErrResponse(getLang(Lang.err_item_equip_not_found));
                return;
            }
            if (equip.getIsCraft() != 1) {
                addErrResponse(getLang(Lang.err_params));
                return;
            }
            if (equip.isEquip() || mUser.getUser().getListIdEquipmentEquip().contains((int) rowId)) {
                addErrResponse(getLang(Lang.err_params));
                return;
            }
            priceTreasure = equip.getPriceTreasure();
            if (!deleteEquipmentForTreasure(equip)) {
                addErrSystem();
                return;
            }
        } else if (bonusType == Bonus.BONUS_PET) {
            UserPetEntity pet = mUser.getResources().getPet(rowId);
            if (pet == null) {
                addErrResponse(getLang(Lang.err_item_equip_not_found));
                return;
            }
            if (pet.getIsCraft() != 1) {
                addErrResponse(getLang(Lang.err_params));
                return;
            }
            if (UserPetEntity.isEquipped(mUser, rowId)) {
                addErrResponse(getLang(Lang.err_params));
                return;
            }
            priceTreasure = pet.getPriceTreasure();
            if (!deletePetForTreasure(pet)) {
                addErrSystem();
                return;
            }
        } else if (bonusType == Bonus.BONUS_MOUNT) {
            UserMountEntity mount = mUser.getResources().getMount(rowId);
            if (mount == null) {
                addErrResponse(getLang(Lang.err_item_equip_not_found));
                return;
            }
            if (mount.getIsCraft() != 1) {
                addErrResponse(getLang(Lang.err_params));
                return;
            }
            if (UserMountEntity.isEquipped(mUser, rowId)) {
                addErrResponse(getLang(Lang.err_params));
                return;
            }
            priceTreasure = mount.getPriceTreasure();
            if (!deleteMountForTreasure(mount)) {
                addErrSystem();
                return;
            }
        } else {
            addErrParam();
            return;
        }

        List<Long> bonus = List.of();
        if (priceTreasure > 0) {
            bonus = Bonus.receiveListItem(mUser,
                    DetailActionType.TREASURE_TRANS.getKey(rowId),
                    Bonus.viewItemPoint(CfgArtifact.ARTIFACT_POINT_ID, priceTreasure));
            if (bonus.isEmpty()) {
                addErrSystem();
                return;
            }
        }
        addResponse(getCommonVector(rowId, 1L));
        if (!bonus.isEmpty()) {
            addBonusToast(bonus);
        }
    }

    /** Request: [buyIndex] — 0..3 = tier artifact 1..4 */
    private void treasureBuy() {
        List<Long> inputs = getInputALong();
        if (inputs.isEmpty()) {
            addErrParam();
            return;
        }
        int buyIndex = inputs.get(0).intValue();
        if (buyIndex < 0 || buyIndex > 3) {
            addErrParam();
            return;
        }
        int artifactTier = buyIndex + 1;
        long price = CfgCraft.getTreasureBuyPrice(buyIndex);
        if (price <= 0) {
            addErrParam();
            return;
        }
        List<Long> fee = Bonus.viewItemPoint(CfgArtifact.ARTIFACT_POINT_ID, -price);
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        int resRank = CfgCraft.rollTreasureArtifactRank();
        ResArtifactEntity res = ResArtifact.pickRandomByRank(resRank);
        if (res == null) {
            List<ResArtifactEntity> all = ResArtifact.getAll();
            if (all.isEmpty()) {
                addErrSystem();
                return;
            }
            res = all.get(ozudo.base.helper.NumberUtil.getRandom(all.size()));
        }
        List<Long> feeBonus = Bonus.receiveListItem(mUser,
                DetailActionType.TREASURE_BUY.getKey(buyIndex), fee);
        if (feeBonus.isEmpty()) {
            addErrSystem();
            return;
        }
        addBonusPrivate(feeBonus);

        List<Long> artifactBonus = Bonus.receiveListItem(mUser,
                DetailActionType.TREASURE_BUY.getKey(res.getId()),
                Bonus.viewItemArtifact(res.getId(), artifactTier));
        if (artifactBonus.isEmpty()) {
            Bonus.receiveListItem(mUser, DetailActionType.UPDATE_FAIL.getKey(),
                    Bonus.reverseBonus(feeBonus));
            addErrSystem();
            return;
        }
        addBonusToast(artifactBonus);
        addResponse(getCommonVector((long) buyIndex, 1L));
    }

    private boolean deleteEquipmentForTreasure(UserEquipmentEntity equip) {
        long id = equip.getId();
        Bonus.clearItemFromSlot(mUser, Bonus.BONUS_EQUIPMENT, id);
        if (!equip.deleteFromDb()) {
            return false;
        }
        mUser.getResources().removeEquipment(id);
        return true;
    }

    private boolean deletePetForTreasure(UserPetEntity pet) {
        long id = pet.getId();
        Bonus.clearItemFromSlot(mUser, Bonus.BONUS_PET, id);
        if (!DBJPA.delete("user_pet", "id", id, "user_id", pet.getUserId())) {
            return false;
        }
        mUser.getResources().removePet(id);
        return true;
    }

    private boolean deleteMountForTreasure(UserMountEntity mount) {
        long id = mount.getId();
        Bonus.clearItemFromSlot(mUser, Bonus.BONUS_MOUNT, id);
        if (!DBJPA.delete("user_mount", "id", id, "user_id", mount.getUserId())) {
            return false;
        }
        mUser.getResources().removeMount(id);
        return true;
    }
}
