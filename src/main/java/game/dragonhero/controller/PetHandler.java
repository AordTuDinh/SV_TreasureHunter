package game.dragonhero.controller;

import game.battle.model.Pet;
import game.battle.model.Player;
import game.battle.type.StateType;
import game.config.CfgPet;
import game.config.CfgQuest;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.dragonhero.mapping.UserEventSevenDayEntity;
import game.dragonhero.mapping.UserInt;
import game.dragonhero.mapping.UserItemFarmEntity;
import game.dragonhero.mapping.UserPetEntity;
import game.dragonhero.mapping.main.ResEnemyEntity;
import game.dragonhero.mapping.main.ResPetEntity;
import game.dragonhero.server.IAction;
import game.dragonhero.service.Services;
import game.dragonhero.service.resource.ResEventTop;
import game.dragonhero.service.resource.ResFarm;
import game.dragonhero.service.resource.ResPet;
import game.dragonhero.service.user.Actions;
import game.dragonhero.service.user.Bonus;
import io.netty.channel.Channel;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.StringHelper;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import java.util.*;

public class PetHandler extends AHandler {
    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(MONSTER_COLLECTION_STATUS, MONSTER_COLLECTION_REWARD, MONSTER_COLLECTION_CARE, MONSTER_COLLECTION_UP_STAR, MONSTER_COLLECTION_GET_STAR, PET_SELECT, PET_SUMMON, PET_COLLECTION_STATUS, PET_COLLECTION_REWARD, PET_COLLECTION_CARE, PET_COLLECTION_UP_STAR, PET_COLLECTION_GET_STAR, PET_INFO);
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
                // monster
                case MONSTER_COLLECTION_STATUS -> petStatus(PetType.MONSTER);
                case MONSTER_COLLECTION_REWARD -> petReward(PetType.MONSTER);
                case MONSTER_COLLECTION_CARE -> petCare(PetType.MONSTER);
                case MONSTER_COLLECTION_UP_STAR -> petUpStar(PetType.MONSTER);
                case MONSTER_COLLECTION_GET_STAR -> petGetStar(PetType.MONSTER);
                // pet
                case PET_SELECT -> petSelect();
                case PET_SUMMON -> petSummon();
                case PET_COLLECTION_STATUS -> petStatus(PetType.ANIMAL);
                case PET_COLLECTION_REWARD -> petReward(PetType.ANIMAL);
                case PET_COLLECTION_CARE -> petCare(PetType.ANIMAL);
                case PET_COLLECTION_UP_STAR -> petUpStar(PetType.ANIMAL);
                case PET_COLLECTION_GET_STAR -> petGetStar(PetType.ANIMAL);

                // chung
                case PET_INFO -> petInfo();
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    private void petSelect() {
        int petId = getInputInt();
        UserPetEntity uPet = mUser.getResources().getPet(PetType.ANIMAL, petId);
        if (uPet != null && uPet.getHp() <= 0) {
            addErrResponse(getLang(Lang.err_pet_can_care));
            return;
        }
        // check  có th tháo pet
        if (user.updatePet(petId, uPet != null ? uPet.getStar() : 0)) {
            // check instance pet
            Player player = mUser.getPlayer();
            Pet pet = player.getPet();
            if (pet == null) {// chưa có pet trong room
                pet = mUser.getPet(player);
                if (pet != null) player.getRoom().addPet(pet);
                player.setPet(pet);
            } else { // đã có pet
                if (petId == 0) { // xóa pet
                    player.getRoom().removePet(pet);
                    player.setPet(null);
                } else {// đổi pet khác
                    Pet newPet = new Pet(uPet, player);
                    player.getRoom().changePet(pet, newPet);
                    player.setPet(newPet);
                    player.protoStatus(StateType.CHANGE_PET, (long) petId);
                }
            }
            addResponse(getCommonVector(petId));
            mUser.reCalculatePoint();
            // change pet battle
        } else addErrSystem();
    }

    private void petInfo() {
        List<Long> ids = getInputALong();
        if (ids.isEmpty() || ids.size() < 2) {
            addErrParam();
            return;
        }
        Pbmethod.PbListPet.Builder pbPets = Pbmethod.PbListPet.newBuilder();
        for (int i = 0; i < ids.size(); i += 2) {
            PetType type = PetType.get(ids.get(i).intValue());
            UserPetEntity uPet = type == PetType.MONSTER ? mUser.getResources().getMPetMonster().get(Math.toIntExact(ids.get(i + 1))) : mUser.getResources().getMPetAnimal().get(Math.toIntExact(ids.get(i + 1)));
            if (uPet != null) pbPets.addPets(uPet.toProto());
        }
        addResponse(pbPets.build());
    }


