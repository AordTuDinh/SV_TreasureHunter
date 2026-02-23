package game.dragonhero.controller;

import game.battle.calculate.IMath;
import game.battle.type.StateType;
import game.config.CfgSmithy;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.dragonhero.mapping.UserHeroEntity;
import game.dragonhero.mapping.UserItemEntity;
import game.dragonhero.mapping.UserItemEquipmentEntity;
import game.dragonhero.mapping.main.ResItemEntity;
import game.dragonhero.mapping.main.ResItemEquipmentEntity;
import game.dragonhero.server.IAction;
import game.dragonhero.service.resource.ResItem;
import game.dragonhero.service.resource.ResPointEquipment;
import game.dragonhero.service.user.Bonus;
import game.monitor.Online;
import game.object.BonusConfig;
import game.object.MyUser;
import game.object.PointBuff;
import game.protocol.CommonProto;
import io.netty.channel.Channel;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.*;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class ItemHandler extends AHandler {
    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(ITEM_EQUIPMENT_INFO, ITEM_EQUIPMENT_LOCK_DESTROY, ITEM_EQUIPMENT_SELECT_ACCESSORY, ITEM_EQUIPMENT_UPGRADE_ACCESSORY, ITEM_USE_FOR_ITEM, ITEM_EQUIPMENT_VIEW_INFO, ITEM_INFO, ITEM_REMOVE, ITEM_EQUIPMENT_UN_EQUIP, ITEM_USED, ITEM_EQUIPMENT_EQUIP);
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
                case IAction.ITEM_REMOVE -> removeItem();
                case IAction.ITEM_USED -> usedItem();
                case IAction.ITEM_USE_FOR_ITEM -> usedItemForItem();
                case IAction.ITEM_INFO -> itemInfo();
                case IAction.ITEM_EQUIPMENT_LOCK_DESTROY -> lockDestroy();
                case IAction.ITEM_EQUIPMENT_SELECT_ACCESSORY -> selectAccessory();
                case IAction.ITEM_EQUIPMENT_UPGRADE_ACCESSORY -> upgradeAccessory();
                case IAction.ITEM_EQUIPMENT_VIEW_INFO -> viewInfoEquipment();
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }


    public static void listEquipment(List<Long> ids, AHandler handler, MyUser mUser) {
        Pbmethod.PbListItemEquipment.Builder lst = Pbmethod.PbListItemEquipment.newBuilder();
        for (int i = 0; i < ids.size(); i++) {
            UserItemEquipmentEntity itemEquipment = mUser.getResources().getItemEquipment(ids.get(i));
            if (itemEquipment == null) {
                handler.addErrResponse(getLang(mUser, Lang.err_has_item));
                return;
            }
            lst.addItemEquip(itemEquipment.toProto());
        }
        handler.addResponse(IAction.ITEM_EQUIPMENT_INFO, lst.build());
    }

    void viewInfoEquipment() {
        try {
            List<Long> ids = CommonProto.parseCommonVector(requestData).getALongList();
            int userId = ids.get(0).intValue();
            List<Long> item = ids.subList(1, ids.size());
            MyUser mUser = Online.getMUser(userId);
            Pbmethod.PbListItemEquipment.Builder lst = Pbmethod.PbListItemEquipment.newBuilder();
            if (mUser != null) { // có online

                for (int i = 0; i < item.size(); i++) {
                    UserItemEquipmentEntity itemEquipment = mUser.getResources().getItemEquipment(item.get(i));
                    if (itemEquipment != null) lst.addItemEquip(itemEquipment.toProto());
                }

            } else {
                String sql = "Select * from user_item_equipment where id in(" + NumberUtil.joiningListLong(item) + ")";
                List<UserItemEquipmentEntity> lstUE = DBJPA.getSelectQuery(sql, UserItemEquipmentEntity.class);
                for (int i = 0; i < lstUE.size(); i++) {
                    lst.addItemEquip(lstUE.get(i).toProto());
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
        int heroId = inputs.get(0).intValue();
        int itemId = inputs.get(1).intValue();
        UserItemEquipmentEntity iEquip = mUser.getResources().getItemEquipment(itemId);
        if (iEquip != null && iEquip.isEquip()) {
            addErrResponse(getLang(Lang.err_use_item_equip));
            return;
        }
        if (iEquip == null) {
            addErrResponse(getLang(Lang.item_not_own));
            return;
        }
        if (!iEquip.hasExpire()) {
            addErrResponse(getLang(Lang.err_item_equip_expire));
            return;
        }
        if (user.getLevel() < iEquip.getRes().getLevelRequire()) {
            addErrResponse(getLang(Lang.err_item_level));
            return;
        }
        UserHeroEntity heroMain = mUser.getResources().getHero(heroId);
        List<Integer> lst = heroMain.getItemEquipment();
        int slotIndex = (iEquip.getRes().getType().value - 1) * 3;
        // unlock old item
        int curItemId = lst.get(slotIndex);
        UserItemEquipmentEntity oldItem = mUser.getResources().getItemEquipment(curItemId);
        if (oldItem != null) oldItem.unEquip();
        // set new item
        lst.set(slotIndex, (int) iEquip.getId());
        lst.set(slotIndex + 1, iEquip.getItemId());
        lst.set(slotIndex + 2, iEquip.getLevel());
        // lock item
        if (!iEquip.isLock()) {
            if (iEquip.update(Arrays.asList("is_lock", 1))) {
                iEquip.setIsLock(1);
                addResponse(ITEM_EQUIPMENT_LOCK_STATUS, getCommonVector(iEquip.getId(), 1));
            } else {
                addErrResponse(getLang(Lang.err_system_down));
                return;
            }
        }
        mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.USE_ITEM_EQUIP, 1);
        // update info

        if (heroMain.updateItemEquip(lst)) {
            iEquip.equip(heroId);
            heroMain.setItemEquipment(StringHelper.toDBString(lst));
            Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
            if (heroId == mUser.getUser().getHeroMain()) {
                mUser.getUser().updateItemEquip(lst);
                pb.addAVector(user.reCalculatePoint(mUser).toCommonVector());
            } else {
                pb.addAVector(user.getCachePoint().toCommonVector());
            }
            pb.addAVector(getCommonIntVector(heroMain.getListIdEquipmentEquip()));
            addResponse(pb.build());
            // send update ui
            mUser.getPlayer().protoStatus(StateType.UPDATE_ITEM_EQUIP, GsonUtil.toListLong(mUser.toListIdDBItemEquip(heroMain)));
            UserHandler.buffInfo(mUser);
        } else addErrResponse();
    }

    void unEquipItem() {
        int id = (int) CommonProto.parseCommonVector(requestData).getALong(0);
        UserItemEquipmentEntity iEquip = mUser.getResources().getItemEquipment(id);
        UserHeroEntity heroMain = mUser.getResources().getHero(iEquip.getHeroIdEquip());
        if (heroMain == null || !iEquip.isEquip()) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        if (iEquip == null) {
            addErrResponse(getLang(Lang.item_not_own));
            return;
        }
        List<Integer> lst = heroMain.getItemEquipment();
        int slotIndex = (iEquip.getRes().getType().value - 1) * 3;
        lst.set(slotIndex, 0);
        lst.set(slotIndex + 1, 0);
        lst.set(slotIndex + 2, 0);
        // update info
        if (heroMain.updateItemEquip(lst)) {
            iEquip.unEquip();
            heroMain.setItemEquipment(lst.toString());
            Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
            if (heroMain.getHeroId() == mUser.getUser().getHeroMain()) {
                mUser.getUser().updateItemEquip(lst);
                pb.addAVector(user.reCalculatePoint(mUser).toCommonVector());
            } else {
                pb.addAVector(user.getCachePoint().toCommonVector());
            }
            pb.addAVector(getCommonIntVector(heroMain.getListIdEquipmentEquip()));
            addResponse(pb.build());
            // send update ui
            mUser.getPlayer().protoStatus(StateType.UPDATE_ITEM_EQUIP, GsonUtil.toListLong(mUser.toListIdDBItemEquip(heroMain)));
            UserHandler.buffInfo(mUser);
        } else addErrResponse();
    }


    void removeItem() {
        int id = (int) CommonProto.parseCommonVector(requestData).getALong(0);
        UserItemEntity item = mUser.getResources().getItem(id);
        if (item == null) {
            addErrResponse(getLang(Lang.item_not_own));
            return;
        }
        if (item.update(Arrays.asList("number", 0, "item_id", id))) {
            item.setNumber(0);
            addResponseSuccess();
        } else {
            addErrResponse();
        }
    }

    void usedItem() {
        List<Long> aLong = CommonProto.parseCommonVector(requestData).getALongList();
        int id = aLong.get(0).intValue();
        int number = aLong.get(1).intValue();
        if (number < 0 || number > 100) {
            addErrParam();
            return;
        }
        UserItemEntity item = mUser.getResources().getItem(id);
        if (item == null) {
            addErrResponse(getLang(Lang.item_not_own));
            return;
        }
        number = item.getType() == ItemType.ITEM_USE || item.getType() == ItemType.ITEM_USE_X1 ? 1 : number;
        List<Long> fee = item.viewBonus(-number);
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        ResItemEntity resItem = item.getRes();
        switch (resItem.getItemType()) {
            case ITEM_OPEN: {
                // nếu >100 thì random 1 số rồi x10 + phần random lẻ,vd: 86 -> random 8 lần x10 + 6 lần lẻ
//                if (number > 100) {
//                    int nguyen = number / 10;
//                    for (int i = 0; i < nguyen; i++) {
//                        fee.addAll(Bonus.xBonus(BonusConfig.getRandomOneBonus(resItem.getItemOpen()), 10));
//                    }
//                    int du = number % 10;
//                    for (int i = 0; i < du; i++) {
//                        fee.addAll(BonusConfig.getRandomOneBonus(resItem.getItemOpen()));
//                    }
//                } else {
                    for (int i = 0; i < number; i++) {
                        fee.addAll(BonusConfig.getRandomOneBonus(resItem.getItemOpen()));
                    }
//                }
                addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.SU_DUNG_ITEM.getKey(id), Bonus.merge(fee))));
            }
            break;
            case ITEM_USE:
                if (mUser.getPlayer() == null || !mUser.getPlayer().isAlive()) return;
                List<Long> aBonus = Bonus.receiveListItem(mUser, DetailActionType.SU_DUNG_ITEM.getKey(id), fee);
                if (aBonus.isEmpty()) {
                    addErrResponse();
                    return;
                }
                List<PointBuff> buffs = item.getRes().getBuffs();
                mUser.getPlayer().protoBuffPoint(buffs);
                addResponse(getCommonVector(aBonus));
                break;
            case ITEM_USE_X1:
                switch (ItemKey.get(item.getItemId())) {
                    case THE_HOAN_TRA_1 -> {
                        if (mUser.getUData().resetLevelStat(mUser)) {
                            addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.SU_DUNG_ITEM.getKey(id), Bonus.merge(fee))));
                        } else {
                            addErrSystem();
                            Bonus.receiveListItem(mUser, DetailActionType.SU_DUNG_ITEM.getKey(id), Bonus.reverseBonus(fee));
                            return;
                        }
                    }
                    case THE_HOAN_TRA_2 -> {
                        List<Long> bonus = mUser.getUData().resetGoldStat(mUser);
                        if (bonus != null) {
                            bonus.addAll(Bonus.merge(fee));
                            addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.SU_DUNG_ITEM.getKey(id), bonus)));
                        } else {
                            addErrSystem();
                            Bonus.receiveListItem(mUser, DetailActionType.SU_DUNG_ITEM.getKey(id), Bonus.reverseBonus(fee));
                            return;
                        }
                    }
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
            case ITEM_OPEN_STATIC: {
                List<Long> bonus = GsonUtil.strToListLong(resItem.getData());
                bonus = Bonus.xBonus(bonus, number);
                fee.addAll(bonus);
                addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.SU_DUNG_ITEM.getKey(id), fee)));
            }
            break;
        }
    }

    void usedItemForItem() {
        List<Long> aLong = CommonProto.parseCommonVector(requestData).getALongList();
        int idItem = aLong.get(0).intValue();
        long idEquip = aLong.get(1);
        UserItemEntity item = mUser.getResources().getItem(idItem);
        if (item == null) {
            addErrResponse(getLang(Lang.item_not_own));
            return;
        }
        List<Long> fee = item.viewBonus(-1);
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        UserItemEquipmentEntity uItemEquip = mUser.getResources().getItemEquipment(idEquip);
        if (uItemEquip == null) {
            addErrResponse(getLang(Lang.err_item_equip_not_found));
            return;
        }
        if (uItemEquip.isEquip()) {
            addErrResponse(getLang(Lang.err_item_equip_cant_up));
            return;
        }
        ResItemEntity resItem = item.getRes();
        switch (resItem.getItemType()) {
            case ITEM_USE_FOR_ITEM_1: {
                addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.SU_DUNG_ITEM.getKey(idItem), Bonus.merge(fee))));
            }
            case ITEM_USE_FOR_ITEM_5: {
                // todo regen item và hòa trả đá
                ResItemEquipmentEntity resItemEquip = uItemEquip.getRes();
                if (resItemEquip.getType() != EquipSlotType.WEAPON) {
                    addErrParam();
                    return;
                }// đá hoàn trả

                if (resItem.getId() == ItemKey.BUA_HOAN_TRA_VU_KHI.id) {
                    int idDaoGam = 39;
                    ResItemEquipmentEntity resDaoGam = ResItem.getItemEquipment(idDaoGam);
                    int stone = 0;
                    if (resItemEquip.getId() == idDaoGam) // dao găm
                    {
                        addErrResponse(getLang(Lang.err_no_forgot));
                        return;
                    }
                    stone += ((resItemEquip.getRank() - 1) * 15) + uItemEquip.getLevel();
                    if (stone > 0) {
                        fee.addAll(Bonus.viewItem(ItemKey.DA_TIEN_HOA_VU_KHI, stone));
                    }
                    String dataPoint = StringHelper.toDBString(ResPointEquipment.genItemEquipData(resDaoGam));
                    if (uItemEquip.updateNextItem(idDaoGam, 0, dataPoint)) {
                        uItemEquip.setLevel(0);
                        uItemEquip.setItemId(idDaoGam);
                        uItemEquip.setPoint(dataPoint);
                        listEquipment(List.of(uItemEquip.getId()), this, mUser);
                    } else {
                        addErrSystem();
                        return;
                    }
                }
                addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.SU_DUNG_ITEM.getKey(idItem), Bonus.merge(fee))));

            }
            break;
        }
    }

    private void itemInfo() {
        int id = getInputInt();
        UserItemEntity item = mUser.getResources().getItem(id);
        if (item == null) {
            addErrResponse();
            return;
        }
        switch (item.getType()) {
            case LOTTE_NORMAL, LOTTE_SPECIAL -> {
                List<Integer> info = GsonUtil.strToListInt(item.getData());
                if (info.size() > 0) {
                    info.remove(0);
                    addResponse(getCommonIntVector(info));
                }
            }
            default -> addErrResponse();
        }
    }

