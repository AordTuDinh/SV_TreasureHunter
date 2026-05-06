package game.treasure.controller;

import game.config.CfgPet;
import game.config.CfgQuest;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.treasure.mapping.UserEventSevenDayEntity;
import game.treasure.mapping.UserInt;
import game.treasure.mapping.UserPetEntity;
import game.treasure.mapping.main.ResPetEntity;
import game.treasure.server.IAction;
import game.treasure.service.Services;
import game.treasure.service.resource.ResPet;
import game.treasure.service.user.Actions;
import game.treasure.service.user.Bonus;
import io.netty.channel.Channel;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import java.util.*;

public class PetHandler extends AHandler {
    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList( PET_SUMMON, PET_INFO);
        actions.forEach(action -> mHandler.put(action, this));
    }

    static PetHandler instance;

    public static PetHandler getInstance() {
        if (instance == null) {
            instance = new PetHandler();
        }
        return instance;
    }

    @Override
    public AHandler newInstance() {
        return new PetHandler();
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        try {
            switch (actionId) {
                case PET_SUMMON -> petSummon();
                case PET_INFO -> petInfo();
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }


    private void petInfo() {
        List<Long> ids = getInputALong();
        if (ids.isEmpty() || ids.size() < 2) {
            addErrParam();
            return;
        }
        Pbmethod.PbListPet.Builder pbPets = Pbmethod.PbListPet.newBuilder();
        for (int i = 0; i < ids.size(); i += 2) {
            UserPetEntity uPet = mUser.getResources().getMPetAnimal().get(Math.toIntExact(ids.get(i + 1)));
            if (uPet != null) pbPets.addPets(uPet.toProto());
        }
        addResponse(pbPets.build());
    }


    private void petSummon() {
        List<Long> inputs = getInputALong();
        int number = inputs.get(0).intValue();
        int idSummon = inputs.get(2).intValue();
        ResPetEntity rPet = ResPet.getPet(idSummon);
        if (number != 1 && number != 10) {
            addErrParam();
            return;
        }
        if (rPet != null && rPet.getShowSummon() == 0) {
            addErrParam();
            return;
        }
        boolean isVip = inputs.get(1) == 1L;
        List<Long> bonus = Bonus.viewItem(isVip ? ItemKey.BONG_SIEU_THU : ItemKey.BONG_LINH_THU, -number);
        String err = Bonus.checkMoney(mUser, bonus);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        bonus.addAll(CfgPet.summonPet(mUser, number, isVip, idSummon));
        addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.SUMMON_PET.getKey(number), Bonus.merge(bonus))));
        // check event 7 day
        UserEventSevenDayEntity uEvent = Services.userDAO.getUserSevenDay(mUser);
        if (uEvent.hasEvent() && uEvent.hasActive(4) && uEvent.update(List.of("summon_pet", uEvent.getSummonPet() + number))) {
            uEvent.setSummonPet(uEvent.getSummonPet() + number);
        }
        // check quest B
        CfgQuest.addNumQuestB(mUser, CfgQuest.INDEX_SUMMON_PET, number);
        // tut
        mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.SUMMON_PET, number);
        mUser.getUData().checkStatusTut(mUser, QuestTutType.HAS_PET, idSummon, this);

    }
}
