package game.treasure.controller;

import game.config.CfgChat;
import game.config.CfgItem;
import game.config.CfgMaterial;
import game.config.CfgMob;
import game.config.CfgTreasure;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.treasure.mapping.UserEntity;
import game.treasure.mapping.UserEquipmentEntity;
import game.treasure.mapping.UserItemEntity;
import game.treasure.mapping.UserItemPointEntity;
import game.treasure.mapping.UserMaterialEntity;
import game.treasure.mapping.UserMobEntity;
import game.treasure.mapping.UserMountEntity;
import game.treasure.mapping.UserPetEntity;
import game.treasure.mapping.main.ResItemEntity;
import game.treasure.mapping.main.ResItemEquipmentEntity;
import game.treasure.server.IAction;
import game.treasure.service.battle.TreasureEventService;
import game.treasure.service.item.EquipmentExpireService;
import game.treasure.service.resource.ResItem;
import game.treasure.service.user.Bonus;
import game.treasure.service.user.EquipSlotBonus;
import game.monitor.Online;
import game.object.MyUser;
import game.protocol.CommonProto;
import protocol.Pbmethod;
import io.netty.channel.Channel;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.*;
import ozudo.base.log.Logs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static game.config.CfgItem.SPEAKER_MAX_LEN;


public class ItemHandler extends AHandler {
    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(ITEM_EQUIPMENT_INFO, ITEM_EQUIPMENT_LOCK_DESTROY, ITEM_UP_LEVEL,
                ITEM_EQUIPMENT_VIEW_INFO, ITEM_INFO, ITEM_SELL, ITEM_EQUIPMENT_UN_EQUIP, ITEM_USED, ITEM_EQUIPMENT_EQUIP, SPEAKER_SEND);
        actions.forEach(action -> mHandler.put(action, this));
    }

    @Override
    public AHandler newInstance() {
        return new ItemHandler();
    }

    static ItemHandler instance;

    public static ItemHandler getInstance() {
        if (instance == null) {
            instance = new ItemHandler();
        }
        return instance;
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        try {
            switch (actionId) {
                case IAction.ITEM_EQUIPMENT_INFO -> listEquipment(getInputALong(), this, mUser);
                case IAction.ITEM_EQUIPMENT_EQUIP -> equipItem();
                case IAction.ITEM_EQUIPMENT_UN_EQUIP -> unEquipItem();
                case IAction.ITEM_SELL -> sellItem();
                case IAction.ITEM_USED -> usedItem();
                case IAction.ITEM_INFO -> itemInfo();
                case IAction.ITEM_EQUIPMENT_LOCK_DESTROY -> lockDestroy();
                case IAction.ITEM_UP_LEVEL -> uplevel();
                case IAction.ITEM_EQUIPMENT_VIEW_INFO -> viewInfoEquipment();
                case IAction.SPEAKER_SEND -> speakerSend();
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }


    public static void listEquipment(List<Long> ids, AHandler handler, MyUser mUser) {
        Pbmethod.PbListEquipment.Builder lst = Pbmethod.PbListEquipment.newBuilder();
        System.out.println("ids = " + ids);
        for (int i = 0; i < ids.size(); i++) {
            UserEquipmentEntity itemEquipment = mUser.getResources().getEquipment(ids.get(i));
            if (itemEquipment == null) {
                handler.addErrResponse(getLang(mUser, Lang.err_has_item));
                return;
            }
            lst.addEquipment(itemEquipment.toProto());
        }
        handler.addResponse(IAction.ITEM_EQUIPMENT_INFO, lst.build());
    }

    void viewInfoEquipment() {
        try {
            List<Long> ids = CommonProto.parseCommonVector(requestData).getALongList();
            int userId = ids.get(0).intValue();
            List<Long> item = ids.subList(1, ids.size());
            MyUser mUser = Online.getMUser(userId);
            Pbmethod.PbListEquipment.Builder lst = Pbmethod.PbListEquipment.newBuilder();
            if (mUser != null) { // có online

                for (int i = 0; i < item.size(); i++) {
                    UserEquipmentEntity itemEquipment = mUser.getResources().getItemEquipment(item.get(i));
                    if (itemEquipment != null) lst.addEquipment(itemEquipment.toProto());
                }

            } else {
                String sql = "Select * from user_equipment where id in(" + NumberUtil.joiningListLong(item) + ")";
                List<UserEquipmentEntity> lstUE = DBJPA.getSelectQuery(sql, UserEquipmentEntity.class);
                for (int i = 0; i < lstUE.size(); i++) {
                    lst.addEquipment(lstUE.get(i).toProto());
                }
            }
            addResponse(lst.build());

        } catch (Exception ex) {
            ex.printStackTrace();
            addErrParam();
        }
    }

    void equipItem() {
        List<Long> inputs = getInputALong();
        int itemId = inputs.get(0).intValue();
        UserEquipmentEntity iEquip = mUser.getResources().getEquipment(itemId);
        if (iEquip == null) {
            addErrResponse(getLang(Lang.item_not_own));
            return;
        }
        if (iEquip.isEquip() || mUser.getUser().getListIdEquipmentEquip().contains(itemId)) {
            addErrResponse(getLang(Lang.err_use_item_equip));
            return;
        }
        if (iEquip.isExpired()) {
            EquipmentExpireService.unequipAndClearBag(mUser, iEquip);
            addErrResponse(getLang(Lang.err_item_expire));
            return;
        }
        ResItemEquipmentEntity resEquip = iEquip.getResEquipment();
        if (resEquip == null) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        int slotIndex = mUser.getUser().equipSlotIndex(resEquip.getType());
        if (slotIndex < 0) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }

        mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.USE_ITEM_EQUIP, 1);
        int oldBonusBag = EquipSlotBonus.bagBonus(mUser);
        int oldBonusMat = EquipSlotBonus.materialBonus(mUser);
        List<UserEquipmentEntity> slotUpdates = new ArrayList<>();
        List<Integer> lst = mUser.getUser().normalizeItemEquipList();
        int oldId = lst.get(slotIndex);
        UserEquipmentEntity oldEquip = oldId > 0 && oldId != itemId
                ? mUser.getResources().getEquipment(oldId) : null;
        if (oldId > 0 && oldId != itemId && oldEquip == null) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        int savedBagSlot = iEquip.getBagSlot();
        if (!Bonus.moveEquipmentOutOfBag(mUser, iEquip)) {
            addErrResponse();
            return;
        }
        slotUpdates.add(iEquip);
        if (oldEquip != null) {
            if (!Bonus.moveEquipmentToBag(mUser, oldEquip)) {
                if (savedBagSlot >= 0)
                    Bonus.moveEquipmentToBag(mUser, iEquip);
                addErrResponse(getLang(Lang.err_max_slot));
                return;
            }
            oldEquip.unEquip();
            slotUpdates.add(oldEquip);
        }
        lst.set(slotIndex, itemId);
        lst.set(slotIndex + 1, iEquip.getItemId());
        lst.set(slotIndex + 2, iEquip.getLevel());
        if (!mUser.getUser().updateItemEquip(lst)) {
            addErrResponse();
            return;
        }
        iEquip.setEquip(true);
        finishEquipChange(slotUpdates, oldBonusBag, oldBonusMat);
    }

    void unEquipItem() {
        int id = (int) CommonProto.parseCommonVector(requestData).getALong(0);
        UserEquipmentEntity iEquip = mUser.getResources().getItemEquipment(id);
        if (iEquip == null) {
            addErrResponse(getLang(Lang.item_not_own));
            return;
        }
        if (!iEquip.isEquip() && !mUser.getUser().getListIdEquipmentEquip().contains(id)) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        List<Integer> lst = mUser.getUser().normalizeItemEquipList();
        int slotIndex = UserEntity.findEquipSlotByItemId(lst, id);
        if (slotIndex < 0) {
            ResItemEquipmentEntity resEquip = iEquip.getResEquipment();
            if (resEquip == null) {
                addErrResponse(getLang(Lang.err_params));
                return;
            }
            slotIndex = mUser.getUser().equipSlotIndex(resEquip.getType());
            if (slotIndex < 0 || lst.get(slotIndex) != id) {
                addErrResponse(getLang(Lang.err_params));
                return;
            }
        }
        if (!mUser.getResources().canAddBagItem(1)) {
            addErrResponse(getLang(Lang.err_max_slot));
            return;
        }
        int oldBonusBag = EquipSlotBonus.bagBonus(mUser);
        int oldBonusMat = EquipSlotBonus.materialBonus(mUser);
        List<Integer> equipBackup = new ArrayList<>(lst);
        lst.set(slotIndex, 0);
        lst.set(slotIndex + 1, 0);
        lst.set(slotIndex + 2, 0);
        if (!mUser.getUser().updateItemEquip(lst)) {
            addErrResponse();
            return;
        }
        List<UserEquipmentEntity> slotUpdates = new ArrayList<>();
        if (!Bonus.moveEquipmentToBag(mUser, iEquip)) {
            mUser.getUser().updateItemEquip(equipBackup);
            addErrResponse(getLang(Lang.err_max_slot));
            return;
        }
        iEquip.unEquip();
        slotUpdates.add(iEquip);
        finishEquipChange(slotUpdates, oldBonusBag, oldBonusMat);
    }

    private void finishEquipChange(List<UserEquipmentEntity> slotUpdates, int oldBonusBag, int oldBonusMat) {
        mUser.getUData().syncSlotsAfterEquipmentChange(mUser, oldBonusBag, oldBonusMat);
        Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
        pb.addAVector(user.reCalculatePoint(mUser).toCommonVector());
        pb.addAVector(getCommonIntVector(mUser.getUser().normalizeItemEquipList()));
        if (slotUpdates != null && !slotUpdates.isEmpty()) {
            List<Integer> slotData = new ArrayList<>();
            for (UserEquipmentEntity item : slotUpdates) {
                slotData.add((int) item.getId());
                slotData.add(item.getBagSlot());
            }
            pb.addAVector(getCommonIntVector(slotData));
        }
        addResponse(pb.build());
        mUser.reCalculatePoint();
        broadcastItemEquipUpdate();
        UserHandler.buffInfo(mUser);
    }

    private void broadcastItemEquipUpdate() {
        if (mUser.getPlayer() == null) return;
        mUser.getPlayer().protoStatus(Pbmethod.SubStateType.UPDATE_ITEM_EQUIP, mUser.getUser().getListItemKeyEquipLong());
    }


    void sellItem() {
        List<Long> req = CommonProto.parseCommonVector(requestData).getALongList();
        if (req == null || req.size() < 2) {
            addErrParam();
            return;
        }
        if (req.get(0) == 1L && req.size() >= 3) {
            sellItemBatchAuto(req.subList(1, req.size()));
            return;
        }
        if (req.size() != 2) {
            addErrParam();
            return;
        }
        int bonusType = req.get(0).intValue();
        long id = req.get(1);
        if (bonusType == Bonus.BONUS_ITEM && TreasureEventService.isRuntimeKeySell(id)) {
            if (!TreasureEventService.clearKeyForSell(mUser)) {
                addErrResponse(getLang(Lang.item_not_own));
                return;
            }
            List<Long> bonus = CfgTreasure.keySellGem() > 0
                    ? Bonus.viewGem(CfgTreasure.keySellGem())
                    : new ArrayList<>();
            if (!bonus.isEmpty()) {
                addBonusToast(Bonus.receiveListItem(mUser, DetailActionType.SELL_ITEM.getKey(CfgTreasure.keyItemId()), bonus));
            }
            addResponse(getCommonVector(id, 1L));
            return;
        }
        if (bonusType == Bonus.BONUS_EQUIPMENT) {
            UserEquipmentEntity equip = mUser.getResources().getEquipment(id);
            if (equip == null) {
                addErrResponse(getLang(Lang.item_not_own));
                return;
            }
            sellEquipment(equip);
            return;
        }
        if (bonusType == Bonus.BONUS_ITEM) {
            UserItemEntity item = mUser.getResources().getItem(id);
            if (item == null) {
                addErrResponse(getLang(Lang.item_not_own));
                return;
            }
            Bonus.clearItemFromSlot(mUser, Bonus.BONUS_ITEM, id);
            if (item.deleteFromDb()) {
                mUser.getResources().removeItem(id);
                List<Long> bonus = CfgItem.getPriceSellItem(item);
                addBonusToast(Bonus.receiveListItem(mUser, DetailActionType.SELL_ITEM.getKey(item.getItemId()), bonus));
                addResponse(getCommonVector(id, 1L));
            } else {
                addErrResponse();
            }
            return;
        }
        if (bonusType == Bonus.BONUS_MATERIAL) {
            UserMaterialEntity material = mUser.getResources().getMaterial(id);
            if (material == null) {
                addErrResponse(getLang(Lang.item_not_own));
                return;
            }
            if (material.deleteFromDb()) {
                mUser.getResources().removeMaterial(id);
                List<Long> bonus = CfgMaterial.getPriceSellMaterial(material);
                addBonusToast(Bonus.receiveListItem(mUser, DetailActionType.SELL_ITEM.getKey(material.getMaterialId()), bonus));
                addResponse(getCommonVector(id, 1L));
            } else {
                addErrResponse();
            }
            return;
        }
        if (bonusType == Bonus.BONUS_MOB) {
            UserMobEntity mob = mUser.getResources().getMob(id);
            if (mob == null) {
                addErrResponse(getLang(Lang.item_not_own));
                return;
            }
            List<Long> price = CfgMob.getPriceSellMob(mob);
            if (price.isEmpty()) {
                addErrResponse(getLang(Lang.err_params));
                return;
            }
            if (!MobHandler.removeUserMob(mUser, mob)) {
                addErrResponse();
                return;
            }
            addBonusToast(Bonus.receiveListItem(mUser, DetailActionType.SELL_ITEM.getKey(mob.getMobId()), price));
            addResponse(getCommonVector(id, 1L));
            return;
        }
        if (bonusType == Bonus.BONUS_PET) {
            UserPetEntity pet = mUser.getResources().getPet(id);
            if (pet == null) {
                addErrResponse(getLang(Lang.item_not_own));
                return;
            }
            sellPet(pet);
            return;
        }
        if (bonusType == Bonus.BONUS_MOUNT) {
            UserMountEntity mount = mUser.getResources().getMount(id);
            if (mount == null) {
                addErrResponse(getLang(Lang.item_not_own));
                return;
            }
            sellMount(mount);
            return;
        }
        addErrParam();
    }

    /** Auto-sell batch: prefix 1 + pairs (bonusType, id). Vàng cộng im lặng, không BONUS_TOAST. */
    private void sellItemBatchAuto(List<Long> pairs) {
        if (pairs == null || pairs.size() < 2 || pairs.size() % 2 != 0)
            return;

        List<Long> allPriceBonus = new ArrayList<>();
        List<Long> soldResponse = new ArrayList<>();
        boolean needEquipRecalc = false;
        int oldBonusBag = EquipSlotBonus.bagBonus(mUser);
        int oldBonusMat = EquipSlotBonus.materialBonus(mUser);

        for (int i = 0; i < pairs.size(); i += 2) {
            int bonusType = pairs.get(i).intValue();
            long id = pairs.get(i + 1);
            SellBatchResult result = trySellInBatch(bonusType, id);
            if (result == null)
                continue;
            if (result.priceBonus != null && !result.priceBonus.isEmpty())
                allPriceBonus.addAll(result.priceBonus);
            soldResponse.add(id);
            soldResponse.add(1L);
            if (result.wasEquipped)
                needEquipRecalc = true;
        }

        if (soldResponse.isEmpty())
            return;

        List<Long> merged = Bonus.merge(allPriceBonus);
        addBonusPrivate(Bonus.receiveListItem(mUser, DetailActionType.SELL_ITEM.getKey(-1), merged));

        if (needEquipRecalc) {
            mUser.getUData().syncSlotsAfterEquipmentChange(mUser, oldBonusBag, oldBonusMat);
            Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
            pb.addAVector(getCommonVector(soldResponse));
            pb.addAVector(user.reCalculatePoint(mUser).toCommonVector());
            pb.addAVector(getCommonIntVector(mUser.getUser().normalizeItemEquipList()));
            addResponse(pb.build());
            mUser.reCalculatePoint();
            broadcastItemEquipUpdate();
            UserHandler.buffInfo(mUser);
        } else {
            addResponse(getCommonVector(soldResponse));
        }
    }

    private static final class SellBatchResult {
        List<Long> priceBonus;
        boolean wasEquipped;
    }

    private SellBatchResult trySellInBatch(int bonusType, long id) {
        if (bonusType == Bonus.BONUS_EQUIPMENT) {
            UserEquipmentEntity equip = mUser.getResources().getEquipment(id);
            if (equip == null)
                return null;
            return sellEquipmentForBatch(equip);
        }
        if (bonusType == Bonus.BONUS_ITEM) {
            UserItemEntity item = mUser.getResources().getItem(id);
            if (item == null)
                return null;
            Bonus.clearItemFromSlot(mUser, Bonus.BONUS_ITEM, id);
            if (!item.deleteFromDb())
                return null;
            mUser.getResources().removeItem(id);
            SellBatchResult result = new SellBatchResult();
            result.priceBonus = CfgItem.getPriceSellItem(item);
            return result;
        }
        if (bonusType == Bonus.BONUS_MATERIAL) {
            UserMaterialEntity material = mUser.getResources().getMaterial(id);
            if (material == null)
                return null;
            if (!material.deleteFromDb())
                return null;
            mUser.getResources().removeMaterial(id);
            SellBatchResult result = new SellBatchResult();
            result.priceBonus = CfgMaterial.getPriceSellMaterial(material);
            return result;
        }
        if (bonusType == Bonus.BONUS_MOB) {
            UserMobEntity mob = mUser.getResources().getMob(id);
            if (mob == null)
                return null;
            if (!MobHandler.removeUserMob(mUser, mob))
                return null;
            SellBatchResult result = new SellBatchResult();
            result.priceBonus = CfgMob.getPriceSellMob(mob);
            return result;
        }
        if (bonusType == Bonus.BONUS_PET) {
            UserPetEntity pet = mUser.getResources().getPet(id);
            if (pet == null)
                return null;
            return sellPetForBatch(pet);
        }
        if (bonusType == Bonus.BONUS_MOUNT) {
            UserMountEntity mount = mUser.getResources().getMount(id);
            if (mount == null)
                return null;
            return sellMountForBatch(mount);
        }
        return null;
    }

    private void sellPet(UserPetEntity pet) {
        long id = pet.getId();
        boolean wasEquipped = UserPetEntity.isEquipped(mUser, id);
        if (wasEquipped && !Bonus.clearPetEquipSlot(mUser)) {
            addErrSystem();
            return;
        }
        Bonus.clearItemFromSlot(mUser, Bonus.BONUS_PET, id);
        if (!pet.deleteFromDb()) {
            addErrSystem();
            return;
        }
        mUser.getResources().removePet(id);
        List<Long> bonus = CfgItem.getPriceSellPet(pet);
        addBonusToast(Bonus.receiveListItem(mUser, DetailActionType.SELL_ITEM.getKey(pet.getPetId()), bonus));
        if (wasEquipped) {
            Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
            pb.addAVector(getCommonVector(id, 1L));
            pb.addAVector(user.reCalculatePoint(mUser).toCommonVector());
            pb.addAVector(getCommonIntVector(mUser.getUser().normalizeItemEquipList()));
            addResponse(pb.build());
            mUser.reCalculatePoint();
            UserHandler.buffInfo(mUser);
        } else {
            addResponse(getCommonVector(id, 1L));
        }
    }

    private void sellMount(UserMountEntity mount) {
        long id = mount.getId();
        boolean wasEquipped = UserMountEntity.isEquipped(mUser, id);
        if (wasEquipped && !Bonus.clearMountEquipSlot(mUser)) {
            addErrSystem();
            return;
        }
        Bonus.clearItemFromSlot(mUser, Bonus.BONUS_MOUNT, id);
        if (!mount.deleteFromDb()) {
            addErrSystem();
            return;
        }
        mUser.getResources().removeMount(id);
        List<Long> bonus = CfgItem.getPriceSellMount(mount);
        addBonusToast(Bonus.receiveListItem(mUser, DetailActionType.SELL_ITEM.getKey(mount.getMountId()), bonus));
        if (wasEquipped) {
            Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
            pb.addAVector(getCommonVector(id, 1L));
            pb.addAVector(user.reCalculatePoint(mUser).toCommonVector());
            pb.addAVector(getCommonIntVector(mUser.getUser().normalizeItemEquipList()));
            addResponse(pb.build());
            mUser.reCalculatePoint();
            UserHandler.buffInfo(mUser);
        } else {
            addResponse(getCommonVector(id, 1L));
        }
    }

    private SellBatchResult sellPetForBatch(UserPetEntity pet) {
        long id = pet.getId();
        boolean wasEquipped = UserPetEntity.isEquipped(mUser, id);
        if (wasEquipped && !Bonus.clearPetEquipSlot(mUser))
            return null;
        Bonus.clearItemFromSlot(mUser, Bonus.BONUS_PET, id);
        if (!pet.deleteFromDb())
            return null;
        mUser.getResources().removePet(id);
        SellBatchResult result = new SellBatchResult();
        result.priceBonus = CfgItem.getPriceSellPet(pet);
        result.wasEquipped = wasEquipped;
        return result;
    }

    private SellBatchResult sellMountForBatch(UserMountEntity mount) {
        long id = mount.getId();
        boolean wasEquipped = UserMountEntity.isEquipped(mUser, id);
        if (wasEquipped && !Bonus.clearMountEquipSlot(mUser))
            return null;
        Bonus.clearItemFromSlot(mUser, Bonus.BONUS_MOUNT, id);
        if (!mount.deleteFromDb())
            return null;
        mUser.getResources().removeMount(id);
        SellBatchResult result = new SellBatchResult();
        result.priceBonus = CfgItem.getPriceSellMount(mount);
        result.wasEquipped = wasEquipped;
        return result;
    }

    private SellBatchResult sellEquipmentForBatch(UserEquipmentEntity equip) {
        long id = equip.getId();
        if (equip.getLockDestroy() == 1)
            return null;
        boolean wasEquipped = equip.isEquip() || mUser.getUser().getListIdEquipmentEquip().contains((int) id);
        if (wasEquipped) {
            if (!clearItemFromEquipList((int) id, equip))
                return null;
            equip.unEquip();
        }
        Bonus.clearItemFromSlot(mUser, Bonus.BONUS_EQUIPMENT, id);
        if (!equip.deleteFromDb())
            return null;
        mUser.getResources().removeEquipment(id);
        SellBatchResult result = new SellBatchResult();
        result.priceBonus = CfgItem.getPriceSellItem(equip);
        result.wasEquipped = wasEquipped;
        return result;
    }

    private void sellEquipment(UserEquipmentEntity equip) {
        long id = equip.getId();
        if (equip.getLockDestroy() == 1) {
            addErrResponse(getLang(Lang.err_item_lock_in_bag));
            return;
        }
        int oldBonusBag = EquipSlotBonus.bagBonus(mUser);
        int oldBonusMat = EquipSlotBonus.materialBonus(mUser);
        boolean wasEquipped = equip.isEquip() || mUser.getUser().getListIdEquipmentEquip().contains((int) id);
        if (wasEquipped) {
            if (!clearItemFromEquipList((int) id, equip)) {
                addErrResponse();
                return;
            }
            equip.unEquip();
        }
        Bonus.clearItemFromSlot(mUser, Bonus.BONUS_EQUIPMENT, id);
        if (equip.deleteFromDb()) {
            mUser.getResources().removeEquipment(id);
            List<Long> bonus = CfgItem.getPriceSellItem(equip);
            addBonusToast(Bonus.receiveListItem(mUser, DetailActionType.SELL_ITEM.getKey(equip.getItemId()), bonus));
            if (wasEquipped) {
                mUser.getUData().syncSlotsAfterEquipmentChange(mUser, oldBonusBag, oldBonusMat);
                Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
                pb.addAVector(getCommonVector(id, 1L));
                pb.addAVector(user.reCalculatePoint(mUser).toCommonVector());
                pb.addAVector(getCommonIntVector(mUser.getUser().normalizeItemEquipList()));
                addResponse(pb.build());
                mUser.reCalculatePoint();
                broadcastItemEquipUpdate();
                UserHandler.buffInfo(mUser);
            } else {
                addResponse(getCommonVector(id, 1L));
            }
        } else {
            addErrResponse();
        }
    }

    private boolean clearItemFromEquipList(int itemId, UserEquipmentEntity item) {
        List<Integer> lst = mUser.getUser().normalizeItemEquipList();
        int slotIndex = UserEntity.findEquipSlotByItemId(lst, itemId);
        if (slotIndex < 0) {
            ResItemEquipmentEntity resEquip = item.getResEquipment();
            if (resEquip == null) return false;
            slotIndex = mUser.getUser().equipSlotIndex(resEquip.getType());
            if (slotIndex < 0 || lst.get(slotIndex) != itemId) return false;
        }
        lst.set(slotIndex, 0);
        lst.set(slotIndex + 1, 0);
        lst.set(slotIndex + 2, 0);
        return mUser.getUser().updateItemEquip(lst);
    }

    void usedItem() {
        List<Long> aLong = CommonProto.parseCommonVector(requestData).getALongList();
        long id = aLong.get(0);
        int number = aLong.get(1).intValue();
        int type = aLong.get(2).intValue();
        if (number < 0 || number > 100) {
            addErrParam();
            return;
        }
        UserItemEntity item = mUser.getResources().getItem(id);
        if (item == null) {
            addErrResponse(getLang(Lang.item_not_own));
            return;
        }
        ResItemEntity resItem = item.getRes();
        switch (resItem.getItemType()) {
//            case ITEM_OPEN: {
//                // nếu >100 thì random 1 số rồi x10 + phần random lẻ,vd: 86 -> random 8 lần x10 + 6 lần lẻ
////                if (number > 100) {
////                    int nguyen = number / 10;
////                    for (int i = 0; i < nguyen; i++) {
////                        fee.addAll(Bonus.xBonus(BonusConfig.getRandomOneBonus(resItem.getItemOpen()), 10));
////                    }
////                    int du = number % 10;
////                    for (int i = 0; i < du; i++) {
////                        fee.addAll(BonusConfig.getRandomOneBonus(resItem.getItemOpen()));
////                    }
////                } else {
//                    for (int i = 0; i < number; i++) {
//                        fee.addAll(BonusConfig.getRandomOneBonus(resItem.getItemOpen()));
//                    }
////                }
//                addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.SU_DUNG_ITEM.getKey(id), Bonus.merge(fee))));
//            }
            //break;
//            case ITEM_USE:
//                if (mUser.getPlayer() == null || !mUser.getPlayer().isAlive()) return;
//                List<Long> aBonus = Bonus.receiveListItem(mUser, DetailActionType.SU_DUNG_ITEM.getKey(id), fee);
//                if (aBonus.isEmpty()) {
//                    addErrResponse();
//                    return;
//                }
//                List<PointBuff> buffs = item.getRes().getBuffs();
//                mUser.getPlayer().protoBuffPoint(buffs);
//                addResponse(getCommonVector(aBonus));
//                break;
            case POSITION:
                switch (Pbmethod.ItemKey.valueOf(item.getItemId())) {
//                    case THE_HOAN_TRA_1 -> {
//                        if (mUser.getUData().resetLevelStat(mUser)) {
//                            addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.SU_DUNG_ITEM.getKey(id), Bonus.merge(fee))));
//                        } else {
//                            addErrSystem();
//                            Bonus.receiveListItem(mUser, DetailActionType.SU_DUNG_ITEM.getKey(id), Bonus.reverseBonus(fee));
//                            return;
//                        }
//                    }
//                    case THE_HOAN_TRA_2 -> {
//                        List<Long> bonus = mUser.getUData().resetGoldStat(mUser);
//                        if (bonus != null) {
//                            bonus.addAll(Bonus.merge(fee));
//                            addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.SU_DUNG_ITEM.getKey(id), bonus)));
//                        } else {
//                            addErrSystem();
//                            Bonus.receiveListItem(mUser, DetailActionType.SU_DUNG_ITEM.getKey(id), Bonus.reverseBonus(fee));
//                            return;
//                        }
//                    }
                    default -> {
                        addErrParam();
                    }
                }
                break;
//            case ITEM_OPEN_STATIC: {
//                List<Long> bonus = GsonUtil.strToListLong(resItem.getData());
//                bonus = Bonus.xBonus(bonus, number);
//                fee.addAll(bonus);
//                addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.SU_DUNG_ITEM.getKey(id), fee)));
//            }
            //break;
        }
    }

    private void uplevel() {
        long id = CommonProto.parseCommonVector(requestData).getALong(0);
        UserEquipmentEntity equip = mUser.getResources().getEquipment(id);
        if (equip != null) {
            uplevelEquipment(equip);
            return;
        }
        UserPetEntity pet = mUser.getResources().getPet(id);
        if (pet != null) {
            uplevelPet(pet);
            return;
        }
        UserMountEntity mount = mUser.getResources().getMount(id);
        if (mount != null) {
            uplevelMount(mount);
            return;
        }
        UserItemEntity item = mUser.getResources().getItem(id);
        if (item == null) {
            addErrResponse(getLang(Lang.item_not_own));
            return;
        }
        if (!CfgItem.canUpLevel(item)) {
            addErrResponse(getLang(Lang.err_item_equip_max_level));
            return;
        }
        List<Long> fee = CfgItem.getUpgradeFee(item);
        if (fee.isEmpty()) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        List<Long> paid = Bonus.receiveListItem(mUser, DetailActionType.NANG_CAP_VAT_PHAM.getKey(item.getItemId()), fee);
        if (paid.isEmpty()) {
            addErrResponse();
            return;
        }
        int newLevel = item.getLevel() + 1;
        if (CfgItem.isItemMedicine(item.getItemId()) && !ResItem.hasUpgradePointData(item)) {
            Bonus.receiveListItem(mUser, DetailActionType.UPDATE_FAIL.getKey(), Bonus.reverseBonus(fee));
            addErrResponse();
            return;
        }
        if (!item.update(List.of("level", newLevel))) {
            Bonus.receiveListItem(mUser, DetailActionType.UPDATE_FAIL.getKey(), Bonus.reverseBonus(fee));
            addErrResponse();
            return;
        }
        item.setLevel(newLevel);
        addBonusToast(paid);
        addResponse(getCommonVector(id, (long) newLevel));
    }

    private void uplevelEquipment(UserEquipmentEntity equip) {
        long id = equip.getId();
        if (!CfgItem.canUpLevel(equip)) {
            addErrResponse(getLang(Lang.err_item_equip_max_level));
            return;
        }
        if (equip.getLockDestroy() == 1) {
            addErrResponse(getLang(Lang.err_item_lock_in_bag));
            return;
        }
        List<Long> fee = CfgItem.getUpgradeFee(equip);
        if (fee.isEmpty()) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        List<Long> paid = Bonus.receiveListItem(mUser, DetailActionType.NANG_CAP_VAT_PHAM.getKey(equip.getItemId()), fee);
        if (paid.isEmpty()) {
            addErrResponse();
            return;
        }
        int newLevel = equip.getLevel() + 1;
        if (!equip.hasUpgradePointData()) {
            Bonus.receiveListItem(mUser, DetailActionType.UPDATE_FAIL.getKey(), Bonus.reverseBonus(fee));
            addErrResponse();
            return;
        }
        if (!equip.update(List.of("level", newLevel))) {
            Bonus.receiveListItem(mUser, DetailActionType.UPDATE_FAIL.getKey(), Bonus.reverseBonus(fee));
            addErrResponse();
            return;
        }
        boolean syncEquip = equip.isEquip() || mUser.getUser().getListIdEquipmentEquip().contains((int) id);
        int oldBonusBag = syncEquip ? EquipSlotBonus.bagBonus(mUser) : 0;
        int oldBonusMat = syncEquip ? EquipSlotBonus.materialBonus(mUser) : 0;
        equip.setLevel(newLevel);
        if (syncEquip) {
            if (!updateEquipSlotLevel((int) id, newLevel)) {
                Bonus.receiveListItem(mUser, DetailActionType.UPDATE_FAIL.getKey(), Bonus.reverseBonus(fee));
                equip.update(List.of("level", newLevel - 1));
                equip.setLevel(newLevel - 1);
                addErrResponse();
                return;
            }
            equip.setEquip(true);
        }
        addBonusToast(paid);
        if (syncEquip) {
            mUser.getUData().syncSlotsAfterEquipmentChange(mUser, oldBonusBag, oldBonusMat);
            Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
            pb.addAVector(getCommonVector(id, (long) newLevel));
            pb.addAVector(user.reCalculatePoint(mUser).toCommonVector());
            pb.addAVector(getCommonIntVector(mUser.getUser().normalizeItemEquipList()));
            addResponse(pb.build());
            mUser.reCalculatePoint();
            broadcastItemEquipUpdate();
            UserHandler.buffInfo(mUser);
        } else {
            addResponse(getCommonVector(id, (long) newLevel));
        }
    }

    private boolean updateEquipSlotLevel(int itemId, int newLevel) {
        List<Integer> lst = mUser.getUser().normalizeItemEquipList();
        int slotIndex = UserEntity.findEquipSlotByItemId(lst, itemId);
        if (slotIndex < 0) return false;
        lst.set(slotIndex + 2, newLevel);
        return mUser.getUser().updateItemEquip(lst);
    }

    private void uplevelPet(UserPetEntity pet) {
        long id = pet.getId();
        if (!CfgItem.canUpLevel(pet)) {
            addErrResponse(getLang(Lang.err_item_equip_max_level));
            return;
        }
        List<Long> fee = CfgItem.getUpgradeFee(pet);
        if (fee.isEmpty()) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        List<Long> paid = Bonus.receiveListItem(mUser, DetailActionType.NANG_CAP_VAT_PHAM.getKey(pet.getPetId()), fee);
        if (paid.isEmpty()) {
            addErrResponse();
            return;
        }
        int newLevel = pet.getLevel() + 1;
        if (!pet.update(List.of("level", newLevel))) {
            Bonus.receiveListItem(mUser, DetailActionType.UPDATE_FAIL.getKey(), Bonus.reverseBonus(fee));
            addErrResponse();
            return;
        }
        boolean syncEquip = UserPetEntity.isEquipped(mUser, id);
        pet.setLevel(newLevel);
        if (syncEquip && !updateEquipSlotLevel((int) id, newLevel)) {
            Bonus.receiveListItem(mUser, DetailActionType.UPDATE_FAIL.getKey(), Bonus.reverseBonus(fee));
            pet.update(List.of("level", newLevel - 1));
            pet.setLevel(newLevel - 1);
            addErrResponse();
            return;
        }
        addBonusToast(paid);
        if (syncEquip) {
            Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
            pb.addAVector(getCommonVector(id, (long) newLevel));
            pb.addAVector(user.reCalculatePoint(mUser).toCommonVector());
            pb.addAVector(getCommonIntVector(mUser.getUser().normalizeItemEquipList()));
            addResponse(pb.build());
            mUser.reCalculatePoint();
            UserHandler.buffInfo(mUser);
        } else {
            addResponse(getCommonVector(id, (long) newLevel));
        }
        addResponse(IAction.PET_INFO, Pbmethod.PbListPet.newBuilder().addPets(pet.toProto()).build());
    }

    private void uplevelMount(UserMountEntity mount) {
        long id = mount.getId();
        if (!CfgItem.canUpLevel(mount)) {
            addErrResponse(getLang(Lang.err_item_equip_max_level));
            return;
        }
        List<Long> fee = CfgItem.getUpgradeFee(mount);
        if (fee.isEmpty()) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        List<Long> paid = Bonus.receiveListItem(mUser, DetailActionType.NANG_CAP_VAT_PHAM.getKey(mount.getMountId()), fee);
        if (paid.isEmpty()) {
            addErrResponse();
            return;
        }
        int newLevel = mount.getLevel() + 1;
        if (!mount.update(List.of("level", newLevel))) {
            Bonus.receiveListItem(mUser, DetailActionType.UPDATE_FAIL.getKey(), Bonus.reverseBonus(fee));
            addErrResponse();
            return;
        }
        boolean syncEquip = UserMountEntity.isEquipped(mUser, id);
        mount.setLevel(newLevel);
        if (syncEquip && !updateEquipSlotLevel((int) id, newLevel)) {
            Bonus.receiveListItem(mUser, DetailActionType.UPDATE_FAIL.getKey(), Bonus.reverseBonus(fee));
            mount.update(List.of("level", newLevel - 1));
            mount.setLevel(newLevel - 1);
            addErrResponse();
            return;
        }
        addBonusToast(paid);
        if (syncEquip) {
            Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
            pb.addAVector(getCommonVector(id, (long) newLevel));
            pb.addAVector(user.reCalculatePoint(mUser).toCommonVector());
            pb.addAVector(getCommonIntVector(mUser.getUser().normalizeItemEquipList()));
            addResponse(pb.build());
            mUser.reCalculatePoint();
            UserHandler.buffInfo(mUser);
        } else {
            addResponse(getCommonVector(id, (long) newLevel));
        }
        addResponse(IAction.MOUNT_INFO, Pbmethod.PbListMount.newBuilder().addMounts(mount.toProto()).build());
    }

    private void itemInfo() {
        int pointId = getInputInt();
        if (!game.config.aEnum.ItemPointKey.isLotteryTicket(pointId)) {
            addErrResponse();
            return;
        }
        UserItemPointEntity row = mUser.getResources().getItemPoint(pointId);
        if (row == null) {
            addErrResponse();
            return;
        }
        List<Long> nums = row.getTicketNumbersForEvent(ozudo.base.helper.DateTime.getNumberDay());
        List<Integer> info = new ArrayList<>();
        for (Long n : nums)
            info.add(n.intValue());
        addResponse(getCommonIntVector(info));
    }

    private void lockDestroy() {
        List<Long> inputs = getInputALong();
        long id = inputs.get(0);
        UserEquipmentEntity itemEquipment = mUser.getResources().getEquipment(id);
        if (itemEquipment == null) {
            addErrResponse(getLang(Lang.item_not_own));
            return;
        }
        int status = inputs.get(1).intValue();
        if (status != 0 && status != 1) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        if (itemEquipment.getLockDestroy() != status && itemEquipment.update(Arrays.asList("lock_destroy", status))) {
            itemEquipment.setLockDestroy(status);
        }
        addResponse(getCommonVector(inputs));
    }



    void speakerSend() {
        String text = getInputString();
        if (StringHelper.isEmpty(text)) {
            addErrParam();
            return;
        }
        text = text.trim();
        if (text.length() > SPEAKER_MAX_LEN) {
            addErrParam();
            return;
        }
        if (text.contains("<") || text.contains(">") || text.contains("[") || text.contains("]")) {
            addErrResponse(getLang(Lang.err_string_prefix));
            return;
        }
        int pointId = game.config.aEnum.ItemPointKey.LOA_THE_GIOI.id;
        if (mUser.getResources().getItemPointNumber(pointId) < 1) {
            addErrResponse(getLang(Lang.item_not_own));
            return;
        }
        List<Long> fee = Bonus.viewItemPoint(pointId, -1);
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        List<Long> bonus = Bonus.receiveListItem(mUser, DetailActionType.SU_DUNG_LOA_THE_GIOI.getKey(), fee);
        if (bonus.isEmpty()) {
            addErrResponse();
            return;
        }
        String filtered = CfgChat.replaceInvalidWord(text);
        String msg = "[" + user.getName() + "]: " + filtered;
        List<Channel> channels = Online.getUserInServer(user.getServer());
        Util.sendSliderChat(channels, msg);
        addBonusPrivate(bonus);
        addResponseSuccess();
    }


}

