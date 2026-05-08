package game.treasure.controller;

import game.config.CfgDaily;
import game.config.CfgQuest;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.treasure.mapping.*;
import game.treasure.mapping.main.ResTutorialQuestEntity;
import game.treasure.service.Services;
import game.object.DataDaily;
import game.object.DataQuest;
import game.treasure.mapping.main.ResQuestEntity;
import game.treasure.server.IAction;
import game.treasure.service.resource.ResQuest;
import game.treasure.service.user.Bonus;
import io.netty.channel.Channel;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.StringHelper;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class QuestHandler extends AHandler {
    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(QUEST_STATUS, QUEST_RECEIVE, BUY_GOLD_STATUS, BUY_GOLD_BUY, QUEST_REWARD_BAR
                );
        actions.forEach(action -> mHandler.put(action, this));
    }

    static QuestHandler instance;
    UserQuestEntity uQuest;

    public static QuestHandler getInstance() {
        if (instance == null) {
            instance = new QuestHandler();
        }
        return instance;
    }

    @Override
    public AHandler newInstance() {
        return new QuestHandler();
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        uQuest = mUser.getUQuest();
        uQuest.checkData(1);
        try {
            switch (actionId) {
                case IAction.QUEST_STATUS -> questStatus(QuestType.get(getInputInt()));
                case IAction.QUEST_RECEIVE -> receiveQuest();
                case IAction.QUEST_REWARD_BAR -> receiveBarQuestD();
                case IAction.BUY_GOLD_STATUS -> buyGoldStatus();
                case IAction.BUY_GOLD_BUY -> buyGoldBuy();
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    void questStatus(QuestType type) {
        // check quest done
        if (type == null || (type != QuestType.QUEST_D && type != QuestType.QUEST_C)) {
            addErrParam();
            return;
        }
        DataQuest dataQuest = uQuest.getDataQuest(type);
        List<Integer> aLong2 = new ArrayList<>();
        List<Integer> quests = uQuest.getQuest(type);
        int numDone = 0;
        boolean update = false;
        for (int i = 0; i < quests.size(); i += 2) {
            ResQuestEntity qe = ResQuest.mQuest.get(quests.get(i));
            aLong2.add(qe.getId()); // id
            aLong2.add(dataQuest.getValue(qe.getId())); //curNum
            if (quests.get(i + 1) != StatusType.DONE.value) {
                int number = qe.getNumber();
                if (type == QuestType.QUEST_C) number = qe.getNumberC();
                StatusType status = CfgQuest.getStatus(dataQuest.getValue(qe.getId()), number);
                aLong2.add(status.value);
                if (status == StatusType.RECEIVE) {
                    numDone++;
                    quests.set(i + 1, status.value);
                    update = true;
                }
            } else {
                aLong2.add(StatusType.DONE.value);
                numDone++;
            }
        }
        Pbmethod.ListCommonVector.Builder lstCm = Pbmethod.ListCommonVector.newBuilder();
        int curPoint = dataQuest.getValue(DataQuest.CUR_POINT_D);
        int timeCD = type == QuestType.QUEST_D ? (int) DateTime.getSecondsToNextDay() : (int) DateTime.getSecondsToNextWeek();
        int numQ = type == QuestType.QUEST_D ? CfgQuest.numberQuestD : CfgQuest.numberQuestC;
        lstCm.addAVector(getCommonVector(timeCD, numDone, numQ, curPoint, type.value));

        List<Integer> topStatus = mUser.getUQuest().getStatus(type);

        for (int i = 0; i < topStatus.size(); i++) {
            if (topStatus.get(i) == StatusType.PROCESSING.value) {
                int point = type == QuestType.QUEST_D ? CfgQuest.config.pointState.get(i) : CfgQuest.config.pointStateC.get(i);
                if (curPoint >= point) {
                    update = true;
                    topStatus.set(i, StatusType.RECEIVE.value);
                }
            }
        }
        if (update) mUser.getUQuest().updateStatus(type, StringHelper.toDBString(topStatus));
        lstCm.addAVector(getCommonIntVector(topStatus));
        lstCm.addAVector(getCommonIntVector(aLong2));
        addResponse(IAction.QUEST_STATUS, lstCm.build());
    }

    void receiveQuest() {
        List<Long> inputs = getInputALong();
        QuestType type = QuestType.get(getInputInt());
        if (type == null || (type != QuestType.QUEST_D && type != QuestType.QUEST_C)) {
            addErrParam();
            return;
        }
        int id = inputs.get(1).intValue();
        DataQuest dataQuest = mUser.getUQuest().getDataQuest(type);
        List<Integer> quests = mUser.getUQuest().getQuest(type);
        for (int i = 0; i < quests.size(); i += 2) {
            if (quests.get(i) == id) {
                ResQuestEntity curQ = ResQuest.mQuest.get(id);
                StatusType status = StatusType.get(quests.get(i + 1));
                if (curQ == null) {
                    addErrResponse(getLang(Lang.err_params));
                    return;
                }
                if (status == StatusType.DONE) {
                    addErrResponse(getLang(Lang.err_received_bonus));
                    return;
                }
                // check data
                StatusType readStatus = CfgQuest.getStatus(dataQuest.getValue(quests.get(i)), type == QuestType.QUEST_D ? curQ.getNumber() : curQ.getNumberC());
                if (readStatus != StatusType.RECEIVE) {
                    if (readStatus != StatusType.RECEIVE) {
                        addErrResponse(getLang(Lang.err_quest_done));
                        return;
                    }
                }
                dataQuest.addValue(DataQuest.CUR_POINT_D, type == QuestType.QUEST_D ? curQ.getBonus() : curQ.getBonusC());
                quests.set(i + 1, StatusType.DONE.value);
                uQuest.update(new ArrayList<>());
                if (mUser.getUQuest().receiveQuestBonus(type, StringHelper.toDBString(quests))) {
                    questStatus(type);
                    // todo update other
                    if (type == QuestType.QUEST_D) {
                        ClanEntity clan = Services.clanDAO.getClan(mUser.getUser().getClan());
//                        if (clan != null) {
//                            clan.checkDynamic(mUser, CfgClan.DYNAMIC_QUEST_D_100, dataQuest.getValue(DataQuest.CUR_POINT_D));
//                        }
                    }
                    ResTutorialQuestEntity res = ResQuest.mTutQuest.get(mUser.getUData().getQuestTutorial());
                    if (res != null && res.getType() == QuestTutType.HAS_POINT_D) {
                        UserHandler.tutorialQuestStatus(mUser, this);
                    }
                    return;
                } else {
                    addErrResponse();
                    return;
                }
            }
        }
        addErrResponse();
    }

    void receiveBarQuestD() {
        List<Long> inputs = getInputALong();
        QuestType type = QuestType.get(getInputInt());
        if (type == null || (type != QuestType.QUEST_D && type != QuestType.QUEST_C)) {
            addErrParam();
            return;
        }
        int index = inputs.get(1).intValue();
        // check index
        if (!CfgQuest.checkIndex(index)) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        // check status
        List<Integer> status = mUser.getUQuest().getStatus(type);
        if (status.get(index) == StatusType.DONE.value) {
            addErrResponse(getLang(Lang.err_received_bonus));
            return;
        }
        // CHECK DONE
        if (status.get(index) == StatusType.PROCESSING.value) {
            addErrResponse(getLang(Lang.err_quest_done));
            return;
        }
        List<Long> bonus = Bonus.receiveListItem(mUser, DetailActionType.NHIEM_VU_HANG_NGAY_2.getKey(index), type == QuestType.QUEST_D ? CfgQuest.getQuestBonus(index) : CfgQuest.getQuestCBonus(index));
        if (bonus.isEmpty()) {
            addErrResponse();
            return;
        }
        status.set(index, StatusType.DONE.value);
        if (mUser.getUQuest().updateStatus(type, StringHelper.toDBString(status))) {
            Pbmethod.ListCommonVector.Builder lst = Pbmethod.ListCommonVector.newBuilder();
            lst.addAVector(getCommonVector(bonus));
            lst.addAVector(getCommonIntVector(mUser.getUQuest().getStatus(type)));
            addResponse(lst.build());
        } else addErrResponse();
    }


    boolean checkItemQuestB(UserItemEntity itemQuestB) {
        if (itemQuestB == null) {
            addErrResponse(getLang(Lang.item_not_own));
            return false;
        }
        if (itemQuestB.getType() != ItemType.QUEST_B) {
            addErrParam();
            return false;
        }
        if (itemQuestB.expired()) {
            addErrResponse(getLang(Lang.err_item_expire));
            return false;
        }
        return true;
    }


    void buyGoldStatus() {
        Pbmethod.CommonVector.Builder builder = Pbmethod.CommonVector.newBuilder();
        builder.addALong(DateTime.getSecondsToNextDay());
        DataDaily uDaily = mUser.getUserDaily().getUDaily();
        builder.addALong(uDaily.getValue(DataDaily.BUY_GOLD_0));
        builder.addALong(uDaily.getValue(DataDaily.BUY_GOLD_1));
        builder.addALong(uDaily.getValue(DataDaily.BUY_GOLD_2));
        addResponse(IAction.BUY_GOLD_STATUS, builder.build());
    }

    void buyGoldBuy() {
        int slot = getInputInt();
        if (!CfgDaily.checkSlotGold(slot)) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        DataDaily uDaily = mUser.getUserDaily().getUDaily();
        if (!CfgDaily.checkHasBuyGold(uDaily, slot)) {
            addErrResponse(getLang(Lang.err_sold_out));
            return;
        }
        List<Long> bonus = Bonus.viewGem(-CfgDaily.config.gemFee.get(slot));
        String err = Bonus.checkMoney(mUser, bonus);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        bonus.addAll(Bonus.viewGold(CfgDaily.config.goldBuy.get(slot)));
        bonus = Bonus.receiveListItem(mUser, DetailActionType.BUY_GOLD_SLOT.getKey(slot), bonus);
        if (!bonus.isEmpty()) {
            if (slot == 0) uDaily.addValue(DataDaily.BUY_GOLD_0, 1);
            if (slot == 1) uDaily.addValue(DataDaily.BUY_GOLD_1, 1);
            if (slot == 2) uDaily.addValue(DataDaily.BUY_GOLD_2, 1);
            CfgQuest.addNumQuest(mUser, DataQuest.CHANGE_GOLD, 1);
            uDaily.update();
            addBonusToastPlus(bonus);
            buyGoldStatus();
            mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.BUY_GOLD, 1);
        } else addErrResponse();
    }

}
