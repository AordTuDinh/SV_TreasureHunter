package game.treasure.controller;

import game.config.CfgMaterial;
import game.config.aEnum.DetailActionType;
import game.config.lang.Lang;
import game.treasure.mapping.UserMaterialEntity;
import game.treasure.mapping.main.ResMaterialEntity;
import game.treasure.server.IAction;
import game.treasure.service.user.Bonus;
import io.netty.channel.Channel;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

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
        List<Long> fee = CfgMaterial.getUpgradeFee(tier, gem.getRank(), gem.getLevel());
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
        float newValue = gem.getValue() + CfgMaterial.rollValue(res, gem.getRank());
        if (gem.update(Arrays.asList("level", newLevel, "value", newValue))) {
            gem.setLevel(newLevel);
            gem.setValue(newValue);
            addBonusToast(aBonus);
            addResponse(gem.toProto().build());
        } else {
            Bonus.receiveListItem(mUser, DetailActionType.UPDATE_FAIL.getKey(), Bonus.reverseBonus(fee));
            addErrSystem();
        }
    }
}
