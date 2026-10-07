package game.treasure.controller;

import game.treasure.mapping.UserEntity;
import game.treasure.mapping.UserWingsEntity;
import game.treasure.mapping.UserPetEntity;
import game.treasure.server.IAction;
import game.config.lang.Lang;
import game.treasure.service.user.Bonus;
import protocol.Pbmethod;
import io.netty.channel.Channel;
import ozudo.base.log.Logs;

import java.util.*;

public class PetHandler extends AHandler {
    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(
                PET_INFO,
                PET_EQUIP, PET_UNEQUIP,
                WINGS_EQUIP, WINGS_UNEQUIP);
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
                case PET_INFO -> petInfo();
                case PET_EQUIP -> equipPet();
                case PET_UNEQUIP -> unequipPet();
                case WINGS_EQUIP -> equipWings();
                case WINGS_UNEQUIP -> unequipWings();
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
        syncAllPetWingsEquipFlags();
        finishPetWingsEquipChange(buildPetSlotPayload(pet, newBagSlot));
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
        syncAllPetWingsEquipFlags();
        Integer bagSlot = Bonus.findPetBagSlot(mUser, pet.getId());
        finishPetWingsEquipChange(buildPetSlotPayload(pet, bagSlot));
        syncEquippedPetInRoom();
    }

    private void equipWings() {
        List<Long> inputs = getInputALong();
        if (inputs.isEmpty()) {
            addErrParam();
            return;
        }
        long rowId = inputs.get(0);
        UserWingsEntity wings = mUser.getResources().getWings(rowId);
        if (wings == null) {
            addErrResponse(getLang(Lang.err_item_equip_not_found));
            return;
        }
        if (Bonus.isBlockedFromBagSlot(wings.getIsTrading(), wings.getInMarket())) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        if (UserWingsEntity.isEquipped(mUser, rowId)) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }

        int slotIdx = UserEntity.equipSlotIndex(Pbmethod.EquipSlotType.WINGS.getNumber());
        List<Integer> lst = mUser.getUser().normalizeItemEquipList();
        int oldRowId = lst.get(slotIdx);
        Integer newBagSlot = Bonus.findWingsBagSlot(mUser, rowId);

        Bonus.moveWingsOutOfBag(mUser, wings);

        if (oldRowId > 0 && oldRowId != (int) rowId) {
            UserWingsEntity oldWings = mUser.getResources().getWings(oldRowId);
            if (oldWings != null) {
                if (newBagSlot != null) {
                    if (!Bonus.moveWingsToBagSlot(mUser, oldWings, newBagSlot)) {
                        Bonus.moveWingsToBag(mUser, wings);
                        addErrResponse(getLang(Lang.err_max_slot));
                        return;
                    }
                } else if (!Bonus.moveWingsToBag(mUser, oldWings)) {
                    Bonus.moveWingsToBag(mUser, wings);
                    addErrResponse(getLang(Lang.err_max_slot));
                    return;
                }
                oldWings.syncEquipFlag(mUser);
            }
        }

        lst.set(slotIdx, (int) wings.getId());
        lst.set(slotIdx + 1, wings.getWingsId());
        lst.set(slotIdx + 2, wings.getLevel());
        if (!mUser.getUser().updateItemEquip(lst)) {
            addErrSystem();
            return;
        }
        syncAllPetWingsEquipFlags();
        finishPetWingsEquipChange(buildWingsSlotPayload(wings, newBagSlot));
    }

    private void unequipWings() {
        List<Long> inputs = getInputALong();
        if (inputs.isEmpty()) {
            addErrParam();
            return;
        }
        long rowId = inputs.get(0);
        if (!UserWingsEntity.isEquipped(mUser, rowId)) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        UserWingsEntity wings = mUser.getResources().getWings(rowId);
        if (wings == null) {
            addErrResponse(getLang(Lang.err_item_equip_not_found));
            return;
        }
        if (!mUser.getResources().canAddBagItem(1)) {
            addErrResponse(getLang(Lang.err_max_slot));
            return;
        }
        if (!Bonus.clearWingsEquipSlot(mUser)) {
            addErrSystem();
            return;
        }
        if (!Bonus.moveWingsToBag(mUser, wings)) {
            addErrResponse(getLang(Lang.err_max_slot));
            return;
        }
        syncAllPetWingsEquipFlags();
        Integer bagSlot = Bonus.findWingsBagSlot(mUser, wings.getId());
        finishPetWingsEquipChange(buildWingsSlotPayload(wings, bagSlot));
    }

    void syncAllPetWingsEquipFlags() {
        for (UserPetEntity p : mUser.getResources().getMPet().values())
            p.syncEquipFlag(mUser);
        for (UserWingsEntity m : mUser.getResources().getMWings().values())
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

    List<Long> buildWingsSlotPayload(UserWingsEntity wings, Integer freedOrNewSlot) {
        List<Long> data = new ArrayList<>();
        if (freedOrNewSlot != null && freedOrNewSlot >= 0) {
            if (wings != null && UserWingsEntity.isEquipped(mUser, wings.getId())) {
                data.add(0L);
                data.add((long) freedOrNewSlot);
            } else if (wings != null) {
                data.add(wings.getId());
                data.add((long) freedOrNewSlot);
            }
        }
        return data;
    }

    private void finishPetWingsEquipChange(List<Long> slotPairUpdates) {
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
