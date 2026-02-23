package game.dragonhero.controller;

import game.config.CfgAchievement;
import game.config.CfgQuest;
import game.config.CfgSmithy;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.dragonhero.mapping.*;
import game.dragonhero.mapping.main.*;
import game.dragonhero.server.IAction;
import game.dragonhero.service.Services;
import game.dragonhero.service.resource.ResPointEquipment;
import game.dragonhero.service.resource.ResWeapon;
import game.dragonhero.service.user.Bonus;
import game.object.DataQuest;
import game.object.EquipmentPoint;
import game.protocol.CommonProto;
import io.netty.channel.Channel;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.NumberUtil;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SmithyHandler extends AHandler {
    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(SMITHY_STATUS, SMITHY_CREATE, SMITHY_DECAY, SMITHY_PIECE_STATUS, SMITHY_UPGRADE, SMITHY_MAKE_WEAPON, SMITHY_UP_LEVEL_WEAPON, SMITHY_COMBINE);
        actions.forEach(action -> mHandler.put(action, this));
    }

    @Override
    public AHandler newInstance() {
        return new SmithyHandler();
    }

    static SmithyHandler instance;

    public static SmithyHandler getInstance() {
        if (instance == null) {
            instance = new SmithyHandler();
        }
        return instance;
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        try {
            switch (actionId) {
                case IAction.SMITHY_STATUS -> status();
                case IAction.SMITHY_CREATE -> create();
                case IAction.SMITHY_DECAY -> decay();
                case IAction.SMITHY_UPGRADE -> upgradeEquip();
                case IAction.SMITHY_PIECE_STATUS -> pieceStatus();
                case IAction.SMITHY_MAKE_WEAPON -> makeWeapon();
                case IAction.SMITHY_UP_LEVEL_WEAPON -> upgradeWeapon();
                case IAction.SMITHY_COMBINE -> combine();
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    void status() {
        addResponse(getCommonIntVector(mUser.getUData().getBossGod()));
    }

    void create() {
        List<Long> inputs = CommonProto.parseCommonVector(requestData).getALongList();
        long itemId = inputs.get(0);
        int xu = inputs.size() < 2 ? 0 : Math.toIntExact(inputs.get(1));
        if (xu > CfgSmithy.maxXuInput) {
            addErrResponse(String.format(getLang(Lang.err_max_xu), CfgSmithy.maxXuInput));
            return;
        }
        if (xu < 0) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        // nguyên liệu
        UserItemEquipmentEntity userItemEquip = mUser.getResources().getItemEquipment(itemId);
//        if (userItemEquip.getHeroIdEquip() > 0) {
//            addErrResponse(getLang(Lang.err_item_equip_cant_up));
//            return;
//        }
        if (userItemEquip == null) {
            addErrResponse(getLang(Lang.err_has_item));
            return;
        }
        if (userItemEquip.getLockDestroy() == 1) {
            addErrResponse(getLang(Lang.err_item_lock_in_bag));
            return;
        }
        // nâng lên cấp này
        ResItemEquipmentEntity target = userItemEquip.getRes().getTarget();
        if (target == null) {
            addErrResponse(getLang(Lang.err_item_equip));
            return;
        }
        // check max level
        if (!userItemEquip.isMaxLevel()) {
            addErrResponse(getLang(Lang.err_item_equip_has_max_level));
            return;
        }
        // check cùng loại
        if (userItemEquip.getRes().getType() != target.getType()) {
            addErrResponse(getLang(Lang.err_item_equip_slot));
            return;
        }
        int mateRank = userItemEquip.getRes().getRank();
        // check fee
        List<Long> fee = CfgSmithy.getFeeCreate(mateRank);
        if (xu > 0) fee.addAll(Bonus.viewItem(ItemKey.LUCKY_COIN.id, -xu));
        String checkMoney = Bonus.checkMoney(mUser, fee);
        if (checkMoney != null) {
            addErrResponse(checkMoney);
            return;
        }
        EquipmentPoint mainPoint = userItemEquip.getMainPoint();
        ResPointEquipmentEntity resMain = ResPointEquipment.getPointEquip(mainPoint.id);
        if (resMain == null) {
            addErrResponse();
            return;
        }
        List<Long> bonus = Bonus.receiveListItem(mUser, DetailActionType.CHE_TAO_TRANG_BI.getKey(), fee);
        if (bonus.isEmpty()) {
            addErrResponse();
            return;
        }
        mainPoint = CfgSmithy.createItem(mainPoint, resMain, xu, mateRank);
        List<Integer> points = userItemEquip.getPoint();
        List<Integer> oldPoint = new ArrayList<>(points);
        points.set(2, mainPoint.addValue);
        if (userItemEquip.updateMainPoint(points.toString(), target.getId())) {
            Pbmethod.ListCommonVector.Builder lcm = Pbmethod.ListCommonVector.newBuilder();
            lcm.addAVector(getCommonVector(bonus));
            lcm.addAVector(getCommonIntVector(userItemEquip.getPoint()));
            lcm.addAVector(getCommonIntVector(oldPoint));
            addResponse(lcm.build());
            mUser.getUData().checkStatusTut(mUser, QuestTutType.HAS_ITEM_EQUIP_ID, userItemEquip.getRes().getId(), this);
        } else addErrResponse();
    }

    void decay() {
        List<Long> inputs = getInputALong();
        List<Long> bonus = new ArrayList<>();
        List<UserItemEquipmentEntity> items = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            UserItemEquipmentEntity uItem = mUser.getResources().getItemEquipment(inputs.get(i));
            if (uItem == null || uItem.getLockDestroy() == 1 || uItem.isEquip()) {
                continue;
            }
            items.add(uItem);
            bonus.addAll(CfgSmithy.bonusDecay.get(uItem.getRes().getRank()));
        }
        bonus = Bonus.receiveListItem(mUser, DetailActionType.PHA_HUY_TRANG_BI.getKey(), Bonus.merge(bonus));
        if (bonus.isEmpty()) {
            addErrResponse(getLang(Lang.err_system_down));
            return;
        }
        if (DBJPA.deleteIn("user_item_equipment", "id", new ArrayList<>(inputs))) {
            mUser.getResources().removeItemEquip(items);
            Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
            pb.addAVector(getCommonVector(bonus));
            pb.addAVector(getCommonVector(inputs));
            addResponse(pb.build());
        } else addErrResponse();
    }

    void upgradeEquip() {
        List<Long> inputs = CommonProto.parseCommonVector(requestData).getALongList();
        int itemId = inputs.get(0).intValue();
        long xu = inputs.get(1);
        if (xu > CfgSmithy.maxXuInput) {
            addErrResponse(String.format(getLang(Lang.err_max_xu), CfgSmithy.maxXuInput));
            return;
        }
        UserItemEquipmentEntity uItem = mUser.getResources().getItemEquipment(itemId);
        if (uItem == null || xu < 0) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        ResItemEquipmentEntity resItem = uItem.getRes();
        int rank = resItem.getRank();
        int level = uItem.getLevel();
        if (uItem.isMaxLevel()) {
            addErrResponse(getLang(Lang.err_item_equip_max_level));
            return;
        }
        List<Long> fee = CfgSmithy.getFeeUpgrade(resItem.getType(), rank, level);
        if (xu > 0) fee.addAll(Bonus.viewItem(ItemKey.LUCKY_COIN.id, -xu));
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        Pbmethod.ListCommonVector.Builder lcm = Pbmethod.ListCommonVector.newBuilder();
        List<Long> bonus = Bonus.receiveListItem(mUser, DetailActionType.NANG_CAP_TRANG_BI.getKey(), fee);
        lcm.addAVector(getCommonVector(bonus));
        // check tutorial
        UserDataEntity uData = mUser.getUData();
        boolean isFirst = uData.getUInt().getValue(UserInt.FIRST_UPGRADE_ITEM_EQUIP) == 0;
        boolean isUp = isFirst ? true : CfgSmithy.isUpgrade(uItem, rank, level, (int) xu);
        List<Integer> oldPoint = uItem.getPoint();
        if (isUp) {
            List<Integer> point = uItem.upgradePointUpLevel(resItem.getType(), rank);
            if (point == null) {
                addErrResponse(getLang(Lang.err_params));
                return;
            }
            uItem.updateUpLevel(point);
            lcm.addAVector(getCommonVector(uItem.getBless()));
            lcm.addAVector(getCommonIntVector(uItem.getPoint()));
            lcm.addAVector(getCommonIntVector(oldPoint));
            if (resItem.getId() <= 38 && uItem.isMaxLevel()) {
                CfgAchievement.addAchievement(mUser, 4, resItem.getId() + 30, 1);
            }
            if (isFirst) uData.getUInt().addOneAndUpdate(UserInt.FIRST_UPGRADE_ITEM_EQUIP);
            mUser.getUData().checkStatusTut(mUser, QuestTutType.HAS_ITEM_EQUIP_LEVEL, resItem.getId(), this);
        } else {
            uItem.addBless();
            lcm.addAVector(getCommonVector(uItem.getBless()));
        }
        addResponse(lcm.build());
        mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.UPGRADE_ITEM_EQUIP, 1);

        switch (resItem.getType()) {
            case HAT -> CfgQuest.addNumQuest(mUser, DataQuest.UPGRADE_HAT, 1);
            case GLOVES -> CfgQuest.addNumQuest(mUser, DataQuest.UPGRADE_GLOVES, 1);
            case ARMOR -> CfgQuest.addNumQuest(mUser, DataQuest.UPGRADE_ARMOR, 1);
            case SHOES -> CfgQuest.addNumQuest(mUser, DataQuest.UPGRADE_SHOES, 1);
        }

    }

    void pieceStatus() {
        UserInt uInt = mUser.getUData().getUInt();
        Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
        Pbmethod.CommonVector.Builder cmm = Pbmethod.CommonVector.newBuilder();
        Pbmethod.CommonVector.Builder cmm2 = Pbmethod.CommonVector.newBuilder();
        for (int i = 2; i <= 6; i++) {
            cmm.addALong(CfgSmithy.getDataByRank(uInt, i));
            cmm.addALong(CfgSmithy.numPhuocLanhCheTao.get(i));
            if (mUser.getResources().getMWeaponByRank().containsKey(i)) {
                cmm2.addALong(mUser.getResources().getMWeaponByRank().get(i));
            } else cmm2.addALong(0);
        }
        pb.addAVector(cmm);
        pb.addAVector(cmm2);
        addResponse(IAction.SMITHY_PIECE_STATUS, pb.build());
    }

    void makeWeapon() {
        List<Long> input = getInputALong();
        int type = input.get(0).intValue();
        PieceType pieceType = PieceType.get(input.get(1).intValue());
        if (pieceType == null) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        int id = input.get(2).intValue();
        int xu = input.get(3).intValue();
        int number = input.get(4).intValue();
        if (xu > CfgSmithy.maxXuInput * number) {
            addErrResponse(String.format(getLang(Lang.err_max_xu), CfgSmithy.maxXuInput));
            return;
        }

        UserPieceEntity uPiece = mUser.getResources().getPiece(pieceType, id);
        if (uPiece == null || number <= 0) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        ResPieceEntity resPiece = uPiece.getRes();
        if (number <= 0 || resPiece.getRank() < 1 || (type == 0 && resPiece.getRank() > 6) || (type == 1 && resPiece.getRank() > 5)) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }

        if (type == 0) { // Chế tạo vũ khí
            List<Long> bonus = Bonus.viewGold(-CfgSmithy.config.dataMakeWeapon.gold.get(resPiece.getRank() - 1) * number);
            if (xu > 0) bonus.addAll(Bonus.viewItem(ItemKey.LUCKY_COIN, -xu));
            bonus.addAll(Bonus.viewPiece(pieceType.value, id, -CfgSmithy.config.dataMakeWeapon.item.get(resPiece.getRank() - 1) * number));
            String err = Bonus.checkMoney(mUser, bonus);
            if (err != null) {
                addErrResponse(err);
                return;
            }
            // tutorial lần đầu chế tạo vũ khí
            if (resPiece.getRank() == RankType.ADVANCED.value && mUser.getUData().getUInt().getValue(UserInt.FIRST_CREATE_WEAPON_RANK_2) == 0) {
                if (mUser.getUData().getUInt().setValueAndUpdate(UserInt.FIRST_CREATE_WEAPON_RANK_2, 1)) {
                    bonus.addAll(Bonus.viewWeapon(CfgSmithy.FIRST_WEAPON_RANK_2));
                    List<Long> status = Bonus.receiveListItem(mUser, DetailActionType.CREATE_WEAPON.getKey(CfgSmithy.FIRST_WEAPON_RANK_2), Bonus.merge(bonus));
                    status.add(0, 1L);
                    addResponse(getCommonVector(status));
                    mUser.reCalculatePoint();
                } else {
                    addErrSystem();
                    return;
                }
            } else {
                List<ResWeaponEntity> resWeapons = ResWeapon.mWeaponSummon.get(resPiece.getRank());
                List<ResWeaponEntity> noHas = new ArrayList<>();
                for (int i = 0; i < resWeapons.size(); i++) {
                    if (!mUser.getResources().hasWeapon(resWeapons.get(i).getId())) {
                        noHas.add(resWeapons.get(i));
                    }
                }
                if (noHas.size() == 0) {
                    addErrResponse(getLang(Lang.err_has_weapon));
                    return;
                }
                UserInt uInt = mUser.getUData().getUInt();
                boolean isSuccess = CfgSmithy.isCreateSuccess(uInt, resPiece.getRank(), xu);

                if (isSuccess) { //thành công
                    ResWeaponEntity ret = noHas.get(NumberUtil.getRandom(noHas.size()));
                    bonus.addAll(Bonus.viewWeapon(ret.getId()));
                    List<Long> status = Bonus.receiveListItem(mUser, DetailActionType.CREATE_WEAPON.getKey(ret.getId()), Bonus.merge(bonus));
                    status.add(0, 1L);
                    addResponse(getCommonVector(status));
                    mUser.reCalculatePoint();
                    // tut
                    mUser.getUData().checkStatusTut(mUser, QuestTutType.HAS_WEAPON_ID, ret.getId(), this);
                    mUser.getUData().checkQuestTutorial(mUser, QuestTutType.HAS_WEAPON_BY_RANK, ret.getRank(), 1);
                } else {
                    List<Long> status = Bonus.receiveListItem(mUser, DetailActionType.CREATE_WEAPON.getKey(0), Bonus.merge(bonus));
                    status.add(0, 0L);
                    addResponse(getCommonVector(status));
                }
            }
            mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.CREATE_WEAPON, 1);
            pieceStatus();
            CfgQuest.addNumQuest(mUser, DataQuest.CREATE_WEAPON, number);

        } else if (type == 1) { // Hop thành mảnh
            List<Long> bonus = Bonus.viewGold(-CfgSmithy.config.dataMakePiece.gold.get(resPiece.getRank() - 1) * number);
            if (xu > 0) bonus.addAll(Bonus.viewItem(ItemKey.LUCKY_COIN, -xu));
            bonus.addAll(Bonus.viewPiece(pieceType.value, id, -CfgSmithy.config.dataMakePiece.item.get(resPiece.getRank() - 1) * number));
            String err = Bonus.checkMoney(mUser, bonus);
            if (err != null) {
                addErrResponse(err);
                return;
            }
            boolean isFail = true;
            int numPer = mUser.getResources().getMWeaponByRank().containsKey(resPiece.getRank()) ? mUser.getResources().getMWeaponByRank().get(resPiece.getRank()) * 20 : 0;
            for (int i = 0; i < number; i++) {
                boolean isSuccess = CfgSmithy.isUpgradeSuccess(resPiece.getRank(), xu, number, numPer);
                if (isSuccess) { //thành công
                    bonus.addAll(Bonus.viewPiece(uPiece.getType(), resPiece.getTarget(), 1));
                    isFail = false;
                }
            }
            List<Long> status = Bonus.receiveListItem(mUser, DetailActionType.MAKE_PIECE.getKey(resPiece.getId()), Bonus.merge(bonus));
            status.add(0, isFail ? 0L : 1L);
            addResponse(getCommonVector(status));
            CfgQuest.addNumQuest(mUser, DataQuest.CREATE_PIECE, number);
        } else addErrResponse(getLang(Lang.err_system_down));
    }

    void upgradeWeapon() {
        List<Long> input = getInputALong();
        int id = input.get(0).intValue();
        int xu = input.get(1).intValue();
        if (xu > CfgSmithy.maxXuInput) {
            addErrResponse(String.format(getLang(Lang.err_max_xu), CfgSmithy.maxXuInput));
            return;
        }
        UserWeaponEntity uWeapon = mUser.getResources().getWeapon(id);
        if (uWeapon == null) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        ResWeaponEntity resWe = uWeapon.getRes();
        if (uWeapon.getLevel() >= resWe.getMaxLevel()) {
            addErrResponse(getLang(Lang.err_max_level));
            return;
        }
        List<Long> price = CfgSmithy.getStoneUpLevel(uWeapon);
        if (xu > 0) price.addAll(Bonus.viewItem(ItemKey.LUCKY_COIN, -xu));
        price.addAll(Bonus.viewGold(-CfgSmithy.config.dataUp2.perGold.get(uWeapon.getLevel()) * resWe.getRank()));
        String error = Bonus.checkMoney(mUser, price);
        if (error != null) {
            addErrResponse(error);
            return;
        }
        UserInt uInt = mUser.getUData().getUInt();
        // lần đầu cường hóa thành công
        UserDataEntity uData = mUser.getUData();
        boolean isFirst = uData.getUInt().getValue(UserInt.FIRST_UPGRADE_WEAPON) == 0;
        boolean isUp = isFirst ? true : CfgSmithy.isUpLevelSuccess(uWeapon, resWe.getRank(), xu);
        Pbmethod.ListCommonVector.Builder lst = Pbmethod.ListCommonVector.newBuilder();
        List<Long> retBonus = Bonus.receiveListItem(mUser, DetailActionType.UPGRADE_WEAPON.getKey(id), price);
        if (retBonus.isEmpty()) {
            addErrResponse();
            return;
        }
        lst.addAVector(getCommonVector(retBonus));
        if (isUp) {
            if (uWeapon.updateUpLevel()) {
                mUser.calComboWeapon();
                mUser.reCalculatePoint();
                if (isFirst) {
                    uInt.setValueAndUpdate(UserInt.FIRST_UPGRADE_WEAPON, 1);
                }
                if (uWeapon.getLevel() >= resWe.getMaxLevel())
                    CfgAchievement.addAchievement(mUser, 4, resWe.getId(), 1);
            } else {
                addErrResponse();
                return;
            }
        } else {
            if (!uWeapon.updateBless()) {
                addErrResponse();
                return;
            }
        }
        mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.UPGRADE_WEAPON, 1);
        lst.addAVector(getCommonVector(isUp ? 1L : 0L, uWeapon.getBless()));
        addResponse(lst.build());
        CfgQuest.addNumQuest(mUser, DataQuest.UPGRADE_WEAPON, 1);
        mUser.getUData().checkStatusTut(mUser, QuestTutType.HAS_COMBO_WEAPON, 0, this);
        // check event 7 day
        UserEventSevenDayEntity uEvent = Services.userDAO.getUserSevenDay(mUser);
        if (uEvent.hasEvent() && uEvent.hasActive(6) && uEvent.update(List.of("up_weapon", uEvent.getUpWeapon() + 1))) {
            uEvent.setUpWeapon(uEvent.getUpWeapon() + 1);
        }
    }

    void combine() {
        try {
            List<Long> input = getInputALong();
            int id = input.get(0).intValue();
            int num = input.get(1).intValue();
            if (num < 0) {
                addErrResponse(getLang(Lang.err_params));
                return;
            }
            UserItemEntity uItem = mUser.getResources().getItem(id);
            if (uItem == null) {
                addErrResponse(getLang(Lang.err_params));
                return;
            }
            ResItemEntity res = uItem.getRes();
            ItemKey item = ItemKey.get(uItem.getItemId());
            if (item == null) {
                addErrResponse();
                return;
            }
            if (item.nextId == 0) {
                addErrResponse(getLang(Lang.max_level));
                return;
            }
            List<Long> bonus = Bonus.viewGold((long) -CfgSmithy.config.dataCombine.gold.get(res.getRank()-1) * num);
            bonus.addAll(Bonus.viewItem(item, (long) -CfgSmithy.config.dataCombine.per.get(res.getRank()-1) * num));
            String err = Bonus.checkMoney(mUser, bonus);
            if (err != null) {
                addErrResponse(err);
                return;
            }
            bonus.addAll(Bonus.viewItem(item.nextId, num));
            addResponse(getCommonVector(Bonus.receiveListItem(mUser, DetailActionType.STONE_COMBINE.getKey(uItem.getItemId()), bonus)));
            CfgQuest.addNumQuest(mUser, DataQuest.CREATE_PIECE, num);
        }catch (Exception e){
            e.printStackTrace();
            addErrSystem();
        }
    }

}
