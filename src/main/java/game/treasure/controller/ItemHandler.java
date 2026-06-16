package game.treasure.controller;

import game.config.CfgItem;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.treasure.mapping.UserEntity;
import game.treasure.mapping.UserItemEntity;
import game.treasure.mapping.main.ResItemEntity;
import game.treasure.mapping.main.ResItemEquipmentEntity;
import game.treasure.server.IAction;
import game.treasure.service.resource.ResItem;
import game.treasure.service.user.Bonus;
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


public class ItemHandler extends AHandler {
    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(ITEM_EQUIPMENT_INFO, ITEM_EQUIPMENT_LOCK_DESTROY, ITEM_UP_LEVEL,  ITEM_EQUIPMENT_VIEW_INFO, ITEM_INFO, ITEM_SELL, ITEM_EQUIPMENT_UN_EQUIP, ITEM_USED, ITEM_EQUIPMENT_EQUIP);
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
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }


    public static void listEquipment(List<Long> ids, AHandler handler, MyUser mUser) {
        Pbmethod.PbListItem.Builder lst = Pbmethod.PbListItem.newBuilder();
        System.out.println("ids = " + ids);
        for (int i = 0; i < ids.size(); i++) {
            UserItemEntity itemEquipment = mUser.getResources().getItemEquipment(ids.get(i));
            if (itemEquipment == null) {
                handler.addErrResponse(getLang(mUser, Lang.err_has_item));
                return;
            }
            lst.addItem(itemEquipment.toProto());
        }
        handler.addResponse(IAction.ITEM_EQUIPMENT_INFO, lst.build());
    }

    void viewInfoEquipment() {
        try {
            List<Long> ids = CommonProto.parseCommonVector(requestData).getALongList();
            int userId = ids.get(0).intValue();
            List<Long> item = ids.subList(1, ids.size());
            MyUser mUser = Online.getMUser(userId);
            Pbmethod.PbListItem.Builder lst = Pbmethod.PbListItem.newBuilder();
            if (mUser != null) { // có online

                for (int i = 0; i < item.size(); i++) {
                    UserItemEntity itemEquipment = mUser.getResources().getItemEquipment(item.get(i));
                    if (itemEquipment != null) lst.addItem(itemEquipment.toProto());
                }

            } else {
                String sql = "Select * from user_item where type=2 and id in(" + NumberUtil.joiningListLong(item) + ")";
                List<UserItemEntity> lstUE = DBJPA.getSelectQuery(sql, UserItemEntity.class);
                for (int i = 0; i < lstUE.size(); i++) {
                    lst.addItem(lstUE.get(i).toProto());
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
        UserItemEntity iEquip = mUser.getResources().getItemEquipment(itemId);
        if (iEquip == null) {
            addErrResponse(getLang(Lang.item_not_own));
            return;
        }
        if (iEquip.isEquip() || mUser.getUser().getListIdEquipmentEquip().contains(itemId)) {
            addErrResponse(getLang(Lang.err_use_item_equip));
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
        List<UserItemEntity> slotUpdates = new ArrayList<>();
        List<Integer> lst = mUser.getUser().normalizeItemEquipList();
        int oldId = lst.get(slotIndex);
        UserItemEntity oldEquip = oldId > 0 && oldId != itemId
                ? mUser.getResources().getItemEquipment(oldId) : null;
        if (oldId > 0 && oldId != itemId && oldEquip == null) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        int savedBagSlot = iEquip.getSlot();
        if (!moveItemOutOfBag(iEquip)) {
            addErrResponse();
            return;
        }
        slotUpdates.add(iEquip);
        if (oldEquip != null) {
            if (!moveItemToBag(oldEquip)) {
                if (savedBagSlot >= 0) iEquip.updateSlot(savedBagSlot);
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
        finishEquipChange(slotUpdates);
    }

    void unEquipItem() {
        int id = (int) CommonProto.parseCommonVector(requestData).getALong(0);
        UserItemEntity iEquip = mUser.getResources().getItemEquipment(id);
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
        List<Integer> equipBackup = new ArrayList<>(lst);
        lst.set(slotIndex, 0);
        lst.set(slotIndex + 1, 0);
        lst.set(slotIndex + 2, 0);
        if (!mUser.getUser().updateItemEquip(lst)) {
            addErrResponse();
            return;
        }
        List<UserItemEntity> slotUpdates = new ArrayList<>();
        if (!moveItemToBag(iEquip)) {
            mUser.getUser().updateItemEquip(equipBackup);
            addErrResponse(getLang(Lang.err_max_slot));
            return;
        }
        iEquip.unEquip();
        slotUpdates.add(iEquip);
        finishEquipChange(slotUpdates);
    }

    private boolean moveItemOutOfBag(UserItemEntity item) {
        if (item.getSlot() < 0) return true;
        return item.updateSlot(-1);
    }

    private boolean moveItemToBag(UserItemEntity item) {
        if (!mUser.getResources().canAddBagItem(1)) return false;
        Integer bagSlot = mUser.getResources().allocBagSlot();
        if (bagSlot == null) return false;
        return item.updateSlot(bagSlot);
    }

    private void finishEquipChange(List<UserItemEntity> slotUpdates) {
        Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
        pb.addAVector(user.reCalculatePoint(mUser).toCommonVector());
        pb.addAVector(getCommonIntVector(mUser.getUser().normalizeItemEquipList()));
        if (slotUpdates != null && !slotUpdates.isEmpty()) {
            List<Integer> slotData = new ArrayList<>();
            for (UserItemEntity item : slotUpdates) {
                slotData.add((int) item.getId());
                slotData.add(item.getSlot());
            }
            pb.addAVector(getCommonIntVector(slotData));
        }
        addResponse(pb.build());
        broadcastItemEquipUpdate();
        UserHandler.buffInfo(mUser);
    }

    private void broadcastItemEquipUpdate() {
        if (mUser.getPlayer() == null) return;
        mUser.getPlayer().protoStatus(Pbmethod.SubStateType.UPDATE_ITEM_EQUIP, mUser.getUser().getListItemKeyEquipLong());
    }


    void sellItem() {
        long id = CommonProto.parseCommonVector(requestData).getALong(0);
        UserItemEntity item = mUser.getResources().getItem(id);
        if (item == null) {
            addErrResponse(getLang(Lang.item_not_own));
            return;
        }
        if (item.isEquipment() && item.getLockDestroy() == 1) {
            addErrResponse(getLang(Lang.err_item_lock_in_bag));
            return;
        }
        boolean wasEquipped = false;
        if (item.isEquipment() && (item.isEquip() || mUser.getUser().getListIdEquipmentEquip().contains((int) id))) {
            wasEquipped = true;
            if (!clearItemFromEquipList((int) id, item)) {
                addErrResponse();
                return;
            }
            item.unEquip();
        }
        if (item.deleteFromDb()) {
            mUser.getResources().removeItem(id);
            List<Long> bonus = CfgItem.getPriceSellItem(item);
            addBonusToast(Bonus.receiveListItem(mUser, DetailActionType.SELL_ITEM.getKey(item.getItemId()), bonus));
            if (wasEquipped) {
                Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
                pb.addAVector(getCommonVector(id, 1L));
                pb.addAVector(user.reCalculatePoint(mUser).toCommonVector());
                pb.addAVector(getCommonIntVector(mUser.getUser().normalizeItemEquipList()));
                addResponse(pb.build());
                broadcastItemEquipUpdate();
                UserHandler.buffInfo(mUser);
            } else {
                addResponse(getCommonVector(id, 1L));
            }
        } else {
            addErrResponse();
        }
    }

    private boolean clearItemFromEquipList(int itemId, UserItemEntity item) {
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
        List<Long> fee = item.viewBonusItem(item.getRes().getItemType().value, -number);
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
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
            case CONSUMABLE:
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
                        if (BuffItemType.buffIds.contains(item.getItemId())) {
                            BuffItemType buffType = BuffItemType.get(item.getItemId());
                            if (buffType != null) {
                                List<Long> aBuffs = mUser.getUData().getBuff();
                                long curBuff = aBuffs.get(buffType.index);
                                if (curBuff < System.currentTimeMillis()) { // chưa có buff
                                    curBuff = System.currentTimeMillis() + DateTime.HOUR_MILLI_SECOND;
                                } else {// đang có sẵn buff
                                    curBuff += DateTime.HOUR_MILLI_SECOND;
                                }
                                aBuffs.set(buffType.index, curBuff);
                                mUser.addBuffs(aBuffs);
                                addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.SU_DUNG_ITEM.getKey(id), fee)));
                            } else addErrParam();
                        }
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
        UserItemEntity item = mUser.getResources().getItem(id);
        if (item == null) {
            addErrResponse(getLang(Lang.item_not_own));
            return;
        }
        if (!CfgItem.canUpLevel(item)) {
            addErrResponse(getLang(Lang.err_item_equip_max_level));
            return;
        }
        if (item.isEquipment() && item.getLockDestroy() == 1) {
            addErrResponse(getLang(Lang.err_item_lock_in_bag));
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
        if ((CfgItem.isItemMedicine(item.getItemId()) || item.isEquipment())
                && !ResItem.hasUpgradePointData(item)) {
            Bonus.receiveListItem(mUser, DetailActionType.UPDATE_FAIL.getKey(), Bonus.reverseBonus(fee));
            addErrResponse();
            return;
        }

        List<Object> updateFields = new ArrayList<>(Arrays.asList("level", newLevel));

        if (!item.update(updateFields)) {
            Bonus.receiveListItem(mUser, DetailActionType.UPDATE_FAIL.getKey(), Bonus.reverseBonus(fee));
            addErrResponse();
            return;
        }
        item.setLevel(newLevel);

        boolean syncEquip = item.isEquipment()
                && (item.isEquip() || mUser.getUser().getListIdEquipmentEquip().contains((int) id));
        if (syncEquip) {
            if (!updateEquipSlotLevel((int) id, newLevel)) {
                Bonus.receiveListItem(mUser, DetailActionType.UPDATE_FAIL.getKey(), Bonus.reverseBonus(fee));
                item.update(Arrays.asList("level", newLevel - 1));
                item.setLevel(newLevel - 1);
                addErrResponse();
                return;
            }
            item.setEquip(true);
        }

        addBonusToast(paid);
        if (syncEquip) {
            Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
            pb.addAVector(getCommonVector(id, (long) newLevel));
            pb.addAVector(user.reCalculatePoint(mUser).toCommonVector());
            pb.addAVector(getCommonIntVector(mUser.getUser().normalizeItemEquipList()));
            addResponse(pb.build());
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

    private void itemInfo() {
        long id = getInputLong();
        UserItemEntity item = mUser.getResources().getItem(id);
        if (item == null) {
            addErrResponse();
            return;
        }
        switch (item.getItemId()) {
            case Pbmethod.ItemKey.TICKER_SPECIAL_VALUE, Pbmethod.ItemKey.TICKER_NORMAL_VALUE -> {
                List<Integer> info = GsonUtil.strToListInt(item.getData());
                if (info.size() > 0) {
                    info.remove(0);
                    addResponse(getCommonIntVector(info));
                }
            }
            default -> addErrResponse();
        }
    }

    private void lockDestroy() {
        List<Long> inputs = getInputALong();
        long id = inputs.get(0);
        UserItemEntity itemEquipment = mUser.getResources().getItemEquipment(id);
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


}

