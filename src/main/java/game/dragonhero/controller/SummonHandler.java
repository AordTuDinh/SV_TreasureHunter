package game.dragonhero.controller;

import game.config.CfgFeature;
import game.config.CfgGacha;
import game.config.CfgQuest;
import game.config.aEnum.DetailActionType;
import game.config.aEnum.FeatureType;
import game.config.aEnum.ItemKey;
import game.config.aEnum.QuestTutType;
import game.config.lang.Lang;
import game.dragonhero.mapping.UserEventSevenDayEntity;
import game.dragonhero.mapping.UserInt;
import game.dragonhero.mapping.UserSummonEntity;
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

import java.util.*;

public class SummonHandler extends AHandler {
    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(SUMMON_STATUS, SUMMON_STONE, SUMMON_REWARD_STONE, SUMMON_PIECE, SUMMON_REWARD_PIECE, SUMMON_STONE_ADS, SUMMON_PIECE_ADS);
        actions.forEach(action -> mHandler.put(action, this));
    }

    static SummonHandler instance;
    UserSummonEntity uSummon;

    public static SummonHandler getInstance() {
        if (instance == null) {
            instance = new SummonHandler();
        }
        return instance;
    }

    @Override
    public AHandler newInstance() {
        return new SummonHandler();
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        checkTimeMonitor("s");
        uSummon = Services.userDAO.getUserSummon(mUser);
        if (!CfgFeature.isOpenFeature(FeatureType.SUMMON_WEAPON, mUser, this)) {
            return;
        }
        try {
            switch (actionId) {
                case IAction.SUMMON_STATUS:
                    status();
                    break;
                case IAction.SUMMON_STONE:
                    summonStone();
                    break;
                case IAction.SUMMON_REWARD_STONE:
                    rewardStone();
                    break;
                case IAction.SUMMON_PIECE:
                    summonPiece();
                    break;
                case IAction.SUMMON_REWARD_PIECE:
                    rewardPiece();
                    break;
                case IAction.SUMMON_STONE_ADS:
                    summonStoneADS();
                    break;
                case IAction.SUMMON_PIECE_ADS:
                    summonPieceADS();
                    break;
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    private void status() {
        Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
        Pbmethod.CommonVector.Builder cmm = Pbmethod.CommonVector.newBuilder();
        // stone
        cmm.addALong(uSummon.getCDSummonFree());
        cmm.addAllALong(CfgGacha.config.prices);
        int levelSummon = uSummon.getLevelStone();
        levelSummon = Math.min(levelSummon, CfgGacha.summonDataStone.size());
        CfgGacha.SummonLevel dataSummon = CfgGacha.summonDataStone.get(levelSummon);
        cmm.addALong(levelSummon);
        cmm.addALong(Math.min(uSummon.getCountSummonStone(), dataSummon.number));
        cmm.addALong(dataSummon.number);
        cmm.addALong(uSummon.hasBonusStone());
        // bonus reward stone
        cmm.addAllALong(dataSummon.bonus);
        pb.addAVector(cmm);
        // piece
        Pbmethod.CommonVector.Builder cmm2 = Pbmethod.CommonVector.newBuilder();
        int  levelSummonPiece = Math.min(uSummon.getLevelPiece(), CfgGacha.summonDataPiece.size());
        dataSummon = CfgGacha.summonDataPiece.get(levelSummonPiece);
        cmm2.addALong(levelSummonPiece);
        cmm2.addALong(Math.min(uSummon.getCountSummonPiece(), dataSummon.number));
        cmm2.addALong(dataSummon.number);
        cmm2.addALong(uSummon.hasBonusPiece());
        // bonus reward stone
        cmm2.addAllALong(dataSummon.bonus);
        pb.addAVector(cmm2);
        Pbmethod.CommonVector.Builder cm3 = Pbmethod.CommonVector.newBuilder();
        cm3.addALong(uSummon.getCDSummonStoneAds());
        cm3.addALong(uSummon.getCDSummonPieceAds());
        pb.addAVector(cm3);
        addResponse(pb.build());
    }

    private void summonStone() {
        List<Long> input = getInputALong();
        int numSum = input.get(0).intValue();
        boolean isFree = numSum == -1;
        int type = isFree ? 0 : input.get(1).intValue();
        if (type != 0 && type != 1) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        if (numSum != 1 && numSum != 10 && numSum != -1) { // check input
            addErrResponse(getLang(Lang.err_number_input));
            return;
        }
        if (numSum == -1 && uSummon.getCDSummonFree() > 0) {
            addErrResponse(getLang(Lang.err_summon_free));
            return;
        }
        List<Long> aBonus = new ArrayList<>();
        if (numSum != -1) { // fee summon
            aBonus = CfgGacha.getFeeSummonStone(type, numSum);
            String error = Bonus.checkMoney(mUser, aBonus);
            if (!StringHelper.isEmpty(error)) {
                addErrResponse(error);
                return;
            }
        }
        // check first x10
        if (numSum == 10 && mUser.getUData().getUInt().getValue(UserInt.FIRST_SUMMON_STONE_X10) == 0) {
            if (mUser.getUData().getUInt().setValueAndUpdate(UserInt.FIRST_SUMMON_STONE_X10, 1)) {
                aBonus.addAll(CfgGacha.firstSummonStoneX10);
            } else {
                addErrSystem();
                return;
            }
        } else {
            numSum = numSum == -1 ? 1 : numSum;
            for (int i = 0; i < numSum; i++) {
                aBonus.addAll(CfgGacha.bonusStone(randomRankStone()));
            }
        }
        List<Long> retBonus = Bonus.receiveListItem(mUser, DetailActionType.SUMMON_STONE.getKey(numSum), aBonus);
        if (retBonus.isEmpty()) {
            addErrResponse();
            return;
        }
        // dem so luong lan summon va check bonus summon
        if (uSummon.addCountSumStone(numSum, isFree, false)) {
            // update chi so nua
            Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
            pb.addAVector(CommonProto.getCommonVectorProto(retBonus, null));
            Pbmethod.CommonVector.Builder cmm = Pbmethod.CommonVector.newBuilder();
            cmm.addALong(uSummon.getLevelStone());
            cmm.addALong(uSummon.getCountSummonStone());
            CfgGacha.SummonLevel dataSummon = CfgGacha.summonDataStone.get(uSummon.getLevelStone());
            cmm.addALong(dataSummon.number);
            cmm.addALong(uSummon.hasBonusStone());
            cmm.addALong(uSummon.getCDSummonFree());
            pb.addAVector(cmm);
            addResponse(pb.build());
            CfgQuest.addNumQuest(mUser, DataQuest.SUMMON_STONE, numSum);
            CfgQuest.addNumQuestB(mUser, CfgQuest.INDEX_SUMMON_STONE, numSum);
            // check tut
            mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.SUMMON_STONE, numSum);
            // check event 7 day
            UserEventSevenDayEntity uEvent = Services.userDAO.getUserSevenDay(mUser);
            if (uEvent.hasEvent() && uEvent.hasActive(2) && uEvent.update(List.of("summon_stone", uEvent.getSummonStone() + numSum))) {
                uEvent.setSummonStone(uEvent.getSummonStone() + numSum);
            }
        } else addErrResponse();
    }


    private void summonStoneADS() {
        int numSum = 10;
        if (uSummon.getCDSummonStoneAds() > 0) {
            addErrParam();
            return;
        }
        List<Long> aBonus = CfgGacha.bonusStone(randomRankStone());
        List<Long> retBonus = Bonus.receiveListItem(mUser, DetailActionType.SUMMON_STONE_ADS.getKey(), aBonus);
        if (retBonus.isEmpty()) {
            addErrResponse();
            return;
        }
        // dem so luong lan summon va check bonus summon
        if (uSummon.addCountSumStone(numSum, false, true)) {
            // update chi so nua
            Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
            pb.addAVector(getCommonVector(retBonus));
            Pbmethod.CommonVector.Builder cmm = Pbmethod.CommonVector.newBuilder();
            cmm.addALong(uSummon.getLevelStone());
            cmm.addALong(uSummon.getCountSummonStone());
            CfgGacha.SummonLevel dataSummon = CfgGacha.summonDataStone.get(uSummon.getLevelStone());
            cmm.addALong(dataSummon.number);
            cmm.addALong(uSummon.hasBonusStone());
            cmm.addALong(uSummon.getCDSummonStoneAds());
            pb.addAVector(cmm);
            addResponse(pb.build());
            CfgQuest.addNumQuest(mUser, DataQuest.SUMMON_STONE, numSum);
            CfgQuest.addNumQuestB(mUser, CfgQuest.INDEX_SUMMON_STONE, numSum);
            // check tut
            mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.SUMMON_STONE, numSum);
            // check event 7 day
            UserEventSevenDayEntity uEvent = Services.userDAO.getUserSevenDay(mUser);
            if (uEvent.hasEvent() && uEvent.hasActive(2) && uEvent.update(List.of("summon_stone", uEvent.getSummonStone() + numSum))) {
                uEvent.setSummonStone(uEvent.getSummonStone() + numSum);
            }
        } else addErrResponse();
    }

    private void summonPiece() {
        int numSum = getInputInt();
        if (numSum != 1 && numSum != 10) { // check input
            addErrResponse(getLang(Lang.err_number_input));
            return;
        }
        List<Long> aBonus = Bonus.viewItem(ItemKey.SCROLL_SUMMON_SPECIAL, -numSum);
        String error = Bonus.checkMoney(mUser, aBonus);
        if (!StringHelper.isEmpty(error)) {
            addErrResponse(error);
            return;
        }
        for (int i = 0; i < numSum; i++) {
            aBonus.addAll(CfgGacha.bonusPiece(randomRankPiece()));
        }
        List<Long> retBonus = Bonus.receiveListItem(mUser, DetailActionType.SUMMON_PIECE.getKey(numSum), aBonus);
        if (retBonus.isEmpty()) {
            addErrResponse();
            return;
        }
        // dem so luong lan summon va check bonus summon
        if (uSummon.addCountSumPiece(numSum, false)) {
            // update chi so nua
            Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
            pb.addAVector(CommonProto.getCommonVectorProto(retBonus));
            Pbmethod.CommonVector.Builder cmm = Pbmethod.CommonVector.newBuilder();
            cmm.addALong(uSummon.getLevelPiece());
            cmm.addALong(uSummon.getCountSummonPiece());
            int levelSummon = uSummon.getLevelPiece()>CfgGacha.summonDataPiece.size()?CfgGacha.summonDataPiece.size()-1:uSummon.getLevelPiece();
            CfgGacha.SummonLevel dataSummon = CfgGacha.summonDataPiece.get(levelSummon);
            cmm.addALong(dataSummon.number);
            cmm.addALong(uSummon.hasBonusPiece());
            cmm.addALong(uSummon.getCDSummonPieceAds());
            pb.addAVector(cmm);
            addResponse(pb.build());
            mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.SUMMON_PIECE, numSum);
            // check quest B
            CfgQuest.addNumQuest(mUser, DataQuest.SUMMON_PIECE, numSum);
            CfgQuest.addNumQuestB(mUser, CfgQuest.INDEX_SUMMON_PIECE, numSum);
            // event 7 day attack boss day 2
            UserEventSevenDayEntity uEvent = Services.userDAO.getUserSevenDay(mUser);
            if (uEvent.hasEvent() && uEvent.hasActive(3) && uEvent.update(List.of("summon_piece", uEvent.getSummonPiece() + numSum))) {
                uEvent.setSummonPiece(uEvent.getSummonPiece() + numSum);
            }
        } else addErrResponse();
    }

    private void summonPieceADS() {
        int numSum =3;
        List<Long> aBonus = CfgGacha.bonusPiece(randomRankPiece());
        List<Long> retBonus = Bonus.receiveListItem(mUser, DetailActionType.SUMMON_PIECE_ADS.getKey(), aBonus);
        if (retBonus.isEmpty()) {
            addErrResponse();
            return;
        }
        // dem so luong lan summon va check bonus summon
        if (uSummon.addCountSumPiece(numSum, true)) {
            // update chi so nua
            Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
            pb.addAVector(CommonProto.getCommonVectorProto(retBonus, null));
            Pbmethod.CommonVector.Builder cmm = Pbmethod.CommonVector.newBuilder();
            cmm.addALong(uSummon.getLevelPiece());
            cmm.addALong(uSummon.getCountSummonPiece());
            CfgGacha.SummonLevel dataSummon = CfgGacha.summonDataPiece.get(uSummon.getLevelPiece());
            cmm.addALong(dataSummon.number);
            cmm.addALong(uSummon.hasBonusPiece());
            cmm.addALong(uSummon.getCDSummonPieceAds());
            pb.addAVector(cmm);
            addResponse(pb.build());
            mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.SUMMON_PIECE, numSum);
            // check quest B
            CfgQuest.addNumQuest(mUser, DataQuest.SUMMON_PIECE, numSum);
            CfgQuest.addNumQuestB(mUser, CfgQuest.INDEX_SUMMON_PIECE, numSum);
            // event 7 day attack boss day 2
            UserEventSevenDayEntity uEvent = Services.userDAO.getUserSevenDay(mUser);
            if (uEvent.hasEvent() && uEvent.hasActive(3) && uEvent.update(List.of("summon_piece", uEvent.getSummonPiece() + numSum))) {
                uEvent.setSummonPiece(uEvent.getSummonPiece() + numSum);
            }
        } else addErrResponse();
    }

    private int randomRankStone() {
        int rand = new Random().nextInt(1000); // lay den 999 thoi
        int levelSummon = Math.min(uSummon.getLevelStone(), CfgGacha.summonDataStone.size());
        CfgGacha.SummonLevel dataSummon = CfgGacha.summonDataStone.get(levelSummon);
        for (int i = 0; i < dataSummon.summonRate.size(); i++) {
            if (rand <= dataSummon.summonRate.get(i)) {
                return i + 1;
            }
        }
        return 1;
    }

    private int randomRankPiece() {
        int rand = new Random().nextInt(1000); // lay den 999 thoi
        int levelSummon = Math.min(uSummon.getLevelPiece(), CfgGacha.summonDataPiece.size());
        CfgGacha.SummonLevel dataSummon = CfgGacha.summonDataPiece.get(levelSummon);
        for (int i = 0; i < dataSummon.summonRate.size(); i++) {
            if (rand <= dataSummon.summonRate.get(i)) {
                return i + 2;
            }
        }
        return 1;
    }

    private void rewardStone() {
        List<Long> aBonus = GsonUtil.strToListLong(uSummon.getBonusStone());
        List<Long> retBonus = Bonus.receiveListItem(mUser, DetailActionType.SUMMON_STONE_TICH_LUY.getKey(), Bonus.merge(aBonus));
        if (retBonus.isEmpty()) {
            addErrResponse();
            return;
        }
        // xoa bonus va cong lv nhan bonus
        if (uSummon.update(Arrays.asList("bonus_stone", "[]"))) {
            uSummon.setBonusStone("[]");
        }
        addResponse(getCommonVector(retBonus));
    }

    private void rewardPiece() {
        List<Long> aBonus = GsonUtil.strToListLong(uSummon.getBonusPiece());
        List<Long> retBonus = Bonus.receiveListItem(mUser, DetailActionType.SUMMON_PIECE_TICH_LUY.getKey(), Bonus.merge(aBonus));
        if (retBonus.isEmpty()) {
            addErrResponse();
            return;
        }
        // xoa bonus va cong lv nhan bonus
        if (uSummon.update(Arrays.asList("bonus_piece", "[]"))) {
            uSummon.setBonusPiece("[]");
        }
        addResponse(getCommonVector(retBonus));
    }
}