    private void petSummon() {
        List<Long> inputs = getInputALong();
        int number = inputs.get(0).intValue();
        int idSummon = inputs.get(2).intValue();
        ResPetEntity rPet = ResPet.getPet(idSummon);
        if (number != 1 && number != 10 ) {
            addErrParam();
            return;
        }
        if(rPet != null && rPet.getShowSummon() == 0){
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

    private void petStatus(PetType petType) {
        UserInt uInt = mUser.getUData().getUInt();
        List<Long> data = new ArrayList<>();
        if (petType == PetType.ANIMAL) {
            data.add((long) uInt.getValue(UserInt.PET_COLLECTION_POINT));
            data.add((long) CfgPet.MAX_POINT_BONUS_PET);
            data.addAll(CfgPet.getBonusPetPoint());
            addResponse(IAction.PET_COLLECTION_STATUS, getCommonVector((data)));
        } else {
            data.add((long) uInt.getValue(UserInt.MONSTER_COLLECTION_POINT));
            data.add((long) CfgPet.MAX_POINT_BONUS_MONSTER);
            data.addAll(CfgPet.getBonusMonsterPoint());
            addResponse(IAction.MONSTER_COLLECTION_STATUS, getCommonVector((data)));
        }
    }

    private void petReward(PetType petType) {
        UserInt uInt = mUser.getUData().getUInt();
        if (petType == PetType.ANIMAL) {
            int point = uInt.getValue(UserInt.PET_COLLECTION_POINT);
            if (point < CfgPet.MAX_POINT_BONUS_PET) {
                addErrResponse(getLang(Lang.err_no_bonus));
                return;
            }
            if (uInt.setValueAndUpdate(UserInt.PET_COLLECTION_POINT, point - CfgPet.MAX_POINT_BONUS_PET)) {
                Actions.save(user, "collection", "pet", "number", 1);
                addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.GET_REWARD_COLLECTION_PET.getKey(), CfgPet.getBonusPetPoint())));
            }
        } else {
            int point = uInt.getValue(UserInt.MONSTER_COLLECTION_POINT);
            if (point < CfgPet.MAX_POINT_BONUS_MONSTER) {
                addErrResponse(getLang(Lang.err_no_bonus));
                return;
            }
            if (uInt.setValueAndUpdate(UserInt.MONSTER_COLLECTION_POINT, point - CfgPet.MAX_POINT_BONUS_MONSTER)) {
                Actions.save(user, "collection", "monster", "number", 1);
                addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.GET_REWARD_COLLECTION_MONSTER.getKey(), CfgPet.getBonusMonsterPoint())));
            }
        }
    }

    private void petCare(PetType petType) {
        List<Long> inputs = getInputALong();
        int id = inputs.get(0).intValue();
        if (id < 0) {
            addErrParam();
            return;
        }
        if (id == 0) { // chăm sóc tất cả pet
            List<UserPetEntity> pets = petType == PetType.ANIMAL ? mUser.getResources().getMPetAnimal().values().stream().toList() : mUser.getResources().getMPetMonster().values().stream().toList();
            // check fee
            // 7 cấp rank item food
            List<Integer> food = NumberUtil.genListInt(7, 0);
            List<Integer> fee = NumberUtil.genListInt(7, 0);
            for (int i = 1; i <= 7; i++) {
                UserItemFarmEntity iFood = mUser.getResources().getItemFarm(ItemFarmType.FOOD, i);
                if (iFood != null) food.set(i - 1, iFood.getNumber());
            }
            // tính ra phí mỗi rank
            List<UserPetEntity> uPetUpdate = new ArrayList<>();
            for (int i = 0; i < pets.size(); i++) {
                UserPetEntity pet = pets.get(i);
                int need = pet.getNeedFood();
                if (need == 0) continue;
                int rank = pet.getResMonster().getRank() - 1;
                int maxRe = Math.min(food.get(rank), need);
                if (maxRe > 0) {
                    food.set(rank, food.get(rank) - maxRe);
                    fee.set(rank, fee.get(rank) + maxRe);
                    pet.addHp(maxRe);
                    uPetUpdate.add(pet);
                }
            }
            List<Long> bonus = new ArrayList<>();
            for (int i = 1; i <= fee.size(); i++) {
                bonus.addAll(Bonus.viewItemFarm(ItemFarmType.FOOD, i, -fee.get(i - 1)));
            }
            List<Long> aBonus = Bonus.receiveListItem(mUser, petType == PetType.ANIMAL ? DetailActionType.PET_CARE_ID.getKey(id) : DetailActionType.MONSTER_CARE_ID.getKey(id), bonus);
            if (!aBonus.isEmpty() && DBJPA.update(uPetUpdate.toArray())) {
                Pbmethod.ListCommonVector.Builder lcm = Pbmethod.ListCommonVector.newBuilder();
                lcm.addAVector(getCommonVector(aBonus));
                List<Long> dataRet = new ArrayList<>();
                for (UserPetEntity userPetEntity : uPetUpdate) {
                    dataRet.add((long) userPetEntity.getPetId());
                    dataRet.add((long) userPetEntity.getHp());
                }
                lcm.addAVector(getCommonVector(dataRet));
                addResponse(lcm.build());
                mUser.reCalculatePoint();
                mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.CARE_PET_MONSTER, uPetUpdate.size());
                addErrResponse(getLang(Lang.success));
            } else addErrSystem();
        } else {
            int num = inputs.get(1).intValue();
            if (num <= 0) {
                addErrParam();
                return;
            }
            UserPetEntity pet = mUser.getResources().getPet(petType.value, id);
            int need = pet.getNeedFood();
            if (need == CfgPet.getMaxHpByStar(pet.getStar())) {
                addErrResponse(getLang(Lang.err_pet_no_care_require));
                return;
            }
            num = Math.min(num, need);
            int rankPet =1;
            if(petType== PetType.ANIMAL){
                ResPetEntity res = pet.getResPet();
                rankPet = res.getRank();
            }else{
                ResEnemyEntity res = pet.getResMonster();
                rankPet = res.getRank();
            }
            List<Long> fee =  ResFarm.getItemFood(rankPet).getFood(-num);

            String err = Bonus.checkMoney(mUser, fee);
            if (err != null) {
                addErrResponse(err);
                return;
            }
            List<Long> bonus = Bonus.receiveListItem(mUser, petType == PetType.ANIMAL ? DetailActionType.PET_CARE_ID.getKey(id) : DetailActionType.MONSTER_CARE_ID.getKey(id), fee);
            pet.addHp(num);
            if (!bonus.isEmpty() && pet.update(List.of("time_care", pet.getTimeCare()))) {
                Pbmethod.ListCommonVector.Builder lcm = Pbmethod.ListCommonVector.newBuilder();
                lcm.addAVector(getCommonVector(bonus));
                lcm.addAVector(getCommonVector(pet.getPetId(), pet.getHp()));
                addResponse(lcm.build());
                if(pet.getNeedFood()==0){
                    addErrResponse(getLang(Lang.msg_pet_full_energy));
                }else addErrResponse(getLang(Lang.msg_pet_healthier));

                mUser.reCalculatePoint();
                mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.CARE_PET_MONSTER, 1);
            } else addErrSystem();
        }
    }

    private void petUpStar(PetType petType) {
        UserPetEntity uPet = mUser.getResources().getPet(petType.value, getInputInt());
        if (uPet == null || (uPet.getType() != PetType.ANIMAL && uPet.getType() != PetType.MONSTER)) {
            addErrParam();
            return;
        }
        if (CfgPet.isMaxStar(uPet.getStar())) {
            addErrResponse(getLang(Lang.err_pet_max_star));
            return;
        }
        List<Long> fee = petType == PetType.ANIMAL ? CfgPet.getFeeUpStarPet(uPet) : CfgPet.getFeeUpStarMonster(uPet);
        String err = Bonus.checkMoney(mUser, fee);
//        System.out.println("fee ========== " + fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        List<Long> bonus = Bonus.receiveListItem(mUser, petType == PetType.ANIMAL ? DetailActionType.UP_STAR_PET.getKey(uPet.getStar()) : DetailActionType.UP_STAR_MONSTER.getKey(uPet.getStar()), fee);
        if (!bonus.isEmpty() && uPet.updateStar()) {
            Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
            pb.addAVector(getCommonVector(uPet.getStar(), CfgPet.getMaxHpByStar(uPet.getStar())));
            pb.addAVector(getCommonIntVector(uPet.getBonusStar()));
            pb.addAVector(getCommonVector(bonus));
            pb.addAVector(user.reCalculatePoint(mUser).toCommonVector());
            addResponse(pb.build());
            ResEventTop.checkEvent(mUser, uPet, TopType.PET_POINT);
        } else addErrSystem();
    }

    private void petGetStar(PetType petType) {
        UserInt userInt = mUser.getUData().getUInt();
        UserPetEntity uPet = mUser.getResources().getPet(petType.value, getInputInt());
        if (uPet == null || (uPet.getType() != PetType.ANIMAL && uPet.getType() != PetType.MONSTER)) {
            addErrParam();
            return;
        }
        List<Integer> bonusStar = uPet.getBonusStar();
        if (bonusStar.isEmpty()) {
            addErrResponse(getLang(Lang.err_no_bonus));
            return;
        }
        int star = bonusStar.get(0);
        bonusStar.remove(0);
        int indexPoint = petType == PetType.ANIMAL ? UserInt.PET_COLLECTION_POINT : UserInt.MONSTER_COLLECTION_POINT;
        if (uPet.update(List.of("bonus_star", StringHelper.toDBString(bonusStar))) && userInt.setValueAndUpdate(indexPoint, userInt.getValue(indexPoint) + star)) {
            uPet.setBonusStar(bonusStar.toString());
            addResponse(getCommonIntVector(bonusStar));
            petStatus(petType);
        } else addErrSystem();
    }
}
