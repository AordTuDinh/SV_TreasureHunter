package game.dragonhero.controller;

import game.battle.model.Player;
import game.config.CfgAchievement;
import game.config.CfgFeature;
import game.config.CfgStat;
import game.config.aEnum.DetailActionType;
import game.config.aEnum.FeatureType;
import game.config.aEnum.ItemKey;
import game.config.aEnum.QuestTutType;
import game.config.lang.Lang;
import game.dragonhero.mapping.UserWeaponEntity;
import game.dragonhero.mapping.main.ResGoldStatEntity;
import game.dragonhero.mapping.main.ResLevelStatEntity;
import game.dragonhero.service.resource.ResStat;
import game.dragonhero.service.user.Actions;
import game.dragonhero.service.user.Bonus;
import game.protocol.CommonProto;
import io.netty.channel.Channel;
import ozudo.base.helper.GsonUtil;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class StatHandler extends AHandler {
    @Override
    public AHandler newInstance() {
        return new StatHandler();
    }

    static StatHandler instance;

    public static StatHandler getInstance() {
        if (instance == null) {
            instance = new StatHandler();
        }
        return instance;
    }

    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(GOLD_STAT_STATUS, POINT_DATA, GOLD_STAT_UPGRADE, LEVEL_STAT_STATUS, LEVEL_STAT_UPGRADE, EQUIP_WEAPON, WEAPON_INFO, AUTO_PROMOTE);
        actions.forEach(action -> mHandler.put(action, this));
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        try {
            switch (actionId) {
                case GOLD_STAT_STATUS:
                    goldStatStatus();
                    break;
                case GOLD_STAT_UPGRADE:
                    if (!CfgFeature.isOpenFeature(FeatureType.SKILL, mUser, this)) {
                        return;
                    }
                    goldStatUpgrade();
                    break;
                case LEVEL_STAT_STATUS:
                    levelStatStatus();
                    break;
                case LEVEL_STAT_UPGRADE:
                    if (!CfgFeature.isOpenFeature(FeatureType.SKILL, mUser, this)) {
                        return;
                    }
                    levelStatUpgrade();
                    break;
                //case UPGRADE_WEAPON:
                //    upgradeWeapon();
                //    break;
                case EQUIP_WEAPON:
                    equipWeapon();
                    break;
                case WEAPON_INFO:
                    weaponInfo();
                    break;
                case AUTO_PROMOTE:
                    //autoPromote();
                    break;
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    public void goldStatStatus() {
        addResponse(mUser.getUData().pbGoldStat().build());
    }

    public void levelStatStatus() {
        addResponse(mUser.getUData().pbLevelStat().build());
    }

    public void goldStatUpgrade() {
        int id = (int) CommonProto.parseCommonVector(requestData).getALong(0);
        int numLevel = (int) CommonProto.parseCommonVector(requestData).getALong(1);

        if (!CfgStat.lstNum.contains(numLevel)) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        ResGoldStatEntity res = ResStat.getGoldItem(id);
        if (res == null) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        int level = mUser.getUData().getlvGoldStat(id);
        // check max level
        if (numLevel > 0) numLevel = numLevel + level > res.getLevelMax() ? res.getLevelMax() - level : numLevel;
        else numLevel = res.getLevelMax() - level;
        if (numLevel <= 0) {
            addErrResponse(getLang(Lang.err_max_level));
            return;
        }
        // check price
        long numFee = ResStat.getFeeItemUpgrade(id, level, numLevel);
        if (numFee < 1) {
            addErrResponse();
            return;
        }
        List<Long> fee = Bonus.viewItem(ItemKey.NANG_LUONG, -numFee);
        String err = Bonus.checkMoney(mUser, fee);
        if (err != null) {
            addErrResponse(err);
            return;
        }
        List<Long> aBonus = Bonus.receiveListItem(mUser, DetailActionType.NANG_KI_NANG2.getKey(), fee);
        if (aBonus.isEmpty()) {
            addErrResponse();
            return;
        }
        if (mUser.getUData().updateGoldStat(id, numLevel)) {
            addResponse(getCommonVector(aBonus));
            mUser.reCalculatePoint();
            addResponse(GOLD_STAT_STATUS, mUser.getUData().pbGoldStat().build());
            mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.UP_SKILL_2, numLevel);
            mUser.getUserDaily().getUDaily();
            // achi chỉ có 10 cái đầu thôi
            if (id <= 13 && level + numLevel >= res.getLevelMax()) {
                CfgAchievement.addAchievement(mUser, 3, id + 29, 1);
            }
        }
    }

    public void levelStatUpgrade() {
        int id = (int) CommonProto.parseCommonVector(requestData).getALong(0);
        int numLevel = (int) CommonProto.parseCommonVector(requestData).getALong(1);
        if (!CfgStat.lstNum.contains(numLevel)) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        ResLevelStatEntity res = ResStat.getLevelItem(id);
        if (res == null) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        // check max level
        int curLevel = mUser.getUData().getlvLevelStat(id);
        if (numLevel != 0) {
            numLevel = numLevel + curLevel > res.getLevelMax() ? res.getLevelMax() - curLevel : numLevel;
        } else numLevel = res.getLevelMax() - curLevel;
        numLevel = numLevel > mUser.getUData().getNumPointLevel() ? mUser.getUData().getNumPointLevel() : numLevel;

        if (numLevel <= 0) {
            addErrResponse(getLang(Lang.err_max_level));
            return;
        }
        if (mUser.getUData().updateLevelStat(id, numLevel)) {
            Actions.save(user, Actions.GRECEIVE, DetailActionType.NANG_KI_NANG1.getKey(numLevel), "type", "levelPoint", "curLevel", curLevel, "addValue", numLevel);
            addResponse(getCommonVector(mUser.getUData().getNumPointLevel()));
            mUser.reCalculatePoint();
            addResponse(LEVEL_STAT_STATUS, mUser.getUData().pbLevelStat().build());
            mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.UP_SKILL_1, numLevel);
            mUser.getUserDaily().getUDaily().update();
            // achi chỉ có 10 cái đầu thôi
            if (id <= 10 && curLevel + numLevel >= res.getLevelMax()) {
                CfgAchievement.addAchievement(mUser, 3, id + 19, 1);
            }
        } else addErrResponse();
    }

    // region weapon
    //void upgradeWeapon() {
    //    int id = (int) CommonProto.parseCommonVector(requestData).getALong(0);
    //    UserWeaponEntity weapon = mUser.getResources().getWeapon(id);
    //    if (weapon == null) {
    //        addErrResponse(getLang(Lang.err_has_item));
    //        return;
    //    }
    //    ResWeaponEntity resWe = weapon.getRes();
    //    if (weapon.getLevel() >= resWe.getMaxLevel()) {
    //        addErrResponse(getLang(Lang.err_max_level));
    //        return;
    //    }
    //    List<Long> price = CfgStat.getFeeUpgrade(weapon);
    //    String error = Bonus.checkMoney(mUser, price);
    //    if (StringHelper.isEmpty(error)) {
    //        weapon.upgradeLevel();
    //        List<Long> retBonus = Bonus.receiveListItem(mUser, DetailActionType.UPGRADE_WEAPON.getKey(id), price);
    //        if (retBonus.isEmpty()) {
    //            addErrResponse();
    //            return;
    //        }
    //        if (DBJPA.update(weapon)) {
    //            Pbmethod.ListCommonVector.Builder lstCmm = Pbmethod.ListCommonVector.newBuilder();
    //            lstCmm.addAVector(mUser.getUser().reCalculatePoint(mUser).toCommonVector());
    //            Pbmethod.CommonVector.Builder cmm = Pbmethod.CommonVector.newBuilder();
    //            cmm.addALong(resWe.getRank());
    //            cmm.addALong(mUser.getUData().cacuLvCombo(mUser, resWe.getRank()));
    //            cmm.addAllALong(retBonus);
    //            cmm.addAString(Lang.getTitle(Lang.upgrade_done));
    //            lstCmm.addAVector(cmm);
    //            addResponse(lstCmm.build());
    //        } else {
    //            addErrResponse();
    //        }
    //    } else addErrResponse(error);
    //}

    void equipWeapon() {
        List<Integer> slots = Arrays.asList(1, 2, 3, 4, 5);
        int id = (int) CommonProto.parseCommonVector(requestData).getALong(0);
        int slot = (int) CommonProto.parseCommonVector(requestData).getALong(1);
        UserWeaponEntity uWea = mUser.getResources().getWeapon(id);
        if (uWea == null) {
            addErrResponse(getLang(Lang.err_has_item));
            return;
        }
        Player player = mUser.getPlayer();
        if (player == null) {
            addErrResponse();
            return;
        }

        List<Integer> lstEquip = user.getWeaponEquipId();
        if (lstEquip.contains(id)) {
            addErrResponse(getLang(Lang.err_item_has_been_used));
            return;
        }
        if (!slots.contains(slot)) {
            addErrResponse(getLang(Lang.invalid_location));
            return;
        }

        if (user.updateWeaponSlot(slot, id, uWea.getLevel())) {
            player.updateWeapon(player.getPoint(), slot - 1, uWea);
            Pbmethod.ListCommonVector.Builder lstCmm = Pbmethod.ListCommonVector.newBuilder();
            Pbmethod.CommonVector.Builder cmm = Pbmethod.CommonVector.newBuilder();
            cmm.addAllALong(GsonUtil.strToListLong(user.getWeapon()));
            cmm.addAString(getLang(Lang.equip_done));
            lstCmm.addAVector(cmm);
            lstCmm.addAVector(mUser.getUser().reCalculatePoint(mUser).toCommonVector());
            addResponse(lstCmm.build());
            // check tut
            mUser.getUData().checkQuestTutDefault(mUser, QuestTutType.USE_WEAPON, 1);
        } else addErrResponse();
    }

    void weaponInfo() {
        List<Long> inputs = getInputALong();
        Pbmethod.PbListUserWeapon.Builder pb = Pbmethod.PbListUserWeapon.newBuilder();
        for (int i = 0; i < inputs.size(); i++) {
            UserWeaponEntity uWeapon = mUser.getResources().getWeapon(inputs.get(i).intValue());
            if (uWeapon != null) pb.addWeapons(uWeapon.toProto(user, mUser.getPlayer().getPoint()));
        }
        addResponse(pb.build());
    }


//    void autoPromote() {
//        int rankType = (int) CommonProto.parseCommonVector(requestData).getALong(0);
//        RankType rank = RankType.get(rankType);
//        if (rank == null) {
//            addErrResponse(Lang.getTitle(Lang.err_params));
//            return;
//        }
//        List<Long> aBonus = new ArrayList<>();
//        for (Map.Entry<Integer, UserWeaponEntity> item : mUser.getResources().getMWeapon().entrySet()) {
//            UserWeaponEntity uWe = item.getValue();
//            ResWeaponEntity res = uWe.getRes();
////            int retNum = uWe.getNumber() / res.getAPromote().get(0);
////            if (retNum > 0 && res.getRank() < rank.value && res.getRank() < RankType.DIVINE.value) {
////                List<Long> newBonus = new ArrayList<>();
////                newBonus.add((long) Bonus.BONUS_WEAPON);
////                // cai nay phai rankdom trong rank nua
////                newBonus.add(ResWeapon.randomItemByRank(res.getRank() + 1));
////                newBonus.add((long) retNum);
////                newBonus.add((long) Bonus.BONUS_WEAPON);
////                // cai nay phai rankdom trong rank nua
////                newBonus.add((long) uWe.getWeaponId());
////                newBonus.add(-(retNum * 10L));
////                aBonus = Bonus.merge(aBonus, newBonus);
////            }
//        }
//        List<Long> retBonus = Bonus.receiveListItem(mUser, "promote_all_weapon", aBonus);
//        if (retBonus.isEmpty()) {
//            addErrResponse(Lang.getTitle(Lang.err_no_bonus));
//            return;
//        }
//        addResponse(CommonProto.getCommonVectorProto(retBonus, null));
//    }

    // endregion


}
