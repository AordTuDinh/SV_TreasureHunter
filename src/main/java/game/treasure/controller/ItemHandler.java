package game.treasure.controller;

import game.config.CfgChat;
import game.config.CfgItem;
import game.config.CfgMaterial;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.treasure.mapping.UserEntity;
import game.treasure.mapping.UserEquipmentEntity;
import game.treasure.mapping.UserItemEntity;
import game.treasure.mapping.UserMaterialEntity;
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
        finishEquipChange(slotUpdates);
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
        finishEquipChange(slotUpdates);
    }

    private void finishEquipChange(List<UserEquipmentEntity> slotUpdates) {
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
        int bonusType = req.get(0).intValue();
        long id = req.get(1);
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
        addErrParam();
    }

    private void sellEquipment(UserEquipmentEntity equip) {
        long id = equip.getId();
        if (equip.getLockDestroy() == 1) {
            addErrResponse(getLang(Lang.err_item_lock_in_bag));
            return;
        }
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
        List<Long> fee = item.viewBonusItem(-number);
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
        UserEquipmentEntity equip = mUser.getResources().getEquipment(id);
        if (equip != null) {
            uplevelEquipment(equip);
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
        equip.setLevel(newLevel);
        boolean syncEquip = equip.isEquip() || mUser.getUser().getListIdEquipmentEquip().contains((int) id);
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
        int itemKey = Pbmethod.ItemKey.LOA_THE_GIOI.getNumber();
        if (mUser.getResources().getItemByItemKey(itemKey) == null) {
            addErrResponse(getLang(Lang.item_not_own));
            return;
        }
        List<Long> fee = Bonus.viewItem( itemKey, -1);
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
    }


}

