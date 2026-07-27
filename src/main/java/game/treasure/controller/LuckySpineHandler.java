package game.treasure.controller;

import game.config.CfgFeature;
import game.config.CfgLuckySpine;
import game.config.CfgQuest;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.treasure.server.IAction;
import game.treasure.service.user.Bonus;
import game.object.DataQuest;
import io.netty.channel.Channel;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class LuckySpineHandler extends AHandler {

    @Override
    public AHandler newInstance() {
        return new LuckySpineHandler();
    }

    static LuckySpineHandler instance;

    final int INDEX_FEE_CASINO_1_TIMES = 0;
    final int INDEX_FEE_CASINO_10_TIMES = 1;

    public static LuckySpineHandler getInstance() {
        if (instance == null) {
            instance = new LuckySpineHandler();
        }
        return instance;
    }

    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(LUCKY_SPINE_STATUS, LUCKY_SPINE_ROTATE, LUCKY_SPINE_BUY_CHIP);
        actions.forEach(action -> mHandler.put(action, this));
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        checkTimeMonitor("s");
        if (!CfgFeature.isOpenFeature(FeatureType.SPIN, mUser, this)) {
            return;
        }
        try {
            switch (actionId) {
                case IAction.LUCKY_SPINE_STATUS -> nStatus();
                case IAction.LUCKY_SPINE_ROTATE -> nRotate();
                case IAction.LUCKY_SPINE_BUY_CHIP -> nBuyChip();
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    void nStatus() {
        Pbmethod.ListCommonVector.Builder builder = Pbmethod.ListCommonVector.newBuilder();
        builder.addAVector(Pbmethod.CommonVector.newBuilder()
                .addAString("1 " + getLang(Lang.label_spin))
                .addAString("10 " + getLang(Lang.label_spin)));
        addResponse(IAction.LUCKY_SPINE_STATUS, builder.build());
    }

    void nRotate() {
        int numberRotate = getInputInt();
        if (numberRotate != 1 && numberRotate != 10) {
            addErrParam();
            return;
        }
        int feeRotate;
        if (numberRotate == 1) {
            feeRotate = CfgLuckySpine.config.feeRotate[INDEX_FEE_CASINO_1_TIMES];
        } else {
            feeRotate = CfgLuckySpine.config.feeRotate[INDEX_FEE_CASINO_10_TIMES];
        }

        if (mUser.getResources().getItemPointNumber(ItemPointKey.CHIP.id) < feeRotate) {
            addErrResponse(getLang(Lang.not_enough_chip));
            return;
        }

        List<Long> retBonus = Bonus.viewItemPoint(ItemPointKey.CHIP.id, -feeRotate);
        int max = 0;
        for (int i = 0; i < numberRotate; i++) {
            int indexResult = CfgLuckySpine.getRandomIndex();
            List<Long> slotBonus = CfgLuckySpine.rollBonusBySlot(indexResult);
            if (slotBonus.isEmpty()) {
                addErrSystem();
                return;
            }
            retBonus.addAll(slotBonus);
            if (max < indexResult) max = indexResult;
        }

        retBonus.addAll(Bonus.viewItemMaterial(MaterialType.LUCKY_COIN, numberRotate));
        retBonus = Bonus.receiveListItem(mUser, DetailActionType.ROTATE_SPINE_NORMAL.getKey(), retBonus);
        if (retBonus.isEmpty()) {
            addErrResponse();
            return;
        }
        retBonus.add(0, (long) max);
        addResponse(getCommonVector(retBonus));

        mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.USE_SPINE_ROTATE, numberRotate);
        CfgQuest.addNumQuest(mUser, DataQuest.SPINE, numberRotate);
        CfgQuest.addNumQuestB(mUser, CfgQuest.INDEX_SPINE, numberRotate);
    }

    void nBuyChip() {
        int numberChip = getInputInt();
        if (numberChip <= 0) {
            addErrParam();
            return;
        }
        if (mUser.getUser().getGem() < (numberChip * CfgLuckySpine.config.priceChip)) {
            addErrResponse(getLang(Lang.err_not_enough_gem));
            return;
        }

        List<Long> bonus = new ArrayList<>();
        bonus.addAll(Bonus.viewItemPoint(ItemPointKey.CHIP.id, numberChip));
        bonus.addAll(Bonus.viewGem(-(numberChip * CfgLuckySpine.config.priceChip)));

        List<Long> retBonus = Bonus.receiveListItem(mUser, DetailActionType.MUA_CHIP_VONG_QUAY.getKey(), bonus);
        if (retBonus.isEmpty()) {
            addErrResponse();
            return;
        }
        addBonusToastPlus(retBonus);
    }
}