//    private void randItem(ResItemEntity resItem, List<Long> curBonus, int xNum) {
//        int rand = NumberUtil.getRandom(100);
//        for (int i = 0; i < resItem.getItemOpen().size(); i++) {
//            ItemData data = resItem.getItemOpen().get(i);
//            if (rand < data.getPer()) {
//                curBonus.addAll(data.getBonus(xNum));
//                return;
//            }
//        }
//    }

    private void lockDestroy() {
        List<Long> inputs = getInputALong();
        long id = inputs.get(0);
        UserItemEquipmentEntity itemEquipment = mUser.getResources().getItemEquipment(id);
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

    private void selectAccessory() {
        List<Long> inputs = getInputALong();
        UserItemEquipmentEntity uItem = mUser.getResources().getItemEquipment(inputs.get(0));
        if (uItem == null) {
            addErrResponse(getLang(Lang.err_item_equip_not_found));
            return;
        }
        if (uItem.getRes().getId() != 39) {
            addErrParam();
            return;
        }
        int idNext = inputs.get(1).intValue();
        if (!CfgSmithy.itemAccessoryUp.contains(idNext)) {
            addErrParam();
            return;
        }
        if (uItem.updateItemId(idNext)) {
            addResponse(getCommonVector(idNext, uItem.getLevel()));
        } else addErrSystem();
    }

    private void upgradeAccessory() {
        List<Long> inputs = getInputALong();
        UserItemEquipmentEntity uItem = mUser.getResources().getItemEquipment(inputs.get(0));
        if (uItem == null) {
            addErrResponse(getLang(Lang.err_item_equip_not_found));
            return;
        }
        ResItemEquipmentEntity resItem = uItem.getRes();
        // check max level
        if (uItem.getLevel() >= resItem.getMaxLevel()) {
            addErrResponse(getLang(Lang.err_max_level));
            return;
        }
        // check fee
        List<Long> fee = resItem.getFeeUpgradeAccessory();
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        List<Long> aBonus = Bonus.receiveListItem(mUser, DetailActionType.UPGRADE_WEAPON_ACCESSORY.getKey(uItem.getItemId()), fee);
        if (aBonus.isEmpty()) {
            addErrSystem();
            return;
        }
        // mở khóa tiến hóa
        int idItem = uItem.getItemId();
        int levelItem = uItem.getLevel() + 1;
        if (levelItem >= resItem.getMaxLevel() && resItem.getTarget() != null) {
            idItem = resItem.getNextId();
            levelItem = 0;
        }
        List<Long> point = IMath.mergePointWeapon(uItem.getPointLong(), resItem.getDataAccessory());
        if (uItem.updateNextItem(idItem, levelItem, StringHelper.toDBString(point))) {
            Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
            pb.addAVector(getCommonVector(aBonus));
            pb.addAVector(getCommonVector(idItem, levelItem));
            pb.addAVector(getCommonVector(point));
            addResponse(pb.build());
        } else {
            Bonus.receiveListItem(mUser, DetailActionType.UPGRADE_WEAPON_ACCESSORY.getKey(-uItem.getItemId()), Bonus.reverseBonus(fee));
            addErrSystem();
        }
    }
}

