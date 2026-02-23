package game.dragonhero.controller;

import com.google.gson.JsonArray;
import game.config.CfgFeature;
import game.config.CfgLuckySpine;
import game.config.CfgQuest;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.dragonhero.mapping.UserInt;
import game.dragonhero.mapping.UserItemEntity;
import game.dragonhero.mapping.UserLuckySpineEntity;
import game.dragonhero.server.IAction;
import game.dragonhero.service.Services;
import game.dragonhero.service.user.Bonus;
import game.object.DataQuest;
import game.protocol.CommonProto;
import io.netty.channel.Channel;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.StringHelper;
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
    UserLuckySpineEntity userSpine;

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
        List<Integer> actions = Arrays.asList(LUCKY_SPINE_STATUS, LUCKY_SPINE_REFRESH, LUCKY_SPINE_ROTATE, LUCKY_SPINE_BUY_CHIP);
        actions.forEach(action -> mHandler.put(action, this));
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        checkTimeMonitor("s");
        userSpine = Services.userDAO.getUserSpine(mUser);
        if (userSpine == null) {
            addErrSystem();
            return;
        }
        userSpine.checkRefreshNormal(mUser.getUser());
        if (!CfgFeature.isOpenFeature(FeatureType.SPIN, mUser, this)) {
            return;
        }
        try {
            switch (actionId) {
                case IAction.LUCKY_SPINE_STATUS -> nStatus();
                case IAction.LUCKY_SPINE_REFRESH -> nRefresh();
                case IAction.LUCKY_SPINE_ROTATE -> nRotate();
                case IAction.LUCKY_SPINE_BUY_CHIP -> nBuyChip();

            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    void nStatus() {
        Pbmethod.ListCommonVector.Builder builder = Pbmethod.ListCommonVector.newBuilder();
        int freeRefresh = userSpine.checkFreeRefresh() ? 1 : 0;
        builder.addAVector(Pbmethod.CommonVector.newBuilder().addAString("1 " + getLang(Lang.label_spin)).addAString("10 " + getLang(Lang.label_spin)).addALong(userSpine.casinoCountdown()).addALong(freeRefresh));
        builder.addAVector(getCommonVector(parseListBonusCasino(userSpine.getBonusNormal())));
        builder.addAVector(getCommonIntVector(userSpine.getStatusNormal()));
        addResponse(IAction.LUCKY_SPINE_STATUS, builder.build());
    }

    List<Long> parseListBonusCasino(String listBonus) {
        JsonArray arrBonus = GsonUtil.parseJsonArray(listBonus);
        List<Long> lstLong = new ArrayList<>();
        for (int i = 0; i < arrBonus.size(); i++) {
            for (int j = 0; j < arrBonus.get(i).getAsJsonArray().size(); j++) {
                lstLong.add(arrBonus.get(i).getAsJsonArray().get(j).getAsLong());
            }
        }
        return lstLong;
    }

    void nRefresh() {
        int feeRefresh = userSpine.checkFreeRefresh() ? 0 : CfgLuckySpine.config.feeRefresh;
        if (mUser.getUser().getGem() < feeRefresh) {
            addErrResponse(getLang(Lang.err_not_enough_gem));
            return;
        }
        List<Long> bonus = new ArrayList<>();
        if (feeRefresh > 0) {
            bonus = Bonus.viewGem(-feeRefresh);
            String err = Bonus.checkMoney(mUser, bonus);
            if (err != null) {
                addErrResponse(err);
                return;
            }
            bonus = Bonus.receiveListItem(mUser, DetailActionType.RESET_SPINE_NORMAL.getKey(), bonus);
            if (bonus.isEmpty()) {
                addErrSystem();
                return;
            }
        }

        if (userSpine.updateResetNormal(user, feeRefresh == 0)) {
            addResponse(getCommonVector(bonus));
            nStatus();
        } else {
            Bonus.receiveListItem(mUser, DetailActionType.RESET_SPINE_NORMAL.getKey(-1), bonus);
        }
    }

    void nRotate() {
        int numberRotate = getInputInt();
        if (numberRotate != 1 && numberRotate != 10) {
            addErrParam();
            return;
        }
        int feeRotate = 0;
        if (numberRotate == 1) {
            feeRotate = CfgLuckySpine.config.feeRotate[INDEX_FEE_CASINO_1_TIMES];
        } else if (numberRotate == 10) {
            feeRotate = CfgLuckySpine.config.feeRotate[INDEX_FEE_CASINO_10_TIMES];
        }

        UserItemEntity uItem = mUser.getResources().getItem(MaterialType.CHIP.id);
        if (uItem == null || uItem.getNumber() < feeRotate) {
            addErrResponse(getLang(Lang.not_enough_chip));
            return;
        }
        List<Long> retBonus = Bonus.viewItemMaterial(MaterialType.CHIP, -feeRotate);

        int max = 0;
        List<List<Long>> allBonus = GsonUtil.strTo2ListLong(userSpine.getBonusNormal());
        //
        boolean update = false;
        List<Integer> lstEnable = userSpine.getStatusNormal();
        for (int i = 0; i < numberRotate; i++) {
            int indexResult = CfgLuckySpine.getRandomIndex();
            //
            while (lstEnable.get(indexResult) != StatusType.PROCESSING.value) {
                indexResult = CfgLuckySpine.getRandomIndex();
            }

            retBonus.addAll(allBonus.get(indexResult));
            if (max < indexResult) max = indexResult;
            if (indexResult >= CfgLuckySpine.indexSelectOne) {
                lstEnable.set(indexResult, StatusType.DONE.value);
                update = true;
            }

        }
        //bonus lucky coin
        retBonus.addAll(Bonus.viewItemMaterial(MaterialType.LUCKY_COIN, numberRotate));
        retBonus = Bonus.receiveListItem(mUser, DetailActionType.ROTATE_SPINE_NORMAL.getKey(), retBonus);
        if (retBonus.isEmpty()) {
            addErrResponse();
            return;
        }
        retBonus.add(0, (long) max);
        if (update) {
            if (userSpine.updateStatusNormal(StringHelper.toDBString(lstEnable))) {
                addResponse(getCommonVector(retBonus));

            } else {
                Bonus.receiveListItem(mUser, DetailActionType.ROTATE_SPINE_NORMAL.getKey(-1), Bonus.viewItemMaterial(MaterialType.CHIP, feeRotate));
                addErrResponse();
                return;
            }
        } else {
            addResponse(getCommonVector(retBonus));
        }
        mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.USE_SPINE_ROTATE, numberRotate);
        CfgQuest.addNumQuest(mUser, DataQuest.SPINE, numberRotate);
        // check quest B
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
        bonus.addAll(Bonus.viewItem(MaterialType.CHIP.id, numberChip));
        bonus.addAll(Bonus.viewGem(-(numberChip * CfgLuckySpine.config.priceChip)));
        //
        List<Long> retBonus = Bonus.receiveListItem(mUser, DetailActionType.MUA_CHIP_VONG_QUAY.getKey(), bonus);
        if (retBonus.isEmpty()) {
            addErrResponse();
            return;
        }
        addBonusToastPlus(retBonus);
    }
}
