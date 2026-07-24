package game.treasure.controller;

import game.config.CfgPet;
import game.config.CfgQuest;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.treasure.mapping.UserEventSevenDayEntity;
import game.treasure.mapping.UserEntity;
import game.treasure.mapping.UserMountEntity;
import game.treasure.mapping.UserPetEntity;
import game.treasure.mapping.main.ResPetEntity;
import game.treasure.server.IAction;
import game.treasure.service.Services;
import game.treasure.service.resource.ResPet;
import game.treasure.service.user.Bonus;
import protocol.Pbmethod;
import io.netty.channel.Channel;
import ozudo.base.log.Logs;

import java.util.*;

public class PetHandler extends AHandler {
    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(
                PET_SUMMON, PET_INFO,
                PET_EQUIP, PET_UNEQUIP,
                MOUNT_EQUIP, MOUNT_UNEQUIP);
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
                case PET_EQUIP -> equipPet();
                case PET_UNEQUIP -> unequipPet();
                case MOUNT_EQUIP -> equipMount();
                case MOUNT_UNEQUIP -> unequipMount();
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }


    private void petInfo() {
        List<Long> ids = getInputALong();
        if (ids.isEmpty()) {
            addErrParam();
            return;
        }
        Pbmethod.PbListPet.Builder pbPets = Pbmethod.PbListPet.newBuilder();
        for (int i = 0; i < ids.size(); i++) {
            long petRowId = ids.get(i);
            UserPetEntity uPet = mUser.getResources().getPet(petRowId);
            if (uPet != null) {
                uPet.syncEquipFlag(mUser);
                pbPets.addPets(uPet.toProto());
            }
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
        if (rPet == null || rPet.getShowSummon() == 0) {
            addErrParam();
            return;
        }
        boolean isVip = inputs.get(1) == 1L;
        List<Long> bonus = Bonus.viewItemPoint(isVip ? game.config.aEnum.ItemPointKey.BONG_SIEU_THU.id : game.config.aEnum.ItemPointKey.BONG_LINH_THU.id, -number);
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

    private void equipPet() {
        List<Long> inputs = getInputALong();
        if (inputs.isEmpty()) {
            addErrParam();
            return;
        }
        long rowId = inputs.get(0);
        UserPetEntity pet = mUser.getResources().getPet(rowId);
        if (pet == null) {
            addErrResponse(getLang(Lang.err_item_equip_not_found));
            return;
        }
        if (Bonus.isBlockedFromBagSlot(pet.getIsTrading(), pet.getInMarket())) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        if (UserPetEntity.isEquipped(mUser, rowId)) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }

        int slotIdx = UserEntity.equipSlotIndex(Pbmethod.EquipSlotType.PET.getNumber());
        List<Integer> lst = mUser.getUser().normalizeItemEquipList();
        int oldRowId = lst.get(slotIdx);
        Integer newBagSlot = Bonus.findPetBagSlot(mUser, rowId);

        Bonus.movePetOutOfBag(mUser, pet);

        if (oldRowId > 0 && oldRowId != (int) rowId) {
            UserPetEntity oldPet = mUser.getResources().getPet(oldRowId);
            if (oldPet != null) {
                if (newBagSlot != null) {
                    if (!Bonus.movePetToBagSlot(mUser, oldPet, newBagSlot)) {
                        Bonus.movePetToBag(mUser, pet);
                        addErrResponse(getLang(Lang.err_max_slot));
                        return;
                    }
                } else if (!Bonus.movePetToBag(mUser, oldPet)) {
                    Bonus.movePetToBag(mUser, pet);
                    addErrResponse(getLang(Lang.err_max_slot));
                    return;
                }
                oldPet.syncEquipFlag(mUser);
            }
        }

        lst.set(slotIdx, (int) pet.getId());
        lst.set(slotIdx + 1, pet.getPetId());
        lst.set(slotIdx + 2, pet.getLevel());
        if (!mUser.getUser().updateItemEquip(lst)) {
            addErrSystem();
            return;
        }
        syncAllPetMountEquipFlags();
        finishPetMountEquipChange(buildPetSlotPayload(pet, newBagSlot));
        syncEquippedPetInRoom();
    }

    private void unequipPet() {
        List<Long> inputs = getInputALong();
        if (inputs.isEmpty()) {
            addErrParam();
            return;
        }
        long rowId = inputs.get(0);
        if (!UserPetEntity.isEquipped(mUser, rowId)) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        UserPetEntity pet = mUser.getResources().getPet(rowId);
        if (pet == null) {
            addErrResponse(getLang(Lang.err_item_equip_not_found));
            return;
        }
        if (!mUser.getResources().canAddBagItem(1)) {
            addErrResponse(getLang(Lang.err_max_slot));
            return;
        }
        if (!Bonus.clearPetEquipSlot(mUser)) {
            addErrSystem();
            return;
        }
        if (!Bonus.movePetToBag(mUser, pet)) {
            addErrResponse(getLang(Lang.err_max_slot));
            return;
        }
        syncAllPetMountEquipFlags();
        Integer bagSlot = Bonus.findPetBagSlot(mUser, pet.getId());
        finishPetMountEquipChange(buildPetSlotPayload(pet, bagSlot));
        syncEquippedPetInRoom();
    }

    private void equipMount() {
        List<Long> inputs = getInputALong();
        if (inputs.isEmpty()) {
            addErrParam();
            return;
        }
        long rowId = inputs.get(0);
        UserMountEntity mount = mUser.getResources().getMount(rowId);
        if (mount == null) {
            addErrResponse(getLang(Lang.err_item_equip_not_found));
            return;
        }
        if (Bonus.isBlockedFromBagSlot(mount.getIsTrading(), mount.getInMarket())) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        if (UserMountEntity.isEquipped(mUser, rowId)) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }

        int slotIdx = UserEntity.equipSlotIndex(Pbmethod.EquipSlotType.MOUNT.getNumber());
        List<Integer> lst = mUser.getUser().normalizeItemEquipList();
        int oldRowId = lst.get(slotIdx);
        Integer newBagSlot = Bonus.findMountBagSlot(mUser, rowId);

        Bonus.moveMountOutOfBag(mUser, mount);

        if (oldRowId > 0 && oldRowId != (int) rowId) {
            UserMountEntity oldMount = mUser.getResources().getMount(oldRowId);
            if (oldMount != null) {
                if (newBagSlot != null) {
                    if (!Bonus.moveMountToBagSlot(mUser, oldMount, newBagSlot)) {
                        Bonus.moveMountToBag(mUser, mount);
                        addErrResponse(getLang(Lang.err_max_slot));
                        return;
                    }
                } else if (!Bonus.moveMountToBag(mUser, oldMount)) {
                    Bonus.moveMountToBag(mUser, mount);
                    addErrResponse(getLang(Lang.err_max_slot));
                    return;
                }
                oldMount.syncEquipFlag(mUser);
            }
        }

        lst.set(slotIdx, (int) mount.getId());
        lst.set(slotIdx + 1, mount.getMountId());
        lst.set(slotIdx + 2, mount.getLevel());
        if (!mUser.getUser().updateItemEquip(lst)) {
            addErrSystem();
            return;
        }
        syncAllPetMountEquipFlags();
        finishPetMountEquipChange(buildMountSlotPayload(mount, newBagSlot));
    }

    private void unequipMount() {
        List<Long> inputs = getInputALong();
        if (inputs.isEmpty()) {
            addErrParam();
            return;
        }
        long rowId = inputs.get(0);
        if (!UserMountEntity.isEquipped(mUser, rowId)) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        UserMountEntity mount = mUser.getResources().getMount(rowId);
        if (mount == null) {
            addErrResponse(getLang(Lang.err_item_equip_not_found));
            return;
        }
        if (!mUser.getResources().canAddBagItem(1)) {
            addErrResponse(getLang(Lang.err_max_slot));
            return;
        }
        if (!Bonus.clearMountEquipSlot(mUser)) {
            addErrSystem();
            return;
        }
        if (!Bonus.moveMountToBag(mUser, mount)) {
            addErrResponse(getLang(Lang.err_max_slot));
            return;
        }
        syncAllPetMountEquipFlags();
        Integer bagSlot = Bonus.findMountBagSlot(mUser, mount.getId());
        finishPetMountEquipChange(buildMountSlotPayload(mount, bagSlot));
    }

    void syncAllPetMountEquipFlags() {
        for (UserPetEntity p : mUser.getResources().getMPet().values())
            p.syncEquipFlag(mUser);
        for (UserMountEntity m : mUser.getResources().getMMount().values())
            m.syncEquipFlag(mUser);
    }

    List<Long> buildPetSlotPayload(UserPetEntity pet, Integer freedOrNewSlot) {
        List<Long> data = new ArrayList<>();
        if (freedOrNewSlot != null && freedOrNewSlot >= 0) {
            if (pet != null && UserPetEntity.isEquipped(mUser, pet.getId())) {
                data.add(0L);
                data.add((long) freedOrNewSlot);
            } else if (pet != null) {
                data.add(pet.getId());
                data.add((long) freedOrNewSlot);
            }
        }
        return data;
    }

    List<Long> buildMountSlotPayload(UserMountEntity mount, Integer freedOrNewSlot) {
        List<Long> data = new ArrayList<>();
        if (freedOrNewSlot != null && freedOrNewSlot >= 0) {
            if (mount != null && UserMountEntity.isEquipped(mUser, mount.getId())) {
                data.add(0L);
                data.add((long) freedOrNewSlot);
            } else if (mount != null) {
                data.add(mount.getId());
                data.add((long) freedOrNewSlot);
            }
        }
        return data;
    }

    private void finishPetMountEquipChange(List<Long> slotPairUpdates) {
        Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
        pb.addAVector(user.reCalculatePoint(mUser).toCommonVector());
        pb.addAVector(getCommonIntVector(mUser.getUser().normalizeItemEquipList()));
        if (slotPairUpdates != null && !slotPairUpdates.isEmpty())
            pb.addAVector(getCommonVector(slotPairUpdates));
        addResponse(pb.build());
        mUser.reCalculatePoint();
        addResponse(IAction.UPDATE_BAG, mUser.getResources().buildUpdateBagPayload());
        UserHandler.buffInfo(mUser);
        if (mUser.getPlayer() != null)
            mUser.getPlayer().broadcastEquipViewEffect();
    }

    /** Equip/unequip pet khi đang ở map: remove pet cũ / add pet mới vào room. */
    private void syncEquippedPetInRoom() {
        game.battle.model.Player player = mUser.getPlayer();
        if (player == null) return;
        game.treasure.table.BaseRoom room = player.getRoom();

        game.battle.model.Pet oldPet = player.getPetUse();
        if (oldPet != null && oldPet.getId() > 0 && room != null)
            room.removeUnit(oldPet.getId());
        player.setPetUse(null);
        mUser.clearCachedPet();

        game.battle.model.Pet newPet = mUser.getPet(player);
        player.setPetUse(newPet);
        if (newPet != null && room != null) {
            newPet.setPos(game.battle.object.Pos.randomPos(player.getPos(), 1f, 1f));
            room.addUnit(newPet);
        }
    }
}
